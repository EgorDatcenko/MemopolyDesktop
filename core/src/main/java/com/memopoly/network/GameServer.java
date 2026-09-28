package com.memopoly.network;

import com.badlogic.gdx.Game;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.memopoly.game.model.*;
import com.memopoly.network.guards.AuctionGuard;
import com.memopoly.network.guards.PhaseGuard;
import com.memopoly.network.guards.TurnGuard;
import com.memopoly.network.handlers.BuyCellActionHandler;
import com.memopoly.network.handlers.BuyBackCellActionHandler;
import com.memopoly.network.handlers.PassBuyActionHandler;
import com.memopoly.network.handlers.MortgageCellActionHandler;
import com.memopoly.network.handlers.MemeBankActionHandler;
import com.memopoly.network.services.AuctionTimerService;
import com.memopoly.network.services.GameStatePublisher;
import com.memopoly.modding.DeckRepository;
import com.memopoly.network.packets.*;
import com.memopoly.utils.AppLog;
import com.memopoly.utils.MonopolyUtils;
import com.memopoly.utils.RoomCodeGenerator;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static com.memopoly.network.packets.GameActionRequest.ActionType.SUBMIT_MEME;
import static com.memopoly.network.packets.GameActionRequest.ActionType.VOTE_MEME;

/**
 * Авторитетный игровой сервер на KryoNet: принимает подключения, проверяет
 * валидность действий, обновляет GameState и рассылает его игрокам.
 */
public class GameServer {
    public static final int MAX_PLAYERS = 6;
    private static final int TCP_PORT = 54555;
    private static final String REJECT_NOT_YOUR_TURN = "NOT_YOUR_TURN";
    private static final String REJECT_INVALID_PHASE = "INVALID_PHASE";
    private static final String REJECT_AUCTION_NOT_ACTIVE = "AUCTION_NOT_ACTIVE";
    private static final String REJECT_AUCTION_OTHER_PLAYER_TURN = "AUCTION_OTHER_PLAYER_TURN";
    private static final int JAIL_CELL_ID = 30;
    private static final int BAN_CELL_ID = 10;
    private static final int MAX_HOUSES_PER_CELL = 5;
    private static final int JAIL_FINE = 50;
    private static final int MAX_JAIL_ATTEMPTS = 3;

    private final Server server;
    private final GameState gameState;
    private final List<BoardCell> board = BoardData.buildCells();
    private final Object stateLock = new Object();
    private final BattleManager battleManager;
    private final AuctionTimerService auctionTimerService;
    private final GameStatePublisher gameStatePublisher;
    private final BuyCellActionHandler buyCellActionHandler;
    private final PassBuyActionHandler passBuyActionHandler;
    private final MortgageCellActionHandler mortgageCellActionHandler;
    private final BuyBackCellActionHandler buyBackCellActionHandler;
    private final MemeBankActionHandler memeBankActionHandler;
    private ScheduledFuture<?> battleTimerFuture;
    private GameState.BattlePhase lastBattlePhase = null;
    private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "memopoly-server-timer");
        thread.setDaemon(true);
        return thread;
    });
    private final Map<String, GameState> games = new HashMap<>();
    private final DevRouteController devRoute = new DevRouteController(board);
    private String hostIP;
    private String roomCode;
    private int hostConnectionId = -1;
    private final AtomicInteger dealtMemeIdCounter = new AtomicInteger(-1);
    private final Map<Integer, ScheduledFuture<?>> disconnectTimers = new ConcurrentHashMap<>();
    private volatile boolean isStopping = false;

    private void cancelDisconnectTimer(int playerId) {
        ScheduledFuture<?> timer = disconnectTimers.remove(playerId);
        if (timer != null) {
            timer.cancel(false);
        }
    }

    private void handlePlayerDisconnectGrace(int connectionId) {
        if (isStopping || timerExecutor.isShutdown() || timerExecutor.isTerminated()) {
            removePlayer(connectionId);
            broadcastGameStateUnsafe();
            return;
        }
        Player p = gameState.getPlayerById(connectionId);
        if (p == null || p.isBankrupt) {
            removePlayer(connectionId);
            broadcastGameStateUnsafe();
            return;
        }

        p.isConnected = false;
        String notice = p.name + " временно отключился. Ожидание: 45 сек...";
        gameState.showNotification(notice);
        gameState.lastActionLog = notice;
        broadcastGameStateUnsafe();

        cancelDisconnectTimer(connectionId);
        if (!isStopping && !timerExecutor.isShutdown() && !timerExecutor.isTerminated()) {
            try {
                ScheduledFuture<?> timer = timerExecutor.schedule(() -> {
                    synchronized (stateLock) {
                        disconnectTimers.remove(connectionId);
                        Player disconnected = gameState.getPlayerById(connectionId);
                        if (disconnected != null && !disconnected.isConnected) {
                            AppLog.info("Server", "Истекло время ожидания переподключения игрока " + disconnected.name);
                            String timeoutNotice = disconnected.name + " не вернулся в игру (таймаут 45 сек). Техническое поражение.";
                            gameState.showNotification(timeoutNotice);
                            gameState.lastActionLog = timeoutNotice;
                            removePlayer(disconnected.id);
                            broadcastGameStateUnsafe();
                        }
                    }
                }, 45, TimeUnit.SECONDS);
                disconnectTimers.put(connectionId, timer);
            } catch (RejectedExecutionException ignored) {
            }
        }
    }

    public GameServer() {
        Log.set(Log.LEVEL_DEBUG);
        AppLog.info("Server", "Создаем GameServer...");

        server = new Server(65536, 65536);
        gameState = new GameState();
        auctionTimerService = new AuctionTimerService(timerExecutor);
        gameStatePublisher = new GameStatePublisher(gameState, this::sendAllTcpSafely, this::sendTcpSafely);
        battleManager = new BattleManager(gameState, board, gameStatePublisher::broadcastState, timerExecutor,
                stateLock);
        buyCellActionHandler = new BuyCellActionHandler(
                gameState,
                board,
                this::isCurrentPlayer,
                player -> player.maxAffordable = getMaxAffordable(player),
                this::rejectBuyFlow);
        passBuyActionHandler = new PassBuyActionHandler(
                gameState,
                board,
                this::isCurrentPlayer,
                player -> player.maxAffordable = getMaxAffordable(player),
                this::findNextAuctionBidderId,
                this::startAuctionTimer,
                this::rejectBuyFlow);
        mortgageCellActionHandler = new MortgageCellActionHandler(
                gameState,
                this::isCurrentPlayer,
                player -> player.maxAffordable = getMaxAffordable(player),
                this::handleMortgage);
        buyBackCellActionHandler = new BuyBackCellActionHandler(
                gameState,
                this::isCurrentPlayer,
                player -> player.maxAffordable = getMaxAffordable(player),
                this::handleBuyBack);
        memeBankActionHandler = new MemeBankActionHandler(
                gameState,
                this::canUseMemeBank,
                gameState::getCurrentPlayer,
                this::finishMemeBankAction);

        registerPackets();
        setupServer();
        startServer();

        AppLog.info("Server", "GameServer готов!");
    }

    public String getHostIP() {
        return hostIP;
    }

    public String getRoomCode() {
        return roomCode;
    }

    private void setupServer() {
        server.start();

        try {
            hostIP = findBestIP();
            roomCode = RoomCodeGenerator.createRoomCode(hostIP, TCP_PORT);
            gameState.roomCode = roomCode;
            if (roomCode == null) {
                AppLog.warn("Server", "Не удалось получить код от Worker, используем UNKNOWN");
                roomCode = "UNKNOWN";
            }
            AppLog.info("Server", "Сервер IP: " + hostIP + ":" + TCP_PORT);
            AppLog.info("Server", "Код комнаты: " + roomCode);
        } catch (Exception e) {
            hostIP = "127.0.0.1";
            roomCode = "UNKNOWN";
            AppLog.warn("Server", "Не удалось получить IP/код: " + e.getMessage());
        }

        try {
            server.bind(TCP_PORT);
            AppLog.info("Server", "Сервер запущен на TCP-порту " + TCP_PORT);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось запустить сервер", e);
        }
    }

    public void setTwitchChatService(com.memopoly.network.twitch.TwitchChatService service) {
        if (battleManager != null) {
            battleManager.setTwitchChatService(service);
        }
    }

    public BattleManager getBattleManager() {
        return battleManager;
    }

    private void registerPackets() {
        NetworkRegistry.register(server.getKryo());
    }

    private void startServer() {
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                AppLog.info("Server", "Новый игрок подключился: id=" + connection.getID() + ", remote="
                        + connection.getRemoteAddressTCP());
            }

            @Override
            public void disconnected(Connection connection) {
                if (isStopping) {
                    return;
                }
                synchronized (stateLock) {
                    if (isStopping) {
                        return;
                    }
                    AppLog.info("Server", "Игрок отключился: id=" + connection.getID() + ", remote="
                            + connection.getRemoteAddressTCP());
                    if (gameState.currentPhase != GameState.GamePhase.WAITING
                            && gameState.currentPhase != GameState.GamePhase.GAME_OVER
                            && gameState.players != null && gameState.players.size() > 1) {
                        handlePlayerDisconnectGrace(connection.getID());
                    } else {
                        cancelDisconnectTimer(connection.getID());
                        removePlayer(connection.getID());
                        broadcastGameStateUnsafe();
                    }
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                try {
                    synchronized (stateLock) {
                        handlePacket(connection, object);
                    }
                } catch (Exception e) {
                    AppLog.warn("Server", "Ошибка обработки пакета: type=" + object.getClass().getSimpleName()
                            + ", connectionId=" + connection.getID() + ", reason=" + e.getMessage());
                    e.printStackTrace();
                }
            }
        });
    }

    private void handlePacket(Connection connection, Object packet) {
        String packetClassName = packet.getClass().getName();
        if (packetClassName.startsWith("com.esotericsoftware.kryonet.FrameworkMessage$")) {
            return;
        }

        AppLog.info("Server",
                "Получен пакет: " + packet.getClass().getSimpleName() + ", connectionId=" + connection.getID());

        if (packet instanceof JoinRoomRequest) {
            handleJoinRequest(connection, (JoinRoomRequest) packet);
        } else if (packet instanceof RollDiceRequest) {
            handleRollDice(connection);
        } else if (packet instanceof StartGameRequest) {
            handleStartGame(connection, (StartGameRequest) packet);
        } else if (packet instanceof GameActionRequest) {
            handleGameAction(connection, (GameActionRequest) packet);
        } else if (packet instanceof BattleResponsePacket) {
            battleManager.handleBattleResponse((BattleResponsePacket) packet);
        } else if (packet instanceof ChatMessage) {
            handleChatMessage(connection, (ChatMessage) packet);
        } else if (packet instanceof TradeOfferPacket) {
            handleTradeOffer(connection, (TradeOfferPacket) packet);
        } else if (packet instanceof TradeResponsePacket) {
            handleTradeResponse(connection, (TradeResponsePacket) packet);
        } else if (packet instanceof TradeCancelPacket) {
            handleTradeCancel(connection);
        } else {
            AppLog.warn("Server", "Неизвестный тип пакета: " + packet.getClass());
        }
    }

    private void handleChatMessage(Connection connection, ChatMessage message) {
        if (message == null || message.message == null || message.message.trim().isEmpty()) {
            return;
        }
        String text = message.message.trim();
        if (text.length() > 180) {
            text = text.substring(0, 180);
        }
        Player sender = gameState.getPlayerById(connection.getID());
        ChatMessage broadcast = new ChatMessage();
        broadcast.playerId = connection.getID();
        broadcast.playerName = sender != null && sender.name != null ? sender.name : "Player " + connection.getID();
        broadcast.message = text;
        broadcast.isSystem = false;
        broadcast.timestamp = System.currentTimeMillis();
        sendAllTcpSafely(broadcast);
    }

    private void handleTradeOffer(Connection connection, TradeOfferPacket packet) {
        synchronized (stateLock) {
            Player proposer = gameState.getPlayerById(connection.getID());
            if (proposer == null)
                return;

            // Валидация: инициатор — текущий игрок, фаза PLAYING, не в баттле/аукционе, нет
            // активной сделки
            if (!isCurrentPlayer(connection.getID())) {
                AppLog.warn("Server", "Trade offer rejected: not current player");
                return;
            }
            if (gameState.currentPhase != GameState.GamePhase.PLAYING) {
                AppLog.warn("Server", "Trade offer rejected: invalid phase " + gameState.currentPhase);
                return;
            }
            if (gameState.isInBattle || gameState.isInAuction) {
                AppLog.warn("Server", "Trade offer rejected: in battle or auction");
                return;
            }
            if (gameState.tradeId != 0) {
                AppLog.warn("Server", "Trade offer rejected: active trade exists");
                return;
            }

            // >= 1 клетка в предложении
            if (packet.myCells == null || packet.myCells.isEmpty()) {
                AppLog.warn("Server", "Trade offer rejected: no cells from proposer");
                return;
            }

            // Проверка что клетки реально принадлежат заявленным владельцам и без филиалов
            for (int cellId : packet.myCells) {
                Integer ownerId = gameState.cellOwners.get(cellId);
                if (ownerId == null || ownerId != proposer.id) {
                    AppLog.warn("Server", "Trade offer rejected: cell " + cellId + " not owned by proposer");
                    return;
                }
                if (gameState.cellHouses.getOrDefault(cellId, 0) > 0) {
                    AppLog.warn("Server", "Trade offer rejected: cell " + cellId + " has houses");
                    return;
                }
            }

            // Цель должна существовать и быть не банкротом
            Player target = gameState.getPlayerById(packet.targetId);
            if (target == null || target.isBankrupt) {
                AppLog.warn("Server", "Trade offer rejected: invalid target");
                return;
            }

            // Все theirCells должны принадлежать ОДНОМУ сопернику (target)
            if (packet.theirCells != null && !packet.theirCells.isEmpty()) {
                for (int cellId : packet.theirCells) {
                    Integer ownerId = gameState.cellOwners.get(cellId);
                    if (ownerId == null || ownerId != target.id) {
                        AppLog.warn("Server", "Trade offer rejected: cell " + cellId + " not owned by target");
                        return;
                    }
                    if (gameState.cellHouses.getOrDefault(cellId, 0) > 0) {
                        AppLog.warn("Server", "Trade offer rejected: cell " + cellId + " has houses");
                        return;
                    }
                }
            }

            // Проверка балансов монет
            if (packet.myMoney < 0 || packet.myMoney > proposer.money) {
                AppLog.warn("Server", "Trade offer rejected: proposer money invalid");
                return;
            }
            if (packet.theirMoney < 0 || packet.theirMoney > target.money) {
                AppLog.warn("Server", "Trade offer rejected: target money invalid");
                return;
            }

            // Сделка должна содержать >= 1 клетку (проверено выше для myCells, но их клетки
            // могут быть пустыми)
            if ((packet.myCells == null || packet.myCells.isEmpty())
                    && (packet.theirCells == null || packet.theirCells.isEmpty())) {
                AppLog.warn("Server", "Trade offer rejected: no cells at all");
                return;
            }

            // Сохраняем сделку
            gameState.tradeId = ++gameState.turnCount; // уникальный ID
            gameState.tradeProposerId = proposer.id;
            gameState.tradeTargetId = target.id;
            gameState.tradeProposerCells = new ArrayList<>(packet.myCells);
            gameState.tradeTargetCells = packet.theirCells != null ? new ArrayList<>(packet.theirCells)
                    : new ArrayList<>();
            gameState.tradeProposerMoney = packet.myMoney;
            gameState.tradeTargetMoney = packet.theirMoney;

            gameState.lastActionLog = proposer.name + " предложил сделку игроку " + target.name;
            broadcastGameStateUnsafe();
        }
    }

    private void handleTradeResponse(Connection connection, TradeResponsePacket packet) {
        synchronized (stateLock) {
            Player target = gameState.getPlayerById(connection.getID());
            if (target == null)
                return;

            // Только цель может ответить и только при активной сделке
            if (gameState.tradeId == 0 || gameState.tradeTargetId != target.id) {
                AppLog.warn("Server", "Trade response rejected: not target or no active trade");
                return;
            }

            Player proposer = gameState.getPlayerById(gameState.tradeProposerId);
            if (proposer == null || proposer.isBankrupt) {
                // Инициатор банкрот — отмена
                cancelTradeInternal();
                return;
            }

            if (packet.accept) {
                // ПОВТОРНАЯ валидация перед исполнением
                // Проверка владения клетками
                for (int cellId : gameState.tradeProposerCells) {
                    Integer ownerId = gameState.cellOwners.get(cellId);
                    if (ownerId == null || ownerId != proposer.id) {
                        AppLog.warn("Server", "Trade execution rejected: proposer no longer owns cell " + cellId);
                        cancelTradeInternal();
                        return;
                    }
                    if (gameState.cellHouses.getOrDefault(cellId, 0) > 0) {
                        AppLog.warn("Server", "Trade execution rejected: cell " + cellId + " has houses");
                        cancelTradeInternal();
                        return;
                    }
                }
                for (int cellId : gameState.tradeTargetCells) {
                    Integer ownerId = gameState.cellOwners.get(cellId);
                    if (ownerId == null || ownerId != target.id) {
                        AppLog.warn("Server", "Trade execution rejected: target no longer owns cell " + cellId);
                        cancelTradeInternal();
                        return;
                    }
                    if (gameState.cellHouses.getOrDefault(cellId, 0) > 0) {
                        AppLog.warn("Server", "Trade execution rejected: cell " + cellId + " has houses");
                        cancelTradeInternal();
                        return;
                    }
                }

                // Проверка балансов
                if (gameState.tradeProposerMoney > proposer.money || gameState.tradeTargetMoney > target.money) {
                    AppLog.warn("Server", "Trade execution rejected: insufficient funds");
                    cancelTradeInternal();
                    return;
                }

                // Атомарное исполнение: обмен клетками
                for (int cellId : gameState.tradeProposerCells) {
                    gameState.cellOwners.put(cellId, target.id);
                    proposer.ownedCells.remove(Integer.valueOf(cellId));
                    target.ownedCells.add(cellId);
                }
                for (int cellId : gameState.tradeTargetCells) {
                    gameState.cellOwners.put(cellId, proposer.id);
                    target.ownedCells.remove(Integer.valueOf(cellId));
                    proposer.ownedCells.add(cellId);
                }

                // Перевод монет в обе стороны
                proposer.pay(gameState.tradeProposerMoney);
                target.receive(gameState.tradeProposerMoney);
                target.pay(gameState.tradeTargetMoney);
                proposer.receive(gameState.tradeTargetMoney);

                gameState.lastActionLog = "Сделка между " + proposer.name + " и " + target.name + " завершена!";
                gameState.showNotification(proposer.name + " и " + target.name + " обменялись клетками и монетами");

                gameState.recordAchievement(proposer.id, "FIRST_TRADE");
                gameState.recordAchievement(target.id, "FIRST_TRADE");

                proposer.successfulTrades++;
                target.successfulTrades++;

                if (proposer.successfulTrades >= 5) {
                    gameState.recordAchievement(proposer.id, "TRADING_POST");
                }
                if (target.successfulTrades >= 5) {
                    gameState.recordAchievement(target.id, "TRADING_POST");
                }

                proposer.tradedWith.add(target.id);
                target.tradedWith.add(proposer.id);

                for (Player p : gameState.players) {
                    if (!p.isBankrupt && p.id != p.id && !p.tradedWith.containsAll(getOtherPlayerIds(p))) {
                        continue;
                    }
                    if (p.tradedWith.size() >= gameState.players.size() - 1) {
                        gameState.recordAchievement(p.id, "NEGOTIATOR");
                    }
                }
                // Завершение сделки
                gameState.tradeId = 0;
                gameState.tradeProposerId = -1;
                gameState.tradeTargetId = -1;
                gameState.tradeProposerCells.clear();
                gameState.tradeTargetCells.clear();
                gameState.tradeProposerMoney = 0;
                gameState.tradeTargetMoney = 0;

                broadcastGameStateUnsafe();
            } else {
                // Decline
                gameState.lastActionLog = target.name + " отклонил сделку";
                gameState.tradeId = 0;
                gameState.tradeProposerId = -1;
                gameState.tradeTargetId = -1;
                gameState.tradeProposerCells.clear();
                gameState.tradeTargetCells.clear();
                gameState.tradeProposerMoney = 0;
                gameState.tradeTargetMoney = 0;
                broadcastGameStateUnsafe();
            }
        }
    }

    private java.util.Set<Integer> getOtherPlayerIds(Player p) {
        java.util.Set<Integer> ids = new java.util.HashSet<>();
        for (Player other : gameState.players) {
            if (other.id != p.id && !other.isBankrupt) {
                ids.add(other.id);
            }
        }
        return ids;
    }

    private void handleTradeCancel(Connection connection) {
        synchronized (stateLock) {
            Player proposer = gameState.getPlayerById(connection.getID());
            if (proposer == null)
                return;

            // Только инициатор может отменить
            if (gameState.tradeId == 0 || gameState.tradeProposerId != proposer.id) {
                AppLog.warn("Server", "Trade cancel rejected: not proposer or no active trade");
                return;
            }

            gameState.lastActionLog = proposer.name + " отменил сделку";
            cancelTradeInternal();
        }
    }

    private void cancelTradeInternal() {
        gameState.tradeId = 0;
        gameState.tradeProposerId = -1;
        gameState.tradeTargetId = -1;
        gameState.tradeProposerCells.clear();
        gameState.tradeTargetCells.clear();
        gameState.tradeProposerMoney = 0;
        gameState.tradeTargetMoney = 0;
        broadcastGameStateUnsafe();
    }

    private Role roleOf(int playerId) {
        return Role.of(gameState.playerRoles == null ? null : gameState.playerRoles.get(playerId));
    }

    private int housePriceFor(BoardCell cell, Player player) {
        return MonopolyUtils.getHousePrice(cell, player != null ? roleOf(player.id) : null);
    }

    private void handleJoinRequest(Connection connection, JoinRoomRequest request) {
        String playerName = request != null ? request.playerName : null;

        if (playerName == null || playerName.trim().isEmpty()) {
            JoinRoomResponse reject = new JoinRoomResponse();
            reject.success = false;
            reject.playerId = -1;
            sendTcpSafely(connection, reject);
            return;
        }

        // Проверяем, не переподключается ли отключившийся игрок
        Player reconnectPlayer = null;
        for (Player p : gameState.players) {
            if (!p.isConnected) {
                if ((request.steamId > 0 && p.steamId == request.steamId)
                        || (p.name != null && p.name.equalsIgnoreCase(playerName.trim()))) {
                    reconnectPlayer = p;
                    break;
                }
            }
        }

        if (reconnectPlayer != null) {
            int oldId = reconnectPlayer.id;
            int newId = connection.getID();
            cancelDisconnectTimer(oldId);
            reconnectPlayer.isConnected = true;
            if (oldId != newId) {
                gameState.remapPlayerId(oldId, newId);
                if (hostConnectionId == oldId) {
                    hostConnectionId = newId;
                }
            }
            JoinRoomResponse response = new JoinRoomResponse();
            response.success = true;
            response.playerId = newId;
            sendTcpSafely(connection, response);
            String notice = reconnectPlayer.name + " вернулся в игру!";
            gameState.showNotification(notice);
            gameState.lastActionLog = notice;
            broadcastGameStateUnsafe();
            return;
        }

        if (gameState.getPlayerById(connection.getID()) != null) {
            return;
        }

        if (gameState.players.size() >= MAX_PLAYERS) {
            JoinRoomResponse reject = new JoinRoomResponse();
            reject.success = false;
            reject.playerId = -1;
            sendTcpSafely(connection, reject);
            AppLog.warn("Server", "Лобби заполнено (максимум " + MAX_PLAYERS + " игроков)");
            return;
        }

        Player newPlayer = new Player(connection.getID(), playerName.trim());
        newPlayer.steamId = request.steamId;
        if (gameState.players.isEmpty()) {
            hostConnectionId = connection.getID();
        }
        gameState.addPlayer(newPlayer);

        JoinRoomResponse response = new JoinRoomResponse();
        response.success = true;
        response.playerId = connection.getID();

        sendTcpSafely(connection, response);
        broadcastGameStateUnsafe();
    }

    private void handleStartGame(Connection connection, StartGameRequest request) {
        if (connection.getID() != hostConnectionId) {
            return;
        }

        if (gameState.players.size() < 3 || gameState.players.size() > MAX_PLAYERS) {
            AppLog.warn("Server", "Нельзя запустить игру: нужно от 3 до " + MAX_PLAYERS + " игроков");
            return;
        }

        gameState.selectedDeckName = request == null ? null : request.deckName;
        gameState.botMemeEnabled = false;

        gameState.rolesEnabled = request != null && request.rolesEnabled;
        gameState.playerRoles = new HashMap<>();
        gameState.roleUsedThisRound = new HashMap<>();
        if (gameState.rolesEnabled) {
            ArrayList<Role> pool = new ArrayList<>(Arrays.asList(Role.values()));
            Collections.shuffle(pool);
            int i = 0;
            for (Player p : gameState.players) {
                gameState.playerRoles.put(p.id, pool.get(i++ % pool.size()).name());
            }
            gameState.lastActionLog += " | Роли выданы!";
        }

        dealSelectedDeckMemes();
        gameState.currentPhase = GameState.GamePhase.PLAYING;
        gameState.lastActionLog = gameState.selectedDeckName == null || gameState.selectedDeckName.isBlank()
                ? "Игра началась"
                : "Игра началась | Колода: " + gameState.selectedDeckName;
        gameState.turnCount = 1;
        gameState.currentPlayerIndex = 0;
        gameState.diceValue = 0;
        gameState.hasRolledThisTurn = false;
        gameState.memeBankPlayerId = -1;
        broadcastGameStateUnsafe();
    }

    private void handleRollDice(Connection connection) {
        Player current = gameState.getCurrentPlayer();
        int currentId = current != null ? current.id : -1;
        System.out.println("[SERVER_DICE] 3. Packet received from player: " + connection.getID()
                + ", currentTurnPlayer: " + currentId + ", phase: " + gameState.currentPhase);

        if (!isCurrentPlayer(connection.getID()) || (gameState.currentPhase != GameState.GamePhase.PLAYING
                && gameState.currentPhase != GameState.GamePhase.PLAYER_ACTION)) {
            System.out.println("[SERVER_DICE] REJECTED: isCurrentPlayer=" + isCurrentPlayer(connection.getID())
                    + ", phase=" + gameState.currentPhase);
            return;
        }

        if (gameState.hasRolledThisTurn) {
            System.out.println("[SERVER_DICE] REJECTED: hasRolledThisTurn is true!");
            return;
        }

        int dice1 = ThreadLocalRandom.current().nextInt(1, 7);
        int dice2 = ThreadLocalRandom.current().nextInt(1, 7);
        int total = dice1 + dice2;

        if (current == null) {
            return;
        }

        System.out
                .println("[SERVER_DICE] 4. Broadcast roll result: " + dice1 + " + " + dice2 + " (total=" + total + ")");

        gameState.diceValue = total;
        gameState.hasRolledThisTurn = true;
        current.maxAffordable = getMaxAffordable(current);

        boolean isDoubles = (dice1 == dice2);

        if (current.inJail) {
            handleJailDiceRoll(current, dice1, dice2, total);

            RollDiceResponse response = new RollDiceResponse();
            response.playerId = connection.getID();
            response.dice1 = dice1;
            response.dice2 = dice2;
            response.total = total;
            response.newPosition = current.position;

            sendAllTcpSafely(response);
            broadcastGameStateUnsafe();
            return;
        }

        if (isDoubles) {
            current.consecutiveDoubles++;
            if (current.consecutiveDoubles >= 3) {
                current.consecutiveDoubles = 0;
                gameState.lastActionLog = current.name + " выбросил дубль 3 раза подряд и отправляется в Бан!";
                notifyPlayers(current.name + " выбросил дубль 3 раза подряд и отправляется в Бан!");
                sendToJail(current);

                RollDiceResponse response = new RollDiceResponse();
                response.playerId = connection.getID();
                response.dice1 = dice1;
                response.dice2 = dice2;
                response.total = total;
                response.newPosition = current.position;

                sendAllTcpSafely(response);
                broadcastGameStateUnsafe();
                return;
            }
        } else {
            current.consecutiveDoubles = 0;
        }

        int oldPosition = current.position;
        current.position = (oldPosition + total) % board.size();
        gameState.lastActionLog = current.name + " бросил кубики: " + total + (isDoubles ? " (Дубль!)" : "");

        if (oldPosition + total >= board.size()) {
            int startBonus = roleOf(current.id) == Role.MEMOLOG ? 215 : 200;
            current.receive(startBonus);
            gameState.roleUsedThisRound.put(current.id, false);
            if (gameState.bankruptEventUsedRound != null) {
                gameState.bankruptEventUsedRound.clear();
            }
            String startMsg = current.name + " прошёл(ла) Старт и получает " + startBonus + " монет!";
            gameState.lastActionLog = startMsg;
            notifyPlayers(startMsg);

            for (Player p : gameState.players) {
                if (p.memeBankBalance > 0) {
                    int percent = roleOf(p.id) == Role.SMM ? 20 : 10;
                    int interest = p.memeBankBalance * percent / 100;
                    p.memeBankBalance += interest;
                    gameState.lastActionLog += " | " + p.name + " получил " + interest + " % по вкладу";
                }
            }
        }

        BoardCell cell = board.get(current.position);
        if (roleOf(current.id) == Role.DOGE && !gameState.roleUsedThisRound.getOrDefault(current.id, false)) {
            gameState.awaitingReroll = true;
            gameState.rerollPlayerId = current.id;
            gameState.rerollOrigin = oldPosition;
            gameState.lastActionLog = current.name + " может пойти на +2 клетки (Доге)";
        } else {
            handleCellLanding(current, cell);
        }

        if (isDoubles && !current.inJail) {
            gameState.hasRolledThisTurn = false;
        }

        RollDiceResponse response = new RollDiceResponse();
        response.playerId = connection.getID();
        response.dice1 = dice1;
        response.dice2 = dice2;
        response.total = total;
        response.newPosition = current.position;

        sendAllTcpSafely(response);
        broadcastGameStateUnsafe();
    }

    private void sendToJail(Player player) {
        if (player == null)
            return;
        player.inJail = true;
        player.jailTurns = 0;
        player.jailRollAttempts = 0;
        player.consecutiveDoubles = 0;
        player.position = BAN_CELL_ID;
        gameState.hasRolledThisTurn = true;
        gameState.lastActionLog = player.name + " отправлен в Бан!";
        notifyPlayers(player.name + " отправлен в Бан!");
        gameState.nextPlayer();
    }

    private void handleCellLanding(Player current, BoardCell cell) {
        if (cell == null) {
            return;
        }

        switch (cell.type) {
            case START:
                break;
            case REST:
                if (cell.id == 20) {
                    current.shields = Math.min(2, current.shields + 1);
                    String shieldMsg = current.name + " получает щит на Бесплатной стоянке!";
                    gameState.lastActionLog = shieldMsg;
                    notifyPlayers(shieldMsg);
                }
                break;
            case JAIL:
                if (roleOf(current.id) == Role.MODERATOR) {
                    if (current.shields >= 2) {
                        gameState.lastActionLog = current.name + " (Модератор со 2 щитами) автоматически обходит Ban";
                        break;
                    } else {
                        gameState.moderatorChoicePending = true;
                        gameState.moderatorPlayerId = current.id;
                        gameState.lastActionLog = current.name
                                + " (Модератор) выбирает: пропустить Ban или сесть + получить щит";
                        break;
                    }
                }
                sendToJail(current);
                break;
            case TAX:
                if (current.shields > 0) {
                    current.shields -= 1;
                    gameState.lastActionLog = current.name + " заблокировал налог щитом!";
                } else {
                    int taxFee = 100;
                    if (current.money < taxFee) {
                        gameState.pendingDebtPlayerId = current.id;
                        gameState.pendingDebtAmount = taxFee;
                        gameState.pendingDebtRecipientId = -1;
                        String taxMsg = "Игрок " + current.name + " задолжал налог " + taxFee;
                        gameState.lastActionLog = taxMsg;
                        notifyPlayers(taxMsg);
                    } else {
                        current.pay(taxFee);
                        gameState.lastActionLog = current.name + " заплатил налог " + taxFee;
                    }
                }
                break;
            case EVENT:
                EventCard eventCard = EventDeck.drawRandom();
                applyEventCard(current, eventCard);
                notifyPlayers(current.name + ": " + eventCard.title + " - " + eventCard.description);
                break;
            case SITUATION:
                int cellIndex = cell.id;
                if (!gameState.cellOwners.containsKey(cellIndex)) {
                    gameState.currentPhase = GameState.GamePhase.PLAYER_ACTION;
                } else if (gameState.cellOwners.get(cellIndex) != current.id) {
                    boolean mortgaged = gameState.cellMortgaged.getOrDefault(cellIndex, false);
                    if (!mortgaged) {
                        Player owner = gameState.getPlayerById(gameState.cellOwners.get(cellIndex));
                        if (current.skipNextRentCollection) {
                            current.skipNextRentCollection = false;
                            gameState.lastActionLog = current.name + " избежал оплаты доната (эффект карты)";
                        } else {
                            int fee = calculateRent(cell);
                            String ownerName = (owner != null ? owner.name : "?");
                            if (current.money < fee) {
                                gameState.pendingDebtPlayerId = current.id;
                                gameState.pendingDebtAmount = fee;
                                gameState.pendingDebtRecipientId = (owner != null ? owner.id : -1);
                                String rentMsg = "Игрок " + current.name + " задолжал " + fee + " игроку "
                                        + ownerName + " за клетку " + cell.name;
                                gameState.lastActionLog = rentMsg;
                                notifyPlayers(rentMsg);
                            } else {
                                current.pay(fee);
                                if (owner != null)
                                    owner.receive(fee);
                                String rentMsg = "Игрок " + current.name + " задонатил " + fee + " игроку "
                                        + ownerName + ", попав на клетку " + cell.name;
                                gameState.lastActionLog = rentMsg;
                                notifyPlayers(rentMsg);
                            }
                        }
                    }
                }
                break;
            case MEME_BANK:
                gameState.currentPhase = GameState.GamePhase.MEME_BANK_ACTION;
                gameState.memeBankPlayerId = current.id;
                gameState.lastActionLog = current.name + " попал на Meme Bank";
                break;
            case MEME_BATTLE:
                battleManager.startSetup(current.id, cell.id);
                startBattleTimer();
                break;
            default:
                break;
        }
    }

    /**
     * Handles dice roll while player is in jail.
     * - If doubles are rolled: player gets out of jail immediately and moves the
     * dice total
     * - If not doubles: increment jailTurns counter
     * - After 3 failed attempts: player must pay fine or stays in jail
     */
    private void handleJailDiceRoll(Player current, int dice1, int dice2, int total) {
        if (current == null)
            return;

        boolean isDoubles = (dice1 == dice2);
        current.jailRollAttempts++;
        current.jailTurns = current.jailRollAttempts;

        if (isDoubles) {
            // Got doubles! Get out of jail immediately and move
            current.inJail = false;
            current.jailTurns = 0;
            current.jailRollAttempts = 0;

            // Move player after getting out of jail
            int oldPosition = current.position;
            current.position = (oldPosition + total) % board.size();
            gameState.lastActionLog = current.name + " выбросил дубль (" + dice1 + ":" + dice2 + ") и выходит из Бана!";
            notifyPlayers(current.name + " выбросил дубль и выходит из Бана!");
            gameState.recordAchievement(current.id, "JAILBREAK");

            // Check if passed Start
            if (oldPosition + total >= board.size()) {
                int startBonus = roleOf(current.id) == Role.MEMOLOG ? 215 : 200;
                current.receive(startBonus);
                String startMsg = current.name + " прошёл(ла) Старт и получает " + startBonus + " монет!";
                gameState.lastActionLog += " | " + startMsg;
                notifyPlayers(startMsg);
                for (Player p : gameState.players) {
                    if (p.memeBankBalance > 0) {
                        int percent = roleOf(p.id) == Role.SMM ? 20 : 10;
                        int interest = p.memeBankBalance * percent / 100;
                        p.memeBankBalance += interest;
                        gameState.lastActionLog += " | " + p.name + " получил " + interest + " % по вкладу";
                        if (p.memeBankBalance >= 500) {
                            gameState.recordAchievement(p.id, "BANKER");
                        }
                    }
                }
            }

            // Handle landing on cell
            BoardCell cell = board.get(current.position);
            handleCellLanding(current, cell);
        } else {
            // No doubles
            if (current.jailRollAttempts >= MAX_JAIL_ATTEMPTS) {
                // After 3 failed attempts, auto pay 50 coins and move
                current.money -= JAIL_FINE;
                current.inJail = false;
                current.jailTurns = 0;
                current.jailRollAttempts = 0;
                gameState.lastActionLog = current.name + " выходит из Бана, заплатив " + JAIL_FINE + " монет";
                notifyPlayers(current.name + " выходит из Бана, заплатив " + JAIL_FINE + " монет");

                int oldPosition = current.position;
                current.position = (oldPosition + total) % board.size();

                if (current.position < oldPosition) {
                    int startBonus = roleOf(current.id) == Role.MEMOLOG ? 215 : 200;
                    current.receive(startBonus);
                }

                BoardCell cell = board.get(current.position);
                handleCellLanding(current, cell);
            } else {
                // Still have attempts remaining
                current.inJail = true;
                gameState.lastActionLog = current.name + " остаётся в Бане (попытка " + current.jailRollAttempts
                        + "/3)";
            }
        }
    }

    public void handleGameAction(Connection connection, GameActionRequest request) {

        if (request == null || request.actionType == null) {
            return;
        }

        Player actingPlayer = gameState.getPlayerById(connection.getID());
        if (actingPlayer == null) {
            return;
        }

        /* ---- DEV‑блок ---- */
        if (request.actionType.name().startsWith("DEV_")) {
            // Только хост может переключать режим
            if (request.actionType == GameActionRequest.ActionType.DEV_SET_TEST_MODE) {
                if (connection.getID() != hostConnectionId) {
                    rejectAction(connection, actingPlayer,
                            request.actionType,
                            "NOT_HOST",
                            "Only host can toggle testMode");
                    return;
                }
                gameState.testMode = request.amount != 0;
                broadcastGameStateUnsafe();
                return;
            }

            if (!gameState.testMode) {
                rejectAction(connection, actingPlayer,
                        request.actionType,
                        "TEST_MODE_OFF",
                        "Dev‑commands only allowed when testMode is ON");
                return;
            }

            // Остальные DEV‑действия
            synchronized (stateLock) {
                switch (request.actionType) {
                    case DEV_NEXT_GROUP_CELL -> {
                        if (!isCurrentPlayer(connection.getID())) {
                            rejectAction(connection, actingPlayer,
                                    request.actionType,
                                    "NOT_YOUR_TURN",
                                    "Only current player can teleport");
                            break;
                        }
                        int next = devRoute.nextUnownedCell(gameState, connection.getID());
                        if (next == -1) {
                            rejectAction(connection, actingPlayer,
                                    request.actionType,
                                    "GROUP_DONE",
                                    "All cells in target group already owned");
                            break;
                        }
                        gameState.players.get(connection.getID()).position = next;
                        gameState.hasRolledThisTurn = true;
                        BoardCell newCell = board.get(next);
                        handleCellLanding(gameState.players.get(connection.getID()), newCell);
                    }
                    case DEV_SET_MONEY -> {
                        if (request.amount <= 0) {
                            rejectAction(connection, actingPlayer,
                                    request.actionType,
                                    "INVALID_AMOUNT",
                                    "Amount must be positive");
                            break;
                        }
                        gameState.players.get(connection.getID()).money += request.amount;
                    }
                    case DEV_GRANT_MONOPOLY -> {
                        Map<BoardCell.Group, List<BoardCell>> groupMap = new LinkedHashMap<>();
                        for (BoardCell c : board) {
                            if (c.isBuildableSituation()) {
                                groupMap.computeIfAbsent(c.group, k -> new ArrayList<>()).add(c);
                            }
                        }
                        List<BoardCell> targetGroup = null;
                        for (List<BoardCell> cells : groupMap.values()) {
                            boolean ownsAll = cells.stream()
                                    .allMatch(c -> gameState.cellOwners.getOrDefault(c.id, -1) == actingPlayer.id);
                            if (!ownsAll) {
                                targetGroup = cells;
                                break;
                            }
                        }
                        if (targetGroup == null && !groupMap.isEmpty()) {
                            targetGroup = groupMap.values().iterator().next();
                        }
                        if (targetGroup != null) {
                            for (BoardCell c : targetGroup) {
                                gameState.cellOwners.put(c.id, actingPlayer.id);
                                if (!actingPlayer.ownedCells.contains(c.id)) {
                                    actingPlayer.ownedCells.add(c.id);
                                }
                            }
                            gameState.lastActionLog = actingPlayer.name + " (DEV) получил монополию группы "
                                    + targetGroup.get(0).group;
                        }
                    }
                    case DEV_SEND_TO_JAIL -> {
                        sendToJail(actingPlayer);
                    }
                    case DEV_START_BATTLE -> {
                        int cellIndex = actingPlayer.position;
                        battleManager.startSetup(actingPlayer.id, cellIndex);
                        gameState.lastActionLog = actingPlayer.name + " (DEV) запустил Мем-Баттл";
                    }
                    default -> rejectAction(connection, actingPlayer,
                            request.actionType,
                            "UNKNOWN_DEV",
                            "Unknown dev command");
                }
                broadcastGameStateUnsafe();
            }
            return;
        }
        switch (request.actionType) {
            case START_MEME_BATTLE: {
                if (gameState.isInBattle
                        && gameState.battlePhase == GameState.BattlePhase.BATTLE_SETUP
                        && isCurrentPlayer(connection.getID())) {
                    String topic = (request.data != null && !request.data.isBlank()) ? request.data.trim() : "";
                    int stakes = Math.min(200, Math.max(10, request.amount));
                    battleManager.confirmSetup(actingPlayer.id, topic, stakes);
                }
                break;
            }
            case CANCEL_MEME_BATTLE: {
                if (gameState.isInBattle
                        && gameState.battlePhase == GameState.BattlePhase.BATTLE_SETUP
                        && gameState.battleOwnerId == actingPlayer.id) {
                    battleManager.cancelSetup(actingPlayer.id);
                }
                break;
            }
            case BUY_CELL:
                if (buyCellActionHandler.handle(connection, actingPlayer, request)) {
                    break;
                }
                break;
            case PASS_BUY:
                if (passBuyActionHandler.handle(connection, actingPlayer, request)) {
                    if (gameState.auctionCurrentPlayerId != -1) {
                        gameState.lastActionLog = "Начинается аукцион! Первый ход: "
                                + getPlayerName(gameState.auctionCurrentPlayerId);
                    }
                    break;
                }
                break;
            case MORTGAGE_CELL:
                mortgageCellActionHandler.handle(connection, actingPlayer, request);
                break;
            case BUY_BACK_CELL:
                buyBackCellActionHandler.handle(connection, actingPlayer, request);
                break;
            case BUY_HOUSE:
                handleBuyHouse(connection, actingPlayer, request.targetId);
                break;
            case SELL_HOUSE:
                handleSellHouse(connection, actingPlayer, request.targetId);
                break;
            case MEME_BANK_DEPOSIT:
                memeBankActionHandler.handle(connection, actingPlayer, request);
                break;
            case MEME_BANK_WITHDRAW:
                memeBankActionHandler.handle(connection, actingPlayer, request);
                break;
            case MEME_BANK_SKIP:
                memeBankActionHandler.handle(connection, actingPlayer, request);
                break;
            case PLACE_AUCTION_BID:
                if (!AuctionGuard.isAuctionActive(gameState)) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_AUCTION_NOT_ACTIVE,
                            "сейчас нет активного аукциона");
                    return;
                }
                if (!AuctionGuard.isCurrentAuctionBidder(gameState, actingPlayer.id)) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_AUCTION_OTHER_PLAYER_TURN,
                            "сейчас ход другого участника аукциона");
                    return;
                }
                actingPlayer.maxAffordable = getMaxAffordable(actingPlayer);
                handleAuctionBid(actingPlayer, request.amount);
                break;
            case CANCEL_AUCTION:
                if (!AuctionGuard.isAuctionActive(gameState)) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_AUCTION_NOT_ACTIVE,
                            "сейчас нет активного аукциона");
                    return;
                }
                if (!AuctionGuard.isCurrentAuctionBidder(gameState, actingPlayer.id)) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_AUCTION_OTHER_PLAYER_TURN,
                            "сейчас ход другого участника аукциона");
                    return;
                }
                gameState.lastActionLog = actingPlayer.name + " остановил аукцион";
                endAuctionInternal();
                cancelAuctionTimer();
                break;
            case PAY_JAIL_FINE:
                if (!isCurrentPlayer(connection.getID())) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_NOT_YOUR_TURN,
                            "можно платить штраф только в свой ход");
                    return;
                }
                if (!actingPlayer.inJail) {
                    rejectAction(connection, actingPlayer, request.actionType, "INVALID_JAIL_STATE",
                            "игрок не находится в бане");
                    return;
                }
                actingPlayer.money -= JAIL_FINE;
                actingPlayer.inJail = false;
                actingPlayer.jailTurns = 0;
                actingPlayer.jailRollAttempts = 0;
                gameState.hasRolledThisTurn = false;
                gameState.lastActionLog = actingPlayer.name + " заплатил " + JAIL_FINE + " монет и вышел из Бана";
                notifyPlayers(actingPlayer.name + " выходит из Бана, заплатив " + JAIL_FINE + " монет");
                break;
            case PAY_DEBT:
                if (gameState.pendingDebtPlayerId != actingPlayer.id) {
                    rejectAction(connection, actingPlayer, request.actionType, "NO_DEBT",
                            "У вас нет активной задолженности");
                    return;
                }
                int debtToPay = gameState.pendingDebtAmount;
                if (actingPlayer.money < debtToPay) {
                    rejectAction(connection, actingPlayer, request.actionType, "INSUFFICIENT_FUNDS",
                            "Недостаточно монет для оплаты задолженности");
                    return;
                }
                actingPlayer.pay(debtToPay);
                if (gameState.pendingDebtRecipientId >= 0) {
                    Player recipient = gameState.getPlayerById(gameState.pendingDebtRecipientId);
                    if (recipient != null) {
                        recipient.receive(debtToPay);
                    }
                }
                String debtPaidMsg = actingPlayer.name + " оплатил(а) задолженность " + debtToPay + " монет!";
                gameState.lastActionLog = debtPaidMsg;
                notifyPlayers(debtPaidMsg);
                gameState.pendingDebtPlayerId = -1;
                gameState.pendingDebtAmount = 0;
                gameState.pendingDebtRecipientId = -1;
                broadcastGameStateUnsafe();
                break;
            case DECLARE_BANKRUPTCY:
                if (gameState.pendingDebtPlayerId == actingPlayer.id) {
                    declareBankruptcy(actingPlayer);
                }
                break;
            case END_TURN:
                if (!isCurrentPlayer(connection.getID())) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_NOT_YOUR_TURN,
                            "завершить ход может только текущий игрок");
                    return;
                }
                if (gameState.pendingDebtPlayerId == actingPlayer.id) {
                    rejectAction(connection, actingPlayer, request.actionType, "DEBT_UNPAID",
                            "Необходимо погасить задолженность перед завершением хода");
                    return;
                }
                if (gameState.moderatorChoicePending && gameState.moderatorPlayerId == actingPlayer.id) {
                    gameState.moderatorChoicePending = false;
                    gameState.moderatorPlayerId = -1;
                    gameState.lastActionLog = actingPlayer.name + " пропустил Ban (Модератор)";
                }
                if (gameState.awaitingReroll && gameState.rerollPlayerId == actingPlayer.id) {
                    handleCellLanding(actingPlayer, board.get(actingPlayer.position));
                    gameState.awaitingReroll = false;
                    gameState.rerollPlayerId = -1;
                }
                if (gameState.tradeId != 0 && gameState.tradeProposerId == actingPlayer.id) {
                    cancelTradeInternal();
                }
                if (gameState.hasRolledThisTurn) {
                    int oldId = gameState.getCurrentPlayer() != null ? gameState.getCurrentPlayer().id : -1;
                    gameState.nextPlayer();
                    Player newCurrent = gameState.getCurrentPlayer();
                    if (newCurrent != null && newCurrent.id != oldId) {
                        AppLog.info("Server",
                                "END_TURN: ход передан " + newCurrent.name + " (id=" + newCurrent.id + ")");
                    }
                }
                break;

            case SUBMIT_MEME: {
                System.out.println("\n[SERVER_LOG] Получен пакет SUBMIT_MEME от игрока ID: "
                        + (actingPlayer != null ? actingPlayer.id : "null"));

                if (gameState == null) {
                    System.out.println("[SERVER_LOG] Ошибка: gameState равен null!");
                    break;
                }

                System.out.println("[SERVER_LOG] Проверка условий:");
                System.out.println("  -> gameState.isInBattle: " + gameState.isInBattle);
                System.out.println("  -> gameState.currentPhase: " + gameState.currentPhase);

                if (gameState.battleParticipants != null && actingPlayer != null) {
                    System.out.println("  -> Игрок в списке участников баттла: "
                            + gameState.battleParticipants.contains(actingPlayer.id));
                } else {
                    System.out.println("  -> Ошибка: battleParticipants или actingPlayer равен null!");
                }

                if (gameState.isInBattle && actingPlayer != null) {

                    Meme newMeme = null;
                    System.out.println("[SERVER_LOG] Мемы в руке игрока " + actingPlayer.name + ":");
                    for (Meme m : actingPlayer.handMemes) {
                        System.out.println("  * Карту с ID: " + m.id);
                        if (m.id == request.targetId) {
                            newMeme = m;
                        }
                    }

                    if (newMeme == null) {
                        System.out.println("[SERVER_LOG] КРИТИЧЕСКАЯ ОШИБКА: Мем с запрошенным ID " + request.targetId
                                + " НЕ НАЙДЕН в руке игрока!");
                        break;
                    }

                    Meme previouslySubmitted = null;
                    for (Meme m : gameState.battleMemes) {
                        if (m.ownerId == actingPlayer.id) {
                            previouslySubmitted = m;
                            break;
                        }
                    }

                    if (previouslySubmitted != null) {
                        System.out.println("[SERVER_LOG] Игрок меняет выбор. Возвращаем мем ID "
                                + previouslySubmitted.id + " в руку.");
                        gameState.battleMemes.remove(previouslySubmitted);
                        actingPlayer.handMemes.add(previouslySubmitted);
                    } else {
                        System.out.println("[SERVER_LOG] Первый выбор мема. Берем карту из колоды.");
                        drawMemeForPlayer(actingPlayer);
                    }

                    actingPlayer.handMemes.remove(newMeme);
                    gameState.battleMemes.add(newMeme);
                    System.out.println("[SERVER_LOG] Успешно! Мем добавлен на стол. Всего мемов на столе: "
                            + gameState.battleMemes.size());

                    gameState.lastActionLog = actingPlayer.name + " выбрал мем для баттла";

                    if (gameState.battleOwnerId == actingPlayer.id
                            && request.data != null && !request.data.isBlank()) {
                        gameState.battleTopic = request.data.trim();
                    }

                    broadcastGameStateUnsafe();
                    battleManager.checkCollectingPhaseCompletion();
                } else {
                    System.out.println("[SERVER_LOG] Пакет отклонён: условия начала баттла не выполнены.");
                }
                break;
            }
            case VOTE_MEME:
                if (gameState.battlePhase == GameState.BattlePhase.VOTING
                        && gameState.containsMeme(request.targetId)) {
                    boolean isJury = gameState.juryIds != null && gameState.juryIds.contains(actingPlayer.id);
                    if (isJury) {
                        if (gameState.juryVotes == null)
                            gameState.juryVotes = new HashMap<>();
                        if (gameState.juryVoteCount == null)
                            gameState.juryVoteCount = new HashMap<>();
                        gameState.juryVotes.put(actingPlayer.id, request.targetId);
                        int count = gameState.juryVoteCount.getOrDefault(actingPlayer.id, 0) + 1;
                        gameState.juryVoteCount.put(actingPlayer.id, count);
                        if (gameState.battleVoters != null && !gameState.battleVoters.contains(actingPlayer.id)) {
                            gameState.battleVoters.add(actingPlayer.id);
                        }
                        gameState.lastActionLog = actingPlayer.name + " (Жюри) проголосовал в мем-баттле";
                        broadcastGameStateUnsafe();
                        battleManager.checkVotingPhaseCompletion();
                    } else if (!gameState.battleVoters.contains(actingPlayer.id)
                            && !gameState.isMemeOwnedBy(request.targetId, actingPlayer.id)) {
                        int currentVotes = gameState.votes.getOrDefault(request.targetId, 0);
                        gameState.votes.put(request.targetId, currentVotes + 1);
                        gameState.battleVoters.add(actingPlayer.id);
                        gameState.lastActionLog = actingPlayer.name + " проголосовал в мем-баттле";
                        broadcastGameStateUnsafe();
                        battleManager.checkVotingPhaseCompletion();
                    }
                }
                break;

            case BANKRUPT_EVENT_TARGET: {
                if (gameState.juryIds != null && gameState.juryIds.contains(actingPlayer.id)) {
                    boolean used = gameState.bankruptEventUsedRound.getOrDefault(actingPlayer.id, false);
                    if (!used && gameState.currentPhase == GameState.GamePhase.PLAYING
                            && !gameState.isInBattle && !gameState.isInAuction) {
                        Player target = gameState.getPlayerById(request.targetId);
                        if (target != null && !target.isBankrupt) {
                            EventCard eventCard = EventDeck.drawRandom();
                            applyEventCard(target, eventCard);
                            gameState.bankruptEventUsedRound.put(actingPlayer.id, true);
                            gameState.lastActionLog = "Таинственное событие обрушилось на " + target.name + ".";
                            notifyPlayers("Таинственное событие обрушилось на " + target.name + ".");
                            broadcastGameStateUnsafe();
                        }
                    }
                }
                break;
            }

            case APPEND_BATTLE_TOPIC: {
                if (gameState.battlePhase == GameState.BattlePhase.COLLECTING_MEMES
                        && actingPlayer.id == gameState.themeContinuatorId
                        && !gameState.themeContinuationUsed) {
                    if (request.data != null && !request.data.trim().isEmpty()) {
                        String appendText = request.data.trim();
                        if (appendText.length() > 60)
                            appendText = appendText.substring(0, 60);
                        String curTopic = gameState.battleTopic == null ? "" : gameState.battleTopic;
                        gameState.battleTopic = curTopic.isEmpty() ? appendText : curTopic + " … " + appendText;
                        gameState.themeContinuationUsed = true;
                        gameState.lastActionLog = actingPlayer.name + " дописал(а) тему.";
                        broadcastGameStateUnsafe();
                    }
                }
                break;
            }
            case STEAL_COINS: {
                if (roleOf(actingPlayer.id) != Role.SCAMMER)
                    break;
                if (!isCurrentPlayer(connection.getID())
                        || (gameState.currentPhase != GameState.GamePhase.PLAYING
                                && gameState.currentPhase != GameState.GamePhase.PLAYER_ACTION))
                    break;
                if (gameState.roleUsedThisRound.getOrDefault(actingPlayer.id, false))
                    break;

                Player target = null;
                for (Player p : gameState.players) {
                    if (p.id != actingPlayer.id && !p.isBankrupt && p.money > 0) {
                        if (target == null || p.money > target.money) {
                            target = p;
                        }
                    }
                }

                if (target == null) {
                    rejectAction(connection, actingPlayer, request.actionType, "NO_SUITABLE_TARGET",
                            "Нет подходящей цели для кражи");
                    break;
                }

                if (target.shields > 0) {
                    target.shields -= 1;
                    gameState.roleUsedThisRound.put(actingPlayer.id, true);
                    gameState.lastActionLog = actingPlayer.name + " попытался украсть 50 монет у " + target.name
                            + ", но щит поглотил кражу!";
                } else {
                    int amount = Math.min(50, target.money);
                    target.money -= amount;
                    actingPlayer.money += amount;
                    gameState.roleUsedThisRound.put(actingPlayer.id, true);
                    gameState.lastActionLog = actingPlayer.name + " украл " + amount + " монет у " + target.name + "!";
                }
                broadcastGameStateUnsafe();
                break;
            }
            case PLUS_TWO: {
                if (!gameState.awaitingReroll || gameState.rerollPlayerId != actingPlayer.id)
                    break;
                if (roleOf(actingPlayer.id) != Role.DOGE)
                    break;
                if (gameState.roleUsedThisRound.getOrDefault(actingPlayer.id, false))
                    break;
                if (actingPlayer.money < 10) {
                    rejectAction(connection, actingPlayer, request.actionType, "INSUFFICIENT_FUNDS",
                            "Недостаточно монет (нужно 10)");
                    break;
                }

                actingPlayer.money -= 10;
                int oldPos = actingPlayer.position;
                int newPos = (oldPos + 2) % board.size();

                if (newPos < oldPos) {
                    int startBonus = roleOf(actingPlayer.id) == Role.MEMOLOG ? 215 : 200;
                    actingPlayer.receive(startBonus);
                    gameState.lastActionLog = actingPlayer.name + " прошёл Старт и получил " + startBonus + "!";
                    for (Player p : gameState.players) {
                        if (p.memeBankBalance > 0) {
                            int percent = roleOf(p.id) == Role.SMM ? 20 : 10;
                            int interest = p.memeBankBalance * percent / 100;
                            p.memeBankBalance += interest;
                            gameState.lastActionLog += " | " + p.name + " получил " + interest + " % по вкладу";
                        }
                    }
                    gameState.roleUsedThisRound.put(actingPlayer.id, false);
                } else {
                    gameState.roleUsedThisRound.put(actingPlayer.id, true);
                }

                actingPlayer.position = newPos;
                handleCellLanding(actingPlayer, board.get(newPos));

                gameState.awaitingReroll = false;
                gameState.rerollPlayerId = -1;

                RollDiceResponse rr = new RollDiceResponse();
                rr.playerId = actingPlayer.id;
                rr.dice1 = 0;
                rr.dice2 = 0;
                rr.total = 2;
                rr.newPosition = newPos;
                sendAllTcpSafely(rr);
                break;
            }
            case CONFIRM_LANDING: {
                if (!gameState.awaitingReroll || gameState.rerollPlayerId != actingPlayer.id)
                    break;
                handleCellLanding(actingPlayer, board.get(actingPlayer.position));
                gameState.awaitingReroll = false;
                gameState.rerollPlayerId = -1;
                break;
            }
            case MODERATOR_SKIP_JAIL: {
                if (!gameState.moderatorChoicePending || gameState.moderatorPlayerId != actingPlayer.id)
                    break;
                gameState.moderatorChoicePending = false;
                gameState.moderatorPlayerId = -1;
                gameState.lastActionLog = actingPlayer.name + " решил пропустить Ban (Модератор)";
                break;
            }
            case MODERATOR_TAKE_JAIL: {
                if (!gameState.moderatorChoicePending || gameState.moderatorPlayerId != actingPlayer.id)
                    break;
                actingPlayer.inJail = true;
                actingPlayer.jailTurns = 0;
                actingPlayer.position = BAN_CELL_ID;
                actingPlayer.shields = Math.min(2, actingPlayer.shields + 1);
                gameState.moderatorChoicePending = false;
                gameState.moderatorPlayerId = -1;
                gameState.lastActionLog = actingPlayer.name + " отправлен в Ban и получил 1 щит (Модератор)";
                break;
            }
            default:
                return;
        }

        broadcastGameStateUnsafe();
    }

    private void startBattleTimer() {
        if (battleTimerFuture != null)
            return;
        if (isStopping || timerExecutor.isShutdown() || timerExecutor.isTerminated())
            return;
        lastBattlePhase = gameState.battlePhase;
        // BATTLE_SETUP даёт 60 секунд, остальные фазы управляются BattleManager
        gameState.battleTimerSeconds = 60;
        try {
            battleTimerFuture = timerExecutor.scheduleAtFixedRate(() -> {
                synchronized (stateLock) {
                    if (!gameState.isInBattle) {
                        stopBattleTimer();
                        return;
                    }
                    if (lastBattlePhase != gameState.battlePhase) {
                        // При смене фазы BattleManager сам устанавливает таймер;
                        // здесь просто обновляем отслеживаемую фазу, не трогаем счётчик
                        lastBattlePhase = gameState.battlePhase;
                    } else {
                        gameState.battleTimerSeconds--;
                    }
                    if (gameState.battleTimerSeconds <= 0
                            && gameState.battlePhase == GameState.BattlePhase.BATTLE_SETUP) {
                        battleManager.cancelSetup(gameState.battleOwnerId);
                        gameState.lastActionLog = "Мем-баттл отменён: организатор не выбрал ставку вовремя";
                        gameState.showNotification("Мем-баттл отменён по таймауту");
                    }
                    broadcastGameStateUnsafe();
                }
            }, 1, 1, TimeUnit.SECONDS);
        } catch (RejectedExecutionException ignored) {
            battleTimerFuture = null;
        }
    }

    private void stopBattleTimer() {
        if (battleTimerFuture != null) {
            battleTimerFuture.cancel(false);
            battleTimerFuture = null;
        }
        lastBattlePhase = null;
    }

    private void dealSelectedDeckMemes() {
        gameState.memeDeckDrawPile.clear();
        for (Player player : gameState.players) {
            player.handMemes.clear();
        }

        ArrayList<Meme> deckMemes = loadSelectedDeckMemes(gameState.selectedDeckName);
        if (deckMemes.isEmpty()) {
            return;
        }

        ArrayList<Meme> pool = new ArrayList<>();
        for (Meme meme : deckMemes) {
            pool.add(copyMemeForOwner(meme, -1));
        }
        Collections.shuffle(pool);

        for (Player player : gameState.players) {
            int handSize = roleOf(player.id) == Role.MEMOLOG ? 6 : 5;
            for (int card = 0; card < handSize; card++) {
                if (pool.isEmpty()) {
                    for (Meme meme : deckMemes) {
                        pool.add(copyMemeForOwner(meme, -1));
                    }
                    Collections.shuffle(pool);
                }
                Meme chosen = null;
                for (int i = 0; i < pool.size(); i++) {
                    Meme candidate = pool.get(i);
                    boolean duplicate = false;
                    for (Meme hm : player.handMemes) {
                        if (hm.imageUrl != null && hm.imageUrl.equals(candidate.imageUrl)) {
                            duplicate = true;
                            break;
                        }
                    }
                    if (!duplicate) {
                        chosen = pool.remove(i);
                        break;
                    }
                }
                if (chosen == null) {
                    chosen = pool.remove(0);
                }
                chosen.ownerId = player.id;
                player.handMemes.add(chosen);
            }
        }

        gameState.memeDeckDrawPile.clear();
        gameState.memeDeckDrawPile.addAll(pool);
    }

    private ArrayList<Meme> loadSelectedDeckMemes(String deckName) {
        ArrayList<Meme> memes = new ArrayList<>();
        if (deckName == null || deckName.isBlank()) {
            return memes;
        }

        for (MemeDeck deck : new DeckRepository().loadDecks()) {
            if (deck != null && deck.name != null && deck.name.equals(deckName) && deck.memes != null) {
                for (Meme meme : deck.memes) {
                    if (meme != null && meme.imageUrl != null && !meme.imageUrl.isBlank()) {
                        memes.add(meme);
                    }
                }
                break;
            }
        }
        return memes;
    }

    private Meme copyMemeForOwner(Meme source, int ownerId) {
        Meme copy = new Meme();
        copy.id = dealtMemeIdCounter.getAndDecrement();
        copy.imageUrl = source.imageUrl;
        copy.description = source.description == null || source.description.isBlank() ? "Мем" : source.description;
        copy.deckName = source.deckName;
        copy.ownerId = ownerId;
        return copy;
    }

    private void drawMemeForPlayer(Player player) {
        if (player == null || gameState.memeDeckDrawPile == null) {
            return;
        }
        if (gameState.memeDeckDrawPile.isEmpty()) {
            ArrayList<Meme> deckMemes = loadSelectedDeckMemes(gameState.selectedDeckName);
            for (Meme meme : deckMemes) {
                gameState.memeDeckDrawPile.add(copyMemeForOwner(meme, -1));
            }
            Collections.shuffle(gameState.memeDeckDrawPile);
        }
        if (gameState.memeDeckDrawPile.isEmpty()) {
            return;
        }
        Meme chosen = null;
        for (int i = 0; i < gameState.memeDeckDrawPile.size(); i++) {
            Meme candidate = gameState.memeDeckDrawPile.get(i);
            boolean duplicate = false;
            for (Meme handMeme : player.handMemes) {
                if (handMeme.imageUrl != null && handMeme.imageUrl.equals(candidate.imageUrl)) {
                    duplicate = true;
                    break;
                }
            }
            if (!duplicate) {
                chosen = gameState.memeDeckDrawPile.remove(i);
                break;
            }
        }
        if (chosen == null) {
            chosen = gameState.memeDeckDrawPile.remove(0);
        }
        chosen.ownerId = player.id;
        player.handMemes.add(chosen);
    }

    private boolean hasSubmittedBattleMeme(int playerId) {
        for (Meme meme : gameState.battleMemes) {
            if (meme.ownerId == playerId) {
                return true;
            }
        }
        return false;
    }

    private void rejectAction(Connection connection, Player player, GameActionRequest.ActionType actionType,
            String reasonCode, String reason) {
        if (player == null || reason == null || reason.isBlank()) {
            return;
        }
        gameState.lastActionLog = player.name + ": " + reason;
        broadcastGameStateUnsafe();
        gameStatePublisher.sendActionRejected(connection, actionType != null ? actionType.name() : "UNKNOWN",
                reasonCode, reason);
    }

    private void rejectBuyFlow(Player player, int reasonType) {
        if (player == null) {
            return;
        }
        String reasonCode = reasonType == 0 ? REJECT_NOT_YOUR_TURN : REJECT_INVALID_PHASE;
        String reason = reasonType == 0 ? "действие доступно только в свой ход" : "действие недоступно в текущей фазе";
        gameState.lastActionLog = player.name + ": " + reason;
    }

    private void handleMortgage(Player current, int cellId) {
        if (!current.ownedCells.contains(cellId)) {
            return;
        }
        if (gameState.cellMortgaged.getOrDefault(cellId, false)) {
            return;
        }
        if (gameState.cellHouses.getOrDefault(cellId, 0) > 0) {
            gameState.lastActionLog = current.name + " не может заложить клетку с постами";
            return;
        }

        BoardCell targetCell = board.get(cellId);
        current.receive(targetCell.price / 2);
        gameState.cellMortgaged.put(cellId, true);
        gameState.lastActionLog = current.name + " заложил " + targetCell.name;
        gameState.recordAchievement(current.id, "FIRST_MORTGAGE");
    }

    private void handleBuyBack(Player current, int cellId) {
        if (!current.ownedCells.contains(cellId)) {
            return;
        }
        if (!gameState.cellMortgaged.getOrDefault(cellId, false)) {
            return;
        }

        BoardCell targetCell = board.get(cellId);
        int buyBackCost = MonopolyUtils.getUnmortgagePrice(targetCell);
        if (current.money < buyBackCost) {
            return;
        }

        current.pay(buyBackCost);
        gameState.cellMortgaged.put(cellId, false);
        gameState.lastActionLog = current.name + " выкупил " + targetCell.name;
    }

    private void handleAuctionBid(Player current, int amount) {
        if (!gameState.isInAuction || gameState.currentPhase != GameState.GamePhase.AUCTION) {
            return;
        }

        int highestBid = 0;
        for (int bid : gameState.auctionBids.values()) {
            highestBid = Math.max(highestBid, bid);
        }

        if (amount < 10 || amount <= highestBid) {
            return;
        }

        current.maxAffordable = getMaxAffordable(current);
        if (current.maxAffordable < amount) {
            return;
        }

        gameState.auctionBids.put(current.id, amount);
        gameState.currentAuctionTime = 30;
        gameState.auctionCurrentPlayerId = findNextAuctionBidderId(current.id);
        gameState.lastActionLog = current.name + " поставил " + amount + " на аукционе. Ход: "
                + getPlayerName(gameState.auctionCurrentPlayerId);
    }

    private void startAuctionTimer() {
        auctionTimerService.start(() -> {
            synchronized (stateLock) {
                if (!gameState.isInAuction) {
                    auctionTimerService.cancel();
                    return;
                }

                gameState.currentAuctionTime--;
                if (gameState.currentAuctionTime <= 0) {
                    endAuctionInternal();
                    auctionTimerService.cancel();
                }
                broadcastGameStateUnsafe();
            }
        });
    }

    private void endAuctionInternal() {
        int winnerId = -1;
        int maxBid = 0;

        for (Map.Entry<Integer, Integer> entry : gameState.auctionBids.entrySet()) {
            if (entry.getValue() > maxBid) {
                maxBid = entry.getValue();
                winnerId = entry.getKey();
            }
        }

        BoardCell auctionCell = board.get(gameState.auctionCellId);
        if (winnerId == -1) {
            gameState.lastActionLog = "Аукцион за " + auctionCell.name + " завершён без ставок";
            gameState.endAuction();
            gameState.nextPlayer();
            return;
        }

        Player winner = gameState.getPlayerById(winnerId);
        if (winner == null) {
            gameState.lastActionLog = "Аукцион завершён без победителя";
            gameState.endAuction();
            gameState.nextPlayer();
            return;
        }

        winner.pay(maxBid);
        checkBankruptcy(winner, false);
        if (!winner.isBankrupt) {
            gameState.cellOwners.put(auctionCell.id, winner.id);
            winner.ownedCells.add(auctionCell.id);
            int fullGroups = com.memopoly.utils.MonopolyUtils.countFullGroups(winner, board);
            if (fullGroups >= 3) {
                gameState.recordAchievement(winner.id, "FULL_SET");
            }

            if (winner.ownedCells.size() >= 10) {
                gameState.recordAchievement(winner.id, "LANDLORD");
            }
            gameState.lastActionLog = winner.name + " выиграл аукцион и купил " + auctionCell.name;
        }
        gameState.endAuction();
        if (gameState.isGameOver()) {
            gameState.currentPhase = GameState.GamePhase.GAME_OVER;
        } else {
            gameState.nextPlayer();
        }
    }

    private boolean checkBankruptcy(Player player, boolean advanceTurn) {
        // Игрок не может быть банкротом пока у него неотрицательный баланс
        if (player.money >= 0) {
            return false;
        }

        // Проверяем, может ли игрок покрыть долг продажей активов
        int liquidValue = getMaxAffordable(player); // money + стоимость залога незаложенных клеток
        if (liquidValue >= 0) {
            // Есть активы — переходим в режим урегулирования, не банкротим
            gameState.currentPhase = GameState.GamePhase.PLAYER_ACTION;
            gameState.lastActionLog = player.name + " в долгу (" + player.money
                    + "). Заложи клетки или продай посты для урегулирования.";
            return false;
        }

        // Банкротство подтверждено: активов не хватает
        player.isBankrupt = true;

        // Register as jury member (spectator who can influence battles)
        long activePlayers = gameState.players.stream().filter(p -> !p.isBankrupt).count();
        if (activePlayers > 1) {
            if (gameState.juryIds == null)
                gameState.juryIds = new java.util.HashSet<>();
            if (gameState.juryOrder == null)
                gameState.juryOrder = new ArrayList<>();
            if (!gameState.juryIds.contains(player.id)) {
                gameState.juryIds.add(player.id);
            }
            if (!gameState.juryOrder.contains(player.id)) {
                gameState.juryOrder.add(player.id);
                // initialize themeContinuatorId to first jury member if not set
                if (gameState.themeContinuatorId <= 0) {
                    gameState.themeContinuatorId = player.id;
                }
            }
        }

        for (int cellIndex : player.ownedCells) {
            gameState.cellOwners.remove(cellIndex);
            gameState.cellMortgaged.remove(cellIndex);
            gameState.cellHouses.remove(cellIndex);
        }
        player.ownedCells.clear();

        if (gameState.isGameOver()) {
            gameState.currentPhase = GameState.GamePhase.GAME_OVER;
        } else if (advanceTurn) {
            gameState.nextPlayer();
        }
        return true;
    }

    private void declareBankruptcy(Player player) {
        if (player == null || player.isBankrupt) return;
        player.isBankrupt = true;

        if (gameState.pendingDebtRecipientId >= 0) {
            Player recipient = gameState.getPlayerById(gameState.pendingDebtRecipientId);
            if (recipient != null && player.money > 0) {
                recipient.receive(player.money);
            }
        }
        player.money = 0;

        long activePlayers = gameState.players.stream().filter(p -> !p.isBankrupt).count();
        if (activePlayers > 1) {
            if (gameState.juryIds == null)
                gameState.juryIds = new java.util.HashSet<>();
            if (gameState.juryOrder == null)
                gameState.juryOrder = new ArrayList<>();
            if (!gameState.juryIds.contains(player.id)) {
                gameState.juryIds.add(player.id);
            }
            if (!gameState.juryOrder.contains(player.id)) {
                gameState.juryOrder.add(player.id);
                if (gameState.themeContinuatorId <= 0) {
                    gameState.themeContinuatorId = player.id;
                }
            }
        }

        for (int cellIndex : player.ownedCells) {
            gameState.cellOwners.remove(cellIndex);
            gameState.cellMortgaged.remove(cellIndex);
            gameState.cellHouses.remove(cellIndex);
        }
        player.ownedCells.clear();

        gameState.pendingDebtPlayerId = -1;
        gameState.pendingDebtAmount = 0;
        gameState.pendingDebtRecipientId = -1;

        String bankruptMsg = player.name + " объявил(а) банкротство!";
        gameState.lastActionLog = bankruptMsg;
        notifyPlayers(bankruptMsg);

        if (gameState.isGameOver()) {
            gameState.currentPhase = GameState.GamePhase.GAME_OVER;
        } else {
            gameState.nextPlayer();
        }
        broadcastGameStateUnsafe();
    }

    private void removePlayer(int playerId) {
        if (gameState.pendingDebtPlayerId == playerId) {
            gameState.pendingDebtPlayerId = -1;
            gameState.pendingDebtAmount = 0;
            gameState.pendingDebtRecipientId = -1;
        }
        gameState.removePlayer(playerId);
        if (hostConnectionId == playerId) {
            hostConnectionId = gameState.players.isEmpty() ? -1 : gameState.players.get(0).id;
        }

        if (gameState.players.isEmpty()) {
            cancelAuctionTimer();
            return;
        }

        if (gameState.currentPlayerIndex >= gameState.players.size()) {
            gameState.currentPlayerIndex = 0;
        }

        if (gameState.isGameOver()) {
            gameState.currentPhase = GameState.GamePhase.GAME_OVER;
        }
    }

    private int getMaxAffordable(Player player) {
        int total = player.money;
        for (int cellIndex : player.ownedCells) {
            boolean mortgaged = gameState.cellMortgaged.getOrDefault(cellIndex, false);
            if (!mortgaged) {
                total += board.get(cellIndex).price / 2;
            }
        }
        return total;
    }

    /**
     * Rent rule: base entrance fee, doubled for an intact monopoly, then multiplied
     * by one plus the number of branches on the landed cell.
     */
    private int calculateRent(BoardCell cell) {
        return MonopolyUtils.calculateRent(cell, gameState, board);
    }

    private void handleBuyHouse(Connection connection, Player player, int cellId) {
        if (!canManageHouses(connection, player, cellId)) {
            return;
        }
        BoardCell cell = board.get(cellId);
        List<BoardCell> groupCells = BoardData.getCellsInGroup(board, cell.group);
        int houses = gameState.cellHouses.getOrDefault(cellId, 0);
        int minimumHouses = groupCells.stream()
                .mapToInt(groupCell -> gameState.cellHouses.getOrDefault(groupCell.id, 0)).min().orElse(0);
        int buildPrice = housePriceFor(cell, player);

        if (houses >= MAX_HOUSES_PER_CELL) {
            rejectAction(connection, player, GameActionRequest.ActionType.BUY_HOUSE, "HOUSE_LIMIT",
                    "на клетке уже максимум постов (построен Тренд)");
            return;
        } else if (houses != minimumHouses) {
            rejectAction(connection, player, GameActionRequest.ActionType.BUY_HOUSE, "UNEVEN_BUILDING",
                    "посты нужно строить равномерно по группе");
            return;
        } else if (player.money < buildPrice) {
            AppLog.warn("Server", "Игрок " + player.name + " не может купить пост: не хватает монет (" + player.money + " < " + buildPrice + ")");
            rejectAction(connection, player, GameActionRequest.ActionType.BUY_HOUSE, "INSUFFICIENT_FUNDS",
                    "недостаточно монет для покупки поста");
            return;
        } else {
            player.pay(buildPrice);
            gameState.cellHouses.put(cellId, houses + 1);
            gameState.lastActionLog = player.name + (houses + 1 == 5 ? " построил Тренд на " : " построил пост на ") + cell.name;
            gameState.recordAchievement(player.id, "FIRST_HOUSE");
            if (houses + 1 >= 4) {
                gameState.recordAchievement(player.id, "BUILDER");
            }
        }
    }

    private void handleSellHouse(Connection connection, Player player, int cellId) {
        if (!canManageHouses(connection, player, cellId)) {
            return;
        }
        BoardCell cell = board.get(cellId);
        List<BoardCell> groupCells = BoardData.getCellsInGroup(board, cell.group);
        int houses = gameState.cellHouses.getOrDefault(cellId, 0);
        int maximumHouses = groupCells.stream()
                .mapToInt(groupCell -> gameState.cellHouses.getOrDefault(groupCell.id, 0)).max().orElse(0);

        if (houses <= 0) {
            rejectAction(connection, player, GameActionRequest.ActionType.SELL_HOUSE, "NO_HOUSES",
                    "на клетке нет постов для продажи");
        } else if (houses != maximumHouses) {
            rejectAction(connection, player, GameActionRequest.ActionType.SELL_HOUSE, "UNEVEN_SELLING",
                    "посты нужно продавать равномерно по группе");
        } else {
            gameState.cellHouses.put(cellId, houses - 1);
            player.receive(housePriceFor(cell, player) / 2);
            gameState.lastActionLog = player.name + (houses == 5 ? " продал Тренд на " : " продал пост на ") + cell.name;
        }
    }

    private boolean canManageHouses(Connection connection, Player player, int cellId) {
        if (!isCurrentPlayer(connection.getID())) {
            rejectAction(connection, player, null, REJECT_NOT_YOUR_TURN, "управлять постами можно только в свой ход");
            return false;
        }
        if (gameState.currentPhase != GameState.GamePhase.PLAYING
                && gameState.currentPhase != GameState.GamePhase.PLAYER_ACTION) {
            rejectAction(connection, player, null, REJECT_INVALID_PHASE,
                    "управление постами доступно только во время хода");
            return false;
        }
        if (cellId < 0 || cellId >= board.size()) {
            return false;
        }
        BoardCell cell = board.get(cellId);
        if (!cell.isBuildableSituation() || !hasFullMonopoly(cell, player.id)) {
            rejectAction(connection, player, null, "NO_MONOPOLY", "нужна полная группа клеток без залогов");
            return false;
        }
        return true;
    }

    private boolean hasFullMonopoly(BoardCell cell, int ownerId) {
        if (cell == null || ownerId < 0 || !cell.isBuildableSituation()) {
            return false;
        }
        List<BoardCell> groupCells = BoardData.getCellsInGroup(board, cell.group);
        return groupCells.size() >= 2 && groupCells.stream()
                .allMatch(groupCell -> gameState.cellOwners.getOrDefault(groupCell.id, -1) == ownerId
                        && !gameState.cellMortgaged.getOrDefault(groupCell.id, false));
    }

    private int getHouseBuildPrice(BoardCell cell) {
        return Math.max(1, cell.price / 2);
    }

    private boolean isCurrentPlayer(int connectionId) {
        return TurnGuard.isCurrentPlayer(gameState, connectionId);
    }

    private boolean canUseMemeBank(int connectionId) {
        return PhaseGuard.canUseMemeBank(gameState, connectionId) && isCurrentPlayer(connectionId);
    }

    private void finishMemeBankAction() {
        gameState.currentPhase = GameState.GamePhase.PLAYING;
        gameState.memeBankPlayerId = -1;
    }

    private int findNextAuctionBidderId(int afterPlayerId) {
        if (gameState.players == null || gameState.players.isEmpty()) {
            return -1;
        }

        int startIndex = -1;
        for (int i = 0; i < gameState.players.size(); i++) {
            if (gameState.players.get(i).id == afterPlayerId) {
                startIndex = i;
                break;
            }
        }

        if (startIndex == -1) {
            return -1;
        }

        for (int offset = 1; offset <= gameState.players.size(); offset++) {
            Player candidate = gameState.players.get((startIndex + offset) % gameState.players.size());
            if (!candidate.isBankrupt) {
                return candidate.id;
            }
        }

        return afterPlayerId;
    }

    private String getPlayerName(int playerId) {
        Player player = gameState.getPlayerById(playerId);
        return player == null ? "—" : player.name;
    }

    private void broadcastGameStateUnsafe() {
        if (gameState != null && gameState.currentPhase != GameState.GamePhase.GAME_OVER && gameState.players != null
                && gameState.players.size() > 1) {
            int activeCount = 0;
            Player winnerCandidate = null;
            for (Player p : gameState.players) {
                if (!p.isBankrupt) {
                    activeCount++;
                    winnerCandidate = p;
                }
            }
            if (activeCount == 1 && winnerCandidate != null) {
                gameState.winnerId = winnerCandidate.id;
                gameState.isGameOver = true;
                gameState.currentPhase = GameState.GamePhase.GAME_OVER;
                gameState.lastActionLog = "Игра окончена! Победил " + winnerCandidate.name + "!";
                stopBattleTimer();
                cancelAuctionTimer();
            }
        }
        gameStatePublisher.broadcastState();
    }

    private void sendTcpSafely(Connection connection, Object packet) {
        try {
            connection.sendTCP(packet);
        } catch (Exception e) {
            AppLog.warn("Server", "sendTCP ERROR: packet=" + packet.getClass().getSimpleName() + ", connectionId="
                    + connection.getID() + ", reason=" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendAllTcpSafely(Object packet) {
        try {
            server.sendToAllTCP(packet);
        } catch (Exception e) {
            AppLog.warn("Server",
                    "sendToAllTCP ERROR: packet=" + packet.getClass().getSimpleName() + ", reason=" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cancelAuctionTimer() {
        auctionTimerService.cancel();
    }

    private static String findBestIP() {
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            while (interfaces.hasMoreElements()) {
                NetworkInterface iface = interfaces.nextElement();
                if (!iface.isUp() || iface.isLoopback()) {
                    continue;
                }

                Enumeration<InetAddress> addresses = iface.getInetAddresses();
                while (addresses.hasMoreElements()) {
                    InetAddress addr = addresses.nextElement();
                    if (addr instanceof Inet4Address) {
                        String ip = addr.getHostAddress();
                        if (ip.startsWith("26.")) {
                            return ip;
                        }
                    }
                }
            }
            return InetAddress.getLocalHost().getHostAddress();
        } catch (Exception e) {
            return "127.0.0.1";
        }
    }

    public void stop() {
        isStopping = true;
        for (ScheduledFuture<?> timer : disconnectTimers.values()) {
            if (timer != null)
                timer.cancel(false);
        }
        disconnectTimers.clear();
        cancelAuctionTimer();
        stopBattleTimer();
        try {
            timerExecutor.shutdownNow();
        } catch (Exception ignored) {
        }
        try {
            server.stop();
        } catch (Exception ignored) {
        }
    }

    private void notifyPlayers(String text) {
        gameState.showNotification(text);
    }

    private void applyEventCard(Player player, EventCard card) {
        if (player == null || card == null || card.effectType == null) {
            return;
        }

        player.maxAffordable = getMaxAffordable(player);
        switch (card.effectType) {
            case RECEIVE_MONEY:
            case RECEIVE_MONEY_LARGE:
                player.receive(card.amount);
                break;
            case PAY_MONEY:
                int payDue = card.amount;
                if (player.money < payDue) {
                    gameState.pendingDebtPlayerId = player.id;
                    gameState.pendingDebtAmount = payDue;
                    gameState.pendingDebtRecipientId = -1;
                } else {
                    player.pay(payDue);
                }
                break;
            case RECEIVE_PER_OWNED_CELL:
                player.receive(player.ownedCells.size() * card.amount);
                break;
            case PAY_PER_OWNED_CELL:
                int cellDue = player.ownedCells.size() * card.amount;
                if (cellDue > 0) {
                    if (player.money < cellDue) {
                        gameState.pendingDebtPlayerId = player.id;
                        gameState.pendingDebtAmount = cellDue;
                        gameState.pendingDebtRecipientId = -1;
                    } else {
                        player.pay(cellDue);
                    }
                }
                break;
            case SKIP_NEXT_RENT_COLLECTION:
                player.receive(card.amount);
                player.skipNextRentCollection = true;
                break;
            case SKIP_TURN:
                player.skipNextTurn = true;
                break;
            case EXTRA_ROLL:
                gameState.hasRolledThisTurn = false;
                gameState.currentPhase = GameState.GamePhase.PLAYING;
                break;
            case RETURN_TO_START:
                player.position = 0;
                gameState.currentPhase = GameState.GamePhase.PLAYING;
                break;
            case MARKET_CRASH:
                if (player.shields > 0 && player.memeBankBalance > 0) {
                    player.shields -= 1;
                    gameState.lastActionLog = player.name + " спас свой вклад в Meme Bank щитом!";
                } else {
                    player.memeBankBalance = 0;
                }
                break;
            case COLLECT_FROM_ALL:
                for (Player other : gameState.players) {
                    if (other.id != player.id && !other.isBankrupt) {
                        other.maxAffordable = getMaxAffordable(other);
                        other.pay(card.amount);
                        if (!other.isBankrupt) {
                            player.receive(card.amount);
                        }
                        checkBankruptcy(other, false);
                    }
                }
                break;
            case DRAW_MEME:
                drawMemeForPlayer(player);
                break;
            default:
                break;
        }
    }
}

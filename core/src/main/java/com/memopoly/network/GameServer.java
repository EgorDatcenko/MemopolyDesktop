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
import com.memopoly.utils.RoomCodeGenerator;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

import static com.memopoly.network.packets.GameActionRequest.ActionType.SUBMIT_MEME;
import static com.memopoly.network.packets.GameActionRequest.ActionType.VOTE_MEME;

/**
 * Авторитетный игровой сервер на KryoNet: принимает подключения, проверяет валидность действий, обновляет GameState и рассылает его игрокам.
 */
public class GameServer {
    private static final int TCP_PORT = 54555;
    private static final String REJECT_NOT_YOUR_TURN = "NOT_YOUR_TURN";
    private static final String REJECT_INVALID_PHASE = "INVALID_PHASE";
    private static final String REJECT_AUCTION_NOT_ACTIVE = "AUCTION_NOT_ACTIVE";
    private static final String REJECT_AUCTION_OTHER_PLAYER_TURN = "AUCTION_OTHER_PLAYER_TURN";
    private static final int JAIL_CELL_ID = 30;
    private static final int BAN_CELL_ID = 10;
    private static final int MAX_HOUSES_PER_CELL = 4;
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

    private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "memopoly-server-timer");
        thread.setDaemon(true);
        return thread;
    });

    private String hostIP;
    private String roomCode;
    private int hostConnectionId = -1;
    private final AtomicInteger dealtMemeIdCounter = new AtomicInteger(-1);

    public GameServer() {
        Log.set(Log.LEVEL_DEBUG);
        AppLog.info("Server", "Создаем GameServer...");

        server = new Server(65536, 65536);
        gameState = new GameState();
        auctionTimerService = new AuctionTimerService(timerExecutor);
        gameStatePublisher = new GameStatePublisher(gameState, this::sendAllTcpSafely, this::sendTcpSafely);
        battleManager = new BattleManager(gameState, board, gameStatePublisher::broadcastState, timerExecutor, stateLock);
        buyCellActionHandler = new BuyCellActionHandler(
            gameState,
            board,
            this::isCurrentPlayer,
            player -> player.maxAffordable = getMaxAffordable(player),
            this::rejectBuyFlow
        );
        passBuyActionHandler = new PassBuyActionHandler(
            gameState,
            board,
            this::isCurrentPlayer,
            player -> player.maxAffordable = getMaxAffordable(player),
            this::findNextAuctionBidderId,
            this::startAuctionTimer,
            this::rejectBuyFlow
        );
        mortgageCellActionHandler = new MortgageCellActionHandler(
            gameState,
            this::isCurrentPlayer,
            player -> player.maxAffordable = getMaxAffordable(player),
            this::handleMortgage
        );
        buyBackCellActionHandler = new BuyBackCellActionHandler(
            gameState,
            this::isCurrentPlayer,
            player -> player.maxAffordable = getMaxAffordable(player),
            this::handleBuyBack
        );
        memeBankActionHandler = new MemeBankActionHandler(
            gameState,
            this::canUseMemeBank,
            gameState::getCurrentPlayer,
            this::finishMemeBankAction
        );

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
            roomCode = RoomCodeGenerator.encodeIP(hostIP);
            AppLog.info("Server", "Сервер IP: " + hostIP);
            AppLog.info("Server", "Код комнаты: " + roomCode);
        } catch (Exception e) {
            hostIP = "127.0.0.1";
            roomCode = RoomCodeGenerator.encodeIP(hostIP);
            AppLog.warn("Server", "Не удалось получить IP, используем localhost");
            AppLog.info("Server", "Код комнаты: " + roomCode);
        }

        try {
            server.bind(TCP_PORT);
            AppLog.info("Server", "Сервер запущен на TCP-порту " + TCP_PORT);
        } catch (IOException e) {
            throw new IllegalStateException("Не удалось запустить сервер", e);
        }
    }

    private void registerPackets() {
        NetworkRegistry.register(server.getKryo());
    }

    private void startServer() {
        server.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                AppLog.info("Server", "Новый игрок подключился: id=" + connection.getID() + ", remote=" + connection.getRemoteAddressTCP());
            }

            @Override
            public void disconnected(Connection connection) {
                synchronized (stateLock) {
                    AppLog.info("Server", "Игрок отключился: id=" + connection.getID() + ", remote=" + connection.getRemoteAddressTCP());
                    removePlayer(connection.getID());
                    broadcastGameStateUnsafe();
                }
            }

            @Override
            public void received(Connection connection, Object object) {
                try {
                    synchronized (stateLock) {
                        handlePacket(connection, object);
                    }
                } catch (Exception e) {
                    AppLog.warn("Server", "Ошибка обработки пакета: type=" + object.getClass().getSimpleName() + ", connectionId=" + connection.getID() + ", reason=" + e.getMessage());
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

        AppLog.info("Server", "Получен пакет: " + packet.getClass().getSimpleName() + ", connectionId=" + connection.getID());

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
            if (proposer == null) return;

            // Валидация: инициатор — текущий игрок, фаза PLAYING, не в баттле/аукционе, нет активной сделки
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

            // Сделка должна содержать >= 1 клетку (проверено выше для myCells, но их клетки могут быть пустыми)
            if ((packet.myCells == null || packet.myCells.isEmpty()) && (packet.theirCells == null || packet.theirCells.isEmpty())) {
                AppLog.warn("Server", "Trade offer rejected: no cells at all");
                return;
            }

            // Сохраняем сделку
            gameState.tradeId = ++gameState.turnCount; // уникальный ID
            gameState.tradeProposerId = proposer.id;
            gameState.tradeTargetId = target.id;
            gameState.tradeProposerCells = new ArrayList<>(packet.myCells);
            gameState.tradeTargetCells = packet.theirCells != null ? new ArrayList<>(packet.theirCells) : new ArrayList<>();
            gameState.tradeProposerMoney = packet.myMoney;
            gameState.tradeTargetMoney = packet.theirMoney;

            gameState.lastActionLog = proposer.name + " предложил сделку игроку " + target.name;
            broadcastGameStateUnsafe();
        }
    }

    private void handleTradeResponse(Connection connection, TradeResponsePacket packet) {
        synchronized (stateLock) {
            Player target = gameState.getPlayerById(connection.getID());
            if (target == null) return;

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

    private void handleTradeCancel(Connection connection) {
        synchronized (stateLock) {
            Player proposer = gameState.getPlayerById(connection.getID());
            if (proposer == null) return;

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

    private void handleJoinRequest(Connection connection, JoinRoomRequest request) {
        String playerName = request != null ? request.playerName : null;

        if (playerName == null || playerName.trim().isEmpty()) {
            JoinRoomResponse reject = new JoinRoomResponse();
            reject.success = false;
            reject.playerId = -1;
            sendTcpSafely(connection, reject);
            return;
        }

        if (gameState.getPlayerById(connection.getID()) != null) {
            return;
        }

        Player newPlayer = new Player(connection.getID(), playerName.trim());
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

        if (gameState.players.size() < 2) {
            AppLog.warn("Server", "Нельзя запустить игру: нужно минимум 2 игрока");
            return;
        }

        gameState.selectedDeckName = request == null ? null : request.deckName;
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
        if (!isCurrentPlayer(connection.getID()) || gameState.currentPhase != GameState.GamePhase.PLAYING) {
            return;
        }

        int dice1 = ThreadLocalRandom.current().nextInt(1, 7);
        int dice2 = ThreadLocalRandom.current().nextInt(1, 7);
        int total = dice1 + dice2;

        Player current = gameState.getCurrentPlayer();
        if (current == null) {
            return;
        }

        gameState.diceValue = total;
        gameState.hasRolledThisTurn = true;
        current.maxAffordable = getMaxAffordable(current);

        // Handle jail logic: if player is in jail, roll dice to try to get out
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

        int oldPosition = current.position;
        current.position = (oldPosition + total) % board.size();
        gameState.lastActionLog = current.name + " бросил кубики: " + total;

        if (current.position < oldPosition) {
            current.receive(200);
            gameState.lastActionLog = current.name + " прошёл Старт и получил 200!";

            for (Player p : gameState.players) {
                if (p.memeBankBalance > 0) {
                    int interest = p.memeBankBalance / 10;
                    p.memeBankBalance += interest;
                    gameState.lastActionLog += " | " + p.name + " получил " + interest + " % по вкладу";
                }
            }
        }

        BoardCell cell = board.get(current.position);
        handleCellLanding(current, cell);

        RollDiceResponse response = new RollDiceResponse();
        response.playerId = connection.getID();
        response.dice1 = dice1;
        response.dice2 = dice2;
        response.total = total;
        response.newPosition = current.position;

        sendAllTcpSafely(response);
        broadcastGameStateUnsafe();
    }

    private void handleCellLanding(Player current, BoardCell cell) {
        if (cell == null) {
            return;
        }

        switch (cell.type) {
            case START:
            case REST:
                break;
            case JAIL:
                current.inJail = true;
                current.jailTurns = 0;
                // Ban is a relocation, not a normal landing: no cell effect or Start reward.
                current.position = BAN_CELL_ID;
                gameState.lastActionLog = current.name + " попал в тюрьму (Copyright Infringement) и отправлен в Ban";
                break;
            case TAX:
                current.pay(100);
                gameState.lastActionLog = current.name + " заплатил налог 100";
                checkBankruptcy(current, true);
                break;
            case EVENT:
                EventCard eventCard = EventDeck.drawRandom();
                applyEventCard(current, eventCard);
                notifyPlayers(current.name + ": " + eventCard.title + " — " + eventCard.description);
                break;
            case SITUATION:
                int cellIndex = cell.id;
                if (!gameState.cellOwners.containsKey(cellIndex)) {
                    gameState.currentPhase = GameState.GamePhase.PLAYER_ACTION;
                } else if (gameState.cellOwners.get(cellIndex) != current.id) {
                    boolean mortgaged = gameState.cellMortgaged.getOrDefault(cellIndex, false);
                    if (!mortgaged) {
                        int fee = calculateRent(cell);
                        current.pay(fee);
                        Player owner = gameState.getPlayerById(gameState.cellOwners.get(cellIndex));
                        if (owner != null) owner.receive(fee);
                        checkBankruptcy(current, true);
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
                break;
            default:
                break;
        }
    }

    /**
     * Handles dice roll while player is in jail.
     * - If doubles are rolled: player gets out of jail immediately and moves the dice total
     * - If not doubles: increment jailTurns counter
     * - After 3 failed attempts: player must pay fine or stays in jail
     */
    private void handleJailDiceRoll(Player current, int dice1, int dice2, int total) {
        if (current == null) return;

        boolean isDoubles = (dice1 == dice2);

        if (isDoubles) {
            // Got doubles! Get out of jail immediately and move
            current.inJail = false;
            current.jailTurns = 0;

            // Move player after getting out of jail
            int oldPosition = current.position;
            current.position = (oldPosition + total) % board.size();
            gameState.lastActionLog = current.name + " вышел из тюрьмы, бросив двойню! Движется на " + total + " клеток";

            // Check if passed Start
            if (current.position < oldPosition) {
                current.receive(200);
                gameState.lastActionLog += " и прошёл Старт, получив 200!";
                for (Player p : gameState.players) {
                    if (p.memeBankBalance > 0) {
                        int interest = p.memeBankBalance / 10;
                        p.memeBankBalance += interest;
                        gameState.lastActionLog += " | " + p.name + " получил " + interest + " % по вкладу";
                    }
                }
            }

            // Handle landing on cell
            BoardCell cell = board.get(current.position);
            handleCellLanding(current, cell);
        } else {
            // No doubles
            current.jailTurns++;
            gameState.lastActionLog = current.name + " бросил " + total + " в тюрьме (попытка " + current.jailTurns + "/3)";

            if (current.jailTurns >= MAX_JAIL_ATTEMPTS) {
                // After 3 failed attempts, must pay fine or stay in jail
                if (current.money >= JAIL_FINE) {
                    current.pay(JAIL_FINE);
                    current.inJail = false;
                    current.jailTurns = 0;
                    gameState.lastActionLog = current.name + " заплатил штраф " + JAIL_FINE + " и вышел из тюрьмы";
                    notifyPlayers(current.name + " заплатил " + JAIL_FINE + " за выход из тюрьмы");
                } else {
                    // Can't afford fine - must stay in jail until doubles or pay later
                    current.inJail = true;
                    gameState.lastActionLog = current.name + " не может позволить себе штраф и остаётся в тюрьме";
                    notifyPlayers(current.name + " не хватает " + (JAIL_FINE - current.money) + " для выхода из тюрьмы");
                }
            } else {
                // Still have attempts remaining
                gameState.lastActionLog = current.name + " остаётся в тюрьме (" + current.jailTurns + "/3 попыток)";
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
                        gameState.lastActionLog = "Начинается аукцион! Первый ход: " + getPlayerName(gameState.auctionCurrentPlayerId);
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
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_AUCTION_NOT_ACTIVE, "сейчас нет активного аукциона");
                    return;
                }
                if (!AuctionGuard.isCurrentAuctionBidder(gameState, actingPlayer.id)) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_AUCTION_OTHER_PLAYER_TURN, "сейчас ход другого участника аукциона");
                    return;
                }
                actingPlayer.maxAffordable = getMaxAffordable(actingPlayer);
                handleAuctionBid(actingPlayer, request.amount);
                break;
            case CANCEL_AUCTION:
                if (!AuctionGuard.isAuctionActive(gameState)) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_AUCTION_NOT_ACTIVE, "сейчас нет активного аукциона");
                    return;
                }
                if (!AuctionGuard.isCurrentAuctionBidder(gameState, actingPlayer.id)) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_AUCTION_OTHER_PLAYER_TURN, "сейчас ход другого участника аукциона");
                    return;
                }
                gameState.lastActionLog = actingPlayer.name + " остановил аукцион";
                endAuctionInternal();
                cancelAuctionTimer();
                break;
            case PAY_JAIL_FINE:
                if (!isCurrentPlayer(connection.getID())) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_NOT_YOUR_TURN, "можно платить штраф только в свой ход");
                    return;
                }
                if (!actingPlayer.inJail) {
                    rejectAction(connection, actingPlayer, request.actionType, "INVALID_JAIL_STATE", "игрок не находится в тюрьме");
                    return;
                }
                if (actingPlayer.money < JAIL_FINE) {
                    rejectAction(connection, actingPlayer, request.actionType, "INSUFFICIENT_FUNDS", "недостаточно монет для штрафа " + JAIL_FINE);
                    return;
                }
                actingPlayer.pay(JAIL_FINE);
                actingPlayer.inJail = false;
                actingPlayer.jailTurns = 0;
                gameState.hasRolledThisTurn = true;
                gameState.lastActionLog = actingPlayer.name + " заплатил " + JAIL_FINE + " и вышел из тюрьмы";
                notifyPlayers(actingPlayer.name + " заплатил " + JAIL_FINE + " за выход из тюрьмы");
                break;
            case END_TURN:
                if (!isCurrentPlayer(connection.getID())) {
                    rejectAction(connection, actingPlayer, request.actionType, REJECT_NOT_YOUR_TURN, "завершить ход может только текущий игрок");
                    return;
                }
                // Отмена активной сделки при завершении хода
                if (gameState.tradeId != 0 && gameState.tradeProposerId == actingPlayer.id) {
                    cancelTradeInternal();
                }
                if (gameState.currentPhase == GameState.GamePhase.PLAYING && gameState.hasRolledThisTurn) {
                    gameState.nextPlayer();
                }
                break;

            case SUBMIT_MEME: {
                System.out.println("\n[SERVER_LOG] Получен пакет SUBMIT_MEME от игрока ID: " + (actingPlayer != null ? actingPlayer.id : "null"));

                if (gameState == null) {
                    System.out.println("[SERVER_LOG] Ошибка: gameState равен null!");
                    break;
                }

                System.out.println("[SERVER_LOG] Проверка условий:");
                System.out.println("  -> gameState.isInBattle: " + gameState.isInBattle);
                System.out.println("  -> gameState.currentPhase: " + gameState.currentPhase);

                if (gameState.battleParticipants != null && actingPlayer != null) {
                    System.out.println("  -> Игрок в списке участников баттла: " + gameState.battleParticipants.contains(actingPlayer.id));
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
                        System.out.println("[SERVER_LOG] КРИТИЧЕСКАЯ ОШИБКА: Мем с запрошенным ID " + request.targetId + " НЕ НАЙДЕН в руке игрока!");
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
                        System.out.println("[SERVER_LOG] Игрок меняет выбор. Возвращаем мем ID " + previouslySubmitted.id + " в руку.");
                        gameState.battleMemes.remove(previouslySubmitted);
                        actingPlayer.handMemes.add(previouslySubmitted);
                    } else {
                        System.out.println("[SERVER_LOG] Первый выбор мема. Берем карту из колоды.");
                        drawMemeForPlayer(actingPlayer);
                    }

                    actingPlayer.handMemes.remove(newMeme);
                    gameState.battleMemes.add(newMeme);
                    System.out.println("[SERVER_LOG] Успешно! Мем добавлен на стол. Всего мемов на столе: " + gameState.battleMemes.size());

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
                    && gameState.containsMeme(request.targetId)
                    && !gameState.battleVoters.contains(actingPlayer.id)
                    && !gameState.isMemeOwnedBy(request.targetId, actingPlayer.id)) {
                    int currentVotes = gameState.votes.getOrDefault(request.targetId, 0);
                    gameState.votes.put(request.targetId, currentVotes + 1);
                    gameState.battleVoters.add(actingPlayer.id);
                    gameState.lastActionLog = actingPlayer.name + " проголосовал в мем-баттле";
                    broadcastGameStateUnsafe();
                    battleManager.checkVotingPhaseCompletion();
                }
                break;
            default:
                return;
        }

        broadcastGameStateUnsafe();
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

        int cardsNeeded = gameState.players.size() * 5;
        ArrayList<Meme> dealPile = new ArrayList<>();
        for (int i = 0; i < cardsNeeded; i++) {
            Meme template = deckMemes.get(i % deckMemes.size());
            dealPile.add(copyMemeForOwner(template, -1));
        }
        Collections.shuffle(dealPile);

        int index = 0;
        for (Player player : gameState.players) {
            for (int card = 0; card < 5; card++) {
                Meme meme = dealPile.get(index++);
                meme.ownerId = player.id;
                player.handMemes.add(meme);
            }
        }

        ArrayList<Meme> drawPile = new ArrayList<>();
        for (Meme meme : deckMemes) {
            drawPile.add(copyMemeForOwner(meme, -1));
        }
        Collections.shuffle(drawPile);
        gameState.memeDeckDrawPile.addAll(drawPile);
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
        Meme meme = gameState.memeDeckDrawPile.remove(0);
        meme.ownerId = player.id;
        player.handMemes.add(meme);
    }

    private boolean hasSubmittedBattleMeme(int playerId) {
        for (Meme meme : gameState.battleMemes) {
            if (meme.ownerId == playerId) {
                return true;
            }
        }
        return false;
    }

    private void rejectAction(Connection connection, Player player, GameActionRequest.ActionType actionType, String reasonCode, String reason) {
        if (player == null || reason == null || reason.isBlank()) {
            return;
        }
        gameState.lastActionLog = player.name + ": " + reason;
        broadcastGameStateUnsafe();
        gameStatePublisher.sendActionRejected(connection, actionType != null ? actionType.name() : "UNKNOWN", reasonCode, reason);
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
            gameState.lastActionLog = current.name + " не может заложить клетку с филиалами";
            return;
        }

        BoardCell targetCell = board.get(cellId);
        current.receive(targetCell.price / 2);
        gameState.cellMortgaged.put(cellId, true);
        gameState.lastActionLog = current.name + " заложил " + targetCell.name;
    }

    private void handleBuyBack(Player current, int cellId) {
        if (!current.ownedCells.contains(cellId)) {
            return;
        }
        if (!gameState.cellMortgaged.getOrDefault(cellId, false)) {
            return;
        }

        BoardCell targetCell = board.get(cellId);
        int buyBackCost = targetCell.price / 2;
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
        gameState.lastActionLog = current.name + " поставил " + amount + " на аукционе. Ход: " + getPlayerName(gameState.auctionCurrentPlayerId);
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
        if (!player.isBankrupt) {
            return false;
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

    private void removePlayer(int playerId) {
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
        int baseFee = cell.getEntranceFee();
        if (baseFee == 0) {
            return 0;
        }
        int monopolyMultiplier = hasFullMonopoly(cell, gameState.cellOwners.get(cell.id)) ? 2 : 1;
        int houses = gameState.cellHouses.getOrDefault(cell.id, 0);
        return baseFee * monopolyMultiplier * (1 + houses);
    }

    private void handleBuyHouse(Connection connection, Player player, int cellId) {
        if (!canManageHouses(connection, player, cellId)) {
            return;
        }
        BoardCell cell = board.get(cellId);
        List<BoardCell> groupCells = BoardData.getCellsInGroup(board, cell.group);
        int houses = gameState.cellHouses.getOrDefault(cellId, 0);
        int minimumHouses = groupCells.stream().mapToInt(groupCell -> gameState.cellHouses.getOrDefault(groupCell.id, 0)).min().orElse(0);
        int buildPrice = getHouseBuildPrice(cell);

        if (houses >= MAX_HOUSES_PER_CELL) {
            rejectAction(connection, player, GameActionRequest.ActionType.BUY_HOUSE, "HOUSE_LIMIT", "на клетке уже максимум филиалов");
        } else if (houses != minimumHouses) {
            rejectAction(connection, player, GameActionRequest.ActionType.BUY_HOUSE, "UNEVEN_BUILDING", "филиалы нужно строить равномерно по группе");
        } else if (player.money < buildPrice) {
            rejectAction(connection, player, GameActionRequest.ActionType.BUY_HOUSE, "INSUFFICIENT_FUNDS", "недостаточно монет для филиала");
        } else {
            player.pay(buildPrice);
            gameState.cellHouses.put(cellId, houses + 1);
            gameState.lastActionLog = player.name + " построил филиал на " + cell.name;
        }
    }

    private void handleSellHouse(Connection connection, Player player, int cellId) {
        if (!canManageHouses(connection, player, cellId)) {
            return;
        }
        BoardCell cell = board.get(cellId);
        List<BoardCell> groupCells = BoardData.getCellsInGroup(board, cell.group);
        int houses = gameState.cellHouses.getOrDefault(cellId, 0);
        int maximumHouses = groupCells.stream().mapToInt(groupCell -> gameState.cellHouses.getOrDefault(groupCell.id, 0)).max().orElse(0);

        if (houses <= 0) {
            rejectAction(connection, player, GameActionRequest.ActionType.SELL_HOUSE, "NO_HOUSES", "на клетке нет филиалов для продажи");
        } else if (houses != maximumHouses) {
            rejectAction(connection, player, GameActionRequest.ActionType.SELL_HOUSE, "UNEVEN_SELLING", "филиалы нужно продавать равномерно по группе");
        } else {
            gameState.cellHouses.put(cellId, houses - 1);
            player.receive(getHouseBuildPrice(cell) / 2);
            gameState.lastActionLog = player.name + " продал филиал на " + cell.name;
        }
    }

    private boolean canManageHouses(Connection connection, Player player, int cellId) {
        if (!isCurrentPlayer(connection.getID())) {
            rejectAction(connection, player, null, REJECT_NOT_YOUR_TURN, "управлять филиалами можно только в свой ход");
            return false;
        }
        if (gameState.currentPhase != GameState.GamePhase.PLAYING) {
            rejectAction(connection, player, null, REJECT_INVALID_PHASE, "управление филиалами доступно только во время хода");
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
        return groupCells.size() >= 2 && groupCells.stream().allMatch(groupCell ->
            gameState.cellOwners.getOrDefault(groupCell.id, -1) == ownerId
                && !gameState.cellMortgaged.getOrDefault(groupCell.id, false)
        );
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
        gameStatePublisher.broadcastState();
    }

    private void sendTcpSafely(Connection connection, Object packet) {
        try {
            connection.sendTCP(packet);
        } catch (Exception e) {
            AppLog.warn("Server", "sendTCP ERROR: packet=" + packet.getClass().getSimpleName() + ", connectionId=" + connection.getID() + ", reason=" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendAllTcpSafely(Object packet) {
        try {
            server.sendToAllTCP(packet);
        } catch (Exception e) {
            AppLog.warn("Server", "sendToAllTCP ERROR: packet=" + packet.getClass().getSimpleName() + ", reason=" + e.getMessage());
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
        cancelAuctionTimer();
        timerExecutor.shutdownNow();
        server.stop();
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
                player.pay(card.amount);
                checkBankruptcy(player, true);
                break;
            case RECEIVE_PER_OWNED_CELL:
                player.receive(player.ownedCells.size() * card.amount);
                break;
            case PAY_PER_OWNED_CELL:
                player.pay(player.ownedCells.size() * card.amount);
                checkBankruptcy(player, true);
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
                player.memeBankBalance = 0;
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

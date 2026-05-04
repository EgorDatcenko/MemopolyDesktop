package com.memopoly.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.BoardData;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.GameActionRequest;
import com.memopoly.network.packets.GameStatePacket;
import com.memopoly.network.packets.JoinRoomRequest;
import com.memopoly.network.packets.JoinRoomResponse;
import com.memopoly.network.packets.RollDiceRequest;
import com.memopoly.network.packets.RollDiceResponse;
import com.memopoly.network.packets.StartGameRequest;
import com.memopoly.utils.RoomCodeGenerator;

import java.io.IOException;
import java.net.Inet4Address;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public class GameServer {
    private static final int TCP_PORT = 54555;

    private final Server server;
    private final GameState gameState;
    private final List<BoardCell> board = BoardData.buildCells();
    private final Object stateLock = new Object();
    private final ScheduledExecutorService timerExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread thread = new Thread(r, "memopoly-server-timer");
        thread.setDaemon(true);
        return thread;
    });

    private String hostIP;
    private String roomCode;
    private int hostConnectionId = -1;
    private ScheduledFuture<?> auctionTask;

    public GameServer() {
        Log.set(Log.LEVEL_DEBUG);
        System.out.println("Создаем GameServer...");

        server = new Server(65536, 65536);
        gameState = new GameState();

        registerPackets();
        setupServer();
        startServer();

        System.out.println("GameServer готов!");
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
            System.out.println("Сервер IP: " + hostIP);
            System.out.println("Код комнаты: " + roomCode);
        } catch (Exception e) {
            hostIP = "127.0.0.1";
            roomCode = RoomCodeGenerator.encodeIP(hostIP);
            System.out.println("Не удалось получить IP, используем localhost");
            System.out.println("Код комнаты: " + roomCode);
        }

        try {
            server.bind(TCP_PORT);
            System.out.println("Сервер запущен на TCP-порту " + TCP_PORT);
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
                System.out.println("Новый игрок подключился: id=" + connection.getID() + ", remote=" + connection.getRemoteAddressTCP());
            }

            @Override
            public void disconnected(Connection connection) {
                synchronized (stateLock) {
                    System.out.println("Игрок отключился: id=" + connection.getID() + ", remote=" + connection.getRemoteAddressTCP());
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
                    System.err.println("Ошибка обработки пакета: type=" + object.getClass().getSimpleName() + ", connectionId=" + connection.getID() + ", reason=" + e.getMessage());
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

        System.out.println("Получен пакет: " + packet.getClass().getSimpleName() + ", connectionId=" + connection.getID());

        if (packet instanceof JoinRoomRequest) {
            handleJoinRequest(connection, (JoinRoomRequest) packet);
        } else if (packet instanceof RollDiceRequest) {
            handleRollDice(connection);
        } else if (packet instanceof StartGameRequest) {
            handleStartGame(connection);
        } else if (packet instanceof GameActionRequest) {
            handleGameAction(connection, (GameActionRequest) packet);
        } else {
            System.out.println("Неизвестный тип пакета: " + packet.getClass());
        }
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

    private void handleStartGame(Connection connection) {
        if (connection.getID() != hostConnectionId) {
            return;
        }

        if (gameState.players.size() < 2) {
            System.out.println("Нельзя запустить игру: нужно минимум 2 игрока");
            return;
        }

        gameState.currentPhase = GameState.GamePhase.PLAYING;
        gameState.lastActionLog = "Игра началась";
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

        int dice1 = (int) (Math.random() * 6) + 1;
        int dice2 = (int) (Math.random() * 6) + 1;
        int total = dice1 + dice2;

        Player current = gameState.getCurrentPlayer();
        if (current == null) {
            return;
        }

        gameState.diceValue = total;
        gameState.hasRolledThisTurn = true;
        current.maxAffordable = getMaxAffordable(current);

        int oldPosition = current.position;
        current.position = (oldPosition + total) % board.size();
        gameState.lastActionLog = current.name + " бросил кубики: " + total;

        if (current.position < oldPosition) {
            current.receive(200);
            gameState.lastActionLog = current.name + " прошёл Старт и получил 200!";
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
            case JAIL:
                break;
            case TAX:
                current.pay(100);
                gameState.lastActionLog = current.name + " заплатил налог 100";
                checkBankruptcy(current, true);
                break;
            case SITUATION:
                int cellIndex = cell.id;
                if (!gameState.cellOwners.containsKey(cellIndex)) {
                    gameState.currentPhase = GameState.GamePhase.PLAYER_ACTION;
                    gameState.lastActionLog = current.name + " попал на " + cell.name;
                } else if (gameState.cellOwners.get(cellIndex) != current.id) {
                    boolean mortgaged = gameState.cellMortgaged.getOrDefault(cellIndex, false);
                    if (!mortgaged) {
                        int fee = cell.getEntranceFee();
                        current.pay(fee);
                        Player owner = gameState.getPlayerById(gameState.cellOwners.get(cellIndex));
                        if (owner != null) {
                            owner.receive(fee);
                        }
                        gameState.lastActionLog = current.name + " заплатил " + fee + " игроку " + (owner != null ? owner.name : "владельцу");
                        checkBankruptcy(current, true);
                    }
                }
                break;
            case MEME_BANK:
                gameState.currentPhase = GameState.GamePhase.MEME_BANK_ACTION;
                gameState.memeBankPlayerId = current.id;
                gameState.lastActionLog = current.name + " попал на Meme Bank";
                break;
            default:
                break;
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
            case BUY_CELL:
                if (!isCurrentPlayer(connection.getID())) {
                    return;
                }
                Player current = gameState.getCurrentPlayer();
                if (current == null) {
                    return;
                }
                BoardCell currentCell = board.get(current.position);
                current.maxAffordable = getMaxAffordable(current);
                if (gameState.currentPhase != GameState.GamePhase.PLAYER_ACTION || currentCell.type != BoardCell.Type.SITUATION) {
                    return;
                }
                if (!current.canAfford(currentCell.price)) {
                    gameState.lastActionLog = current.name + " не может купить — недостаточно средств";
                    broadcastGameStateUnsafe();
                    return;
                }
                current.pay(currentCell.price);
                gameState.cellOwners.put(currentCell.id, current.id);
                current.ownedCells.add(currentCell.id);
                gameState.lastActionLog = current.name + " купил " + currentCell.name;
                gameState.currentPhase = GameState.GamePhase.PLAYING;
                break;
            case PASS_BUY:
                if (!isCurrentPlayer(connection.getID())) {
                    return;
                }
                current = gameState.getCurrentPlayer();
                if (current == null) {
                    return;
                }
                currentCell = board.get(current.position);
                current.maxAffordable = getMaxAffordable(current);
                if (gameState.currentPhase != GameState.GamePhase.PLAYER_ACTION || currentCell.type != BoardCell.Type.SITUATION) {
                    return;
                }
                gameState.startAuction(currentCell.id);
                gameState.auctionStarterPlayerId = current.id;
                gameState.auctionCurrentPlayerId = findNextAuctionBidderId(current.id);
                gameState.lastActionLog = "Начинается аукцион! Первый ход: " + getPlayerName(gameState.auctionCurrentPlayerId);
                startAuctionTimer();
                break;
            case MORTGAGE_CELL:
                if (!isCurrentPlayer(connection.getID()) || gameState.currentPhase == GameState.GamePhase.AUCTION || gameState.currentPhase == GameState.GamePhase.MEME_BATTLE) {
                    return;
                }
                actingPlayer.maxAffordable = getMaxAffordable(actingPlayer);
                handleMortgage(actingPlayer, request.targetId);
                break;
            case BUY_BACK_CELL:
                if (!isCurrentPlayer(connection.getID()) || gameState.currentPhase == GameState.GamePhase.AUCTION || gameState.currentPhase == GameState.GamePhase.MEME_BATTLE) {
                    return;
                }
                actingPlayer.maxAffordable = getMaxAffordable(actingPlayer);
                handleBuyBack(actingPlayer, request.targetId);
                break;
            case MEME_BANK_DEPOSIT:
                if (!canUseMemeBank(connection.getID())) {
                    return;
                }
                current = gameState.getCurrentPlayer();
                if (current == null) {
                    return;
                }
                if (request.amount <= 0 || request.amount > 500) {
                    gameState.lastActionLog = current.name + " не может внести такую сумму в Meme Bank";
                    break;
                }
                if (request.amount > current.money) {
                    gameState.lastActionLog = current.name + " не хватает наличных для вклада в Meme Bank";
                    break;
                }
                current.money -= request.amount;
                current.memeBankBalance += request.amount;
                gameState.lastActionLog = current.name + " внёс " + request.amount + " в Meme Bank";
                finishMemeBankAction();
                break;
            case MEME_BANK_WITHDRAW:
                if (!canUseMemeBank(connection.getID())) {
                    return;
                }
                current = gameState.getCurrentPlayer();
                if (current == null) {
                    return;
                }
                if (current.memeBankBalance <= 0) {
                    gameState.lastActionLog = current.name + " попытался снять деньги из пустого Meme Bank";
                    break;
                }
                int withdrawnAmount = current.memeBankBalance;
                current.money += withdrawnAmount;
                current.memeBankBalance = 0;
                gameState.lastActionLog = current.name + " снял " + withdrawnAmount + " из Meme Bank";
                finishMemeBankAction();
                break;
            case MEME_BANK_SKIP:
                if (!canUseMemeBank(connection.getID())) {
                    return;
                }
                current = gameState.getCurrentPlayer();
                if (current != null) {
                    gameState.lastActionLog = current.name + " пропустил действие на Meme Bank";
                }
                finishMemeBankAction();
                break;
            case PLACE_AUCTION_BID:
                if (gameState.currentPhase != GameState.GamePhase.AUCTION || !gameState.isInAuction) {
                    return;
                }
                if (actingPlayer.id != gameState.auctionCurrentPlayerId) {
                    return;
                }
                actingPlayer.maxAffordable = getMaxAffordable(actingPlayer);
                handleAuctionBid(actingPlayer, request.amount);
                break;
            case END_TURN:
                if (!isCurrentPlayer(connection.getID())) {
                    return;
                }
                if (gameState.currentPhase == GameState.GamePhase.PLAYING && gameState.hasRolledThisTurn) {
                    gameState.nextPlayer();
                }
                break;
            default:
                return;
        }

        broadcastGameStateUnsafe();
    }

    private void handleMortgage(Player current, int cellId) {
        if (!current.ownedCells.contains(cellId)) {
            return;
        }
        if (gameState.cellMortgaged.getOrDefault(cellId, false)) {
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
        cancelAuctionTimer();
        auctionTask = timerExecutor.scheduleAtFixedRate(() -> {
            synchronized (stateLock) {
                if (!gameState.isInAuction) {
                    cancelAuctionTimer();
                    return;
                }

                gameState.currentAuctionTime--;
                if (gameState.currentAuctionTime <= 0) {
                    endAuctionInternal();
                    cancelAuctionTimer();
                }
                broadcastGameStateUnsafe();
            }
        }, 1, 1, TimeUnit.SECONDS);
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

    private boolean isCurrentPlayer(int connectionId) {
        return gameState.players != null
            && !gameState.players.isEmpty()
            && gameState.currentPlayerIndex >= 0
            && gameState.currentPlayerIndex < gameState.players.size()
            && gameState.players.get(gameState.currentPlayerIndex).id == connectionId;
    }

    private boolean canUseMemeBank(int connectionId) {
        return gameState.currentPhase == GameState.GamePhase.MEME_BANK_ACTION
            && gameState.memeBankPlayerId == connectionId
            && isCurrentPlayer(connectionId);
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
        GameStatePacket packet = new GameStatePacket(gameState);
        sendAllTcpSafely(packet);
    }

    private void sendTcpSafely(Connection connection, Object packet) {
        try {
            connection.sendTCP(packet);
        } catch (Exception e) {
            System.err.println("sendTCP ERROR: packet=" + packet.getClass().getSimpleName() + ", connectionId=" + connection.getID() + ", reason=" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendAllTcpSafely(Object packet) {
        try {
            server.sendToAllTCP(packet);
        } catch (Exception e) {
            System.err.println("sendToAllTCP ERROR: packet=" + packet.getClass().getSimpleName() + ", reason=" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void cancelAuctionTimer() {
        if (auctionTask != null) {
            auctionTask.cancel(false);
            auctionTask = null;
        }
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
}

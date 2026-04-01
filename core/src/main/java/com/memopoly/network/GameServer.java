package com.memopoly.network;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import com.esotericsoftware.minlog.Log;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.BoardData;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.CreateRoomRequest;
import com.memopoly.network.packets.CreateRoomResponse;
import com.memopoly.network.packets.GameStatePacket;
import com.memopoly.network.packets.JoinRoomRequest;
import com.memopoly.network.packets.JoinRoomResponse;
import com.memopoly.network.packets.RollDiceRequest;
import com.memopoly.network.packets.RollDiceResponse;
import com.memopoly.network.packets.StartGameRequest;
import com.memopoly.utils.RoomCodeGenerator;

import java.io.IOException;
import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class GameServer {
    private final Server server;
    private final GameState gameState;
    private String hostIP;
    private String roomCode;
    private final List<BoardCell> board = BoardData.buildCells();

    public GameServer() {
        Log.set(Log.LEVEL_DEBUG);
        System.out.println("🔧 Создаем GameServer...");

        server = new Server(65536, 65536);
        gameState = new GameState();

        System.out.println("🔧 Настраиваем сервер...");
        registerPackets();
        setupServer();
        startServer();

        System.out.println("🔧 GameServer готов!");
    }
    public BoardCell getCurrentCell() {
        Player current = gameState.getCurrentPlayer();
        if (current == null) return null;
        return board.get(current.position);
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
            hostIP = InetAddress.getLocalHost().getHostAddress();
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
            server.bind(54555, 54777);
            System.out.println("Сервер запущен на портах 54555/54777");
        } catch (IOException e) {
            System.err.println("Ошибка запуска сервера: " + e.getMessage());
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
                System.out.println("Игрок отключился: id=" + connection.getID() + ", remote=" + connection.getRemoteAddressTCP());
                removePlayer(connection.getID());
            }

            @Override
            public void received(Connection connection, Object object) {
                try {
                    handlePacket(connection, object);
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
            handleRollDice(connection, (RollDiceRequest) packet);
        } else if (packet instanceof StartGameRequest) {
            startGame();
        } else {
            System.out.println("Неизвестный тип пакета: " + packet.getClass());
        }
    }

    private void handleJoinRequest(Connection connection, JoinRoomRequest request) {
        String playerName = request != null ? request.playerName : null;

        if (playerName == null || playerName.trim().isEmpty()) {
            System.out.println("Отклоняем JoinRoomRequest: пустое имя, connectionId=" + connection.getID());

            JoinRoomResponse reject = new JoinRoomResponse();
            reject.success = false;
            reject.playerId = -1;
            sendTcpSafely(connection, reject);
            return;
        }

        String normalizedName = playerName.trim();
        System.out.println("Получен JoinRoomRequest от connectionId=" + connection.getID() + ", playerName=" + normalizedName);

        Player newPlayer = new Player(connection.getID(), normalizedName);
        gameState.addPlayer(newPlayer);

        System.out.println("Игрок добавлен в игру. Всего игроков: " + gameState.players.size());

        JoinRoomResponse response = new JoinRoomResponse();
        response.success = true;
        response.playerId = connection.getID();

        sendTcpSafely(connection, response);
        broadcastGameState();
    }

    private void handleRollDice(Connection connection, RollDiceRequest request) {
        if (gameState.players != null && !gameState.players.isEmpty()
            && gameState.currentPlayerIndex >= 0
            && gameState.currentPlayerIndex < gameState.players.size()
            && gameState.players.get(gameState.currentPlayerIndex).id == connection.getID()) {

            int dice1 = (int) (Math.random() * 6) + 1;
            int dice2 = (int) (Math.random() * 6) + 1;
            int total = dice1 + dice2;

            gameState.diceValue = total;
            gameState.lastActionLog = gameState.getCurrentPlayer().name + " бросил кубики: " + total;

            Player current = gameState.getCurrentPlayer();
            int oldPosition = current.position;
            current.position = (oldPosition + total) % 40;

            // Проверка прохождения Старта
            if (current.position < oldPosition) {
                current.receive(200);
                gameState.lastActionLog = current.name + " прошёл Старт и получил 200!";
            }

            BoardCell cell = getCurrentCell();
            if (cell != null) {
                switch (cell.type) {
                    case START:
                    case REST:
                    case JAIL:
                        gameState.nextPlayer();
                        break;
                    case TAX:
                        current.pay(100);
                        gameState.lastActionLog = current.name + " заплатил налог 100";
                        gameState.nextPlayer();
                        break;
                    case SITUATION:
                        gameState.currentPhase = GameState.GamePhase.PLAYER_ACTION;
                        gameState.lastActionLog = current.name + " попал на " + cell.name;
                        break;
                    default:
                        gameState.nextPlayer();
                        break;
                }
            }

            RollDiceResponse response = new RollDiceResponse();
            response.playerId = connection.getID();
            response.dice1 = dice1;
            response.dice2 = dice2;
            response.total = total;

            sendAllTcpSafely(response);
            broadcastGameState();
        }
    }

    public void startGame() {
        if (gameState.players == null || gameState.players.size() < 2) {
            System.out.println("Нельзя запустить игру: нужно минимум 2 игрока");
            return;
        }

        gameState.currentPhase = GameState.GamePhase.PLAYING;
        gameState.lastActionLog = "Игра началась";
        gameState.turnCount = 1;
        broadcastGameState();
        System.out.println("Игра запущена сервером");
    }

    private void removePlayer(int playerId) {
        gameState.removePlayer(playerId);
        broadcastGameState();
    }

    private void broadcastGameState() {
        GameStatePacket packet = new GameStatePacket(gameState);
        sendAllTcpSafely(packet);
    }

    private void sendTcpSafely(Connection connection, Object packet) {
        try {
            connection.sendTCP(packet);
            System.out.println("sendTCP OK: packet=" + packet.getClass().getSimpleName() + ", connectionId=" + connection.getID());
        } catch (Exception e) {
            System.err.println("sendTCP ERROR: packet=" + packet.getClass().getSimpleName() + ", connectionId=" + connection.getID() + ", reason=" + e.getMessage());
            e.printStackTrace();
        }
    }

    private void sendAllTcpSafely(Object packet) {
        try {
            server.sendToAllTCP(packet);
            System.out.println("sendToAllTCP OK: packet=" + packet.getClass().getSimpleName() + ", recipients=" + server.getConnections().size());
        } catch (Exception e) {
            System.err.println("sendToAllTCP ERROR: packet=" + packet.getClass().getSimpleName() + ", reason=" + e.getMessage());
            e.printStackTrace();
        }
    }

    public void stop() {
        server.stop();
    }
}

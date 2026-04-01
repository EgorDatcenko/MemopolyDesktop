package com.memopoly.network;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.minlog.Log;
import com.memopoly.game.model.GameState;
import com.memopoly.network.packets.CreateRoomRequest;
import com.memopoly.network.packets.GameStatePacket;
import com.memopoly.network.packets.JoinRoomRequest;
import com.memopoly.network.packets.JoinRoomResponse;
import com.memopoly.network.packets.RollDiceRequest;
import com.memopoly.network.packets.RollDiceResponse;
import com.memopoly.network.packets.StartGameRequest;

import java.io.IOException;

public class GameClient {
    private final Client client;
    private volatile GameState gameState;
    private final NetworkListener listener;
    private String pendingJoinPlayerName;
    private volatile boolean clientLoopRunning;
    private Thread clientUpdateThread;

    public GameClient(NetworkListener listener) {
        Log.set(Log.LEVEL_DEBUG);
        this.listener = listener;
        client = new Client(65536, 65536);
        gameState = new GameState();

        registerPackets();
        setupClient();
        startClient();
    }

    private void setupClient() {
        clientLoopRunning = true;
        clientUpdateThread = new Thread(() -> {
            while (clientLoopRunning) {
                try {
                    client.update(16);
                } catch (IOException e) {
                    System.err.println("Ошибка update-клиента: " + e.getMessage());
                }
            }
        }, "memopoly-client-update");
        clientUpdateThread.setDaemon(true);
        clientUpdateThread.start();
    }

    private void registerPackets() {
        NetworkRegistry.register(client.getKryo());
    }

    private void startClient() {
        client.addListener(new Listener() {
            @Override
            public void connected(Connection connection) {
                System.out.println("Подключились к серверу! Connection ID: " + connection.getID());
                Gdx.app.postRunnable(() ->
                listener.onConnected()
                );

                if (pendingJoinPlayerName != null) {
                    JoinRoomRequest request = new JoinRoomRequest();
                    request.playerName = pendingJoinPlayerName;
                    sendJoinRoom(request);
                    pendingJoinPlayerName = null;
                }
            }

            @Override
            public void disconnected(Connection connection) {
                System.out.println("Отключились от сервера");
                Gdx.app.postRunnable(() ->
                listener.onDisconnected()
                );
            }

            @Override
            public void received(Connection connection, Object object) {
                handlePacket(object);
            }
        });
    }

    public void connect(String hostIP, int port) {
        if (client.isConnected()) {
            System.out.println("Клиент уже подключен, новый connect пропущен");
            return;
        }

        Thread connectThread = new Thread(() -> {
            try {
                System.out.println("Пытаемся подключиться к " + hostIP + ":" + port);
                client.connect(15000, hostIP, port, 54777);
                System.out.println("Подключение к " + hostIP + ":" + port + " завершено");
            } catch (IOException e) {
                System.err.println("Ошибка подключения: " + e.getMessage());
            }
        }, "memopoly-client-connect");

        connectThread.setDaemon(true);
        connectThread.start();
    }

    public void connectAndJoin(String hostIP, int port, String playerName) {
        pendingJoinPlayerName = playerName;
        connect(hostIP, port);
    }

    private void handlePacket(Object packet) {
        if (packet instanceof JoinRoomResponse) {
            JoinRoomResponse response = (JoinRoomResponse) packet;
            if (response.success) {
                System.out.println("JoinRoomResponse: успех, playerId=" + response.playerId);
                Gdx.app.postRunnable(() ->
                    listener.onJoinedRoom()
                );
            } else {
                System.out.println("JoinRoomResponse: отказ во входе");
            }
        } else if (packet instanceof GameStatePacket) {
            gameState = ((GameStatePacket) packet).gameState;
            Gdx.app.postRunnable(() ->
                listener.onGameStateUpdated(gameState)
            );
        } else if (packet instanceof RollDiceResponse) {
            Gdx.app.postRunnable(() ->
            listener.onDiceRolled((RollDiceResponse) packet)
            );
        }
    }

    public void sendCreateRoom(CreateRoomRequest request) {
        client.sendTCP(request);
    }

    public void sendJoinRoom(JoinRoomRequest request) {
        if (request == null || request.playerName == null || request.playerName.trim().isEmpty()) {
            System.out.println("JoinRoomRequest не отправлен: пустое имя игрока");
            return;
        }

        if (!client.isConnected()) {
            System.out.println("JoinRoomRequest не отправлен: клиент не подключен");
            return;
        }

        request.playerName = request.playerName.trim();
        client.sendTCP(request);
        System.out.println("JoinRoomRequest отправлен через TCP, playerName=" + request.playerName);
    }

    public void sendStartGame(StartGameRequest request) {
        if (!client.isConnected()) {
            System.out.println("StartGameRequest не отправлен: клиент не подключен");
            return;
        }
        client.sendTCP(request);
    }

    public void sendRollDice(RollDiceRequest request) {
        client.sendTCP(request);
    }

    public void disconnect() {
        clientLoopRunning = false;
        client.close();
    }
}

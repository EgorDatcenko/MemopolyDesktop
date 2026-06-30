package com.memopoly.network.services;

import com.esotericsoftware.kryonet.Connection;
import com.memopoly.game.model.GameState;
import com.memopoly.network.packets.ActionRejectedPacket;
import com.memopoly.network.packets.GameStatePacket;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Сервис рассылки состояния: управляет отправкой пакетов GameStatePacket клиентам на сервере.
 */
public class GameStatePublisher {
    private final GameState gameState;
    private final Consumer<Object> sendAllTcp;
    private final BiConsumer<Connection, Object> sendTcp;

    public GameStatePublisher(GameState gameState, Consumer<Object> sendAllTcp, BiConsumer<Connection, Object> sendTcp) {
        this.gameState = gameState;
        this.sendAllTcp = sendAllTcp;
        this.sendTcp = sendTcp;
    }

    public void broadcastState() {
        sendAllTcp.accept(new GameStatePacket(gameState));
    }

    public void sendActionRejected(Connection connection, String actionType, String reasonCode, String reason) {
        sendTcp.accept(connection, new ActionRejectedPacket(actionType, reasonCode, reason));
    }
}

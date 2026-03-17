package com.memopoly.network.packets;

import com.memopoly.game.model.GameState;

public class GameStatePacket {
    public GameState gameState;  // Всё состояние игры
    public long timestamp;
    public GameStatePacket() {
    }
    public GameStatePacket(GameState gameState) {
        this.gameState = gameState;
        this.timestamp = System.currentTimeMillis();
    }
}

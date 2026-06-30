package com.memopoly.network.packets;

import com.memopoly.game.model.GameState;

/**
 * Пакет состояния игры: содержит полную копию GameState, отправляемую сервером клиентам при любых изменениях.
 */
public class GameStatePacket {
    public GameState gameState;  
    public long timestamp;
    public GameStatePacket() {
    }
    public GameStatePacket(GameState gameState) {
        this.gameState = gameState;
        this.timestamp = System.currentTimeMillis();
    }
}

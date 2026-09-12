package com.memopoly.network.packets;

import com.memopoly.game.model.GameState;

/**
 * Пакет ответа на вход в комнату: содержит результат проверки кода и статус подключения.
 */
public class JoinRoomResponse {
    public boolean success;
    public int playerId;
    public GameState gameState;
    public JoinRoomResponse() {}
}

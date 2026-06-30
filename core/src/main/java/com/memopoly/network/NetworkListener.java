package com.memopoly.network;

import com.memopoly.game.model.GameState;
import com.memopoly.network.packets.RollDiceResponse;
import com.memopoly.network.packets.ChatMessage;

/**
 * Сетевой интерфейс обратных вызовов: определяет методы для обработки сетевых событий (подключение, изменение состояния, чат).
 */
public interface NetworkListener {
    void onGameStateUpdated(GameState gameState);
    void onDiceRolled(RollDiceResponse response);
    void onConnected();
    void onJoinedRoom();
    void onDisconnected();
    void onConnectionFailed(String reason);
    void onActionRejected(String actionType, String reasonCode, String reason);
    void onChatMessage(ChatMessage message);
}

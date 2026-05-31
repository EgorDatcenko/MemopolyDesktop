package com.memopoly.network;

import com.memopoly.game.model.GameState;
import com.memopoly.network.packets.RollDiceResponse;
import com.memopoly.network.packets.ChatMessage;

public interface NetworkListener {
    void onGameStateUpdated(GameState gameState);
    void onDiceRolled(RollDiceResponse response);
    void onConnected();
    void onJoinedRoom();
    void onDisconnected();
    void onConnectionFailed(String reason);
    default void onActionRejected(String actionType, String reasonCode, String reason) {}
    default void onChatMessage(ChatMessage message) {}
}

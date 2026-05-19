package com.memopoly.network;

import com.memopoly.game.model.GameState;
import com.memopoly.network.packets.RollDiceResponse;

public interface NetworkListener {
    void onGameStateUpdated(GameState gameState);
    void onDiceRolled(RollDiceResponse response);
    void onConnected();
    void onJoinedRoom();
    void onDisconnected();
    void onConnectionFailed(String reason);
    void onActionRejected(String actionType, String reasonCode, String reason);
}

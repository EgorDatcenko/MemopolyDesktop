package com.memopoly.network.packets;

import com.memopoly.game.model.GameState;

public class JoinRoomResponse {
    public boolean success;
    public int playerId;
    public GameState gameState;
    public JoinRoomResponse() {}
}

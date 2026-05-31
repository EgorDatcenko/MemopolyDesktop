package com.memopoly.network.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.GameActionRequest;

public interface GameActionHandler {
    boolean handle(Connection connection, Player actingPlayer, GameActionRequest request);
}

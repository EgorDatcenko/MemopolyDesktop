package com.memopoly.network.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.GameActionRequest;

/**
 * Общий интерфейс обработчиков действий: задаёт стандартный контракт выполнения игровых действий на сервере.
 */
public interface GameActionHandler {
    boolean handle(Connection connection, Player actingPlayer, GameActionRequest request);
}

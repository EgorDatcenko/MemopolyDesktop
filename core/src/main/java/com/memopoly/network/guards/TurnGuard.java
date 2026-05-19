package com.memopoly.network.guards;

import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;

public final class TurnGuard {
    private TurnGuard() {
    }

    public static boolean isCurrentPlayer(GameState gameState, int connectionId) {
        Player current = gameState.getCurrentPlayer();
        return current != null && current.id == connectionId;
    }
}

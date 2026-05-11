package com.memopoly.network.guards;

import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.GameState;

public final class PhaseGuard {
    private PhaseGuard() {
    }

    public static boolean canBuySituationCell(GameState gameState, BoardCell cell) {
        return gameState.currentPhase == GameState.GamePhase.PLAYER_ACTION
            && cell != null
            && cell.type == BoardCell.Type.SITUATION;
    }

    public static boolean canUseMemeBank(GameState gameState, int playerId) {
        return gameState.currentPhase == GameState.GamePhase.MEME_BANK_ACTION
            && gameState.memeBankPlayerId == playerId;
    }
}

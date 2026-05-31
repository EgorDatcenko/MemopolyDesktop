package com.memopoly.network.guards;

import com.memopoly.game.model.GameState;

public final class AuctionGuard {
    private AuctionGuard() {
    }

    public static boolean isAuctionActive(GameState gameState) {
        return gameState.currentPhase == GameState.GamePhase.AUCTION && gameState.isInAuction;
    }

    public static boolean isCurrentAuctionBidder(GameState gameState, int playerId) {
        return playerId == gameState.auctionCurrentPlayerId;
    }
}

package com.memopoly.game.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Юнит-тесты логики GameState: проверяют смену ходов, логику банкротства и базовые правила аукционов.
 */
class GameStateBaselineTest {

    @Test
    void nextPlayerResetsTurnFieldsAndSetsPlayingPhase() {
        GameState state = new GameState();
        state.addPlayer(new Player(1, "P1"));
        state.addPlayer(new Player(2, "P2"));

        state.currentPhase = GameState.GamePhase.AUCTION;
        state.hasRolledThisTurn = true;
        state.diceValue = 9;
        state.memeBankPlayerId = 1;
        int previousTurnCount = state.turnCount;

        state.nextPlayer();

        assertEquals(GameState.GamePhase.PLAYING, state.currentPhase);
        assertFalse(state.hasRolledThisTurn);
        assertEquals(0, state.diceValue);
        assertEquals(-1, state.memeBankPlayerId);
        assertEquals(previousTurnCount + 1, state.turnCount);
        assertNotNull(state.getCurrentPlayer());
    }

    @Test
    void startAuctionSetsAuctionPhaseAndFlags() {
        GameState state = new GameState();

        state.startAuction(12);

        assertTrue(state.isInAuction);
        assertEquals(12, state.auctionCellId);
        assertEquals(GameState.GamePhase.AUCTION, state.currentPhase);
        assertEquals(30, state.currentAuctionTime);
        assertTrue(state.auctionBids.isEmpty());
    }

    @Test
    void endAuctionRestoresPlayingDefaults() {
        GameState state = new GameState();
        state.startAuction(7);
        state.auctionStarterPlayerId = 10;
        state.auctionCurrentPlayerId = 11;
        state.auctionBids.put(11, 120);

        state.endAuction();

        assertFalse(state.isInAuction);
        assertEquals(-1, state.auctionCellId);
        assertEquals(-1, state.auctionStarterPlayerId);
        assertEquals(-1, state.auctionCurrentPlayerId);
        assertEquals(30, state.currentAuctionTime);
        assertEquals(GameState.GamePhase.PLAYING, state.currentPhase);
        assertTrue(state.auctionBids.isEmpty());
    }

    @Test
    void bankruptcyPlayersAreSkippedOnNextPlayer() {
        GameState state = new GameState();
        Player p1 = new Player(1, "P1");
        Player p2 = new Player(2, "P2");
        Player p3 = new Player(3, "P3");
        p2.isBankrupt = true;

        state.addPlayer(p1);
        state.addPlayer(p2);
        state.addPlayer(p3);
        state.currentPlayerIndex = 0;

        state.nextPlayer();

        assertEquals(3, state.getCurrentPlayer().id);
        assertEquals("P3", state.getCurrentPlayer().name);
    }
}

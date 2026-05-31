package com.memopoly.network.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.GameActionRequest;

import java.util.function.Consumer;
import java.util.function.IntPredicate;

public class BuyBackCellActionHandler implements GameActionHandler {
    private final GameState gameState;
    private final IntPredicate isCurrentPlayer;
    private final Consumer<Player> updateMaxAffordable;
    private final java.util.function.BiConsumer<Player, Integer> buyBackExecutor;

    public BuyBackCellActionHandler(GameState gameState,
                                    IntPredicate isCurrentPlayer,
                                    Consumer<Player> updateMaxAffordable,
                                    java.util.function.BiConsumer<Player, Integer> buyBackExecutor) {
        this.gameState = gameState;
        this.isCurrentPlayer = isCurrentPlayer;
        this.updateMaxAffordable = updateMaxAffordable;
        this.buyBackExecutor = buyBackExecutor;
    }

    @Override
    public boolean handle(Connection connection, Player actingPlayer, GameActionRequest request) {
        if (request.actionType != GameActionRequest.ActionType.BUY_BACK_CELL) return false;
        if (!isCurrentPlayer.test(connection.getID()) || gameState.currentPhase == GameState.GamePhase.AUCTION || gameState.currentPhase == GameState.GamePhase.MEME_BATTLE) {
            return true;
        }
        updateMaxAffordable.accept(actingPlayer);
        buyBackExecutor.accept(actingPlayer, request.targetId);
        return true;
    }
}

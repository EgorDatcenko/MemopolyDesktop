package com.memopoly.network.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.GameActionRequest;

import java.util.function.Consumer;
import java.util.function.IntPredicate;

/**
 * Обработчик залога клетки: переводит имущество игрока в состояние залога (ипотеки) для получения быстрых денег.
 */
public class MortgageCellActionHandler implements GameActionHandler {
    private final GameState gameState;
    private final IntPredicate isCurrentPlayer;
    private final Consumer<Player> updateMaxAffordable;
    private final java.util.function.BiConsumer<Player, Integer> mortgageExecutor;

    public MortgageCellActionHandler(GameState gameState,
                                     IntPredicate isCurrentPlayer,
                                     Consumer<Player> updateMaxAffordable,
                                     java.util.function.BiConsumer<Player, Integer> mortgageExecutor) {
        this.gameState = gameState;
        this.isCurrentPlayer = isCurrentPlayer;
        this.updateMaxAffordable = updateMaxAffordable;
        this.mortgageExecutor = mortgageExecutor;
    }

    @Override
    public boolean handle(Connection connection, Player actingPlayer, GameActionRequest request) {
        if (request.actionType != GameActionRequest.ActionType.MORTGAGE_CELL) return false;
        if (!isCurrentPlayer.test(connection.getID()) || gameState.currentPhase == GameState.GamePhase.AUCTION || gameState.currentPhase == GameState.GamePhase.MEME_BATTLE) {
            return true;
        }
        updateMaxAffordable.accept(actingPlayer);
        mortgageExecutor.accept(actingPlayer, request.targetId);
        return true;
    }
}

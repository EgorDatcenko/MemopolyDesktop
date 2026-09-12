package com.memopoly.network.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.network.guards.PhaseGuard;
import com.memopoly.network.packets.GameActionRequest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.ObjIntConsumer;
import java.util.function.ToIntFunction;

/**
 * Обработчик отказа от покупки клетки: переводит игру в режим аукциона, если игрок отказался покупать клетку, на которую наступил.
 */
public class PassBuyActionHandler implements GameActionHandler {
    private final GameState gameState;
    private final List<BoardCell> board;
    private final IntPredicate isCurrentPlayer;
    private final Consumer<Player> updateMaxAffordable;
    private final ToIntFunction<Integer> findNextAuctionBidderId;
    private final Runnable startAuctionTimer;
    private final ObjIntConsumer<Player> reject;

    public PassBuyActionHandler(GameState gameState,
                                List<BoardCell> board,
                                IntPredicate isCurrentPlayer,
                                Consumer<Player> updateMaxAffordable,
                                ToIntFunction<Integer> findNextAuctionBidderId,
                                Runnable startAuctionTimer,
                                ObjIntConsumer<Player> reject) {
        this.gameState = gameState;
        this.board = board;
        this.isCurrentPlayer = isCurrentPlayer;
        this.updateMaxAffordable = updateMaxAffordable;
        this.findNextAuctionBidderId = findNextAuctionBidderId;
        this.startAuctionTimer = startAuctionTimer;
        this.reject = reject;
    }

    @Override
    public boolean handle(Connection connection, Player actingPlayer, GameActionRequest request) {
        if (request.actionType != GameActionRequest.ActionType.PASS_BUY) return false;
        if (!isCurrentPlayer.test(connection.getID())) {
            reject.accept(actingPlayer, 0);
            return true;
        }
        Player current = gameState.getCurrentPlayer();
        if (current == null) return true;
        BoardCell currentCell = board.get(current.position);
        updateMaxAffordable.accept(current);
        if (!PhaseGuard.canBuySituationCell(gameState, currentCell)) {
            reject.accept(current, 1);
            return true;
        }
        gameState.startAuction(currentCell.id);
        gameState.auctionStarterPlayerId = current.id;
        gameState.auctionCurrentPlayerId = findNextAuctionBidderId.applyAsInt(current.id);
        gameState.lastActionLog = "Начинается аукцион! Первый ход: " + (gameState.auctionCurrentPlayerId == -1 ? "—" : gameState.auctionCurrentPlayerId);
        startAuctionTimer.run();
        return true;
    }
}

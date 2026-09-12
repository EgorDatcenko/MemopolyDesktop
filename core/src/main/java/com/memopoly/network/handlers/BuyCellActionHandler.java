package com.memopoly.network.handlers;

import com.esotericsoftware.kryonet.Connection;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.BoardData;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.network.guards.PhaseGuard;
import com.memopoly.network.packets.GameActionRequest;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.IntPredicate;
import java.util.function.ObjIntConsumer;

/**
 * Обработчик покупки клетки: обрабатывает решение игрока приобрести свободную клетку на поле.
 */
public class BuyCellActionHandler implements GameActionHandler {
    private final GameState gameState;
    private final List<BoardCell> board;
    private final IntPredicate isCurrentPlayer;
    private final Consumer<Player> updateMaxAffordable;
    private final ObjIntConsumer<Player> reject;

    public BuyCellActionHandler(GameState gameState,
                                List<BoardCell> board,
                                IntPredicate isCurrentPlayer,
                                Consumer<Player> updateMaxAffordable,
                                ObjIntConsumer<Player> reject) {
        this.gameState = gameState;
        this.board = board;
        this.isCurrentPlayer = isCurrentPlayer;
        this.updateMaxAffordable = updateMaxAffordable;
        this.reject = reject;
    }

    @Override
    public boolean handle(Connection connection, Player actingPlayer, GameActionRequest request) {
        if (request.actionType != GameActionRequest.ActionType.BUY_CELL) return false;
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
        if (!current.canAfford(currentCell.price)) {
            gameState.lastActionLog = current.name + " не может купить — недостаточно средств";
            return true;
        }
        current.pay(currentCell.price);
        gameState.cellOwners.put(currentCell.id, current.id);
        current.ownedCells.add(currentCell.id);
        gameState.showNotification(current.name + " купил " + currentCell.name);
        gameState.lastActionLog = current.name + " купил " + currentCell.name;
        gameState.recordAchievement(current.id, "FIRST_BUY");
        if (current.ownedCells.size() >= 10) {
            gameState.recordAchievement(current.id, "LANDLORD");
        }
        for (String group : com.memopoly.utils.MonopolyUtils.getOwnedGroups(current, board)) {
            BoardCell.Group groupEnum = BoardCell.Group.valueOf(group);
            if (com.memopoly.utils.MonopolyUtils.hasFullGroup(current, groupEnum, board)) {
                gameState.recordAchievement(current.id, "MONOPOLIST");
                break;
            }
        }
        gameState.currentPhase = GameState.GamePhase.PLAYING;
        return true;
    }

    private java.util.Set<String> getOwnedGroups(Player player) {
        java.util.Set<String> groups = new java.util.HashSet<>();
        for (int cellId : player.ownedCells) {
            BoardCell cell = board.get(cellId);
            if (cell.group != null) {
                groups.add(cell.group.name());
            }
        }
        return groups;
    }

    private boolean hasFullGroup(Player player, String groupName) {
        BoardCell.Group group = BoardCell.Group.valueOf(groupName);
        List<BoardCell> groupCells = BoardData.getCellsInGroup(board, group);
        for (BoardCell cell : groupCells) {
            if (!player.ownedCells.contains(cell.id)) {
                return false;
            }
        }
        return true;
    }

}

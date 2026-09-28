package com.memopoly.utils;

import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.BoardData;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.game.model.Role;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public final class MonopolyUtils {
    private MonopolyUtils() {}

    /**
     * Возвращает набор названий групп (цветов), которыми владеет игрок.
     */
    public static Set<String> getOwnedGroups(Player player, List<BoardCell> board) {
        Set<String> groups = new HashSet<>();
        if (player == null || player.ownedCells == null) return groups;
        for (int cellId : player.ownedCells) {
            if (cellId < 0 || cellId >= board.size()) continue;
            BoardCell cell = board.get(cellId);
            if (cell.group != null && cell.isBuildableSituation()) {
                groups.add(cell.group.name());
            }
        }
        return groups;
    }

    /**
     * Проверяет, владеет ли игрок ВСЕМИ клетками заданной группы (полная монополия).
     */
    public static boolean hasFullGroup(Player player, BoardCell.Group group, List<BoardCell> board) {
        if (player == null || group == null || board == null) return false;
        List<BoardCell> groupCells = BoardData.getCellsInGroup(board, group);
        if (groupCells == null || groupCells.size() < 2) return false;
        for (BoardCell cell : groupCells) {
            if (!player.ownedCells.contains(cell.id)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Возвращает количество полных монополий у игрока.
     */
    public static int countFullGroups(Player player, List<BoardCell> board) {
        if (player == null) return 0;
        int count = 0;
        for (String groupName : getOwnedGroups(player, board)) {
            try {
                BoardCell.Group group = BoardCell.Group.valueOf(groupName);
                if (hasFullGroup(player, group, board)) {
                    count++;
                }
            } catch (IllegalArgumentException ignored) {
                // неизвестная группа
            }
        }
        return count;
    }

    public static int getHousePrice(BoardCell cell, Role role) {
        if (cell == null) return 0;
        int base = Math.max(1, cell.price / 2);
        return role == Role.MONOPOLIST ? Math.max(1, base * 85 / 100) : base;
    }

    public static int getHousePrice(BoardCell cell) {
        return getHousePrice(cell, (Role) null);
    }

    public static boolean hasFullMonopoly(BoardCell cell, int ownerId, GameState state, List<BoardCell> board) {
        if (cell == null || ownerId < 0 || !cell.isBuildableSituation() || state == null || board == null) {
            return false;
        }
        List<BoardCell> groupCells = BoardData.getCellsInGroup(board, cell.group);
        if (groupCells == null || groupCells.size() < 2) {
            return false;
        }
        for (BoardCell groupCell : groupCells) {
            if (state.cellOwners == null || state.cellOwners.getOrDefault(groupCell.id, -1) != ownerId) {
                return false;
            }
            if (state.cellMortgaged != null && Boolean.TRUE.equals(state.cellMortgaged.get(groupCell.id))) {
                return false;
            }
        }
        return true;
    }

    public static int calculateRent(BoardCell cell, GameState state, List<BoardCell> board) {
        if (cell == null) {
            return 0;
        }
        int baseFee = cell.getEntranceFee();
        if (baseFee == 0) {
            return 0;
        }
        int ownerId = (state != null && state.cellOwners != null) ? state.cellOwners.getOrDefault(cell.id, -1) : -1;
        int monopolyMultiplier = hasFullMonopoly(cell, ownerId, state, board) ? 2 : 1;
        int houses = (state != null && state.cellHouses != null) ? state.cellHouses.getOrDefault(cell.id, 0) : 0;
        return baseFee * monopolyMultiplier * (1 + houses);
    }

    public static int calculateRentForTier(BoardCell cell, int houses, boolean hasMonopoly) {
        if (cell == null) {
            return 0;
        }
        int baseFee = cell.getEntranceFee();
        if (baseFee == 0) {
            return 0;
        }
        int monopolyMultiplier = (houses > 0 || hasMonopoly) ? 2 : 1;
        return baseFee * monopolyMultiplier * (1 + houses);
    }

    public static int getMortgageValue(BoardCell cell) {
        return cell != null ? cell.price / 2 : 0;
    }

    public static int getUnmortgagePrice(BoardCell cell) {
        if (cell == null) {
            return 0;
        }
        return (int) ((cell.price / 2f) * 1.10f);
    }
}


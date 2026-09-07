package com.memopoly.utils;

import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.BoardData;
import com.memopoly.game.model.Player;

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
}

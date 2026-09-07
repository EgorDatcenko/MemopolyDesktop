package com.memopoly.network;

import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.GameState;

import java.util.*;

public class DevRouteController {
    private final List<BoardCell> board;          // исходный список
    private final List<Integer> groupCells;       // id целевой группы

    public List<Integer> getGroupCells() {
        return groupCells;
    }

    public DevRouteController(List<BoardCell> board) {
        this.board = board;
        // ищем первую подходящую группу
        Map<BoardCell.Group, List<BoardCell>> groups = new HashMap<>();
        for (BoardCell c : board) {
            groups.computeIfAbsent(c.group, k -> new ArrayList<>()).add(c);
        }
        BoardCell.Group targetGroup = null;
        for (BoardCell.Group key : groups.keySet()) {
            List<BoardCell> cells = groups.get(key);
            if (cells.size() >= 3 && cells.stream().allMatch(cd -> cd.type == BoardCell.Type.SITUATION)) {
                targetGroup = key;
                break;
            }
        }
        if (targetGroup == null) {
            throw new IllegalStateException("No dev group available");
        }
        groupCells = groups.get(targetGroup)
                .stream().map(c -> c.id).sorted().toList();
    }

    /** Первая непокупенная по id из groupCells */
    public int nextUnownedCell(GameState state, int playerId) {
        for (int id : groupCells) {
            if (!Objects.equals(state.cellOwners.get(id), playerId))
                return id;
        }
        return -1;
    }
}

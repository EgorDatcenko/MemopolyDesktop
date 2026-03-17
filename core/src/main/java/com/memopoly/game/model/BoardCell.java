package com.memopoly.game.model;

import java.util.ArrayList;
import java.util.List;

public class BoardCell {
    public enum Type{
        SITUATION,
        EVENT,
        TAX,
        START,
        REST,
        MEME_BATTLE
    }
    public enum Group {
        REDDIT, TIKTOK, INSTAGRAM, TWITTER,
        TELEGRAM, DISCORD, YOUTUBE, TWITCH,
        SPECIAL
    }
    public int id;
    public Type type;
    public String name;
    public int price;
    public Group group;
    public int ownerId;
    public boolean isMortgaged;

    public BoardCell() {}

    public BoardCell(int id, Type type, String name) {
        this();
        this.id = id;
        this.type = type;
        this.name = name;
        this.ownerId = -1; // -1 = не куплена
        this.isMortgaged = false;
    }

    public BoardCell(int id, String name, int price, Group group) {
        this(id, Type.SITUATION, name);
        this.price = price;
        this.group = group;
    }
    public static List<BoardCell> buildCells(){
        List<BoardCell> cells = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            switch (i){
                case 0:
                    BoardCell cell = new BoardCell(i, Type.START, "START");
                    cells.add(cell);
                    break;
                case 1:
                    BoardCell cell1 = new BoardCell(i, "Upvote", 60, Group.REDDIT);
                    cells.add(cell1);
                    break;
                case 2:
                    BoardCell cell2 = new BoardCell(i, Type.EVENT, "EVENT");
                    cells.add(cell2);
                    break;
                case 3:
                    BoardCell cell3 = new BoardCell(i, "Subreddits", 60, Group.REDDIT);
                    cells.add(cell3);
                    break;
                case 4:
                    BoardCell cell4 = new BoardCell(i, Type.TAX, "TAX");
                    cells.add(cell4);
                    break;
                case 5:
                    BoardCell cell5 = new BoardCell(i, Type.TAX, "TAX");
                    cells.add(cell5);
                    break;
            }
        }
        return cells;
    }
    public boolean isOwned() {
        return ownerId >= 0;
    }

    public boolean isActive() {
        return !isMortgaged && isOwned();
    }

    public int getEntranceFee() {
        if (!isActive()) return 0;
        return Math.max(10, price / 5);
    }
}

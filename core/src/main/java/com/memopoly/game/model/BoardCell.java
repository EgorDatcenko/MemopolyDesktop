package com.memopoly.game.model;

import javax.sound.sampled.LineEvent;
import java.util.ArrayList;
import java.util.List;

public class BoardCell {
    public enum Type{
        SITUATION,
        EVENT,
        TAX,
        START,
        REST,
        JAIL,
        REPORT,
        MEME_BATTLE,
        MEME_BANK
    }
    public enum Group {
        REDDIT, TIKTOK, INSTAGRAM, X,
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
                    BoardCell cell3 = new BoardCell(i, "Subreddits", 80, Group.REDDIT);
                    cells.add(cell3);
                    break;
                case 4:
                    BoardCell cell4 = new BoardCell(i, Type.TAX, "TAX");
                    cells.add(cell4);
                    break;
                case 5:
                    BoardCell cell5 = new BoardCell(i, Type.MEME_BATTLE, "MEME BATTLE");
                    cells.add(cell5);
                    break;
                case 6:
                    BoardCell cell6 = new BoardCell(i, "Trends", 120, Group.TIKTOK);
                    cells.add(cell6);
                    break;
                case 7:
                    BoardCell cell7 = new BoardCell(i, "Music", 125, Group.TIKTOK);
                    cells.add(cell7);
                    break;
                case 8:
                    BoardCell cell8 = new BoardCell(i, Type.MEME_BANK, "MEME_BANK");
                    cells.add(cell8);
                    break;
                case 9:
                    BoardCell cell9 = new BoardCell(i, "Feed", 130, Group.TIKTOK);
                    cells.add(cell9);
                    break;
                case 10:
                    BoardCell cell10 = new BoardCell(i, Type.REST, "Lack of sense of humor");
                    cells.add(cell10);
                    break;
                case 11:
                    BoardCell cell11 = new BoardCell(i, "Reels", 140, Group.INSTAGRAM);
                    cells.add(cell11);
                    break;
                case 12:
                    BoardCell cell12 = new BoardCell(i, Type.EVENT, "EVENT");
                    cells.add(cell12);
                    break;
                case 13:
                    BoardCell cell13 = new BoardCell(i, "Feed", 150, Group.INSTAGRAM);
                    cells.add(cell13);
                case 14:
                    BoardCell cell14 = new BoardCell(i, "Stories", 160, Group.INSTAGRAM);
                    cells.add(cell14);
                case 15:
                    BoardCell cell15 = new BoardCell(i, Type.MEME_BATTLE, "MEME BATTLE");
                    cells.add(cell15);
                case 16:
                    BoardCell cell16 = new BoardCell(i, "Posts", 180, Group.X);
                    cells.add(cell16);
                    break;
                case 17:
                    BoardCell cell17 = new BoardCell(i, Type.EVENT, "EVENT");
                    cells.add(cell17);
                    break;
                case 18:
                    BoardCell cell18 = new BoardCell(i, "News", 190, Group.X);
                    cells.add(cell18);
                    break;
                case 19:
                    BoardCell cell19 = new BoardCell(i, "Grok", 200, Group.X);
                    cells.add(cell19);
                    break;
                case 20:
                    BoardCell cell20 = new BoardCell(i, Type.REST, "FREE_PARKING");
                    cells.add(cell20);
                    break;
                case 21:
                    BoardCell cell21 = new BoardCell(i, "Chats", 210, Group.TELEGRAM);
                    cells.add(cell21);
                    break;
                case 22:
                    BoardCell cell22 = new BoardCell(i, Type.MEME_BATTLE, "MEME BATTLE");
                    cells.add(cell22);
                    break;
                case 23:
                    BoardCell cell23 = new BoardCell(i, "Channels", 230, Group.TELEGRAM);
                    cells.add(cell23);
                    break;
                case 24:
                    BoardCell cell24 = new BoardCell(i, "Stories", 240, Group.TELEGRAM);
                    cells.add(cell24);
                    break;
                case 25:
                    BoardCell cell25 = new BoardCell(i, Type.MEME_BATTLE, "MEME BATTLE");
                    cells.add(cell25);
                    break;
                case 26:
                    BoardCell cell26 = new BoardCell(i, "Voice channels", 250, Group.DISCORD);
                    cells.add(cell26);
                    break;
                case 27:
                    BoardCell cell27 = new BoardCell(i, "Chats", 260, Group.DISCORD);
                    cells.add(cell27);
                    break;
                case 28:
                    BoardCell cell28 = new BoardCell(i, Type.REPORT, "Copyright Infringement");
                    cells.add(cell28);
                    break;
                case 29:
                    BoardCell cell29 = new BoardCell(i, "Channels", 270, Group.DISCORD);
                    cells.add(cell29);
                    break;
                case 30:
                    BoardCell cell30 = new BoardCell(i, Type.JAIL, "Copyright Infringement");
                    cells.add(cell30);
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

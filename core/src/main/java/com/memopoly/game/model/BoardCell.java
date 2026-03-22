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

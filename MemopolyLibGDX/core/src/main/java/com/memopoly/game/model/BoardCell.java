package com.memopoly.game.model;

public class BoardCell {
    public enum Type{
        SITUATION,
        EVENT,
        TAX,
        START,
        REST,
        MEME_BATTLE
    }
    public int id;
    public Type type;
    public String name;
    public int price;
    public String groupName;
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

    public BoardCell(int id, String name, int price, String groupName) {
        this(id, Type.SITUATION, name);
        this.price = price;
        this.groupName = groupName;
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

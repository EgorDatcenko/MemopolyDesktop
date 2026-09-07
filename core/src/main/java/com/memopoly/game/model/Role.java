package com.memopoly.game.model;

public enum Role {
    MEMOLOG, SMM, MONOPOLIST, MODERATOR, SCAMMER, DOGE;

    public static Role of(String name) {
        if (name == null) return null;
        try { return Role.valueOf(name); } catch (IllegalArgumentException e) { return null; }
    }
}

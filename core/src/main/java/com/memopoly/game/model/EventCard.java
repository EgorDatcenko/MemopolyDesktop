package com.memopoly.game.model;

public class EventCard {
    public enum EffectType{
        RECEIVE_MONEY,    //+n
        SKIP_TURN,        // Пропустить ход
        COLLECT_FROM_ALL, // Каждый платит n
        PAY_MONEY,        // Заплатить n
        DRAW_MEME,        // Все получают карту
        RECEIVE_MONEY_LARGE // +n > 100
    }

    public int id;
    public String title;
    public String description;
    public EffectType effectType;
    public int amount; // Для денежных эффектов
    public String targetPlayer; // Для специфических эффектов

    // Пустой конструктор для KryoNet
    public EventCard() {}

    public EventCard(int id, String title, String description, EffectType effectType) {
        this();
        this.id = id;
        this.title = title;
        this.description = description;
        this.effectType = effectType;
        this.amount = 0; // По умолчанию
    }

    public EventCard(int id, String title, String description, EffectType effectType, int amount) {
        this(id, title, description, effectType);
        this.amount = amount;
    }

    // Фабричные методы для удобства
    public static EventCard createReceiveCard(int id, String title, String desc, int amount) {
        return new EventCard(id, title, desc, EffectType.RECEIVE_MONEY, amount);
    }

    public static EventCard createPayCard(int id, String title, String desc, int amount) {
        return new EventCard(id, title, desc, EffectType.PAY_MONEY, amount);
    }

    public static EventCard createSkipTurnCard(int id, String title, String desc) {
        return new EventCard(id, title, desc, EffectType.SKIP_TURN);
    }

    public static EventCard createCollectFromAllCard(int id, String title, String desc, int amount) {
        return new EventCard(id, title, desc, EffectType.COLLECT_FROM_ALL, amount);
    }

    public static EventCard createDrawMemeCard(int id, String title, String desc) {
        return new EventCard(id, title, desc, EffectType.DRAW_MEME);
    }
}

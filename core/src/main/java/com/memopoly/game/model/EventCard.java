package com.memopoly.game.model;

/**
 * Модель карточки событий (шанс): содержит описание события и тип применяемого эффекта (деньги, пропуск хода, взять карту и т.д.).
 */
public class EventCard {
    public enum EffectType{
        RECEIVE_MONEY,    
        SKIP_TURN,        
        COLLECT_FROM_ALL, 
        PAY_MONEY,        
        DRAW_MEME,        
        RECEIVE_MONEY_LARGE,
        RECEIVE_PER_OWNED_CELL,
        PAY_PER_OWNED_CELL,
        SKIP_NEXT_RENT_COLLECTION,
        EXTRA_ROLL,
        RETURN_TO_START,
        MARKET_CRASH
    }

    public int id;
    public String title;
    public String description;
    public EffectType effectType;
    public int amount; 
    public String targetPlayer; 

    public EventCard() {}

    public EventCard(int id, String title, String description, EffectType effectType) {
        this();
        this.id = id;
        this.title = title;
        this.description = description;
        this.effectType = effectType;
        this.amount = 0; 
    }

    public EventCard(int id, String title, String description, EffectType effectType, int amount) {
        this(id, title, description, effectType);
        this.amount = amount;
    }

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

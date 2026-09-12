package com.memopoly.utils;

import com.badlogic.gdx.graphics.Color;
import com.memopoly.game.model.Role;

public final class RoleInfo {
    private RoleInfo() {
    }

    public static Color color(Role role) {
        switch (role) {
            case MEMOLOG:
                return new Color(0.23f, 0.70f, 0.98f, 1f); // blue
            case SMM:
                return new Color(0.77f, 0.48f, 0.98f, 1f); // purple
            case MONOPOLIST:
                return new Color(1.00f, 0.57f, 0.16f, 1f); // orange
            case MODERATOR:
                return new Color(0.95f, 0.34f, 0.28f, 1f); // red
            case SCAMMER:
                return new Color(1.00f, 0.84f, 0.31f, 1f); // yellow
            case DOGE:
                return new Color(0.35f, 0.78f, 0.35f, 1f); // green
            default:
                return Color.GRAY;
        }
    }

    /** Индекс в массиве playerPlaqueSets BoardRenderer (0..5). */
    public static int plaqueIndex(Role role) {
        switch (role) {
            case MODERATOR:
                return 0; // red
            case MEMOLOG:
                return 1; // blue
            case SCAMMER:
                return 2; // yellow
            case MONOPOLIST:
                return 3; // orange
            case SMM:
                return 4; // purple
            case DOGE:
                return 5; // green
            default:
                return 0;
        }
    }

    public static String name(Role role, boolean ru) {
        switch (role) {
            case MEMOLOG:
                return ru ? "Мемолог" : "Memologist";
            case SMM:
                return ru ? "СММ-специалист" : "SMM Manager";
            case MONOPOLIST:
                return ru ? "Монополист" : "Monopolist";
            case MODERATOR:
                return ru ? "Модератор" : "Moderator";
            case SCAMMER:
                return ru ? "Скаммер" : "Scammer";
            case DOGE:
                return ru ? "Доге" : "Doge";
            default:
                return "";
        }
    }

    public static String description(Role role, boolean ru) {
        switch (role) {
            case MEMOLOG:
                return ru ? "6 карт на старте и 215 монет за проход Старта."
                        : "Starts with 6 cards & receives 215 coins for passing Start.";
            case SMM:
                return ru ? "Вклад в Meme Bank даёт 20% по вкладу." : "Meme Bank deposit pays 20% interest.";
            case MONOPOLIST:
                return ru ? "Филиалы на 15% дешевле." : "Branches cost 15% less.";
            case MODERATOR:
                return ru ? "Выбор: пропустить тюрьму или сесть и получить щит."
                        : "Choice: skip jail or enter jail and gain a shield.";
            case SCAMMER:
                return ru ? "Кража 50 у цели богаче себя; щит цели поглощает кражу."
                        : "Steal 50 from target richer than self; target's shield absorbs steal.";
            case DOGE:
                return ru ? "+2 клетки за 10 монет, раз в круг." : "+2 cells for 10 coins, once per round.";
            default:
                return "";
        }
    }

    public static String shieldDescription(boolean ru) {
        return ru ? "Щит поглощает налог, кражу Скаммера или сгорание вклада. Максимум 2."
                : "Shield absorbs tax, Scammer steal, or deposit crash. Max 2.";
    }

    public static String parkingDescription(boolean ru) {
        return ru ? "Свободная парковка восстанавливает 1 щит (максимум 2)."
                : "Free parking recharges 1 shield (max 2).";
    }
}

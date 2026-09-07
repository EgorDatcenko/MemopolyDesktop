package com.memopoly.steam;

import java.util.HashMap;
import java.util.Map;

/**
 * Каталог достижений: API-имя → [titleRu, titleEn, descriptionRu, descriptionEn].
 * Те же API-имена регистрируются в Steamworks dashboard при создании ачивок.
 */
public final class AchievementCatalog {
    private static final Map<String, String[]> CATALOG = new HashMap<>();

    static {
        // Простые (первый раз)
        put("FIRST_BUY",
            "Бизнесмен", "Businessman",
            "Купи первую клетку", "Buy your first cell");
        put("FIRST_BATTLE",
            "Начинающий мемолог", "Meme Beginner",
            "Участвуй в мем-баттле", "Participate in a meme battle");
        put("FIRST_TRADE",
            "Предприниматель", "Entrepreneur",
            "Заключи первую сделку", "Make your first trade");
        put("FIRST_HOUSE",
            "Время деньги", "Time is Money",
            "Построй первый филиал", "Build your first branch");
        put("FIRST_MORTGAGE",
            "Тяжёлые времена", "Hard Times",
            "Заложи первую клетку", "Mortgage your first cell");
        put("JAILBREAK",
            "Счастливчик", "Lucky",
            "Выйди из тюрьмы выбив дубль", "Escape jail with doubles");

        // Средние
        put("MONOPOLIST",
            "Начинающий монополист", "Aspiring Monopolist",
            "Собери полную группу одного цвета", "Own a complete color group");
        put("LANDLORD",
            "На опыте", "Experienced",
            "Имей 10+ клеток одновременно", "Own 10+ cells at once");
        put("BANKER",
            "Опытный экономист", "Skilled Economist",
            "Баланс в Meme Bank достиг 500", "Meme Bank balance reached 500");
        put("TRADING_POST",
            "Трейдер", "Trader",
            "5 успешных сделок за игру", "5 successful trades in one game");
        put("MEME_LORD",
            "Мем-лорд", "Meme Lord",
            "Выиграй 3 мем-баттла за игру", "Win 3 meme battles in one game");
        put("BUILDER",
            "Боб-строитель", "Bob the Builder",
            "Построй 4 филиала на одной клетке", "Build 4 branches on one cell");

        // Сложные
        put("TYCOON",
            "Магнат", "Tycoon",
            "Выиграй игру с 5000+ монет", "Win a game with 5000+ coins");
        put("FULL_SET",
            "Опытный монополист", "Master Monopolist",
            "Собери 3 разные монополии", "Own 3 complete color groups");
        put("NEGOTIATOR",
            "Дипломат", "Diplomat",
            "Заключи сделку с каждым игроком за игру", "Trade with every player in one game");
        put("UNSTOPPABLE",
            "Его уже не остановить", "Unstoppable",
            "Выиграй 3 игры подряд", "Win 3 games in a row");
    }

    private static void put(String id, String titleRu, String titleEn, String descRu, String descEn) {
        CATALOG.put(id, new String[] { titleRu, titleEn, descRu, descEn });
    }

    /**
     * @return [titleRu, titleEn, descriptionRu, descriptionEn] или null если id неизвестен
     */
    public static String[] get(String achievementId) {
        return CATALOG.get(achievementId);
    }

    /**
     * Все API-имена ачивок (для dashboard Steamworks).
     */
    public static String[] allIds() {
        return CATALOG.keySet().toArray(new String[0]);
    }
}

package com.memopoly.utils;

public final class TexturePathResolver {
    private TexturePathResolver() {}

    public static String resolveScreenTexture(String fileName, LanguageManager.Language language) {
        if (language == LanguageManager.Language.RU) {
            return "screen_ui_ru/" + mapRuScreenFile(fileName);
        }
        return "screen_ui_en/" + fileName;
    }

    public static String resolveMenuTexture(String fileName, LanguageManager.Language language) {
        if (language == LanguageManager.Language.RU) {
            return "menu_ru/" + mapRuMenuFile(fileName);
        }
        return "menu_en/" + fileName;
    }

    public static String resolveGameScreenTexture(String fileName, LanguageManager.Language language) {
        if (language == LanguageManager.Language.RU) {
            return "gamescreen_ui_ru/" + mapRuGameFile(fileName);
        }
        return "gamescreen_ui_en/" + fileName;
    }

    public static String resolveBoardMapTexture(LanguageManager.Language language) {
        return language == LanguageManager.Language.RU ? "gamescreen_ui_ru/map_ru.png" : "gamescreen_ui_en/map.png";
    }

    private static String mapRuScreenFile(String fileName) {
        return switch (fileName) {
            case "start_the_game_btn.png" -> "начать_игру_btn.png";
            case "copy_the_code_btn.png" -> "копировать_код_btn.png";
            case "create_btn.png" -> "создать_btn.png";
            case "connect_btn_for_window.png" -> "подключиться_btn.png";
            case "cancel_btn.png" -> "отмена_btn.png";
            case "back_btn.png" -> "назад_btn.png";
            default -> fileName;
        };
    }

    private static String mapRuMenuFile(String fileName) {
        return switch (fileName) {
            case "create_game_btn.png" -> "создать_игру_btn.png";
            case "connect_btn.png" -> "войти_btn.png";
            case "settings_btn.png" -> "настройки_btn.png";
            case "exit_btn.png" -> "выход_btn.png";
            default -> fileName;
        };
    }

    private static String mapRuGameFile(String fileName) {
        return switch (fileName) {
            case "buy_btn.png" -> "купить_btn.png";
            case "pass_btn.png" -> "пропустить_btn.png";
            case "end_of_turn_btn.png" -> "конец_хода_btn.png";
            case "make_a_bet_btn.png" -> "сделать_ставку_btn.png";
            case "mortgage_btn.png" -> "заложить_btn.png";
            case "reverse_mortgage_btn.png" -> "выкупить_btn.png";
            case "auction_btn.png" -> "аукцион_btn.png";
            case "cancel_btn.png" -> "отмена_btn.png";
            default -> fileName;
        };
    }
}

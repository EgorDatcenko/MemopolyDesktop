package com.memopoly.utils;

public final class TexturePathResolver {
    private TexturePathResolver() {}

    public static String resolveScreenTexture(String fileName, LanguageManager.Language language) {
        return "screen_ui_" + language.code() + "/" + normalizeScreenFileName(fileName);
    }

    public static String resolveMenuTexture(String fileName, LanguageManager.Language language) {
        return "menu_" + language.code() + "/" + fileName;
    }

    public static String resolveGameScreenTexture(String fileName, LanguageManager.Language language) {
        return "gamescreen_ui_" + language.code() + "/" + normalizeGameFileName(fileName, language);
    }

    public static String resolveBoardMapTexture(LanguageManager.Language language) {
        return language == LanguageManager.Language.RU ? "gamescreen_ui_ru/map_ru.png" : "gamescreen_ui_en/map.png";
    }

    private static String normalizeScreenFileName(String fileName) {
        return switch (fileName) {
            case "connect_btn_for_window.png" -> "connect_btn_for_window.png";
            default -> fileName;
        };
    }

    private static String normalizeGameFileName(String fileName, LanguageManager.Language language) {
        return switch (fileName) {
            case "make_a_bet_btn.png" -> "create_bet_btn.png";
            case "reverse_mortgage_btn.png" -> "unmortgage_btn.png";
            case "exit_to_menu.png" -> "exit_to_menu_" + language.code() + ".png";
            default -> fileName;
        };
    }
}

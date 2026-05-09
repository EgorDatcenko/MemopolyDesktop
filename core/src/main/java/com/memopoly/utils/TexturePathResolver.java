package com.memopoly.utils;

public final class TexturePathResolver {
    private TexturePathResolver() {}

    public static String resolveScreenTexture(String fileName, LanguageManager.Language language) {
        return "screen_ui_" + language.code() + "/" + fileName;
    }

    public static String resolveMenuTexture(String fileName, LanguageManager.Language language) {
        return "menu_" + language.code() + "/" + fileName;
    }

    public static String resolveGameScreenTexture(String fileName, LanguageManager.Language language) {
        return "gamescreen_ui_" + language.code() + "/" + fileName;
    }

    public static String resolveBoardMapTexture(LanguageManager.Language language) {
        return language == LanguageManager.Language.RU ? "gamescreen_ui_ru/map_ru.png" : "gamescreen_ui_en/map.png";
    }
}

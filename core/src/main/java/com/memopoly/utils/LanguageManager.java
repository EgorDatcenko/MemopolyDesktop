package com.memopoly.utils;

import com.badlogic.gdx.Preferences;

public class LanguageManager {
    public enum Language {
        EN, RU;

        public static Language fromCode(String code) {
            if (code == null) {
                return EN;
            }
            return "ru".equalsIgnoreCase(code) ? RU : EN;
        }

        public String code() {
            return name().toLowerCase();
        }
    }

    private static final String LANGUAGE_KEY = "language";

    private final Preferences preferences;

    public LanguageManager(Preferences preferences) {
        this.preferences = preferences;
    }

    public Language getLanguage() {
        return Language.fromCode(preferences.getString(LANGUAGE_KEY, Language.EN.code()));
    }

    public void setLanguage(Language language) {
        preferences.putString(LANGUAGE_KEY, language.code());
        preferences.flush();
    }
}

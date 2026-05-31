package com.memopoly.utils;

import com.badlogic.gdx.Gdx;

public final class AppLog {
    private AppLog() {}

    public static void info(String tag, String message) {
        Gdx.app.log(tag, message);
    }

    public static void warn(String tag, String message) {
        Gdx.app.error(tag, message);
    }

    public static void error(String tag, String message, Throwable throwable) {
        Gdx.app.error(tag, message, throwable);
    }
}

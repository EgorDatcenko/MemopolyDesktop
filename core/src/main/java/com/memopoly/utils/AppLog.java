package com.memopoly.utils;

import com.badlogic.gdx.Gdx;

public final class AppLog {
    private AppLog() {}

    public static void info(String tag, String message) {
        if (Gdx.app != null) {
            Gdx.app.log(tag, message);
        } else {
            System.out.println("[" + tag + "] " + message);
        }
    }

    public static void warn(String tag, String message) {
        if (Gdx.app != null) {
            Gdx.app.error(tag, message);
        } else {
            System.err.println("[" + tag + "] " + message);
        }
    }
}

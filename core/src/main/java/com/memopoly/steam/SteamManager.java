package com.memopoly.steam;

import com.codedisaster.steamworks.*;
import com.memopoly.utils.AppLog;
import com.codedisaster.steamworks.SteamUtils;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamNativeHandle;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public final class SteamManager {
    public static final int APP_ID = 480;

    private static boolean available = false;
    private static SteamFriends friends;
    private static SteamMatchmaking matchmaking;
    private static SteamUserStats stats;
    private static SteamUtils utils;
    private static SteamUser user;
    private SteamManager() {}

    public static boolean init() {
        if (available) return true;
        try {
            // Диагностика
            AppLog.info("Steam", "CWD: " + new File(".").getAbsolutePath());

            File appidFile = new File("steam_appid.txt");
            AppLog.info("Steam", "steam_appid.txt exists: " + appidFile.exists());
            AppLog.info("Steam", "steam_appid.txt path: " + appidFile.getAbsolutePath());
            if (appidFile.exists()) {
                try (java.util.Scanner sc = new java.util.Scanner(appidFile)) {
                    AppLog.info("Steam", "steam_appid.txt content: '" + sc.nextLine() + "'");
                } catch (Exception e) {}
            }

            SteamAPI.loadLibraries();
            AppLog.info("Steam", "loadLibraries OK");
            AppLog.info("Steam", "isSteamRunning = " + SteamAPI.isSteamRunning());

            //boolean running = SteamAPI.isSteamRunning();
            //AppLog.info("Steam", "isSteamRunning() = " + running);
            if (!SteamAPI.init()) {
                AppLog.warn("Steam", "SteamAPI.init() вернул false");
                return false;
            }

            if (!SteamAPI.init()) {
                AppLog.warn("Steam", "SteamAPI.init() вернул false");
                return false;
            }

            friends     = new SteamFriends(new FriendsCallback());
            matchmaking = new SteamMatchmaking(new MatchmakingCallback());
            stats       = new SteamUserStats(new UserStatsCallback());
            stats.requestCurrentStats(); // в 1.9.0 существует
            utils = new SteamUtils(new UtilsCallback());
            user  = new SteamUser(new UserCallback());

            available = true;
            AppLog.info("Steam", "Инициализирован: " + getPersonaName());
            return true;

        } catch (Throwable t) {
            AppLog.warn("Steam", "Steam недоступен: " + t);
            available = false;
            return false;
        }
    }

    public static SteamUtils getUtils() { return utils; }

    public static long getLocalSteamIdHandle() {
        if (!available || user == null) return 0L;
        try {
            return SteamNativeHandle.getNativeHandle(user.getSteamID());
        } catch (Throwable t) { return 0L; }
    }

    public static SteamUserStats getStats() {
        return stats;
    }

    public static void update() {
        if (available) SteamAPI.runCallbacks();
    }

    public static boolean isAvailable() {
        return available;
    }

    public static SteamFriends getFriends() {
        return friends;
    }

    public static String getPersonaName() {
        if (!available || friends == null) return null;
        try {
            return friends.getPersonaName();
        } catch (Throwable t) {
            return null;
        }
    }

    public static SteamMatchmaking getMatchmaking() {
        return matchmaking;
    }

    public static void shutdown() {
        if (available) {
            try { SteamAPI.shutdown(); } catch (Throwable ignored) {}
            available = false;
            friends = null;
            matchmaking = null;
        }
    }
}

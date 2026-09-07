package com.memopoly.steam;

import com.codedisaster.steamworks.*;
import com.memopoly.utils.AppLog;
import com.codedisaster.steamworks.SteamUtils;
import com.codedisaster.steamworks.SteamUser;
import com.codedisaster.steamworks.SteamNativeHandle;

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
            SteamAPI.loadLibraries();
            AppLog.info("Steam", "Рабочая папка (user.dir): " + System.getProperty("user.dir"));
            if (!SteamAPI.init()) {
                AppLog.warn("Steam", "SteamAPI.init() == false. Запущен ли Steam-клиент?");
                return false;
            }
            friends = new SteamFriends(new FriendsCallback());
            matchmaking = new SteamMatchmaking(new MatchmakingCallback());
            stats = new SteamUserStats(new UserStatsCallback());
            stats.requestCurrentStats();
            utils = new SteamUtils(new UtilsCallback());
            user = new SteamUser(new UserCallback());
            available = true;
            AppLog.info("Steam", "Steam initialized, user: " + getPersonaName());
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

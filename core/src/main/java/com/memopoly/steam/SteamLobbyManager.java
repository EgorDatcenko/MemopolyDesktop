package com.memopoly.steam;

import com.codedisaster.steamworks.*;
import com.memopoly.utils.AppLog;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamMatchmaking;
import com.memopoly.utils.AppLog;

/**
 * Обёртка над SteamMatchmaking: создание/вход в лобби, запись IP+порта KryoNet-сервера.
 * Храним объекты SteamID напрямую (конструктор SteamID(long) не публичный).
 */
public final class SteamLobbyManager {
    public static final int MAX_LOBBY_MEMBERS = 6;

    private static SteamID currentLobby = null;   // null = нет активного лобби
    private static String pendingHostIp = null;
    private static int pendingHostPort = 0;

    private SteamLobbyManager() {}

    /**
     * Хост запрашивает создание лобби; IP+порт запишем, когда Steam подтвердит создание.
     */
    public static void createLobby(String hostIp, int port) {
        if (!SteamManager.isAvailable()) {
            AppLog.warn("SteamLobby", "Steam недоступен, лобби не создано");
            return;
        }
        SteamMatchmaking mm = SteamManager.getMatchmaking();
        if (mm == null) return;

        pendingHostIp = hostIp;
        pendingHostPort = port;

        // Тип лобби — enum, а не int!
        mm.createLobby(SteamMatchmaking.LobbyType.FriendsOnly, MAX_LOBBY_MEMBERS);
        AppLog.info("SteamLobby", "Запрошено создание лобби для " + hostIp + ":" + port);
    }

    /**
     * Вызывается из MatchmakingCallback.onLobbyCreated — сохраняем объект SteamID
     * и сразу пишем данные подключения.
     */
    public static void onLobbyCreated(boolean success, SteamID lobby) {
        if (!success) {
            AppLog.warn("SteamLobby", "Не удалось создать лобби");
            return;
        }
        currentLobby = lobby;
        AppLog.info("SteamLobby", "Лобби создано: " + lobby.getAccountID());
        if (pendingHostIp != null) {
            setLobbyConnectionData(lobby, pendingHostIp, pendingHostPort);
            pendingHostIp = null;
        }
    }

    /**
     * Записывает IP+порт KryoNet-сервера в данные лобби.
     */
    public static void setLobbyConnectionData(SteamID lobby, String ip, int port) {
        if (!SteamManager.isAvailable()) return;
        SteamMatchmaking mm = SteamManager.getMatchmaking();
        if (mm == null) return;
        mm.setLobbyData(lobby, "ip", ip);
        mm.setLobbyData(lobby, "port", String.valueOf(port));
        AppLog.info("SteamLobby", "Записаны данные лобби: " + ip + ":" + port);
    }

    /**
     * Гость запрашивает вход в лобби (объект SteamID приходит из колбэка инвайта).
     */
    public static void joinLobby(SteamID lobby) {
        if (!SteamManager.isAvailable()) return;
        SteamMatchmaking mm = SteamManager.getMatchmaking();
        if (mm == null) return;
        mm.joinLobby(lobby);
        AppLog.info("SteamLobby", "Запрошен вход в лобби " + lobby.getAccountID());
    }

    /**
     * Читает IP+порт из данных лобби.
     */
    public static LobbyConnectionData getLobbyConnectionData(SteamID lobby) {
        if (!SteamManager.isAvailable()) return null;
        SteamMatchmaking mm = SteamManager.getMatchmaking();
        if (mm == null) return null;

        String ip = mm.getLobbyData(lobby, "ip");
        String portStr = mm.getLobbyData(lobby, "port");
        if (ip == null || ip.isEmpty() || portStr == null || portStr.isEmpty()) return null;

        try {
            return new LobbyConnectionData(ip, Integer.parseInt(portStr));
        } catch (NumberFormatException e) {
            AppLog.warn("SteamLobby", "Неверный порт в данных лобби: " + portStr);
            return null;
        }
    }

    public static SteamID getCurrentLobby() {
        return currentLobby;
    }

    public static void clearLobby() {
        currentLobby = null;
        pendingHostIp = null;
    }

    public static class LobbyConnectionData {
        public final String ip;
        public final int port;

        public LobbyConnectionData(String ip, int port) {
            this.ip = ip;
            this.port = port;
        }
    }
    public interface OnLobbyJoinCallback {
        void onJoin(String ip, int port);
    }

    private static OnLobbyJoinCallback joinCallback;

    public static void setJoinCallback(OnLobbyJoinCallback callback) {
        joinCallback = callback;
    }

    /**
     * Открывает системный диалог Steam для инвайта друзей в текущее лобби.
     */
    public static void inviteFriends() {
        if (!SteamManager.isAvailable() || currentLobby == null) {
            AppLog.warn("SteamLobby", "Нет активного лобби для инвайта");
            return;
        }
        SteamFriends friends = SteamManager.getFriends();
        if (friends != null) {
            friends.activateGameOverlayInviteDialog(currentLobby);
            AppLog.info("SteamLobby", "Открыт диалог инвайта друзей");
        }
    }

    /**
     * Вызывается из колбэка onLobbyInvite — автоконнект к серверу через данные лобби.
     */
    public static void onLobbyInvite(SteamID lobby) {
        AppLog.info("SteamLobby", "Получен инвайт в лобби " + lobby.getAccountID());
        LobbyConnectionData data = getLobbyConnectionData(lobby);
        if (data != null && joinCallback != null) {
            AppLog.info("SteamLobby", "Автоконнект к " + data.ip + ":" + data.port);
            joinCallback.onJoin(data.ip, data.port);
        }
    }
}

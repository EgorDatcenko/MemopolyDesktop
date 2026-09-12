package com.memopoly.steam;

import com.codedisaster.steamworks.*;
import com.memopoly.utils.AppLog;

public class MatchmakingCallback implements SteamMatchmakingCallback {

    @Override
    public void onFavoritesListChanged(int ip, int queryPort, int connPort, int appID, int flags, boolean add, int accountID) {

    }

    @Override
    public void onLobbyInvite(SteamID steamIDUser, SteamID steamIDLobby, long gameID) {
        SteamLobbyManager.onLobbyInvite(steamIDLobby);
    }

    @Override
    public void onLobbyEnter(SteamID steamIDLobby, int chatPermissions, boolean blocked, SteamMatchmaking.ChatRoomEnterResponse response) {
        AppLog.info("SteamLobby", "Вошли в лобби " + steamIDLobby.getAccountID() + ", response=" + response);
        // В Этапе B добавим чтение IP/порта и автоконнект к KryoNet-серверу
    }

    @Override
    public void onLobbyDataUpdate(SteamID steamIDLobby, SteamID steamIDMember, boolean success) {
        // Не используем пока
    }

    @Override
    public void onLobbyChatUpdate(SteamID steamIDLobby, SteamID steamIDUser, SteamID steamIDMakingChange, SteamMatchmaking.ChatMemberStateChange chatMemberStateChange) {
        // Не используем пока
    }

    @Override
    public void onLobbyChatMessage(SteamID steamIDLobby, SteamID steamIDUser, SteamMatchmaking.ChatEntryType entryType, int chatID) {
        // Не используем пока
    }

    @Override
    public void onLobbyGameCreated(SteamID steamIDLobby, SteamID steamIDGameServer, int ip, short port) {
        // Не используем пока
    }

    @Override
    public void onLobbyMatchList(int lobbiesMatching) {
        // Не используем пока
    }

    @Override
    public void onLobbyKicked(SteamID steamIDLobby, SteamID steamIDAdmin, boolean kickedDueToDisconnect) {
        AppLog.warn("SteamLobby", "Исключены из лобби " + steamIDLobby.getAccountID());
        SteamLobbyManager.clearLobby();
    }

    @Override
    public void onLobbyCreated(SteamResult result, SteamID steamIDLobby) {
        boolean success = (result == SteamResult.OK);
        SteamLobbyManager.onLobbyCreated(success, steamIDLobby);
    }

    @Override
    public void onFavoritesListAccountsUpdated(SteamResult result) {
        // Не используем
    }
}

package com.memopoly.steam;

import com.codedisaster.steamworks.*;

/**
 * Пустая реализация колбэков SteamUser — нам нужен только getSteamID().
 */
public class UserCallback implements SteamUserCallback {

    @Override
    public void onAuthSessionTicket(SteamAuthTicket authTicket, SteamResult result) {

    }

    @Override
    public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID ownerSteamID) {

    }

    @Override
    public void onMicroTxnAuthorization(int appID, long orderID, boolean authorized) {
    }

    public void onGetAuthSessionTicket(SteamAuthTicket authTicket, SteamResult result) {
    }

    @Override
    public void onEncryptedAppTicket(SteamResult result) {
    }
}

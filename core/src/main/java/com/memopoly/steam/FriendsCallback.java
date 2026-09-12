package com.memopoly.steam;


import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamFriendsCallback;
import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamResult;

public class FriendsCallback implements SteamFriendsCallback {

    @Override
    public void onPersonaStateChange(SteamID steamID, SteamFriends.PersonaChange personaChange) {
    }

    public void onGameOverlayActivated(boolean active) {
    }

    @Override
    public void onGameRichPresenceJoinRequested(SteamID steamID, String connect) {
    }

    @Override
    public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {
    }

    @Override
    public void onAvatarImageLoaded(SteamID steamID, int i, int i1, int i2) {

    }

    @Override
    public void onFriendRichPresenceUpdate(SteamID steamID, int i) {

    }

    @Override
    public void onGameServerChangeRequested(String server, String password) {
    }

    public void onSetPersonaNameResponse(boolean success, boolean localSuccess, SteamResult result) {
    }
}

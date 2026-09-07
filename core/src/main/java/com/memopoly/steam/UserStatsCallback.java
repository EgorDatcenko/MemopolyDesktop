package com.memopoly.steam;

import com.codedisaster.steamworks.SteamID;
import com.codedisaster.steamworks.SteamLeaderboardEntriesHandle;
import com.codedisaster.steamworks.SteamLeaderboardHandle;
import com.codedisaster.steamworks.SteamResult;
import com.codedisaster.steamworks.SteamUserStatsCallback;
import com.memopoly.utils.AppLog;

public class UserStatsCallback implements SteamUserStatsCallback {

    @Override
    public void onUserStatsReceived(long gameId, SteamID steamIDUser, SteamResult result) {
        AppLog.info("Steam", "Stats received: " + result);
    }

    @Override
    public void onUserStatsStored(long gameId, SteamResult result) {
        AppLog.info("Steam", "Stats stored: " + result);
    }

    @Override
    public void onUserStatsUnloaded(SteamID steamIDUser) {
    }

    @Override
    public void onUserAchievementStored(long gameId, boolean isGroupAchievement, String achievementName, int curProgress, int maxProgress) {
        AppLog.info("Steam", "Achievement stored: " + achievementName);
    }

    @Override
    public void onLeaderboardFindResult(SteamLeaderboardHandle leaderboard, boolean found) {
    }

    @Override
    public void onLeaderboardScoresDownloaded(SteamLeaderboardHandle leaderboard, SteamLeaderboardEntriesHandle entries, int numEntries) {
    }

    @Override
    public void onLeaderboardScoreUploaded(boolean success, SteamLeaderboardHandle leaderboard, int score, boolean scoreChanged, int globalRankNew, int globalRankPrevious) {
    }

    @Override
    public void onNumberOfCurrentPlayersReceived(boolean success, int players) {
    }

    @Override
    public void onGlobalStatsReceived(long gameId, SteamResult result) {
    }
}

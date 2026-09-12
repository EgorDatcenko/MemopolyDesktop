package com.memopoly.steam;

import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.codedisaster.steamworks.SteamFriends;
import com.codedisaster.steamworks.SteamID;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;

public final class SteamAvatarCache {
    private static final int SIZE = 64;
    private static final Map<Long, Texture> cache = new HashMap<>();
    private static final Map<Long, Boolean> unavailable = new HashMap<>();

    public static Texture getAvatar(long steamId) {
        if (steamId == 0 || !SteamManager.isAvailable()) return null;
        if (cache.containsKey(steamId)) return cache.get(steamId);
        if (unavailable.containsKey(steamId)) return null;
        try {
            SteamFriends friends = SteamManager.getFriends();
            SteamID id = SteamID.createFromNativeHandle(steamId);
            int handle = friends.getMediumFriendAvatar(id);
            if (handle <= 0) {
                friends.requestUserInformation(id, false);
                unavailable.put(steamId, true);
                return null;
            }
            ByteBuffer buf = ByteBuffer.allocateDirect(SIZE * SIZE * 4);
            if (!SteamManager.getUtils().getImageRGBA(handle, buf)) {
                unavailable.put(steamId, true);
                return null;
            }
            Pixmap pixmap = new Pixmap(SIZE, SIZE, Pixmap.Format.RGBA8888);
            ByteBuffer px = pixmap.getPixels();
            byte[] row = new byte[SIZE * 4];
            for (int y = 0; y < SIZE; y++) {
                buf.position(y * SIZE * 4);
                buf.get(row);
                px.position(y * SIZE * 4);
                px.put(row);
            }
            float c = (SIZE - 1) / 2f, r = SIZE / 2f; // круговая маска
            for (int y = 0; y < SIZE; y++) {
                for (int x = 0; x < SIZE; x++) {
                    float dx = x - c, dy = y - c;
                    if (dx * dx + dy * dy > r * r) {
                        px.position((y * SIZE + x) * 4 + 3);
                        px.put((byte) 0);
                    }
                }
            }
            px.position(0);
            Texture t = new Texture(pixmap);
            pixmap.dispose();
            cache.put(steamId, t);
            return t;
        } catch (Throwable t) {
            unavailable.put(steamId, true);
            return null;
        }
    }
}

package com.memopoly.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.HashMap;
import java.util.Map;

public final class UiShapes {
    private static final Map<String, Texture> cache = new HashMap<>();

    public static Texture circle(int radius, Color color) {
        String key = "c" + radius + "|" + color;
        Texture cached = cache.get(key);
        if (cached != null) return cached;
        int size = radius * 2;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float c = radius - 0.5f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - c, dy = y - c;
                if (dx * dx + dy * dy <= radius * radius) {
                    p.setColor(color);
                    p.drawPixel(x, y);
                }
            }
        }
        Texture t = new Texture(p);
        p.dispose();
        cache.put(key, t);
        return t;
    }

    public static Texture ring(int radius, int thickness, Color color) {
        String key = "r" + radius + "_" + thickness + "|" + color;
        Texture cached = cache.get(key);
        if (cached != null) return cached;
        int size = radius * 2;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float c = radius - 0.5f;
        float inner = radius - thickness;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - c, dy = y - c;
                float d2 = dx * dx + dy * dy;
                if (d2 <= radius * radius && d2 >= inner * inner) {
                    p.setColor(color);
                    p.drawPixel(x, y);
                }
            }
        }
        Texture t = new Texture(p);
        p.dispose();
        cache.put(key, t);
        return t;
    }
}

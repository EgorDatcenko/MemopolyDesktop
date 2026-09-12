package com.memopoly.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.HashMap;
import java.util.Map;

public final class UiShapes {
    private static final Map<String, Texture> cache = new HashMap<>();

    public static Texture circle(int radius, Color color) {
        if (color.a < 1f) {
            return circleSmooth(radius, color);
        }
        String key = "c" + radius + "|" + color;
        Texture cached = cache.get(key);
        if (cached != null)
            return cached;
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
        if (cached != null)
            return cached;
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

    public static Texture circleSmooth(int radius, Color color) {
        String key = "csm" + radius + "|" + color;
        Texture cached = cache.get(key);
        if (cached != null)
            return cached;
        int size = radius * 2;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        float c = radius - 0.5f;
        float scale = radius - 0.5f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - c, dy = y - c;
                float dist = Math.max(0f, (float) Math.sqrt(dx * dx + dy * dy));
                float edge = Math.max(0f, 1.0f - (dist - radius + 0.75f) / 0.75f);
                float alpha = color.a * edge;
                if (alpha > 0.0f) {
                    p.setColor(color.r, color.g, color.b, alpha);
                    p.drawPixel(x, y);
                }
            }
        }
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        cache.put(key, t);
        return t;
    }

    public static Texture glyphInfoIcon() {
        Texture cached = cache.get("info_glyph");
        if (cached != null)
            return cached;
        int size = 64;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        p.setColor(Color.valueOf("3E4362"));
        p.fillCircle(32, 32, 30);
        p.setColor(Color.WHITE);
        p.fillCircle(32, 19, 4);
        p.fillRectangle(29, 27, 6, 22);
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        cache.put("info_glyph", t);
        return t;
    }
}

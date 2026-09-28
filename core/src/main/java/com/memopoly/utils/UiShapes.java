package com.memopoly.utils;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;

import java.util.HashMap;
import java.util.Map;

public final class UiShapes {
    private static final Map<String, Texture> cache = new HashMap<>();

    private static boolean isValid(Texture t) {
        return t != null && t.getTextureObjectHandle() != 0;
    }

    public static Texture circle(int radius, Color color) {
        if (color.a < 1f) {
            return circleSmooth(radius, color);
        }
        String key = "c" + radius + "|" + color;
        Texture cached = cache.get(key);
        if (isValid(cached))
            return cached;
        int size = radius * 2;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
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
        if (isValid(cached))
            return cached;
        int size = radius * 2;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
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
        if (isValid(cached))
            return cached;
        int size = radius * 2;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        float c = radius - 0.5f;
        final float aa = 1.25f; // ширина полосы сглаживания в пикселях текстуры
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - c, dy = y - c;
                float dist = (float) Math.sqrt(dx * dx + dy * dy);
                // КЛЮЧ: edge строго в [0..1]: 1 внутри круга, плавный спад к краю, 0 снаружи
                float edge = Math.min(1f, Math.max(0f, (radius - dist) / aa));
                float alpha = color.a * edge;
                if (alpha > 0f) {
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
        if (isValid(cached))
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

    public static Texture getCornerBracketTexture() {
        Texture cached = cache.get("corner_bracket_28");
        if (isValid(cached))
            return cached;
        int size = 28;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        p.setColor(Color.BLACK);
        p.fillRectangle(0, 0, 22, 5);
        p.fillRectangle(0, 0, 5, 22);
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        cache.put("corner_bracket_28", t);
        return t;
    }

    public static Texture outlinedCircle(int radius, int strokeWidth, Color fillColor, Color strokeColor) {
        String key = "oc_" + radius + "_" + strokeWidth + "_" + fillColor + "_" + strokeColor;
        Texture cached = cache.get(key);
        if (isValid(cached))
            return cached;
        int size = radius * 2;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        float c = radius - 0.5f;
        float inner = radius - strokeWidth;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - c, dy = y - c;
                float d2 = dx * dx + dy * dy;
                if (d2 <= radius * radius) {
                    if (d2 >= inner * inner) {
                        p.setColor(strokeColor);
                    } else {
                        p.setColor(fillColor);
                    }
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

    public static Texture outlinedPill(int width, int height, int strokeWidth, Color fillColor, Color strokeColor) {
        String key = "op_" + width + "_" + height + "_" + strokeWidth + "_" + fillColor + "_" + strokeColor;
        Texture cached = cache.get(key);
        if (isValid(cached))
            return cached;
        Pixmap p = new Pixmap(width, height, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        float r = height / 2f;
        float cLeft = r - 0.5f;
        float cRight = width - r - 0.5f;
        float innerR = r - strokeWidth;

        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                float cy = r - 0.5f;
                float dy = y - cy;
                boolean inside = false;
                boolean isStroke = false;

                if (x < r) {
                    float dx = x - cLeft;
                    float d2 = dx * dx + dy * dy;
                    if (d2 <= r * r) {
                        inside = true;
                        if (d2 >= innerR * innerR) isStroke = true;
                    }
                } else if (x > width - r) {
                    float dx = x - cRight;
                    float d2 = dx * dx + dy * dy;
                    if (d2 <= r * r) {
                        inside = true;
                        if (d2 >= innerR * innerR) isStroke = true;
                    }
                } else {
                    inside = true;
                    if (y < strokeWidth || y >= height - strokeWidth) {
                        isStroke = true;
                    }
                }

                if (inside) {
                    p.setColor(isStroke ? strokeColor : fillColor);
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

    public static Texture statusConnected(int size) {
        String key = "sc_" + size;
        Texture cached = cache.get(key);
        if (isValid(cached))
            return cached;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        p.setColor(Color.valueOf("2ECC71"));
        p.fillCircle(size / 2, size / 2, size / 2 - 1);
        p.setColor(Color.WHITE);
        int x1 = (int) (size * 0.28f), y1 = (int) (size * 0.50f);
        int x2 = (int) (size * 0.44f), y2 = (int) (size * 0.68f);
        int x3 = (int) (size * 0.74f), y3 = (int) (size * 0.32f);
        for (int w = -1; w <= 1; w++) {
            p.drawLine(x1, y1 + w, x2, y2 + w);
            p.drawLine(x2, y2 + w, x3, y3 + w);
            p.drawLine(x1 + w, y1, x2 + w, y2);
            p.drawLine(x2 + w, y2, x3 + w, y3);
        }
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        cache.put(key, t);
        return t;
    }

    public static Texture statusError(int size) {
        String key = "se_" + size;
        Texture cached = cache.get(key);
        if (isValid(cached))
            return cached;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        p.setColor(Color.valueOf("E74C3C"));
        p.fillCircle(size / 2, size / 2, size / 2 - 1);
        p.setColor(Color.WHITE);
        int p1 = (int) (size * 0.30f);
        int p2 = (int) (size * 0.70f);
        for (int w = -1; w <= 1; w++) {
            p.drawLine(p1, p1 + w, p2, p2 + w);
            p.drawLine(p1, p2 + w, p2, p1 + w);
            p.drawLine(p1 + w, p1, p2 + w, p2);
            p.drawLine(p1 + w, p2, p2 + w, p1);
        }
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        cache.put(key, t);
        return t;
    }

    public static Texture statusConnecting(int size) {
        String key = "scon_" + size;
        Texture cached = cache.get(key);
        if (isValid(cached))
            return cached;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(0f, 0f, 0f, 0f);
        p.fill();
        p.setColor(Color.valueOf("F1C40F"));
        p.fillCircle(size / 2, size / 2, size / 2 - 1);
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        cache.put(key, t);
        return t;
    }

    public static void disposeAll() {
        for (Texture tex : cache.values()) {
            if (tex != null) {
                tex.dispose();
            }
        }
        cache.clear();
    }
}

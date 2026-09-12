package com.memopoly.utils;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;

public final class UiFonts {
    public static BitmapFont create(String internalPath, int targetSize) {
        FreeTypeFontGenerator g = new FreeTypeFontGenerator(Gdx.files.internal(internalPath));
        FreeTypeFontGenerator.FreeTypeFontParameter p = new FreeTypeFontGenerator.FreeTypeFontParameter();
        p.size = targetSize * 3; // текстура в 3 раза крупнее
        p.magFilter = Texture.TextureFilter.Linear;
        p.minFilter = Texture.TextureFilter.Linear;
        p.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя№\u2026…";
        BitmapFont font = g.generateFont(p);
        font.getData().setScale(1f / 3f);
        font.setUseIntegerPositions(false);
        g.dispose();
        return font;
    }
}

package com.memopoly.game.model;

import com.badlogic.gdx.utils.Array;

/**
 * Управляет колодой мемов: отвечает за стопку добора и сброса, перемешивание и
 * выдачу карт игрокам.
 */
public class MemeDeck {
    public String name;
    public Array<Meme> memes = new Array<>();
    public transient java.util.List<com.badlogic.gdx.graphics.Texture> dynamicTextures = new java.util.ArrayList<>();
    public transient com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> coverAnimation;

    public MemeDeck() {
    }

    public MemeDeck(String name) {
        this.name = name;
    }

    public String getPreviewImagePath() {
        if (memes == null || memes.size == 0) {
            return null;
        }
        Meme first = memes.first();
        return first == null ? null : first.imageUrl;
    }

    public com.badlogic.gdx.graphics.g2d.Animation<com.badlogic.gdx.graphics.g2d.TextureRegion> getCoverAnimation() {
        if (coverAnimation != null) {
            return coverAnimation;
        }
        if (memes != null && memes.size > 0 && memes.first() != null) {
            return memes.first().animation;
        }
        return null;
    }

    public void dispose() {
        if (dynamicTextures != null) {
            for (com.badlogic.gdx.graphics.Texture tex : dynamicTextures) {
                if (tex != null) {
                    try {
                        tex.dispose();
                    } catch (Exception ignored) {
                    }
                }
            }
            dynamicTextures.clear();
        }
    }
}

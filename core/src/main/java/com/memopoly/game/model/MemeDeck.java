package com.memopoly.game.model;

import com.badlogic.gdx.utils.Array;

/**
 * Управляет колодой мемов: отвечает за стопку добора и сброса, перемешивание и выдачу карт игрокам.
 */
public class MemeDeck {
    public String name;
    public Array<Meme> memes = new Array<>();

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
}

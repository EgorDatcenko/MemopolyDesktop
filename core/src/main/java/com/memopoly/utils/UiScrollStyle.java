package com.memopoly.utils;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.utils.BaseDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.memopoly.utils.LanguageManager.Language;

/**
 * Единая точка стилизации скроллбаров: тонкий трек + крупный круглый ползунок.
 * Текстуры кешируются статически и не диспозятся.
 */
public final class UiScrollStyle {
    private static final String TRACK_TEXTURE_PATH = "scroll.png";
    private static final String KNOB_TEXTURE_PATH = "screen_ui/knob.png";
    private static final float KNOB_SCALE = 3f; // ползунок в KNOB_SCALE раз толще трека

    private static Texture trackTexture;
    private static Texture knobTexture;

    private UiScrollStyle() {}

    public static void apply(ScrollPane scroll, Language language) {
        if (trackTexture == null) {
            trackTexture = new Texture(TRACK_TEXTURE_PATH);
            trackTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }
        if (knobTexture == null) {
            knobTexture = new Texture(KNOB_TEXTURE_PATH);
            knobTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        }

        ScrollPane.ScrollPaneStyle style = new ScrollPane.ScrollPaneStyle(scroll.getStyle());
        style.background = null;

        // Полоса: в два раза тоньше исходной текстуры
        TextureRegionDrawable track = new TextureRegionDrawable(new TextureRegion(trackTexture));
        float barWidth = Math.max(2f, trackTexture.getWidth() * 0.5f);
        track.setMinWidth(barWidth);
        track.setMinHeight(barWidth);
        style.vScroll = track;
        style.hScroll = track;

        // Ползунок: крупный квадрат (круг) с центром на треке;
        // визуальный размер фиксированный и не зависит от bounds скролл-панели
        float knobSide = barWidth * KNOB_SCALE;

        SquareKnobDrawable vKnob = new SquareKnobDrawable(new TextureRegion(knobTexture), knobSide);
        vKnob.setMinWidth(barWidth);
        vKnob.setMinHeight(barWidth);
        style.vScrollKnob = vKnob;

        SquareKnobDrawable hKnob = new SquareKnobDrawable(new TextureRegion(knobTexture), knobSide);
        hKnob.setMinWidth(barWidth);
        hKnob.setMinHeight(barWidth);
        style.hScrollKnob = hKnob;

        scroll.setStyle(style);
    }

    /**
     * Drawable ползунка: игнорирует переданные bounds и всегда рисует текстуру
     * квадратом фиксированного размера side, центрируя его по центру bounds —
     * позиция при скролле сохраняется, круг никогда не сжимается.
     */
    private static final class SquareKnobDrawable extends BaseDrawable {
        private final TextureRegion region;
        private final float side;

        SquareKnobDrawable(TextureRegion region, float side) {
            this.region = region;
            this.side = side;
        }

        @Override
        public void draw(Batch batch, float x, float y, float width, float height) {
            float cx = x + width / 2f;
            float cy = y + height / 2f;
            batch.draw(region, cx - side / 2f, cy - side / 2f, side, side);
        }
    }
}

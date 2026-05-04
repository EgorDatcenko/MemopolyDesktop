package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.memopoly.Memopoly;

import java.util.function.Supplier;

public class LoadingScreen extends BaseScreen {
    private final Supplier<BaseScreen> nextScreenSupplier;
    private final String title;
    private final float minDurationSeconds;

    private ShapeRenderer shapeRenderer;
    private float elapsed;
    private boolean switched;

    public LoadingScreen(Memopoly game, String title, Supplier<BaseScreen> nextScreenSupplier) {
        this(game, title, nextScreenSupplier, 0.6f);
    }

    public LoadingScreen(Memopoly game, String title, Supplier<BaseScreen> nextScreenSupplier, float minDurationSeconds) {
        super(game);
        this.title = title;
        this.nextScreenSupplier = nextScreenSupplier;
        this.minDurationSeconds = minDurationSeconds;
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        elapsed = 0f;
        switched = false;
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float progress = Math.min(1f, elapsed / minDurationSeconds);
        drawProgressBar(progress);

        if (!switched && progress >= 1f) {
            switched = true;
            game.screenManager.set(nextScreenSupplier.get());
        }
    }

    private void drawProgressBar(float progress) {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float barWidth = screenW * 0.42f;
        float barHeight = 24f;
        float x = (screenW - barWidth) / 2f;
        float y = screenH * 0.48f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.18f, 0.16f, 0.24f, 1f);
        shapeRenderer.rect(x, y, barWidth, barHeight);
        shapeRenderer.setColor(0.97f, 0.83f, 0.25f, 1f);
        shapeRenderer.rect(x + 3f, y + 3f, (barWidth - 6f) * progress, barHeight - 6f);
        shapeRenderer.end();

        game.getBatch().begin();
        game.getBatch().end();
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
    }
}

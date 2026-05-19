package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Matrix4;
import com.memopoly.Memopoly;

import java.util.function.Supplier;

public class LoadingScreen extends BaseScreen {
    private static final String BACKGROUND_TEXTURE_PATH = "background.png";

    private final Supplier<BaseScreen> nextScreenSupplier;
    private final String title;
    private final float minDurationSeconds;
    private final Matrix4 screenProjection = new Matrix4();

    private ShapeRenderer shapeRenderer;
    private Texture backgroundTexture;
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
        backgroundTexture = loadTexture(BACKGROUND_TEXTURE_PATH);
        elapsed = 0f;
        switched = false;
    }

    @Override
    public void render(float delta) {
        elapsed += delta;

        Gdx.gl.glClearColor(0.08f, 0.08f, 0.12f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        drawBackground();

        float progress = Math.min(1f, elapsed / minDurationSeconds);
        drawProgressBar(progress);

        if (!switched && progress >= 1f) {
            switched = true;
            game.screenManager.set(nextScreenSupplier.get());
        }
    }

    private void drawBackground() {
        screenProjection.setToOrtho2D(0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.getBatch().setProjectionMatrix(screenProjection);
        game.getBatch().begin();
        game.getBatch().draw(backgroundTexture, 0f, 0f, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.getBatch().end();
    }

    private void drawProgressBar(float progress) {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float barWidth = screenW * 0.42f;
        float barHeight = 24f;
        float x = (screenW - barWidth) / 2f;
        float y = screenH * 0.48f;

        shapeRenderer.setProjectionMatrix(screenProjection);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        shapeRenderer.setColor(0.18f, 0.16f, 0.24f, 1f);
        shapeRenderer.rect(x, y, barWidth, barHeight);
        shapeRenderer.setColor(0.97f, 0.83f, 0.25f, 1f);
        shapeRenderer.rect(x + 3f, y + 3f, (barWidth - 6f) * progress, barHeight - 6f);
        shapeRenderer.end();
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    @Override
    public void dispose() {
        if (shapeRenderer != null) {
            shapeRenderer.dispose();
        }
        if (backgroundTexture != null) {
            backgroundTexture.dispose();
        }
    }
}

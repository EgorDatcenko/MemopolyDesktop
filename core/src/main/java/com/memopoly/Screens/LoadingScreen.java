package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.memopoly.Memopoly;
import com.memopoly.assets.Assets;

public class LoadingScreen extends BaseScreen {

    private ShapeRenderer shapeRenderer;
    private Assets assets;
    public LoadingScreen(Memopoly game) {
        super(game);
    }

    @Override
    public void show() {
        shapeRenderer = new ShapeRenderer();
        assets.loadAll(); // запускаем загрузку
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.1f, 0.1f, 0.15f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        float progress = assets.update(); // обновляем загрузку

        drawProgressBar(progress);

        if (assets.isLoaded()) {
            game.screenManager.set(new MainMenuScreen(game));
        }
    }

    private void drawProgressBar(float progress) {
        float screenW = Gdx.graphics.getWidth();
        float screenH = Gdx.graphics.getHeight();
        float barWidth = screenW * 0.6f;
        float barHeight = 20f;
        float x = (screenW - barWidth) / 2f;
        float y = screenH / 2f - barHeight / 2f;

        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);

        // Фон прогресс-бара
        shapeRenderer.setColor(0.3f, 0.3f, 0.3f, 1f);
        shapeRenderer.rect(x, y, barWidth, barHeight);

        // Заполненная часть
        shapeRenderer.setColor(0.2f, 0.6f, 1f, 1f);
        shapeRenderer.rect(x, y, barWidth * progress, barHeight);

        shapeRenderer.end();
    }

    @Override
    public void dispose() {
        shapeRenderer.dispose();
    }
}

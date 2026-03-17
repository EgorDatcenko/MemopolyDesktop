package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.memopoly.Memopoly;

public class GameScreen extends BaseScreen{

    private final Stage stage;
    private final ScreenManager screenManager;
    private BoardRenderer boardRenderer;
    public GameScreen(Memopoly game){
        super(game);
        stage = new Stage();
        screenManager = new ScreenManager(game);
        Gdx.input.setInputProcessor(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        boardRenderer.render(delta);
    }
    @Override
    public void resize(int width, int height) {
        boardRenderer.resize(width, height);
    }
}

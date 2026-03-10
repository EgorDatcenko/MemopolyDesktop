package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Screen;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.memopoly.Memopoly;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;

public class LobbyScreen implements Screen {
    private final Memopoly game;
    private final Stage stage;
    private VisLabel statusLabel;
    private Table playersTable;
    private VisTextButton startButton;
    private int lastPlayersCount = -1;

    public LobbyScreen(Memopoly game) {
        this.game = game;
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);
        createUI();
    }

    private void createUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(20);

        VisLabel title = new VisLabel("Комната ожидания");
        VisLabel roomCode = new VisLabel("Код: " + game.getRoomCode());
        statusLabel = new VisLabel("Ожидаем игроков...");

        playersTable = new Table();
        ScrollPane playersScroll = new ScrollPane(playersTable, VisUI.getSkin());
        playersScroll.setFadeScrollBars(false);

        startButton = new VisTextButton("Начать игру");
        startButton.setVisible(game.isHost());
        startButton.setDisabled(true);
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.startGameAsHost();
            }
        });

        VisTextButton backButton = new VisTextButton("Назад");
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showExitDialog();
            }
        });

        root.add(title).left().row();
        root.add(roomCode).left().padTop(6).row();
        root.add(statusLabel).left().padTop(6).row();
        root.add(playersScroll).growX().height(260).padTop(12).row();

        Table buttons = new Table();
        buttons.add(startButton).padRight(10);
        buttons.add(backButton);
        root.add(buttons).left().padTop(12);

        stage.addActor(root);
    }

    private void showExitDialog() {
        Dialog dialog = new Dialog("Выйти из комнаты?", VisUI.getSkin()) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    game.leaveRoomToMenu();
                }
            }
        };

        dialog.text("Вы точно хотите выйти из комнаты?");
        dialog.button("Выйти", true);
        dialog.button("Отмена", false);
        dialog.show(stage);
    }

    private void rebuildPlayers(GameState state) {
        playersTable.clearChildren();
        if (state == null || state.players == null) {
            return;
        }

        for (Player p : state.players) {
            playersTable.add(new VisLabel(p.name + " | $" + p.money)).left().row();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        GameState state = game.getLatestGameState();
        int count = (state == null || state.players == null) ? 0 : state.players.size();

        if (count != lastPlayersCount) {
            rebuildPlayers(state);
            lastPlayersCount = count;
        }

        if (state != null && state.currentPhase == GameState.GamePhase.PLAYING) {
            statusLabel.setText("Игра запущена. Переход на GameScreen - следующий шаг.");
        } else {
            statusLabel.setText("Игроков в комнате: " + count);
        }

        if (game.isHost()) {
            startButton.setDisabled(count < 2);
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void show() {
    }

    @Override
    public void hide() {
    }

    @Override
    public void pause() {
    }

    @Override
    public void resume() {
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}

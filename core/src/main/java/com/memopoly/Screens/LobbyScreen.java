package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.memopoly.Memopoly;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.StartGameRequest;
import com.memopoly.utils.ClipboardUtils;

public class LobbyScreen extends BaseScreen {
    private static final Color BACKGROUND_COLOR = new Color(0.10f, 0.10f, 0.17f, 1f);
    private static final Color PANEL_COLOR = new Color(0.18f, 0.16f, 0.27f, 0.98f);
    private static final Color PANEL_SHADOW = new Color(0.06f, 0.05f, 0.10f, 0.95f);
    private static final Color TITLE_COLOR = new Color(1.00f, 0.83f, 0.25f, 1f);
    private static final Color SUBTITLE_COLOR = new Color(0.82f, 0.80f, 0.88f, 1f);
    private static final Color PRIMARY_BUTTON = new Color(0.96f, 0.55f, 0.16f, 1f);
    private static final Color SECONDARY_BUTTON = new Color(0.24f, 0.74f, 0.98f, 1f);
    private static final Color DANGER_BUTTON = new Color(0.82f, 0.25f, 0.24f, 1f);
    private static final String BACKGROUND_TEXTURE_PATH = "background.png";
    private static final String START_BUTTON_TEXTURE_PATH = "start_the_game_btn.png";
    private static final String COPY_BUTTON_TEXTURE_PATH = "copy_the_code_btn.png";
    private static final String BACK_BUTTON_TEXTURE_PATH = "back_btn.png";
    private static final String CANCEL_BUTTON_TEXTURE_PATH = "cancel_btn.png";

    private final Stage stage;
    private final Texture backgroundTexture;
    private final Texture startButtonTexture;
    private final Texture copyButtonTexture;
    private final Texture backButtonTexture;
    private final Texture cancelButtonTexture;
    private VisLabel statusLabel;
    private Table playersTable;
    private ImageButton startButton;
    private int lastPlayersCount = -1;
    private boolean gameStarted = false;

    public LobbyScreen(Memopoly game) {
        super(game);
        stage = new Stage(new ScreenViewport());
        backgroundTexture = loadTexture(BACKGROUND_TEXTURE_PATH);
        startButtonTexture = loadTexture(START_BUTTON_TEXTURE_PATH);
        copyButtonTexture = loadTexture(COPY_BUTTON_TEXTURE_PATH);
        backButtonTexture = loadTexture(BACK_BUTTON_TEXTURE_PATH);
        cancelButtonTexture = loadTexture(CANCEL_BUTTON_TEXTURE_PATH);
        Gdx.input.setInputProcessor(stage);
        createUI();
    }

    private void createUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(28);

        Table shadowPanel = new Table();
        shadowPanel.setBackground(panel(PANEL_SHADOW));
        shadowPanel.pad(18f);

        Table panel = new Table();
        panel.setBackground(panel(PANEL_COLOR));
        panel.pad(24f, 28f, 24f, 28f);

        VisLabel title = new VisLabel("Комната ожидания");
        title.setFontScale(1.9f);
        title.setColor(TITLE_COLOR);

        VisLabel roomCode = new VisLabel("Код комнаты: " + game.getRoomCode());
        roomCode.setColor(SUBTITLE_COLOR);
        statusLabel = new VisLabel("Ожидаем игроков...");
        statusLabel.setColor(new Color(0.94f, 0.91f, 0.76f, 1f));

        playersTable = new Table();
        playersTable.top().left();
        ScrollPane playersScroll = new ScrollPane(playersTable, VisUI.getSkin());
        playersScroll.setFadeScrollBars(false);
        playersScroll.setScrollingDisabled(true, false);
        playersScroll.getStyle().background = panel(new Color(0.13f, 0.12f, 0.20f, 0.95f));

        startButton = createImageButton(startButtonTexture);
        startButton.setVisible(game.isHost());
        startButton.setDisabled(true);
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                StartGameRequest request = new StartGameRequest();
                game.getClient().sendStartGame(request);
            }
        });

        ImageButton copyCodeButton = createImageButton(copyButtonTexture);
        copyCodeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ClipboardUtils.copyToClipboard(game.getRoomCode());
            }
        });

        ImageButton backButton = createImageButton(backButtonTexture);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showExitDialog();
            }
        });

        VisLabel playersTitle = new VisLabel("Игроки");
        playersTitle.setColor(TITLE_COLOR);
        playersTitle.setFontScale(1.2f);

        panel.add(title).left().row();
        panel.add(roomCode).left().padTop(8f).row();
        panel.add(statusLabel).left().padTop(8f).padBottom(18f).row();
        panel.add(playersTitle).left().padBottom(10f).row();
        panel.add(playersScroll).width(540f).height(300f).row();

        Table buttons = new Table();
        buttons.add(startButton).width(240f).height(64f).padRight(12f);
        buttons.add(copyCodeButton).width(240f).height(64f).padRight(12f);
        buttons.add(backButton).width(170f).height(64f);
        panel.add(buttons).left().padTop(18f);

        shadowPanel.add(panel);
        root.add(shadowPanel).center();

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
        dialog.getButtonTable().clearChildren();
        dialog.getButtonTable().defaults().pad(10f);
        ImageButton backButton = createImageButton(backButtonTexture);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.leaveRoomToMenu();
                dialog.hide();
            }
        });
        ImageButton cancelButton = createImageButton(cancelButtonTexture);
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(backButton).size(170f, 58f);
        dialog.getButtonTable().add(cancelButton).size(176f, 58f);
        dialog.show(stage);
    }

    private void rebuildPlayers(GameState state) {
        playersTable.clearChildren();
        if (state == null || state.players == null) {
            return;
        }

        for (Player p : state.players) {
            Table row = new Table();
            row.setBackground(panel(new Color(0.22f, 0.20f, 0.33f, 0.95f)));
            row.pad(10f, 12f, 10f, 12f);

            VisLabel name = new VisLabel(p.name);
            name.setColor(Color.WHITE);
            VisLabel money = new VisLabel("$" + p.money);
            money.setColor(new Color(0.99f, 0.83f, 0.29f, 1f));

            row.add(name).expandX().left();
            row.add(money).right();
            playersTable.add(row).growX().padBottom(8f).row();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().draw(backgroundTexture, 0f, 0f, stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        stage.getBatch().end();

        GameState state = game.getLatestGameState();
        int count = (state == null || state.players == null) ? 0 : state.players.size();

        if (count != lastPlayersCount) {
            rebuildPlayers(state);
            lastPlayersCount = count;
        }

        if (state != null && state.currentPhase == GameState.GamePhase.PLAYING) {
            if (!gameStarted) {
                gameStarted = true;
                game.openGameLoading();
            }
        } else {
            statusLabel.setText("Игроков в комнате: " + count);
        }
        if (game.isHost()) {
            startButton.setDisabled(count < 2);
        }

        stage.act(delta);
        stage.draw();
    }

    private Drawable panel(Color color) {
        return VisUI.getSkin().newDrawable("white", color);
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private ImageButton createImageButton(Texture texture) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = drawable;
        style.imageOver = drawable.tint(new Color(1f, 1f, 1f, 0.96f));
        style.imageDown = drawable.tint(new Color(0.86f, 0.86f, 0.86f, 1f));
        style.imageDisabled = drawable.tint(new Color(0.45f, 0.45f, 0.45f, 1f));
        Drawable transparent = panel(new Color(1f, 1f, 1f, 0f));
        style.up = transparent;
        style.over = transparent;
        style.down = transparent;
        style.disabled = transparent;
        return new ImageButton(style);
    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        startButtonTexture.dispose();
        copyButtonTexture.dispose();
        backButtonTexture.dispose();
        cancelButtonTexture.dispose();
        stage.dispose();
    }
}

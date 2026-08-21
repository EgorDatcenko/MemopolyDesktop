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
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.memopoly.Memopoly;
import com.memopoly.utils.LanguageManager.Language;
import com.memopoly.utils.TexturePathResolver;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.MemeDeck;
import com.memopoly.game.model.Player;
import com.memopoly.modding.DeckRepository;
import com.memopoly.network.packets.StartGameRequest;
import com.memopoly.utils.ClipboardUtils;

/**
 * Экран комнаты ожидания: показывает список подключённых игроков, позволяет выбирать колоду и запускать матч.
 */
public class LobbyScreen extends BaseScreen {
    private static final float COMMON_BUTTON_HEIGHT = 64f;
    private static final float LOBBY_WINDOW_ASPECT = 930f / 550f;
    private static final float LOBBY_WINDOW_WIDTH = 1116f;
    private static final float LOBBY_WINDOW_HEIGHT = LOBBY_WINDOW_WIDTH / LOBBY_WINDOW_ASPECT;
    private static final float EXIT_DIALOG_SCALE = 0.6f;
    private static final float EXIT_DIALOG_TEXT_WIDTH = 440f;
    private static final float EXIT_DIALOG_BUTTON_BOTTOM_PADDING = 30f;
    private static final Color BACKGROUND_COLOR = new Color(0.10f, 0.10f, 0.17f, 1f);
    private static final Color TEXT_DARK = new Color(0.00f, 0.04f, 0.24f, 1f);   // #000A3E
    private static final Color MONEY_COLOR = new Color(0.85f, 0.62f, 0.09f, 1f);
    private static final String BACKGROUND_TEXTURE_PATH = "background.png";
    private static final String CHOOSE_DECK_BTN_TEXTURE_PATH = "choose_deck_btn.png";
    private static final String CHOOSE_DECK_FALLBACK_TEXTURE_PATH = "choose_deck.png";
    private static final String START_BUTTON_TEXTURE_PATH = "start_the_game_btn.png";
    private static final String COPY_BUTTON_TEXTURE_PATH = "copy_the_code_btn.png";
    private static final String NO_BUTTON_TEXTURE_PATH = "no_btn.png";
    private static final String CANCEL_BUTTON_TEXTURE_PATH = "cancel_btn.png";
    private static final String LOBBY_WINDOW_TEXTURE_PATH = "lobby_window.png";
    private static final String GAME_OVERLAY_WINDOW_TEXTURE_PATH = "game_overlay_window.png";
    private static final String YES_BUTTON_TEXTURE_PATH = "yes_btn.png";

    private final Stage stage;
    private final Texture backgroundTexture;
    private final Texture chooseDeckBtnTexture;
    private final Texture startButtonTexture;
    private final Texture copyButtonTexture;
    private final Texture noButtonTexture;
    private final Texture cancelButtonTexture;
    private final Texture yesButtonTexture;
    private final Texture lobbyWindowTexture;
    private final Texture gameOverlayWindowTexture;
    private VisLabel statusLabel;
    private Table playersTable;
    private ImageButton startButton;
    //private ChatWidget chatWidget;
    private VisLabel selectedDeckLabel;
    private String selectedDeckName;
    private int lastPlayersCount = -1;
    private boolean gameStarted = false;
    private final Language language;
    private final DeckRepository deckRepository = new DeckRepository();

    public LobbyScreen(Memopoly game) {
        super(game);
        stage = new Stage(new ScreenViewport());
        language = game.getLanguageManager().getLanguage();
        backgroundTexture = loadTexture(BACKGROUND_TEXTURE_PATH);
        chooseDeckBtnTexture = loadTextureWithFallback(
            TexturePathResolver.resolveScreenTexture(CHOOSE_DECK_BTN_TEXTURE_PATH, language),
            TexturePathResolver.resolveScreenTexture(CHOOSE_DECK_FALLBACK_TEXTURE_PATH, language));
        startButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(START_BUTTON_TEXTURE_PATH, language));
        copyButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(COPY_BUTTON_TEXTURE_PATH, language));
        noButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(NO_BUTTON_TEXTURE_PATH, language));
        cancelButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CANCEL_BUTTON_TEXTURE_PATH, language));
        yesButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(YES_BUTTON_TEXTURE_PATH, language));
        lobbyWindowTexture = loadTexture(LOBBY_WINDOW_TEXTURE_PATH);
        gameOverlayWindowTexture = loadTexture(GAME_OVERLAY_WINDOW_TEXTURE_PATH);
        Gdx.input.setInputProcessor(stage);
        createUI();
    }

    private void createUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(28);

        Table panel = new Table();
        panel.setBackground(window(lobbyWindowTexture));
        panel.pad(30f, 40f, 30f, 40f);

        // Заголовок по центру + кнопка выхода в правом верхнем углу
        VisLabel title = new VisLabel(t("lobby"));
        title.setFontScale(1.6f);

        ImageButton closeButton = createImageButton(cancelButtonTexture);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showExitDialog();
            }
        });

        Table titleRow = new Table();
        titleRow.add().expandX();
        titleRow.add(title).padLeft(100f);
        titleRow.add().expandX();
        titleRow.add(closeButton).size(90f, 80f);
        panel.add(titleRow).growX().padBottom(50f).row();

        // Код комнаты по центру
        VisLabel roomCode = new VisLabel(t("code") + ": " + game.getRoomCode());
        roomCode.setFontScale(0.8f);
        panel.add(roomCode).center().padBottom(20f).row();

        playersTable = new Table();
        playersTable.top().left();
        ScrollPane playersScroll = new ScrollPane(playersTable, VisUI.getSkin());
        playersScroll.setFadeScrollBars(true);            // полоса прокрутки видна только во время скролла
        playersScroll.setScrollingDisabled(true, false);
        playersScroll.setOverscroll(false, false);        // без «резинового» смещения контента

        ScrollPane.ScrollPaneStyle scrollStyle = new ScrollPane.ScrollPaneStyle(playersScroll.getStyle());
        scrollStyle.background = null;
        playersScroll.setStyle(scrollStyle);

        Table playersBox = new Table();
        playersBox.setBackground(window(gameOverlayWindowTexture));
        playersBox.pad(20f);                              // подбери под толщину рамки текстуры
        playersBox.add(playersScroll).grow();
        panel.add(playersBox).width(640f).height(270f).center().padBottom(24f).row();

        startButton = createImageButton(startButtonTexture);
        startButton.setVisible(game.isHost());
        startButton.setDisabled(true);
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                StartGameRequest request = new StartGameRequest();
                request.deckName = selectedDeckName;
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

        statusLabel = new VisLabel(t("waiting_players")); // пока не выводим на экран, оставляем на будущее

        // Нижний ряд: START | COPY CODE | ... | DECK: [кнопка колоды]
        Table bottomRow = new Table();
        bottomRow.left();
        bottomRow.add(startButton).size(220f, 110f).padRight(16f);
        bottomRow.add(copyCodeButton).size(220f, 110f);
        bottomRow.add().expandX();
        if (game.isHost()) {
            VisLabel deckTitle = new VisLabel(t("selected_deck") + ":");
            bottomRow.add(deckTitle).padRight(10f);
            bottomRow.add(createDeckButton()).size(220f, 110f);
        }
        panel.add(bottomRow).growX().row();

        root.add(panel).size(LOBBY_WINDOW_WIDTH, LOBBY_WINDOW_HEIGHT).center();
        stage.addActor(root);

        //Table chatRoot = new Table();
        //chatRoot.setFillParent(true);
        //chatWidget = new ChatWidget(game);
        //chatRoot.add(chatWidget).width(380f).height(210f).expand().left().bottom().padLeft(24f).padBottom(24f);
        //stage.addActor(chatRoot);
    }

    /** Кнопка выбора колоды: текстура + имя выбранной колоды поверх неё. */
    private Actor createDeckButton() {
        selectedDeckName = resolveInitialDeckName();
        selectedDeckLabel = new VisLabel(deckDisplayName());
        selectedDeckLabel.setFontScale(0.9f);

        ImageButton chooseDeckButton = createImageButton(chooseDeckBtnTexture);
        chooseDeckButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showDeckSelectionDialog();
            }
        });

        Stack stack = new Stack();
        stack.add(chooseDeckButton);
        Table overlay = new Table();
        overlay.add(selectedDeckLabel).center();
        stack.add(overlay);
        return stack;
    }

    private String resolveInitialDeckName() {
        Array<MemeDeck> decks = deckRepository.loadDecks();
        if (decks == null || decks.size == 0) {
            return null;
        }
        MemeDeck first = decks.first();
        return first == null ? null : first.name;
    }

    private String deckDisplayName() {
        return selectedDeckName == null || selectedDeckName.isBlank() ? t("default_deck") : selectedDeckName;
    }

    private void showDeckSelectionDialog() {
        Dialog dialog = new Dialog("", VisUI.getSkin());
        dialog.setBackground(window(lobbyWindowTexture));

        VisLabel title = new VisLabel(t("choose_deck"));
        title.setFontScale(1.25f);
        dialog.getContentTable().add(title).padTop(42f).padBottom(16f).row();

        Table deckRows = new Table();
        deckRows.defaults().left().growX().padBottom(8f);
        addDeckChoiceRow(deckRows, dialog, null);
        Array<MemeDeck> decks = deckRepository.loadDecks();
        if (decks != null) {
            for (MemeDeck deck : decks) {
                if (deck != null && deck.name != null && !deck.name.isBlank()) {
                    addDeckChoiceRow(deckRows, dialog, deck.name);
                }
            }
        }

        ScrollPane decksScroll = new ScrollPane(deckRows, VisUI.getSkin());
        decksScroll.setFadeScrollBars(false);
        decksScroll.setScrollingDisabled(true, false);
        dialog.getContentTable().add(decksScroll).width(470f).height(250f).padBottom(14f).row();

        VisTextButton closeButton = new VisTextButton(t("cancel"));
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(closeButton).width(170f).height(44f).padBottom(26f);

        dialog.show(stage);
        float dialogWidth = lobbyWindowTexture.getWidth() * 0.52f;
        float dialogHeight = lobbyWindowTexture.getHeight() * 0.52f;
        dialog.setSize(dialogWidth, dialogHeight);
        dialog.setPosition(
            (stage.getWidth() - dialogWidth) * 0.5f,
            (stage.getHeight() - dialogHeight) * 0.5f
        );
    }

    private void addDeckChoiceRow(Table deckRows, Dialog dialog, String deckName) {
        String displayName = deckName == null ? t("default_deck") : deckName;
        VisTextButton deckButton = new VisTextButton(displayName);
        deckButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedDeckName = deckName;
                if (selectedDeckLabel != null) {
                    selectedDeckLabel.setText(deckDisplayName());
                }
                dialog.hide();
            }
        });
        deckRows.add(deckButton).width(430f).height(42f).row();
    }

    private void showExitDialog() {
        Dialog dialog = new Dialog("", VisUI.getSkin()) {
            @Override
            protected void result(Object object) {
                if (Boolean.TRUE.equals(object)) {
                    game.leaveRoomToMenu();
                }
            }
        };

        dialog.setBackground(window(lobbyWindowTexture));
        VisLabel confirmLabel = new VisLabel(t("leave_room_confirm"));
        confirmLabel.setFontScale(1f);
        confirmLabel.setWrap(true);
        confirmLabel.setAlignment(Align.center);
        dialog.getContentTable().add(confirmLabel)
            .width(EXIT_DIALOG_TEXT_WIDTH)
            .padTop(82f)
            .padLeft(28f)
            .padRight(28f)
            .row();
        dialog.getButtonTable().clearChildren();
        dialog.getButtonTable().defaults().pad(10f).padBottom(EXIT_DIALOG_BUTTON_BOTTOM_PADDING);
        ImageButton backButton = createImageButton(yesButtonTexture);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.leaveRoomToMenu();
                dialog.hide();
            }
        });
        ImageButton cancelButton = createImageButton(noButtonTexture);
        cancelButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(backButton).size(150f, COMMON_BUTTON_HEIGHT);
        dialog.getButtonTable().add(cancelButton).size(150f, COMMON_BUTTON_HEIGHT);
        dialog.show(stage);
        float dialogWidth = lobbyWindowTexture.getWidth() * EXIT_DIALOG_SCALE;
        float dialogHeight = lobbyWindowTexture.getHeight() * EXIT_DIALOG_SCALE;
        dialog.setSize(dialogWidth, dialogHeight);
        dialog.setPosition(
            (stage.getWidth() - dialogWidth) * 0.5f,
            (stage.getHeight() - dialogHeight) * 0.5f
        );
    }

    private void rebuildPlayers(GameState state) {
        playersTable.clearChildren();
        if (state == null || state.players == null) {
            return;
        }

        for (Player p : state.players) {
            Table row = new Table();
            row.setBackground(panel(new Color(0.00f, 0.04f, 0.24f, 0.06f)));
            row.pad(10f, 12f, 10f, 12f);

            VisLabel name = new VisLabel(p.name);
            name.setColor(TEXT_DARK);
            VisLabel money = new VisLabel("$" + p.money);
            money.setColor(MONEY_COLOR);

            row.add(name).expandX().left();
            row.add(money).right();
            playersTable.add(row).growX().padBottom(8f).row();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().setColor(Color.WHITE);
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
                return; // экран уже переключён, дальше рисовать нельзя
            }
        } else {
            statusLabel.setText(t("players_in_room") + ": " + count);
        }
        if (game.isHost()) {
            startButton.setDisabled(count < 2);
        }
        //if (chatWidget != null) {
        //    chatWidget.refresh();
        //}
        stage.act(delta);
        stage.draw();
        stage.getBatch().setColor(Color.WHITE);
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    private Drawable panel(Color color) {
        return VisUI.getSkin().newDrawable("white", color);
    }

    private Drawable window(Texture texture) {
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private Texture loadTextureWithFallback(String primaryPath, String fallbackPath) {
        return Gdx.files.internal(primaryPath).exists() ? loadTexture(primaryPath) : loadTexture(fallbackPath);
    }

    private ImageButton createImageButton(Texture texture) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = drawable;
        style.imageOver = drawable.tint(new Color(0.82f, 0.82f, 0.82f, 1f));
        style.imageDown = drawable.tint(new Color(0.70f, 0.70f, 0.70f, 1f));
        style.imageDisabled = drawable.tint(new Color(0.45f, 0.45f, 0.45f, 1f));
        Drawable transparent = panel(new Color(1f, 1f, 1f, 0f));
        style.up = transparent;
        style.over = transparent;
        style.down = transparent;
        style.disabled = transparent;
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.stretch);
        button.getImageCell().grow();
        return button;
    }

    private String t(String key) {
        boolean ru = language == Language.RU;
        return switch (key) {
            case "lobby" -> ru ? "Комната ожидания" : "LOBBY";
            case "code" -> ru ? "Код" : "CODE";
            case "room_code" -> ru ? "Код комнаты" : "Room code";
            case "waiting_players" -> ru ? "Ожидаем игроков..." : "Waiting for players...";
            case "players" -> ru ? "Игроки" : "Players";
            case "leave_room" -> ru ? "Выйти из комнаты?" : "Leave room?";
            case "leave_room_confirm" -> ru ? "Вы точно хотите выйти из комнаты?" : "Are you sure you want to leave the room?";
            case "players_in_room" -> ru ? "Игроков в комнате" : "Players in room";
            case "choose_deck" -> ru ? "Выбрать колоду" : "Choose deck";
            case "selected_deck" -> ru ? "Колода" : "DECK";
            case "default_deck" -> ru ? "Стандартная" : "Default";
            case "cancel" -> ru ? "Отмена" : "Cancel";
            default -> key;
        };
    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        chooseDeckBtnTexture.dispose();
        startButtonTexture.dispose();
        copyButtonTexture.dispose();
        noButtonTexture.dispose();
        yesButtonTexture.dispose();
        cancelButtonTexture.dispose();
        lobbyWindowTexture.dispose();
        gameOverlayWindowTexture.dispose();
        //if (chatWidget != null) {
        //    chatWidget.dispose();
        //}
        stage.dispose();
    }
}

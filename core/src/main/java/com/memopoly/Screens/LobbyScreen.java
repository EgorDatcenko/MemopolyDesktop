package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.memopoly.Memopoly;
import com.memopoly.steam.SteamLobbyManager;
import com.memopoly.utils.LanguageManager.Language;
import com.memopoly.utils.TexturePathResolver;
import com.memopoly.ui.AnimatedImage;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.scenes.scene2d.utils.FocusListener;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.MemeDeck;
import com.memopoly.game.model.Player;
import com.memopoly.modding.DeckRepository;
import com.memopoly.network.twitch.TwitchChatService;
import com.memopoly.utils.UiShapes;
import com.memopoly.network.packets.StartGameRequest;
import com.memopoly.utils.ClipboardUtils;
import com.memopoly.utils.UiScrollStyle;
import com.memopoly.utils.UiShapes;

/**
 * Экран комнаты ожидания: показывает список подключённых игроков, позволяет
 * выбирать колоду и запускать матч.
 */
public class LobbyScreen extends BaseScreen {
    private static final float COMMON_BUTTON_HEIGHT = 64f;
    private static final float LOBBY_WINDOW_ASPECT = 930f / 550f;
    private static final float LOBBY_WINDOW_WIDTH = 1050f;
    private static final float LOBBY_WINDOW_HEIGHT = 760f;
    private static final float EXIT_DIALOG_SCALE = 0.6f;
    private static final float EXIT_DIALOG_TEXT_WIDTH = 440f;
    private static final float EXIT_DIALOG_BUTTON_BOTTOM_PADDING = 30f;
    private static final Color BACKGROUND_COLOR = new Color(0.10f, 0.10f, 0.17f, 1f);
    private static final Color TEXT_DARK = Color.valueOf("000A3E"); // #000A3E
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
    private static final String DECK_WINDOW_TEXTURE_PATH = "chat_window.png";

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
    private final Texture inviteFriendsTexture;
    private final Texture rolesOnTexture;
    private final Texture rolesOffTexture;
    private final Texture selectTexture;
    public final Texture deckWindowTexture;
    private VisLabel statusLabel;
    private Table playersTable;
    private ImageButton startButton;
    private ImageButton inviteButton;
    private Texture lobbyMessageTexture;
    // private ChatWidget chatWidget;
    private VisLabel selectedDeckLabel;
    private String selectedDeckName;
    private int lastPlayersCount = -1;
    private boolean gameStarted = false;
    private final Language language;
    private final DeckRepository deckRepository = new DeckRepository();
    private boolean rolesEnabled = false;
    private boolean twitchEnabled = false;
    private Table panel;
    private Table twitchInputContainer;
    private com.badlogic.gdx.scenes.scene2d.ui.Cell<?> twitchInputCell;
    private final Array<Dialog> openDialogs = new Array<>();
    private final Texture twitchTexture;
    private final Texture inputTexture;
    private VisTextField twitchField;
    private Image twitchStatusIcon;
    private Stack twitchInputStack;
    private VisLabel playersCountLabel;
    private float twitchDebounceTimer = -1f;
    private String lastCheckedTwitchChannel = "";

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
        cancelButtonTexture = loadTexture(
                TexturePathResolver.resolveScreenTexture(CANCEL_BUTTON_TEXTURE_PATH, language));
        yesButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(YES_BUTTON_TEXTURE_PATH, language));
        inviteFriendsTexture = loadTexture(
                TexturePathResolver.resolveScreenTexture("invite_friends_btn.png", language));
        rolesOnTexture = loadTexture(TexturePathResolver.resolveScreenTexture("on_btn.png", language));
        rolesOffTexture = loadTexture(TexturePathResolver.resolveScreenTexture("off_btn.png", language));
        selectTexture = loadTexture(TexturePathResolver.resolveScreenTexture("select.png", language));
        lobbyWindowTexture = loadTexture(LOBBY_WINDOW_TEXTURE_PATH);
        deckWindowTexture = loadTexture(DECK_WINDOW_TEXTURE_PATH);
        gameOverlayWindowTexture = loadTexture(GAME_OVERLAY_WINDOW_TEXTURE_PATH);
        lobbyMessageTexture = loadTextureIfExists("lobby_message.png");
        twitchTexture = loadTextureIfExists("twitch.png");
        inputTexture = loadTextureIfExists("input.png");
        Gdx.input.setInputProcessor(stage);
        createUI();
    }

    private void createUI() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(28);

        panel = new Table();
        panel.setBackground(window(lobbyWindowTexture));
        panel.pad(30f, 40f, 30f, 40f);

        // Заголовок по центру + кнопка выхода в правом верхнем углу
        VisLabel title = new VisLabel(t("lobby"));
        title.setFontScale(1.6f);
        title.setColor(TEXT_DARK);

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
        titleRow.add(closeButton).size(55f, 55f);
        panel.add(titleRow).growX().padBottom(30f).row();

        // Код комнаты по центру
        VisLabel roomCode = new VisLabel(t("code") + ": " + game.getRoomCode());
        roomCode.setFontScale(0.8f);
        roomCode.setColor(TEXT_DARK);
        panel.add(roomCode).center().padBottom(20f).row();

        twitchEnabled = game.getSettingsPreferences().getBoolean("twitch_enabled", false);

        Table settingsRow = new Table();
        settingsRow.center();

        if (game.isHost()) {
            // Левая колонка: Роли
            Table rolesCol = new Table();
            VisLabel rolesTitle = new VisLabel(t("lobby_roles"));
            rolesTitle.setFontScale(0.9f);
            rolesTitle.setColor(TEXT_DARK);
            ImageButton rolesButton = createImageButton(rolesOffTexture != null ? rolesOffTexture : noButtonTexture);
            rolesButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    rolesEnabled = !rolesEnabled;
                    setButtonTexture(rolesButton, rolesEnabled
                            ? (rolesOnTexture != null ? rolesOnTexture : yesButtonTexture)
                            : (rolesOffTexture != null ? rolesOffTexture : noButtonTexture));
                }
            });
            rolesCol.add(rolesTitle).padRight(10f);
            rolesCol.add(rolesButton).size(120f, 55f);
            settingsRow.add(rolesCol).padRight(40f);
        }

        // Колонка: Twitch
        Table twitchCol = new Table();
        VisLabel twitchTitle = new VisLabel(t("lobby_twitch"));
        twitchTitle.setFontScale(0.9f);
        twitchTitle.setColor(TEXT_DARK);
        ImageButton twitchToggleButton = createImageButton(twitchEnabled
                ? (rolesOnTexture != null ? rolesOnTexture : yesButtonTexture)
                : (rolesOffTexture != null ? rolesOffTexture : noButtonTexture));
        twitchToggleButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                twitchEnabled = !twitchEnabled;
                Preferences prefs = game.getSettingsPreferences();
                prefs.putBoolean("twitch_enabled", twitchEnabled);
                prefs.flush();
                setButtonTexture(twitchToggleButton, twitchEnabled
                        ? (rolesOnTexture != null ? rolesOnTexture : yesButtonTexture)
                        : (rolesOffTexture != null ? rolesOffTexture : noButtonTexture));
                updateTwitchInputVisibility();
            }
        });

        Image twitchInfoIcon = new Image(UiShapes.glyphInfoIcon());
        twitchInfoIcon.setScaling(Scaling.fit);

        Table twitchTooltipContent = new Table();
        twitchTooltipContent.setBackground(window(gameOverlayWindowTexture));
        twitchTooltipContent.pad(12f, 16f, 12f, 16f);

        VisLabel tipLabel = new VisLabel(t("lobby_twitch_tooltip"));
        tipLabel.setColor(Color.valueOf("000A3E"));
        tipLabel.setFontScale(0.75f);
        tipLabel.setAlignment(Align.center);
        tipLabel.setWrap(true);
        twitchTooltipContent.add(tipLabel).width(440f).center();

        com.kotcrab.vis.ui.widget.Tooltip twitchTooltip = new com.kotcrab.vis.ui.widget.Tooltip.Builder(twitchTooltipContent)
                .target(twitchInfoIcon).build();
        twitchTooltip.setBackground((com.badlogic.gdx.scenes.scene2d.utils.Drawable) null);
        twitchInfoIcon.addListener(new InputListener() {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                twitchTooltip.toFront();
            }
        });

        twitchCol.add(twitchTitle).padRight(10f);
        twitchCol.add(twitchToggleButton).size(120f, 55f).padRight(8f);
        twitchCol.add(twitchInfoIcon).size(22f, 22f);

        settingsRow.add(twitchCol);
        panel.add(settingsRow).center().padBottom(16f).row();

        twitchInputContainer = new Table();
        twitchInputContainer.center();

        Table twitchRow = new Table();
        twitchRow.center();

        twitchInputStack = new Stack();

        NinePatch inputPatch = new NinePatch(inputTexture != null ? inputTexture : selectTexture, 16, 16, 16, 16);
        NinePatchDrawable inputBg = new NinePatchDrawable(inputPatch);
        inputBg.setMinWidth(0f);
        inputBg.setMinHeight(0f);
        inputBg.setLeftWidth(48f);
        inputBg.setRightWidth(16f);

        twitchField = new VisTextField();
        VisTextField.VisTextFieldStyle fieldStyle = new VisTextField.VisTextFieldStyle(twitchField.getStyle());
        fieldStyle.background = inputBg;
        fieldStyle.backgroundOver = inputBg;
        fieldStyle.focusedBackground = inputBg;
        fieldStyle.disabledBackground = inputBg;
        fieldStyle.focusBorder = null;
        fieldStyle.font = VisUI.getSkin().get("default", Label.LabelStyle.class).font;
        fieldStyle.fontColor = TEXT_DARK;
        fieldStyle.messageFont = fieldStyle.font;
        fieldStyle.messageFontColor = new Color(0.5f, 0.5f, 0.5f, 0.8f);
        twitchField.setStyle(fieldStyle);
        twitchField.setMessageText(t("twitch_nickname_placeholder"));

        twitchInputStack.add(twitchField);

        if (twitchTexture != null) {
            Table iconTable = new Table();
            iconTable.left();
            iconTable.setTouchable(Touchable.disabled);
            Image twitchIcon = new Image(twitchTexture);
            twitchIcon.setScaling(Scaling.fit);
            iconTable.add(twitchIcon).size(28f, 28f).padLeft(12f);
            twitchInputStack.add(iconTable);
        }

        twitchStatusIcon = new Image(UiShapes.statusError(24));
        twitchStatusIcon.setScaling(Scaling.fit);

        playersCountLabel = new VisLabel("1/6");
        playersCountLabel.setColor(TEXT_DARK);
        playersCountLabel.setFontScale(0.85f);

        twitchRow.add(twitchInputStack).size(320f, 52f);
        twitchRow.add(twitchStatusIcon).size(24f, 24f).padLeft(8f);
        twitchRow.add(playersCountLabel).padLeft(16f);
        twitchInputContainer.add(twitchRow).center();

        twitchInputCell = panel.add(twitchInputContainer).center();
        twitchInputCell.row();

        game.getTwitchChatService().setStatusListener(status -> {
            Gdx.app.postRunnable(() -> updateTwitchStatusIcon(status));
        });

        twitchField.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                twitchDebounceTimer = 0.8f;
            }
        });

        twitchField.addListener(new FocusListener() {
            @Override
            public void keyboardFocusChanged(FocusEvent event, Actor actor, boolean focused) {
                if (!focused) {
                    triggerTwitchConnect();
                }
            }
        });

        updateTwitchInputVisibility();

        playersTable = new Table();
        playersTable.top().left();
        ScrollPane playersScroll = new ScrollPane(playersTable, VisUI.getSkin());
        playersScroll.setFadeScrollBars(true);
        playersScroll.setScrollingDisabled(true, false);
        playersScroll.setOverscroll(false, false);
        UiScrollStyle.apply(playersScroll, language);

        Table playersBox = new Table();
        playersBox.setBackground(window(gameOverlayWindowTexture));
        playersBox.pad(20f);
        playersBox.add(playersScroll).grow();
        panel.add(playersBox).width(640f).height(240f).center().padBottom(12f).row();

        startButton = createImageButton(startButtonTexture);
        startButton.setVisible(game.isHost());
        startButton.setDisabled(true);
        startButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                StartGameRequest request = new StartGameRequest();
                request.deckName = selectedDeckName;
                request.rolesEnabled = rolesEnabled;
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

        Table bottomRow = new Table();
        bottomRow.left();
        bottomRow.add(startButton).size(190f, 80f).padRight(16f);
        if (game.isHost()) {
            inviteButton = createImageButton(
                    inviteFriendsTexture != null ? inviteFriendsTexture : startButtonTexture);
            inviteButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    SteamLobbyManager.inviteFriends();
                }
            });
            bottomRow.add(inviteButton).size(190f, 80f).padRight(16f);
        }
        bottomRow.add(copyCodeButton).size(190f, 80f);

        bottomRow.add().expandX();
        if (game.isHost()) {
            VisLabel deckTitle = new VisLabel(t("selected_deck") + ":");
            deckTitle.setColor(TEXT_DARK);
            bottomRow.add(deckTitle).padRight(10f);
            bottomRow.add(createDeckButton()).size(190f, 80f);
        }
        panel.add(bottomRow).growX().row();

        Stack centerStack = new Stack();
        Table windowHolder = new Table();
        windowHolder.add(panel).size(LOBBY_WINDOW_WIDTH, LOBBY_WINDOW_HEIGHT).center();
        centerStack.add(windowHolder);

        root.add(centerStack).center();
        stage.addActor(root);

        // Table chatRoot = new Table();
        // chatRoot.setFillParent(true);
        // chatWidget = new ChatWidget(game);
        // chatRoot.add(chatWidget).width(380f).height(210f).expand().left().bottom().padLeft(24f).padBottom(24f);
        // stage.addActor(chatRoot);
    }

    private void setButtonTexture(ImageButton button, Texture texture) {
        TextureRegionDrawable d = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton.ImageButtonStyle s = button.getStyle();
        s.imageUp = d;
        s.imageOver = d.tint(new Color(0.82f, 0.82f, 0.82f, 1f));
        s.imageDown = d.tint(new Color(0.70f, 0.70f, 0.70f, 1f));
    }

    /** Кнопка выбора колоды: текстура + имя выбранной колоды поверх неё. */
    private Actor createDeckButton() {
        selectedDeckName = resolveInitialDeckName();
        selectedDeckLabel = new VisLabel(deckDisplayName());
        selectedDeckLabel.setFontScale(0.9f);
        selectedDeckLabel.setColor(TEXT_DARK);

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
        dialog.setBackground(window(deckWindowTexture));

        VisLabel title = new VisLabel(t("choose_deck"));
        title.setFontScale(1.25f);
        title.setColor(TEXT_DARK);

        ImageButton closeButton = createImageButton(cancelButtonTexture);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });

        Table titleRow = new Table();
        titleRow.add().size(55f).padLeft(30f);
        titleRow.add(title).expandX();
        titleRow.add(closeButton).size(55f, 55f).padRight(30f);

        // прижимаем контент к верху — заголовок и крестик поднимаются к верхней кромке
        // окна
        dialog.getContentTable().top();
        dialog.getContentTable().add(titleRow).growX().padTop(24f).padBottom(16f).row();

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
        // убираем рамку скина вокруг списка — тот самый синий контур
        UiScrollStyle.apply(decksScroll, language);
        dialog.getContentTable().add(decksScroll).width(400f).height(350f).padBottom(20f).row();

        dialog.show(stage);
        openDialogs.add(dialog);
        float dialogWidth = 700f; // было 940f — окно уже
        float dialogHeight = 520f;
        dialog.setSize(dialogWidth, dialogHeight);
        dialog.setPosition(
                (stage.getWidth() - dialogWidth) * 0.5f,
                (stage.getHeight() - dialogHeight) * 0.5f);
    }

    private void addDeckChoiceRow(Table deckRows, Dialog dialog, String deckName) {
        String displayName = deckName == null ? t("default_deck") : deckName;
        MemeDeck deck = deckName != null ? deckRepository.getDeckByName(deckName) : null;
        Animation<TextureRegion> coverAnim = deck != null ? deck.getCoverAnimation() : null;

        VisTextButton deckButton;
        if (selectTexture != null) {
            VisTextButton.VisTextButtonStyle style = new VisTextButton.VisTextButtonStyle(
                    VisUI.getSkin().get("default", VisTextButton.VisTextButtonStyle.class));
            style.font = VisUI.getSkin().get("default", Label.LabelStyle.class).font;
            style.fontColor = TEXT_DARK;
            NinePatch selectPatch = new NinePatch(selectTexture, 46, 46, 20, 20);
            NinePatchDrawable selectDrawable = new NinePatchDrawable(selectPatch);
            selectDrawable.setMinWidth(0f);
            selectDrawable.setMinHeight(0f);
            style.up = selectDrawable;
            style.over = selectDrawable.tint(new Color(0.90f, 0.90f, 0.95f, 1f));
            style.down = selectDrawable.tint(new Color(0.82f, 0.82f, 0.90f, 1f));
            style.checked = selectDrawable;
            deckButton = new VisTextButton(displayName, style);
        } else {
            deckButton = new VisTextButton(displayName);
            deckButton.getLabel().setFontScale(1.3f);
        }
        deckButton.pad(10f, 16f, 10f, 16f);
        deckButton.setFocusBorderEnabled(false);
        deckButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                selectedDeckName = deckName;
                if (selectedDeckLabel != null)
                    selectedDeckLabel.setText(deckDisplayName());
                dialog.hide();
            }
        });

        Table rowTable = new Table();
        if (coverAnim != null) {
            AnimatedImage preview = new AnimatedImage(coverAnim);
            rowTable.add(preview).size(48f, 48f).padRight(10f);
        }
        rowTable.add(deckButton).width(350f).height(54f);
        deckRows.add(rowTable).padBottom(10f).row();
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

        dialog.setBackground(window(deckWindowTexture));
        VisLabel confirmLabel = new VisLabel(t("leave_room_confirm"));
        confirmLabel.setFontScale(1f);
        confirmLabel.setWrap(true);
        confirmLabel.setAlignment(Align.center);
        confirmLabel.setColor(TEXT_DARK);
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
        openDialogs.add(dialog);
        float dialogWidth = 500f;
        float dialogHeight = 300f;
        dialog.setSize(dialogWidth, dialogHeight);
        dialog.setPosition(
                (stage.getWidth() - dialogWidth) * 0.5f,
                (stage.getHeight() - dialogHeight) * 0.5f);
    }

    private void updateStartButtonState() {
        if (startButton == null)
            return;
        boolean isHost = game.isHost();
        GameState state = game.getLatestGameState();
        int count = (state == null || state.players == null) ? 0 : state.players.size();

        if (!isHost || count < 3 || count > 6) {
            startButton.setDisabled(true);
            startButton.setTouchable(Touchable.disabled);
            setButtonTexture(startButton, startButtonTexture);
            startButton.setColor(0.5f, 0.5f, 0.5f, 1f);
        } else {
            startButton.setDisabled(false);
            startButton.setTouchable(Touchable.enabled);
            setButtonTexture(startButton, startButtonTexture);
            startButton.setColor(1f, 1f, 1f, 1f);
        }
        if (inviteButton != null) {
            boolean full = count >= 6;
            inviteButton.setDisabled(full);
            inviteButton.setTouchable(full ? Touchable.disabled : Touchable.enabled);
            inviteButton.setColor(full ? new Color(0.5f, 0.5f, 0.5f, 1f) : Color.WHITE);
        }
    }

    private void rebuildPlayers(GameState state) {
        playersTable.clearChildren();
        int count = (state == null || state.players == null) ? 0 : state.players.size();
        if (playersCountLabel != null) {
            playersCountLabel.setText(count + "/6");
        }
        if (state == null || state.players == null) {
            updateStartButtonState();
            return;
        }

        for (Player p : state.players) {
            Table row = new Table();
            row.setBackground(panel(new Color(0.00f, 0.04f, 0.24f, 0.06f)));
            row.pad(10f, 12f, 10f, 12f);

            VisLabel name = new VisLabel(p.name);
            name.setColor(TEXT_DARK);

            row.add(name).expandX().left().row();
            playersTable.add(row).growX().padBottom(8f).row();
        }

        if (count < 3) {
            Table minRow = new Table();
            minRow.pad(8f, 12f, 8f, 12f);
            VisLabel minLabel = new VisLabel(language == Language.RU ? "МИНИМУМ: 3 ИГРОКА" : "MINIMUM: 3 PLAYERS");
            minLabel.setColor(Color.valueOf("E74C3C"));
            minLabel.setFontScale(0.85f);
            minLabel.setAlignment(Align.center);
            minRow.add(minLabel).expandX().center();
            playersTable.add(minRow).growX().padBottom(8f).row();
        }

        updateStartButtonState();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().setColor(Color.WHITE);
        stage.getBatch().begin();
        stage.getBatch().draw(backgroundTexture, 0f, 0f, stage.getViewport().getWorldWidth(),
                stage.getViewport().getWorldHeight());
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
            statusLabel.setText(t("players_in_room") + ": " + count + " / 6");
        }
        updateStartButtonState();
        if (twitchDebounceTimer > 0f) {
            twitchDebounceTimer -= delta;
            if (twitchDebounceTimer <= 0f) {
                twitchDebounceTimer = -1f;
                triggerTwitchConnect();
            }
        }
        // if (chatWidget != null) {
        // chatWidget.refresh();
        // }
        stage.act(delta);
        stage.draw();
        stage.getBatch().setColor(Color.WHITE);
    }

    private void recenterOpenDialogs() {
        for (int i = openDialogs.size - 1; i >= 0; i--) {
            Dialog d = openDialogs.get(i);
            if (d.getStage() != stage) {
                openDialogs.removeIndex(i); // диалог закрыт
            } else {
                d.setPosition(
                        (stage.getWidth() - d.getWidth()) * 0.5f,
                        (stage.getHeight() - d.getHeight()) * 0.5f);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        recenterOpenDialogs();
    }

    private Drawable panel(Color color) {
        return VisUI.getSkin().newDrawable("white", color);
    }

    private Drawable window(Texture texture) {
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
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

    private void showAlertDialog(String text) {
        Dialog dialog = new Dialog("", VisUI.getSkin());
        dialog.setBackground(window(deckWindowTexture));
        VisLabel label = new VisLabel(text);
        label.setFontScale(0.95f);
        label.setWrap(true);
        label.setAlignment(Align.center);
        label.setColor(TEXT_DARK);
        dialog.getContentTable().add(label).width(440f).padTop(50f).padLeft(20f).padRight(20f).row();
        dialog.getButtonTable().clearChildren();
        dialog.getButtonTable().defaults().pad(10f).padBottom(20f);
        ImageButton okButton = createImageButton(yesButtonTexture);
        okButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(okButton).size(150f, COMMON_BUTTON_HEIGHT);
        dialog.show(stage);
        openDialogs.add(dialog);
        float w = 500f, h = 260f;
        dialog.setSize(w, h);
        dialog.setPosition((stage.getWidth() - w) * 0.5f, (stage.getHeight() - h) * 0.5f);
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
            case "leave_room_confirm" ->
                ru ? "Вы точно хотите выйти из комнаты?" : "Are you sure you want to leave the room?";
            case "players_in_room" -> ru ? "Игроков в комнате" : "Players in room";
            case "choose_deck" -> ru ? "Выбрать колоду" : "Choose deck";
            case "selected_deck" -> ru ? "Колода" : "DECK";
            case "default_deck" -> ru ? "Стандартная" : "Default";
            case "cancel" -> ru ? "Отмена" : "Cancel";
            case "invite_friends" -> ru ? "Пригласить друзей" : "Invite friends";
            case "roles" -> ru ? "Роли" : "Roles";
            case "lobby_roles" -> ru ? "Роли:" : "Roles:";
            case "lobby_twitch" -> "Twitch:";
            case "lobby_twitch_tooltip" -> ru
                    ? "Включает голосование зрителей чата Twitch за мемы в Мем-Баттле"
                    : "Enables Twitch chat voting for memes in Meme Battle";
            case "on" -> ru ? "ВКЛ" : "ON";
            case "off" -> ru ? "ВЫКЛ" : "OFF";
            case "twitch_nickname_placeholder" -> ru ? "никнейм" : "channel name";
            default -> key;
        };
    }

    private void triggerTwitchConnect() {
        if (!twitchEnabled || twitchField == null) return;
        String text = twitchField.getText().trim();
        if (text.equals(lastCheckedTwitchChannel)) {
            return;
        }
        lastCheckedTwitchChannel = text;
        Preferences prefs = game.getSettingsPreferences();
        prefs.putString("twitch_channel", text);
        prefs.flush();

        if (text.isEmpty()) {
            game.getTwitchChatService().disconnect();
            updateTwitchStatusIcon(TwitchChatService.ConnectionStatus.DISCONNECTED);
        } else {
            game.getTwitchChatService().connectAsync(text);
        }
    }

    private void updateTwitchInputVisibility() {
        if (twitchInputStack != null) {
            twitchInputStack.setVisible(twitchEnabled);
        }
        if (twitchStatusIcon != null) {
            twitchStatusIcon.setVisible(twitchEnabled);
        }
        if (twitchInputContainer != null) {
            twitchInputContainer.setVisible(true);
        }
        if (twitchInputCell != null) {
            if (twitchEnabled) {
                twitchInputCell.height(52f).padBottom(14f);
            } else {
                twitchInputCell.height(28f).padBottom(10f);
            }
            if (panel != null) {
                panel.invalidate();
            }
        }
        if (!twitchEnabled) {
            game.getTwitchChatService().disconnect();
            updateTwitchStatusIcon(TwitchChatService.ConnectionStatus.DISCONNECTED);
        } else {
            Preferences prefs = game.getSettingsPreferences();
            String savedChannel = prefs.getString("twitch_channel", "");
            if (!savedChannel.isEmpty()) {
                if (twitchField != null) {
                    twitchField.setText(savedChannel);
                }
                lastCheckedTwitchChannel = savedChannel;
                game.getTwitchChatService().connectAsync(savedChannel);
            } else {
                updateTwitchStatusIcon(game.getTwitchChatService().getStatus());
            }
        }
    }

    private void updateTwitchStatusIcon(TwitchChatService.ConnectionStatus status) {
        if (twitchStatusIcon == null) return;
        switch (status) {
            case CONNECTED -> twitchStatusIcon.setDrawable(new TextureRegionDrawable(new TextureRegion(UiShapes.statusConnected(24))));
            case CONNECTING -> twitchStatusIcon.setDrawable(new TextureRegionDrawable(new TextureRegion(UiShapes.statusConnecting(24))));
            case ERROR, DISCONNECTED -> twitchStatusIcon.setDrawable(new TextureRegionDrawable(new TextureRegion(UiShapes.statusError(24))));
        }
    }

    private Texture loadTextureIfExists(String path) {
        return Gdx.files.internal(path).exists() ? loadTexture(path) : null;
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
        if (lobbyMessageTexture != null)
            lobbyMessageTexture.dispose();
        if (inviteFriendsTexture != null)
            inviteFriendsTexture.dispose();
        if (rolesOnTexture != null)
            rolesOnTexture.dispose();
        if (rolesOffTexture != null)
            rolesOffTexture.dispose();
        if (selectTexture != null)
            selectTexture.dispose();
        if (twitchTexture != null)
            twitchTexture.dispose();
        if (inputTexture != null)
            inputTexture.dispose();
        // if (chatWidget != null) {
        // chatWidget.dispose();
        // }
        stage.dispose();
    }
}

package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.memopoly.Memopoly;
import com.memopoly.steam.SteamAvatarCache;
import com.memopoly.utils.*;
import com.memopoly.utils.LanguageManager.Language;
import com.memopoly.game.model.*;
import com.memopoly.network.packets.BattleResponsePacket;
import com.memopoly.network.packets.GameActionRequest;
import com.memopoly.network.packets.RollDiceRequest;
import com.memopoly.network.packets.TradeOfferPacket;
import com.memopoly.network.packets.TradeResponsePacket;
import com.memopoly.network.packets.TradeCancelPacket;

import javax.swing.event.ChangeEvent;
import java.util.*;
import java.util.List;

/**
 * Экран игрового процесса: отображает доску, статистику игроков, кнопки
 * действий и всплывающие окна баттлов/аукционов.
 */
public class GameScreen extends BaseScreen {
    private static final float COMMON_BUTTON_HEIGHT = 64f;
    private static final float WORLD_WIDTH = 1920f;
    private static final float WORLD_HEIGHT = 1080f;
    private static final Color TITLE_COLOR = new Color(1.00f, 0.83f, 0.25f, 1f);
    private static final Color TEXT_SOFT = Color.valueOf("000A3E");
    private static final Color ACCENT_GOLD = new Color(0.99f, 0.83f, 0.29f, 1f);
    private static final String DICE_BUTTON_TEXTURE_PATH = "button_dice.png";
    private static final String MONEY_TEXTURE_PATH = "money.png";
    private static final String BUY_BUTTON_TEXTURE_PATH = "buy_btn.png";
    private static final String AUCTION_BUTTON_TEXTURE_PATH = "auction_btn.png";
    private static final String END_TURN_BUTTON_TEXTURE_PATH = "end_of_turn_btn.png";
    private static final String PLACE_BID_BUTTON_TEXTURE_PATH = "make_a_bet_btn.png";
    private static final String MORTGAGE_BUTTON_TEXTURE_PATH = "mortgage_btn.png";
    private static final String BUY_BACK_BUTTON_TEXTURE_PATH = "reverse_mortgage_btn.png";
    private static final String EXIT_TO_MENU_BUTTON_TEXTURE_PATH = "exit_to_menu.png";
    private static final String DEPOSIT_BUTTON_TEXTURE_PATH = "deposit_btn.png";
    private static final String WITHDRAW_BUTTON_TEXTURE_PATH = "withdraw_btn.png";
    private static final String PARTICIPATE_BUTTON_TEXTURE_PATH = "participate_btn.png";
    private static final String DECLINE_BUTTON_TEXTURE_PATH = "decline_btn.png";
    private static final String NOTIFICATION_WINDOW_TEXTURE_PATH = "notification_window.png";
    private static final String BUY_AND_AUCTION_WINDOW_TEXTURE_PATH = "buy_and_auction_window.png";
    private static final String AUCTION_OR_MEMEBANK_WINDOW_TEXTURE_PATH = "auction_or_memebank_window.png";
    private static final Color BATTLE_TEXT_COLOR = Color.valueOf("000A3E");
    private static final String INPUT_TEXTURE_PATH = "input.png";
    private static final String BACKGROUND_TEXTURE_PATH = "background.png";
    private static final String GAME_OVERLAY_WINDOW_TEXTURE_PATH = "game_overlay_window.png";
    private static final String PASS_BUTTON_TEXTURE_PATH = "pass_btn.png";
    private static final String PLAYERS_WINDOW_TEXTURE_PATH = "the_left_sidebar_window.png";
    private static final String MY_CELLS_WINDOW_TEXTURE_PATH = "my_cells_sidebar.png";
    private static final String MEMES_WINDOW_TEXTURE_PATH = "memes_sidebar.png";
    private static final float BUY_AND_AUCTION_MODAL_MIN_W = 780f;
    private static final float BUY_AND_AUCTION_MODAL_MIN_H = 430f;
    private static final float AUCTION_MODAL_MIN_W = 800f;
    private static final float AUCTION_MODAL_MIN_H = 320f;
    private static final float MEME_BANK_MODAL_MIN_W = 640f;
    private static final float MEME_BANK_MODAL_MIN_H = 420f;
    private static final float NOTIFICATION_MODAL_MIN_W = 520f;
    private static final float NOTIFICATION_MODAL_MIN_H = 220f;

    private final Stage stage;
    private final BoardRenderer boardRenderer;
    private final List<BoardCell> boardCells;
    private final Texture diceButtonTexture;
    private final Texture moneyTexture;
    private final Texture buyButtonTexture;
    private final Texture auctionButtonTexture;
    private final Texture endTurnButtonTexture;
    private final Texture placeBidButtonTexture;
    private final Texture mortgageButtonTexture;
    private final Texture buyBackButtonTexture;
    private final Texture exitToMenuButtonTexture;
    private final Texture depositButtonTexture;
    private final Texture withdrawButtonTexture;
    private final Texture skipButtonTexture;
    private final Texture participateButtonTexture;
    private final Texture declineButtonTexture;
    private final Texture notificationWindowTexture;
    private final Texture buyAndAuctionWindowTexture;
    private final Texture auctionOrMemeBankWindowTexture;
    private final Texture inputTexture;
    private final Texture backgroundTexture;
    private final Texture gameOverlayWindowTexture;
    private final Texture[] cellTextures;
    private final Texture playersWindowTexture;
    private final Texture myCellsWindowTexture;
    private final Texture memesWindowTexture;

    private final VisLabel titleLabel;
    private final VisLabel phaseLabel;
    private final VisLabel turnLabel;
    private final VisLabel cellLabel;
    private final VisLabel logLabel;
    private final VisLabel auctionLabel;
    private final VisLabel diceTitleLabel;
    private final VisLabel diceHintLabel;
    private final VisLabel currentCellTitleLabel;
    private final VisLabel currentCellMetaLabel;
    private final VisLabel feedTitleLabel;
    private final VisLabel feedDescriptionLabel;
    private final Image currentCellImage;
    private final Image buyModalCellImage;
    private final Image auctionModalCellImage;
    private final Table playersTable;
    private final Table ownedCellsTable;
    private final Table handMemesTable;
    private final Table diceOverlay;
    private final Table currentCellOverlay;
    private final Table feedOverlay;
    private final Table turnNotificationModal;
    private final Table buyOrAuctionModal;
    private final Table auctionModal;
    private final Table memeBankModal;
    private final VisLabel turnModalLabel;
    private final VisLabel buyAuctionModalLabel;
    private final VisLabel auctionModalLabel;
    private final VisLabel memeBankModalLabel;

    private final ImageButton diceButton;
    private final ImageButton buyButton;
    private final ImageButton passButton;
    private final ImageButton endTurnButton;
    private final ImageButton placeBidButton;
    private final Actor cancelAuctionButton;
    private final VisTextField bidField;
    private final VisTextField memeBankAmountField;
    private final Actor memeBankDepositButton;
    private final Actor memeBankWithdrawButton;
    private final Actor memeBankSkipButton;

    private String lastPlayersSignature = "";
    private String lastOwnedCellsSignature = "";
    private String lastHandMemesSignature = "";
    private long lastShownNotificationTimestamp = 0L;
    private float notificationVisibleTime = 0f;
    private final Map<String, Texture> memeTextureCache = new HashMap<>();
    private final Map<Integer, Integer> lastKnownPositions = new HashMap<>();
    private String lastBattleSignature = "";
    // Input validation error labels
    private VisLabel memeBankErrorLabel;
    private VisLabel auctionErrorLabel;
    private VisTextButton moderatorSkipButton;
    private VisTextButton moderatorTakeJailButton;
    // Trade UI fields
    private static final int TRADE_MODE_CLOSED = 0;
    private static final int TRADE_MODE_BUILD = 1;
    private static final int TRADE_MODE_WAIT = 2;
    private static final int TRADE_MODE_INCOMING = 3;
    private Table tradeWindow;
    private int tradeMode = TRADE_MODE_CLOSED;
    private Set<Integer> selectedMyCells = new HashSet<>();
    private Set<Integer> selectedTheirCells = new HashSet<>();
    private Integer tradeTargetId = null;
    private boolean isTradeWindowOpen = false;
    private boolean isIncomingTrade = false;
    private boolean isInitiator = false;
    private int draftMyMoney = 0;
    private int draftTheirMoney = 0;
    private Actor tradeTouchLayer;
    private static final String PLUS_TWO_BUTTON_TEXTURE_PATH = "+2_btn.png";
    private static final String STAY_BUTTON_TEXTURE_PATH = "stay_btn.png";
    private static final String DEAL_BUTTON_TEXTURE_PATH = "deal_btn.png";
    private static final String STEAL_50_BUTTON_TEXTURE_PATH = "steal_50_btn.png";
    private final Texture plusTwoButtonTexture;
    private final Texture stayButtonTexture;
    private final Texture dealButtonTexture;
    private final Texture stealButtonTexture;

    private Button stealButton;
    private Button stayButton;
    private Button plusTwoButton;
    private Button dealButton;
    private final Language language;
    private Table battleOverlay;
    private Table sidePanelContainer;
    private Table sideRoot;
    private VisLabel battleTimerLabel;
    private VisLabel stakeValueLabel;
    private int localStake = 50;
    private VisTextField topicField;
    private ImageButton startBattleButton;
    private int selectedBattleMemeId = -1;
    private ChatWidget chatWidget;
    private final Texture tenUpTexture;
    private final Texture tenDownTexture;
    private final Texture startBattleTexture;
    private final Texture inputMemeBattleTexture;
    private final Texture cardBoardTexture;
    private Table roleTooltip;
    private VisLabel roleTooltipLabel;
    private int lastTurnPlayerId = -1;
    private static final String BUILD_BUTTON_TEXTURE_PATH = "build_btn.png";
    private static final String SELL_BUTTON_TEXTURE_PATH = "sell_btn.png";
    private final Texture buildButtonTexture;
    private final Texture sellButtonTexture;
    private static final String SHIELD_TEXTURE_PATH = "shield.png";
    private final Texture shieldTexture;
    private Texture timerCircleTexture;
    // Рамки аватарок строго по цвету роли (RoleInfo.color)
    private final Map<Role, Texture> avatarFrameByRole = new EnumMap<>(Role.class);
    private final Map<Actor, Role> roleInfoIcons = new HashMap<>();

    public GameScreen(Memopoly game) {
        super(game);
        this.language = game.getLanguageManager().getLanguage();
        Language language = this.language;
        boardRenderer = new BoardRenderer(game);
        stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        boardCells = BoardData.buildCells();
        diceButtonTexture = new Texture(DICE_BUTTON_TEXTURE_PATH);
        diceButtonTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        moneyTexture = new Texture(MONEY_TEXTURE_PATH);
        moneyTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        buyButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(BUY_BUTTON_TEXTURE_PATH, language));
        auctionButtonTexture = loadTexture(
                TexturePathResolver.resolveGameScreenTexture(AUCTION_BUTTON_TEXTURE_PATH, language));
        endTurnButtonTexture = loadTexture(
                TexturePathResolver.resolveGameScreenTexture(END_TURN_BUTTON_TEXTURE_PATH, language));
        placeBidButtonTexture = loadTexture(
                TexturePathResolver.resolveGameScreenTexture(PLACE_BID_BUTTON_TEXTURE_PATH, language));
        mortgageButtonTexture = loadTexture(
                TexturePathResolver.resolveGameScreenTexture(MORTGAGE_BUTTON_TEXTURE_PATH, language));
        buyBackButtonTexture = loadTexture(
                TexturePathResolver.resolveGameScreenTexture(BUY_BACK_BUTTON_TEXTURE_PATH, language));
        exitToMenuButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(EXIT_TO_MENU_BUTTON_TEXTURE_PATH, language));
        depositButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(DEPOSIT_BUTTON_TEXTURE_PATH, language));
        withdrawButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(WITHDRAW_BUTTON_TEXTURE_PATH, language));
        skipButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(PASS_BUTTON_TEXTURE_PATH, language));
        participateButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(PARTICIPATE_BUTTON_TEXTURE_PATH, language));
        declineButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(DECLINE_BUTTON_TEXTURE_PATH, language));
        plusTwoButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(PLUS_TWO_BUTTON_TEXTURE_PATH, language));
        stayButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(STAY_BUTTON_TEXTURE_PATH, language));
        dealButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(DEAL_BUTTON_TEXTURE_PATH, language));
        stealButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(STEAL_50_BUTTON_TEXTURE_PATH, language));
        shieldTexture = loadTextureIfExistsLinear(SHIELD_TEXTURE_PATH);
        timerCircleTexture = UiShapes.circle(128, Color.WHITE);
        timerCircleTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        buildButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(BUILD_BUTTON_TEXTURE_PATH, language));
        sellButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(SELL_BUTTON_TEXTURE_PATH, language));
        notificationWindowTexture = loadTexture(NOTIFICATION_WINDOW_TEXTURE_PATH);
        buyAndAuctionWindowTexture = loadTexture(BUY_AND_AUCTION_WINDOW_TEXTURE_PATH);
        auctionOrMemeBankWindowTexture = loadTexture(AUCTION_OR_MEMEBANK_WINDOW_TEXTURE_PATH);
        inputTexture = loadTexture(INPUT_TEXTURE_PATH);
        backgroundTexture = loadTexture(BACKGROUND_TEXTURE_PATH);
        cellTextures = loadCellTextures();
        gameOverlayWindowTexture = loadTexture(GAME_OVERLAY_WINDOW_TEXTURE_PATH);
        playersWindowTexture = loadTextureWithFallback(PLAYERS_WINDOW_TEXTURE_PATH, MY_CELLS_WINDOW_TEXTURE_PATH);
        myCellsWindowTexture = loadTextureWithFallback(MY_CELLS_WINDOW_TEXTURE_PATH, PLAYERS_WINDOW_TEXTURE_PATH);
        memesWindowTexture = loadTextureWithFallback(MEMES_WINDOW_TEXTURE_PATH, PLAYERS_WINDOW_TEXTURE_PATH);
        tenUpTexture = loadOrFallback("ten_up.png", diceButtonTexture);
        tenDownTexture = loadOrFallback("ten_down.png", diceButtonTexture);
        startBattleTexture = loadOrFallback("start_battle.png", placeBidButtonTexture);
        inputMemeBattleTexture = loadOrFallback("input_memebattle.png", inputTexture);
        cardBoardTexture = loadOrFallback("card_board.png", gameOverlayWindowTexture);
        loadAvatarFrameTextures();
        titleLabel = new VisLabel(t("title"));
        phaseLabel = new VisLabel(t("phase_idle"));
        turnLabel = new VisLabel(t("turn_idle"));
        cellLabel = new VisLabel(t("cell_idle"));
        logLabel = new VisLabel(t("log_idle"));
        auctionLabel = new VisLabel("");
        diceTitleLabel = new VisLabel("");
        diceHintLabel = new VisLabel(t("dice_hint_idle"));
        currentCellTitleLabel = new VisLabel("");
        currentCellMetaLabel = new VisLabel("-");
        feedTitleLabel = new VisLabel("");
        feedDescriptionLabel = new VisLabel(t("feed_idle"));
        currentCellImage = new Image();
        currentCellImage.setScaling(Scaling.fit);
        buyModalCellImage = new Image();
        buyModalCellImage.setScaling(Scaling.fit);
        auctionModalCellImage = new Image();
        auctionModalCellImage.setScaling(Scaling.fit);
        playersTable = new Table();
        ownedCellsTable = new Table();
        handMemesTable = new Table();
        diceOverlay = new Table();
        currentCellOverlay = new Table();
        feedOverlay = new Table();
        turnNotificationModal = new Table();
        buyOrAuctionModal = new Table();
        auctionModal = new Table();
        memeBankModal = new Table();
        turnModalLabel = new VisLabel("");
        buyAuctionModalLabel = new VisLabel("");
        auctionModalLabel = new VisLabel("");
        memeBankModalLabel = new VisLabel("");
        diceButton = createDiceButton();
        buyButton = createActionButton(buyButtonTexture);
        passButton = createActionButton(auctionButtonTexture);
        endTurnButton = createActionButton(endTurnButtonTexture);
        placeBidButton = createActionButton(placeBidButtonTexture);
        cancelAuctionButton = createOptionalActionButton(declineButtonTexture, t("btn_decline"));
        bidField = new VisTextField();
        memeBankAmountField = new VisTextField();
        memeBankDepositButton = createOptionalActionButton(depositButtonTexture, t("btn_deposit"));
        memeBankWithdrawButton = createOptionalActionButton(withdrawButtonTexture, t("btn_withdraw"));
        memeBankSkipButton = createOptionalActionButton(skipButtonTexture, t("btn_skip"));
        applyInputFieldStyle(bidField);
        applyInputFieldStyle(memeBankAmountField);
        bidField.setTextFieldFilter(new VisTextField.TextFieldFilter.DigitsOnlyFilter());
        memeBankAmountField.setTextFieldFilter(new VisTextField.TextFieldFilter.DigitsOnlyFilter());
        memeBankErrorLabel = new VisLabel("");
        memeBankErrorLabel.setColor(Color.RED);
        memeBankErrorLabel.setFontScale(0.7f);
        auctionErrorLabel = new VisLabel("");
        auctionErrorLabel.setColor(Color.RED);
        auctionErrorLabel.setFontScale(0.7f);
        createUi();
        createBattleOverlay();
        Gdx.input.setInputProcessor(stage);
    }

    private Texture loadTextureIfExistsLinear(String path) {
        return Gdx.files.internal(path).exists() ? loadTexture(path) : null;
    }

    private Texture loadOrFallback(String path, Texture fallback) {
        Texture t = loadTextureIfExists(path);
        return t != null ? t : fallback;
    }

    private void loadAvatarFrameTextures() {
        putAvatarFrame(Role.MEMOLOG, "purple_avatar.png");
        putAvatarFrame(Role.SMM, "blue_avatar.png");
        putAvatarFrame(Role.MONOPOLIST, "orange_avatar.png");
        putAvatarFrame(Role.MODERATOR, "red_avatar.png");
        putAvatarFrame(Role.SCAMMER, "yellow_avatar.png");
        putAvatarFrame(Role.DOGE, "green_avatar.png");
    }

    private void putAvatarFrame(Role role, String path) {
        Texture frame = loadTextureIfExists(path);
        if (frame != null) {
            avatarFrameByRole.put(role, frame);
        }
    }
    // GameScreen.java — createUi (фрагмент создания кнопок)

    private void createUi() {
        Table root = new Table();
        sideRoot = root;
        root.setFillParent(true);
        root.pad(18);
        diceTitleLabel.setColor(BATTLE_TEXT_COLOR);
        diceTitleLabel.setFontScale(0.92f);
        diceHintLabel.setWrap(true);
        diceHintLabel.setColor(BATTLE_TEXT_COLOR);
        diceHintLabel.setFontScale(1.00f);
        diceOverlay.center();
        Table diceContent = new Table();
        diceContent.center();
        stealButton = createTextureOrPillButton(stealButtonTexture, t("steal"));
        stealButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showScammerTargetWindow();
            }
        });
        stayButton = createTextureOrPillButton(stayButtonTexture, t("stay"));
        stayButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameState st = game.getLatestGameState();
                if (st != null && st.awaitingReroll && st.rerollPlayerId == game.getClient().getLocalPlayerId()) {
                    sendAction(GameActionRequest.ActionType.CONFIRM_LANDING, 0, 0);
                }
            }
        });
        plusTwoButton = createTextureOrPillButton(plusTwoButtonTexture, t("plus_two"));
        plusTwoButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameState st = game.getLatestGameState();
                if (st != null && st.awaitingReroll && st.rerollPlayerId == game.getClient().getLocalPlayerId()) {
                    sendAction(GameActionRequest.ActionType.PLUS_TWO, 0, 0);
                }
            }
        });
        moderatorSkipButton = createPillButton(t("skip_jail"));
        moderatorSkipButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.MODERATOR_SKIP_JAIL, 0, 0);
            }
        });
        moderatorTakeJailButton = createPillButton(t("take_jail"));
        moderatorTakeJailButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.MODERATOR_TAKE_JAIL, 0, 0);
            }
        });
        Table diceButtonColumn = new Table();
        diceButtonColumn.center();
        diceButtonColumn.add(diceButton).size(150, 64).row();
        diceButtonColumn.add(endTurnButton).size(150, 64).padTop(8f).row();
        diceButtonColumn.add(stealButton).size(150, 64).padTop(8f).row();
        diceButtonColumn.add(stayButton).size(150, 64).padTop(8f).row();
        diceButtonColumn.add(plusTwoButton).size(150, 64).padTop(8f).row();
        diceButtonColumn.add(moderatorSkipButton).size(150, 64).padTop(8f).row();
        diceButtonColumn.add(moderatorTakeJailButton).size(150, 64).padTop(8f).row();
        // Компенсирующий спейсер сверху = высоте блока подсказки (40+8):
        // тогда колонка кнопок центрируется СТРОГО по центру экрана,
        // а подсказка остаётся под колонкой и не тянет её вверх.
        diceContent.add().height(48f).row();
        diceContent.add(diceButtonColumn).center().row();
        diceContent.add(diceHintLabel).width(600f).height(40f).center().padTop(8f);
        diceOverlay.add(diceContent).center();
        titleLabel.setColor(TITLE_COLOR);
        titleLabel.setFontScale(0.98f);
        phaseLabel.setColor(BATTLE_TEXT_COLOR);
        phaseLabel.setWrap(true);
        phaseLabel.setFontScale(0.82f);
        turnLabel.setColor(BATTLE_TEXT_COLOR);
        turnLabel.setWrap(true);
        turnLabel.setFontScale(0.82f);
        cellLabel.setColor(BATTLE_TEXT_COLOR);
        cellLabel.setWrap(true);
        cellLabel.setFontScale(0.82f);
        logLabel.setWrap(true);
        logLabel.setColor(BATTLE_TEXT_COLOR);
        logLabel.setFontScale(0.82f);
        applyGameScreenLineSpacing();
        auctionLabel.setWrap(true);
        auctionLabel.setColor(BATTLE_TEXT_COLOR);
        playersTable.top().left();
        ownedCellsTable.top().center();
        handMemesTable.top().center();
        ScrollPane playersScroll = new ScrollPane(playersTable);
        playersScroll.setFadeScrollBars(false);
        playersScroll.setScrollingDisabled(true, false);
        playersScroll.getStyle().background = null;
        ScrollPane ownedScroll = new ScrollPane(ownedCellsTable);
        ownedScroll.setFadeScrollBars(false);
        ownedScroll.setScrollingDisabled(true, false);
        ownedScroll.getStyle().background = null;
        ScrollPane memesScroll = new ScrollPane(handMemesTable);
        memesScroll.setFadeScrollBars(false);
        memesScroll.setScrollingDisabled(true, false);
        memesScroll.getStyle().background = null;
        chatWidget = new ChatWidget(game, 340f);
        buyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.BUY_CELL, 0, 0);
            }
        });
        passButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameState st = game.getLatestGameState();
                if (st != null && st.awaitingReroll && st.rerollPlayerId == game.getClient().getLocalPlayerId()) {
                    sendAction(GameActionRequest.ActionType.CONFIRM_LANDING, 0, 0);
                } else {
                    sendAction(GameActionRequest.ActionType.PASS_BUY, 0, 0);
                }
            }
        });
        endTurnButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.END_TURN, 0, 0);
            }
        });
        bidField.setMessageText(t("bid_placeholder"));
        placeBidButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int bid = parseBid();
                if (bid <= 0) {
                    auctionErrorLabel.setText(t("bid_must_be_positive"));
                } else {
                    auctionErrorLabel.setText("");
                    sendAction(GameActionRequest.ActionType.PLACE_AUCTION_BID, 0, bid);
                    bidField.setText("");
                }
            }
        });
        cancelAuctionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.CANCEL_AUCTION, 0, 0);
            }
        });
        memeBankAmountField.setMessageText(t("amount_placeholder"));
        memeBankDepositButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int amount = parseAmount(memeBankAmountField);
                GameState state = game.getLatestGameState();
                Player localPlayer = state != null ? state.getPlayerById(game.getClient().getLocalPlayerId()) : null;
                if (amount <= 0)
                    memeBankErrorLabel.setText(t("amount_must_be_positive"));
                else if (amount > 500)
                    memeBankErrorLabel.setText(t("max_500_coins"));
                else if (localPlayer != null && amount > localPlayer.money)
                    memeBankErrorLabel.setText(t("not_enough_coins"));
                else {
                    memeBankErrorLabel.setText("");
                    sendAction(GameActionRequest.ActionType.MEME_BANK_DEPOSIT, 0, amount);
                    memeBankAmountField.setText("");
                }
            }
        });
        memeBankWithdrawButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.MEME_BANK_WITHDRAW, 0, 0);
            }
        });
        memeBankSkipButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.MEME_BANK_SKIP, 0, 0);
            }
        });
        Actor exitToMenuButton = createExitToMenuButton();
        dealButton = createTextureOrPillButton(dealButtonTexture, t("btn_deal"));
        dealButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameState gs = game.getLatestGameState();
                if (gs != null && gs.tradeId == 0 && tradeMode == TRADE_MODE_CLOSED) {
                    openTradeBuildWindow(gs);
                }
            }
        });
        VisLabel playersTitleLabel = new VisLabel("PLAYERS");
        playersTitleLabel.setFontScale(1.2f);
        playersTitleLabel.setColor(BATTLE_TEXT_COLOR);
        Table playersPanel = new Table();
        playersPanel.setBackground(window(playersWindowTexture));
        playersPanel.pad(24f, 14f, 14f, 14f);
        playersPanel.add(playersTitleLabel).center().padBottom(10f).row();
        playersPanel.add(playersScroll).grow().row();
        Table bottomRow = new Table();
        bottomRow.add(exitToMenuButton).width(220f).height(56f);
        bottomRow.add(dealButton).width(140f).height(56f).expandX().right();
        playersPanel.add(bottomRow).growX().padTop(10f).padBottom(16f);
        Table leftColumn = new Table();
        leftColumn.top();
        leftColumn.add(playersPanel).width(380f).expandY().fillY().row();
        leftColumn.add(chatWidget).width(380f).height(270f).padTop(16f);
        root.add(leftColumn).top().left();
        root.add().expand().fill();
        VisLabel ownedTitleLabel = new VisLabel("MY CELLS");
        ownedTitleLabel.setFontScale(1.2f);
        ownedTitleLabel.setColor(BATTLE_TEXT_COLOR);
        VisLabel memesTitleLabel = new VisLabel("MEMES");
        memesTitleLabel.setFontScale(1.2f);
        memesTitleLabel.setColor(BATTLE_TEXT_COLOR);
        Table ownedPanel = new Table();
        ownedPanel.setBackground(window(myCellsWindowTexture));
        ownedPanel.pad(24f, 12f, 14f, 12f);
        Table header = new Table();
        header.add(ownedTitleLabel).center();
        ownedPanel.add(header).growX().padBottom(10f).row();
        ownedPanel.add(ownedScroll).grow();
        Table memesPanel = new Table();
        memesPanel.setBackground(window(memesWindowTexture));
        memesPanel.pad(24f, 12f, 14f, 12f);
        memesPanel.add(memesTitleLabel).center().padBottom(10f).row();
        memesPanel.add(memesScroll).grow();
        Table rightColumn = new Table();
        rightColumn.top();
        rightColumn.add(ownedPanel).width(380f).height(480f).row();
        rightColumn.add(memesPanel).width(380f).expandY().fillY().padTop(16f).row();
        root.add(rightColumn).top().right();
        configureModal(turnNotificationModal, notificationWindowTexture, turnModalLabel, NOTIFICATION_MODAL_MIN_W,
                NOTIFICATION_MODAL_MIN_H, true);
        configureModal(buyOrAuctionModal, buyAndAuctionWindowTexture, buyAuctionModalLabel, BUY_AND_AUCTION_MODAL_MIN_W,
                BUY_AND_AUCTION_MODAL_MIN_H, false);
        configureModal(auctionModal, auctionOrMemeBankWindowTexture, auctionModalLabel, AUCTION_MODAL_MIN_W,
                AUCTION_MODAL_MIN_H, false);
        configureModal(memeBankModal, auctionOrMemeBankWindowTexture, memeBankModalLabel, MEME_BANK_MODAL_MIN_W,
                MEME_BANK_MODAL_MIN_H, false);
        setupModalControls();
        stage.addActor(root);
        stage.addActor(diceOverlay);
        stage.addActor(buyOrAuctionModal);
        stage.addActor(auctionModal);
        stage.addActor(memeBankModal);
        stage.addActor(turnNotificationModal);
        layoutBoardOverlays();
        roleTooltip = new Table();
        roleTooltip.setBackground(window(gameOverlayWindowTexture));
        roleTooltip.pad(12f);
        roleTooltipLabel = new VisLabel("");
        roleTooltipLabel.setWrap(true);
        roleTooltipLabel.setColor(BATTLE_TEXT_COLOR);
        roleTooltipLabel.setFontScale(0.8f);
        roleTooltip.add(roleTooltipLabel).width(260f);
        roleTooltip.setVisible(false);
        roleTooltip.setTouchable(Touchable.disabled);
        stage.addActor(roleTooltip);

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.F9) {
                    devPanelVisible = !devPanelVisible;
                    return true;
                }
                return false;
            }
        });

        createDevPanel();
    }

    private Table devPanel;
    private boolean devPanelVisible = false;
    private String lastDevPanelSig = "";

    private void createDevPanel() {
        devPanel = new Table();
        devPanel.top().left();
        devPanel.pad(8f);
        devPanel.setPosition(430f, WORLD_HEIGHT - 280f);
        devPanel.setVisible(false);
        stage.addActor(devPanel);
    }

    private void updateDevPanel(GameState state) {
        if (devPanel == null)
            return;
        devPanel.setVisible(devPanelVisible);
        if (!devPanelVisible)
            return;

        boolean isHost = game.isHost();
        boolean tm = state != null && state.testMode;
        String sig = isHost + "|" + tm + "|" + language;
        if (sig.equals(lastDevPanelSig) && devPanel.hasChildren()) {
            return;
        }
        lastDevPanelSig = sig;
        devPanel.clearChildren();

        if (!isHost) {
            VisLabel label = new VisLabel(t("dev_host_only"));
            label.setColor(BATTLE_TEXT_COLOR);
            devPanel.add(label).left().pad(4f);
        } else if (!tm) {
            VisTextButton enableBtn = createPillButton(t("dev_enable"));
            enableBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendAction(GameActionRequest.ActionType.DEV_SET_TEST_MODE, 0, 1);
                }
            });
            devPanel.add(enableBtn).left().pad(4f);
        } else {
            VisLabel indicator = new VisLabel(t("dev_test_mode_indicator"));
            indicator.setColor(BATTLE_TEXT_COLOR);
            indicator.setFontScale(1.1f);
            devPanel.add(indicator).left().padBottom(6f).row();

            VisTextButton nextBtn = createPillButton(t("dev_next_cell"));
            nextBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendAction(GameActionRequest.ActionType.DEV_NEXT_GROUP_CELL, 0, 0);
                }
            });
            devPanel.add(nextBtn).left().padBottom(4f).row();

            VisTextButton moneyBtn = createPillButton(t("dev_add_money"));
            moneyBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendAction(GameActionRequest.ActionType.DEV_SET_MONEY, 0, 1000);
                }
            });
            devPanel.add(moneyBtn).left().padBottom(4f).row();

            VisTextButton monopolyBtn = createPillButton(t("dev_grant_monopoly"));
            monopolyBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendAction(GameActionRequest.ActionType.DEV_GRANT_MONOPOLY, 0, 0);
                }
            });
            devPanel.add(monopolyBtn).left().padBottom(4f).row();

            VisTextButton disableBtn = createPillButton(t("dev_disable"));
            disableBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendAction(GameActionRequest.ActionType.DEV_SET_TEST_MODE, 0, 0);
                }
            });
            devPanel.add(disableBtn).left().padBottom(4f).row();
        }
        devPanel.pack();
        devPanel.setPosition(430f, WORLD_HEIGHT - 280f);
    }

    private Button createTextureOrPillButton(Texture texture, String fallbackText) {
        return texture != null ? createActionButton(texture) : createPillButton(fallbackText);
    }

    private Texture createCircleTexture(int radius) {
        int size = radius * 2;
        Pixmap p = new Pixmap(size, size, Pixmap.Format.RGBA8888);
        p.setColor(1f, 1f, 1f, 0.95f);
        float c = radius - 0.5f;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                float dx = x - c, dy = y - c;
                if (dx * dx + dy * dy <= radius * radius)
                    p.drawPixel(x, y);
            }
        }
        Texture t = new Texture(p);
        p.dispose();
        t.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return t;
    }

    private void updateRoleTooltip() {
        if (roleTooltip == null)
            return;
        Vector2 mouse = stage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
        Actor hovered = null;
        for (Map.Entry<Actor, Role> entry : roleInfoIcons.entrySet()) {
            Actor icon = entry.getKey();
            if (icon.getStage() == null)
                continue;
            Vector2 pos = icon.localToStageCoordinates(new Vector2(0, 0));
            if (mouse.x >= pos.x && mouse.x <= pos.x + icon.getWidth()
                    && mouse.y >= pos.y && mouse.y <= pos.y + icon.getHeight()) {
                hovered = icon;
                break;
            }
        }
        if (hovered != null) {
            roleTooltipLabel.setText(RoleInfo.description(roleInfoIcons.get(hovered), language == Language.RU));
            roleTooltip.pack();
            Vector2 pos = hovered.localToStageCoordinates(new Vector2(0, 0));
            float x = Math.min(pos.x + 30f, stage.getWidth() - roleTooltip.getWidth() - 8f);
            float y = Math.max(8f,
                    Math.min(pos.y - roleTooltip.getHeight() / 2f, stage.getHeight() - roleTooltip.getHeight() - 8f));
            roleTooltip.setPosition(x, y);
            roleTooltip.setVisible(true);
            roleTooltip.toFront();
        } else {
            roleTooltip.setVisible(false);
        }
    }

    private Drawable window(Texture texture) {
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private void createBattleOverlay() {
        battleOverlay = new Table();
        battleOverlay.setFillParent(true);
        battleOverlay.setVisible(false);
        battleOverlay.setTouchable(Touchable.disabled);
        stage.addActor(battleOverlay);
    }

    /** Открывает окно создания сделки (Инициатор) */
    private void openTradeWindow(GameState gs) {
        if (tradeWindow != null || isTradeWindowOpen)
            return;

        isInitiator = true;
        isIncomingTrade = false;
        tradeTargetId = null;
        selectedMyCells.clear();
        selectedTheirCells.clear();
        draftMyMoney = 0;
        draftTheirMoney = 0;

        // Скрываем оверлей кубиков
        if (diceOverlay != null)
            diceOverlay.setVisible(false);

        tradeWindow = new Window(t("window_trade_title"), VisUI.getSkin());
        tradeWindow.setSize(700, 420);
        float newX = (Gdx.graphics.getWidth() - tradeWindow.getWidth()) / 2f;
        float newY = (Gdx.graphics.getHeight() - tradeWindow.getHeight()) / 2f;
        tradeWindow.setPosition(newX, newY);

        // Основной контейнер: две половины
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.pad(10);

        Table leftPanel = new Table();
        leftPanel.setBackground(VisUI.getSkin().getDrawable("white"));
        leftPanel.pad(10);
        leftPanel.setColor(Color.LIGHT_GRAY);

        Table rightPanel = new Table();
        rightPanel.setBackground(VisUI.getSkin().getDrawable("white"));
        rightPanel.pad(10);
        rightPanel.setColor(new Color(0.8f, 0.8f, 0.8f, 1f));

        mainTable.add(leftPanel).expand().fill();
        mainTable.add(rightPanel).expand().fill();

        rebuildTradeWindowContent(gs, leftPanel, rightPanel);

        tradeWindow.add(mainTable).grow();

        // Кнопки управления
        Table btnTable = new Table();
        VisTextButton proposeBtn = new VisTextButton(t("btn_propose"));
        proposeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendTradeOffer();
            }
        });

        VisTextButton cancelBtn = new VisTextButton(t("btn_cancel"));
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                closeTradeWindow();
            }
        });

        btnTable.add(proposeBtn).padRight(10);
        btnTable.add(cancelBtn);

        tradeWindow.add(btnTable).row();

        stage.addActor(tradeWindow);

        // Включаем режим выбора на поле
        boardRenderer.setTradeDimming(getTradeableCellsList(gs));

        // Добавляем невидимый слой для кликов по полю
        addTradeTouchLayer(gs);

        isTradeWindowOpen = true;
    }

    /** Открывает окно входящей сделки (Цель, read-only) */
    private void showIncomingTradeWindow(GameState gs) {
        if (tradeWindow != null || isTradeWindowOpen)
            return;

        isInitiator = false;
        isIncomingTrade = true;

        // Скрываем оверлей кубиков
        if (diceOverlay != null)
            diceOverlay.setVisible(false);

        tradeWindow = new Window(t("window_trade_title"), VisUI.getSkin());
        tradeWindow.setSize(700, 420);
        float newX = (Gdx.graphics.getWidth() - tradeWindow.getWidth()) / 2f;
        float newY = (Gdx.graphics.getHeight() - tradeWindow.getHeight()) / 2f;
        tradeWindow.setPosition(newX, newY);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.pad(10);

        Table leftPanel = new Table();
        leftPanel.setBackground(VisUI.getSkin().getDrawable("white"));
        leftPanel.pad(10);
        leftPanel.setColor(Color.LIGHT_GRAY);

        Table rightPanel = new Table();
        rightPanel.setBackground(VisUI.getSkin().getDrawable("white"));
        rightPanel.pad(10);
        rightPanel.setColor(new Color(0.8f, 0.8f, 0.8f, 1f));

        mainTable.add(leftPanel).expand().fill();
        mainTable.add(rightPanel).expand().fill();

        rebuildTradeWindowContent(gs, leftPanel, rightPanel);

        tradeWindow.add(mainTable).grow();

        // Кнопки только для принимающего
        Table btnTable = new Table();
        VisTextButton acceptBtn = new VisTextButton(t("btn_accept"));
        acceptBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendTradeResponse(true);
            }
        });

        VisTextButton declineBtn = new VisTextButton(t("btn_decline"));
        declineBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendTradeResponse(false);
            }
        });

        btnTable.add(acceptBtn).padRight(10);
        btnTable.add(declineBtn);

        tradeWindow.add(btnTable).row();

        stage.addActor(tradeWindow);

        isTradeWindowOpen = true;
    }

    /** Пересобирает содержимое окна сделки */
    private void rebuildTradeWindowContent(GameState gs, Table leftPanel, Table rightPanel) {
        leftPanel.clear();
        rightPanel.clear();

        Player localPlayer = gs.players.get(
                gs.currentPlayerIndex >= 0 && gs.currentPlayerIndex < gs.players.size() ? gs.currentPlayerIndex : 0);
        String initiatorName = isInitiator ? (localPlayer != null ? localPlayer.name : "You")
                : getPlayerName(gs, gs.tradeProposerId);
        String targetName = isInitiator ? getOpponentName(gs) : (localPlayer != null ? localPlayer.name : "You");

        // Левая панель (инициатор)
        VisLabel initNameLabel = new VisLabel(initiatorName);
        initNameLabel.setFontScale(1.1f);
        leftPanel.add(initNameLabel).center().padBottom(5f).row();

        // Слайдер денег инициатора (только если не read-only)
        if (!isIncomingTrade && isInitiator) {
            int maxMoney = localPlayer != null ? localPlayer.money : 0;
            VisSlider initSlider = createMoneySlider(0, maxMoney, 1, 0);
            initSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    draftMyMoney = (int) initSlider.getValue();
                }
            });
            leftPanel.add(initSlider).width(200).padBottom(10f).row();
            initSlider.setVisible(selectedMyCells.size() > 0);
        } else if (isIncomingTrade) {
            VisLabel initMoneyLabel = new VisLabel(t("money_offered") + ": " + gs.tradeProposerMoney);
            leftPanel.add(initMoneyLabel).padBottom(10f).row();
        }

        // Чипы клеток инициатора
        Table initCellsTable = new Table();
        initCellsTable.defaults().size(50, 50).pad(2);
        ArrayList<Integer> cells = isInitiator ? new ArrayList<>(selectedMyCells) : gs.tradeProposerCells;
        for (int cellId : cells) {
            Image cellImg = new Image(cellTextures[cellId % cellTextures.length]);
            initCellsTable.add(cellImg);
        }
        leftPanel.add(initCellsTable).row();

        // Правая панель (цель)
        VisLabel targetNameLabel = new VisLabel(targetName);
        targetNameLabel.setFontScale(1.1f);
        rightPanel.add(targetNameLabel).center().padBottom(5f).row();

        // Слайдер денег цели (только если выбрана цель и не read-only)
        if (!isIncomingTrade && isInitiator && tradeTargetId != null) {
            Player targetPlayer = gs.getPlayerById(tradeTargetId);
            int maxMoney = targetPlayer != null ? targetPlayer.money : 0;
            VisSlider targetSlider = createMoneySlider(0, maxMoney, 1, 0);
            targetSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    draftTheirMoney = (int) targetSlider.getValue();
                }
            });
            rightPanel.add(targetSlider).width(200).padBottom(10f).row();
            targetSlider.setVisible(selectedTheirCells.size() > 0);
        } else if (isIncomingTrade) {
            VisLabel targetMoneyLabel = new VisLabel(t("money_requested") + ": " + gs.tradeTargetMoney);
            rightPanel.add(targetMoneyLabel).padBottom(10f).row();
        }

        // Чипы клеток цели
        Table targetCellsTable = new Table();
        targetCellsTable.defaults().size(50, 50).pad(2);
        ArrayList<Integer> targetCells = isInitiator ? new ArrayList<>(selectedTheirCells) : gs.tradeTargetCells;
        for (int cellId : targetCells) {
            Image cellImg = new Image(cellTextures[cellId % cellTextures.length]);
            targetCellsTable.add(cellImg);
        }
        rightPanel.add(targetCellsTable).row();
    }

    /** Создаёт слайдер для выбора суммы */
    private VisSlider createMoneySlider(float min, float max, float step, float defaultValue) {
        VisSlider slider = new VisSlider(min, max, step, false);
        slider.setValue(defaultValue);
        slider.setAnimateDuration(0.2f);
        return slider;
    }

    /** Возвращает список торгуемых клеток для затемнения */
    private Set<Integer> getTradeableCellsList(GameState gs) {
        Set<Integer> tradeable = new HashSet<>();
        Player localPlayer = gs.players.get(
                gs.currentPlayerIndex >= 0 && gs.currentPlayerIndex < gs.players.size() ? gs.currentPlayerIndex : 0);
        if (localPlayer == null)
            return tradeable;

        // Свои клетки (без филиалов)
        for (int cellId : localPlayer.ownedCells) {
            if (gs.cellHouses.getOrDefault(cellId, 0) == 0) {
                tradeable.add(cellId);
            }
        }

        // Клетки других игроков (для выбора цели)
        for (Player p : gs.players) {
            if (p.id != localPlayer.id && !p.isBankrupt) {
                for (int cellId : p.ownedCells) {
                    if (gs.cellHouses.getOrDefault(cellId, 0) == 0) {
                        tradeable.add(cellId);
                    }
                }
            }
        }

        return tradeable;
    }

    /** Добавляет невидимый слой для обработки кликов по полю */
    private void addTradeTouchLayer(GameState gs) {
        if (tradeTouchLayer != null)
            tradeTouchLayer.remove();

        tradeTouchLayer = new Actor();
        com.badlogic.gdx.math.Rectangle bounds = boardRenderer.getBoardBounds();
        tradeTouchLayer.setBounds(bounds.x, bounds.y, bounds.width, bounds.height);
        tradeTouchLayer.setTouchable(Touchable.enabled);
        tradeTouchLayer.addListener(new com.badlogic.gdx.scenes.scene2d.InputListener() {
            @Override
            public boolean touchDown(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y, int pointer,
                    int button) {
                handleCellClickForTrade(gs, x, y);
                return true;
            }
        });

        stage.addActor(tradeTouchLayer);
    }

    // ===================== СДЕЛКИ =====================

    private void openTradeBuildWindow(GameState gs) {
        selectedMyCells.clear();
        selectedTheirCells.clear();
        tradeTargetId = null;
        draftMyMoney = 0;
        draftTheirMoney = 0;
        tradeMode = TRADE_MODE_BUILD;
        if (diceOverlay != null)
            diceOverlay.setVisible(false);
        addTradeTouchLayer();
        buildTradeWindow(gs);
        updateTradeDimming(gs);
    }

    private void buildTradeWindow(GameState gs) {
        tradeWindow = new Table();
        tradeWindow.setBackground(window(buyAndAuctionWindowTexture));
        tradeWindow.pad(24f);
        tradeWindow.setSize(900f, 560f);
        tradeWindow.setPosition((WORLD_WIDTH - 900f) / 2f, (WORLD_HEIGHT - 560f) / 2f);
        stage.addActor(tradeWindow);
        rebuildTradeContent(gs);
    }

    private void rebuildTradeContent(GameState gs) {
        tradeWindow.clearChildren();

        VisLabel title = new VisLabel(t("window_trade_title"));
        title.setColor(BATTLE_TEXT_COLOR);
        title.setFontScale(1.2f);
        tradeWindow.add(title).center().padBottom(12f).row();

        Table halves = new Table();
        halves.add(buildTradeHalf(gs, true)).expand().fill().padRight(10f);
        halves.add(buildTradeHalf(gs, false)).expand().fill();
        tradeWindow.add(halves).expand().fill().padBottom(12f).row();

        Table btnRow = new Table();
        if (tradeMode == TRADE_MODE_BUILD) {
            ImageButton proposeBtn = createActionButton(placeBidButtonTexture);
            proposeBtn.setDisabled(selectedMyCells.isEmpty());
            proposeBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendTradeOffer();
                }
            });
            ImageButton cancelBtn = createActionButton(declineButtonTexture);
            cancelBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    closeTradeWindow();
                }
            });
            btnRow.add(proposeBtn).size(180f, 56f).padRight(14f);
            btnRow.add(cancelBtn).size(180f, 56f);
        } else if (tradeMode == TRADE_MODE_WAIT) {
            VisLabel wait = new VisLabel(t("trade_waiting"));
            wait.setColor(BATTLE_TEXT_COLOR);
            ImageButton cancelBtn = createActionButton(declineButtonTexture);
            cancelBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    cancelTrade();
                }
            });
            btnRow.add(wait).padRight(14f);
            btnRow.add(cancelBtn).size(180f, 56f);
        } else { // TRADE_MODE_INCOMING
            ImageButton acceptBtn = createActionButton(participateButtonTexture);
            acceptBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendTradeResponse(true);
                }
            });
            ImageButton declineBtn = createActionButton(declineButtonTexture);
            declineBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendTradeResponse(false);
                }
            });
            btnRow.add(acceptBtn).size(180f, 56f).padRight(14f);
            btnRow.add(declineBtn).size(180f, 56f);
        }
        tradeWindow.add(btnRow).center().row();
    }

    private Table buildTradeHalf(GameState gs, boolean left) {
        Table half = new Table();
        half.setBackground(window(gameOverlayWindowTexture));
        half.pad(14f);

        int localId = game.getClient().getLocalPlayerId();
        String name;
        Set<Integer> cells;
        int money;
        int maxMoney;

        if (tradeMode == TRADE_MODE_INCOMING) {
            if (left) {
                name = getPlayerName(gs, gs.tradeProposerId);
                cells = new HashSet<>(gs.tradeProposerCells);
                money = gs.tradeProposerMoney;
            } else {
                name = getPlayerName(gs, gs.tradeTargetId);
                cells = new HashSet<>(gs.tradeTargetCells);
                money = gs.tradeTargetMoney;
            }
            maxMoney = money;
        } else {
            if (left) {
                Player p = gs.getPlayerById(localId);
                name = (p != null ? p.name : "?") + " (" + t("trade_you") + ")";
                cells = selectedMyCells;
                money = draftMyMoney;
                maxMoney = p != null ? p.money : 0;
            } else {
                Player target = tradeTargetId != null ? gs.getPlayerById(tradeTargetId) : null;
                name = target != null ? target.name : t("select_opponent");
                cells = selectedTheirCells;
                money = draftTheirMoney;
                maxMoney = target != null ? target.money : 0;
            }
        }

        VisLabel nameLabel = new VisLabel(name);
        nameLabel.setColor(BATTLE_TEXT_COLOR);
        nameLabel.setFontScale(1.05f);
        half.add(nameLabel).center().padBottom(8f).row();

        boolean editable = tradeMode == TRADE_MODE_BUILD;
        Image coin = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        coin.setScaling(Scaling.fit);
        VisLabel moneyLabel = new VisLabel(String.valueOf(money));
        moneyLabel.setColor(BATTLE_TEXT_COLOR);

        if (editable) {
            VisSlider slider = new VisSlider(0, Math.max(1, maxMoney), 1, false);
            slider.setValue(money);
            slider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    int v = (int) slider.getValue();
                    if (left)
                        draftMyMoney = v;
                    else
                        draftTheirMoney = v;
                    moneyLabel.setText(String.valueOf(v));
                }
            });
            Table sliderRow = new Table();
            sliderRow.add(slider).width(200f).padRight(10f);
            sliderRow.add(moneyLabel).padRight(4f);
            sliderRow.add(coin).size(20f, 20f);
            half.add(sliderRow).center().padBottom(8f).row();
        } else {
            Table moneyRow = new Table();
            moneyRow.add(moneyLabel).padRight(4f);
            moneyRow.add(coin).size(20f, 20f);
            half.add(moneyRow).center().padBottom(8f).row();
        }

        Table chips = new Table();
        int c = 0;
        for (int cellId : cells) {
            chips.add(createCellChip(cellId, editable, left)).size(96f, 72f).pad(4f);
            if (++c % 3 == 0)
                chips.row();
        }
        if (c == 0) {
            VisLabel hint = new VisLabel(left ? t("trade_pick_own") : t("trade_pick_their"));
            hint.setColor(BATTLE_TEXT_COLOR);
            hint.setWrap(true);
            hint.setFontScale(0.8f);
            chips.add(hint).width(280f).center();
        }
        half.add(chips).expand().fill().top().row();
        return half;
    }

    private Table createCellChip(int cellId, boolean clickable, boolean leftSide) {
        Table chip = new Table();
        chip.setBackground(window(cardBoardTexture));
        chip.pad(4f);
        Image img = new Image(cellTextures[cellId % cellTextures.length]);
        img.setScaling(Scaling.fill);
        chip.add(img).expand().fill();
        if (clickable) {
            chip.setTouchable(Touchable.enabled);
            chip.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    GameState gs = game.getLatestGameState();
                    if (gs == null || tradeMode != TRADE_MODE_BUILD)
                        return;
                    if (leftSide) {
                        selectedMyCells.remove(cellId);
                    } else {
                        selectedTheirCells.remove(cellId);
                        if (selectedTheirCells.isEmpty())
                            tradeTargetId = null;
                    }
                    rebuildTradeContent(gs);
                    updateTradeDimming(gs);
                }
            });
        }
        return chip;
    }

    private void addTradeTouchLayer() {
        removeTradeTouchLayer();
        Table layer = new Table();
        com.badlogic.gdx.math.Rectangle b = boardRenderer.getBoardBounds();
        layer.setBounds(b.x, b.y, b.width, b.height);
        layer.setTouchable(Touchable.enabled);
        layer.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                GameState gs = game.getLatestGameState();
                if (gs == null || tradeMode != TRADE_MODE_BUILD)
                    return;
                handleCellClickForTrade(gs, layer.getX() + x, layer.getY() + y);
            }
        });
        stage.addActor(layer);
        tradeTouchLayer = layer;
    }

    private void removeTradeTouchLayer() {
        if (tradeTouchLayer != null) {
            tradeTouchLayer.remove();
            tradeTouchLayer = null;
        }
    }

    private void handleCellClickForTrade(GameState gs, float worldX, float worldY) {
        int cellId = boardRenderer.getCellAt(worldX, worldY);
        if (cellId < 0)
            return;
        Integer ownerId = gs.cellOwners.get(cellId);
        if (ownerId == null)
            return;
        if (gs.cellHouses.getOrDefault(cellId, 0) > 0)
            return;
        int localId = game.getClient().getLocalPlayerId();
        if (ownerId == localId) {
            if (!selectedMyCells.remove(cellId))
                selectedMyCells.add(cellId);
        } else {
            if (tradeTargetId == null) {
                tradeTargetId = ownerId;
                selectedTheirCells.add(cellId);
            } else if (tradeTargetId == ownerId) {
                if (!selectedTheirCells.remove(cellId))
                    selectedTheirCells.add(cellId);
                if (selectedTheirCells.isEmpty())
                    tradeTargetId = null;
            } else {
                return;
            }
        }
        rebuildTradeContent(gs);
        updateTradeDimming(gs);
    }

    private void updateTradeDimming(GameState gs) {
        if (tradeMode != TRADE_MODE_BUILD) {
            boardRenderer.setTradeDimming(null);
            return;
        }
        int localId = game.getClient().getLocalPlayerId();
        Set<Integer> selectable = new HashSet<>();
        for (Player p : gs.players) {
            if (p.isBankrupt)
                continue;
            boolean allowed = (p.id == localId) || (tradeTargetId == null) || (p.id == tradeTargetId);
            if (!allowed)
                continue;
            for (int cellId : p.ownedCells) {
                if (gs.cellHouses.getOrDefault(cellId, 0) == 0)
                    selectable.add(cellId);
            }
        }
        boardRenderer.setTradeDimming(selectable);
    }

    private void sendTradeOffer() {
        GameState gs = game.getLatestGameState();
        if (gs == null || tradeTargetId == null || selectedMyCells.isEmpty())
            return;
        TradeOfferPacket packet = new TradeOfferPacket();
        packet.targetId = tradeTargetId;
        packet.myCells = new ArrayList<>(selectedMyCells);
        packet.theirCells = new ArrayList<>(selectedTheirCells);
        packet.myMoney = draftMyMoney;
        packet.theirMoney = draftTheirMoney;
        game.getClient().sendTradeOffer(packet);
        tradeMode = TRADE_MODE_WAIT;
        removeTradeTouchLayer();
        boardRenderer.setTradeDimming(null);
        if (diceOverlay != null)
            diceOverlay.setVisible(true);
        rebuildTradeContent(gs);
    }

    private void sendTradeResponse(boolean accept) {
        TradeResponsePacket packet = new TradeResponsePacket();
        packet.accept = accept;
        game.getClient().sendTradeResponse(packet);
        closeTradeWindow();
    }

    private void cancelTrade() {
        game.getClient().sendTradeCancel(new TradeCancelPacket());
        closeTradeWindow();
    }

    private void closeTradeWindow() {
        if (tradeWindow != null) {
            tradeWindow.remove();
            tradeWindow = null;
        }
        removeTradeTouchLayer();
        boardRenderer.setTradeDimming(null);
        tradeMode = TRADE_MODE_CLOSED;
        selectedMyCells.clear();
        selectedTheirCells.clear();
        tradeTargetId = null;
        draftMyMoney = 0;
        draftTheirMoney = 0;
        if (diceOverlay != null)
            diceOverlay.setVisible(true);
    }

    private String getPlayerName(GameState gs, int playerId) {
        Player p = gs.getPlayerById(playerId);
        return p != null ? p.name : "?";
    }

    /** Получает имя соперника для сделки */
    private String getOpponentName(GameState gs) {
        if (tradeTargetId != null) {
            return getPlayerName(gs, tradeTargetId);
        }
        return t("select_opponent");
    }

    private Meme getSubmittedBattleMeme(GameState state, int playerId) {
        if (state == null || state.battleMemes == null) {
            return null;
        }
        for (Meme meme : state.battleMemes) {
            if (meme.ownerId == playerId) {
                return meme;
            }
        }
        return null;
    }

    private void refreshBattleOverlay(GameState state) {
        boolean inBattle = state != null && state.currentPhase == GameState.GamePhase.MEME_BATTLE;
        boardRenderer.setBattleMode(inBattle);
        diceOverlay.setVisible(!inBattle); // убирает «Твой ход» поверх баттла
        if (!inBattle) {
            battleOverlay.clearChildren();
            battleOverlay.setVisible(false);
            battleOverlay.setTouchable(Touchable.disabled);
            lastBattleSignature = "";
            selectedBattleMemeId = -1;
            battleTimerLabel = null;
            return;
        }
        battleOverlay.setVisible(true);
        battleOverlay.setTouchable(Touchable.enabled);
        int localId = game.getClient().getLocalPlayerId();
        String signature = buildBattleSignature(state, localId);
        if (!signature.equals(lastBattleSignature)) {
            lastBattleSignature = signature;
            selectedBattleMemeId = -1;
            rebuildBattleContent(state);
        }
        if (battleTimerLabel != null) {
            int t = state.battleTimerSeconds > 0 ? state.battleTimerSeconds : state.currentAuctionTime;
            battleTimerLabel.setText(String.valueOf(Math.max(0, t)));
        }
    }

    private void rebuildBattleContent(GameState state) {
        battleOverlay.clearChildren();
        battleTimerLabel = new VisLabel("");
        battleTimerLabel.setColor(BATTLE_TEXT_COLOR);
        battleTimerLabel.setFontScale(1.1f);
        int localId = game.getClient().getLocalPlayerId();
        boolean isOwner = state.battleOwnerId == localId;
        Table panel = new Table();
        panel.center();
        switch (state.battlePhase) {
            case BATTLE_SETUP:
                panel.add(isOwner ? buildStakePanel() : buildWaitPanel(t("organizer_chooses"))).center().expand();
                break;
            case INVITE:
                panel.add(isOwner ? buildWaitPanel(t("waiting_answers")) : buildInvitePanel(state)).center().expand();
                break;
            case COLLECTING_MEMES:
                panel.add(buildCollectingPanel(state, isOwner, localId)).center().expand();
                break;
            case VOTING:
                panel.add(buildVotingPanel(state, localId)).center().expand();
                break;
            case RESULTS:
                panel.add(buildWaitPanel(state.lastActionLog == null ? t("battle_finished") : state.lastActionLog))
                        .center().expand();
                break;
            default:
                break;
        }
        Stack stack = new Stack();
        Table centerHolder = new Table();
        centerHolder.center();
        centerHolder.add(panel).width(860f).height(920f);
        stack.add(centerHolder);
        Table timerHolder = new Table();
        timerHolder.top().left();
        Stack timerStack = new Stack();
        Image circle = new Image(timerCircleTexture);
        circle.setScaling(Scaling.fit);
        timerStack.add(circle);
        Table labelHolder = new Table();
        labelHolder.add(battleTimerLabel);
        timerStack.add(labelHolder);
        timerHolder.add(timerStack).size(72f, 72f).pad(24f);
        stack.add(timerHolder);
        battleOverlay.add(stack).expand().fill();
    }

    private Table buildWaitPanel(String text) {
        Table p = new Table();
        VisLabel label = new VisLabel(text);
        label.setColor(BATTLE_TEXT_COLOR);
        label.setWrap(true);
        label.setFontScale(1.1f);
        p.add(label).width(600f).center();
        return p;
    }

    private Table buildStakePanel() {
        Table p = new Table();
        p.center();
        VisLabel title = new VisLabel(t("choose_stakes"));
        title.setColor(BATTLE_TEXT_COLOR);
        title.setFontScale(1.2f);
        p.add(title).center().padBottom(18f).row();
        localStake = 50;
        stakeValueLabel = new VisLabel(String.valueOf(localStake));
        stakeValueLabel.setColor(BATTLE_TEXT_COLOR);
        stakeValueLabel.setFontScale(1.2f);
        Image moneyIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        moneyIcon.setScaling(Scaling.fit);
        Table stakeRow = new Table();
        stakeRow.add(stakeValueLabel).padRight(8f);
        stakeRow.add(moneyIcon).size(28f, 28f);
        p.add(stakeRow).center().padBottom(26f).row();
        ImageButton minusBtn = createActionButton(tenDownTexture);
        minusBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                localStake = Math.max(10, localStake - 10);
                stakeValueLabel.setText(String.valueOf(localStake));
            }
        });
        ImageButton plusBtn = createActionButton(tenUpTexture);
        plusBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                localStake = Math.min(200, localStake + 10);
                stakeValueLabel.setText(String.valueOf(localStake));
            }
        });
        ImageButton proposeBtn = createActionButton(placeBidButtonTexture);
        proposeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.START_MEME_BATTLE, 0, localStake, "");
            }
        });
        ImageButton declineBtn = createActionButton(declineButtonTexture);
        declineBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.CANCEL_MEME_BATTLE, 0, 0, null);
            }
        });
        Table btnRow = new Table();
        btnRow.add(minusBtn).size(70f, 70f).padRight(10f);
        btnRow.add(plusBtn).size(70f, 70f).padRight(30f);
        btnRow.add(proposeBtn).size(170f, 60f).padRight(10f);
        btnRow.add(declineBtn).size(170f, 60f);
        p.add(btnRow).center().row();
        return p;
    }

    private Table buildInvitePanel(GameState state) {
        Table p = new Table();
        p.center();
        VisLabel label = new VisLabel(t("invite_battle") + " " + state.battleStakes);
        label.setColor(BATTLE_TEXT_COLOR);
        label.setWrap(true);
        label.setFontScale(1.1f);
        p.add(label).width(700f).center().padBottom(20f).row();
        ImageButton yes = createActionButton(participateButtonTexture);
        yes.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                BattleResponsePacket packet = new BattleResponsePacket();
                packet.playerId = game.getClient().getLocalPlayerId();
                packet.accepted = true;
                game.getClient().sendBattleResponse(packet);
            }
        });
        ImageButton no = createActionButton(declineButtonTexture);
        no.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                BattleResponsePacket packet = new BattleResponsePacket();
                packet.playerId = game.getClient().getLocalPlayerId();
                packet.accepted = false;
                game.getClient().sendBattleResponse(packet);
            }
        });
        Table btnRow = new Table();
        btnRow.add(yes).size(180f, 64f).padRight(14f);
        btnRow.add(no).size(180f, 64f);
        p.add(btnRow).center().row();
        return p;
    }

    private Table buildCollectingPanel(GameState state, boolean isOwner, int localId) {
        Table p = new Table();
        p.center();
        Meme submitted = getSubmittedBattleMeme(state, localId);
        Table topRow = new Table();
        if (isOwner && submitted == null) {
            topicField = new VisTextField();
            topicField.setMessageText(t("topic_placeholder"));
            applyBattleInputFieldStyle(topicField);
            topicField.setMaxLength(120);
            topRow.add(topicField).width(470f).height(60f).left();
        } else {
            String topic = state.battleTopic == null || state.battleTopic.isBlank() ? t("waiting_topic")
                    : state.battleTopic;
            VisLabel topicLabel = new VisLabel(topic);
            topicLabel.setColor(BATTLE_TEXT_COLOR);
            topicLabel.setWrap(true);
            topRow.add(topicLabel).width(470f).left();
        }
        startBattleButton = createActionButton(startBattleTexture);
        startBattleButton.setDisabled(selectedBattleMemeId == -1 || submitted != null);
        startBattleButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (selectedBattleMemeId == -1)
                    return;
                String topic = (topicField != null) ? topicField.getText().trim() : "";
                sendAction(GameActionRequest.ActionType.SUBMIT_MEME, selectedBattleMemeId, 0, topic);
            }
        });
        topRow.add(startBattleButton).size(120f, 60f).padLeft(14f).right();
        p.add(topRow).width(700f).center().padBottom(14f).row();
        VisLabel choose = new VisLabel(t("choose_funniest"));
        choose.setColor(BATTLE_TEXT_COLOR);
        choose.setFontScale(0.8f);
        choose.setWrap(true);
        p.add(choose).width(800f).center().padBottom(12f).row();
        Table grid = new Table();
        int col = 0, count = 0;
        Player localPlayer = state.getPlayerById(localId);
        if (localPlayer != null && localPlayer.handMemes != null) {
            for (Meme meme : localPlayer.handMemes) {
                if (count >= 6)
                    break;
                boolean selected = meme.id == selectedBattleMemeId;
                grid.add(createBattleMemeCard(meme, selected, submitted != null, 0, () -> {
                    selectedBattleMemeId = (selectedBattleMemeId == meme.id) ? -1 : meme.id;
                    rebuildBattleContent(state);
                })).size(330f, 248f).pad(10f);
                count++;
                if (++col % 2 == 0)
                    grid.row();
            }
        }
        p.add(grid).center().row();
        return p;
    }

    private Table buildVotingPanel(GameState state, int localId) {
        Table p = new Table();
        p.center();
        boolean hasVoted = state.battleVoters != null && state.battleVoters.contains(localId);
        if (hasVoted) {
            return buildWaitPanelInner(p, t("already_voted"));
        }
        VisLabel hint = new VisLabel(t("vote_hint"));
        hint.setColor(BATTLE_TEXT_COLOR);
        hint.setFontScale(0.9f);
        p.add(hint).center().padBottom(12f).row();
        Table grid = new Table();
        int col = 0;
        if (state.battleMemes != null) {
            for (Meme meme : state.battleMemes) {
                final int memeId = meme.id;
                boolean ownMeme = meme.ownerId == localId;
                grid.add(createBattleMemeCard(meme, false, ownMeme, state.votes.getOrDefault(memeId, 0),
                        () -> sendAction(GameActionRequest.ActionType.VOTE_MEME, memeId, 0, null))).size(330f, 248f)
                        .pad(10f);
                if (++col % 2 == 0)
                    grid.row();
            }
        }
        p.add(grid).center().row();
        return p;
    }

    private Table buildWaitPanelInner(Table p, String text) {
        VisLabel label = new VisLabel(text);
        label.setColor(BATTLE_TEXT_COLOR);
        label.setWrap(true);
        p.add(label).width(600f).center();
        return p;
    }

    private Table createBattleMemeCard(Meme meme, boolean selected, boolean disabled, int votes, Runnable action) {
        Table card = new Table();
        card.pad(6f);
        Stack content = new Stack();
        Drawable memeDrawable = getMemeDrawable(meme);
        if (memeDrawable != null) {
            Image image = new Image(memeDrawable);
            image.setScaling(Scaling.fill);
            content.add(image);
        } else {
            VisLabel placeholder = new VisLabel(t("no_preview"));
            placeholder.setColor(BATTLE_TEXT_COLOR);
            content.add(placeholder);
        }
        if (votes > 0) {
            Table badgeOverlay = new Table();
            badgeOverlay.top().right();
            Table badge = new Table();
            badge.setBackground(new TextureRegionDrawable(new TextureRegion(UiShapes.circle(22, Color.WHITE))));
            VisLabel count = new VisLabel(String.valueOf(votes));
            count.setColor(BATTLE_TEXT_COLOR);
            badge.add(count);
            badgeOverlay.add(badge).size(44f).pad(6f);
            content.add(badgeOverlay);
        }
        card.add(content).expand().fill();
        if (selected) {
            card.setOrigin(com.badlogic.gdx.utils.Align.center);
            card.setScale(1.05f);
        }
        card.setTouchable(disabled ? Touchable.disabled : Touchable.enabled);
        if (!disabled && action != null) {
            card.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    action.run();
                }
            });
        }
        return card;
    }

    private String buildBattleSignature(GameState state, int localId) {
        StringBuilder sb = new StringBuilder();
        sb.append(state.battlePhase).append('|');
        sb.append(state.battleMemes == null ? 0 : state.battleMemes.size()).append('|');
        if (state.battleMemes != null) {
            for (Meme m : state.battleMemes) {
                sb.append(m.id).append(':').append(m.ownerId).append(';');
            }
        }
        sb.append('|');
        sb.append(state.battleParticipants).append('|');
        sb.append(state.battleInvited).append('|');
        sb.append(state.battleVoters).append('|');
        sb.append(state.lastActionLog).append('|');
        Player localPlayer = state.getPlayerById(localId);
        if (localPlayer != null && localPlayer.handMemes != null) {
            for (Meme m : localPlayer.handMemes) {
                sb.append(m.id).append(',');
            }
        }
        return sb.toString();
    }

    private int parseBid() {
        try {
            return Integer.parseInt(bidField.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private int parseAmount(VisTextField field) {
        try {
            return Integer.parseInt(field.getText().trim());
        } catch (NumberFormatException e) {
            return -1;
        }
    }

    private void sendAction(GameActionRequest.ActionType type, int targetId, int amount) {
        sendAction(type, targetId, amount, null);
    }

    private void sendAction(GameActionRequest.ActionType type, int targetId, int amount, String data) {
        GameActionRequest request = new GameActionRequest();
        request.actionType = type;
        request.targetId = targetId;
        request.amount = amount;
        request.data = data;
        game.getClient().sendGameAction(request);
    }

    // GameScreen.java — refreshUi (замена dealButton.setDisabled)

    private void refreshUi(GameState state) {
        if (state == null) {
            titleLabel.setText(t("title"));
            phaseLabel.setText(t("phase_waiting_data"));
            turnLabel.setText(t("turn_idle"));
            cellLabel.setText(t("cell_idle"));
            logLabel.setText(t("log_connecting"));
            currentCellMetaLabel.setText(t("cell_meta_connecting"));
            currentCellImage.setDrawable(null);
            diceHintLabel.setText(t("dice_hint_waiting"));
            feedDescriptionLabel.setText(t("feed_connecting"));
            rebuildPlayersIfNeeded(null, null, -1);
            rebuildOwnedCellsIfNeeded(null, null, false, GameState.GamePhase.WAITING);
            setButtonsEnabled(false, false, false, false, false);
            auctionLabel.setText("");
            return;
        }

        if (state.players != null) {
            Set<Integer> currentPlayerIds = new HashSet<>();
            for (Player p : state.players) {
                currentPlayerIds.add(p.id);
                Integer lastPos = lastKnownPositions.get(p.id);
                if (lastPos == null) {
                    lastKnownPositions.put(p.id, p.position);
                } else if (lastPos != p.position) {
                    int from = lastPos;
                    if (state.awaitingReroll && p.id == state.rerollPlayerId && state.rerollOrigin != -1
                            && state.rerollOrigin != lastPos) {
                        from = state.rerollOrigin;
                    }
                    boardRenderer.animateMovement(p.id, from, p.position);
                    lastKnownPositions.put(p.id, p.position);
                }
            }
            lastKnownPositions.keySet().retainAll(currentPlayerIds);
        }

        Player current = state.getCurrentPlayer();
        int localPlayerId = game.getClient().getLocalPlayerId();
        Player localPlayer = state.getPlayerById(localPlayerId);
        boolean myTurn = current != null && current.id == localPlayerId;
        BoardCell currentCell = current != null ? boardCells.get(current.position) : null;

        if (current != null && current.id != lastTurnPlayerId) {
            lastTurnPlayerId = current.id;
            chatWidget.showSystemMessage(t("turn_prefix") + " " + current.name);
        }

        phaseLabel.setText(t("phase_prefix") + " " + state.currentPhase);
        turnLabel.setText(current == null ? t("turn_idle")
                : t("turn_prefix") + " " + current.name + " | " + t("turn_number") + " " + state.turnCount);
        cellLabel.setText(currentCell == null ? t("cell_idle")
                : t("cell_prefix") + " " + currentCell.name + " [" + currentCell.type + "]");
        logLabel.setText(t("log_prefix") + " " + (state.lastActionLog == null ? "-" : state.lastActionLog));
        currentCellMetaLabel.setText(buildCellMeta(currentCell, state));
        TextureRegionDrawable cellDrawable = currentCell == null ? null
                : new TextureRegionDrawable(new TextureRegion(cellTextures[currentCell.id]));
        currentCellImage.setDrawable(cellDrawable);
        buyModalCellImage.setDrawable(cellDrawable);
        TextureRegionDrawable auctionCellDrawable = state.auctionCellId >= 0
                && state.auctionCellId < cellTextures.length
                        ? new TextureRegionDrawable(new TextureRegion(cellTextures[state.auctionCellId]))
                        : null;
        auctionModalCellImage.setDrawable(auctionCellDrawable);
        diceHintLabel.setText(buildDiceHint(state, current, localPlayer));
        feedDescriptionLabel.setText(buildFeedDescription(state, current, currentCell, localPlayer));
        buyAuctionModalLabel.setText(buildCellMeta(currentCell, state));

        rebuildPlayersIfNeeded(state, current, localPlayerId);
        rebuildOwnedCellsIfNeeded(state, localPlayer, myTurn, state.currentPhase);
        boolean canSubmitBattleMeme = canSubmitBattleMeme(state, localPlayerId);
        rebuildHandMemesIfNeeded(localPlayer, canSubmitBattleMeme);
        refreshActions(state, myTurn, currentCell);
        refreshBattleOverlay(state);

        if (dealButton != null) {
            boolean canTrade = myTurn
                    && state.currentPhase == GameState.GamePhase.PLAYING
                    && !state.isInBattle
                    && !state.isInAuction
                    && state.tradeId == 0
                    && tradeMode == TRADE_MODE_CLOSED;
            setActorDisabled(dealButton, !canTrade);
        }

        if (state.tradeId != 0) {
            if (state.tradeTargetId == localPlayerId && tradeMode == TRADE_MODE_CLOSED) {
                tradeMode = TRADE_MODE_INCOMING;
                if (diceOverlay != null)
                    diceOverlay.setVisible(false);
                buildTradeWindow(state);
            } else if (tradeMode == TRADE_MODE_INCOMING && state.tradeTargetId != localPlayerId) {
                closeTradeWindow();
            } else if (tradeMode == TRADE_MODE_WAIT && state.tradeProposerId != localPlayerId) {
                closeTradeWindow();
            }
        } else if (tradeMode == TRADE_MODE_WAIT || tradeMode == TRADE_MODE_INCOMING) {
            closeTradeWindow();
        }
    }

    private void rebuildPlayersIfNeeded(GameState state, Player current, int localPlayerId) {
        String signature = buildPlayersSignature(state, current, localPlayerId);
        if (signature.equals(lastPlayersSignature)) {
            return;
        }
        lastPlayersSignature = signature;
        rebuildPlayers(state, current, localPlayerId);
    }

    private void rebuildOwnedCellsIfNeeded(GameState state, Player localPlayer, boolean myTurn,
            GameState.GamePhase currentPhase) {
        String signature = buildOwnedCellsSignature(state, localPlayer, myTurn, currentPhase);
        if (signature.equals(lastOwnedCellsSignature)) {
            return;
        }
        lastOwnedCellsSignature = signature;
        rebuildOwnedCells(state, localPlayer, myTurn, currentPhase);
    }

    private void rebuildHandMemesIfNeeded(Player localPlayer, boolean canSubmitBattleMeme) {
        String signature = buildHandMemesSignature(localPlayer, canSubmitBattleMeme);
        if (signature.equals(lastHandMemesSignature)) {
            return;
        }
        lastHandMemesSignature = signature;
        rebuildHandMemes(localPlayer, canSubmitBattleMeme);
    }

    // GameScreen.java — rebuildPlayers (замена кольца на текстуру рамки)

    private void rebuildPlayers(GameState state, Player current, int localPlayerId) {
        playersTable.clearChildren();
        roleInfoIcons.clear();
        if (state == null || state.players == null)
            return;
        boolean ru = language == Language.RU;
        for (Player player : state.players) {
            Role role = Role.of(state.playerRoles == null ? null : state.playerRoles.get(player.id));
            Color roleColor = role != null ? RoleInfo.color(role) : null;
            Table row = new Table();
            row.left();
            row.pad(8, 10, 8, 10);

            // Stack: НИЖНИЙ слой — рамка роли (или фолбэк-кольцо), ВЕРХНИЙ — аватар.
            // Порядок в Stack: аватар снизу (50x50 fit), рамка сверху (68x68 fit)
            Stack avatarStack = new Stack();

            Texture avatar = SteamAvatarCache.getAvatar(player.steamId);
            Image av;
            if (avatar != null) {
                av = new Image(avatar);
            } else {
                av = new Image(UiShapes.circle(25, roleColor != null ? roleColor : Color.LIGHT_GRAY));
            }
            av.setScaling(Scaling.fit);
            Table avTable = new Table();
            avTable.center();
            avTable.add(av).size(50f, 50f);
            avatarStack.add(avTable);

            Texture frameTexture = role != null ? avatarFrameByRole.get(role) : null;
            if (frameTexture != null) {
                Image frame = new Image(frameTexture);
                frame.setScaling(Scaling.fit);
                avatarStack.add(frame);
            } else {
                avatarStack.add(new Image(UiShapes.ring(34, 5, roleColor != null ? roleColor : Color.GRAY)));
            }
            row.add(avatarStack).size(68f).padRight(10);

            String displayName = player.name;
            if (displayName.length() > 12) {
                displayName = displayName.substring(0, 12) + "…";
            }
            VisLabel nameLabel = new VisLabel(displayName);
            nameLabel.setWrap(false);
            nameLabel.setFontScale(0.85f);
            nameLabel.setColor(BATTLE_TEXT_COLOR);

            Table mid = new Table();
            mid.top().left();
            mid.add(nameLabel).left().padBottom(4f).row();

            if (role != null) {
                Table roleRow = new Table();
                roleRow.left();
                VisLabel roleLabel = new VisLabel(RoleInfo.name(role, ru));
                roleLabel.setColor(roleColor);
                roleLabel.setFontScale(0.7f);
                roleRow.add(roleLabel).left();
                Actor infoIcon = createRoleInfoIcon(role);
                roleInfoIcons.put(infoIcon, role);
                roleRow.add(infoIcon).size(22f).padLeft(6f);
                mid.add(roleRow).left().row();
            } else {
                mid.add().height(22f).row();
            }

            if (player.isBankrupt) {
                VisLabel bankruptLabel = new VisLabel(t("bankrupt"));
                bankruptLabel.setColor(new Color(0.85f, 0.15f, 0.15f, 1f));
                bankruptLabel.setFontScale(0.6f);
                mid.add(bankruptLabel).left().row();
            }
            row.add(mid).width(150f).left().top().padRight(8);

            Table rightCol = new Table();
            rightCol.top().right();

            Table moneyCell = new Table();
            moneyCell.right();
            Image coinIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
            coinIcon.setScaling(Scaling.fit);
            VisLabel moneyLabel = new VisLabel(String.valueOf(player.money));
            moneyLabel.setColor(ACCENT_GOLD);
            moneyLabel.setFontScale(0.8f);
            moneyCell.add(coinIcon).size(20f, 20f).padRight(4);
            moneyCell.add(moneyLabel);
            rightCol.add(moneyCell).right().padBottom(4f).row();

            Table shieldBox = new Table();
            shieldBox.right();
            if (player.shields > 0 && shieldTexture != null) {
                Image shieldIcon = new Image(shieldTexture);
                shieldIcon.setScaling(Scaling.fit);
                VisLabel shieldCount = new VisLabel("x" + player.shields);
                shieldCount.setColor(BATTLE_TEXT_COLOR);
                shieldCount.setFontScale(0.8f);
                shieldBox.add(shieldIcon).size(18f, 18f).padRight(4);
                shieldBox.add(shieldCount);
            } else {
                shieldBox.add().height(18f);
            }
            rightCol.add(shieldBox).right().row();

            row.add(rightCol).width(100f).right().top();
            playersTable.add(row).width(340).left().padBottom(8).row();
        }
    }

    private Actor createRoleInfoIcon(Role role) {
        Stack stack = new Stack();
        Image background = new Image(UiShapes.circle(11, new Color(0f, 0.04f, 0.24f, 0.9f)));
        stack.add(background);

        Table center = new Table();
        VisLabel i = new VisLabel("i");
        i.setColor(Color.WHITE);
        i.setFontScale(0.7f);
        center.add(i);
        stack.add(center);

        stack.setSize(22f, 22f);
        return stack;
    }

    private void showRoleTooltip(Role role, Actor anchor) {
        roleTooltipLabel.setText(RoleInfo.description(role, language == Language.RU));
        roleTooltip.pack();
        Vector2 pos = anchor.localToStageCoordinates(new Vector2(0, 0));
        float x = pos.x + 30f;
        float y = pos.y - roleTooltip.getHeight() / 2f;
        x = Math.min(x, stage.getWidth() - roleTooltip.getWidth() - 8f);
        y = Math.max(8f, Math.min(y, stage.getHeight() - roleTooltip.getHeight() - 8f));
        roleTooltip.setPosition(x, y);
        roleTooltip.setVisible(true);
        roleTooltip.toFront();
    }

    private void hideRoleTooltip() {
        if (roleTooltip != null)
            roleTooltip.setVisible(false);
    }

    private void rebuildOwnedCells(GameState state, Player localPlayer, boolean myTurn,
            GameState.GamePhase currentPhase) {
        ownedCellsTable.clearChildren();
        if (localPlayer == null || localPlayer.ownedCells.isEmpty()) {
            VisLabel emptyLabel = new VisLabel(t("empty_owned_cells"));
            emptyLabel.setWrap(true);
            emptyLabel.setColor(BATTLE_TEXT_COLOR);
            emptyLabel.setFontScale(0.8f);
            ownedCellsTable.add(emptyLabel).width(300).center().padTop(10).row();
            return;
        }
        boolean canManageCell = myTurn
                && currentPhase != GameState.GamePhase.AUCTION
                && currentPhase != GameState.GamePhase.MEME_BATTLE;
        boolean hasAnyMonopoly = MonopolyUtils.countFullGroups(localPlayer, boardCells) > 0;
        int count = 0;
        for (int cellId : localPlayer.ownedCells) {
            BoardCell cell = boardCells.get(cellId);
            boolean mortgaged = state.cellMortgaged.getOrDefault(cellId, false);
            int houses = state.cellHouses.getOrDefault(cellId, 0);
            boolean inMonopoly = hasAnyMonopoly && isCellInMonopoly(cell, localPlayer);

            Table cellCard = new Table();
            Image cellImage = new Image(new TextureRegionDrawable(new TextureRegion(cellTextures[cellId])));
            cellImage.setScaling(Scaling.fit);
            cellCard.add(cellImage).size(160f, 200f).row();

            if (inMonopoly && !mortgaged) {
                // Клетка в монополии и не в залоге: BUILD сверху, SELL снизу, без оверлея цены
                Button buildHouseButton = createTextureOrPillButton(buildButtonTexture, t("build_branch"));
                buildHouseButton.setDisabled(!canManageCell || houses >= 4);
                buildHouseButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        sendAction(GameActionRequest.ActionType.BUY_HOUSE, cellId, 0);
                    }
                });
                Button sellHouseButton = createTextureOrPillButton(sellButtonTexture, t("sell_branch"));
                sellHouseButton.setDisabled(!canManageCell || houses == 0);
                sellHouseButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        sendAction(GameActionRequest.ActionType.SELL_HOUSE, cellId, 0);
                    }
                });
                cellCard.add(buildHouseButton).width(150f).height(40f).padTop(4f).row();
                cellCard.add(sellHouseButton).width(150f).height(40f).padTop(4f).row();
            } else {
                // Вне монополии (любой залог) ИЛИ в монополии, но в залоге:
                // одна кнопка MORTGAGE/UNMORTGAGE с оверлеем цены
                ImageButton actionButton = createActionButton(mortgaged ? buyBackButtonTexture : mortgageButtonTexture);
                actionButton.setDisabled(!canManageCell);
                actionButton.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        sendAction(
                                mortgaged ? GameActionRequest.ActionType.BUY_BACK_CELL
                                        : GameActionRequest.ActionType.MORTGAGE_CELL,
                                cellId,
                                0);
                    }
                });

                VisLabel priceLabel = new VisLabel(String.valueOf(cell.price / 2));
                priceLabel.setFontScale(0.45f);
                priceLabel.setColor(canManageCell ? Color.valueOf("000A3E") : new Color(0.35f, 0.38f, 0.45f, 1f));

                Table priceOverlay = new Table();
                priceOverlay.center().bottom();
                priceOverlay.add(priceLabel).padBottom(4f);

                Stack buttonStack = new Stack();
                buttonStack.add(actionButton);
                buttonStack.add(priceOverlay);
                cellCard.add(buttonStack).width(150f).height(48f).padTop(6f).row();
            }

            ownedCellsTable.add(cellCard).pad(6f);
            if (++count % 2 == 0) {
                ownedCellsTable.row();
            }
        }
    }

    /**
     * true, если клетка входит в цветовую группу и игрок владеет ВСЕМИ клетками
     * этой группы.
     */
    private boolean isCellInMonopoly(BoardCell cell, Player player) {
        if (cell == null || cell.group == null || player == null) {
            return false;
        }
        for (BoardCell other : boardCells) {
            if (other.group == null || !other.group.equals(cell.group)) {
                continue;
            }
            if (!player.ownedCells.contains(other.id)) {
                return false;
            }
        }
        return true;
    }

    private void rebuildHandMemes(Player localPlayer, boolean canSubmitBattleMeme) {
        handMemesTable.clearChildren();
        if (localPlayer == null || localPlayer.handMemes == null || localPlayer.handMemes.isEmpty()) {
            VisLabel emptyLabel = new VisLabel(t("empty_hand_memes"));
            emptyLabel.setWrap(true);
            emptyLabel.setColor(BATTLE_TEXT_COLOR);
            emptyLabel.setFontScale(0.8f);
            handMemesTable.add(emptyLabel).width(300).left().row();
            return;
        }
        for (Meme meme : localPlayer.handMemes) {
            handMemesTable
                    .add(createMemeCard(meme, false, !canSubmitBattleMeme,
                            () -> sendAction(GameActionRequest.ActionType.SUBMIT_MEME, meme.id, 0)))
                    .width(300f).height(225f).pad(8f).row(); // 4:3, одна в ряд
        }
    }

    private boolean canSubmitBattleMeme(GameState state, int localPlayerId) {
        if (state == null || state.battleParticipants == null || state.battleMemes == null) {
            return false;
        }
        if (state.currentPhase != GameState.GamePhase.MEME_BATTLE
                || state.battlePhase != GameState.BattlePhase.COLLECTING_MEMES
                || !state.battleParticipants.contains(localPlayerId)) {
            return false;
        }
        for (Meme meme : state.battleMemes) {
            if (meme.ownerId == localPlayerId) {
                return false;
            }
        }
        return true;
    }

    /**
     * Creates a clickable meme card. The card itself acts as the button - no
     * separate select/vote button.
     * 
     * @param meme       the meme to display
     * @param isSelected whether this card is already selected (shows a highlight
     *                   border)
     * @param disabled   if true the card is not clickable
     * @param action     the action to run when the card is clicked
     */
    private Table createMemeCard(Meme meme, boolean isSelected, boolean disabled, Runnable action) {
        Table card = new Table();
        card.pad(10f);

        Drawable memeDrawable = getMemeDrawable(meme);
        if (memeDrawable != null) {
            Image image = new Image(memeDrawable);
            image.setScaling(Scaling.fill);
            card.add(image).expand().fill();
        } else {
            VisLabel placeholder = new VisLabel(t("no_preview"));
            placeholder.setColor(BATTLE_TEXT_COLOR);
            card.add(placeholder).expand();
        }

        card.setTouchable(disabled ? Touchable.disabled : Touchable.enabled);
        if (!disabled && action != null) {
            card.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
                @Override
                public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                    action.run();
                }
            });
        }
        return card;
    }

    private Drawable getMemeDrawable(Meme meme) {
        Texture texture = getMemeTexture(meme == null ? null : meme.imageUrl);
        return texture == null ? null : new TextureRegionDrawable(new TextureRegion(texture));
    }

    private Texture getMemeTexture(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        if (memeTextureCache.containsKey(imagePath)) {
            return memeTextureCache.get(imagePath);
        }

        FileHandle file = Gdx.files.local(imagePath);
        if (!file.exists()) {
            file = Gdx.files.absolute(imagePath);
        }
        if (!file.exists()) {
            file = Gdx.files.internal(imagePath);
        }
        if (!file.exists()) {
            memeTextureCache.put(imagePath, null);
            return null;
        }

        Texture texture = new Texture(file);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        memeTextureCache.put(imagePath, texture);
        return texture;
    }

    private void refreshActions(GameState state, boolean myTurn, BoardCell currentCell) {
        boolean canRoll = myTurn && state.currentPhase == GameState.GamePhase.PLAYING && !state.hasRolledThisTurn
                && !state.awaitingReroll;
        boolean canBuyOrPass = myTurn && state.currentPhase == GameState.GamePhase.PLAYER_ACTION
                && currentCell != null && currentCell.type == BoardCell.Type.SITUATION;
        boolean canEndTurn = myTurn && state.currentPhase == GameState.GamePhase.PLAYING && state.hasRolledThisTurn;
        boolean showTurnControls = myTurn && state.currentPhase == GameState.GamePhase.PLAYING;
        boolean canBid = state.currentPhase == GameState.GamePhase.AUCTION
                && game.getClient().getLocalPlayerId() == state.auctionCurrentPlayerId;
        boolean canUseMemeBank = state.currentPhase == GameState.GamePhase.MEME_BANK_ACTION
                && game.getClient().getLocalPlayerId() == state.memeBankPlayerId;
        Player localPlayer = state.getPlayerById(game.getClient().getLocalPlayerId());
        boolean canDepositToMemeBank = canUseMemeBank && localPlayer != null && localPlayer.memeBankBalance <= 0;
        boolean canWithdrawFromMemeBank = canUseMemeBank && localPlayer != null && localPlayer.memeBankBalance > 0;

        setButtonsEnabled(canRoll, canBuyOrPass, canBuyOrPass, canEndTurn, canBid);
        diceButton.setVisible(showTurnControls);
        buyButton.setVisible(canBuyOrPass);
        passButton.setVisible(canBuyOrPass);
        endTurnButton.setVisible(canEndTurn);
        bidField.setVisible(canBid);
        placeBidButton.setVisible(canBid);
        cancelAuctionButton.setVisible(canBid);
        memeBankAmountField.setVisible(canDepositToMemeBank);
        memeBankDepositButton.setVisible(canDepositToMemeBank);
        memeBankWithdrawButton.setVisible(canWithdrawFromMemeBank);
        memeBankSkipButton.setVisible(canUseMemeBank);
        memeBankAmountField.setDisabled(!canDepositToMemeBank);
        memeBankAmountField.setFocusTraversal(false);
        setActorDisabled(memeBankDepositButton, !canDepositToMemeBank);
        setActorDisabled(memeBankWithdrawButton, !canWithdrawFromMemeBank);
        setActorDisabled(memeBankSkipButton, !canUseMemeBank);
        setModalVisible(buyOrAuctionModal, canBuyOrPass);
        setModalVisible(auctionModal, state.currentPhase == GameState.GamePhase.AUCTION);
        setModalVisible(memeBankModal, state.currentPhase == GameState.GamePhase.MEME_BANK_ACTION && canUseMemeBank);

        if (state.currentPhase == GameState.GamePhase.AUCTION) {
            String auctionText = t("auction_status")
                    + " " + state.currentAuctionTime + " " + t("seconds")
                    + " | " + t("turn_short") + " " + getAuctionTurnName(state)
                    + " | " + t("bids") + " " + state.auctionBids.size();
            auctionLabel.setText(auctionText);
            auctionModalLabel.setText(auctionText);
        } else if (state.currentPhase == GameState.GamePhase.MEME_BANK_ACTION && canUseMemeBank) {
            int bankBalance = localPlayer == null ? 0 : localPlayer.memeBankBalance;
            String memeBankText = t("meme_bank_title") + ": " + t("balance")
                    + " " + bankBalance + " | " + t("deposit_up_to_500");
            auctionLabel.setText(memeBankText);
            memeBankModalLabel.setText(memeBankText);
        } else {
            auctionLabel.setText("");
            auctionModalLabel.setText("");
            memeBankModalLabel.setText("");
        }
        int localId = game.getClient().getLocalPlayerId();
        boolean awaiting = state.awaitingReroll && state.rerollPlayerId == localId;
        boolean modPending = state.moderatorChoicePending && state.moderatorPlayerId == localId;
        if (modPending) {
            diceButton.setVisible(false);
            endTurnButton.setVisible(false);
            passButton.setVisible(false);
            buyButton.setVisible(false);
            stayButton.setVisible(false);
            plusTwoButton.setVisible(false);
            stealButton.setVisible(false);
            moderatorSkipButton.setVisible(true);
            moderatorTakeJailButton.setVisible(true);
            moderatorSkipButton.setDisabled(false);
            moderatorTakeJailButton.setDisabled(false);
            diceHintLabel.setText(t("moderator_choice"));
        } else {
            moderatorSkipButton.setVisible(false);
            moderatorTakeJailButton.setVisible(false);
            if (awaiting) {
                diceButton.setVisible(false);
                endTurnButton.setVisible(false);
                passButton.setVisible(false);
                buyButton.setVisible(false);
                stayButton.setVisible(true);
                stayButton.setDisabled(false);
                plusTwoButton.setVisible(true);
                plusTwoButton.setDisabled(false);
                diceHintLabel.setText(t("doge_choice"));
            } else {
                stayButton.setVisible(false);
                plusTwoButton.setVisible(false);
            }
        }
        Role localRole = Role.of(state.playerRoles == null ? null : state.playerRoles.get(localId));
        boolean canSteal = localRole == Role.SCAMMER && myTurn
                && state.currentPhase == GameState.GamePhase.PLAYING
                && !state.roleUsedThisRound.getOrDefault(localId, false) && !awaiting && !modPending;
        stealButton.setVisible(canSteal);
        stealButton.setDisabled(!canSteal);
    }

    private void setButtonsEnabled(boolean roll, boolean buy, boolean pass, boolean endTurn, boolean bid) {
        diceButton.setDisabled(!roll);
        buyButton.setDisabled(!buy);
        passButton.setDisabled(!pass);
        endTurnButton.setDisabled(!endTurn);
        placeBidButton.setDisabled(!bid);
        cancelAuctionButton.setVisible(bid);
        setActorDisabled(cancelAuctionButton, !bid);
        bidField.setDisabled(!bid);
    }

    private void setActorDisabled(Actor actor, boolean disabled) {
        if (actor instanceof ImageButton imageButton) {
            imageButton.setDisabled(disabled);
        } else if (actor instanceof VisTextButton textButton) {
            textButton.setDisabled(disabled);
        }
    }

    private void setModalVisible(Table modal, boolean visible) {
        modal.setVisible(visible);
        modal.setTouchable(visible ? Touchable.enabled : Touchable.disabled);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderBackground();
        GameState state = game.getLatestGameState();
        boardRenderer.update(delta);
        refreshUi(state);
        boardRenderer.setCameraLocked(isCameraBlocked());
        boardRenderer.updateCamera(delta, state);
        boardRenderer.render(boardCells, state);
        updateRoleTooltip();
        updateDevPanel(state);
        refreshNotification(state, delta);
        if (chatWidget != null) {
            chatWidget.refresh();
        }
        stage.act(delta);
        stage.draw();
    }

    private boolean isCameraBlocked() {
        return tradeMode != TRADE_MODE_CLOSED
                || tradeWindow != null
                || buyOrAuctionModal.isVisible()
                || auctionModal.isVisible()
                || memeBankModal.isVisible()
                || turnNotificationModal.isVisible();
    }

    private void renderBackground() {
        game.getBatch().setProjectionMatrix(stage.getCamera().combined);
        game.getBatch().begin();
        game.getBatch().draw(backgroundTexture, 0f, 0f, WORLD_WIDTH, WORLD_HEIGHT);
        game.getBatch().end();
    }

    private void refreshNotification(GameState state, float delta) {
        if (state == null) {
            setModalVisible(turnNotificationModal, false);
            notificationVisibleTime = 0f;
            return;
        }

        if (state.notificationTimestamp != 0L
                && state.notificationTimestamp != lastShownNotificationTimestamp
                && state.notificationText != null
                && !state.notificationText.isBlank()) {
            lastShownNotificationTimestamp = state.notificationTimestamp;
            turnModalLabel.setText(state.notificationText);
            setModalVisible(turnNotificationModal, true);
            turnNotificationModal.setTouchable(Touchable.disabled);
            notificationVisibleTime = 3f;
        }

        if (notificationVisibleTime > 0f) {
            notificationVisibleTime -= delta;
            if (notificationVisibleTime <= 0f) {
                setModalVisible(turnNotificationModal, false);
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        boardRenderer.resize(width, height);
        stage.getViewport().update(width, height, true);
        if (sideRoot != null) {
            sideRoot.invalidateHierarchy();
        }
        layoutBoardOverlays();
    }

    @Override
    public void dispose() {
        boardRenderer.dispose();
        diceButtonTexture.dispose();
        moneyTexture.dispose();
        buyButtonTexture.dispose();
        auctionButtonTexture.dispose();
        endTurnButtonTexture.dispose();
        placeBidButtonTexture.dispose();
        mortgageButtonTexture.dispose();
        buyBackButtonTexture.dispose();
        gameOverlayWindowTexture.dispose();
        playersWindowTexture.dispose();
        myCellsWindowTexture.dispose();
        memesWindowTexture.dispose();
        if (shieldTexture != null)
            shieldTexture.dispose();
        if (exitToMenuButtonTexture != null)
            exitToMenuButtonTexture.dispose();
        if (depositButtonTexture != null)
            depositButtonTexture.dispose();
        if (withdrawButtonTexture != null)
            withdrawButtonTexture.dispose();
        if (participateButtonTexture != null)
            participateButtonTexture.dispose();
        if (declineButtonTexture != null)
            declineButtonTexture.dispose();
        if (plusTwoButtonTexture != null)
            plusTwoButtonTexture.dispose();
        if (stayButtonTexture != null)
            stayButtonTexture.dispose();
        if (dealButtonTexture != null)
            dealButtonTexture.dispose();
        if (stealButtonTexture != null)
            stealButtonTexture.dispose();
        for (Texture frameTexture : avatarFrameByRole.values()) {
            if (frameTexture != null)
                frameTexture.dispose();
        }
        if (timerCircleTexture != null) {
            timerCircleTexture.dispose();
        }
        notificationWindowTexture.dispose();
        buyAndAuctionWindowTexture.dispose();
        auctionOrMemeBankWindowTexture.dispose();
        tenUpTexture.dispose();
        tenDownTexture.dispose();
        startBattleTexture.dispose();
        inputMemeBattleTexture.dispose();
        cardBoardTexture.dispose();
        inputTexture.dispose();
        backgroundTexture.dispose();
        for (Texture cellTexture : cellTextures)
            cellTexture.dispose();
        for (Texture memeTexture : memeTextureCache.values()) {
            if (memeTexture != null)
                memeTexture.dispose();
        }
        if (chatWidget != null)
            chatWidget.dispose();
        stage.dispose();
    }

    private String buildCellMeta(BoardCell currentCell, GameState state) {
        if (currentCell == null) {
            return t("no_active_cell");
        }

        Integer ownerId = state.cellOwners.get(currentCell.id);
        String ownerText = ownerId == null ? t("cell_free") : t("cell_owner") + " " + state.getPlayerById(ownerId).name;
        String headerText = currentCell.name;
        String priceText = currentCell.type == BoardCell.Type.SITUATION
                ? t("cell_price") + " " + currentCell.price + " " + t("coins")
                : t("cell_type") + " " + currentCell.type;
        String mortgageText = state.cellMortgaged.getOrDefault(currentCell.id, false) ? " | " + t("cell_mortgaged")
                : "";
        if (currentCell.type == BoardCell.Type.MEME_BANK) {
            Player localPlayer = state.getPlayerById(game.getClient().getLocalPlayerId());
            int bankBalance = localPlayer == null ? 0 : localPlayer.memeBankBalance;
            return headerText + "\n" + priceText + "\n" + t("your_balance") + " " + bankBalance;
        }
        return headerText + "\n" + priceText + "\n" + ownerText + mortgageText;
    }

    private String buildFeedDescription(GameState state, Player current, BoardCell currentCell, Player localPlayer) {
        if (current == null) {
            return t("feed_waiting_player");
        }

        StringBuilder builder = new StringBuilder();
        builder.append(t("phase_prefix") + " ").append(state.currentPhase);
        if (localPlayer != null) {
            builder.append("\n").append(t("your_money") + " ").append(localPlayer.money).append(" " + t("coins"));
        }
        if (currentCell != null) {
            builder.append("\n").append(t("current_cell") + " ").append(currentCell.name);
        }
        if (state.lastActionLog != null && !state.lastActionLog.isBlank()) {
            builder.append("\n").append(state.lastActionLog);
        }
        return builder.toString();
    }

    private String buildDiceHint(GameState state, Player current, Player localPlayer) {
        if (current == null) {
            return t("dice_hint_waiting_first");
        }
        if (localPlayer != null && current.id == localPlayer.id) {
            return t("your_turn");
        }
        return "";
    }

    private Drawable panel(Color color) {
        return VisUI.getSkin().newDrawable("white", color);
    }

    private ImageButton createDiceButton() {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(diceButtonTexture));
        style.imageUp = drawable;
        style.imageOver = drawable.tint(new Color(0.82f, 0.82f, 0.82f, 1f));
        style.imageDown = drawable.tint(new Color(0.88f, 0.88f, 0.88f, 1f));
        style.imageDisabled = drawable.tint(new Color(0.45f, 0.45f, 0.45f, 1f));
        Drawable transparent = panel(new Color(1f, 1f, 1f, 0f));
        style.up = transparent;
        style.over = transparent;
        style.down = transparent;
        style.disabled = transparent;

        ImageButton button = new ImageButton(style);
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                GameState st = game.getLatestGameState();
                if (st != null && st.awaitingReroll && st.rerollPlayerId == game.getClient().getLocalPlayerId()) {
                    sendAction(GameActionRequest.ActionType.PLUS_TWO, 0, 0);
                } else {
                    RollDiceRequest request = new RollDiceRequest();
                    request.playerId = game.getClient().getLocalPlayerId();
                    game.getClient().sendRollDice(request);
                }
            }
        });
        return button;
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private Texture loadTextureWithFallback(String primaryPath, String fallbackPath) {
        return Gdx.files.internal(primaryPath).exists() ? loadTexture(primaryPath) : loadTexture(fallbackPath);
    }

    private Texture loadTextureIfExists(String path) {
        return Gdx.files.internal(path).exists() ? loadTexture(path) : null;
    }

    private ImageButton createActionButton(Texture texture) {
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        style.imageUp = drawable;
        style.imageOver = drawable.tint(new Color(0.82f, 0.82f, 0.82f, 1f));
        style.imageDown = drawable.tint(new Color(0.70f, 0.70f, 0.70f, 1f));
        style.imageDisabled = drawable.tint(new Color(0.45f, 0.45f, 0.45f, 1f));
        Drawable transparent = panel(new Color(1f, 1f, 1f, 0f));
        style.up = transparent;
        style.over = transparent;
        style.down = transparent;
        style.disabled = transparent;
        return new ImageButton(style);
    }

    private Table createMoneyValue(int amount) {
        Table table = new Table();
        Image icon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        icon.setScaling(Scaling.fit);
        VisLabel valueLabel = new VisLabel(String.valueOf(amount));
        valueLabel.setColor(ACCENT_GOLD);
        table.add(icon).size(18, 18).padRight(6);
        table.add(valueLabel);
        return table;
    }

    private Texture[] loadCellTextures() {
        Texture[] textures = new Texture[40];
        for (int i = 0; i < textures.length; i++) {
            textures[i] = new Texture(Gdx.files.internal(i + ".png"), true);
            textures[i].setFilter(Texture.TextureFilter.MipMapLinearLinear, Texture.TextureFilter.Linear);
        }
        return textures;
    }

    private void layoutBoardOverlays() {
        com.badlogic.gdx.math.Rectangle boardBounds = boardRenderer.getBoardBounds();

        // Кнопки хода и подсказка — строго по центру экрана
        float diceW = 700f;
        float diceH = 560f;
        diceOverlay.setBounds((WORLD_WIDTH - diceW) / 2f, (WORLD_HEIGHT - diceH) / 2f, diceW, diceH);

        // CURRENT CELL и FEED временно отключены
        // com.badlogic.gdx.math.Rectangle currentBounds =
        // boardRenderer.getCurrentCellPanelBounds();
        // com.badlogic.gdx.math.Rectangle feedBounds =
        // boardRenderer.getFeedPanelBounds();
        // currentCellOverlay.setBounds(currentBounds.x, currentBounds.y,
        // currentBounds.width, currentBounds.height);
        // feedOverlay.setBounds(feedBounds.x, feedBounds.y, feedBounds.width,
        // feedBounds.height);

        // Модалки: фиксированный размер и центровка вместо fillParent
        turnNotificationModal.setBounds((WORLD_WIDTH - 640f) / 2f, (WORLD_HEIGHT - 280f) / 2f, 640f, 280f);
        buyOrAuctionModal.setBounds((WORLD_WIDTH - 1000f) / 2f, (WORLD_HEIGHT - 640f) / 2f, 1000f, 640f);
        auctionModal.setBounds((WORLD_WIDTH - 1000f) / 2f, (WORLD_HEIGHT - 440f) / 2f, 1000f, 440f);
        memeBankModal.setBounds((WORLD_WIDTH - 900f) / 2f, (WORLD_HEIGHT - 500f) / 2f, 900f, 500f);
    }

    private void configureModal(Table modal, Texture texture, VisLabel contentLabel, float minWidth, float minHeight,
            boolean centerText) {
        modal.setVisible(false);
        modal.center();
        Table window = new Table();
        window.setBackground(new TextureRegionDrawable(new TextureRegion(texture)));
        window.pad(24f);
        modal.clearChildren();
        contentLabel.setWrap(true);
        contentLabel.setColor(BATTLE_TEXT_COLOR);
        if (centerText) {
            contentLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
            window.add(contentLabel).width(minWidth * 0.85f).expand().center().pad(12f).row();
        } else {
            contentLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
            window.add(contentLabel).width(minWidth * 0.85f).center().pad(12f).row();
        }
        modal.add(window).minSize(minWidth, minHeight).fill().center();
    }

    private void setupModalControls() {
        addBuyAuctionControls();
        addAuctionControls();
        addMemeBankControls();
    }

    private void addBuyAuctionControls() {
        Table window = (Table) buyOrAuctionModal.getCells().first().getActor();
        window.clearChildren();
        window.pad(30f);
        buyAuctionModalLabel.setFontScale(1.25f);
        buyAuctionModalLabel.setColor(BATTLE_TEXT_COLOR);
        Table details = new Table();
        details.add(buyModalCellImage).size(240f, 240f).left();
        details.add(buyAuctionModalLabel).width(480f).left().padLeft(36f);
        window.add(details).center().padBottom(24f).row();
        Table controls = new Table();
        controls.add(buyButton).size(200f, 70f).padRight(20f);
        controls.add(passButton).size(200f, 70f).padLeft(20f);
        window.add(controls).center();
    }

    private void applyGameScreenLineSpacing() {
        Set<com.badlogic.gdx.graphics.g2d.BitmapFont> fonts = new HashSet<>();
        fonts.add(titleLabel.getStyle().font);
        fonts.add(phaseLabel.getStyle().font);
        fonts.add(turnLabel.getStyle().font);
        fonts.add(cellLabel.getStyle().font);
        fonts.add(logLabel.getStyle().font);
        fonts.add(diceTitleLabel.getStyle().font);
        fonts.add(diceHintLabel.getStyle().font);
        fonts.add(currentCellMetaLabel.getStyle().font);
        fonts.add(feedDescriptionLabel.getStyle().font);
        fonts.add(buyAuctionModalLabel.getStyle().font);
        for (com.badlogic.gdx.graphics.g2d.BitmapFont font : fonts) {
            if (font != null) {
                font.getData().setLineHeight(font.getCapHeight() * 1.55f);
            }
        }
    }

    private void addAuctionControls() {
        Table window = (Table) auctionModal.getCells().first().getActor();
        window.clearChildren();
        auctionModalLabel.setFontScale(1.15f);
        auctionModalLabel.setColor(BATTLE_TEXT_COLOR);
        Table details = new Table();
        details.add(auctionModalCellImage).size(160f, 160f).left();
        details.add(auctionModalLabel).width(560f).left().padLeft(24f);
        window.add(details).center().padBottom(20f).row();
        Table controls = new Table();
        controls.add(bidField).size(240f, 54f).padRight(12f);
        controls.add(placeBidButton).size(200f, 70f).padRight(12f);
        controls.add(cancelAuctionButton).size(200f, 70f).row();
        controls.add(auctionErrorLabel).colspan(3).center().padTop(6f);
        window.add(controls).center();
    }

    private void addMemeBankControls() {
        Table window = (Table) memeBankModal.getCells().first().getActor();
        window.pad(30f);
        Table controls = new Table();
        controls.center();
        controls.add(memeBankAmountField).size(260f, 54f).row();
        controls.add(memeBankErrorLabel).center().padTop(12f).row();
        Table row = new Table();
        row.add(memeBankDepositButton).size(200f, 70f).padRight(12f);
        row.add(memeBankWithdrawButton).size(200f, 70f);
        controls.add(row).center().padTop(12f).row();
        controls.add(memeBankSkipButton).size(200f, 70f).center().padTop(12f);
        window.add(controls).center().expand();
    }

    private Actor createOptionalActionButton(Texture texture, String fallbackText) {
        return texture != null ? createActionButton(texture) : createPillButton(fallbackText);
    }

    private VisTextButton createPillButton(String text) {
        VisTextButton.VisTextButtonStyle style = new VisTextButton.VisTextButtonStyle(
                VisUI.getSkin().get("default", VisTextButton.VisTextButtonStyle.class));
        style.font = VisUI.getSkin().get("default", Label.LabelStyle.class).font;
        style.fontColor = BATTLE_TEXT_COLOR;
        TextureRegionDrawable pill = new TextureRegionDrawable(new TextureRegion(inputTexture));
        style.up = pill;
        style.over = pill.tint(new Color(0.92f, 0.92f, 0.92f, 1f));
        style.down = pill.tint(new Color(0.85f, 0.85f, 0.85f, 1f));
        style.focusBorder = null;
        return new VisTextButton(text, style);
    }

    private Actor createExitToMenuButton() {
        Actor button = exitToMenuButtonTexture != null
                ? createActionButton(exitToMenuButtonTexture)
                : new VisTextButton(t("exit_to_menu"));
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.leaveRoomToMenu();
            }
        });
        return button;
    }

    private void applyInputFieldStyle(VisTextField field) {
        VisTextField.VisTextFieldStyle style = new VisTextField.VisTextFieldStyle(field.getStyle());
        style.background = new TextureRegionDrawable(new TextureRegion(inputTexture));
        style.backgroundOver = style.background;
        style.focusedBackground = style.background;
        style.disabledBackground = style.background;
        field.setStyle(style);
    }

    private void applyBattleInputFieldStyle(VisTextField field) {
        VisTextField.VisTextFieldStyle style = new VisTextField.VisTextFieldStyle(field.getStyle());
        Drawable bg = new TextureRegionDrawable(new TextureRegion(inputMemeBattleTexture));
        style.background = bg;
        style.backgroundOver = bg;
        style.focusedBackground = bg;
        style.disabledBackground = bg;
        field.setStyle(style);
    }

    private String buildPlayersSignature(GameState state, Player current, int localPlayerId) {
        if (state == null || state.players == null) {
            return "none";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(localPlayerId).append('|');
        builder.append(current == null ? -1 : current.id).append('|');
        for (Player player : state.players) {
            builder.append(player.id).append(':')
                    .append(player.money).append(':')
                    .append(player.isBankrupt).append(':')
                    .append(player.name).append(':')
                    .append(player.steamId).append(':')
                    .append(player.shields).append(':')
                    .append(state.playerRoles == null ? "" : state.playerRoles.get(player.id))
                    .append(';');
        }
        return builder.toString();
    }

    private void showScammerTargetWindow() {
        GameState state = game.getLatestGameState();
        int localPlayerId = game.getClient().getLocalPlayerId();
        Player localPlayer = state == null ? null : state.getPlayerById(localPlayerId);
        if (localPlayer == null || state.players == null) {
            return;
        }

        List<Player> targets = new ArrayList<>();
        for (Player player : state.players) {
            if (player.id != localPlayerId && !player.isBankrupt && player.money > localPlayer.money) {
                targets.add(player);
            }
        }
        if (targets.isEmpty()) {
            diceHintLabel.setText(t("no_steal_target"));
            return;
        }

        Dialog dialog = new Dialog("", VisUI.getSkin());
        dialog.setBackground(window(gameOverlayWindowTexture));
        dialog.getContentTable().pad(20f);

        VisLabel title = new VisLabel(t("steal_target_title"));
        title.setColor(BATTLE_TEXT_COLOR);
        title.setFontScale(1.1f);
        dialog.getContentTable().add(title).padBottom(15f).row();

        for (Player target : targets) {
            VisTextButton btn = new VisTextButton(
                    target.name + " | " + target.money + " | " + t("shields") + ": " + target.shields);
            btn.getLabel().setColor(BATTLE_TEXT_COLOR);
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendAction(GameActionRequest.ActionType.STEAL_COINS, target.id, 0);
                    dialog.hide();
                }
            });
            dialog.getContentTable().add(btn).width(360f).height(44f).padBottom(8f).row();
        }

        VisTextButton cancelBtn = new VisTextButton(t("cancel"));
        cancelBtn.getLabel().setColor(BATTLE_TEXT_COLOR);
        cancelBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(cancelBtn).width(160f).height(38f).padTop(10f);
        dialog.show(stage);
        dialog.pack();
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) * 0.5f,
                (stage.getHeight() - dialog.getHeight()) * 0.5f);
    }

    private String getAuctionTurnName(GameState state) {
        Player player = state.getPlayerById(state.auctionCurrentPlayerId);
        return player == null ? "—" : player.name;
    }

    private String t(String key) {
        boolean ru = language == Language.RU;
        return switch (key) {
            case "waiting_answers" -> ru ? "Ждём ответов игроков..." : "Waiting for players' answers...";
            case "choose_stakes" -> ru ? "Выбери ставку" : "Choose the stakes";
            case "invite_battle" ->
                ru ? "Тебя приглашают на мем-баттл! Ставка:" : "You are invited to a meme battle! Stakes:";
            case "organizer_chooses" -> ru ? "Организатор выбирает ставку..." : "Organizer is choosing the stakes...";
            case "topic_placeholder" -> ru ? "Тема или ситуация..." : "Write the situation or topic";
            case "waiting_topic" -> ru ? "Ждём тему от организатора..." : "Waiting for the topic...";
            case "choose_funniest" ->
                ru ? "Выбери самый смешной мем для ситуации" : "CHOOSE THE FUNNIEST CARD FOR THE SITUATION";
            case "vote_hint" -> ru ? "Голосуй за лучший мем!" : "Vote for the best meme!";
            case "already_voted" ->
                ru ? "Ты уже проголосовал. Ждём остальных..." : "You already voted. Waiting for others...";
            case "battle_finished" -> ru ? "Баттл завершён!" : "Battle finished!";
            case "branches" -> ru ? "Филиалы" : "Branches";
            case "build_branch" -> ru ? "Построить" : "Build";
            case "sell_branch" -> ru ? "Продать" : "Sell";
            case "window_trade_title" -> ru ? "Сделка" : "Trade";
            case "btn_deal" -> ru ? "Сделка" : "Deal";
            case "btn_propose" -> ru ? "Предложить" : "Propose";
            case "btn_cancel" -> ru ? "Отмена" : "Cancel";
            case "btn_accept" -> ru ? "Принять" : "Accept";
            case "btn_decline" -> ru ? "Отказаться" : "Decline";
            case "btn_deposit" -> ru ? "Вложить" : "Deposit";
            case "btn_withdraw" -> ru ? "Снять" : "Withdraw";
            case "btn_skip" -> ru ? "Пропустить" : "Skip";
            case "trade_waiting" -> ru ? "Ждём ответа..." : "Waiting for response...";
            case "select_opponent" -> ru ? "Выбери соперника" : "Select opponent";
            case "trade_you" -> ru ? "ты" : "you";
            case "trade_pick_own" -> ru ? "Выбери свои клетки на доске" : "Pick your cells on the board";
            case "trade_pick_their" -> ru ? "Выбери клетки соперника на доске" : "Pick opponent's cells on the board";
            case "money_offered" -> ru ? "Предложено монет" : "Money offered";
            case "money_requested" -> ru ? "Запрошено монет" : "Money requested";
            case "reroll_hint" -> ru ? "Перебросить кубики или остаться?" : "Reroll the dice or stay?";
            case "steal" -> ru ? "Украсть 50" : "Steal 50";
            case "stay" -> ru ? "Остаться" : "Stay";
            case "plus_two" -> ru ? "+2 клетки" : "+2 cells";
            case "doge_choice" -> ru ? "Остаться или пойти на 2 клетки?" : "Stay or move 2 cells?";
            case "skip_jail" -> ru ? "Пропустить Ban" : "Skip Ban";
            case "take_jail" -> ru ? "В Ban + Щит" : "Ban + Shield";
            case "moderator_choice" -> ru ? "Модератор: пропустить Ban или сесть + получить щит?"
                    : "Moderator: Skip Ban or enter Ban + gain shield?";
            case "steal_target_title" -> ru ? "Выбери цель кражи" : "Choose steal target";
            case "shields" -> ru ? "щиты" : "shields";
            case "no_steal_target" -> ru ? "Нет подходящей цели" : "No suitable target";
            case "cancel" -> ru ? "Отмена" : "Cancel";
            case "bankrupt" -> ru ? "банкрот" : "bankrupt";
            case "title" -> ru ? "Мемополия" : "Memopoly";
            case "panel_players" -> ru ? "ИГРОКИ" : "PLAYERS";
            case "panel_my_cells" -> ru ? "МОИ КЛЕТКИ" : "MY CELLS";
            case "panel_memes" -> ru ? "МЕМЫ" : "MEMES";
            case "phase_idle" -> ru ? "Фаза: -" : "Phase: -";
            case "turn_idle" -> ru ? "Ход: -" : "Turn: -";
            case "cell_idle" -> ru ? "Клетка: -" : "Cell: -";
            case "log_idle" -> ru ? "События появятся здесь" : "Events will appear here";
            case "dice_hint_idle" ->
                ru ? "Кнопка броска появится, когда ход твой." : "Roll button will appear when it's your turn.";
            case "feed_idle" -> ru ? "Последние действия игроков будут собираться здесь."
                    : "Latest player actions will be collected here.";
            case "phase_waiting_data" -> ru ? "Фаза: ожидание данных" : "Phase: waiting for data";
            case "log_connecting" -> ru ? "Подключение к состоянию игры..." : "Connecting to game state...";
            case "cell_meta_connecting" -> ru ? "Информация о клетке появится, когда состояние игры загрузится."
                    : "Cell info will appear once the game state is loaded.";
            case "dice_hint_waiting" ->
                ru ? "Кнопка броска появится, когда ход будет твоим." : "Roll button will appear when it's your turn.";
            case "feed_connecting" -> ru ? "Подключаемся к игровой комнате и ждём актуальное состояние матча."
                    : "Joining the game room and waiting for the latest match state.";
            case "turn_prefix" -> ru ? "Сейчас ходит:" : "Now playing:";
            case "phase_prefix" -> ru ? "Фаза:" : "Phase:";
            case "cell_prefix" -> ru ? "Клетка:" : "Cell:";
            case "log_prefix" -> ru ? "Лог:" : "Log:";
            case "turn_number" -> ru ? "ход №" : "turn #";
            case "turn_short" -> ru ? "ход:" : "turn:";
            case "no_active_cell" -> ru ? "Нет активной клетки." : "No active cell.";
            case "cell_free" -> ru ? "Свободна" : "Free";
            case "cell_owner" -> ru ? "Владелец:" : "Owner:";
            case "cell_price" -> ru ? "Цена:" : "Price:";
            case "cell_type" -> ru ? "Тип:" : "Type:";
            case "cell_mortgaged" -> ru ? "заложена" : "mortgaged";
            case "your_balance" -> ru ? "Твой баланс:" : "Your balance:";
            case "your_money" -> ru ? "Твои деньги:" : "Your money:";
            case "current_cell" -> ru ? "Текущая клетка:" : "Current cell:";
            case "feed_waiting_player" -> ru ? "Ожидаем игрока, которому принадлежит следующий ход."
                    : "Waiting for the player whose turn is next.";
            case "dice_hint_waiting_first" ->
                ru ? "Ждём первого активного игрока." : "Waiting for the first active player.";
            case "your_turn" -> ru ? "Твой ход" : "Your turn";
            case "coins" -> ru ? "монет" : "coins";
            case "seconds" -> ru ? "сек." : "sec.";
            case "bids" -> ru ? "ставок:" : "bids:";
            case "auction_status" -> ru ? "Аукцион: осталось" : "Auction: left";
            case "meme_bank_title" -> ru ? "Meme Bank" : "Meme Bank";
            case "balance" -> ru ? "на счету" : "balance";
            case "deposit_up_to_500" -> ru ? "можно вложить до 500" : "deposit up to 500";
            case "bid_placeholder" -> ru ? "Ставка" : "Bid";
            case "bid_must_be_positive" -> ru ? "Ставка должна быть > 0" : "Bid must be > 0";
            case "amount_placeholder" -> ru ? "Сумма" : "Amount";
            case "amount_must_be_positive" -> ru ? "Введи сумму > 0" : "Enter amount > 0";
            case "max_500_coins" -> ru ? "Максимум 500 монет!" : "Maximum 500 coins!";
            case "not_enough_coins" -> ru ? "Недостаточно монет!" : "Not enough coins!";
            case "empty_owned_cells" -> ru ? "Пока нет купленных клеток" : "No owned cells yet";
            case "empty_hand_memes" -> ru ? "Колода ещё не выдала мемы" : "Deck hasn't dealt memes yet";
            case "no_preview" -> ru ? "Нет превью" : "No preview";
            case "exit_to_menu" -> ru ? "Выйти в меню" : "Exit to menu";
            default -> key;
        };
    }

    private String buildOwnedCellsSignature(GameState state, Player localPlayer, boolean myTurn,
            GameState.GamePhase currentPhase) {
        if (state == null || localPlayer == null) {
            return "none";
        }

        StringBuilder builder = new StringBuilder();
        builder.append(localPlayer.id).append('|')
                .append(myTurn).append('|')
                .append(currentPhase).append('|')
                .append(localPlayer.money).append('|');

        for (int cellId : localPlayer.ownedCells) {
            builder.append(cellId).append(':')
                    .append(state.cellMortgaged.getOrDefault(cellId, false))
                    .append(':').append(state.cellHouses.getOrDefault(cellId, 0))
                    .append(';');
        }
        return builder.toString();
    }

    private String buildHandMemesSignature(Player localPlayer, boolean canSubmitBattleMeme) {
        if (localPlayer == null || localPlayer.handMemes == null) {
            return "none|" + canSubmitBattleMeme;
        }

        StringBuilder builder = new StringBuilder();
        builder.append(canSubmitBattleMeme).append('|');
        for (Meme meme : localPlayer.handMemes) {
            builder.append(meme.id).append(':')
                    .append(meme.imageUrl).append(':')
                    .append(meme.description)
                    .append(';');
        }
        return builder.toString();
    }
}

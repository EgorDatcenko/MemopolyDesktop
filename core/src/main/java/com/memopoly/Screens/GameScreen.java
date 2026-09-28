package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.*;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.memopoly.Memopoly;
import com.memopoly.network.packets.*;
import com.memopoly.steam.SteamAvatarCache;
import com.memopoly.utils.*;
import com.memopoly.utils.LanguageManager.Language;
import com.memopoly.ui.AnimatedImage;
import com.memopoly.game.model.*;

import javax.swing.event.ChangeEvent;
import java.util.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

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
    private static final String MY_CELLS_WINDOW_TEXTURE_PATH = "my_cells_window.png";
    private static final String MEMES_WINDOW_TEXTURE_PATH = "memes_window.png";
    private static final float BUY_AND_AUCTION_MODAL_MIN_W = 765f;
    private static final float BUY_AND_AUCTION_MODAL_MIN_H = 570f;
    private static final float AUCTION_MODAL_MIN_W = 765f;
    private static final float AUCTION_MODAL_MIN_H = 570f;
    private static final float MEME_BANK_MODAL_MIN_W = 765f;
    private static final float MEME_BANK_MODAL_MIN_H = 570f;
    private static final float NOTIFICATION_MODAL_MIN_W = 628f;
    private static final float NOTIFICATION_MODAL_MIN_H = 243f;

    private static final Pattern DONATION_PATTERN_RU = Pattern.compile(
            "^(Игрок\\s+.+?\\s+задонатил\\s+\\d+)(\\s+игроку\\s+.+?,\\s+попав\\s+на\\s+клетку\\s+.+)$"
    );
    private static final Pattern DONATION_PATTERN_EN = Pattern.compile(
            "^(Player\\s+.+?\\s+donated\\s+\\d+)(\\s+to\\s+.+?\\s+after\\s+landing\\s+on\\s+.+)$"
    );
    private static final Pattern DONATION_EXTRACT_RU = Pattern.compile(
            "^Игрок\\s+(.+?)\\s+задонатил\\s+(\\d+)\\s+игроку\\s+(.+?),\\s+попав\\s+на\\s+клетку\\s+(.+)$"
    );
    private static final Pattern COINS_AMOUNT_PATTERN = Pattern.compile(
            "(.*?)(\\d+)\\s*(?:монет[а-я]*|coins?)(.*)", Pattern.DOTALL
    );
    private static final Pattern BONUS_AMOUNT_PATTERN = Pattern.compile("(\\d+)");
    private final List<Image> avatarFrameImages = new ArrayList<>();
    private float avatarFrameAnimTime = 0f;
    private static final float AVATAR_FRAME_SPIN_PERIOD = 9f; // секунд на полный оборот (очень медленно)
    private static final float AVATAR_FRAME_PULSE = 0.12f;
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
    private final Texture settingsButtonTexture;
    private final Texture depositButtonTexture;
    private final Texture withdrawButtonTexture;
    private final Texture skipButtonTexture;
    private final Texture participateButtonTexture;
    private final Texture declineButtonTexture;
    private final Texture backButtonTexture;
    private final Texture applyButtonTexture;
    private final Texture notificationWindowTexture;
    private final Texture buyAndAuctionWindowTexture;
    private final Texture auctionOrMemeBankWindowTexture;
    private final Texture inputTexture;
    private final Texture twitchTexture;
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
    private Table notificationModalContainer;
    private final Table buyOrAuctionModal;
    private final Table auctionModal;
    private final Table memeBankModal;
    private final VisLabel turnModalLabel;
    private final Table turnModalContentTable = new Table();
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
    private Texture payBtnRuTexture;
    private Texture payBtnEnTexture;
    private Button payJailFineButton;

    private String lastPlayersSignature = "";
    private String lastOwnedCellsSignature = "";
    private String lastHandMemesSignature = "";
    private long lastShownNotificationTimestamp = 0L;
    private float notificationVisibleTime = 0f;
    private final Map<String, Texture> memeTextureCache = new HashMap<>();
    private final Map<String, com.badlogic.gdx.graphics.g2d.Animation<TextureRegion>> memeAnimationCache = new HashMap<>();
    private final List<Texture> dynamicGifTextures = new ArrayList<>();
    private final Map<Integer, Integer> lastKnownPositions = new HashMap<>();
    private final Map<Integer, Boolean> lastInJailStates = new HashMap<>();
    private final Map<Integer, Integer> pendingJailStep = new HashMap<>();
    private float jailNotificationTimer = 0f;
    private String lastBattleSignature = "";
    private boolean battleInviteAnswered = false;
    private final List<Table> battleCardHolders = new ArrayList<>();
    private GameState.BattlePhase lastAnimatedBattlePhase = null;
    // Input validation error labels
    private VisLabel memeBankErrorLabel;
    private VisLabel auctionErrorLabel;
    private int memeBankAmount = 10;
    private VisLabel memeBankAmountLabel;
    private Button moderatorSkipButton;
    private Button moderatorTakeJailButton;
    private Table moderatorPromptTable;
    private VisLabel moderatorPromptLabel;
    // Debt UI fields
    private final Table debtModal;
    private VisLabel debtAmountLabel;
    private VisLabel debtStatusLabel;
    private Actor debtPayButton;
    private Actor debtSurrenderButton;
    private boolean isShieldAnimRunning = false;
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
    private Actor pendingActionTouchLayer;
    private Table pendingActionPlaqueLayer;
    private static final String PLUS_TWO_BUTTON_TEXTURE_PATH = "+2_btn.png";
    private static final String STAY_BUTTON_TEXTURE_PATH = "stay_btn.png";
    private static final String DEAL_BUTTON_TEXTURE_PATH = "deal_btn.png";
    private static final String STEAL_50_BUTTON_TEXTURE_PATH = "steal_50_btn.png";
    private final Texture plusTwoButtonTexture;
    private final Texture stayButtonTexture;
    private final Texture dealButtonTexture;
    private final Texture stealButtonTexture;
    private final Texture rectForMoneyTexture;
    private final Texture longRectForMoneyTexture;
    private final Texture houseIconTexture;

    private final com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable longRectMoneyPatch;
    private BoardRenderer.PendingActionMode pendingActionMode = BoardRenderer.PendingActionMode.NONE;

    private Button stealButton;
    private Button stayButton;
    private Button plusTwoButton;
    private Button dealButton;
    private Button bankruptEventButton;
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
    private List<Meme> battleHandSnapshot = null;
    private ChatWidget chatWidget;
    private final Texture tenUpTexture;
    private final Texture tenDownTexture;
    private final Texture changeTopicBtnTexture;
    private final List<String> battleTopics = new ArrayList<>();
    private int lastBattleTopicIndex = -1;
    private final Texture startBattleTexture;
    private final Texture inputMemeBattleTexture;
    private final Texture cardBoardTexture;
    private Table roleTooltip;
    private VisLabel roleTooltipLabel;
    private int lastTurnPlayerId = -1;
    private final Texture dealModalTexture;
    private final Texture dealSliderBgTexture;
    private final Texture dealSliderKnobTexture;
    private Table rentTooltip;
    private int lastHoveredRentCell = -1;
    private int lastHoveredRentHouses = -1;
    private boolean lastHoveredRentMonopoly = false;
    private Texture greenHouseTexture;
    private Texture redHouseTexture;
    private String cachedBattleResultLog = null;
    private static final String BUILD_BUTTON_TEXTURE_PATH = "build_btn.png";
    private static final String SELL_BUTTON_TEXTURE_PATH = "sell_btn.png";
    private final Texture buildButtonTexture;
    private final Texture sellButtonTexture;
    private static final String SHIELD_TEXTURE_PATH = "shield.png";
    private final Texture shieldTexture;
    private final Texture banShieldTexture;
    private final Texture rulesButtonTexture;
    private final Texture brokeShieldTexture;
    private final Map<Integer, Integer> lastKnownShields = new HashMap<>();
    private Group activeShieldAnimGroup = null;
    private boolean pendingShieldBreak = false;
    private String pendingStartMessage = null;
    private Table leftColumn;
    private Table memesPanel;
    private ScrollPane ownedScroll;
    private Table actionsHeaderContainer;
    private Texture timerCircleTexture;
    // Рамки аватарок строго по цвету роли (RoleInfo.color)
    private final Map<Role, Texture> avatarFrameByRole = new EnumMap<>(Role.class);
    private final Map<Actor, Role> roleInfoIcons = new HashMap<>();

    private VisLabel bidValueLabel;
    private VisLabel waitingBidLabel;
    private Table auctionControlsRow;
    private Table actionRow;
    private String lastActionRowSig = "";
    private int bidValue = 10;
    private ImageButton minusBidButton;
    private ImageButton plusBidButton;
    private ImageButton minusHundredBidButton;
    private ImageButton plusHundredBidButton;
    private VisLabel auctionLastOfferHeaderLabel;
    private Table auctionLastOfferRow;
    private VisLabel auctionLastOfferLabel;
    private Image auctionLastOfferCoinIcon;
    private VisLabel auctionTitleLabel;
    private VisLabel auctionTimerLabel;
    private VisLabel auctionTurnLabel;
    private VisLabel auctionCellNameLabel;
    private int lastHandledAuctionTurnPlayerId = -1;
    private int lastHandledAuctionHighestBid = -1;
    private final Texture hundUpTexture;
    private final Texture hundDownTexture;
    private Table bankBalanceRow;
    private VisLabel bankBalanceLabel;

    private static final String DICE_FACE_A_TEXTURE_PATH = "dice_a.png";
    private static final String DICE_FACE_B_TEXTURE_PATH = "dice_b.png";
    private static final float DICE_SIZE = 87f;
    private static final float DICE_ROLL_DURATION = 0.9f;
    private static final float DICE_BOUNCE_T = 0.55f;
    private static final float DICE_SETTLE_DURATION = 0.25f;
    private static final float DICE_HOLD_DURATION = 1.4f;
    private static final float DICE_FADE_DURATION = 0.3f;
    private final Texture diceFaceATexture;
    private final Texture diceFaceBTexture;
    private Texture diceShadowTexture;
    private Group diceRollOverlay;
    private Image diceImageLeft;
    private Image diceImageRight;
    private Image diceShadowLeft;
    private Image diceShadowRight;
    private VisLabel diceTotalLabel;

    private enum DiceAnimPhase {
        IDLE, ROLLING, SETTLE, HOLD, FADE
    }

    private DiceAnimPhase diceAnimPhase = DiceAnimPhase.IDLE;
    private float diceAnimTimer = 0f;
    private float diceFlickerTimer = 0f;
    private boolean diceIsDoubles = false;

    private boolean hasPendingMove = false;
    private int pendingMovePlayerId = -1;
    private int pendingFromCell = -1;
    private int pendingToCell = -1;
    private float pendingMoveTimer = 0f;

    private String currentBattleTopicInput = "";

    public GameScreen(Memopoly game) {
        super(game);
        this.language = game.getLanguageManager().getLanguage();
        Language language = this.language;
        boardRenderer = new BoardRenderer(game);
        boardRenderer.setMovementStepListener((playerId, cellIndex) -> {
            if (cellIndex == 0) {
                spawnStartBonusPill(playerId);
                if (pendingStartMessage != null) {
                    showLocalNotification(pendingStartMessage);
                    pendingStartMessage = null;
                } else {
                    GameState st = game.getLatestGameState();
                    Player p = st != null ? st.getPlayerById(playerId) : null;
                    if (p != null) {
                        int bonus = (st.playerRoles != null && Role.MEMOLOG.name().equals(st.playerRoles.get(playerId))) ? 215 : 200;
                        String msg = language == Language.RU
                                ? (p.name + " прошел(ла) через Старт и получает " + bonus + " монет!")
                                : (p.name + " passed Start and receives " + bonus + " coins!");
                        showLocalNotification(msg);
                    }
                }
            }
        });
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
        settingsButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveMenuTexture("settings_btn.png", language));
        depositButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(DEPOSIT_BUTTON_TEXTURE_PATH, language));
        withdrawButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(WITHDRAW_BUTTON_TEXTURE_PATH, language));
        skipButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture("skip_btn.png", language));
        banShieldTexture = loadTextureIfExistsLinear("ban_shield.png");
        rulesButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture("rules_btn.png", language));
        brokeShieldTexture = loadTextureIfExistsLinear("broke_shield.png");
        participateButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(PARTICIPATE_BUTTON_TEXTURE_PATH, language));
        declineButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture(DECLINE_BUTTON_TEXTURE_PATH, language));
        backButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveScreenTexture("back_btn.png", language));
        applyButtonTexture = loadTextureIfExists(
                TexturePathResolver.resolveScreenTexture("apply_btn.png", language));
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
        twitchTexture = loadTextureIfExists("twitch.png");
        backgroundTexture = loadTexture(BACKGROUND_TEXTURE_PATH);
        cellTextures = loadCellTextures();
        gameOverlayWindowTexture = loadTexture(GAME_OVERLAY_WINDOW_TEXTURE_PATH);
        playersWindowTexture = loadTextureWithFallback(PLAYERS_WINDOW_TEXTURE_PATH, MY_CELLS_WINDOW_TEXTURE_PATH);
        myCellsWindowTexture = loadTextureWithFallback(MY_CELLS_WINDOW_TEXTURE_PATH, PLAYERS_WINDOW_TEXTURE_PATH);
        memesWindowTexture = loadTextureWithFallback(MEMES_WINDOW_TEXTURE_PATH, PLAYERS_WINDOW_TEXTURE_PATH);
        tenUpTexture = loadOrFallback("ten_up.png", diceButtonTexture);
        tenDownTexture = loadOrFallback("ten_down.png", diceButtonTexture);
        hundUpTexture = loadOrFallback("hund_up.png", diceButtonTexture);
        hundDownTexture = loadOrFallback("hund_down.png", diceButtonTexture);
        startBattleTexture = loadOrFallback("start_battle.png", placeBidButtonTexture);
        inputMemeBattleTexture = loadOrFallback("input_memebattle.png", inputTexture);
        cardBoardTexture = loadOrFallback("card_board.png", gameOverlayWindowTexture);
        dealModalTexture = loadTextureIfExists("deal_modal.png");
        dealSliderBgTexture = loadTextureIfExists("screen_ui/background.png");
        dealSliderKnobTexture = loadTextureIfExists("screen_ui/knob.png");
        payBtnRuTexture = loadTextureIfExists("gamescreen_ui_ru/pay_btn.png");
        payBtnEnTexture = loadTextureIfExists("gamescreen_ui_en/pay_btn.png");
        diceFaceATexture = loadTextureIfExists(DICE_FACE_A_TEXTURE_PATH);
        diceFaceBTexture = loadTextureIfExists(DICE_FACE_B_TEXTURE_PATH);
        changeTopicBtnTexture = loadTextureIfExists("change_topic_btn.png");
        rectForMoneyTexture = loadTextureIfExistsLinear("rect_for_money.png");
        longRectForMoneyTexture = loadTextureIfExistsLinear("long_rect_for_money.png");
        houseIconTexture = loadTextureIfExistsLinear("build_branch.png");
        greenHouseTexture = loadTextureIfExistsLinear("green_house.png");
        redHouseTexture = loadTextureIfExistsLinear("red_house.png");

        longRectMoneyPatch = longRectForMoneyTexture != null ? new com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable(
                new com.badlogic.gdx.graphics.g2d.NinePatch(longRectForMoneyTexture, 266, 266, 0, 0)) : null;
        if (longRectMoneyPatch != null) {
            longRectMoneyPatch.setMinWidth(0f);
            longRectMoneyPatch.setMinHeight(0f);
        }
        loadBattleTopics();
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
        handMemesTable = new Table() {
            @Override
            public void act(float delta) {
                super.act(delta);
                tickAnimatedChildren(this, delta);
            }

            private void tickAnimatedChildren(Group group, float delta) {
                for (Actor a : group.getChildren()) {
                    if (a instanceof com.memopoly.ui.AnimatedImage) {
                        a.act(delta);
                    } else if (a instanceof Group) {
                        tickAnimatedChildren((Group) a, delta);
                    }
                }
            }
        };
        diceOverlay = new Table();
        currentCellOverlay = new Table();
        feedOverlay = new Table();
        turnNotificationModal = new Table();
        buyOrAuctionModal = new Table();
        auctionModal = new Table();
        memeBankModal = new Table();
        debtModal = new Table();
        turnModalLabel = new VisLabel("");
        turnModalLabel.setFontScale(0.85f);
        bidValueLabel = new VisLabel("");
        bidValueLabel.setColor(ACCENT_GOLD);
        waitingBidLabel = new VisLabel("");
        waitingBidLabel.setColor(BATTLE_TEXT_COLOR);
        waitingBidLabel.setWrap(true);
        waitingBidLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        waitingBidLabel.setFontScale(0.9f);
        auctionControlsRow = new Table();
        buyAuctionModalLabel = new VisLabel("");
        auctionModalLabel = new VisLabel("");
        memeBankModalLabel = new VisLabel("");

        auctionTitleLabel = new VisLabel(t("auction_title"));
        auctionTitleLabel.setFontScale(1.05f);
        auctionTitleLabel.setColor(BATTLE_TEXT_COLOR);

        auctionTimerLabel = new VisLabel("30");
        auctionTimerLabel.setFontScale(0.85f);
        auctionTimerLabel.setColor(BATTLE_TEXT_COLOR);
        auctionTimerLabel.setAlignment(com.badlogic.gdx.utils.Align.center);

        auctionTurnLabel = new VisLabel("");
        auctionTurnLabel.setFontScale(0.95f);
        auctionTurnLabel.setColor(BATTLE_TEXT_COLOR);

        auctionCellNameLabel = new VisLabel("");
        auctionCellNameLabel.setFontScale(0.95f);
        auctionCellNameLabel.setColor(BATTLE_TEXT_COLOR);

        auctionLastOfferHeaderLabel = new VisLabel(t("auction_last_offer"));
        auctionLastOfferHeaderLabel.setFontScale(0.85f);
        auctionLastOfferHeaderLabel.setColor(BATTLE_TEXT_COLOR);

        auctionLastOfferLabel = new VisLabel(" — ");
        auctionLastOfferLabel.setFontScale(0.9f);
        auctionLastOfferLabel.setColor(BATTLE_TEXT_COLOR);

        auctionLastOfferCoinIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        auctionLastOfferCoinIcon.setScaling(Scaling.fit);
        auctionLastOfferCoinIcon.setVisible(false);

        auctionLastOfferRow = new Table();
        auctionLastOfferRow.left();
        auctionLastOfferRow.add(auctionLastOfferLabel);
        auctionLastOfferRow.add(auctionLastOfferCoinIcon).size(20f, 20f).padLeft(4f);

        diceButton = createDiceButton();
        buyButton = createActionButton(buyButtonTexture);
        passButton = createActionButton(auctionButtonTexture);
        endTurnButton = createActionButton(endTurnButtonTexture);
        placeBidButton = createActionButton(placeBidButtonTexture);
        cancelAuctionButton = createOptionalActionButton(declineButtonTexture, t("btn_decline"));
        minusBidButton = createActionButton(tenDownTexture);
        plusBidButton = createActionButton(tenUpTexture);
        minusHundredBidButton = createActionButton(hundDownTexture);
        plusHundredBidButton = createActionButton(hundUpTexture);
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

    public static NinePatchDrawable createPillDrawable(Texture texture, float targetHeight) {
        return createPillDrawable(texture, targetHeight, 224, 224);
    }

    public static NinePatchDrawable createPillDrawable(Texture texture, float targetHeight, int left, int right) {
        if (texture == null) return null;
        NinePatch patch = new NinePatch(texture, left, right, 0, 0);

        // Масштабируем углы под целевую высоту виджета (например, 28px или 19px)
        float scale = targetHeight / (float) texture.getHeight();
        patch.scale(scale, scale);

        NinePatchDrawable drawable = new NinePatchDrawable(patch);
        // Сбрасываем исходные минимальные габариты текстуры, чтобы Scene2D не распирало
        drawable.setMinWidth(0f);
        drawable.setMinHeight(0f);
        return drawable;
    }

    private NinePatchDrawable rectMoneyBg() {
        return createPillDrawable(rectForMoneyTexture, 28f);
    }

    private Texture loadOrFallback(String path, Texture fallback) {
        Texture t = loadTextureIfExists(path);
        return t != null ? t : fallback;
    }

    private static final Role[] NO_ROLE_FALLBACK_ORDER = { Role.MODERATOR, Role.MONOPOLIST, Role.DOGE, Role.MEMOLOG };

    private void loadAvatarFrameTextures() {
        putAvatarFrame(Role.MEMOLOG, "blue_avatar.png");
        putAvatarFrame(Role.SMM, "purple_avatar.png");
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
        diceHintLabel.setWrap(false);
        diceHintLabel.setAlignment(Align.center);
        diceHintLabel.setColor(BATTLE_TEXT_COLOR);
        diceHintLabel.setFontScale(1.00f);
        diceOverlay.center();
        diceOverlay.setTouchable(Touchable.childrenOnly);
        Table diceContent = new Table();
        diceContent.center();
        stealButton = createTextureOrPillButton(stealButtonTexture, t("steal"));
        stealButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.log("SCAMMER", "Steal button clicked by localPlayerId=" + game.getClient().getLocalPlayerId());
                sendAction(GameActionRequest.ActionType.STEAL_COINS, 0, 0);
            }
        });
        bankruptEventButton = createTextureOrPillButton(dealButtonTexture, "Событие");
        bankruptEventButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showBankruptEventTargetWindow();
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
        Texture payTex = (language == Language.RU ? payBtnRuTexture : payBtnEnTexture);
        if (payTex == null)
            payTex = diceButtonTexture;
        payJailFineButton = createTextureOrPillButton(payTex, t("pay_jail_fine"));
        payJailFineButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.PAY_JAIL_FINE, 0, 0);
            }
        });
        moderatorSkipButton = createTextureOrPillButton(skipButtonTexture, t("skip_jail"));
        moderatorSkipButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.MODERATOR_SKIP_JAIL, 0, 0);
            }
        });
        moderatorTakeJailButton = createTextureOrPillButton(banShieldTexture, t("take_jail"));
        moderatorTakeJailButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.MODERATOR_TAKE_JAIL, 0, 0);
            }
        });

        moderatorPromptTable = new Table();
        moderatorPromptTable.center();
        moderatorPromptLabel = new VisLabel(language == Language.RU
                ? "Пропустить Ban или улететь в Ban, получив "
                : "Skip Ban or enter Ban and gain ");
        moderatorPromptLabel.setColor(BATTLE_TEXT_COLOR);
        moderatorPromptLabel.setFontScale(0.85f);
        moderatorPromptTable.add(moderatorPromptLabel).center();
        if (shieldTexture != null) {
            Image modShieldIcon = new Image(shieldTexture);
            modShieldIcon.setScaling(Scaling.fit);
            moderatorPromptTable.add(modShieldIcon).size(20f, 20f).padLeft(4f).center();
        }
        moderatorPromptTable.setVisible(false);

        actionRow = new Table();
        actionRow.center();
        actionRow.defaults().center();
        diceContent.defaults().center();
        diceContent.add(diceHintLabel).colspan(2).center().expandX().padBottom(14f).row();
        diceContent.add(moderatorPromptTable).colspan(2).center().expandX().padBottom(14f).row();
        diceContent.add(actionRow).colspan(2).center().expandX().row();
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
        ownedScroll = new ScrollPane(ownedCellsTable);
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
                GameState st = game.getLatestGameState();
                int highestBid = 0;
                if (st != null && st.auctionBids != null) {
                    for (int bid : st.auctionBids.values()) {
                        if (bid > highestBid)
                            highestBid = bid;
                    }
                }
                if (bidValue <= highestBid) {
                    auctionErrorLabel.setText(t("auction_bid_must_be_higher"));
                } else {
                    auctionErrorLabel.setText("");
                    sendAction(GameActionRequest.ActionType.PLACE_AUCTION_BID, 0, bidValue);
                }
            }
        });
        cancelAuctionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.CANCEL_AUCTION, 0, 0);
            }
        });
        attachRepeatingListener(minusBidButton, () -> changeBidBy(-10));
        attachRepeatingListener(plusBidButton, () -> changeBidBy(10));
        attachRepeatingListener(minusHundredBidButton, () -> changeBidBy(-100));
        attachRepeatingListener(plusHundredBidButton, () -> changeBidBy(100));
        memeBankDepositButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int amount = memeBankAmount;
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
                    memeBankAmount = 10;
                    if (memeBankAmountLabel != null)
                        memeBankAmountLabel.setText("10");
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
        playersPanel = new Table();
        playersPanelRef = playersPanel;
        playersPanel.setBackground(window(playersWindowTexture));
        playersPanel.pad(24f, 14f, 14f, 14f);
        playersPanel.add(playersTitleLabel).center().padBottom(10f).row();
        playersPanel.add(playersScroll).grow().row();

        bankBalanceRow = new Table();
        bankBalanceRow.center();
        VisLabel bankTitleLabel = new VisLabel(language == Language.RU ? "Вклад: " : "Bank: ");
        bankTitleLabel.setColor(BATTLE_TEXT_COLOR);
        bankTitleLabel.setFontScale(0.85f);
        bankBalanceLabel = new VisLabel("0");
        bankBalanceLabel.setColor(ACCENT_GOLD);
        bankBalanceLabel.setFontScale(0.85f);
        Image bankCoinIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        bankCoinIcon.setScaling(Scaling.fit);
        bankBalanceRow.add(bankTitleLabel);
        bankBalanceRow.add(bankBalanceLabel).padRight(4f);
        bankBalanceRow.add(bankCoinIcon).size(18f, 18f);
        bankBalanceRow.setVisible(false);
        playersPanel.add(bankBalanceRow).growX().padTop(6f).padBottom(4f).row();

        Table bottomRow = new Table();
        Actor settingsBtn = createTextureOrPillButton(settingsButtonTexture,
                language == Language.RU ? "Настройки" : "Settings");
        settingsBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showSettingsModal();
            }
        });
        Actor rulesBtn = createTextureOrPillButton(rulesButtonTexture,
                language == Language.RU ? "Правила" : "Rules");
        rulesBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showRulesModal();
            }
        });
        bottomRow.add(exitToMenuButton).width(110f).height(56f).left().padRight(8f);
        bottomRow.add(settingsBtn).width(110f).height(56f).center().padRight(8f);
        bottomRow.add(rulesBtn).width(110f).height(56f).right();
        playersPanel.add(bottomRow).growX().padTop(10f).padBottom(16f);
        leftColumn = new Table();
        leftColumn.top();
        leftColumn.add(playersPanel).width(380f).expandY().fillY().row();
        leftColumn.add(chatWidget).width(380f).height(270f).padTop(16f);
        root.add(leftColumn).top().left();
        root.add().expand().fill();
        VisLabel ownedTitleLabel = new VisLabel("ACTIONS");
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
        actionsHeaderContainer = new Table();
        ownedPanel.add(actionsHeaderContainer).growX().padBottom(8f).row();
        ownedPanel.add(ownedScroll).grow();
        memesPanel = new Table();
        memesPanel.setBackground(window(memesWindowTexture));
        memesPanel.pad(24f, 12f, 14f, 12f);
        memesPanel.add(memesTitleLabel).center().padBottom(10f).row();
        memesPanel.add(memesScroll).grow();
        Table rightColumn = new Table();
        rightColumn.top();
        rightColumn.add(ownedPanel).width(380f).height(530f).row();
        rightColumn.add(memesPanel).width(380f).expandY().fillY().padTop(12f).row();
        root.add(rightColumn).top().right();
        configureModal(turnNotificationModal, notificationWindowTexture, turnModalLabel, NOTIFICATION_MODAL_MIN_W,
                NOTIFICATION_MODAL_MIN_H, true);
        Table turnModalWindow = (Table) turnNotificationModal.getCells().first().getActor();
        turnModalWindow.clearChildren();
        turnModalWindow.pad(14f, 26f, 14f, 26f);
        turnModalContentTable.center();
        float turnTextWidth = Math.max(480f, NOTIFICATION_MODAL_MIN_W * 0.88f);
        turnModalWindow.add(turnModalContentTable).width(turnTextWidth).expand().center().pad(14f, 26f, 14f, 26f).row();
        configureModal(buyOrAuctionModal, buyAndAuctionWindowTexture, buyAuctionModalLabel, BUY_AND_AUCTION_MODAL_MIN_W,
                BUY_AND_AUCTION_MODAL_MIN_H, false);
        configureModal(auctionModal, auctionOrMemeBankWindowTexture, auctionModalLabel, AUCTION_MODAL_MIN_W,
                AUCTION_MODAL_MIN_H, false);
        configureModal(memeBankModal, auctionOrMemeBankWindowTexture, memeBankModalLabel, MEME_BANK_MODAL_MIN_W,
                MEME_BANK_MODAL_MIN_H, false);
        setupModalControls();
        stage.addActor(root);
        stage.addActor(diceOverlay);
        createDiceRollOverlay();
        stage.addActor(buyOrAuctionModal);
        stage.addActor(auctionModal);
        stage.addActor(memeBankModal);
        stage.addActor(debtModal);
        notificationModalContainer = new Table();
        notificationModalContainer.setFillParent(true);
        notificationModalContainer.center();
        notificationModalContainer.setTouchable(Touchable.childrenOnly);
        notificationModalContainer.add(turnNotificationModal).center();
        stage.addActor(notificationModalContainer);
        layoutBoardOverlays();
        roleTooltip = new Table();
        roleTooltip.setBackground(window(gameOverlayWindowTexture));
        roleTooltip.pad(12f);
        roleTooltipLabel = new VisLabel("");
        roleTooltipLabel.setWrap(true);
        roleTooltipLabel.setColor(BATTLE_TEXT_COLOR);
        roleTooltipLabel.setFontScale(0.8f);
        roleTooltip.add(roleTooltipLabel).width(440f);
        roleTooltip.setVisible(false);
        roleTooltip.setTouchable(Touchable.disabled);
        stage.addActor(roleTooltip);

        rentTooltip = new Table();
        if (dealModalTexture != null) {
            NinePatchDrawable dealModalDrawable = new NinePatchDrawable(new NinePatch(dealModalTexture, 30, 30, 30, 30));
            dealModalDrawable.setMinWidth(0f);
            dealModalDrawable.setMinHeight(0f);
            rentTooltip.setBackground(dealModalDrawable);
        } else if (gameOverlayWindowTexture != null) {
            NinePatchDrawable overlayDrawable = new NinePatchDrawable(new NinePatch(gameOverlayWindowTexture, 24, 24, 24, 24));
            overlayDrawable.setMinWidth(0f);
            overlayDrawable.setMinHeight(0f);
            rentTooltip.setBackground(overlayDrawable);
        } else {
            rentTooltip.setBackground(VisUI.getSkin().getDrawable("white"));
        }
        rentTooltip.pad(10f, 16f, 10f, 16f);
        rentTooltip.setVisible(false);
        rentTooltip.setTouchable(Touchable.disabled);
        stage.addActor(rentTooltip);

        stage.addListener(new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, int keycode) {
                if (keycode == Input.Keys.ESCAPE && pendingActionMode != BoardRenderer.PendingActionMode.NONE) {
                    clearPendingActionSelection();
                    return true;
                }
                if (keycode == Input.Keys.F9) {
                    devPanelVisible = !devPanelVisible;
                    if (devPanel != null) {
                        devPanel.setVisible(devPanelVisible);
                        if (devPanelVisible) {
                            devPanel.setTouchable(Touchable.enabled);
                            devPanel.toFront();
                        } else {
                            devPanel.setTouchable(Touchable.disabled);
                        }
                    }
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
        devPanel.setTouchable(Touchable.disabled);
        stage.addActor(devPanel);
    }

    private void updateDevPanel(GameState state) {
        if (devPanel == null)
            return;
        devPanel.setVisible(devPanelVisible);
        if (!devPanelVisible) {
            devPanel.setTouchable(Touchable.disabled);
            return;
        }
        devPanel.setTouchable(Touchable.enabled);
        devPanel.toFront();

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
                    Gdx.app.log("DEV", "Dev mode toggle clicked by " + game.getClient().getLocalPlayerId());
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

            VisTextButton sendJailBtn = createPillButton(t("dev_send_to_jail"));
            sendJailBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendAction(GameActionRequest.ActionType.DEV_SEND_TO_JAIL, 0, 0);
                }
            });
            devPanel.add(sendJailBtn).left().padBottom(4f).row();

            VisTextButton testBattleBtn = createPillButton(language == Language.RU ? "Тест Баттла" : "Test Battle");
            testBattleBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendAction(GameActionRequest.ActionType.DEV_START_BATTLE, 0, 0);
                }
            });
            devPanel.add(testBattleBtn).left().padBottom(4f).row();

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

    private void attachRepeatingListener(Button button, Runnable action) {
        button.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, int buttonId) {
                if (button.isDisabled()) {
                    return false;
                }
                button.clearActions();
                action.run();
                button.addAction(Actions.sequence(
                        Actions.delay(0.4f),
                        Actions.forever(Actions.sequence(
                                Actions.run(action),
                                Actions.delay(0.09f)
                        ))
                ));
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (!button.isOver()) {
                    button.clearActions();
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, int buttonId) {
                button.clearActions();
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor) {
                if (pointer == -1 || !button.isPressed()) {
                    button.clearActions();
                }
            }
        });
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

    private void updateAvatarFrameAnimation(float delta) {
        avatarFrameAnimTime += delta;
        float phase = (avatarFrameAnimTime % AVATAR_FRAME_SPIN_PERIOD) / AVATAR_FRAME_SPIN_PERIOD;
        float rotation = -360f * phase; // по часовой, медленно (9с на оборот)
        float scale = 1f + AVATAR_FRAME_PULSE * (float) Math.sin(phase * Math.PI);
        for (Image frame : avatarFrameImages) {
            if (frame.getWidth() > 0f && frame.getHeight() > 0f) {
                frame.setOrigin(frame.getWidth() * 0.5f, frame.getHeight() * 0.5f);
            }
            frame.setRotation(rotation);
            frame.setScale(scale);
        }
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

    private void updateRentTooltip() {
        if (rentTooltip == null || boardRenderer == null) {
            return;
        }
        if (isCameraBlocked() || tradeWindow != null || (buyOrAuctionModal != null && buyOrAuctionModal.isVisible())
                || (auctionModal != null && auctionModal.isVisible())
                || (memeBankModal != null && memeBankModal.isVisible())
                || (turnNotificationModal != null && turnNotificationModal.isVisible())
                || (battleOverlay != null && battleOverlay.isVisible())) {
            rentTooltip.setVisible(false);
            lastHoveredRentCell = -1;
            return;
        }

        GameState state = game.getLatestGameState();
        if (state == null || boardCells == null) {
            rentTooltip.setVisible(false);
            lastHoveredRentCell = -1;
            return;
        }

        Vector2 worldMouse = boardRenderer.screenToWorld(Gdx.input.getX(), Gdx.input.getY());
        int hoveredCellId = boardRenderer.getHoveredRentCell(worldMouse.x, worldMouse.y, state, boardCells);

        if (hoveredCellId >= 0 && hoveredCellId < boardCells.size()) {
            BoardCell cell = boardCells.get(hoveredCellId);
            int houses = (state.cellHouses != null) ? state.cellHouses.getOrDefault(hoveredCellId, 0) : 0;
            int ownerId = (state.cellOwners != null) ? state.cellOwners.getOrDefault(hoveredCellId, -1) : -1;
            boolean hasMonopoly = MonopolyUtils.hasFullMonopoly(cell, ownerId, state, boardCells);

            if (hoveredCellId != lastHoveredRentCell || houses != lastHoveredRentHouses || hasMonopoly != lastHoveredRentMonopoly) {
                rebuildRentTooltipContent(cell, houses, hasMonopoly);
                lastHoveredRentCell = hoveredCellId;
                lastHoveredRentHouses = houses;
                lastHoveredRentMonopoly = hasMonopoly;
            }

            Vector2 stageTouch = stage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            float tooltipW = rentTooltip.getWidth();
            float tooltipH = rentTooltip.getHeight();

            float x = stageTouch.x + 16f;
            float y = stageTouch.y - tooltipH - 8f;

            if (x + tooltipW > stage.getWidth() - 8f) {
                x = stageTouch.x - tooltipW - 16f;
            }
            x = Math.max(8f, Math.min(x, stage.getWidth() - tooltipW - 8f));
            y = Math.max(8f, Math.min(y, stage.getHeight() - tooltipH - 8f));

            rentTooltip.setPosition(x, y);
            rentTooltip.setVisible(true);
            rentTooltip.toFront();
        } else {
            rentTooltip.setVisible(false);
            lastHoveredRentCell = -1;
        }
    }

    private void rebuildRentTooltipContent(BoardCell cell, int currentHouses, boolean hasMonopoly) {
        rentTooltip.clearChildren();
        boolean isRu = (language == Language.RU);

        VisLabel titleLabel = new VisLabel(cell.name != null ? cell.name : "Cell " + cell.id);
        titleLabel.setColor(Color.valueOf("000A3E"));
        titleLabel.setFontScale(0.95f);
        rentTooltip.add(titleLabel).colspan(2).padBottom(8f).center().row();

        if (!cell.isBuildableSituation()) {
            Table feeTable = new Table();
            feeTable.center();
            VisLabel feeLabel = new VisLabel((isRu ? "Донат: " : "Donation: ") + cell.getEntranceFee());
            feeLabel.setColor(Color.valueOf("C97A00"));
            feeLabel.setFontScale(0.9f);
            feeTable.add(feeLabel).padRight(4f);
            if (moneyTexture != null) {
                Image coin = new Image(moneyTexture);
                coin.setScaling(Scaling.fit);
                feeTable.add(coin).size(18f, 18f);
            }
            rentTooltip.add(feeTable).colspan(2).padBottom(4f).center().row();
            rentTooltip.pack();
            return;
        }

        int activeTier;
        if (currentHouses > 0) {
            activeTier = currentHouses + 1; // 1 house -> 2, 2 -> 3, 3 -> 4, 4 -> 5, 5 (Trend) -> 6
        } else if (hasMonopoly) {
            activeTier = 1;
        } else {
            activeTier = 0;
        }

        for (int tier = 0; tier < 7; tier++) {
            boolean isActive = (tier == activeTier);
            Color labelColor = isActive ? Color.valueOf("C97A00") : Color.valueOf("000A3E");
            Color valueColor = isActive ? Color.valueOf("C97A00") : Color.valueOf("000A3E");

            Table labelTable = new Table();
            labelTable.left();

            int rentValue;
            if (tier == 0) {
                rentValue = MonopolyUtils.calculateRentForTier(cell, 0, false);
                VisLabel lbl = new VisLabel(isRu ? "Донат: " : "Donation: ");
                lbl.setColor(labelColor);
                lbl.setFontScale(0.85f);
                labelTable.add(lbl);
            } else if (tier == 1) {
                rentValue = MonopolyUtils.calculateRentForTier(cell, 0, true);
                VisLabel lbl = new VisLabel(isRu ? "Донат (x2 с монополией): " : "Donation (x2 with monopoly): ");
                lbl.setColor(labelColor);
                lbl.setFontScale(0.85f);
                labelTable.add(lbl);
            } else if (tier >= 2 && tier <= 5) {
                int housesCount = tier - 1; // 1 to 4
                rentValue = MonopolyUtils.calculateRentForTier(cell, housesCount, true);
                if (greenHouseTexture != null) {
                    for (int h = 0; h < housesCount; h++) {
                        Image house = new Image(greenHouseTexture);
                        house.setScaling(Scaling.fit);
                        labelTable.add(house).size(24f, 24f).padRight(2f);
                    }
                }
                VisLabel colon = new VisLabel(": ");
                colon.setColor(labelColor);
                colon.setFontScale(0.85f);
                labelTable.add(colon);
            } else { // tier == 6, Trend (5th house level)
                rentValue = MonopolyUtils.calculateRentForTier(cell, 5, true);
                if (redHouseTexture != null) {
                    Image house = new Image(redHouseTexture);
                    house.setScaling(Scaling.fit);
                    labelTable.add(house).size(24f, 24f).padRight(2f);
                }
                VisLabel colon = new VisLabel(": ");
                colon.setColor(labelColor);
                colon.setFontScale(0.85f);
                labelTable.add(colon);
            }

            Table valueTable = new Table();
            valueTable.right();
            VisLabel valueLabel = new VisLabel(String.valueOf(rentValue));
            valueLabel.setColor(valueColor);
            valueLabel.setFontScale(0.85f);
            valueTable.add(valueLabel);
            if (moneyTexture != null) {
                Image coin = new Image(moneyTexture);
                coin.setScaling(Scaling.fit);
                valueTable.add(coin).size(18f, 18f).padLeft(4f);
            }

            rentTooltip.add(labelTable).left().padRight(12f).padBottom(2f);
            rentTooltip.add(valueTable).right().padBottom(2f).row();
        }

        rentTooltip.pack();
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
        com.badlogic.gdx.math.Rectangle bBounds = boardRenderer != null ? boardRenderer.getBoardBounds()
                : new com.badlogic.gdx.math.Rectangle(420f, 0f, 1080f, 1080f);
        float safePad = 10f;
        float ringW = bBounds.width * 0.128f;
        float ringH = bBounds.height * 0.128f;
        float minX = bBounds.x + ringW + safePad;
        float maxX = bBounds.x + bBounds.width - ringW - safePad;
        float minY = bBounds.y + ringH + safePad;
        float maxY = bBounds.y + bBounds.height - ringH - safePad;
        tradeWindow.setSize(maxX - minX, maxY - minY);
        tradeWindow.setPosition(minX, minY);

        // Основной контейнер: две половины
        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.pad(10);

        NinePatchDrawable panelBg = dealModalTexture != null
                ? new NinePatchDrawable(new NinePatch(dealModalTexture, 30, 30, 30, 30))
                : (NinePatchDrawable) VisUI.getSkin().getDrawable("white");

        Table leftPanel = new Table();
        leftPanel.setBackground(panelBg);
        leftPanel.pad(16f, 20f, 16f, 20f);

        Table rightPanel = new Table();
        rightPanel.setBackground(panelBg);
        rightPanel.pad(16f, 20f, 16f, 20f);

        mainTable.add(leftPanel).expand().fill().padRight(8f);
        mainTable.add(rightPanel).expand().fill().padLeft(8f);

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
        com.badlogic.gdx.math.Rectangle bBounds = boardRenderer != null ? boardRenderer.getBoardBounds()
                : new com.badlogic.gdx.math.Rectangle(420f, 0f, 1080f, 1080f);
        float safePad = 10f;
        float ringW = bBounds.width * 0.128f;
        float ringH = bBounds.height * 0.128f;
        float minX = bBounds.x + ringW + safePad;
        float maxX = bBounds.x + bBounds.width - ringW - safePad;
        float minY = bBounds.y + ringH + safePad;
        float maxY = bBounds.y + bBounds.height - ringH - safePad;
        tradeWindow.setSize(maxX - minX, maxY - minY);
        tradeWindow.setPosition(minX, minY);

        Table mainTable = new Table();
        mainTable.setFillParent(true);
        mainTable.pad(10);

        NinePatchDrawable panelBg = dealModalTexture != null
                ? new NinePatchDrawable(new NinePatch(dealModalTexture, 30, 30, 30, 30))
                : (NinePatchDrawable) VisUI.getSkin().getDrawable("white");

        Table leftPanel = new Table();
        leftPanel.setBackground(panelBg);
        leftPanel.pad(16f, 20f, 16f, 20f);

        Table rightPanel = new Table();
        rightPanel.setBackground(panelBg);
        rightPanel.pad(16f, 20f, 16f, 20f);

        mainTable.add(leftPanel).expand().fill().padRight(8f);
        mainTable.add(rightPanel).expand().fill().padLeft(8f);

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
        leftPanel.clearChildren();
        rightPanel.clearChildren();
        NinePatchDrawable panelBg = dealModalTexture != null
                ? new NinePatchDrawable(new NinePatch(dealModalTexture, 30, 30, 30, 30))
                : (NinePatchDrawable) VisUI.getSkin().getDrawable("white");
        leftPanel.setBackground(panelBg);
        rightPanel.setBackground(panelBg);

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
            VisSlider initSlider = createMoneySlider(0, maxMoney, 5f, 0);
            initSlider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    draftMyMoney = (int) initSlider.getValue();
                }
            });
            leftPanel.add(initSlider).width(260f).padBottom(10f).row();
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
            VisSlider targetSlider = createMoneySlider(0, maxMoney, 5f, 0);
            targetSlider.addListener(new ChangeListener() {

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    draftTheirMoney = (int) targetSlider.getValue();
                }
            });
            rightPanel.add(targetSlider).width(260f).padBottom(10f).row();
            targetSlider.setVisible(selectedTheirCells.size() > 0);
        } else if (isIncomingTrade) {

            VisLabel targetMoneyLabel = new VisLabel(
                    t("money_requested") + ": " + gs.tradeTargetMoney);
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
        VisSlider slider;
        if (dealSliderBgTexture != null && dealSliderKnobTexture != null) {
            Slider.SliderStyle style = new Slider.SliderStyle();
            NinePatchDrawable bgDrawable = new NinePatchDrawable(new NinePatch(dealSliderBgTexture, 6, 6, 6, 6));
            bgDrawable.setMinHeight(10f);
            TextureRegionDrawable knobDrawable = new TextureRegionDrawable(new TextureRegion(dealSliderKnobTexture));
            knobDrawable.setMinSize(22f, 22f);
            style.background = bgDrawable;
            style.knob = knobDrawable;
            style.knobOver = knobDrawable;
            style.knobDown = knobDrawable;
            slider = new VisSlider(min, max, step, false, style);
        } else {
            slider = new VisSlider(min, max, step, false);
        }
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
        clearPendingActionSelection();
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
        tradeWindow.pad(20f);
        com.badlogic.gdx.math.Rectangle bBounds = boardRenderer != null ? boardRenderer.getBoardBounds()
                : new com.badlogic.gdx.math.Rectangle(420f, 0f, 1080f, 1080f);
        float windowWidth = 772f;
        float windowHeight = 670f;
        float centerX = bBounds.x + bBounds.width / 2f;
        float centerY = bBounds.y + bBounds.height / 2f;
        tradeWindow.setSize(windowWidth, windowHeight);
        tradeWindow.setPosition(centerX - windowWidth / 2f, centerY - windowHeight / 2f);
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
        half.pad(10f);

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
        nameLabel.setFontScale(0.75f);
        half.add(nameLabel).center().padBottom(8f).row();

        boolean editable = tradeMode == TRADE_MODE_BUILD;
        Image coin = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        coin.setScaling(Scaling.fit);
        VisLabel moneyLabel = new VisLabel(String.valueOf(money));
        moneyLabel.setColor(BATTLE_TEXT_COLOR);

        if (editable) {
            final int maxAllowedMoney = Math.max(0, maxMoney);
            VisSlider slider = createMoneySlider(0, Math.max(1, maxAllowedMoney), 1f, money);
            slider.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    int v = com.badlogic.gdx.math.MathUtils.clamp((int) slider.getValue(), 0, maxAllowedMoney);
                    if (left)
                        draftMyMoney = v;
                    else
                        draftTheirMoney = v;
                    moneyLabel.setText(String.valueOf(v));
                }
            });

            Table sliderRow = new Table();
            sliderRow.add(slider).width(260f).padRight(10f);
            sliderRow.add(moneyLabel).padRight(4f);
            sliderRow.add(coin).size(20f, 20f).padRight(16f);
            half.add(sliderRow).center().padBottom(8f).row();
        } else {
            Table moneyRow = new Table();
            moneyRow.add(moneyLabel).padRight(4f);
            moneyRow.add(coin).size(20f, 20f).padRight(16f);
            half.add(moneyRow).center().padBottom(8f).row();
        }

        Table chips = new Table();
        int c = 0;
        for (int cellId : cells) {
            chips.add(createCellChip(cellId, editable, left)).size(160f, 130f).pad(6f);
            if (++c % 2 == 0)
                chips.row();
        }
        if (c == 0) {
            VisLabel hint = new VisLabel(left ? t("trade_pick_own") : t("trade_pick_their"));
            hint.setColor(BATTLE_TEXT_COLOR);
            hint.setWrap(true);
            hint.setAlignment(Align.center);
            hint.setFontScale(0.95f);
            chips.add(hint).width(360f).center();
        }
        half.add(chips).expand().fill().top().row();
        return half;
    }

    private Table createCellChip(int cellId, boolean clickable, boolean leftSide) {
        Table chip = new Table();
        chip.pad(2f);
        com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> cellAnim = (boardRenderer != null
                && boardRenderer.getCellAnimations() != null)
                        ? boardRenderer.getCellAnimations().get(cellId)
                        : null;
        Actor img = cellAnim != null ? new AnimatedImage(cellAnim)
                : new Image(cellTextures[cellId % cellTextures.length]);
        if (img instanceof Image)
            ((Image) img).setScaling(Scaling.fit);
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
                if (pendingActionMode != BoardRenderer.PendingActionMode.NONE) {
                    handlePendingActionClick(layer.getX() + x, layer.getY() + y);
                    return;
                }
                GameState gs = game.getLatestGameState();
                if (gs == null || tradeMode != TRADE_MODE_BUILD)
                    return;
                handleCellClickForTrade(gs, layer.getX() + x, layer.getY() + y);
            }
        });
        stage.addActor(layer);
        tradeTouchLayer = layer;
    }

    private void handlePendingActionClick(float worldX, float worldY) {
        if (pendingActionMode == BoardRenderer.PendingActionMode.NONE) {
            return;
        }
        int cellId = boardRenderer.getCellAt(worldX, worldY);
        GameState state = game.getLatestGameState();
        int localId = game.getClient().getLocalPlayerId();
        Player player = state != null ? state.getPlayerById(localId) : null;
        if (player == null || !getPendingActionSelectableCells(state, player).contains(cellId)) {
            clearPendingActionSelection();
            return;
        }

        boolean mortgaged = state.cellMortgaged.getOrDefault(cellId, false);
        int houses = state.cellHouses.getOrDefault(cellId, 0);
        BoardCell cell = cellId >= 0 && cellId < boardCells.size() ? boardCells.get(cellId) : null;

        if (pendingActionMode == BoardRenderer.PendingActionMode.MORTGAGE) {
            if (!mortgaged && houses == 0) {
                sendAction(GameActionRequest.ActionType.MORTGAGE_CELL, cellId, 0);
            } else if (mortgaged) {
                int unmortgageCost = MonopolyUtils.getUnmortgagePrice(cell);
                if (player.money >= unmortgageCost) {
                    sendAction(GameActionRequest.ActionType.BUY_BACK_CELL, cellId, 0);
                } else {
                    showLocalNotification(language == Language.RU ? "Недостаточно монет для выкупа клетки" : "Not enough coins to unmortgage");
                }
            }
        } else if (pendingActionMode == BoardRenderer.PendingActionMode.UNMORTGAGE && mortgaged) {
            int unmortgageCost = MonopolyUtils.getUnmortgagePrice(cell);
            if (player.money >= unmortgageCost) {
                sendAction(GameActionRequest.ActionType.BUY_BACK_CELL, cellId, 0);
            } else {
                showLocalNotification(language == Language.RU ? "Недостаточно монет для выкупа клетки" : "Not enough coins to unmortgage");
            }
        } else if (pendingActionMode == BoardRenderer.PendingActionMode.BUILD_SELL
                && cell != null && cell.group != null && isCellInMonopoly(cell, player)
                && houses < 5) {
            Role role = Role.of(state.playerRoles != null ? state.playerRoles.get(player.id) : null);
            int cost = MonopolyUtils.getHousePrice(cell, role);
            if (player.money >= cost) {
                sendAction(GameActionRequest.ActionType.BUY_HOUSE, cellId, 0);
            } else {
                showLocalNotification(language == Language.RU ? "Недостаточно монет для покупки поста" : "Not enough coins to buy post");
            }
        } else if (pendingActionMode == BoardRenderer.PendingActionMode.SELL_HOUSE
                && cell != null && cell.group != null && isCellInMonopoly(cell, player)
                && houses > 0) {
            sendAction(GameActionRequest.ActionType.SELL_HOUSE, cellId, 0);
        }
        // Оверлей НЕ закрывается — обновляем плашки с актуальным состоянием
        rebuildOwnedCells(state, player, true, state.currentPhase);
        syncPendingActionLayers(state, player);
    }

    private void spawnStartBonusPill(int playerId) {
        GameState st = game.getLatestGameState();
        int bonus = 200;
        if (st != null && st.playerRoles != null && Role.MEMOLOG.name().equals(st.playerRoles.get(playerId))) {
            bonus = 215;
        }

        Table pill = new Table();
        Texture pillBg = UiShapes.outlinedPill(90, 36, 2, Color.WHITE, Color.valueOf("000A3E"));
        pill.setBackground(new TextureRegionDrawable(new TextureRegion(pillBg)));
        pill.pad(4f, 10f, 4f, 10f);

        VisLabel bonusLabel = new VisLabel("+" + bonus);
        bonusLabel.setColor(Color.valueOf("000A3E"));
        bonusLabel.setFontScale(0.95f);
        pill.add(bonusLabel).padRight(4f);

        if (moneyTexture != null) {
            Image coin = new Image(moneyTexture);
            coin.setScaling(Scaling.fit);
            pill.add(coin).size(16f, 16f);
        }
        pill.pack();

        Rectangle startBounds = boardRenderer.getCellBounds(0);
        float worldX = startBounds.x + startBounds.width * 0.5f;
        float worldY = startBounds.y + startBounds.height * 0.55f;
        Vector2 stagePos = boardRenderer.worldToStageCoordinates(worldX, worldY, stage);

        pill.setPosition(stagePos.x - pill.getWidth() * 0.5f, stagePos.y);
        pill.getColor().a = 1f;
        pill.setTouchable(Touchable.disabled);

        pill.addAction(Actions.sequence(
                Actions.parallel(
                        Actions.moveBy(0, 40f, 1.1f, com.badlogic.gdx.math.Interpolation.pow2Out),
                        Actions.sequence(
                                Actions.delay(0.5f),
                                Actions.fadeOut(0.6f))),
                Actions.removeActor()));

        stage.addActor(pill);
        pill.toFront();
    }

    private boolean isPurchaseNotification(String message) {
        if (message == null) return false;
        String lower = message.toLowerCase();
        return lower.contains("купил(а)") || lower.contains("купил") || lower.contains("купила")
                || lower.contains("purchased") || lower.contains("bought");
    }

    public void showEventModal(String title, String description) {
        String msg = (title != null && !title.isBlank()) ? (title + " - " + description) : description;
        showEventModal(msg);
    }

    public void showEventModal(String message) {
        if (message == null || message.isBlank())
            return;
        if (chatWidget != null) {
            chatWidget.showSystemMessage(message);
        }
        updateNotificationModalContent(message);
        turnNotificationModal.pack();
        if (notificationModalContainer != null) {
            notificationModalContainer.invalidateHierarchy();
            notificationModalContainer.toFront();
        }
        setModalVisible(turnNotificationModal, true);
        turnNotificationModal.setTouchable(Touchable.disabled);
        turnNotificationModal.toFront();
        notificationVisibleTime = isPurchaseNotification(message) ? 3.0f : 6.0f;
    }

    private void showLocalNotification(String message) {
        if (message == null || message.isBlank())
            return;
        if (chatWidget != null) {
            chatWidget.showSystemMessage(message);
        }
        turnNotificationModal.clearActions();
        turnNotificationModal.getColor().a = 1f;
        updateNotificationModalContent(message);
        turnNotificationModal.pack();
        if (notificationModalContainer != null) {
            notificationModalContainer.invalidateHierarchy();
            notificationModalContainer.toFront();
        }
        setModalVisible(turnNotificationModal, true);
        turnNotificationModal.setTouchable(Touchable.disabled);
        turnNotificationModal.toFront();
        notificationVisibleTime = isPurchaseNotification(message) ? 3.0f : 6.0f;
    }

    private Color getPlayerRoleColor(GameState state, Player player) {
        if (player == null) return BATTLE_TEXT_COLOR;
        Role role = null;
        if (state != null && state.playerRoles != null) {
            role = Role.of(state.playerRoles.get(player.id));
        }
        if (role == null && state != null && state.players != null) {
            int idx = state.players.indexOf(player);
            if (idx >= 0 && idx < NO_ROLE_FALLBACK_ORDER.length) {
                role = NO_ROLE_FALLBACK_ORDER[idx];
            }
        }
        return role != null ? com.memopoly.utils.RoleInfo.color(role) : BATTLE_TEXT_COLOR;
    }

    private Player findPlayerByName(GameState state, String name) {
        if (state == null || state.players == null || name == null) return null;
        for (Player p : state.players) {
            if (name.equalsIgnoreCase(p.name)) return p;
        }
        return null;
    }

    private void updateNotificationModalContent(String rawText) {
        if (turnModalContentTable == null) return;
        turnModalContentTable.clearChildren();
        if (rawText == null || rawText.isBlank()) {
            turnModalLabel.setText("");
            return;
        }

        String text = rawText.replace("\u2014", " - ").replace("\u2013", " - ");
        turnModalLabel.setText(text);

        GameState state = game.getLatestGameState();
        final float maxContentWidth = 340f;

        // 1. Free Parking Shield notification
        if (text.contains("получает щит на Бесплатной стоянке") || text.contains("receives a shield at Free Parking")) {
            Table shieldRow = new Table();
            shieldRow.center();
            Player p = null;
            if (state != null && state.players != null) {
                for (Player pl : state.players) {
                    if (text.startsWith(pl.name) || text.contains(" " + pl.name + " ")) {
                        p = pl;
                        break;
                    }
                }
            }
            if (p != null) {
                VisLabel nLbl = new VisLabel(p.name);
                nLbl.setColor(getPlayerRoleColor(state, p));
                nLbl.setFontScale(0.88f);
                shieldRow.add(nLbl).padRight(4f);

                String rest = language == Language.EN
                        ? "receives a shield at Free Parking!"
                        : "получает щит на Бесплатной стоянке!";
                VisLabel rLbl = new VisLabel(rest);
                rLbl.setColor(BATTLE_TEXT_COLOR);
                rLbl.setFontScale(0.88f);
                shieldRow.add(rLbl).padRight(8f);
            } else {
                VisLabel sLbl = new VisLabel(language == Language.EN
                        ? text.replace("получает щит на Бесплатной стоянке!", "receives a shield at Free Parking!")
                        : text);
                sLbl.setColor(BATTLE_TEXT_COLOR);
                sLbl.setFontScale(0.88f);
                sLbl.setWrap(true);
                sLbl.setAlignment(Align.center);
                shieldRow.add(sLbl).width(maxContentWidth - 30f).padRight(8f);
            }
            if (shieldTexture != null) {
                Image sIcon = new Image(new TextureRegionDrawable(new TextureRegion(shieldTexture)));
                sIcon.setScaling(Scaling.fit);
                shieldRow.add(sIcon).size(20f, 20f);
            }
            turnModalContentTable.add(shieldRow).width(maxContentWidth).center().padBottom(6f).row();
            return;
        }

        // 2. Donation notification
        Matcher mExtract = DONATION_EXTRACT_RU.matcher(text);
        if (mExtract.matches()) {
            String payerName = mExtract.group(1);
            String fee = mExtract.group(2);
            String ownerName = mExtract.group(3);
            String cellName = mExtract.group(4);

            Player payer = findPlayerByName(state, payerName);
            Player owner = findPlayerByName(state, ownerName);

            Table row1 = new Table();
            row1.center();

            VisLabel pref = new VisLabel(language == Language.EN ? "Player " : "Игрок ");
            pref.setColor(BATTLE_TEXT_COLOR);
            pref.setFontScale(0.85f);
            row1.add(pref);

            VisLabel payerLbl = new VisLabel(payerName);
            payerLbl.setColor(getPlayerRoleColor(state, payer));
            payerLbl.setFontScale(0.85f);
            row1.add(payerLbl);

            VisLabel donLbl = new VisLabel(language == Language.EN ? " donated " + fee : " задонатил " + fee);
            donLbl.setColor(BATTLE_TEXT_COLOR);
            donLbl.setFontScale(0.85f);
            row1.add(donLbl);

            if (moneyTexture != null) {
                Image coin = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
                coin.setScaling(Scaling.fit);
                row1.add(coin).size(18f, 18f).padLeft(4f).padRight(4f).padBottom(2f);
            }

            VisLabel toLbl = new VisLabel(language == Language.EN ? "to " : "игроку ");
            toLbl.setColor(BATTLE_TEXT_COLOR);
            toLbl.setFontScale(0.85f);
            row1.add(toLbl);

            VisLabel ownerLbl = new VisLabel(ownerName);
            ownerLbl.setColor(getPlayerRoleColor(state, owner));
            ownerLbl.setFontScale(0.85f);
            row1.add(ownerLbl);

            turnModalContentTable.add(row1).center().padBottom(6f).row();

            VisLabel row2 = new VisLabel(language == Language.EN ? ("after landing on " + cellName) : (", попав на клетку " + cellName));
            row2.setColor(BATTLE_TEXT_COLOR);
            row2.setFontScale(0.85f);
            row2.setWrap(true);
            row2.setAlignment(Align.center);
            turnModalContentTable.add(row2).width(maxContentWidth).center().padBottom(6f).row();
            return;
        }

        // 3. Start Notification (Screenshot 2)
        boolean isStartToast = text.contains("через Старт") || text.contains("passed Start")
                || text.contains("прошел(ла) Старт") || text.contains("прошёл(ла) Старт");
        if (isStartToast) {
            Player found = null;
            if (state != null && state.players != null) {
                for (Player pl : state.players) {
                    if (text.startsWith(pl.name)) {
                        found = pl;
                        break;
                    }
                }
            }
            String pName = found != null ? found.name : "Игрок";
            Color roleColor = getPlayerRoleColor(state, found);

            Matcher mDigits = BONUS_AMOUNT_PATTERN.matcher(text);
            String bonusStr = mDigits.find() ? mDigits.group(1) : "200";

            Table row1 = new Table();
            row1.center();
            VisLabel nLbl = new VisLabel(pName);
            nLbl.setColor(roleColor);
            nLbl.setFontScale(0.90f);
            row1.add(nLbl);

            VisLabel restLbl = new VisLabel(language == Language.RU ? " прошёл(ла) Старт" : " passed Start");
            restLbl.setColor(BATTLE_TEXT_COLOR);
            restLbl.setFontScale(0.90f);
            row1.add(restLbl);
            turnModalContentTable.add(row1).center().padBottom(6f).row();

            Table row2 = new Table();
            row2.center();
            VisLabel recvLbl = new VisLabel(language == Language.RU ? ("и получает " + bonusStr) : ("and receives " + bonusStr));
            recvLbl.setColor(BATTLE_TEXT_COLOR);
            recvLbl.setFontScale(0.90f);
            row2.add(recvLbl);

            if (moneyTexture != null) {
                Image coin = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
                coin.setScaling(Scaling.fit);
                row2.add(coin).size(18f, 18f).padLeft(4f).padRight(4f).padBottom(2f);
            }

            VisLabel exclLbl = new VisLabel("!");
            exclLbl.setColor(BATTLE_TEXT_COLOR);
            exclLbl.setFontScale(0.90f);
            row2.add(exclLbl);
            turnModalContentTable.add(row2).center().padBottom(6f).row();
            return;
        }

        // 4. Event Card notification (Screenshot 4)
        int sepIndex = text.indexOf(" - ");
        if (sepIndex > 0) {
            String titlePart = text.substring(0, sepIndex).trim();
            String descPart = text.substring(sepIndex + 3).trim();

            Table titleRow = new Table();
            titleRow.center();
            int colIndex = titlePart.indexOf(":");
            if (colIndex > 0) {
                String pName = titlePart.substring(0, colIndex).trim();
                String cardTitle = titlePart.substring(colIndex + 1).trim();
                Player p = findPlayerByName(state, pName);
                if (pName.length() + cardTitle.length() >= 22) {
                    VisLabel nLbl = new VisLabel(pName);
                    nLbl.setColor(getPlayerRoleColor(state, p));
                    nLbl.setFontScale(0.90f);
                    titleRow.add(nLbl).center().row();

                    VisLabel tLbl = new VisLabel(cardTitle);
                    tLbl.setColor(BATTLE_TEXT_COLOR);
                    tLbl.setFontScale(0.90f);
                    tLbl.setWrap(true);
                    tLbl.setAlignment(Align.center);
                    titleRow.add(tLbl).width(maxContentWidth).center();
                } else {
                    VisLabel nLbl = new VisLabel(pName);
                    nLbl.setColor(getPlayerRoleColor(state, p));
                    nLbl.setFontScale(0.90f);
                    titleRow.add(nLbl);

                    VisLabel tLbl = new VisLabel(": " + cardTitle);
                    tLbl.setColor(BATTLE_TEXT_COLOR);
                    tLbl.setFontScale(0.90f);
                    titleRow.add(tLbl);
                }
            } else {
                VisLabel tLbl = new VisLabel(titlePart);
                tLbl.setColor(BATTLE_TEXT_COLOR);
                tLbl.setFontScale(0.90f);
                tLbl.setWrap(true);
                tLbl.setAlignment(Align.center);
                titleRow.add(tLbl).width(maxContentWidth);
            }
            turnModalContentTable.add(titleRow).width(maxContentWidth).center().padBottom(6f).row();

            Matcher mDesc = COINS_AMOUNT_PATTERN.matcher(descPart);
            if (mDesc.matches()) {
                String dPref = mDesc.group(1).trim();
                String dAmt = mDesc.group(2).trim();
                String dSuf = mDesc.group(3).trim();

                VisLabel descLbl = new VisLabel(dPref);
                descLbl.setColor(BATTLE_TEXT_COLOR);
                descLbl.setFontScale(0.85f);
                descLbl.setWrap(true);
                descLbl.setAlignment(Align.center);
                turnModalContentTable.add(descLbl).width(maxContentWidth).center().padBottom(6f).row();

                Table amtRow = new Table();
                amtRow.center();
                VisLabel amtLbl = new VisLabel(dAmt);
                amtLbl.setColor(BATTLE_TEXT_COLOR);
                amtLbl.setFontScale(0.85f);
                amtRow.add(amtLbl);

                if (moneyTexture != null) {
                    Image coin = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
                    coin.setScaling(Scaling.fit);
                    amtRow.add(coin).size(18f, 18f).padLeft(4f).padRight(4f).padBottom(2f);
                }
                if (!dSuf.isBlank() && !dSuf.equals(".") && !dSuf.equals("!")) {
                    String cleanSuf = (dSuf.startsWith(",") || dSuf.startsWith(".")) ? dSuf.substring(1).trim() : dSuf.trim();
                    if (!cleanSuf.isBlank()) {
                        VisLabel sufLbl = new VisLabel(cleanSuf);
                        sufLbl.setColor(BATTLE_TEXT_COLOR);
                        sufLbl.setFontScale(0.85f);
                        amtRow.add(sufLbl);
                    }
                }
                turnModalContentTable.add(amtRow).center().padBottom(6f).row();
                return;
            } else {
                VisLabel descLbl = new VisLabel(descPart);
                descLbl.setColor(BATTLE_TEXT_COLOR);
                descLbl.setFontScale(0.85f);
                descLbl.setWrap(true);
                descLbl.setAlignment(Align.center);
                turnModalContentTable.add(descLbl).width(maxContentWidth).center().padBottom(6f).row();
                return;
            }
        }

        // 5. Generic coins amount pattern (without " - ")
        Matcher mCoins = COINS_AMOUNT_PATTERN.matcher(text);
        if (mCoins.matches()) {
            String prefix = mCoins.group(1).trim();
            String amount = mCoins.group(2).trim();
            String suffix = mCoins.group(3).trim();

            Table pRow = new Table();
            pRow.center();
            Player found = null;
            if (state != null && state.players != null) {
                for (Player pl : state.players) {
                    if (prefix.startsWith(pl.name)) {
                        found = pl;
                        break;
                    }
                }
            }
            if (found != null) {
                VisLabel nLbl = new VisLabel(found.name);
                nLbl.setColor(getPlayerRoleColor(state, found));
                nLbl.setFontScale(0.88f);
                pRow.add(nLbl);

                VisLabel rLbl = new VisLabel(prefix.substring(found.name.length()));
                rLbl.setColor(BATTLE_TEXT_COLOR);
                rLbl.setFontScale(0.88f);
                pRow.add(rLbl);
            } else {
                VisLabel prefLbl = new VisLabel(prefix);
                prefLbl.setColor(BATTLE_TEXT_COLOR);
                prefLbl.setFontScale(0.88f);
                prefLbl.setWrap(true);
                prefLbl.setAlignment(Align.center);
                pRow.add(prefLbl).width(maxContentWidth);
            }
            turnModalContentTable.add(pRow).width(maxContentWidth).center().padBottom(6f).row();

            Table amtRow = new Table();
            amtRow.center();
            VisLabel amtLbl = new VisLabel(amount);
            amtLbl.setColor(BATTLE_TEXT_COLOR);
            amtLbl.setFontScale(0.88f);
            amtRow.add(amtLbl);

            if (moneyTexture != null) {
                Image coin = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
                coin.setScaling(Scaling.fit);
                amtRow.add(coin).size(18f, 18f).padLeft(4f).padRight(4f).padBottom(2f);
            }
            turnModalContentTable.add(amtRow).center().padBottom(6f).row();

            if (!suffix.isBlank() && !suffix.equals("!") && !suffix.equals(".")) {
                VisLabel sufLbl = new VisLabel(suffix.trim());
                sufLbl.setColor(BATTLE_TEXT_COLOR);
                sufLbl.setFontScale(0.85f);
                sufLbl.setWrap(true);
                sufLbl.setAlignment(Align.center);
                turnModalContentTable.add(sufLbl).width(maxContentWidth).center().padBottom(6f).row();
            }
            return;
        }

        // 6. Cell Purchase notification (Screenshot 1) and other fallback messages
        Player found = null;
        if (state != null && state.players != null) {
            for (Player pl : state.players) {
                if (text.startsWith(pl.name)) {
                    found = pl;
                    break;
                }
            }
        }
        if (found != null) {
            String rest = text.substring(found.name.length()).trim();
            if ((found.name.length() + rest.length()) >= 28 || rest.length() > 18) {
                VisLabel nLbl = new VisLabel(found.name);
                nLbl.setColor(getPlayerRoleColor(state, found));
                nLbl.setFontScale(0.88f);
                turnModalContentTable.add(nLbl).center().padBottom(2f).row();

                VisLabel rLbl = new VisLabel(rest);
                rLbl.setColor(BATTLE_TEXT_COLOR);
                rLbl.setFontScale(0.88f);
                rLbl.setWrap(true);
                rLbl.setAlignment(Align.center);
                turnModalContentTable.add(rLbl).width(maxContentWidth).center().padBottom(6f).row();
            } else {
                Table fRow = new Table();
                fRow.center();
                VisLabel nLbl = new VisLabel(found.name);
                nLbl.setColor(getPlayerRoleColor(state, found));
                nLbl.setFontScale(0.88f);
                fRow.add(nLbl).padRight(4f);

                VisLabel rLbl = new VisLabel(rest);
                rLbl.setColor(BATTLE_TEXT_COLOR);
                rLbl.setFontScale(0.88f);
                fRow.add(rLbl);

                turnModalContentTable.add(fRow).width(maxContentWidth).center().padBottom(6f).row();
            }
        } else {
            VisLabel label = new VisLabel(text);
            label.setColor(BATTLE_TEXT_COLOR);
            label.setFontScale(0.85f);
            label.setAlignment(Align.center);
            label.setWrap(true);
            turnModalContentTable.add(label).width(maxContentWidth).center().padBottom(6f).row();
        }
    }

    private void syncPendingActionLayers(GameState state, Player player) {
        removePendingActionLayers();
        if (pendingActionMode == BoardRenderer.PendingActionMode.NONE || state == null || player == null) {
            return;
        }

        Set<Integer> selectable = getPendingActionSelectableCells(state, player);
        if (selectable.isEmpty()) {
            return;
        }

        Rectangle boardBounds = boardRenderer.getBoardBounds();
        pendingActionPlaqueLayer = new Table();
        pendingActionPlaqueLayer.setBounds(boardBounds.x, boardBounds.y, boardBounds.width, boardBounds.height);
        pendingActionPlaqueLayer.setTouchable(Touchable.disabled);
        for (int cellId : selectable) {
            Table plaque = createPendingActionPlaque(cellId, state);
            Rectangle cellBounds = boardRenderer.getCellBounds(cellId);
            Rectangle strip = boardRenderer.getOwnerStripBounds(cellId);
            plaque.pack();

            float plaqueX, plaqueY;
            if (cellId >= 1 && cellId <= 9) { // Нижний ряд: НАД цветной полосой
                plaqueX = cellBounds.x - boardBounds.x + (cellBounds.width - plaque.getWidth()) * 0.5f;
                plaqueY = strip.y - boardBounds.y + strip.height + 2f;
            } else if (cellId >= 21 && cellId <= 29) { // Верхний ряд: ПОД цветной полосой
                plaqueX = cellBounds.x - boardBounds.x + (cellBounds.width - plaque.getWidth()) * 0.5f;
                plaqueY = strip.y - boardBounds.y - plaque.getHeight() - 2f;
            } else if (cellId >= 11 && cellId <= 19) { // Левый ряд: СПРАВА от цветной полосы
                plaqueX = strip.x - boardBounds.x + strip.width + 2f;
                plaqueY = cellBounds.y - boardBounds.y + (cellBounds.height - plaque.getHeight()) * 0.5f;
            } else if (cellId >= 31 && cellId <= 39) { // Правый ряд: СЛЕВА от цветной полосы
                plaqueX = strip.x - boardBounds.x - plaque.getWidth() - 2f;
                plaqueY = cellBounds.y - boardBounds.y + (cellBounds.height - plaque.getHeight()) * 0.5f;
            } else {
                plaqueX = cellBounds.x - boardBounds.x + (cellBounds.width - plaque.getWidth()) * 0.5f;
                plaqueY = cellBounds.y - boardBounds.y + 2f;
            }

            plaque.setPosition(plaqueX, plaqueY);
            plaque.setTouchable(Touchable.disabled);
            pendingActionPlaqueLayer.addActor(plaque);
        }
        stage.addActor(pendingActionPlaqueLayer);

        Table layer = new Table();
        layer.setBounds(boardBounds.x, boardBounds.y, boardBounds.width, boardBounds.height);
        layer.setTouchable(Touchable.enabled);
        layer.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                handlePendingActionClick(layer.getX() + x, layer.getY() + y);
            }
        });
        stage.getRoot().addActorAfter(pendingActionPlaqueLayer, layer);
        pendingActionTouchLayer = layer;
    }

    private Set<Integer> getPendingActionSelectableCells(GameState state, Player player) {
        Set<Integer> selectable = new HashSet<>();
        if (state == null || player == null) {
            return selectable;
        }
        for (int cellId : player.ownedCells) {
            boolean mortgaged = state.cellMortgaged.getOrDefault(cellId, false);
            int houses = state.cellHouses.getOrDefault(cellId, 0);
            BoardCell cell = cellId >= 0 && cellId < boardCells.size() ? boardCells.get(cellId) : null;
            if (pendingActionMode == BoardRenderer.PendingActionMode.MORTGAGE) {
                if ((!mortgaged && houses == 0) || mortgaged) {
                    selectable.add(cellId);
                }
            } else if (pendingActionMode == BoardRenderer.PendingActionMode.UNMORTGAGE && mortgaged) {
                selectable.add(cellId);
            } else if (pendingActionMode == BoardRenderer.PendingActionMode.BUILD_SELL
                    && cell != null && cell.group != null && isCellInMonopoly(cell, player) && houses < 5) {
                selectable.add(cellId);
            } else if (pendingActionMode == BoardRenderer.PendingActionMode.SELL_HOUSE
                    && houses > 0 && cell != null && cell.group != null && isCellInMonopoly(cell, player)) {
                selectable.add(cellId);
            }
        }
        return selectable;
    }

    private Table createPendingActionPlaque(int cellId, GameState state) {
        BoardCell cell = boardCells.get(cellId);
        int houses = state.cellHouses.getOrDefault(cellId, 0);
        boolean mortgaged = state != null && state.cellMortgaged.getOrDefault(cellId, false);
        String amount;
        boolean canAfford = true;
        Player localPlayer = state != null ? state.getPlayerById(game.getClient().getLocalPlayerId()) : null;
        int playerMoney = localPlayer != null ? localPlayer.money : 0;
        Role role = Role.of(state != null && state.playerRoles != null && localPlayer != null ? state.playerRoles.get(localPlayer.id) : null);

        boolean isLong = false;
        if (pendingActionMode == BoardRenderer.PendingActionMode.MORTGAGE) {
            if (!mortgaged) {
                amount = "+ " + MonopolyUtils.getMortgageValue(cell);
            } else {
                int unmortgageCost = MonopolyUtils.getUnmortgagePrice(cell);
                amount = "- " + unmortgageCost;
                canAfford = playerMoney >= unmortgageCost;
            }
        } else if (pendingActionMode == BoardRenderer.PendingActionMode.UNMORTGAGE) {
            int unmortgageCost = MonopolyUtils.getUnmortgagePrice(cell);
            amount = "- " + unmortgageCost;
            canAfford = playerMoney >= unmortgageCost;
        } else if (pendingActionMode == BoardRenderer.PendingActionMode.SELL_HOUSE) {
            int housePrice = MonopolyUtils.getHousePrice(cell, role);
            amount = "+ " + (housePrice / 2);
            isLong = true;
        } else {
            // BUILD_SELL — показываем стоимость покупки
            int houseCost = MonopolyUtils.getHousePrice(cell, role);
            amount = "- " + houseCost;
            canAfford = playerMoney >= houseCost;
            isLong = true;
        }

        Table plaque = new Table();
        float plaqueHeight = 19f;
        NinePatchDrawable bg = isLong
                ? createPillDrawable(longRectForMoneyTexture != null ? longRectForMoneyTexture : rectForMoneyTexture, plaqueHeight, 266, 266)
                : createPillDrawable(rectForMoneyTexture, plaqueHeight, 224, 224);
        if (bg != null) {
            plaque.setBackground(bg);
        }
        plaque.pad(2f, 8f, 2f, 8f);

        if (!canAfford) {
            plaque.getColor().a = 0.55f;
        }

        if (isLong && houseIconTexture != null) {
            Image houseIcon = new Image(new TextureRegionDrawable(new TextureRegion(houseIconTexture)));
            houseIcon.setScaling(Scaling.fit);
            plaque.add(houseIcon).size(12f, 12f).padRight(3f);
        }

        VisLabel amountLabel = new VisLabel(amount);
        amountLabel.setColor(canAfford ? ACCENT_GOLD : new Color(0.9f, 0.4f, 0.4f, 1f));
        amountLabel.setFontScale(0.48f);
        plaque.add(amountLabel).padRight(3f);

        Image coinIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        coinIcon.setScaling(Scaling.fit);
        plaque.add(coinIcon).size(12f, 12f);

        return plaque;
    }

    private void clearPendingActionSelection() {
        pendingActionMode = BoardRenderer.PendingActionMode.NONE;
        if (boardRenderer != null) {
            boardRenderer.setPendingActionMode(BoardRenderer.PendingActionMode.NONE, -1);
        }
        removePendingActionLayers();
        updateOverlayDimming();
        GameState state = game.getLatestGameState();
        if (state != null) {
            int localId = game.getClient().getLocalPlayerId();
            Player player = state.getPlayerById(localId);
            boolean myTurn = state.currentPlayerIndex >= 0 && state.currentPlayerIndex < state.players.size()
                    && state.players.get(state.currentPlayerIndex).id == localId;
            rebuildOwnedCells(state, player, myTurn, state.currentPhase);
            refreshActions(state);
        }
    }

    private void updateOverlayDimming() {
        boolean inOverlay = (pendingActionMode != BoardRenderer.PendingActionMode.NONE);
        Color tint = inOverlay ? new Color(0.4f, 0.4f, 0.4f, 1f) : Color.WHITE;
        Touchable touch = inOverlay ? Touchable.disabled : Touchable.enabled;
        if (leftColumn != null) {
            leftColumn.setColor(tint);
            leftColumn.setTouchable(touch);
        }
        if (diceOverlay != null) {
            diceOverlay.setColor(tint);
            diceOverlay.setTouchable(touch);
        }
        if (actionRow != null) {
            actionRow.setColor(tint);
            actionRow.setTouchable(touch);
        }
        if (memesPanel != null) {
            memesPanel.setColor(tint);
            memesPanel.setTouchable(touch);
        }
        if (ownedScroll != null) {
            ownedScroll.setColor(tint);
            ownedScroll.setTouchable(Touchable.enabled);
        }
    }

    private void removePendingActionLayers() {
        if (pendingActionTouchLayer != null) {
            pendingActionTouchLayer.remove();
            pendingActionTouchLayer = null;
        }
        if (pendingActionPlaqueLayer != null) {
            pendingActionPlaqueLayer.remove();
            pendingActionPlaqueLayer = null;
        }
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

    private Table playersPanel;
    private Table playersPanelRef;
    private final Vector2 tmpStageCoord = new Vector2();
    private boolean pendingBattleOverlay = false;
    private final Map<Integer, Integer> displayedMoney = new HashMap<>();
    private final Map<Integer, VisLabel> moneyLabelRefs = new HashMap<>();
    private final Map<Integer, Table> moneyCellRefs = new HashMap<>();
    private final Map<Integer, Integer> lastMoney = new HashMap<>();
    /** Целевые балансы, ожидающие флаша — анимация дельты стартует после окончания движения фишки. */
    private final Map<Integer, Integer> pendingMoneyDeltas = new HashMap<>();

    private enum MoneyDeltaPhase {
        SLIDE,
        FADE_OUT,
        COUNT
    }

    private static class MoneyDeltaAnim {
        int playerId;
        int from;
        int to;
        MoneyDeltaPhase phase = MoneyDeltaPhase.SLIDE;
        float timer = 0f;
        int currentDisplay;
        Group actor;
    }

    private final Map<Integer, MoneyDeltaAnim> activeMoneyAnims = new HashMap<>();
    private float localBattleTimer = 0f;
    private GameState.BattlePhase lastBattleSubPhase = null;
    private int lastDisplayedBattleTimer = -1;

    private Map<Integer, Actor> nameLabelRefs = new HashMap<>();

    private void startMoneyDeltaAnimation(int playerId, int targetMoney) {
        MoneyDeltaAnim existing = activeMoneyAnims.remove(playerId);
        if (existing != null) {
            if (existing.actor != null) {
                existing.actor.remove();
            }
            displayedMoney.put(playerId, existing.to);
            VisLabel lbl = moneyLabelRefs.get(playerId);
            if (lbl != null) {
                lbl.setText(String.valueOf(existing.to));
            }
        }

        int from = displayedMoney.getOrDefault(playerId, lastMoney.getOrDefault(playerId, targetMoney));
        int to = targetMoney;
        lastMoney.put(playerId, to);
        if (from == to) {
            displayedMoney.put(playerId, to);
            VisLabel lbl = moneyLabelRefs.get(playerId);
            if (lbl != null) {
                lbl.setText(String.valueOf(to));
            }
            return;
        }

        int delta = to - from;
        Group indicatorGroup = createMoneyDeltaIndicator(delta);
        stage.addActor(indicatorGroup);
        indicatorGroup.toFront();

        MoneyDeltaAnim anim = new MoneyDeltaAnim();
        anim.playerId = playerId;
        anim.from = from;
        anim.to = to;
        anim.phase = MoneyDeltaPhase.SLIDE;
        anim.timer = 0f;
        anim.currentDisplay = from;
        anim.actor = indicatorGroup;
        activeMoneyAnims.put(playerId, anim);
    }

    private Group createMoneyDeltaIndicator(int delta) {
        float indicatorHeight = 28f;
        NinePatchDrawable indicatorBg = createPillDrawable(rectForMoneyTexture, indicatorHeight);

        Table indicatorTable = new Table();
        indicatorTable.setTouchable(Touchable.disabled);
        if (indicatorBg != null) {
            indicatorTable.setBackground(indicatorBg);
        }
        indicatorTable.pad(3f, 12f, 3f, 12f);

        String sign = delta > 0 ? "+" : "-";
        VisLabel deltaLabel = new VisLabel(sign + Math.abs(delta));
        deltaLabel.setFontScale(0.65f);
        deltaLabel.setColor(delta > 0 ? new Color(0.20f, 0.70f, 0.30f, 1f) : new Color(0.85f, 0.20f, 0.20f, 1f));

        Image coinIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        coinIcon.setScaling(Scaling.fit);

        indicatorTable.add(deltaLabel).padRight(4f);
        indicatorTable.add(coinIcon).size(18f, 18f);
        indicatorTable.pack();
        indicatorTable.setHeight(indicatorHeight);

        return indicatorTable;
    }

    private void updateMoneyDeltaAnimation(float delta) {
        if (activeMoneyAnims.isEmpty())
            return;

        List<Integer> finished = new ArrayList<>();
        float panelRightX = WORLD_WIDTH * 0.25f;
        if (playersPanelRef != null && playersPanelRef.getStage() != null) {
            tmpStageCoord.set(playersPanelRef.getWidth(), 0f);
            panelRightX = playersPanelRef.localToStageCoordinates(tmpStageCoord).x;
        }

        for (MoneyDeltaAnim anim : activeMoneyAnims.values()) {
            Table cell = moneyCellRefs.get(anim.playerId);
            if (cell == null || cell.getStage() == null) {
                if (anim.actor != null) {
                    anim.actor.remove();
                }
                displayedMoney.put(anim.playerId, anim.to);
                VisLabel lbl = moneyLabelRefs.get(anim.playerId);
                if (lbl != null) {
                    lbl.setText(String.valueOf(anim.to));
                }
                finished.add(anim.playerId);
                continue;
            }

            tmpStageCoord.set(0f, 0f);
            cell.localToStageCoordinates(tmpStageCoord);
            float moneyCellStageX = tmpStageCoord.x;
            float moneyCellStageY = tmpStageCoord.y;

            float actorW = anim.actor.getWidth();
            float actorH = anim.actor.getHeight();
            float targetY = moneyCellStageY + (cell.getHeight() - actorH) / 2f;
            anim.actor.setY(targetY);

            float spawnX = panelRightX + 16f;
            float stopX = moneyCellStageX + cell.getWidth() + 10f;

            if (anim.phase == MoneyDeltaPhase.SLIDE) {
                anim.timer += delta;
                float progress = Math.min(1f, anim.timer / 1.10f);
                float alpha = com.badlogic.gdx.math.Interpolation.pow2Out.apply(progress);
                anim.actor.setX(spawnX + (stopX - spawnX) * alpha);
                anim.actor.getColor().a = 1f;

                if (anim.timer >= 1.10f) {
                    anim.actor.setX(stopX);
                    anim.phase = MoneyDeltaPhase.FADE_OUT;
                    anim.timer = 0f;
                }
            } else if (anim.phase == MoneyDeltaPhase.FADE_OUT) {
                anim.timer += delta;
                float fadeProgress = Math.min(1f, anim.timer / 0.30f);
                anim.actor.getColor().a = Math.max(0f, 1f - fadeProgress);
                anim.actor.setX(stopX);

                if (anim.timer >= 0.30f) {
                    anim.actor.remove();
                    anim.phase = MoneyDeltaPhase.COUNT;
                    anim.timer = 0f;
                    anim.currentDisplay = anim.from;
                }
            } else if (anim.phase == MoneyDeltaPhase.COUNT) {
                int diff = anim.to - anim.currentDisplay;
                if (diff == 0) {
                    finished.add(anim.playerId);
                } else {
                    int sign = diff > 0 ? 1 : -1;
                    int step = sign * Math.max(1, (int) Math.ceil(Math.abs(anim.to - anim.from) * delta / 1.00f));
                    if (Math.abs(step) >= Math.abs(diff)) {
                        anim.currentDisplay = anim.to;
                    } else {
                        anim.currentDisplay += step;
                    }
                    displayedMoney.put(anim.playerId, anim.currentDisplay);
                    VisLabel lbl = moneyLabelRefs.get(anim.playerId);
                    if (lbl != null) {
                        lbl.setText(String.valueOf(anim.currentDisplay));
                    }
                    if (anim.currentDisplay == anim.to) {
                        finished.add(anim.playerId);
                    }
                }
            }
        }

        for (int pid : finished) {
            MoneyDeltaAnim st = activeMoneyAnims.remove(pid);
            if (st != null) {
                if (st.actor != null) {
                    st.actor.remove();
                }
                displayedMoney.put(pid, st.to);
                VisLabel lbl = moneyLabelRefs.get(pid);
                if (lbl != null) {
                    lbl.setText(String.valueOf(st.to));
                }
            }
        }
    }

    private float battleOverlayDelayTimer = 0f;
    private boolean battleDelayTriggered = false;
    private Table gameOverModal = null;

    private void showGameOverModal(GameState state) {
        if (gameOverModal != null)
            return;
        if (diceOverlay != null)
            diceOverlay.setVisible(false);
        setButtonsEnabled(false, false, false, false, false);

        int winnerId = state != null ? state.winnerId : -1;
        Player winner = state != null ? state.getPlayerById(winnerId) : null;
        if (winner == null && state != null && state.players != null) {
            for (Player p : state.players) {
                if (p != null && !p.isBankrupt) {
                    winner = p;
                    break;
                }
            }
        }
        String winnerName = winner != null && winner.name != null && !winner.name.isBlank()
                ? winner.name
                : (state != null && state.players != null && !state.players.isEmpty() ? state.players.get(0).name
                        : "Игрок");

        gameOverModal = new Table();
        gameOverModal.setBackground(new TextureRegionDrawable(new TextureRegion(gameOverlayWindowTexture)));
        gameOverModal.pad(30f);

        VisLabel titleLbl = new VisLabel("ИГРА ОКОНЧЕНА");
        titleLbl.setFontScale(1.2f);
        titleLbl.setColor(ACCENT_GOLD);

        VisLabel winnerLbl = new VisLabel("ПОБЕДИТЕЛЬ: " + winnerName.toUpperCase());
        winnerLbl.setFontScale(0.9f);
        winnerLbl.setColor(Color.WHITE);

        Actor exitActor;
        if (exitToMenuButtonTexture != null) {
            ImageButton exitBtn = createActionButton(exitToMenuButtonTexture);
            exitBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new MainMenuScreen(game));
                }
            });
            exitActor = exitBtn;
        } else {
            VisTextButton exitBtn = new VisTextButton("ВЫЙТИ В МЕНЮ");
            exitBtn.addListener(new ChangeListener() {

                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    game.setScreen(new MainMenuScreen(game));
                }
            });
            exitActor = exitBtn;
        }

        gameOverModal.add(titleLbl).padBottom(15f).row();
        gameOverModal.add(winnerLbl).padBottom(25f).row();
        gameOverModal.add(exitActor).size(200f, 65f).row();

        gameOverModal.setBounds((WORLD_WIDTH - 520f) / 2f, (WORLD_HEIGHT - 240f) / 2f, 520f, 240f);
        stage.addActor(gameOverModal);
    }

    private boolean wasInBattle = false;
    private String lastCanRollAuditSig = "";

    private void refreshBattleOverlay(GameState state) {
        boolean inBattle = state != null && state.currentPhase == GameState.GamePhase.MEME_BATTLE;
        if (!inBattle) {
            if (wasInBattle) {
                wasInBattle = false;
                battleDelayTriggered = false;
                pendingBattleOverlay = false;
                battleOverlayDelayTimer = 0f;
                battleOverlay.clearChildren();
                battleOverlay.setVisible(false);
                battleOverlay.setTouchable(Touchable.disabled);
                if (boardRenderer != null) {
                    boardRenderer.setBattleMode(false);
                }
                lastBattleSignature = "";
                selectedBattleMemeId = -1;
                battleTimerLabel = null;
                cachedBattleResultLog = null;
                lastBattleSubPhase = null;
                lastDisplayedBattleTimer = -1;
                lastAnimatedBattlePhase = null;
                battleCardHolders.clear();

                if (actionRow != null) {
                    actionRow.setVisible(true);
                    actionRow.setTouchable(Touchable.enabled);
                }
                if (diceOverlay != null) {
                    diceOverlay.setVisible(true);
                }
                if (state != null) {
                    Player current = state.getCurrentPlayer();
                    int localId = game.getClient().getLocalPlayerId();
                    boolean myTurn = current != null && (current.id == localId || (localId <= 0 && game.isHost()));
                    boolean moverAnimating = current != null && boardRenderer != null
                            && boardRenderer.isAnimating(current.id);
                    BoardCell currentCell = current != null && boardCells != null && current.position >= 0
                            && current.position < boardCells.size() ? boardCells.get(current.position) : null;
                    lastActionRowSig = "";
                    refreshActions(state, myTurn, currentCell, moverAnimating);
                }
            }
            return;
        }
        wasInBattle = true;

        boolean isRollingOrMoving = diceAnimPhase != DiceAnimPhase.IDLE || hasPendingMove
                || (boardRenderer != null && boardRenderer.hasActiveAnimations());

        if (!battleDelayTriggered) {
            battleDelayTriggered = true;
            battleOverlayDelayTimer = 0.55f;
            pendingBattleOverlay = true;
        }

        if (isRollingOrMoving || battleOverlayDelayTimer > 0f) {
            if (diceOverlay != null)
                diceOverlay.setVisible(false);
            battleOverlay.setVisible(false);
            battleOverlay.setTouchable(Touchable.disabled);
            if (boardRenderer != null) {
                boardRenderer.setBattleMode(false);
            }
            return;
        }

        boardRenderer.setBattleMode(true);
        if (diceOverlay != null)
            diceOverlay.setVisible(false);

        if (state.battlePhase == GameState.BattlePhase.RESULTS) {
            currentBattleTopicInput = "";
            battleHandSnapshot = null;
        }

        battleOverlay.setVisible(true);
        battleOverlay.setTouchable(Touchable.childrenOnly);
        battleOverlay.toFront();

        int localId = game.getClient().getLocalPlayerId();
        String signature = buildBattleSignature(state, localId);
        if (!signature.equals(lastBattleSignature)) {
            lastBattleSignature = signature;
            selectedBattleMemeId = -1;
            battleTimerLabel = null;
            rebuildBattleContent(state);
        }

        float delta = Gdx.graphics.getDeltaTime();
        if (state.battlePhase != lastBattleSubPhase) {
            localBattleTimer = state.battleTimerSeconds;
            lastBattleSubPhase = state.battlePhase;
            lastDisplayedBattleTimer = -1;
        } else {
            localBattleTimer -= delta;
            if (Math.abs(localBattleTimer - state.battleTimerSeconds) > 1.2f) {
                localBattleTimer = state.battleTimerSeconds;
            }
        }

        int displaySeconds = Math.max(0, (int) Math.ceil(localBattleTimer));
        if (battleTimerLabel != null && displaySeconds != lastDisplayedBattleTimer) {
            lastDisplayedBattleTimer = displaySeconds;
            battleTimerLabel.setText(String.valueOf(displaySeconds));
        }
    }

    private Table createBattleTimerWidget(float size) {
        Stack timerStack = new Stack();
        int radius = Math.round(size / 2f);
        // Outer dark blue circle border
        Image border = new Image(new TextureRegionDrawable(new TextureRegion(UiShapes.circleSmooth(radius, Color.valueOf("000A3E")))));
        border.setScaling(Scaling.fit);
        timerStack.add(border);

        // Inner white circle
        Image fill = new Image(new TextureRegionDrawable(new TextureRegion(UiShapes.circleSmooth(Math.max(1, radius - 2), Color.WHITE))));
        fill.setScaling(Scaling.fit);
        Table fillHolder = new Table();
        fillHolder.center();
        fillHolder.add(fill).size(size - 4f, size - 4f);
        timerStack.add(fillHolder);

        // Label
        if (battleTimerLabel == null) {
            battleTimerLabel = new VisLabel("");
        }
        battleTimerLabel.setColor(Color.valueOf("000A3E"));
        battleTimerLabel.setFontScale(size >= 50f ? 1.0f : 0.85f);
        Table labelHolder = new Table();
        labelHolder.center();
        labelHolder.add(battleTimerLabel).center();
        timerStack.add(labelHolder);

        Table widget = new Table();
        widget.center();
        widget.add(timerStack).size(size, size);
        return widget;
    }

    private void rebuildBattleContent(GameState state) {
        battleOverlay.clearChildren();
        battleCardHolders.clear();
        battleTimerLabel = new VisLabel("");
        battleTimerLabel.setColor(Color.valueOf("000A3E"));
        battleTimerLabel.setFontScale(1.0f);
        int localId = game.getClient().getLocalPlayerId();
        boolean isOwner = state.battleOwnerId == localId;
        if (state.battlePhase != GameState.BattlePhase.INVITE) {
            battleInviteAnswered = false;
        }
        Table panel = new Table();
        panel.center();
        panel.setTouchable(Touchable.enabled);
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
                panel.add(buildResultsPanel(state)).center().expand();
                break;
            default:
                break;
        }

        // Pop animation: fire only when battle phase changes, not on every UI update
        if (state.battlePhase != lastAnimatedBattlePhase) {
            if (state.battlePhase == GameState.BattlePhase.VOTING) {
                for (int i = 0; i < battleCardHolders.size(); i++) {
                    Table h = battleCardHolders.get(i);
                    h.getColor().a = 0f;
                    h.setScale(1f);
                    h.addAction(Actions.sequence(
                            Actions.alpha(0f),
                            Actions.delay(i * 2.0f),
                            Actions.fadeIn(0.5f)));
                }
                lastAnimatedBattlePhase = state.battlePhase;
            } else if (state.battlePhase == GameState.BattlePhase.COLLECTING_MEMES) {
                float delayStep = 0.18f;
                for (int i = 0; i < battleCardHolders.size(); i++) {
                    Table h = battleCardHolders.get(i);
                    h.setScale(0f);
                    h.addAction(Actions.sequence(
                            Actions.delay(i * delayStep),
                            Actions.scaleTo(1f, 1f, 0.30f, com.badlogic.gdx.math.Interpolation.swingOut)));
                }
                lastAnimatedBattlePhase = state.battlePhase;
            } else if (state.battlePhase == GameState.BattlePhase.RESULTS) {
                lastAnimatedBattlePhase = state.battlePhase;
            }
        }

        Stack stack = new Stack();
        Table centerHolder = new Table();
        centerHolder.center();
        centerHolder.setTouchable(Touchable.childrenOnly);
        centerHolder.add(panel).width(860f).height(920f);
        stack.add(centerHolder);
        boolean isOwnerSetup = state.battlePhase == GameState.BattlePhase.BATTLE_SETUP && isOwner;
        if (!isOwnerSetup) {
            Table timerHolder = new Table();
            timerHolder.top().left();
            timerHolder.setTouchable(Touchable.disabled);
            Table timerWidget = createBattleTimerWidget(54f);
            timerHolder.add(timerWidget).size(54f, 54f).pad(20f);
            stack.add(timerHolder);
        }
        battleOverlay.add(stack).expand().fill();
    }

    private Table buildWaitPanel(String text) {
        Table p = new Table();
        p.setTouchable(Touchable.enabled);
        VisLabel label = new VisLabel(text);
        label.setColor(BATTLE_TEXT_COLOR);
        label.setWrap(false);
        label.setAlignment(com.badlogic.gdx.utils.Align.center);
        label.setFontScale(1.1f);
        p.add(label).expandX().center();
        return p;
    }

    private void loadBattleTopics() {
        battleTopics.clear();
        try {
            com.badlogic.gdx.files.FileHandle fh = Gdx.files.internal("battle_topics.txt");
            if (fh.exists()) {
                String raw = fh.readString("UTF-8");
                String[] lines = raw.split("\\r?\\n");
                for (String line : lines) {
                    String trimmed = line.trim();
                    if (!trimmed.isEmpty()) {
                        battleTopics.add(trimmed);
                    }
                }
            }
        } catch (Exception e) {
            Gdx.app.log("GameScreen", "Failed to load battle_topics.txt: " + e.getMessage());
        }
        if (battleTopics.isEmpty()) {
            battleTopics.add("Когда код скомпилировался с первого раза");
            battleTopics.add("Когда в пятницу в 17:59 прилетает критический баг");
            battleTopics.add("Лицо тимлида, когда джун дропнул прод");
            battleTopics.add("Пытаешься объяснить бабушке, кем ты работаешь");
            battleTopics.add("Когда сказали, что зарплату задержат на пару дней");
        }
    }

    private String getRandomBattleTopic() {
        if (battleTopics.isEmpty()) {
            loadBattleTopics();
        }
        if (battleTopics.size() == 1) {
            return battleTopics.get(0);
        }
        int nextIndex;
        do {
            nextIndex = com.badlogic.gdx.math.MathUtils.random(battleTopics.size() - 1);
        } while (nextIndex == lastBattleTopicIndex && battleTopics.size() > 1);
        lastBattleTopicIndex = nextIndex;
        return battleTopics.get(nextIndex);
    }

    private Table buildStakePanel() {
        Table p = new Table();
        p.center();
        VisLabel title = new VisLabel(t("battle_setup_title"));
        title.setColor(Color.valueOf("000A3E"));
        title.setFontScale(1.02f);
        title.setAlignment(Align.center);
        p.add(title).center().padLeft(28f).padRight(28f).padBottom(14f).row();

        if (currentBattleTopicInput == null || currentBattleTopicInput.trim().isEmpty()) {
            currentBattleTopicInput = getRandomBattleTopic();
        }

        Table topicContainer = new Table();
        NinePatchDrawable topicBg = null;
        if (gameOverlayWindowTexture != null) {
            topicBg = new NinePatchDrawable(new NinePatch(gameOverlayWindowTexture, 0, 0, 0, 0));
        } else if (cardBoardTexture != null) {
            topicBg = new NinePatchDrawable(new NinePatch(cardBoardTexture, 28, 28, 28, 28));
        }
        if (topicBg != null) {
            topicBg.setMinWidth(0f);
            topicBg.setMinHeight(0f);
            topicContainer.setBackground(topicBg);
        }
        topicContainer.pad(12f, 20f, 12f, 20f);

        VisLabel suggestedTopicLabel = new VisLabel(currentBattleTopicInput);
        suggestedTopicLabel.setColor(Color.valueOf("000A3E"));
        suggestedTopicLabel.setFontScale(0.65f);
        suggestedTopicLabel.setAlignment(Align.center);
        suggestedTopicLabel.setWrap(true);
        topicContainer.add(suggestedTopicLabel).grow().center();

        Image changeTopicIcon = new Image(changeTopicBtnTexture != null
                ? new TextureRegionDrawable(new TextureRegion(changeTopicBtnTexture))
                : new TextureRegionDrawable(new TextureRegion(diceFaceATexture)));
        changeTopicIcon.setScaling(Scaling.fit);
        Button changeTopicBtn = new Button(new Button.ButtonStyle());
        changeTopicBtn.add(changeTopicIcon).size(44f, 44f);
        changeTopicBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                currentBattleTopicInput = getRandomBattleTopic();
                suggestedTopicLabel.setText(currentBattleTopicInput);
            }
        });

        Table topicRow = new Table();
        topicRow.center();
        Table timerWidget = createBattleTimerWidget(54f);
        topicRow.add(timerWidget).size(54f, 54f).padRight(12f);
        topicRow.add(topicContainer).size(580f, 130f);
        topicRow.add(changeTopicBtn).size(44f, 44f).padLeft(12f);
        p.add(topicRow).center().padBottom(14f).row();

        VisTextField topicInput = new VisTextField();
        String placeholderText = language == Language.RU
                ? "Введите свою тему или оставьте предложенную..."
                : "Enter your topic or keep the suggested one...";
        topicInput.setMessageText(placeholderText);
        topicInput.setMaxLength(120);
        VisTextField.VisTextFieldStyle style = new VisTextField.VisTextFieldStyle(topicInput.getStyle());
        NinePatchDrawable inputBg = new NinePatchDrawable(new NinePatch(inputMemeBattleTexture, 28, 28, 14, 14));
        inputBg.setMinWidth(0f);
        inputBg.setMinHeight(0f);
        inputBg.setLeftWidth(24f);
        inputBg.setRightWidth(24f);
        inputBg.setTopHeight(10f);
        inputBg.setBottomHeight(10f);
        style.background = inputBg;
        style.backgroundOver = inputBg;
        style.focusedBackground = inputBg;
        style.disabledBackground = inputBg;
        style.focusBorder = null;
        String fontPath = game.getLanguageManager().getLanguage() == Language.RU ? "fonts_ru/Rubik-Bold.ttf"
                : "fonts_en/Rubik-Bold.ttf";
        BitmapFont inputFont = UiFonts.create(fontPath, 20);
        if (inputFont != null) {
            style.font = inputFont;
        }
        style.fontColor = Color.valueOf("000A3E");
        style.messageFont = style.font;
        style.messageFontColor = new Color(0.45f, 0.45f, 0.50f, 0.85f);
        topicInput.setStyle(style);
        topicInput.setText("");

        p.add(topicInput).width(596f).height(56f).center().padBottom(18f).row();

        localStake = 50;
        stakeValueLabel = new VisLabel(String.valueOf(localStake));
        stakeValueLabel.setColor(BATTLE_TEXT_COLOR);
        stakeValueLabel.setFontScale(1.2f);
        Image moneyIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        moneyIcon.setScaling(Scaling.fit);
        Table stakeRow = new Table();
        stakeRow.add(stakeValueLabel).padRight(8f);
        stakeRow.add(moneyIcon).size(28f, 28f);
        p.add(stakeRow).center().padBottom(20f).row();

        ImageButton minusBtn = createActionButton(tenDownTexture);
        attachRepeatingListener(minusBtn, () -> {
            localStake = Math.max(10, localStake - 10);
            stakeValueLabel.setText(String.valueOf(localStake));
        });
        ImageButton plusBtn = createActionButton(tenUpTexture);
        attachRepeatingListener(plusBtn, () -> {
            localStake = Math.min(200, localStake + 10);
            stakeValueLabel.setText(String.valueOf(localStake));
        });
        ImageButton proposeBtn = createActionButton(placeBidButtonTexture);
        proposeBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String customTopic = topicInput.getText().trim();
                String topic = customTopic.isEmpty() ? currentBattleTopicInput : customTopic;
                sendAction(GameActionRequest.ActionType.START_MEME_BATTLE, 0, localStake, topic);
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
        VisLabel label = new VisLabel(t("invite_battle"));
        label.setColor(BATTLE_TEXT_COLOR);
        label.setAlignment(Align.center);
        label.setFontScale(1.1f);
        p.add(label).center().expandX().padBottom(8f).row();

        String topic = state.battleTopic == null ? "" : state.battleTopic.trim();
        if (!topic.isEmpty()) {
            VisLabel topicLabel = new VisLabel((language == Language.RU ? "Тема: " : "Topic: ") + topic);
            topicLabel.setColor(Color.valueOf("000A3E"));
            topicLabel.setFontScale(0.95f);
            topicLabel.setAlignment(Align.center);
            topicLabel.setWrap(true);
            p.add(topicLabel).width(650f).center().expandX().padTop(4f).padBottom(8f).row();
        }

        Table stakeRow = new Table();
        stakeRow.center();
        VisLabel stakeLabel = new VisLabel(
                (language == Language.RU ? "Ставка: " : "Stakes: ") + state.battleStakes + " ");
        stakeLabel.setColor(BATTLE_TEXT_COLOR);
        stakeLabel.setFontScale(1.05f);
        stakeRow.add(stakeLabel);
        Image moneyIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        moneyIcon.setScaling(Scaling.fit);
        stakeRow.add(moneyIcon).size(20f, 20f);
        p.add(stakeRow).center().expandX().padBottom(20f).row();

        if (battleInviteAnswered) {
            VisLabel waitingLabel = new VisLabel(language == Language.RU ? "Ожидание выбора других игроков..." : "Waiting for other players...");
            waitingLabel.setColor(Color.valueOf("000A3E"));
            waitingLabel.setAlignment(Align.center);
            waitingLabel.setFontScale(0.85f);
            p.add(waitingLabel).center().pad(12f).row();
        } else {
            Table actionContainer = new Table();
            actionContainer.center();

            ImageButton yes = createActionButton(participateButtonTexture);
            ImageButton no = createActionButton(declineButtonTexture);
            Table btnRow = new Table();
            btnRow.add(yes).size(180f, 64f).padRight(14f);
            btnRow.add(no).size(180f, 64f);
            actionContainer.add(btnRow);

            yes.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    BattleResponsePacket packet = new BattleResponsePacket();
                    packet.playerId = game.getClient().getLocalPlayerId();
                    packet.accepted = true;
                    game.getClient().sendBattleResponse(packet);
                    battleInviteAnswered = true;
                    actionContainer.clearChildren();
                    VisLabel waitingLabel = new VisLabel(language == Language.RU ? "Ожидание выбора других игроков..." : "Waiting for other players...");
                    waitingLabel.setColor(Color.valueOf("000A3E"));
                    waitingLabel.setAlignment(Align.center);
                    waitingLabel.setFontScale(0.85f);
                    actionContainer.add(waitingLabel).center().pad(12f);
                }
            });
            no.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    BattleResponsePacket packet = new BattleResponsePacket();
                    packet.playerId = game.getClient().getLocalPlayerId();
                    packet.accepted = false;
                    game.getClient().sendBattleResponse(packet);
                    battleInviteAnswered = true;
                    actionContainer.clearChildren();
                    VisLabel waitingLabel = new VisLabel(language == Language.RU ? "Ожидание выбора других игроков..." : "Waiting for other players...");
                    waitingLabel.setColor(Color.valueOf("000A3E"));
                    waitingLabel.setAlignment(Align.center);
                    waitingLabel.setFontScale(0.85f);
                    actionContainer.add(waitingLabel).center().pad(12f);
                }
            });
            p.add(actionContainer).center().row();
        }
        return p;
    }

    private Table buildCollectingPanel(GameState state, boolean isOwner, int localId) {
        Table p = new Table();
        p.center();
        Meme submitted = getSubmittedBattleMeme(state, localId);
        Table topRow = new Table();
        String topic = state.battleTopic == null || state.battleTopic.isBlank() ? "" : state.battleTopic;
        VisLabel topicLabel = new VisLabel(topic);
        topicLabel.setColor(Color.valueOf("000A3E"));
        topicLabel.setAlignment(Align.center);
        topicLabel.setWrap(true);
        topicLabel.setFontScale(1.05f);
        topRow.add(topicLabel).width(500f).center();

        startBattleButton = createActionButton(startBattleTexture);
        startBattleButton.setDisabled(selectedBattleMemeId == -1 || submitted != null);
        startBattleButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (selectedBattleMemeId == -1)
                    return;
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

        // APPEND_BATTLE_TOPIC UI для держателя темы (банкрот-жюри)
        if (state.themeContinuatorId == localId && !state.themeContinuationUsed) {
            Table appendRow = new Table();
            VisTextField appendField = new VisTextField("");
            appendField.setMaxLength(60);
            appendField.setMessageText("Дописать тему...");
            Actor appendBtn = createTextureOrPillButton(startBattleTexture, "+Тема");
            appendBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    String txt = appendField.getText().trim();
                    if (!txt.isEmpty()) {
                        sendAction(GameActionRequest.ActionType.APPEND_BATTLE_TOPIC, 0, 0, txt);
                    }
                }
            });
            appendRow.add(appendField).width(320f).height(44f).padRight(8f);
            appendRow.add(appendBtn).size(100f, 44f);
            p.add(appendRow).padBottom(10f).row();
        }

        Table grid = new Table();
        grid.defaults().space(12f).pad(10f);
        int col = 0, count = 0;
        Player localPlayer = state.getPlayerById(localId);
        if (battleHandSnapshot == null && localPlayer != null && localPlayer.handMemes != null) {
            battleHandSnapshot = new ArrayList<>(localPlayer.handMemes);
        }
        List<Meme> displayMemes = battleHandSnapshot != null ? battleHandSnapshot
                : (localPlayer != null ? localPlayer.handMemes : null);
        if (displayMemes != null) {
            int idx = 0;
            for (Meme meme : displayMemes) {
                if (count >= 6)
                    break;
                boolean selected = meme.id == selectedBattleMemeId
                        || (submitted != null && meme.id == submitted.id);
                Table card = createBattleMemeCard(meme, selected, submitted != null, false, 0, () -> {
                    selectedBattleMemeId = (selectedBattleMemeId == meme.id) ? -1 : meme.id;
                    rebuildBattleContent(state);
                });
                Table holder = new Table();
                holder.setSize(330f, 248f);
                holder.setTransform(true);
                holder.setOrigin(Align.center);
                if (state.battlePhase != lastAnimatedBattlePhase) {
                    holder.setScale(0f);
                } else {
                    holder.setScale(1f);
                }
                holder.add(card).expand().fill();
                battleCardHolders.add(holder);
                grid.add(holder).size(330f, 248f);
                count++;
                idx++;
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
        VisLabel hint = new VisLabel(t("vote_hint"));
        hint.setColor(BATTLE_TEXT_COLOR);
        hint.setFontScale(0.9f);
        p.add(hint).center().padBottom(6f).row();

        if (state.battleTopic != null && !state.battleTopic.isBlank()) {
            VisLabel topicLabel = new VisLabel(state.battleTopic);
            topicLabel.setColor(Color.valueOf("000A3E"));
            topicLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
            topicLabel.setWrap(true);
            p.add(topicLabel).width(620f).padLeft(35f).padRight(35f).center().padBottom(12f).row();
        }

        boolean hasVoted = state.battleVoters != null && state.battleVoters.contains(localId);
        boolean isJury = state.juryIds != null && state.juryIds.contains(localId);
        Table grid = new Table();
        grid.defaults().space(12f).pad(10f);
        int col = 0;
        if (state.battleMemes != null) {
            for (Meme meme : state.battleMemes) {
                final int memeId = meme.id;
                boolean ownMeme = meme.ownerId == localId;
                // Jury can always vote (not own meme check); regular players can't vote own
                boolean cardDisabled = isJury
                        ? (hasVoted || ownMeme)
                        : (hasVoted || ownMeme);
                int voteCount = state.votes != null ? state.votes.getOrDefault(memeId, 0) : 0;
                int slotNumber = col + 1;
                int twitchVoteCount = 0;
                if (game.getTwitchChatService() != null && game.getTwitchChatService().isConnected()) {
                    twitchVoteCount = game.getTwitchChatService().getVotesForSlot(slotNumber);
                } else if (state.twitchVotes != null) {
                    twitchVoteCount = state.twitchVotes.getOrDefault(memeId, 0);
                }

                Table card = createBattleMemeCard(meme, false, cardDisabled, ownMeme, voteCount, twitchVoteCount, true,
                        () -> sendAction(GameActionRequest.ActionType.VOTE_MEME, memeId, 0, null));
                Table holder = new Table();
                holder.setSize(330f, 248f);
                holder.setTransform(true);
                holder.setOrigin(Align.center);
                holder.setScale(1f);
                if (state.battlePhase != lastAnimatedBattlePhase) {
                    holder.getColor().a = 0f;
                } else {
                    holder.getColor().a = 1f;
                }
                holder.add(card).expand().fill();
                battleCardHolders.add(holder);
                grid.add(holder).size(330f, 248f);
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

    private Table createBattleMemeCard(Meme meme, boolean selected, boolean disabled, boolean isOwnMeme, int votes,
            Runnable action) {
        return createBattleMemeCard(meme, selected, disabled, isOwnMeme, votes, 0, false, action);
    }

    private Table createBattleMemeCard(Meme meme, boolean selected, boolean disabled, boolean isOwnMeme, int votes,
            int twitchVotes, boolean showCounters, Runnable action) {
        Table card = new Table();
        card.pad(6f);
        Stack content = new Stack();
        Image image = createMemeImageWidget(meme);
        if (image != null) {
            float cardWidth = 318f;
            float cardHeight = cardWidth * 3f / 4f;
            image.setSize(cardWidth, cardHeight);
            image.setScaling(Scaling.fillX);
            image.setAlign(Align.center);
            if (isOwnMeme) {
                image.setColor(0.45f, 0.45f, 0.45f, 1f);
            }
            Container<Image> container = new Container<>(image);
            container.size(cardWidth, cardHeight);
            container.clip(true);
            content.add(container);
        } else {
            VisLabel placeholder = new VisLabel(t("no_preview"));
            placeholder.setColor(BATTLE_TEXT_COLOR);
            content.add(placeholder);
        }
        if (selected) {
            float cardWidth = 318f;
            float cardHeight = cardWidth * 3f / 4f;
            com.badlogic.gdx.scenes.scene2d.Group bracketGroup = new com.badlogic.gdx.scenes.scene2d.Group();
            bracketGroup.setSize(cardWidth, cardHeight);
            Texture bracketTex = UiShapes.getCornerBracketTexture();

            Image tl = new Image(bracketTex);
            tl.setPosition(0, cardHeight - 24f);

            Image tr = new Image(bracketTex);
            tr.setPosition(cardWidth - 24f, cardHeight - 24f);
            tr.setOrigin(12f, 12f);
            tr.setScale(-1f, 1f);

            Image bl = new Image(bracketTex);
            bl.setPosition(0, 0);
            bl.setOrigin(12f, 12f);
            bl.setScale(1f, -1f);

            Image br = new Image(bracketTex);
            br.setPosition(cardWidth - 24f, 0);
            br.setOrigin(12f, 12f);
            br.setScale(-1f, -1f);

            tl.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.forever(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy(-4f, 4f, 0.4f,
                                    com.badlogic.gdx.math.Interpolation.sine),
                            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy(4f, -4f, 0.4f,
                                    com.badlogic.gdx.math.Interpolation.sine))));
            tr.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.forever(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy(4f, 4f, 0.4f,
                                    com.badlogic.gdx.math.Interpolation.sine),
                            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy(-4f, -4f, 0.4f,
                                    com.badlogic.gdx.math.Interpolation.sine))));
            bl.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.forever(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy(-4f, -4f, 0.4f,
                                    com.badlogic.gdx.math.Interpolation.sine),
                            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy(4f, 4f, 0.4f,
                                    com.badlogic.gdx.math.Interpolation.sine))));
            br.addAction(com.badlogic.gdx.scenes.scene2d.actions.Actions.forever(
                    com.badlogic.gdx.scenes.scene2d.actions.Actions.sequence(
                            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy(4f, -4f, 0.4f,
                                    com.badlogic.gdx.math.Interpolation.sine),
                            com.badlogic.gdx.scenes.scene2d.actions.Actions.moveBy(-4f, 4f, 0.4f,
                                    com.badlogic.gdx.math.Interpolation.sine))));

            bracketGroup.addActor(tl);
            bracketGroup.addActor(tr);
            bracketGroup.addActor(bl);
            bracketGroup.addActor(br);
            content.add(bracketGroup);
        }

        if (showCounters) {
            Table bottomOverlay = new Table();
            bottomOverlay.bottom().center().padBottom(8f);

            Table countersRow = new Table();
            countersRow.center();

            // 1. Счётчик игроков: круг с тёмно-синим контуром 2px (000A3E), белым фоном и числом голосов
            Table playerBadge = new Table();
            Texture playerCircleTex = UiShapes.outlinedCircle(19, 2, Color.WHITE, Color.valueOf("000A3E"));
            playerBadge.setBackground(new TextureRegionDrawable(new TextureRegion(playerCircleTex)));
            VisLabel count = new VisLabel(String.valueOf(votes));
            count.setColor(Color.valueOf("000A3E"));
            count.setFontScale(1.05f);
            playerBadge.add(count);
            countersRow.add(playerBadge).size(38f, 38f);

            // 2. Счётчик Twitch: компактная пилюля с фиолетовым контуром (9146FF), белым фоном, мини-иконкой twitch.png (22x22f) и числом зрителей
            Table twitchBadge = new Table();
            Texture twitchPillTex = UiShapes.outlinedPill(68, 36, 2, Color.WHITE, Color.valueOf("9146FF"));
            twitchBadge.setBackground(new TextureRegionDrawable(new TextureRegion(twitchPillTex)));
            twitchBadge.pad(2f, 6f, 2f, 6f);
            if (twitchTexture != null) {
                Image ti = new Image(twitchTexture);
                ti.setScaling(Scaling.fit);
                twitchBadge.add(ti).size(22f, 22f).padLeft(2f).padRight(4f);
            }
            VisLabel twitchCount = new VisLabel(String.valueOf(twitchVotes));
            twitchCount.setColor(Color.valueOf("9146FF"));
            twitchCount.setFontScale(1.0f);
            twitchBadge.add(twitchCount);
            countersRow.add(twitchBadge).size(68f, 36f).padLeft(8f);

            bottomOverlay.add(countersRow).bottom().center();
            content.add(bottomOverlay);
        }

        card.add(content).expand().fill();
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

    private Table buildResultsPanel(GameState state) {
        Table root = new Table();
        root.center();

        if (state.battleTopic != null && !state.battleTopic.isBlank()) {
            VisLabel topicLabel = new VisLabel(state.battleTopic);
            topicLabel.setColor(Color.valueOf("000A3E"));
            topicLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
            topicLabel.setWrap(true);
            root.add(topicLabel).width(620f).padLeft(35f).padRight(35f).center().padBottom(12f).row();
        }

        Stack stack = new Stack();
        Table gridContainer = new Table();
        gridContainer.center();
        Table grid = new Table();
        grid.defaults().space(12f).pad(10f);

        Group freeOverlay = new Group();
        freeOverlay.setSize(860f, 640f);
        freeOverlay.setTouchable(Touchable.childrenOnly);

        int col = 0;
        Table winnerHolder = null;
        List<Table> loserHolders = new ArrayList<>();

        if (state.battleMemes != null) {
            for (Meme meme : state.battleMemes) {
                final int memeId = meme.id;
                int voteCount = state.votes != null ? state.votes.getOrDefault(memeId, 0) : 0;
                int twitchVoteCount = state.twitchVotes != null ? state.twitchVotes.getOrDefault(memeId, 0) : 0;

                Table card = createBattleMemeCard(meme, false, true, false, voteCount, twitchVoteCount, false, null);
                Table holder = new Table();
                holder.setSize(330f, 248f);
                holder.setTransform(true);
                holder.setOrigin(Align.center);
                holder.setScale(1f);
                holder.add(card).expand().fill();

                if (memeId == state.battleWinnerMemeId) {
                    winnerHolder = holder;
                } else {
                    loserHolders.add(holder);
                }

                grid.add(holder).size(330f, 248f);
                if (++col % 2 == 0)
                    grid.row();
            }
        }

        gridContainer.add(grid).center();
        stack.add(gridContainer);
        stack.add(freeOverlay);
        root.add(stack).size(860f, 640f).center().row();

        // Немедленная валидация для точного расчета стартовых координат в сетке
        root.validate();
        gridContainer.validate();
        grid.validate();

        // 1. Анимация исчезновения проигравших карточек
        for (Table loser : loserHolders) {
            loser.setTransform(true);
            loser.setOrigin(Align.center);
            loser.addAction(Actions.sequence(
                    Actions.parallel(
                            Actions.scaleTo(0f, 0f, 0.45f, com.badlogic.gdx.math.Interpolation.pow2In),
                            Actions.fadeOut(0.4f)),
                    Actions.visible(false)));
        }

        // 2. Анимация центрирования и зума победителя
        if (winnerHolder != null) {
            Vector2 pos = new Vector2(0, 0);
            if (winnerHolder.getStage() != null && freeOverlay.getStage() != null) {
                winnerHolder.localToStageCoordinates(pos);
                freeOverlay.stageToLocalCoordinates(pos);
            } else {
                winnerHolder.localToAscendantCoordinates(stack, pos);
            }

            winnerHolder.remove();
            winnerHolder.setPosition(pos.x, pos.y);
            freeOverlay.addActor(winnerHolder);

            winnerHolder.setTransform(true);
            winnerHolder.setOrigin(Align.center);

            float targetCenterX = (860f - winnerHolder.getWidth()) / 2f;
            float targetCenterY = (640f - winnerHolder.getHeight()) / 2f + 25f;

            winnerHolder.addAction(Actions.parallel(
                    Actions.moveTo(targetCenterX, targetCenterY, 0.6f, com.badlogic.gdx.math.Interpolation.pow2Out),
                    Actions.scaleTo(1.25f, 1.25f, 0.6f, com.badlogic.gdx.math.Interpolation.pow2Out)));

            // 3. Нижняя плашка победы
            Table victoryPlaque = new Table();
            victoryPlaque.center();
            victoryPlaque.setSize(860f, 50f);
            victoryPlaque.setPosition(0f, targetCenterY - 60f);

            boolean ru = language == Language.RU;
            if (state.battleWinnerPlayerId > 0) {
                Player winner = state.getPlayerById(state.battleWinnerPlayerId);
                Color roleColor = Color.valueOf("000A3E");
                if (state.playerRoles != null && winner != null && state.playerRoles.containsKey(winner.id)) {
                    try {
                        Role r = Role.valueOf(state.playerRoles.get(winner.id));
                        roleColor = RoleInfo.color(r);
                    } catch (Exception ignored) {}
                }
                VisLabel nickLabel = new VisLabel(winner != null ? winner.name : "Игрок");
                nickLabel.setColor(roleColor);
                nickLabel.setFontScale(1.1f);

                String prizeStr = String.valueOf(state.battlePrizeAmount > 0 ? state.battlePrizeAmount : state.battleBank);
                String winText = (ru ? " побеждает и получает " : " wins and gets ") + prizeStr + " ";
                VisLabel textLabel = new VisLabel(winText);
                textLabel.setColor(Color.valueOf("000A3E"));
                textLabel.setFontScale(1.1f);

                Image coin = new Image(moneyTexture);
                victoryPlaque.add(nickLabel);
                victoryPlaque.add(textLabel);
                victoryPlaque.add(coin).size(20f, 20f);
            } else {
                VisLabel tieLabel = new VisLabel(ru ? "Ничья! Банк разделён." : "Draw! The bank is split.");
                tieLabel.setColor(Color.valueOf("000A3E"));
                tieLabel.setFontScale(1.1f);
                victoryPlaque.add(tieLabel);
            }

            victoryPlaque.getColor().a = 0f;
            victoryPlaque.addAction(Actions.sequence(
                    Actions.delay(0.35f),
                    Actions.fadeIn(0.35f)));
            freeOverlay.addActor(victoryPlaque);
        } else {
            Table victoryPlaque = new Table();
            victoryPlaque.center();
            victoryPlaque.setSize(860f, 50f);
            victoryPlaque.setPosition(0f, 40f);
            boolean ru = language == Language.RU;
            VisLabel tieLabel = new VisLabel(ru ? "Ничья! Банк разделён." : "Draw! The bank is split.");
            tieLabel.setColor(Color.valueOf("000A3E"));
            tieLabel.setFontScale(1.1f);
            victoryPlaque.add(tieLabel);
            freeOverlay.addActor(victoryPlaque);
        }

        return root;
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
        if (state.battlePhase == GameState.BattlePhase.VOTING) {
            if (game.getTwitchChatService() != null) {
                sb.append('|').append(game.getTwitchChatService().getVotesSignature());
            }
            if (state.votes != null) {
                for (Map.Entry<Integer, Integer> v : state.votes.entrySet()) {
                    sb.append(v.getKey()).append('=').append(v.getValue()).append(',');
                }
            }
        }
        if (state.battlePhase == GameState.BattlePhase.RESULTS) {
            sb.append('|').append(state.battleWinnerMemeId).append(':').append(state.battleWinnerPlayerId);
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

    private void playShieldBreakAnimation() {
        if (shieldTexture == null || stage == null) {
            return;
        }
        if (activeShieldAnimGroup != null) {
            activeShieldAnimGroup.remove();
            activeShieldAnimGroup = null;
        }
        isShieldAnimRunning = true;
        if (actionRow != null) {
            actionRow.setVisible(false);
        }

        Group shieldAnimGroup = new Group();
        shieldAnimGroup.setSize(stage.getWidth(), stage.getHeight());
        shieldAnimGroup.setPosition(0, 0);
        shieldAnimGroup.setTouchable(Touchable.disabled);

        Image shieldImage = new Image(shieldTexture);
        shieldImage.setSize(140f, 163f);
        shieldImage.setOrigin(Align.center);
        shieldImage.setTouchable(Touchable.disabled);
        shieldImage.setPosition((stage.getWidth() - 140f) / 2f,
                (stage.getHeight() - 163f) / 2f);
        shieldImage.setScale(1.1f);
        shieldImage.getColor().a = 1f;

        shieldAnimGroup.addActor(shieldImage);
        stage.addActor(shieldAnimGroup);
        shieldAnimGroup.toFront();
        activeShieldAnimGroup = shieldAnimGroup;

        shieldImage.addAction(Actions.sequence(
                Actions.delay(0.45f),
                Actions.scaleTo(0.1f, 0.1f, 0.75f, com.badlogic.gdx.math.Interpolation.pow2In),
                Actions.run(() -> {
                    if (brokeShieldTexture != null) {
                        shieldImage.setDrawable(new TextureRegionDrawable(new TextureRegion(brokeShieldTexture)));
                    }
                }),
                Actions.scaleTo(1.1f, 1.1f, 0.70f, com.badlogic.gdx.math.Interpolation.bounceOut),
                Actions.delay(0.70f),
                Actions.fadeOut(0.45f),
                Actions.run(() -> {
                    shieldAnimGroup.remove();
                    if (activeShieldAnimGroup == shieldAnimGroup) {
                        activeShieldAnimGroup = null;
                    }
                    isShieldAnimRunning = false;
                    lastActionRowSig = "";
                    if (actionRow != null) {
                        actionRow.setVisible(true);
                        actionRow.getColor().a = 0f;
                        actionRow.addAction(Actions.fadeIn(0.2f));
                    }
                    refreshActions(game.getLatestGameState());
                })
        ));
    }

    // GameScreen.java — refreshUi (замена dealButton.setDisabled)

    private void refreshUi(GameState state) {
        if (state == null) {
            isShieldAnimRunning = false;
            lastKnownPositions.clear();
            lastInJailStates.clear();
            lastKnownShields.clear();
            pendingShieldBreak = false;
            if (activeShieldAnimGroup != null) {
                activeShieldAnimGroup.remove();
                activeShieldAnimGroup = null;
            }
            pendingJailStep.clear();
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

        if (state.currentPhase == GameState.GamePhase.GAME_OVER || state.isGameOver) {
            if (diceOverlay != null)
                diceOverlay.setVisible(false);
            setButtonsEnabled(false, false, false, false, false);
            showGameOverModal(state);
            return;
        }

        if (state.players != null) {
            Set<Integer> currentPlayerIds = new HashSet<>();
            int localPlayerId = game.getClient().getLocalPlayerId();
            for (Player p : state.players) {
                currentPlayerIds.add(p.id);
                Integer lastShields = lastKnownShields.get(p.id);
                Integer lastPos = lastKnownPositions.get(p.id);
                if (p.id == localPlayerId && lastShields != null && p.shields < lastShields) {
                    boolean isMoving = hasPendingMove
                            || (boardRenderer != null && boardRenderer.isMovementActive())
                            || diceAnimPhase != DiceAnimPhase.IDLE
                            || (lastPos != null && !lastPos.equals(p.position));
                    if (isMoving) {
                        pendingShieldBreak = true;
                    } else {
                        playShieldBreakAnimation();
                    }
                }
                lastKnownShields.put(p.id, p.shields);

                boolean wasInJail = Boolean.TRUE.equals(lastInJailStates.get(p.id));
                boolean isNewArrest = p.inJail && !wasInJail;

                if (lastPos == null) {
                    lastKnownPositions.put(p.id, p.position);
                    lastInJailStates.put(p.id, p.inJail);
                    if (boardRenderer != null) {
                        boardRenderer.setVisualPosition(p.id, p.position);
                    }
                } else if (lastPos != p.position || isNewArrest) {
                    int from = lastPos;
                    if (state.awaitingReroll && p.id == state.rerollPlayerId && state.rerollOrigin != -1
                            && state.rerollOrigin != lastPos) {
                        from = state.rerollOrigin;
                    }
                    if (isNewArrest && p.position == 10) {
                        if (from == 30) {
                            if (!pendingJailStep.containsKey(p.id) && boardRenderer != null) {
                                boardRenderer.animateMovement(p.id, 30, 10, true);
                            }
                        } else {
                            if (!pendingJailStep.containsKey(p.id)) {
                                pendingJailStep.put(p.id, 1);
                                if (diceAnimPhase == DiceAnimPhase.ROLLING) {
                                    pendingMovePlayerId = p.id;
                                    pendingFromCell = from;
                                    pendingToCell = 30;
                                    hasPendingMove = true;
                                    pendingMoveTimer = 0f;
                                    if (boardRenderer != null) {
                                        boardRenderer.setVisualPosition(p.id, from);
                                    }
                                } else {
                                    if (boardRenderer != null) {
                                        boardRenderer.animateMovement(p.id, from, 30);
                                    }
                                }
                            }
                        }
                    } else {
                        if (diceAnimPhase == DiceAnimPhase.ROLLING) {
                            pendingMovePlayerId = p.id;
                            pendingFromCell = from;
                            pendingToCell = p.position;
                            hasPendingMove = true;
                            pendingMoveTimer = 0f;
                            if (boardRenderer != null) {
                                boardRenderer.setVisualPosition(p.id, from);
                            }
                        } else {
                            if (boardRenderer != null) {
                                boardRenderer.animateMovement(p.id, from, p.position);
                            }
                        }
                    }
                    lastKnownPositions.put(p.id, p.position);
                    lastInJailStates.put(p.id, p.inJail);
                } else {
                    lastInJailStates.put(p.id, p.inJail);
                    if (boardRenderer != null) {
                        boolean inJailPending = pendingJailStep.containsKey(p.id);
                        boardRenderer.syncVisualPosition(p.id, p.position,
                                (hasPendingMove && pendingMovePlayerId == p.id)
                                        || diceAnimPhase == DiceAnimPhase.ROLLING
                                        || inJailPending);
                    }
                }

                Integer prevMoney = lastMoney.get(p.id);
                if (prevMoney != null && prevMoney != p.money) {
                    boolean movementActive = hasPendingMove
                            || (boardRenderer != null && boardRenderer.hasActiveAnimations())
                            || diceAnimPhase != DiceAnimPhase.IDLE
                            || !pendingJailStep.isEmpty();
                    if (movementActive) {
                        // Фишка ещё движется или кубики крутятся — откладываем бейдж до конца анимации
                        pendingMoneyDeltas.put(p.id, p.money);
                        lastMoney.put(p.id, p.money);
                    } else {
                        startMoneyDeltaAnimation(p.id, p.money);
                    }
                } else if (prevMoney == null) {
                    lastMoney.put(p.id, p.money);
                    displayedMoney.putIfAbsent(p.id, p.money);
                }
            }
            lastKnownPositions.keySet().retainAll(currentPlayerIds);
            lastInJailStates.keySet().retainAll(currentPlayerIds);
            lastMoney.keySet().retainAll(currentPlayerIds);
            displayedMoney.keySet().retainAll(currentPlayerIds);
            for (Integer animPid : new ArrayList<>(activeMoneyAnims.keySet())) {
                if (!currentPlayerIds.contains(animPid)) {
                    MoneyDeltaAnim st = activeMoneyAnims.remove(animPid);
                    if (st != null && st.actor != null) {
                        st.actor.remove();
                    }
                }
            }
        }

        Player current = state.getCurrentPlayer();
        int localPlayerId = game.getClient().getLocalPlayerId();
        if (localPlayerId <= 0 && game.isHost() && state.players != null && !state.players.isEmpty()) {
            localPlayerId = state.players.get(0).id;
        }
        Player localPlayer = state.getPlayerById(localPlayerId);
        boolean myTurn = current != null && (current.id == localPlayerId || (localPlayerId <= 0 && game.isHost()));
        BoardCell currentCell = current != null ? boardCells.get(current.position) : null;

        if (current != null && current.id != lastTurnPlayerId) {
            lastTurnPlayerId = current.id;
            lastActionRowSig = "";
            if (pendingJailStep.isEmpty() && !hasPendingMove) {
                hasPendingMove = false;
                pendingMoveTimer = 0f;
                diceAnimPhase = DiceAnimPhase.IDLE;
            }
            if (!pendingMoneyDeltas.isEmpty()) {
                for (Map.Entry<Integer, Integer> entry : new ArrayList<>(pendingMoneyDeltas.entrySet())) {
                    startMoneyDeltaAnimation(entry.getKey(), entry.getValue());
                }
                pendingMoneyDeltas.clear();
            }
            Color roleColor = getPlayerRoleColor(state, current);
            chatWidget.showTurnMessage(t("turn_prefix"), current.name, roleColor);
        }

        phaseLabel.setText(t("phase_prefix") + " " + state.currentPhase);
        turnLabel.setText(current == null ? t("turn_idle")
                : t("turn_prefix") + " " + current.name + " | " + t("turn_number") + " " + state.turnCount);
        cellLabel.setText(currentCell == null ? t("cell_idle")
                : t("cell_prefix") + " " + currentCell.name + " [" + currentCell.type + "]");
        String actionLog = state.lastActionLog;
        if (actionLog != null && language == Language.EN) {
            Matcher mExtract = DONATION_EXTRACT_RU.matcher(actionLog);
            if (mExtract.matches()) {
                actionLog = "Player " + mExtract.group(1) + " donated " + mExtract.group(2) + " to "
                        + mExtract.group(3) + " after landing on " + mExtract.group(4);
            }
        }
        logLabel.setText(t("log_prefix") + " " + (actionLog == null ? "-" : actionLog));
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
        // Animation gating: defer modals/buttons while the current mover is still
        // animating
        boolean moverAnimating = current != null && boardRenderer.isAnimating(current.id);

        refreshActions(state, myTurn, currentCell, moverAnimating);
        refreshBattleOverlay(state);

        if (dealButton != null) {
            boolean canTrade = myTurn
                    && state.currentPhase == GameState.GamePhase.PLAYING
                    && !state.isInBattle
                    && !state.isInAuction
                    && state.tradeId == 0
                    && tradeMode == TRADE_MODE_CLOSED
                    && !moverAnimating;
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
        } else if (tradeMode == TRADE_MODE_CLOSED && tradeWindow == null) {
            removeTradeTouchLayer();
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
        avatarFrameImages.clear();
        if (state == null || state.players == null)
            return;
        boolean ru = language == Language.RU;
        Player localPlayer = state.getPlayerById(localPlayerId);
        if (bankBalanceRow != null && bankBalanceLabel != null) {
            if (localPlayer != null && localPlayer.memeBankBalance > 0) {
                bankBalanceLabel.setText(String.valueOf(localPlayer.memeBankBalance));
                bankBalanceRow.setVisible(true);
            } else {
                bankBalanceRow.setVisible(false);
            }
        }
        for (int i = 0; i < state.players.size(); i++) {
            Player player = state.players.get(i);
            Role role = Role.of(state.playerRoles == null ? null : state.playerRoles.get(player.id));
            Role visualRole = role != null ? role : NO_ROLE_FALLBACK_ORDER[i % 4];
            Color roleColor = RoleInfo.color(visualRole);
            Table row = new Table();
            row.left();
            row.pad(8, 6, 8, 10);

            Stack avatarStack = new Stack();
            Texture frameTexture = avatarFrameByRole.get(visualRole);
            if (frameTexture != null) {
                Image frame = new Image(frameTexture);
                frame.setScaling(Scaling.fit);
                frame.setSize(60f, 60f); // размер известен сразу, не ждём layout
                frame.setOrigin(30f, 30f); // ось вращения — строго центр рамки
                avatarFrameImages.add(frame);
                avatarStack.add(frame);
            } else {
                Image ring = new Image(UiShapes.ring(34, 5, roleColor != null ? roleColor : Color.GRAY));
                ring.setSize(60f, 60f);
                ring.setOrigin(30f, 30f);
                avatarFrameImages.add(ring);
                avatarStack.add(ring);
            }

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
            avTable.pad(9f);
            avTable.add(av).size(50f, 50f);
            avatarStack.add(avTable);

            row.add(avatarStack).size(60f).padRight(14f);

            String displayName = player.name;
            if (displayName.length() > 8) {
                displayName = displayName.substring(0, 8) + "…";
            }
            VisLabel nameLabel = new VisLabel(displayName);
            nameLabel.setWrap(false);
            nameLabel.setFontScale(0.72f);
            nameLabel.setColor(BATTLE_TEXT_COLOR);
            nameLabelRefs.put(player.id, nameLabel);

            Table mid = new Table();
            mid.top().left();
            mid.add(nameLabel).left().padBottom(4f).row();

            if (state.rolesEnabled && role != null) {
                Table roleRow = new Table();
                roleRow.left();
                VisLabel roleLabel = new VisLabel(RoleInfo.name(role, ru));
                roleLabel.setColor(roleColor);
                roleLabel.setFontScale(0.48f);
                roleRow.add(roleLabel).left();
                Actor infoIcon = createRoleInfoIcon(role);
                roleInfoIcons.put(infoIcon, role);
                roleRow.add(infoIcon).size(14f, 14f).padLeft(4f);
                mid.add(roleRow).left().row();
            } else {
                mid.add().height(16f).row();
            }

            if (player.isBankrupt) {
                VisLabel bankruptLabel = new VisLabel(t("bankrupt"));
                bankruptLabel.setColor(new Color(0.85f, 0.15f, 0.15f, 1f));
                bankruptLabel.setFontScale(0.6f);
                mid.add(bankruptLabel).left().row();
            }
            row.add(mid).width(135f).left().top().padRight(4f);

            Table rightCol = new Table();
            rightCol.top().right();
            rightCol.padRight(0);

            float pillHeight = 28f;
            NinePatchDrawable pillDrawable = createPillDrawable(rectForMoneyTexture, pillHeight);

            Table moneyCell = new Table();
            moneyCell.right();
            moneyCell.clearChildren();
            if (pillDrawable != null) {
                moneyCell.setBackground(pillDrawable);
            }
            moneyCell.pad(3f, 12f, 3f, 12f);
            moneyCellRefs.put(player.id, moneyCell);
            Image coinIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
            coinIcon.setScaling(Scaling.fit);
            int shownVal = displayedMoney.getOrDefault(player.id, player.money);
            displayedMoney.put(player.id, shownVal);
            VisLabel moneyLabel = new VisLabel(String.valueOf(shownVal));
            moneyLabel.setColor(ACCENT_GOLD);
            moneyLabel.setFontScale(0.65f);
            moneyLabelRefs.put(player.id, moneyLabel);
            moneyCell.add(moneyLabel).padRight(4f);
            moneyCell.add(coinIcon).size(18f, 18f);
            rightCol.add(moneyCell).height(pillHeight).right().padBottom(4f).row();

            Table shieldBox = new Table();
            shieldBox.right();
            if (player.shields > 0 && shieldTexture != null) {
                Image shieldIcon1 = new Image(shieldTexture);
                shieldIcon1.setScaling(Scaling.fit);
                shieldBox.add(shieldIcon1).size(20f, 20f);
                if (player.shields > 1) {
                    Image shieldIcon2 = new Image(shieldTexture);
                    shieldIcon2.setScaling(Scaling.fit);
                    shieldBox.add(shieldIcon2).size(20f, 20f).padLeft(2f);
                }
            } else {
                shieldBox.add().height(20f);
            }
            rightCol.add(shieldBox).right().row();

            row.add(rightCol).width(130f).right().top();
            playersTable.add(row).growX().left().padBottom(8).row();
        }
    }

    private Actor createRoleInfoIcon(Role role) {
        Image infoIcon = new Image(new TextureRegionDrawable(new TextureRegion(UiShapes.glyphInfoIcon())));
        infoIcon.setScaling(Scaling.fit);
        infoIcon.setSize(14f, 14f);
        infoIcon.setTouchable(Touchable.enabled);
        return infoIcon;
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
        if (actionsHeaderContainer != null) {
            actionsHeaderContainer.clearChildren();
        }
        if (ownedCellsTable != null) {
            ownedCellsTable.clearChildren();
        }
        if (!myTurn && pendingActionMode != BoardRenderer.PendingActionMode.NONE) {
            clearPendingActionSelection();
        }
        if (boardRenderer != null) {
            boardRenderer.setPendingActionMode(pendingActionMode, localPlayer != null ? localPlayer.id : -1);
        }
        syncPendingActionLayers(state, localPlayer);

        if (!myTurn) {
            VisLabel waitingLabel = new VisLabel(language == Language.RU ? "Ожидание хода..." : "Waiting for turn...");
            waitingLabel.setColor(BATTLE_TEXT_COLOR);
            waitingLabel.setFontScale(0.85f);
            if (actionsHeaderContainer != null) {
                actionsHeaderContainer.add(waitingLabel).center().pad(12f).row();
            } else if (ownedCellsTable != null) {
                ownedCellsTable.add(waitingLabel).center().pad(12f).row();
            }
            return;
        }

        boolean hasAnyMonopoly = localPlayer != null && MonopolyUtils.countFullGroups(localPlayer, boardCells) > 0;
        boolean inOverlay = (pendingActionMode != BoardRenderer.PendingActionMode.NONE);

        Table actionsHeader = new Table();
        actionsHeader.center();

        // 1. Mortgage button
        Actor mortBtn;
        if (mortgageButtonTexture != null) {
            ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
            TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(mortgageButtonTexture));
            style.up = drawable;
            style.over = drawable.tint(new Color(0.92f, 0.92f, 0.92f, 1f));
            style.down = drawable.tint(new Color(0.85f, 0.85f, 0.85f, 1f));
            ImageButton imgBtn = new ImageButton(style);
            imgBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (pendingActionMode == BoardRenderer.PendingActionMode.MORTGAGE) {
                        clearPendingActionSelection();
                    } else {
                        pendingActionMode = BoardRenderer.PendingActionMode.MORTGAGE;
                        rebuildOwnedCells(state, localPlayer, myTurn, currentPhase);
                    }
                }
            });
            mortBtn = imgBtn;
        } else {
            VisTextButton pillBtn = createPillButton(t("mortgage_cell"));
            pillBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    if (pendingActionMode == BoardRenderer.PendingActionMode.MORTGAGE) {
                        clearPendingActionSelection();
                    } else {
                        pendingActionMode = BoardRenderer.PendingActionMode.MORTGAGE;
                        rebuildOwnedCells(state, localPlayer, myTurn, currentPhase);
                    }
                }
            });
            mortBtn = pillBtn;
        }

        // 2. Deal button
        Actor dealBtn;
        if (dealButtonTexture != null) {
            ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
            TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(dealButtonTexture));
            style.up = drawable;
            style.over = drawable.tint(new Color(0.92f, 0.92f, 0.92f, 1f));
            style.down = drawable.tint(new Color(0.85f, 0.85f, 0.85f, 1f));
            ImageButton imgBtn = new ImageButton(style);
            imgBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    openTradeBuildWindow(game.getLatestGameState());
                }
            });
            dealBtn = imgBtn;
        } else {
            VisTextButton pillBtn = createPillButton(t("btn_deal"));
            pillBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    openTradeBuildWindow(game.getLatestGameState());
                }
            });
            dealBtn = pillBtn;
        }

        boolean isMortgageActive = (pendingActionMode == BoardRenderer.PendingActionMode.MORTGAGE
                || pendingActionMode == BoardRenderer.PendingActionMode.UNMORTGAGE);
        if (inOverlay) {
            if (isMortgageActive) {
                mortBtn.setColor(Color.WHITE);
                mortBtn.setTouchable(Touchable.enabled);
            } else {
                mortBtn.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
                mortBtn.setTouchable(Touchable.disabled);
            }
            dealBtn.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
            dealBtn.setTouchable(Touchable.disabled);
        } else {
            mortBtn.setColor(Color.WHITE);
            mortBtn.setTouchable(Touchable.enabled);
            dealBtn.setColor(Color.WHITE);
            dealBtn.setTouchable(Touchable.enabled);
        }

        Table row1 = new Table();
        row1.center();
        row1.add(mortBtn).width(110f).height(44f).pad(4f);
        row1.add(dealBtn).width(110f).height(44f).pad(4f);
        actionsHeader.add(row1).row();

        // 3. Build & Sell buttons if monopoly owned
        if (hasAnyMonopoly) {
            Actor buildBtn;
            if (buildButtonTexture != null) {
                ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
                TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(buildButtonTexture));
                style.up = drawable;
                style.over = drawable.tint(new Color(0.92f, 0.92f, 0.92f, 1f));
                style.down = drawable.tint(new Color(0.85f, 0.85f, 0.85f, 1f));
                ImageButton imgBtn = new ImageButton(style);
                imgBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (pendingActionMode == BoardRenderer.PendingActionMode.BUILD_SELL) {
                            clearPendingActionSelection();
                        } else {
                            pendingActionMode = BoardRenderer.PendingActionMode.BUILD_SELL;
                            rebuildOwnedCells(state, localPlayer, myTurn, currentPhase);
                        }
                    }
                });
                buildBtn = imgBtn;
            } else {
                VisTextButton pillBtn = createPillButton(t("btn_build"));
                pillBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (pendingActionMode == BoardRenderer.PendingActionMode.BUILD_SELL) {
                            clearPendingActionSelection();
                        } else {
                            pendingActionMode = BoardRenderer.PendingActionMode.BUILD_SELL;
                            rebuildOwnedCells(state, localPlayer, myTurn, currentPhase);
                        }
                    }
                });
                buildBtn = pillBtn;
            }

            boolean isBuildActive = (pendingActionMode == BoardRenderer.PendingActionMode.BUILD_SELL);
            if (inOverlay) {
                if (isBuildActive) {
                    buildBtn.setColor(Color.WHITE);
                    buildBtn.setTouchable(Touchable.enabled);
                } else {
                    buildBtn.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
                    buildBtn.setTouchable(Touchable.disabled);
                }
            } else {
                buildBtn.setColor(Color.WHITE);
                buildBtn.setTouchable(Touchable.enabled);
            }

            Actor sellBtn = null;
            boolean hasAnyHouse = localPlayer != null && localPlayer.ownedCells != null
                    && localPlayer.ownedCells.stream().anyMatch(cid -> state.cellHouses.getOrDefault(cid, 0) > 0);
            if (hasAnyHouse) {
                VisTextButton pillBtn = createPillButton(t("btn_sell_house"));
                pillBtn.addListener(new ChangeListener() {
                    @Override
                    public void changed(ChangeEvent event, Actor actor) {
                        if (pendingActionMode == BoardRenderer.PendingActionMode.SELL_HOUSE) {
                            clearPendingActionSelection();
                        } else {
                            pendingActionMode = BoardRenderer.PendingActionMode.SELL_HOUSE;
                            rebuildOwnedCells(state, localPlayer, myTurn, currentPhase);
                        }
                    }
                });
                sellBtn = pillBtn;

                boolean isSellActive = (pendingActionMode == BoardRenderer.PendingActionMode.SELL_HOUSE);
                if (inOverlay) {
                    if (isSellActive) {
                        sellBtn.setColor(Color.WHITE);
                        sellBtn.setTouchable(Touchable.enabled);
                    } else {
                        sellBtn.setColor(new Color(0.4f, 0.4f, 0.4f, 1f));
                        sellBtn.setTouchable(Touchable.disabled);
                    }
                } else {
                    sellBtn.setColor(Color.WHITE);
                    sellBtn.setTouchable(Touchable.enabled);
                }
            }

            Table row2 = new Table();
            row2.center();
            row2.add(buildBtn).width(110f).height(44f).pad(4f);
            if (sellBtn != null) {
                row2.add(sellBtn).width(110f).height(44f).pad(4f);
            }
            actionsHeader.add(row2).row();
        }

        if (actionsHeaderContainer != null) {
            actionsHeaderContainer.add(actionsHeader).growX().center().row();
        } else if (ownedCellsTable != null) {
            ownedCellsTable.add(actionsHeader).growX().padBottom(8f).row();
        }

        updateOverlayDimming();
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

        Image image = createMemeImageWidget(meme);
        if (image != null) {
            float cardWidth = 320f;
            float cardHeight = cardWidth * 3f / 4f;
            image.setSize(cardWidth, cardHeight);
            image.setScaling(Scaling.fillX);
            image.setAlign(Align.center);
            Container<Image> container = new Container<>(image);
            container.size(cardWidth, cardHeight);
            container.clip(true);
            card.add(container).expand().fill();
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

    private Image createMemeImageWidget(Meme meme) {
        if (meme == null || meme.imageUrl == null || meme.imageUrl.isBlank()) {
            return null;
        }
        com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> anim = getMemeAnimation(meme.imageUrl);
        if (anim != null) {
            return new com.memopoly.ui.AnimatedImage(anim);
        }
        Drawable memeDrawable = getMemeDrawable(meme);
        return memeDrawable != null ? new Image(memeDrawable) : null;
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

    public com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> getMemeAnimation(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        if (memeAnimationCache.containsKey(imagePath)) {
            return memeAnimationCache.get(imagePath);
        }

        FileHandle file = Gdx.files.local(imagePath);
        if (!file.exists())
            file = Gdx.files.absolute(imagePath);
        if (!file.exists())
            file = Gdx.files.internal(imagePath);
        if (!file.exists()) {
            memeAnimationCache.put(imagePath, null);
            return null;
        }

        if (imagePath.toLowerCase().endsWith(".gif")) {
            GifDecoder.GifResult result = GifDecoder
                    .loadGIFResult(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP, file);
            if (result != null) {
                if (result.textures != null) {
                    dynamicGifTextures.addAll(result.textures);
                }
                memeAnimationCache.put(imagePath, result.animation);
                return result.animation;
            }
        }

        Texture texture = getMemeTexture(imagePath);
        if (texture == null) {
            memeAnimationCache.put(imagePath, null);
            return null;
        }
        com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> staticAnim = new com.badlogic.gdx.graphics.g2d.Animation<>(
                1.0f, new TextureRegion(texture));
        staticAnim.setPlayMode(com.badlogic.gdx.graphics.g2d.Animation.PlayMode.LOOP);
        memeAnimationCache.put(imagePath, staticAnim);
        return staticAnim;
    }

    private void refreshActions(GameState state, boolean myTurn, BoardCell currentCell, boolean moverAnimating) {
        if (isShieldAnimRunning) {
            if (actionRow != null) {
                actionRow.setVisible(false);
            }
            return;
        }
        int activePlayerId = state != null && state.getCurrentPlayer() != null ? state.getCurrentPlayer().id : -1;
        boolean isMovementActive = hasPendingMove
                || (boardRenderer != null && boardRenderer.isAnimating(activePlayerId));

        boolean validPhaseForRoll = state != null && (state.currentPhase == GameState.GamePhase.PLAYING
                || state.currentPhase == GameState.GamePhase.ROLLING_DICE
                || (state.currentPhase == GameState.GamePhase.PLAYER_ACTION && !state.hasRolledThisTurn));

        boolean canRoll = myTurn && validPhaseForRoll && (state != null && !state.hasRolledThisTurn)
                && (state != null && !state.awaitingReroll) && !isMovementActive;

        Player cp = state != null ? state.getCurrentPlayer() : null;
        int currentLocalId = game.getClient().getLocalPlayerId();
        String auditSig = myTurn + ":" + (cp != null ? cp.id : -1) + ":" + (state != null && state.hasRolledThisTurn)
                + ":" + isMovementActive + ":" + (state != null ? state.currentPhase : "null") + ":" + canRoll;
        if (!auditSig.equals(lastCanRollAuditSig) || Gdx.input.justTouched()) {
            lastCanRollAuditSig = auditSig;
            Gdx.app.log("CAN_ROLL_AUDIT", "isMyTurn=" + myTurn +
                    " (myId=" + currentLocalId + ", currentTurn=" + (cp != null ? cp.id : -1) + ")" +
                    ", hasRolled=" + (state != null && state.hasRolledThisTurn) +
                    ", isMovementActive=" + isMovementActive +
                    ", hasPendingMove=" + hasPendingMove +
                    ", isDiceRolling=" + (diceAnimPhase == DiceAnimPhase.ROLLING) +
                    ", phase=" + (state != null ? state.currentPhase : "null") +
                    ", canRoll=" + canRoll);
        }

        boolean canBuyOrPass = myTurn && state.currentPhase == GameState.GamePhase.PLAYER_ACTION
                && currentCell != null && currentCell.type == BoardCell.Type.SITUATION && !moverAnimating
                && !isMovementActive;
        boolean canEndTurn = myTurn
                && (state.currentPhase == GameState.GamePhase.PLAYING
                        || state.currentPhase == GameState.GamePhase.PLAYER_ACTION)
                && state.hasRolledThisTurn
                && !moverAnimating && !isMovementActive;
        boolean showTurnControls = myTurn && (state.currentPhase == GameState.GamePhase.PLAYING
                || state.currentPhase == GameState.GamePhase.ROLLING_DICE
                || state.currentPhase == GameState.GamePhase.PLAYER_ACTION);
        boolean canBid = state.currentPhase == GameState.GamePhase.AUCTION
                && game.getClient().getLocalPlayerId() == state.auctionCurrentPlayerId && !isMovementActive;
        boolean canUseMemeBank = state.currentPhase == GameState.GamePhase.MEME_BANK_ACTION
                && game.getClient().getLocalPlayerId() == state.memeBankPlayerId && !moverAnimating
                && !isMovementActive;
        Player localPlayer = state.getPlayerById(game.getClient().getLocalPlayerId());
        boolean isLocalInDebt = state != null && state.pendingDebtPlayerId == game.getClient().getLocalPlayerId();
        if (isLocalInDebt) {
            canEndTurn = false;
        }

        boolean canDepositToMemeBank = canUseMemeBank && localPlayer != null && localPlayer.memeBankBalance < 500;
        boolean canWithdrawFromMemeBank = canUseMemeBank && localPlayer != null && localPlayer.memeBankBalance > 0;

        if (pendingActionMode != BoardRenderer.PendingActionMode.NONE) {
            canRoll = false;
            canEndTurn = false;
        }

        setButtonsEnabled(canRoll, canBuyOrPass, canBuyOrPass, canEndTurn, canBid);
        diceButton.setTouchable(showTurnControls ? Touchable.enabled : Touchable.disabled);
        buyButton.setVisible(canBuyOrPass);
        passButton.setVisible(canBuyOrPass);
        placeBidButton.setVisible(canBid);
        cancelAuctionButton.setVisible(canBid);
        if (state.currentPhase == GameState.GamePhase.AUCTION) {
            auctionControlsRow.setVisible(canBid);
            waitingBidLabel.setVisible(!canBid);
            if (!canBid) {
                waitingBidLabel.setText(t("bid_waiting") + getAuctionTurnName(state));
            }
        } else {
            auctionControlsRow.setVisible(false);
            waitingBidLabel.setVisible(false);
        }
        memeBankDepositButton.setVisible(canDepositToMemeBank);
        memeBankWithdrawButton.setVisible(canWithdrawFromMemeBank);
        setActorDisabled(memeBankDepositButton, !canDepositToMemeBank);
        setActorDisabled(memeBankWithdrawButton, !canWithdrawFromMemeBank);
        setModalVisible(buyOrAuctionModal, canBuyOrPass && !isMovementActive);
        setModalVisible(auctionModal,
                state.currentPhase == GameState.GamePhase.AUCTION && !moverAnimating && !isMovementActive);
        setModalVisible(memeBankModal,
                state.currentPhase == GameState.GamePhase.MEME_BANK_ACTION && canUseMemeBank && !isMovementActive);
        if (state.currentPhase == GameState.GamePhase.MEME_BANK_ACTION && canUseMemeBank && !isMovementActive) {
            int deposit = localPlayer != null ? localPlayer.memeBankBalance : 0;
            updateMemeBankControls(deposit, localPlayer);
        }
        setModalVisible(debtModal, isLocalInDebt && !isMovementActive);
        if (isLocalInDebt && !isMovementActive) {
            debtModal.toFront();
            updateDebtModal(state, localPlayer);
        }
        if (isMovementActive) {
            setModalVisible(buyOrAuctionModal, false);
            setModalVisible(memeBankModal, false);
            setModalVisible(debtModal, false);
        }

        if (state.currentPhase == GameState.GamePhase.AUCTION) {
            auctionTimerLabel.setText(String.valueOf(state.currentAuctionTime));
            auctionTurnLabel.setText(t("auction_turn") + " " + getAuctionTurnName(state));

            if (state.auctionCellId >= 0 && boardCells != null && state.auctionCellId < boardCells.size()) {
                BoardCell cell = boardCells.get(state.auctionCellId);
                auctionCellNameLabel.setText(t("auction_cell") + " " + cell.name);
                if (cellTextures != null && state.auctionCellId < cellTextures.length
                        && cellTextures[state.auctionCellId] != null) {
                    auctionModalCellImage.setDrawable(
                            new TextureRegionDrawable(new TextureRegion(cellTextures[state.auctionCellId])));
                }
            } else {
                auctionCellNameLabel.setText(t("auction_cell") + " —");
            }

            int highestBidVal = 0;
            int highestBidderId = -1;
            if (state.auctionBids != null) {
                for (java.util.Map.Entry<Integer, Integer> entry : state.auctionBids.entrySet()) {
                    if (entry.getValue() > highestBidVal) {
                        highestBidVal = entry.getValue();
                        highestBidderId = entry.getKey();
                    }
                }
            }

            if (highestBidderId != -1) {
                Player bidder = state.getPlayerById(highestBidderId);
                String bidderName = bidder != null ? bidder.name : ("Player_" + highestBidderId);
                auctionLastOfferLabel.setText(bidderName + " - " + highestBidVal + " ");
                auctionLastOfferCoinIcon.setVisible(true);
            } else {
                auctionLastOfferLabel.setText(t("auction_no_bids"));
                auctionLastOfferCoinIcon.setVisible(false);
            }

            int turnPlayerId = state.auctionCurrentPlayerId;
            if (turnPlayerId != lastHandledAuctionTurnPlayerId || highestBidVal != lastHandledAuctionHighestBid) {
                lastHandledAuctionTurnPlayerId = turnPlayerId;
                lastHandledAuctionHighestBid = highestBidVal;
                if (highestBidVal == 0) {
                    bidValue = 10;
                } else {
                    bidValue = highestBidVal;
                }
                updateBidRowUi();
                auctionErrorLabel.setText("");
            }
        } else {
            lastHandledAuctionTurnPlayerId = -1;
            lastHandledAuctionHighestBid = -1;
            auctionLabel.setText("");
            auctionModalLabel.setText("");
            memeBankModalLabel.setText("");
        }
        if (state.currentPhase == GameState.GamePhase.MEME_BANK_ACTION && canUseMemeBank) {
            int bankBalance = localPlayer == null ? 0 : localPlayer.memeBankBalance;
            String line1 = t("mb_balance") + ": " + bankBalance;
            String line2 = t("mb_max");
            auctionLabel.setText(t("meme_bank_title") + ": " + line1 + " | " + line2);
            memeBankModalLabel.setText(line1 + "\n" + line2);
        }
        int localId = game.getClient().getLocalPlayerId();
        boolean awaiting = state.awaitingReroll && state.rerollPlayerId == localId;
        boolean modPending = state.moderatorChoicePending && state.moderatorPlayerId == localId;
        boolean awaitingEff = awaiting && !moverAnimating && !isMovementActive;
        boolean modPendingEff = modPending && !moverAnimating && !isMovementActive;
        Player localP = state != null ? state.getPlayerById(localId) : null;
        boolean inJail = localP != null && localP.inJail;

        if (modPendingEff) {
            diceHintLabel.setText("");
            diceHintLabel.setVisible(false);
            if (moderatorPromptTable != null) {
                moderatorPromptTable.setVisible(true);
            }
        } else {
            if (moderatorPromptTable != null) {
                moderatorPromptTable.setVisible(false);
            }
            if (awaitingEff) {
                diceHintLabel.setText(t("doge_choice"));
                diceHintLabel.setVisible(true);
            } else if (inJail && showTurnControls) {
                diceHintLabel.setText(t("jail_roll_attempt") + " " + (localP.jailRollAttempts + 1) + "/3");
                diceHintLabel.setVisible(true);
            } else {
                diceHintLabel.setText("");
                diceHintLabel.setVisible(false);
            }
        }
        Role localRole = Role.of(state.playerRoles == null ? null : state.playerRoles.get(localId));
        boolean canSteal = localRole == Role.SCAMMER && myTurn
                && (state.currentPhase == GameState.GamePhase.PLAYING
                        || state.currentPhase == GameState.GamePhase.PLAYER_ACTION)
                && !state.roleUsedThisRound.getOrDefault(localId, false) && !awaiting && !modPending
                && !isMovementActive;
        stealButton.setDisabled(!canSteal);
        rebuildActionRow(showTurnControls, canEndTurn, canSteal, awaitingEff, modPendingEff, inJail);
    }

    private void refreshActions(GameState state) {
        if (state == null) return;
        int localId = game.getClient().getLocalPlayerId();
        Player localPlayer = state.getPlayerById(localId);
        boolean myTurn = state.currentPlayerIndex >= 0 && state.currentPlayerIndex < state.players.size()
                && state.players.get(state.currentPlayerIndex).id == localId;
        BoardCell currentCell = (localPlayer != null && boardCells != null && localPlayer.position >= 0 && localPlayer.position < boardCells.size())
                ? boardCells.get(localPlayer.position) : null;
        int activePlayerId = state.getCurrentPlayer() != null ? state.getCurrentPlayer().id : -1;
        boolean moverAnimating = boardRenderer != null && boardRenderer.isAnimating(activePlayerId);
        refreshActions(state, myTurn, currentCell, moverAnimating);
    }

    private void changeBidBy(int delta) {
        GameState st = game.getLatestGameState();
        Player local = st == null ? null : st.getPlayerById(game.getClient().getLocalPlayerId());
        int maxBid = Math.max(10, local == null ? 0 : local.money);
        bidValue = Math.max(10, Math.min(maxBid, bidValue + delta));
        auctionErrorLabel.setText("");
        updateBidRowUi();
    }

    private void updateBidRowUi() {
        GameState st = game.getLatestGameState();
        Player local = st == null ? null : st.getPlayerById(game.getClient().getLocalPlayerId());
        int maxBid = Math.max(10, local == null ? 0 : local.money);
        bidValue = Math.max(10, Math.min(maxBid, bidValue));
        bidValueLabel.setText(String.valueOf(bidValue));
        setActorDisabled(minusBidButton, bidValue <= 10);
        setActorDisabled(plusBidButton, bidValue >= maxBid);
        setActorDisabled(minusHundredBidButton, bidValue <= 10);
        setActorDisabled(plusHundredBidButton, bidValue >= maxBid);
    }

    private void rebuildActionRow(boolean showTurnControls, boolean canEndTurn, boolean canSteal,
            boolean awaitingEff, boolean modPendingEff, boolean inJail) {
        if (isShieldAnimRunning) {
            if (actionRow != null) {
                actionRow.setVisible(false);
            }
            return;
        }
        boolean diceShow = showTurnControls && !awaitingEff && !modPendingEff;
        boolean payJailShow = showTurnControls && inJail && !awaitingEff && !modPendingEff;
        boolean endTurnShow = canEndTurn && !awaitingEff && !modPendingEff;
        boolean stayShow = awaitingEff && !modPendingEff;
        boolean plusTwoShow = awaitingEff && !modPendingEff;
        boolean modSkipShow = modPendingEff;
        boolean modTakeShow = modPendingEff;
        String sig = (diceShow ? "1" : "0") + (payJailShow ? "1" : "0") + (endTurnShow ? "1" : "0")
                + (canSteal ? "1" : "0") + (stayShow ? "1" : "0") + (plusTwoShow ? "1" : "0")
                + (modSkipShow ? "1" : "0") + (modTakeShow ? "1" : "0");
        if (sig.equals(lastActionRowSig) || actionRow == null) {
            return;
        }
        lastActionRowSig = sig;
        Gdx.app.log("ACTION_ROW", "rebuildActionRow() EXECUTED! sig: " + sig);
        actionRow.clearChildren();
        actionRow.defaults().center();
        boolean first = true;
        if (stayShow && plusTwoShow) {
            Table buttonsRow = new Table();
            buttonsRow.center();
            buttonsRow.defaults().center();
            buttonsRow.add(stayButton).size(160f, 60f).padRight(20f);
            buttonsRow.add(plusTwoButton).size(160f, 60f);
            actionRow.add(buttonsRow).center().fillX();

            actionRow.clearActions();
            diceHintLabel.clearActions();
            actionRow.getColor().a = 0f;
            diceHintLabel.getColor().a = 0f;
            actionRow.addAction(Actions.sequence(Actions.alpha(0f), Actions.delay(0.8f), Actions.fadeIn(0.3f)));
            diceHintLabel.addAction(Actions.sequence(Actions.alpha(0f), Actions.delay(0.8f), Actions.fadeIn(0.3f)));
        } else if (modSkipShow && modTakeShow) {
            Table buttonsRow = new Table();
            buttonsRow.center();
            buttonsRow.defaults().center();
            buttonsRow.add(moderatorSkipButton).size(168f, 64f).padRight(20f);
            buttonsRow.add(moderatorTakeJailButton).size(146f, 64f);
            actionRow.add(buttonsRow).center().fillX();

            actionRow.clearActions();
            if (moderatorPromptTable != null) {
                moderatorPromptTable.clearActions();
                moderatorPromptTable.getColor().a = 0f;
                moderatorPromptTable.addAction(Actions.sequence(Actions.alpha(0f), Actions.delay(0.8f), Actions.fadeIn(0.3f)));
            }
            actionRow.getColor().a = 0f;
            actionRow.addAction(Actions.sequence(Actions.alpha(0f), Actions.delay(0.8f), Actions.fadeIn(0.3f)));
        } else {
            actionRow.clearActions();
            diceHintLabel.clearActions();
            if (moderatorPromptTable != null) {
                moderatorPromptTable.clearActions();
                moderatorPromptTable.getColor().a = 1f;
            }
            actionRow.getColor().a = 1f;
            diceHintLabel.getColor().a = 1f;
            if (diceShow) {
                actionRow.add(diceButton).size(150, 64);
                first = false;
            }
            if (payJailShow) {
                actionRow.add(payJailFineButton).size(184f, 64f).padLeft(first ? 0f : 8f);
                first = false;
            }
            if (endTurnShow) {
                actionRow.add(endTurnButton).size(150, 64).padLeft(first ? 0f : 8f);
                first = false;
            }
            if (canSteal) {
                actionRow.add(stealButton).size(150, 64).padLeft(first ? 0f : 8f);
                first = false;
            }
            GameState st = game.getLatestGameState();
            int localId = game.getClient().getLocalPlayerId();
            Player localP = st != null ? st.getPlayerById(localId) : null;
            boolean bankruptEventShow = st != null && localP != null && localP.isBankrupt && st.juryIds != null
                    && st.juryIds.contains(localId)
                    && (st.bankruptEventUsedRound == null || !st.bankruptEventUsedRound.containsKey(localId));
            if (bankruptEventShow) {
                actionRow.add(bankruptEventButton).size(150, 64).padLeft(first ? 0f : 8f);
                first = false;
            }
        }
        if (diceOverlay != null) {
            diceOverlay.setTouchable(Touchable.childrenOnly);
            diceOverlay.toFront();
        }
        if (actionRow != null) {
            actionRow.setTouchable(Touchable.enabled);
            actionRow.toFront();
        }
    }

    private void setButtonsEnabled(boolean roll, boolean buy, boolean pass, boolean endTurn, boolean bid) {
        diceButton.setDisabled(!roll);
        diceButton.setTouchable(roll ? Touchable.enabled : Touchable.disabled);
        buyButton.setDisabled(!buy);
        buyButton.setTouchable(buy ? Touchable.enabled : Touchable.disabled);
        passButton.setDisabled(!pass);
        passButton.setTouchable(pass ? Touchable.enabled : Touchable.disabled);
        endTurnButton.setDisabled(!endTurn);
        endTurnButton.setTouchable(endTurn ? Touchable.enabled : Touchable.disabled);
        placeBidButton.setDisabled(!bid);
        placeBidButton.setTouchable(bid ? Touchable.enabled : Touchable.disabled);
        cancelAuctionButton.setVisible(bid);
        setActorDisabled(cancelAuctionButton, !bid);
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
        if (visible) {
            modal.toFront();
        }
    }

    @Override
    public void show() {
        Gdx.input.setInputProcessor(stage);
        hasPendingMove = false;
        pendingMoveTimer = 0f;
        diceAnimPhase = DiceAnimPhase.IDLE;
        lastTurnPlayerId = -1;
        lastActionRowSig = "";
        lastKnownPositions.clear();
        lastInJailStates.clear();
        lastKnownShields.clear();
        pendingShieldBreak = false;
        if (activeShieldAnimGroup != null) {
            activeShieldAnimGroup.remove();
            activeShieldAnimGroup = null;
        }
        pendingJailStep.clear();
        if (roleTooltip != null) roleTooltip.setVisible(false);
        if (rentTooltip != null) rentTooltip.setVisible(false);
        lastHoveredRentCell = -1;
    }

    @Override
    public void render(float delta) {
        if (Gdx.input.justTouched()) {
            com.badlogic.gdx.math.Vector2 touch = stage.screenToStageCoordinates(
                    new com.badlogic.gdx.math.Vector2(Gdx.input.getX(), Gdx.input.getY()));
            com.badlogic.gdx.scenes.scene2d.Actor hit = stage.hit(touch.x, touch.y, true);
            Gdx.app.log("CLICK_DIAG", "Touch: " + touch + " -> HIT ACTOR: " + hit +
                    (hit != null ? " (parent=" + hit.getParent() + ", touchable=" + hit.getTouchable() + ")" : ""));
        }
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        renderBackground();
        GameState state = game.getLatestGameState();
        boardRenderer.update(delta);
        if (pendingBattleOverlay && state != null && state.currentPhase == GameState.GamePhase.MEME_BATTLE) {
            boolean isRollingOrMoving = diceAnimPhase != DiceAnimPhase.IDLE || hasPendingMove
                    || (boardRenderer != null && boardRenderer.hasActiveAnimations());
            boolean isDiceVisible = diceOverlay != null && diceOverlay.isVisible();
            if (!isRollingOrMoving && !isDiceVisible) {
                battleOverlayDelayTimer -= delta;
                if (battleOverlayDelayTimer <= 0f) {
                    pendingBattleOverlay = false;
                    refreshBattleOverlay(state);
                }
            }
        }
        RollDiceResponse pendingRoll = game.consumePendingDiceRoll();
        if (pendingRoll != null && !(pendingRoll.dice1 == 0 && pendingRoll.dice2 == 0)) {
            Gdx.app.log("DICE_TRACE", "5. Client received roll packet: d1=" + pendingRoll.dice1 + ", d2="
                    + pendingRoll.dice2 + ", total=" + pendingRoll.total + ", newPos=" + pendingRoll.newPosition);
            startDiceRoll(pendingRoll.dice1, pendingRoll.dice2, pendingRoll.total);
        }
        refreshUi(state);
        if (Gdx.input.justTouched()) {
            Vector2 touch = stage.screenToStageCoordinates(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
            Actor hitActor = stage.hit(touch.x, touch.y, true);
            Gdx.app.log("DICE_DEBUG", "Hit actor at touch (" + touch.x + ", " + touch.y + "): " + hitActor
                    + " | inputProcessor=" + Gdx.input.getInputProcessor());
        }
        updateMoneyDeltaAnimation(delta);
        updateAvatarFrameAnimation(delta);
        updateDiceRollAnimation(delta);
        checkPendingMove(delta);
        if (activeShieldAnimGroup != null && activeShieldAnimGroup.hasParent()) {
            activeShieldAnimGroup.toFront();
        }
        if (pendingShieldBreak && !hasPendingMove && (boardRenderer == null || !boardRenderer.isMovementActive()) && diceAnimPhase == DiceAnimPhase.IDLE) {
            pendingShieldBreak = false;
            playShieldBreakAnimation();
        }
        if (pendingStartMessage != null && !hasPendingMove && (boardRenderer == null || !boardRenderer.isMovementActive()) && diceAnimPhase == DiceAnimPhase.IDLE) {
            showLocalNotification(pendingStartMessage);
            pendingStartMessage = null;
        }
        boardRenderer.setCameraLocked(isCameraBlocked());
        boardRenderer.updateCamera(delta, state);
        boardRenderer.render(boardCells, state);
        updateRoleTooltip();
        updateRentTooltip();
        updateDevPanel(state);
        refreshNotification(state, delta);
        if (chatWidget != null) {
            chatWidget.refresh();
        }
        if (handMemesTable != null) {
            handMemesTable.act(delta);
        }
        stage.act(delta);
        stage.draw();
    }

    private void checkPendingMove(float delta) {
        if (hasPendingMove) {
            pendingMoveTimer += delta;
            boolean diceDone = diceAnimPhase != DiceAnimPhase.ROLLING;
            boolean timeout = pendingMoveTimer >= 1.2f;
            if (diceDone || timeout) {
                boardRenderer.animateMovement(pendingMovePlayerId, pendingFromCell, pendingToCell);
                hasPendingMove = false;
                pendingMoveTimer = 0f;
            }
        }

        if (boardRenderer != null && !pendingJailStep.isEmpty()) {
            Iterator<Map.Entry<Integer, Integer>> it = pendingJailStep.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<Integer, Integer> entry = it.next();
                int pid = entry.getKey();
                int step = entry.getValue();

                if (step == 1 && !boardRenderer.isAnimating(pid) && diceAnimPhase == DiceAnimPhase.IDLE
                        && !hasPendingMove) {
                    entry.setValue(2);
                    boardRenderer.setVisualPosition(pid, 30);
                    String jailMsg = language == Language.RU
                            ? "Copyright Infringement!\nОтправляйся в Бан!"
                            : "Copyright Infringement!\nGo to Jail!";
                    updateNotificationModalContent(jailMsg);
                    if (chatWidget != null) {
                        chatWidget.showSystemMessage(jailMsg.replace("\n", " "));
                    }
                    turnNotificationModal.pack();
                    if (notificationModalContainer != null) {
                        notificationModalContainer.invalidateHierarchy();
                        notificationModalContainer.toFront();
                    }
                    setModalVisible(turnNotificationModal, true);
                    turnNotificationModal.toFront();
                    jailNotificationTimer = 2.5f;
                } else if (step == 2) {
                    boardRenderer.setVisualPosition(pid, 30);
                    jailNotificationTimer -= delta;
                    if (jailNotificationTimer <= 0f) {
                        setModalVisible(turnNotificationModal, false);
                        entry.setValue(3);
                        boardRenderer.animateMovement(pid, 30, 10, true);
                    }
                } else if (step == 3 && !boardRenderer.isAnimating(pid)) {
                    boardRenderer.setVisualPosition(pid, 10);
                    it.remove();
                }
            }
        }

        // Флаш отложенных дельта-бейджей: стартуем анимацию монет только после остановки фишек
        if (!pendingMoneyDeltas.isEmpty() && boardRenderer != null) {
            boolean movementActive = hasPendingMove
                    || boardRenderer.hasActiveAnimations()
                    || diceAnimPhase != DiceAnimPhase.IDLE
                    || !pendingJailStep.isEmpty();
            if (!movementActive) {
                for (Map.Entry<Integer, Integer> entry : new ArrayList<>(pendingMoneyDeltas.entrySet())) {
                    startMoneyDeltaAnimation(entry.getKey(), entry.getValue());
                }
                pendingMoneyDeltas.clear();
            }
        }
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
            boolean isStartMsg = state.notificationText.contains("через Старт")
                    || state.notificationText.contains("through Start")
                    || state.notificationText.contains("passed Start");
            boolean movementActive = hasPendingMove
                    || (boardRenderer != null && boardRenderer.hasActiveAnimations())
                    || diceAnimPhase != DiceAnimPhase.IDLE
                    || !pendingJailStep.isEmpty();
            if (isStartMsg && movementActive) {
                lastShownNotificationTimestamp = state.notificationTimestamp;
                pendingStartMessage = state.notificationText;
                return;
            }
            if (movementActive) {
                return;
            }
            lastShownNotificationTimestamp = state.notificationTimestamp;
            showLocalNotification(state.notificationText);
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
        if (banShieldTexture != null)
            banShieldTexture.dispose();
        if (brokeShieldTexture != null)
            brokeShieldTexture.dispose();
        if (rulesButtonTexture != null)
            rulesButtonTexture.dispose();
        if (skipButtonTexture != null)
            skipButtonTexture.dispose();
        if (exitToMenuButtonTexture != null)
            exitToMenuButtonTexture.dispose();
        if (settingsButtonTexture != null)
            settingsButtonTexture.dispose();
        if (depositButtonTexture != null)
            depositButtonTexture.dispose();
        if (withdrawButtonTexture != null)
            withdrawButtonTexture.dispose();
        if (participateButtonTexture != null)
            participateButtonTexture.dispose();
        if (declineButtonTexture != null)
            declineButtonTexture.dispose();
        if (applyButtonTexture != null)
            applyButtonTexture.dispose();
        if (plusTwoButtonTexture != null)
            plusTwoButtonTexture.dispose();
        if (stayButtonTexture != null)
            stayButtonTexture.dispose();
        if (dealButtonTexture != null)
            dealButtonTexture.dispose();
        if (stealButtonTexture != null)
            stealButtonTexture.dispose();
        if (rectForMoneyTexture != null)
            rectForMoneyTexture.dispose();
        if (longRectForMoneyTexture != null)
            longRectForMoneyTexture.dispose();
        if (twitchTexture != null)
            twitchTexture.dispose();
        if (houseIconTexture != null)
            houseIconTexture.dispose();
        if (greenHouseTexture != null)
            greenHouseTexture.dispose();
        if (redHouseTexture != null)
            redHouseTexture.dispose();
        if (changeTopicBtnTexture != null)
            changeTopicBtnTexture.dispose();
        if (payBtnRuTexture != null)
            payBtnRuTexture.dispose();
        if (payBtnEnTexture != null)
            payBtnEnTexture.dispose();
        for (Texture frameTexture : avatarFrameByRole.values()) {
            if (frameTexture != null)
                frameTexture.dispose();
        }
        if (diceFaceATexture != null)
            diceFaceATexture.dispose();
        if (diceFaceBTexture != null)
            diceFaceBTexture.dispose();
        if (diceShadowTexture != null)
            diceShadowTexture.dispose();
        if (timerCircleTexture != null) {
            timerCircleTexture.dispose();
        }
        notificationWindowTexture.dispose();
        buyAndAuctionWindowTexture.dispose();
        auctionOrMemeBankWindowTexture.dispose();
        tenUpTexture.dispose();
        tenDownTexture.dispose();
        if (hundUpTexture != null)
            hundUpTexture.dispose();
        if (hundDownTexture != null)
            hundDownTexture.dispose();
        startBattleTexture.dispose();
        inputMemeBattleTexture.dispose();
        cardBoardTexture.dispose();
        if (dealModalTexture != null)
            dealModalTexture.dispose();
        if (dealSliderBgTexture != null)
            dealSliderBgTexture.dispose();
        if (dealSliderKnobTexture != null)
            dealSliderKnobTexture.dispose();
        inputTexture.dispose();
        backgroundTexture.dispose();
        for (Texture cellTexture : cellTextures)
            cellTexture.dispose();
        for (Texture memeTexture : memeTextureCache.values()) {
            if (memeTexture != null)
                memeTexture.dispose();
        }
        for (Texture gifTex : dynamicGifTextures) {
            if (gifTex != null) {
                try {
                    gifTex.dispose();
                } catch (Exception ignored) {
                }
            }
        }
        dynamicGifTextures.clear();
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
        return "";
    }

    private Drawable panel(Color color) {
        return VisUI.getSkin().newDrawable("white", color);
    }

    private void createDiceRollOverlay() {
        diceRollOverlay = new Group();
        diceRollOverlay.setTouchable(Touchable.disabled);
        diceRollOverlay.setVisible(false);
        // circle, а не circleSmooth — круг заполняет всю текстуру, эллипс будет гладким
        diceShadowTexture = UiShapes.circle(64, new Color(0.06f, 0.06f, 0.10f, 0.45f));
        diceShadowLeft = new Image(diceShadowTexture);
        diceShadowLeft.setScaling(Scaling.stretch); // явный stretch на весь размер Image
        diceShadowRight = new Image(diceShadowTexture);
        diceShadowRight.setScaling(Scaling.stretch);
        diceImageLeft = new Image();
        diceImageRight = new Image();
        diceImageLeft.setSize(DICE_SIZE, DICE_SIZE);
        diceImageRight.setSize(DICE_SIZE, DICE_SIZE);
        diceTotalLabel = new VisLabel("");
        diceTotalLabel.setColor(BATTLE_TEXT_COLOR);
        diceTotalLabel.setFontScale(1.4f);
        diceTotalLabel.setAlignment(Align.center);
        diceTotalLabel.setVisible(false);
        diceRollOverlay.addActor(diceShadowLeft);
        diceRollOverlay.addActor(diceShadowRight);
        diceRollOverlay.addActor(diceImageLeft);
        diceRollOverlay.addActor(diceImageRight);
        diceRollOverlay.addActor(diceTotalLabel);
        stage.addActor(diceRollOverlay);
    }

    private void startDiceRoll(int dice1, int dice2, int total) {
        Texture a = diceFaceATexture != null ? diceFaceATexture
                : (diceFaceBTexture != null ? diceFaceBTexture : diceButtonTexture);
        Texture b = diceFaceBTexture != null ? diceFaceBTexture
                : (diceFaceATexture != null ? diceFaceATexture : diceButtonTexture);
        if (a == null || b == null || diceRollOverlay == null) {
            return; // нет текстур кубиков — анимацию не показываем
        }
        diceIsDoubles = dice1 == dice2;
        diceImageLeft.setDrawable(new TextureRegionDrawable(new TextureRegion(a)));
        diceImageRight.setDrawable(new TextureRegionDrawable(new TextureRegion(b)));
        diceTotalLabel.setText(diceIsDoubles ? total + " " + t("dice_double") : String.valueOf(total));
        diceTotalLabel.pack();
        diceTotalLabel.setVisible(false);
        diceRollOverlay.setColor(1f, 1f, 1f, 1f);
        diceRollOverlay.setVisible(true);
        diceAnimPhase = DiceAnimPhase.ROLLING;
        diceAnimTimer = 0f;
        diceFlickerTimer = 0f;
        applyDiceRollFrame(0f);
    }

    private Rectangle getActionRowStageBounds() {
        if (actionRow == null) {
            return new Rectangle(WORLD_WIDTH / 2f, WORLD_HEIGHT / 2f, 0f, 0f);
        }
        Vector2 pos = actionRow.localToStageCoordinates(new Vector2(0f, 0f));
        return new Rectangle(pos.x, pos.y, actionRow.getWidth(), actionRow.getHeight());
    }

    /**
     * Кадр полёта: t = 0..1. Кубики летят сверху и садятся ПОД рядом кнопок хода.
     */
    private void applyDiceRollFrame(float t) {
        Rectangle row = getActionRowStageBounds();
        float rowCenterX = row.x + row.width / 2f;
        float rowBottom = row.y;
        float half = DICE_SIZE / 2f;
        float groundY = rowBottom - 24f - DICE_SIZE; // линия посадки — ТА ЖЕ, что и раньше
        // Посадка — без изменений
        float landLX = rowCenterX - 58f - half;
        float landRX = rowCenterX + 58f - half;
        // Вылет — левее и выше, чем раньше
        float startLX = rowCenterX - 430f - half;
        float startRX = rowCenterX - 340f - half;
        float startH = 300f; // высота вылета над плоскостью
        // Точка удара о плоскость — между вылетом и посадкой
        float bounceLX = rowCenterX - 200f - half;
        float bounceRX = rowCenterX - 120f - half;
        float tb = DICE_BOUNCE_T;
        float xL, xR, hL, hR;
        if (t < tb) {
            // Сегмент A: падение по дуге из точки вылета в точку удара
            float k = t / tb;
            xL = startLX + (bounceLX - startLX) * k;
            xR = startRX + (bounceRX - startRX) * k;
            hL = startH * (1f - k) + 60f * (float) Math.sin(Math.PI * k);
            hR = startH * (1f - k) + 44f * (float) Math.sin(Math.PI * k);
        } else {
            // Сегмент B: отскок по меньшей дуге до точки посадки
            float k = (t - tb) / (1f - tb);
            xL = bounceLX + (landLX - bounceLX) * k;
            xR = bounceRX + (landRX - bounceRX) * k;
            hL = 84f * (float) Math.sin(Math.PI * k);
            hR = 64f * (float) Math.sin(Math.PI * k);
        }
        // Сквош в момент удара о плоскость
        float q = Math.max(0f, 1f - Math.abs(t - tb) / 0.07f);
        float sqX = 1f + 0.22f * q;
        float sqY = 1f - 0.28f * q;
        diceImageLeft.setOrigin(half, half);
        diceImageRight.setOrigin(half, half);
        diceImageLeft.setPosition(xL, groundY + hL);
        diceImageRight.setPosition(xR, groundY + hR);
        diceImageLeft.setRotation((1f - t) * 620f);
        diceImageRight.setRotation(-(1f - t) * 760f);
        diceImageLeft.setScale(sqX, sqY);
        diceImageRight.setScale(sqX, sqY);
        // Тени: следуют по X за кубиками, сжимаются/светлеют на высоте,
        // в моменты удара (h=0) плотные + пульс расширения
        applyDiceShadow(diceShadowLeft, xL, hL, startH, q, groundY);
        applyDiceShadow(diceShadowRight, xR, hR, startH, q, groundY);
    }

    /**
     * Тень кубика: diceX — левый край кубика, h — высота над плоскостью,
     * maxH — высота вылета, impactPulse — 1 в кадр удара, groundY — линия посадки.
     */
    private void applyDiceShadow(Image shadow, float diceX, float h, float maxH,
            float impactPulse, float groundY) {
        float hn = Math.max(0f, Math.min(1f, h / maxH));
        // Ширина: на высоте сжимается до ~55%, в момент удара расширяется пульсом
        float w = DICE_SIZE * (0.90f - 0.35f * hn) * (1f + 0.28f * impactPulse);
        float hh = w * 0.34f; // более плоский эллипс (было 0.32f — почти то же, но чуть мягче)
        shadow.setSize(w, hh);
        // Центр тени под центром кубика; небольшое смещение вниз для «посадки»
        shadow.setPosition(diceX + DICE_SIZE / 2f - w / 2f, groundY - hh / 2f + 6f);
        // Альфа: плотная у земли, бледная в полёте, ярче в момент удара
        float alpha = Math.min(1f, 0.85f - 0.45f * hn + 0.30f * impactPulse);
        shadow.setColor(1f, 1f, 1f, alpha);
    }

    private void updateDiceRollAnimation(float delta) {
        if (diceAnimPhase == DiceAnimPhase.IDLE || diceRollOverlay == null) {
            return;
        }
        diceAnimTimer += delta;
        switch (diceAnimPhase) {
            case ROLLING: {
                float t = Math.min(1f, diceAnimTimer / DICE_ROLL_DURATION);
                applyDiceRollFrame(t);
                if (t < 0.78f) { // грани мельтешат только в полёте, перед посадкой фиксируются
                    diceFlickerTimer += delta;
                    if (diceFlickerTimer >= 0.08f) {
                        diceFlickerTimer = 0f;
                        Drawable tmp = diceImageLeft.getDrawable();
                        diceImageLeft.setDrawable(diceImageRight.getDrawable());
                        diceImageRight.setDrawable(tmp);
                    }
                }
                if (t >= 1f) {
                    diceAnimPhase = DiceAnimPhase.SETTLE;
                    diceAnimTimer = 0f;
                }
                break;
            }
            case SETTLE: {
                float k = Math.min(1f, diceAnimTimer / DICE_SETTLE_DURATION);
                float e = 1f - (1f - k) * (1f - k);
                applyDiceRollFrame(1f);
                float q = 1f - e;
                diceImageLeft.setRotation(14f * q);
                diceImageRight.setRotation(-14f * q);
                diceImageLeft.setScale(1f + 0.16f * q, 1f - 0.20f * q);
                diceImageRight.setScale(1f + 0.16f * q, 1f - 0.20f * q);
                Rectangle row = getActionRowStageBounds();
                float groundY = row.y - 24f - DICE_SIZE;
                // maxH = 300f — как в полёте, чтобы формула hn=h/maxH совпадала
                applyDiceShadow(diceShadowLeft, diceImageLeft.getX(), 0f, 300f, q, groundY);
                applyDiceShadow(diceShadowRight, diceImageRight.getX(), 0f, 300f, q, groundY);
                diceTotalLabel.setVisible(true);
                float ls = e < 0.7f ? (e / 0.7f) * 1.15f : 1.15f - 0.15f * ((e - 0.7f) / 0.3f);
                diceTotalLabel.setFontScale(1.4f * Math.max(0.05f, ls));
                layoutDiceTotalLabel();
                if (k >= 1f) {
                    diceTotalLabel.setFontScale(1.4f);
                    diceAnimPhase = DiceAnimPhase.HOLD;
                    diceAnimTimer = 0f;
                }
                break;
            }
            case HOLD: {
                layoutDiceTotalLabel();
                if (diceAnimTimer >= DICE_HOLD_DURATION) {
                    diceAnimPhase = DiceAnimPhase.FADE;
                    diceAnimTimer = 0f;
                }
                break;
            }
            case FADE: {
                float f = Math.min(1f, diceAnimTimer / DICE_FADE_DURATION);
                float a = 1f - f;
                diceImageLeft.setColor(1f, 1f, 1f, a);
                diceImageRight.setColor(1f, 1f, 1f, a);
                diceShadowLeft.setColor(1f, 1f, 1f, a);
                diceShadowRight.setColor(1f, 1f, 1f, a);
                Color base = BATTLE_TEXT_COLOR;
                diceTotalLabel.setColor(base.r, base.g, base.b, a);
                if (f >= 1f) {
                    diceAnimPhase = DiceAnimPhase.IDLE;
                    diceRollOverlay.setVisible(false);
                    diceImageLeft.setColor(1f, 1f, 1f, 1f);
                    diceImageRight.setColor(1f, 1f, 1f, 1f);
                    diceShadowLeft.setColor(1f, 1f, 1f, 1f);
                    diceShadowRight.setColor(1f, 1f, 1f, 1f);
                    diceTotalLabel.setColor(base.r, base.g, base.b, 1f);
                    diceTotalLabel.setVisible(false);
                }
                break;
            }
            default:
                break;
        }
    }

    private void layoutDiceTotalLabel() {
        Rectangle row = getActionRowStageBounds();
        float rowCenterX = row.x + row.width / 2f;
        float diceBottomY = row.y - 24f - DICE_SIZE; // линия посадки кубиков (низ)
        diceTotalLabel.setPosition(
                rowCenterX - diceTotalLabel.getWidth() / 2f,
                diceBottomY - diceTotalLabel.getHeight() - 8f);
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
        button.addListener(new com.badlogic.gdx.scenes.scene2d.utils.ClickListener() {
            @Override
            public void clicked(com.badlogic.gdx.scenes.scene2d.InputEvent event, float x, float y) {
                Gdx.app.log("DICE_TRACE", "1. ClickListener.clicked() TRIGGERED | disabled=" + button.isDisabled());
                GameState st = game.getLatestGameState();
                int localId = game.getClient().getLocalPlayerId();
                Player cp = st != null ? st.getCurrentPlayer() : null;
                int cpId = cp != null ? cp.id : -1;
                if (button.isDisabled()) {
                    Gdx.app.log("DICE_TRACE",
                            "CLICK INFO: diceButton was marked disabled! localId=" + localId + ", currentTurnId=" + cpId
                                    + ", phase=" + (st != null ? st.currentPhase : "null")
                                    + ", hasRolled=" + (st != null && st.hasRolledThisTurn)
                                    + ", awaitingReroll=" + (st != null && st.awaitingReroll)
                                    + ", hasPendingMove=" + hasPendingMove);
                }
                if (st != null && st.awaitingReroll && st.rerollPlayerId == localId) {
                    sendAction(GameActionRequest.ActionType.PLUS_TWO, 0, 0);
                } else {
                    int pid = localId;
                    if (pid <= 0 && cp != null) {
                        pid = cp.id;
                    }
                    if (pid <= 0 && game.isHost()) {
                        pid = 1;
                    }
                    Gdx.app.log("DICE_TRACE", "2. sendAction(ROLL_DICE) CALLED for localPlayerId=" + pid);
                    RollDiceRequest request = new RollDiceRequest();
                    request.playerId = pid;
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
        float diceW = 900f;
        float diceH = 300f;
        float diceY = (WORLD_HEIGHT - diceH) / 2f;
        diceOverlay.setBounds((WORLD_WIDTH - diceW) / 2f, diceY, diceW, diceH);

        // Модалка уведомлений центрируется через notificationModalContainer
        turnNotificationModal.pack();
        if (notificationModalContainer != null) {
            notificationModalContainer.invalidateHierarchy();
        }
        buyOrAuctionModal.setBounds((WORLD_WIDTH - BUY_AND_AUCTION_MODAL_MIN_W) / 2f,
                (WORLD_HEIGHT - BUY_AND_AUCTION_MODAL_MIN_H) / 2f, BUY_AND_AUCTION_MODAL_MIN_W,
                BUY_AND_AUCTION_MODAL_MIN_H);
        auctionModal.setBounds((WORLD_WIDTH - AUCTION_MODAL_MIN_W) / 2f, (WORLD_HEIGHT - AUCTION_MODAL_MIN_H) / 2f,
                AUCTION_MODAL_MIN_W, AUCTION_MODAL_MIN_H);
        memeBankModal.setBounds((WORLD_WIDTH - MEME_BANK_MODAL_MIN_W) / 2f, (WORLD_HEIGHT - MEME_BANK_MODAL_MIN_H) / 2f,
                MEME_BANK_MODAL_MIN_W, MEME_BANK_MODAL_MIN_H);
        debtModal.setBounds((WORLD_WIDTH - 560f) / 2f, (WORLD_HEIGHT - 260f) / 2f, 560f, 260f);
    }

    private void configureModal(Table modal, Texture texture, VisLabel contentLabel, float minWidth, float minHeight,
            boolean centerText) {
        modal.setVisible(false);
        modal.setTouchable(Touchable.disabled);
        modal.center();
        Table window = new Table();
        window.setBackground(new TextureRegionDrawable(new TextureRegion(texture)));
        window.pad(24f);
        modal.clearChildren();
        contentLabel.setWrap(true);
        contentLabel.setColor(BATTLE_TEXT_COLOR);
        float textWidth = Math.max(480f, minWidth * 0.88f);
        if (centerText) {
            contentLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
            window.add(contentLabel).width(textWidth).expand().center().pad(12f).row();
        } else {
            contentLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
            window.add(contentLabel).width(textWidth).center().pad(12f).row();
        }
        modal.add(window).minSize(minWidth, minHeight).fill().center();
    }

    private void setupModalControls() {
        addBuyAuctionControls();
        addAuctionControls();
        addMemeBankControls();
        setupDebtControls();
    }

    private void addBuyAuctionControls() {
        Table window = (Table) buyOrAuctionModal.getCells().first().getActor();
        window.clearChildren();
        window.pad(20f);
        buyAuctionModalLabel.setFontScale(1.1f);
        buyAuctionModalLabel.setColor(BATTLE_TEXT_COLOR);
        Table details = new Table();
        details.add(buyModalCellImage).size(180f, 180f).left();
        details.add(buyAuctionModalLabel).width(380f).left().padLeft(20f);
        window.add(details).center().padBottom(16f).row();
        Table controls = new Table();
        controls.add(buyButton).size(160f, 56f).padRight(16f);
        controls.add(passButton).size(160f, 56f);
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
        window.pad(16f, 20f, 16f, 20f);

        // 1. Header TopBar (3 cells for true geometric centering)
        Table topBar = new Table();
        topBar.add().size(44f, 44f); // Balancing empty cell on the left

        auctionTitleLabel.setText(t("auction_title"));
        auctionTitleLabel.setFontScale(1.05f);
        auctionTitleLabel.setColor(BATTLE_TEXT_COLOR);
        topBar.add(auctionTitleLabel).expandX().center();

        Stack timerStack = new Stack();
        Image timerBg = new Image(new TextureRegionDrawable(new TextureRegion(timerCircleTexture)));
        timerBg.setScaling(Scaling.fit);
        timerStack.add(timerBg);

        auctionTimerLabel.setFontScale(0.85f);
        auctionTimerLabel.setColor(BATTLE_TEXT_COLOR);
        auctionTimerLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        timerStack.add(auctionTimerLabel);

        topBar.add(timerStack).size(44f, 44f).right();
        window.add(topBar).growX().padBottom(14f).row();

        // 2. Middle Details (Left Col + Right Col)
        Table details = new Table();
        details.center();

        // Left block (Cell image 200x200 standalone)
        Table leftBlock = new Table();
        leftBlock.add(auctionModalCellImage).size(200f, 200f).center();

        details.add(leftBlock).left().padLeft(24f);

        // Right info column (Turn, Cell, Last Offer)
        Table rightCol = new Table();
        rightCol.top().left();
        rightCol.add(auctionTurnLabel).left().padBottom(6f).row();
        rightCol.add(auctionCellNameLabel).left().padBottom(12f).row();
        rightCol.add(auctionLastOfferHeaderLabel).left().padBottom(4f).row();
        rightCol.add(auctionLastOfferRow).left().row();

        details.add(rightCol).top().left().padLeft(28f);
        window.add(details).growX().center().padBottom(14f).row();

        // 3. Single Horizontal Row for Bid Adjustment: [-10] [-100] [coin + amount]
        // [+100] [+10] (Buttons 90x62, 1.4x scale)
        Table bidRow = new Table();
        bidRow.center();

        Table bidContainer = new Table();
        bidContainer.center();
        Image coinIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
        coinIcon.setScaling(Scaling.fit);

        bidValueLabel.setFontScale(1.0f);
        bidValueLabel.setColor(ACCENT_GOLD);

        bidContainer.add(coinIcon).size(20f, 20f).padRight(4f);
        bidContainer.add(bidValueLabel);

        bidRow.add(minusBidButton).size(90f, 62f).padRight(6f);
        bidRow.add(minusHundredBidButton).size(90f, 62f).padRight(10f);
        bidRow.add(bidContainer).padLeft(12f).padRight(12f);
        bidRow.add(plusHundredBidButton).size(90f, 62f).padLeft(10f);
        bidRow.add(plusBidButton).size(90f, 62f).padLeft(6f);

        window.add(bidRow).center().padBottom(12f).row();

        // 4. Action buttons (BID / DECLINE) & Single-line Waiting label stacked nicely
        Stack actionStack = new Stack();
        auctionControlsRow.clearChildren();
        auctionControlsRow.add(placeBidButton).size(160f, 56f).padRight(12f);
        auctionControlsRow.add(cancelAuctionButton).size(160f, 56f);
        actionStack.add(auctionControlsRow);

        waitingBidLabel.setWrap(false);
        waitingBidLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        actionStack.add(waitingBidLabel);

        window.add(actionStack).width(600f).center().padTop(10f).padBottom(4f).row();

        auctionErrorLabel.setFontScale(0.75f);
        auctionErrorLabel.setColor(Color.RED);
        auctionErrorLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        window.add(auctionErrorLabel).height(20f).center().row();
    }

    private int lastRenderedMemeBankDeposit = -1;

    private void addMemeBankControls() {
        lastRenderedMemeBankDeposit = -1;
        updateMemeBankControls(0, null);
    }

    private void updateMemeBankControls(int deposit, Player localPlayer) {
        if (memeBankModal == null || memeBankModal.getCells().isEmpty()) return;
        Table window = (Table) memeBankModal.getCells().first().getActor();
        if (window == null) return;
        if (lastRenderedMemeBankDeposit == deposit) return;
        lastRenderedMemeBankDeposit = deposit;

        window.clearChildren();
        window.pad(20f, 24f, 20f, 24f);

        // Title
        VisLabel titleLbl = new VisLabel(t("meme_bank_title"));
        titleLbl.setColor(BATTLE_TEXT_COLOR);
        titleLbl.setFontScale(1.2f);
        titleLbl.setWrap(true);
        titleLbl.setAlignment(com.badlogic.gdx.utils.Align.center);
        window.add(titleLbl).width(480f).center().padTop(4f).padBottom(6f).row();

        // Balance + Max labels — will be updated in refreshActions
        memeBankModalLabel.setFontScale(0.95f);
        memeBankModalLabel.setColor(BATTLE_TEXT_COLOR);
        memeBankModalLabel.setWrap(true);
        memeBankModalLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        window.add(memeBankModalLabel).width(480f).center().padBottom(10f).row();

        Texture skipTex = loadTextureIfExists(
                TexturePathResolver.resolveGameScreenTexture("skip_btn.png", language));
        Actor skipBtn = skipTex != null ? createActionButton(skipTex) : createPillButton(t("btn_skip"));
        skipBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.MEME_BANK_SKIP, 0, 0);
            }
        });

        Table bankActionRow = new Table();
        bankActionRow.center();

        if (deposit >= 500) {
            // Deposit is full (500 max). Only withdraw and skip.
            bankActionRow.add(memeBankWithdrawButton).size(180f, 60f).padRight(12f);
            bankActionRow.add(skipBtn).size(180f, 60f);
            window.add(bankActionRow).center().padTop(12f).row();
        } else {
            // Deposit < 500: player can deposit more
            memeBankAmountLabel = new VisLabel(String.valueOf(memeBankAmount));
            memeBankAmountLabel.setColor(BATTLE_TEXT_COLOR);
            memeBankAmountLabel.setFontScale(1.1f);
            Image mbCoinIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
            mbCoinIcon.setScaling(Scaling.fit);

            ImageButton mbMinusBtn = createActionButton(tenDownTexture);
            ImageButton mbPlusBtn = createActionButton(tenUpTexture);
            attachRepeatingListener(mbMinusBtn, () -> {
                memeBankAmount = Math.max(10, memeBankAmount - 10);
                memeBankAmountLabel.setText(String.valueOf(memeBankAmount));
                memeBankErrorLabel.setText("");
            });
            attachRepeatingListener(mbPlusBtn, () -> {
                GameState st = game.getLatestGameState();
                Player lp = st != null ? st.getPlayerById(game.getClient().getLocalPlayerId()) : null;
                int maxCanDeposit = Math.min(500 - deposit, lp != null ? lp.money : 500);
                memeBankAmount = Math.min(Math.max(10, maxCanDeposit), memeBankAmount + 10);
                memeBankAmountLabel.setText(String.valueOf(memeBankAmount));
                memeBankErrorLabel.setText("");
            });

            Table amountRow = new Table();
            amountRow.add(mbMinusBtn).size(60f, 60f).padRight(8f);
            Table valHolder = new Table();
            valHolder.add(memeBankAmountLabel).padRight(4f);
            valHolder.add(mbCoinIcon).size(18f, 18f);
            amountRow.add(valHolder).padRight(8f);
            amountRow.add(mbPlusBtn).size(60f, 60f);
            window.add(amountRow).center().padBottom(10f).row();

            if (deposit > 0) {
                // Deposit between 1 and 499: show ALL 3 BUTTONS (ВКЛАД, ВЫВЕСТИ, ПРОПУСТИТЬ)
                bankActionRow.add(memeBankDepositButton).size(150f, 54f).padRight(8f);
                bankActionRow.add(memeBankWithdrawButton).size(150f, 54f).padRight(8f);
                bankActionRow.add(skipBtn).size(150f, 54f);
            } else {
                // Deposit == 0: show ВКЛАД and ПРОПУСТИТЬ
                bankActionRow.add(memeBankDepositButton).size(160f, 56f).padRight(12f);
                bankActionRow.add(skipBtn).size(160f, 56f);
            }
            window.add(bankActionRow).center().padTop(6f).row();
        }

        // Error label
        window.add(memeBankErrorLabel).center().padTop(8f).row();
    }

    private void setupDebtControls() {
        debtModal.clearChildren();
        debtModal.setVisible(false);
        debtModal.setTouchable(Touchable.disabled);
        debtModal.center();

        Table window = new Table();
        if (buyAndAuctionWindowTexture != null) {
            window.setBackground(new TextureRegionDrawable(new TextureRegion(buyAndAuctionWindowTexture)));
        }
        window.pad(20f, 24f, 20f, 24f);

        VisLabel headerLbl = new VisLabel(language == Language.RU ? "ЗАДОЛЖЕННОСТЬ" : "DEBT PENDING");
        headerLbl.setFontScale(1.15f);
        headerLbl.setColor(TITLE_COLOR);
        headerLbl.setAlignment(Align.center);
        window.add(headerLbl).width(500f).center().padBottom(6f).row();

        debtAmountLabel = new VisLabel("");
        debtAmountLabel.setFontScale(0.95f);
        debtAmountLabel.setColor(BATTLE_TEXT_COLOR);
        debtAmountLabel.setAlignment(Align.center);

        Table debtAmountRow = new Table();
        debtAmountRow.center();
        debtAmountRow.add(debtAmountLabel).padRight(4f);
        if (moneyTexture != null) {
            Image cIcon = new Image(new TextureRegionDrawable(new TextureRegion(moneyTexture)));
            cIcon.setScaling(Scaling.fit);
            debtAmountRow.add(cIcon).size(18f, 18f);
        }
        window.add(debtAmountRow).center().padBottom(8f).row();

        VisLabel debtDescLabel = new VisLabel(language == Language.RU
                ? "Продайте посты/тренд, заложите клетки или заключите сделку с игроками, чтобы собрать нужную сумму."
                : "Sell posts/trends, mortgage cells or make a deal with players to collect the required amount.");
        debtDescLabel.setFontScale(0.82f);
        debtDescLabel.setColor(BATTLE_TEXT_COLOR);
        debtDescLabel.setWrap(true);
        debtDescLabel.setAlignment(Align.center);
        window.add(debtDescLabel).width(480f).center().padBottom(8f).row();

        debtStatusLabel = new VisLabel("");
        debtStatusLabel.setFontScale(0.85f);
        debtStatusLabel.setColor(ACCENT_GOLD);
        debtStatusLabel.setAlignment(Align.center);
        window.add(debtStatusLabel).width(480f).center().padBottom(12f).row();

        Table btnRow = new Table();
        btnRow.center();

        debtPayButton = createPillButton(language == Language.RU ? "ОПЛАТИТЬ" : "PAY DEBT");
        debtPayButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.PAY_DEBT, 0, 0);
            }
        });

        debtSurrenderButton = createPillButton(language == Language.RU ? "СДАТЬСЯ" : "SURRENDER");
        debtSurrenderButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.DECLARE_BANKRUPTCY, 0, 0);
            }
        });

        btnRow.add(debtPayButton).size(160f, 52f).padRight(16f);
        btnRow.add(debtSurrenderButton).size(160f, 52f);
        window.add(btnRow).center().row();

        debtModal.add(window).size(560f, 260f);
    }

    private void updateDebtModal(GameState state, Player localPlayer) {
        if (state == null || debtAmountLabel == null) return;
        int debt = state.pendingDebtAmount;
        int balance = localPlayer != null ? localPlayer.money : 0;
        int diff = Math.max(0, debt - balance);

        debtAmountLabel.setText(language == Language.RU
                ? ("Недостаточно монет для оплаты (" + debt + ")")
                : ("Insufficient coins to pay (" + debt + ")"));

        if (diff > 0) {
            debtStatusLabel.setText(language == Language.RU
                    ? ("Ваш баланс: " + balance + " (Не хватает: " + diff + ")")
                    : ("Your balance: " + balance + " (Missing: " + diff + ")"));
            debtStatusLabel.setColor(Color.RED);
            setActorDisabled(debtPayButton, true);
        } else {
            debtStatusLabel.setText(language == Language.RU
                    ? ("Ваш баланс: " + balance + " — Достаточно для оплаты!")
                    : ("Your balance: " + balance + " — Ready to pay!"));
            debtStatusLabel.setColor(ACCENT_GOLD);
            setActorDisabled(debtPayButton, false);
        }
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
        VisTextButton btn = new VisTextButton(text, style);
        btn.getLabel().setAlignment(Align.center);
        btn.getLabelCell().expand().fill();
        return btn;
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
        TextureRegionDrawable bg = new TextureRegionDrawable(new TextureRegion(inputMemeBattleTexture));
        style.background = bg;
        style.backgroundOver = bg;
        style.focusedBackground = bg;
        style.disabledBackground = bg;
        style.focusBorder = null;
        try {
            String fontPath = game.getLanguageManager().getLanguage() == com.memopoly.utils.LanguageManager.Language.RU
                    ? "fonts_ru/Rubik-Bold.ttf"
                    : "fonts_en/Rubik-Bold.ttf";
            BitmapFont customFont = UiFonts.create(fontPath, 16);
            if (customFont != null) {
                style.font = customFont;
            }
        } catch (Exception ignored) {
        }
        style.fontColor = BATTLE_TEXT_COLOR;
        style.messageFont = style.font;
        style.messageFontColor = new Color(0.0f, 0.04f, 0.24f, 0.55f);
        bg.setLeftWidth(24f);
        bg.setRightWidth(24f);
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

    private void showBankruptEventTargetWindow() {
        GameState state = game.getLatestGameState();
        if (state == null)
            return;
        int localId = game.getClient().getLocalPlayerId();
        List<Player> targets = new ArrayList<>();
        for (Player p : state.players) {
            if (p.id != localId && !p.isBankrupt) {
                targets.add(p);
            }
        }
        if (targets.isEmpty())
            return;

        Dialog dialog = new Dialog("", VisUI.getSkin());
        dialog.setBackground(window(gameOverlayWindowTexture));
        dialog.getContentTable().pad(20f);

        VisLabel title = new VisLabel("Событие Жюри: выберите цель");
        title.setColor(BATTLE_TEXT_COLOR);
        title.setFontScale(1.1f);
        dialog.getContentTable().add(title).padBottom(15f).row();

        for (Player target : targets) {
            VisTextButton btn = new VisTextButton(target.name + " | " + target.money + " монет");
            btn.getLabel().setColor(BATTLE_TEXT_COLOR);
            btn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendAction(GameActionRequest.ActionType.BANKRUPT_EVENT_TARGET, target.id, 0);
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

    private CheckBox.CheckBoxStyle createSettingsCheckBoxStyle() {
        CheckBox.CheckBoxStyle style = new CheckBox.CheckBoxStyle(VisUI.getSkin().get(CheckBox.CheckBoxStyle.class));
        Texture offTex = loadTextureIfExists("screen_ui/checkboxOff.png");
        Texture onTex = loadTextureIfExists("screen_ui/checkboxOn.png");
        float boxSize = 36f;
        if (offTex != null) {
            TextureRegionDrawable off = new TextureRegionDrawable(new TextureRegion(offTex));
            off.setMinWidth(boxSize);
            off.setMinHeight(boxSize);
            style.checkboxOff = off;
        }
        if (onTex != null) {
            TextureRegionDrawable on = new TextureRegionDrawable(new TextureRegion(onTex));
            on.setMinWidth(boxSize);
            on.setMinHeight(boxSize);
            style.checkboxOn = on;
        }
        style.font = VisUI.getSkin().get("default", Label.LabelStyle.class).font;
        style.fontColor = Color.valueOf("000A3E");
        return style;
    }

    private Slider.SliderStyle createSettingsSliderStyle() {
        Slider.SliderStyle style = new Slider.SliderStyle(
                VisUI.getSkin().get("default-horizontal", Slider.SliderStyle.class));
        if (dealSliderBgTexture != null) {
            TextureRegionDrawable track = new TextureRegionDrawable(new TextureRegion(dealSliderBgTexture));
            track.setMinHeight(12f);
            style.background = track;
            style.backgroundOver = track;
        }
        if (dealSliderKnobTexture != null) {
            TextureRegionDrawable knob = new TextureRegionDrawable(new TextureRegion(dealSliderKnobTexture));
            knob.setMinWidth(32f);
            knob.setMinHeight(32f);
            style.knob = knob;
            Drawable knobOver = knob.tint(new Color(0.82f, 0.82f, 0.82f, 1f));
            knobOver.setMinWidth(32f);
            knobOver.setMinHeight(32f);
            Drawable knobDown = knob.tint(new Color(0.70f, 0.70f, 0.70f, 1f));
            knobDown.setMinWidth(32f);
            knobDown.setMinHeight(32f);
            style.knobOver = knobOver;
            style.knobDown = knobDown;
        }
        return style;
    }

    private void showSettingsModal() {
        Dialog dialog = new Dialog("", VisUI.getSkin());
        dialog.setBackground(new TextureRegionDrawable(new TextureRegion(buyAndAuctionWindowTexture)));
        dialog.getContentTable().pad(35f, 35f, 26f, 30f);

        Color textDark = Color.valueOf("000A3E");

        VisLabel titleLabel = new VisLabel(language == Language.RU ? "НАСТРОЙКИ" : "SETTINGS");
        titleLabel.setFontScale(1.6f);
        titleLabel.setColor(textDark);

        Actor closeButton = backButtonTexture != null
                ? createActionButton(backButtonTexture)
                : createPillButton("X");
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });

        Table titleRow = new Table();
        titleRow.top();
        titleRow.add().size(48f).padLeft(30f);
        titleRow.add(titleLabel).expandX().center();
        titleRow.add(closeButton).size(48f, 48f).padRight(30f).top().right();
        dialog.getContentTable().top();
        dialog.getContentTable().add(titleRow).growX().padTop(16f).padBottom(24f).row();

        Preferences preferences = game.getSettingsPreferences();

        VisLabel musicLabel = new VisLabel(language == Language.RU ? "Музыка:" : "Music:");
        musicLabel.setColor(textDark);
        VisSlider musicSlider = new VisSlider(0f, 1f, 0.01f, false, createSettingsSliderStyle());
        musicSlider.setValue(preferences.getFloat("music_volume", 0.7f));
        VisLabel musicVal = new VisLabel(Math.round(musicSlider.getValue() * 100f) + "%");
        musicVal.setColor(textDark);

        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                musicVal.setText(Math.round(musicSlider.getValue() * 100f) + "%");
            }
        });

        Table musicRow = new Table();
        musicRow.add(musicLabel).width(180f).left().padRight(14f);
        musicRow.add(musicSlider).width(420f).padRight(14f);
        musicRow.add(musicVal).width(80f).left();
        dialog.getContentTable().add(musicRow).padBottom(16f).row();

        VisLabel sfxLabel = new VisLabel(language == Language.RU ? "Эффекты:" : "Effects:");
        sfxLabel.setColor(textDark);
        VisSlider sfxSlider = new VisSlider(0f, 1f, 0.01f, false, createSettingsSliderStyle());
        sfxSlider.setValue(preferences.getFloat("sfx_volume", 0.85f));
        VisLabel sfxVal = new VisLabel(Math.round(sfxSlider.getValue() * 100f) + "%");
        sfxVal.setColor(textDark);

        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sfxVal.setText(Math.round(sfxSlider.getValue() * 100f) + "%");
            }
        });

        Table sfxRow = new Table();
        sfxRow.add(sfxLabel).width(180f).left().padRight(14f);
        sfxRow.add(sfxSlider).width(420f).padRight(14f);
        sfxRow.add(sfxVal).width(80f).left();
        dialog.getContentTable().add(sfxRow).padBottom(16f).row();

        CheckBox fullscreenCheckBox = new CheckBox(
                " " + (language == Language.RU ? "Полноэкранный режим" : "Fullscreen mode"), createSettingsCheckBoxStyle());
        fullscreenCheckBox.setChecked(preferences.getBoolean("fullscreen", false));
        dialog.getContentTable().add(fullscreenCheckBox).left().padLeft(65f).padBottom(8f).row();

        VisLabel statusLabel = new VisLabel(language == Language.RU
                ? "Изменения сохраняются после нажатия \"Применить\""
                : "Changes are saved after clicking \"Apply\"");
        statusLabel.setColor(new Color(textDark.r, textDark.g, textDark.b, 0.7f));
        statusLabel.setWrap(true);
        statusLabel.setAlignment(Align.center);
        dialog.getContentTable().add(statusLabel).width(440f).center().padTop(4f).padBottom(16f).row();

        Table btnRow = new Table();
        Actor saveBtn = applyButtonTexture != null
                ? createActionButton(applyButtonTexture)
                : createTextureOrPillButton(null, language == Language.RU ? "Применить" : "Apply");
        saveBtn.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                float mv = musicSlider.getValue();
                float sv = sfxSlider.getValue();
                boolean fs = fullscreenCheckBox.isChecked();
                preferences.putFloat("music_volume", mv);
                preferences.putFloat("sfx_volume", sv);
                preferences.putBoolean("fullscreen", fs);
                preferences.flush();
                game.applySettings(mv, sv, fs);
                dialog.hide();
            }
        });
        btnRow.add(saveBtn).size(190f, 80f);
        dialog.getContentTable().add(btnRow).center().padTop(8f).padBottom(20f).row();

        dialog.show(stage);
        dialog.setSize(900f, 620f);
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) * 0.5f,
                (stage.getHeight() - dialog.getHeight()) * 0.5f);
    }

    private void showRulesModal() {
        NinePatch ninePatch = new NinePatch(buyAndAuctionWindowTexture, 140, 140, 140, 140);
        float patchScale = 620f / (float) buyAndAuctionWindowTexture.getHeight();
        ninePatch.scale(patchScale, patchScale);
        NinePatchDrawable dialogBg = new NinePatchDrawable(ninePatch);
        dialogBg.setMinWidth(0f);
        dialogBg.setMinHeight(0f);

        Window.WindowStyle style = new Window.WindowStyle(VisUI.getSkin().get(Window.WindowStyle.class));
        style.background = dialogBg;
        style.stageBackground = null;
        Dialog dialog = new Dialog("", style);
        dialog.setClip(false);
        dialog.getContentTable().setClip(false);
        dialog.getContentTable().pad(20f);
        dialog.getButtonTable().clear();

        VisLabel titleLabel = new VisLabel(language == Language.RU ? "ПРАВИЛА ИГРЫ" : "RULES");
        titleLabel.setFontScale(1.4f);
        titleLabel.setColor(BATTLE_TEXT_COLOR);

        Actor closeButton = null;
        if (backButtonTexture != null) {
            ImageButton backBtn = createActionButton(backButtonTexture);
            backBtn.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    dialog.hide();
                }
            });
            closeButton = backBtn;
        }

        Table titleRow = new Table();
        titleRow.top();
        titleRow.add().size(50f, 48.5f).padLeft(16f);
        titleRow.add(titleLabel).expandX().center();
        if (closeButton != null) {
            titleRow.add(closeButton).size(50f, 48.5f).padRight(16f).padTop(12f).top().right();
        } else {
            titleRow.add().size(50f, 48.5f).padRight(16f);
        }
        dialog.getContentTable().top();
        dialog.getContentTable().add(titleRow).growX().padTop(16f).padBottom(14f).row();

        Table content = new Table();
        content.left().top();
        content.defaults().left().padBottom(16f).growX();

        boolean ru = language == Language.RU;

        addRuleSectionWithCells(content, new int[] { 0 }, null,
                ru ? "1. Клетка СТАРТ" : "1. START Cell",
                ru ? "Каждый раз при прохождении через клетку «Старт» игрок получает +200 монет (или +215 монет, если у него роль «Мемолог»)."
                        : "Every time a player passes through the Start cell, they receive +200 coins (+215 coins for the \"Memolog\" role).");

        addRuleSectionWithCells(content, new int[] { 1 }, null,
                ru ? "2. Покупка клеток интернет-пространства и Монополия" : "2. Purchasing Internet Space Cells & Monopoly",
                ru ? "Попадая на свободную клетку интернет-пространства, вы можете приобрести её в собственность. Собрав все клетки одной цветовой группы (Монополию), вы получаете право развивать её, выкладывая Посты (до 4 постов) и создавая Тренд. Чем выше уровень застройки клетки, тем больший Донат обязаны платить соперники при попадании на неё!"
                        : "Landing on an unowned internet space cell allows you to purchase it. Collecting all cells of one color group (a Monopoly) grants you the right to develop it by publishing Posts (up to 4 posts) and creating a Trend. The higher the cell development level, the larger the Donation opponents must pay upon landing on it!");

        addRuleSectionWithCells(content, new int[] { 3 }, null,
                ru ? "3. События" : "3. Events",
                ru ? "Приземление на клетку с вопросом открывает карту случайного события, способную принести мгновенную прибыль, наложить штраф или изменить ход всей партии."
                        : "Landing on a question mark cell reveals a random event card that can grant instant profits, impose a penalty, or alter the course of the entire game.");

        addRuleSectionWithCells(content, new int[] { 30, 10 }, null,
                ru ? "4. Бан и Copyright Infringement" : "4. Ban & Copyright Infringement",
                ru ? "Попадание на клетку «Copyright Infringement» немедленно отправляет игрока в Ban (на клетку «Ban»). Чтобы выйти из Бана, выбросьте дубль на кубиках или заплатите штраф в 50 монет."
                        : "Landing on the \"Copyright Infringement\" cell immediately sends the player to Ban (to the \"Ban\" cell). To leave Ban, roll doubles on the dice or pay a 50 coin fine.");

        addRuleSectionWithCells(content, new int[] { 5 }, null,
                ru ? "5. Мем-Баттл" : "5. Meme Battle",
                ru ? "Игрок, приземлившийся на клетку «Мем-Баттл», определяет ставку и задает тему (ситуацию) для мини-игры. Участники выбирают и выкладывают свои самые смешные мемы под заданную ситуацию. Все игроки голосуют за лучший мем раунда. Автор мема, набравшего наибольшее количество голосов, побеждает и забирает весь банк ставок!"
                        : "The player who lands on the \"Meme Battle\" cell determines the bet and sets the theme (situation) for the mini-game. Participants select and submit their funniest memes for the situation. All players vote for the round's best meme. The author of the meme with the most votes wins and takes the entire prize pool!");

        Texture actionIcon = mortgageButtonTexture != null ? mortgageButtonTexture : null;
        addRuleSectionWithCells(content, new int[] {}, actionIcon,
                ru ? "6. Панель ДЕЙСТВИЯ (ACTIONS)" : "6. ACTIONS Panel",
                ru ? "В панели ДЕЙСТВИЯ (ACTIONS) доступны операции управления имуществом: залог клеток для экстренного получения монет, выкуп из залога, строительство/продажа постов, а также запуск Сделки для обмена клетками и деньгами с соперниками."
                        : "The ACTIONS panel provides property management operations: mortgaging cells for emergency coins, unmortgaging, building/selling posts, and initiating Deals to trade cells and coins with opponents.");

        ScrollPane scrollPane = new ScrollPane(content, VisUI.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        UiScrollStyle.apply(scrollPane, language);

        dialog.getContentTable().add(scrollPane).width(780f).height(480f).center().padBottom(16f).row();

        dialog.show(stage);
        dialog.setSize(860f, 620f);
        dialog.setPosition((stage.getWidth() - dialog.getWidth()) * 0.5f,
                (stage.getHeight() - dialog.getHeight()) * 0.5f);
    }

    private void addRuleSectionWithCells(Table table, int[] cellIds, Texture extraIcon, String headerText, String bodyText) {
        Table sectionRow = new Table();
        sectionRow.left().top();

        Table iconBox = new Table();
        iconBox.left().top();
        if (cellIds != null && cellIds.length > 0 && cellTextures != null) {
            for (int cellId : cellIds) {
                if (cellId >= 0 && cellId < cellTextures.length && cellTextures[cellId] != null) {
                    Image cellImg = new Image(
                            new TextureRegionDrawable(new TextureRegion(cellTextures[cellId])));
                    cellImg.setScaling(Scaling.fit);
                    iconBox.add(cellImg).size(50f, 70f).padRight(6f);
                }
            }
        }
        if (extraIcon != null) {
            Image iconImg = new Image(new TextureRegionDrawable(new TextureRegion(extraIcon)));
            iconImg.setScaling(Scaling.fit);
            iconBox.add(iconImg).size(60f, 40f).padRight(6f);
        }
        if (iconBox.getChildren().size > 0) {
            sectionRow.add(iconBox).left().top().padRight(12f);
        }

        Table textTable = new Table();
        textTable.top().left();
        VisLabel header = new VisLabel(headerText != null ? headerText : "");
        header.setFontScale(1.05f);
        header.setColor(ACCENT_GOLD);
        header.setWrap(true);
        header.setAlignment(Align.left);

        VisLabel body = new VisLabel(bodyText != null ? bodyText : "");
        body.setFontScale(0.85f);
        body.setColor(BATTLE_TEXT_COLOR);
        body.setWrap(true);
        body.setAlignment(Align.left);

        textTable.add(header).growX().left().row();
        textTable.add(body).growX().left().padTop(4f).row();

        sectionRow.add(textTable).expandX().fillX().left().top();
        table.add(sectionRow).growX().padBottom(14f).row();
    }

    private String getAuctionTurnName(GameState state) {
        Player player = state.getPlayerById(state.auctionCurrentPlayerId);
        return player == null ? "—" : player.name;
    }

    private String t(String key) {
        boolean ru = language == Language.RU;
        return switch (key) {
            case "waiting_answers" -> ru ? "Ждём ответов игроков..." : "Waiting for players' answers...";
            case "battle_setup_title" -> ru ? "Выбери ставку и смешную тему для мини-игры"
                    : "Choose stake and a funny topic for the mini-game";
            case "choose_stakes" -> ru ? "Выбери ставку" : "Choose the stakes";
            case "invite_battle" -> ru ? "Тебя приглашают на мем-баттл!" : "You are invited to a meme battle!";
            case "organizer_chooses" -> ru ? "Организатор выбирает ставку..." : "Organizer is choosing the stakes...";
            case "topic_placeholder" -> ru ? "Тема или ситуация..." : "Write the situation or topic";
            case "waiting_topic" -> ru ? "Ждём тему от организатора..." : "Waiting for the topic...";
            case "choose_funniest" ->
                ru ? "Выбери самый смешной мем для ситуации" : "CHOOSE THE FUNNIEST CARD FOR THE SITUATION";
            case "vote_hint" -> ru ? "Голосуй за лучший мем!" : "Vote for the best meme!";
            case "already_voted" ->
                ru ? "Ты уже проголосовал. Ждём остальных..." : "You already voted. Waiting for others...";
            case "battle_finished" -> ru ? "Баттл завершён!" : "Battle finished!";
            case "branches" -> ru ? "Посты" : "Posts";
            case "build_branch" -> ru ? "Купить пост" : "Buy Post";
            case "sell_branch" -> ru ? "Продать пост" : "Sell Post";
            case "window_trade_title" -> ru ? "Сделка" : "Trade";
            case "btn_build" -> ru ? "Купить пост" : "Buy Post";
            case "btn_sell_house" -> ru ? "Продать пост" : "Sell Post";
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
            case "doge_choice" -> ru ? "Стоп или пройти на 2 клетки вперед?" : "Stay or move 2 cells?";
            case "skip_jail" -> ru ? "Пропустить Ban" : "Skip Ban";
            case "take_jail" -> ru ? "В Ban + Щит" : "Ban + Shield";
            case "jail_roll_attempt" -> ru ? "Попытка дубля" : "Double attempt";
            case "pay_jail_fine" -> ru ? "Выкупиться" : "Pay Fine";
            case "dev_send_to_jail" -> ru ? "[В тюрьму]" : "[Send to Jail]";
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
            case "auction_title" -> ru ? "Аукцион" : "Auction";
            case "auction_turn" -> ru ? "Ход:" : "Turn:";
            case "auction_cell" -> ru ? "Клетка:" : "Cell:";
            case "auction_status" -> ru ? "Аукцион: осталось" : "Auction: left";
            case "auction_last_offer" -> ru ? "Последнее предложение:" : "Last offer:";
            case "auction_no_bids" -> ru ? "Нет ставок" : "No bids";
            case "auction_bid_must_be_higher" ->
                ru ? "Ставка должна быть выше предыдущей!" : "Bid must be higher than current offer!";
            case "meme_bank_title" -> ru ? "Meme Bank" : "Meme Bank";
            case "mb_balance" -> ru ? "На счету" : "Balance";
            case "mb_max" -> ru ? "Максимум: 500" : "Maximum: 500";
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
            case "bid_waiting" -> ru ? "Дождитесь ставки от: " : "Waiting for bid from: ";
            case "dice_double" -> ru ? "дубль!" : "doubles!";
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

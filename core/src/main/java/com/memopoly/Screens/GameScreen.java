package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Cell;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.memopoly.Memopoly;
import com.memopoly.utils.LanguageManager.Language;
import com.memopoly.utils.TexturePathResolver;
import com.memopoly.game.model.*;
import com.memopoly.network.packets.BattleResponsePacket;
import com.memopoly.network.packets.GameActionRequest;
import com.memopoly.network.packets.RollDiceRequest;

import javax.swing.event.ChangeEvent;
import java.util.*;

import com.badlogic.gdx.scenes.scene2d.ui.Stack;
/**
 * Экран игрового процесса: отображает доску, статистику игроков, кнопки действий и всплывающие окна баттлов/аукционов.
 */
public class GameScreen extends BaseScreen {
    private static final float COMMON_BUTTON_HEIGHT = 64f;
    private static final float WORLD_WIDTH = 1920f;
    private static final float WORLD_HEIGHT = 1080f;
    private static final Color TITLE_COLOR = new Color(1.00f, 0.83f, 0.25f, 1f);
    private static final Color TEXT_SOFT = Color.WHITE;
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
        auctionButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(AUCTION_BUTTON_TEXTURE_PATH, language));
        endTurnButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(END_TURN_BUTTON_TEXTURE_PATH, language));
        placeBidButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(PLACE_BID_BUTTON_TEXTURE_PATH, language));
        mortgageButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(MORTGAGE_BUTTON_TEXTURE_PATH, language));
        buyBackButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(BUY_BACK_BUTTON_TEXTURE_PATH, language));
        exitToMenuButtonTexture = loadTextureIfExists(TexturePathResolver.resolveGameScreenTexture(EXIT_TO_MENU_BUTTON_TEXTURE_PATH, language));
        depositButtonTexture = loadTextureIfExists(TexturePathResolver.resolveGameScreenTexture(DEPOSIT_BUTTON_TEXTURE_PATH, language));
        withdrawButtonTexture = loadTextureIfExists(TexturePathResolver.resolveGameScreenTexture(WITHDRAW_BUTTON_TEXTURE_PATH, language));
        skipButtonTexture = loadTextureIfExists(TexturePathResolver.resolveGameScreenTexture(PASS_BUTTON_TEXTURE_PATH, language));
        participateButtonTexture = loadTextureIfExists(TexturePathResolver.resolveGameScreenTexture(PARTICIPATE_BUTTON_TEXTURE_PATH, language));
        declineButtonTexture = loadTextureIfExists(TexturePathResolver.resolveGameScreenTexture(DECLINE_BUTTON_TEXTURE_PATH, language));
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

        titleLabel = new VisLabel("Мемополия");
        phaseLabel = new VisLabel("Фаза: -");
        turnLabel = new VisLabel("Ход: -");
        cellLabel = new VisLabel("Клетка: -");
        logLabel = new VisLabel("События появятся здесь");
        auctionLabel = new VisLabel("");
        diceTitleLabel = new VisLabel("");
        diceHintLabel = new VisLabel("Кнопка броска появится, когда ход твой.");
        currentCellTitleLabel = new VisLabel("");
        currentCellMetaLabel = new VisLabel("-");
        feedTitleLabel = new VisLabel("");
        feedDescriptionLabel = new VisLabel("Последние действия игроков будут собираться здесь.");
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
        cancelAuctionButton = createOptionalActionButton(declineButtonTexture, "Отказаться");
        bidField = new VisTextField();
        memeBankAmountField = new VisTextField();
        memeBankDepositButton = createOptionalActionButton(depositButtonTexture, "Вложить");
        memeBankWithdrawButton = createOptionalActionButton(withdrawButtonTexture, "Снять");
        memeBankSkipButton = createOptionalActionButton(skipButtonTexture, "Пропустить");
        applyInputFieldStyle(bidField);
        applyInputFieldStyle(memeBankAmountField);
        // Digits-only filters for numeric fields
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
    private Texture loadOrFallback(String path, Texture fallback) {
        Texture t = loadTextureIfExists(path);
        return t != null ? t : fallback;
    }
    private void createUi() {
        Table root = new Table();
        sideRoot = root;
        root.setFillParent(true);
        root.pad(18);

        // ===== оверлеи доски (без изменений) =====
        diceTitleLabel.setColor(Color.WHITE);
        diceTitleLabel.setFontScale(0.92f);
        diceHintLabel.setWrap(true);
        diceHintLabel.setColor(Color.WHITE);
        diceHintLabel.setFontScale(1.00f);
        diceOverlay.center();
        Table diceContent = new Table();
        diceContent.center();
        Table diceButtonColumn = new Table();
        diceButtonColumn.add(diceButton).size(150, 84).row();
        diceButtonColumn.add(endTurnButton).size(150, COMMON_BUTTON_HEIGHT).padTop(8f);
        diceContent.add(diceButtonColumn).padRight(18f);
        diceContent.add(diceHintLabel).width(320f).left();
        diceOverlay.add(diceContent).center();

        // ===== CURRENT CELL: временно отключено (может понадобиться в будущем) =====
//currentCellTitleLabel.setColor(Color.WHITE);
//currentCellTitleLabel.setFontScale(0.92f);
//currentCellMetaLabel.setWrap(true);
//currentCellMetaLabel.setColor(Color.WHITE);
//currentCellMetaLabel.setFontScale(0.82f);
//currentCellOverlay.top().left();
//currentCellOverlay.defaults().left();
//Table currentCellContent = new Table();
//currentCellContent.left().top();
//currentCellContent.add(currentCellImage).size(145, 145).padLeft(34).top();
//currentCellContent.add(currentCellMetaLabel).width(180).top().left().padLeft(12).padTop(24);
//currentCellOverlay.add(currentCellContent).left().padLeft(8).padTop(-20).padBottom(10);

// ===== FEED: временно отключено (может понадобиться в будущем) =====
//feedTitleLabel.setFontScale(0.92f);
//feedDescriptionLabel.setWrap(true);
//feedDescriptionLabel.setColor(Color.WHITE);
//feedDescriptionLabel.setFontScale(0.82f);
//feedOverlay.top().left();
//feedOverlay.defaults().left();
//feedOverlay.add(feedDescriptionLabel).width(330).padLeft(24).padTop(-8).row();
//auctionLabel.setFontScale(0.74f);
//feedOverlay.add(auctionLabel).width(282).padLeft(24).padTop(2);

        titleLabel.setColor(TITLE_COLOR);
        titleLabel.setFontScale(0.98f);
        phaseLabel.setColor(TEXT_SOFT);
        phaseLabel.setWrap(true);
        phaseLabel.setFontScale(0.82f);
        turnLabel.setColor(Color.WHITE);
        turnLabel.setWrap(true);
        turnLabel.setFontScale(0.82f);
        cellLabel.setColor(Color.WHITE);
        cellLabel.setWrap(true);
        cellLabel.setFontScale(0.82f);
        logLabel.setWrap(true);
        logLabel.setColor(Color.WHITE);
        logLabel.setFontScale(0.82f);
        applyGameScreenLineSpacing();
        auctionLabel.setWrap(true);
        auctionLabel.setColor(Color.WHITE);

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

        // MEMES: одна карточка в ряд, скролл вниз
        ScrollPane memesScroll = new ScrollPane(handMemesTable);
        memesScroll.setFadeScrollBars(false);
        memesScroll.setScrollingDisabled(true, false);
        memesScroll.getStyle().background = null;

        chatWidget = new ChatWidget(game, 340f);

        // ===== слушатели кнопок (как было) =====
        buyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.BUY_CELL, 0, 0);
            }
        });
        passButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.PASS_BUY, 0, 0);
            }
        });
        endTurnButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendAction(GameActionRequest.ActionType.END_TURN, 0, 0);
            }
        });
        bidField.setMessageText("Ставка");
        placeBidButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int bid = parseBid();
                if (bid <= 0) {
                    auctionErrorLabel.setText("Ставка должна быть > 0");
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
        memeBankAmountField.setMessageText("Сумма");
        memeBankDepositButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int amount = parseAmount(memeBankAmountField);
                GameState state = game.getLatestGameState();
                Player localPlayer = state != null ? state.getPlayerById(game.getClient().getLocalPlayerId()) : null;
                if (amount <= 0) {
                    memeBankErrorLabel.setText("Введи сумму > 0");
                } else if (amount > 500) {
                    memeBankErrorLabel.setText("Максимум 500 монет!");
                } else if (localPlayer != null && amount > localPlayer.money) {
                    memeBankErrorLabel.setText("Недостаточно монет!");
                } else {
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

        VisLabel playersTitleLabel = new VisLabel("PLAYERS");
        playersTitleLabel.setFontScale(1.2f);

        Table playersPanel = new Table();
        playersPanel.setBackground(window(playersWindowTexture));
        playersPanel.pad(24f, 14f, 14f, 14f);
        playersPanel.add(playersTitleLabel).center().padBottom(10f).row();
        playersPanel.add(playersScroll).grow().row();
        playersPanel.add(exitToMenuButton).width(220f).height(56f).padTop(10f); // выход внизу окна PLAYERS

        Table leftColumn = new Table();
        leftColumn.top();
        leftColumn.add(playersPanel).width(380f).expandY().fillY().row();
        leftColumn.add(chatWidget).width(380f).height(270f).padTop(16f);
        root.add(leftColumn).top().left();

        root.add().expand().fill();

        VisLabel ownedTitleLabel = new VisLabel("MY CELLS");
        ownedTitleLabel.setFontScale(1.2f);
        VisLabel memesTitleLabel = new VisLabel("MEMES");
        memesTitleLabel.setFontScale(1.2f);

        Table ownedPanel = new Table();
        ownedPanel.setBackground(window(myCellsWindowTexture));
        ownedPanel.pad(24f, 12f, 14f, 12f);
        ownedPanel.add(ownedTitleLabel).center().padBottom(10f).row();
        ownedPanel.add(ownedScroll).grow();

        Table memesPanel = new Table();
        memesPanel.setBackground(window(memesWindowTexture));
        memesPanel.pad(24f, 12f, 14f, 12f);
        memesPanel.add(memesTitleLabel).center().padBottom(10f).row();
        memesPanel.add(memesScroll).grow();

        Table rightColumn = new Table();
        rightColumn.top();
        rightColumn.add(ownedPanel).width(380f).height(500f).row();
        rightColumn.add(memesPanel).width(380f).expandY().fillY().row();
        root.add(rightColumn).top().right();

        configureModal(turnNotificationModal, notificationWindowTexture, turnModalLabel, NOTIFICATION_MODAL_MIN_W, NOTIFICATION_MODAL_MIN_H, true);
        configureModal(buyOrAuctionModal, buyAndAuctionWindowTexture, buyAuctionModalLabel, BUY_AND_AUCTION_MODAL_MIN_W, BUY_AND_AUCTION_MODAL_MIN_H, false);
        configureModal(auctionModal, auctionOrMemeBankWindowTexture, auctionModalLabel, AUCTION_MODAL_MIN_W, AUCTION_MODAL_MIN_H, false);
        configureModal(memeBankModal, auctionOrMemeBankWindowTexture, memeBankModalLabel, MEME_BANK_MODAL_MIN_W, MEME_BANK_MODAL_MIN_H, false);
        setupModalControls();

        stage.addActor(root);
        stage.addActor(diceOverlay);
        //stage.addActor(currentCellOverlay);
        //stage.addActor(feedOverlay);
        stage.addActor(buyOrAuctionModal);
        stage.addActor(auctionModal);
        stage.addActor(memeBankModal);
        stage.addActor(turnNotificationModal);

        layoutBoardOverlays();
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
        battleTimerLabel = null;
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
                panel.add(buildWaitPanel(state.lastActionLog == null ? t("battle_finished") : state.lastActionLog)).center().expand();
                break;
            default:
                break;
        }
        battleOverlay.add(panel).width(860f).height(920f).center();
    }

    private void addTimerRow(Table p) {
        battleTimerLabel = new VisLabel("");
        battleTimerLabel.setColor(BATTLE_TEXT_COLOR);
        battleTimerLabel.setFontScale(1.1f);
        p.add(battleTimerLabel).center().padBottom(10f).row();
    }

    private Table buildWaitPanel(String text) {
        Table p = new Table();
        addTimerRow(p);
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
        addTimerRow(p);

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
        addTimerRow(p);
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
        addTimerRow(p);
        Meme submitted = getSubmittedBattleMeme(state, localId);

        Table topRow = new Table();
        if (isOwner && submitted == null) {
            topicField = new VisTextField();
            topicField.setMessageText(t("topic_placeholder"));
            applyBattleInputFieldStyle(topicField);
            topicField.setMaxLength(120);
            topRow.add(topicField).width(470f).height(60f).padRight(14f);
        } else {
            String topic = state.battleTopic == null || state.battleTopic.isBlank() ? t("waiting_topic") : state.battleTopic;
            VisLabel topicLabel = new VisLabel(topic);
            topicLabel.setColor(BATTLE_TEXT_COLOR);
            topicLabel.setWrap(true);
            topRow.add(topicLabel).width(470f).padRight(14f);
        }
        startBattleButton = createActionButton(startBattleTexture);
        startBattleButton.setDisabled(selectedBattleMemeId == -1 || submitted != null);
        startBattleButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (selectedBattleMemeId == -1) return;
                String topic = (topicField != null) ? topicField.getText().trim() : "";
                sendAction(GameActionRequest.ActionType.SUBMIT_MEME, selectedBattleMemeId, 0, topic);
            }
        });
        topRow.add(startBattleButton).size(120f, 60f);
        p.add(topRow).center().padBottom(14f).row();

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
                if (count >= 6) break;
                boolean selected = meme.id == selectedBattleMemeId;
                grid.add(createBattleMemeCard(meme, selected, submitted != null, () -> {
                    selectedBattleMemeId = (selectedBattleMemeId == meme.id) ? -1 : meme.id;
                    rebuildBattleContent(state);
                })).size(330f, 248f).pad(10f);
                count++;
                if (++col % 2 == 0) grid.row();
            }
        }
        p.add(grid).center().row();
        return p;
    }

    private Table buildVotingPanel(GameState state, int localId) {
        Table p = new Table();
        p.center();
        addTimerRow(p);
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
                grid.add(createBattleMemeCard(meme, false, ownMeme, () ->
                    sendAction(GameActionRequest.ActionType.VOTE_MEME, memeId, 0, null)
                )).size(330f, 248f).pad(10f);
                if (++col % 2 == 0) grid.row();
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

    private Table createBattleMemeCard(Meme meme, boolean selected, boolean disabled, Runnable action) {
        Table card = new Table();

        TextureRegionDrawable baseFrame = new TextureRegionDrawable(new TextureRegion(cardBoardTexture));
        Drawable frame = selected ? baseFrame.tint(new Color(0.55f, 0.55f, 0.62f, 1f)) : baseFrame;
        card.setBackground(frame);
        card.pad(10f);

        Drawable memeDrawable = getMemeDrawable(meme);
        if (memeDrawable != null) {
            Image image = new Image(memeDrawable);
            image.setScaling(Scaling.fill);
            card.add(image).expand().fill();
        } else {
            VisLabel placeholder = new VisLabel("Нет превью");
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

    private void refreshUi(GameState state) {
        if (state == null) {
            titleLabel.setText("Мемополия");
            phaseLabel.setText("Фаза: ожидание данных");
            turnLabel.setText("Ход: -");
            cellLabel.setText("Клетка: -");
            logLabel.setText("Подключение к состоянию игры...");
            currentCellMetaLabel.setText("Информация о клетке появится, когда состояние игры загрузится.");
            currentCellImage.setDrawable(null);
            diceHintLabel.setText("Кнопка броска появится, когда ход будет твоим.");
            feedDescriptionLabel.setText("Подключаемся к игровой комнате и ждём актуальное состояние матча.");
            rebuildPlayersIfNeeded(null, null, -1);
            rebuildOwnedCellsIfNeeded(null, null, false, GameState.GamePhase.WAITING);
            //rebuildHandMemesIfNeeded(null, false);
            setButtonsEnabled(false, false, false, false, false);
            auctionLabel.setText("");
            //refreshBattleOverlay(state);
            return;
        }

        if (state.players != null) {
            Set<Integer> currentPlayerIds = new HashSet<>();
            for (Player p : state.players) {
                currentPlayerIds.add(p.id);
                Integer lastPos = lastKnownPositions.get(p.id);
                if (lastPos == null) {
                    // Первая встреча игрока — просто запоминаем позицию без анимации
                    lastKnownPositions.put(p.id, p.position);
                } else if (lastPos != p.position) {
                    // Позиция изменилась — запускаем пошаговую анимацию
                    boardRenderer.animateMovement(p.id, lastPos, p.position);
                    lastKnownPositions.put(p.id, p.position);
                }
            }
            // Удаляем записи для игроков, которые покинули игру
            lastKnownPositions.keySet().retainAll(currentPlayerIds);
        }

        Player current = state.getCurrentPlayer();
        int localPlayerId = game.getClient().getLocalPlayerId();
        Player localPlayer = state.getPlayerById(localPlayerId);
        boolean myTurn = current != null && current.id == localPlayerId;
        BoardCell currentCell = current != null ? boardCells.get(current.position) : null;

        phaseLabel.setText("Фаза: " + state.currentPhase);
        turnLabel.setText(current == null ? "Ход: -" : "Ход: " + current.name + " | ход №" + state.turnCount);
        cellLabel.setText(currentCell == null ? "Клетка: -" : "Клетка: " + currentCell.name + " [" + currentCell.type + "]");
        logLabel.setText("Лог: " + (state.lastActionLog == null ? "-" : state.lastActionLog));
        currentCellMetaLabel.setText(buildCellMeta(currentCell, state));
        TextureRegionDrawable cellDrawable = currentCell == null ? null : new TextureRegionDrawable(new TextureRegion(cellTextures[currentCell.id]));
        currentCellImage.setDrawable(cellDrawable);
        buyModalCellImage.setDrawable(cellDrawable);
        TextureRegionDrawable auctionCellDrawable = state.auctionCellId >= 0 && state.auctionCellId < cellTextures.length
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
    }

    private void rebuildPlayersIfNeeded(GameState state, Player current, int localPlayerId) {
        String signature = buildPlayersSignature(state, current, localPlayerId);
        if (signature.equals(lastPlayersSignature)) {
            return;
        }
        lastPlayersSignature = signature;
        rebuildPlayers(state, current, localPlayerId);
    }

    private void rebuildOwnedCellsIfNeeded(GameState state, Player localPlayer, boolean myTurn, GameState.GamePhase currentPhase) {
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

    private void rebuildPlayers(GameState state, Player current, int localPlayerId) {
        playersTable.clearChildren();
        if (state == null || state.players == null) {
            return;
        }
        for (Player player : state.players) {
            String bankrupt = player.isBankrupt ? " [банкрот]" : "";
            Table row = new Table();
            row.pad(8, 10, 8, 10);
            VisLabel nameLabel = new VisLabel(player.name + bankrupt);
            nameLabel.setWrap(true);
            Table moneyCell = createMoneyValue(player.money);
            row.add(nameLabel).width(220).left().padRight(8);
            row.add(moneyCell).right();
            playersTable.add(row).width(340).left().padBottom(8).row();
        }
    }

    private void rebuildOwnedCells(GameState state, Player localPlayer, boolean myTurn, GameState.GamePhase currentPhase) {
        ownedCellsTable.clearChildren();
        if (localPlayer == null || localPlayer.ownedCells.isEmpty()) {
            VisLabel emptyLabel = new VisLabel("Пока нет купленных клеток");
            emptyLabel.setWrap(true);
            emptyLabel.setFontScale(0.7f);
            ownedCellsTable.add(emptyLabel).width(300).center().padTop(10).row();
            return;
        }
        int count = 0;
        for (int cellId : localPlayer.ownedCells) {
            BoardCell cell = boardCells.get(cellId);
            boolean mortgaged = state.cellMortgaged.getOrDefault(cellId, false);

            Table cellCard = new Table();
            Image cellImage = new Image(new TextureRegionDrawable(new TextureRegion(cellTextures[cellId])));
            cellImage.setScaling(Scaling.fit);
            cellCard.add(cellImage).size(160f, 200f).row();

            ImageButton actionButton = createActionButton(mortgaged ? buyBackButtonTexture : mortgageButtonTexture);
            boolean canManageCell = myTurn && currentPhase != GameState.GamePhase.AUCTION && currentPhase != GameState.GamePhase.MEME_BATTLE;
            actionButton.setDisabled(!canManageCell);
            actionButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    sendAction(
                        mortgaged ? GameActionRequest.ActionType.BUY_BACK_CELL : GameActionRequest.ActionType.MORTGAGE_CELL,
                        cellId,
                        0
                    );
                }
            });

            VisLabel priceLabel = new VisLabel(String.valueOf(cell.price / 2));
            priceLabel.setFontScale(0.45f);
            priceLabel.setColor(canManageCell ? Color.valueOf("000A3E") : Color.WHITE);

            Table priceOverlay = new Table();
            priceOverlay.center().bottom();
            priceOverlay.add(priceLabel).padBottom(4f);

            Stack buttonStack = new Stack();
            buttonStack.add(actionButton);
            buttonStack.add(priceOverlay);
            cellCard.add(buttonStack).width(150f).height(48f).padTop(6f);

            ownedCellsTable.add(cellCard).pad(6f);
            if (++count % 2 == 0) {
                ownedCellsTable.row();
            }
        }
    }

    private void rebuildHandMemes(Player localPlayer, boolean canSubmitBattleMeme) {
        handMemesTable.clearChildren();
        if (localPlayer == null || localPlayer.handMemes == null || localPlayer.handMemes.isEmpty()) {
            VisLabel emptyLabel = new VisLabel("Колода ещё не выдала мемы");
            emptyLabel.setWrap(true);
            handMemesTable.add(emptyLabel).width(300).left().row();
            return;
        }
        for (Meme meme : localPlayer.handMemes) {
            handMemesTable.add(createMemeCard(meme, false, !canSubmitBattleMeme, () ->
                sendAction(GameActionRequest.ActionType.SUBMIT_MEME, meme.id, 0)
            )).width(300f).height(225f).pad(8f).row(); // 4:3, одна в ряд
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
     * Creates a clickable meme card. The card itself acts as the button - no separate select/vote button.
     * @param meme       the meme to display
     * @param isSelected whether this card is already selected (shows a highlight border)
     * @param disabled   if true the card is not clickable
     * @param action     the action to run when the card is clicked
     */
    private Table createMemeCard(Meme meme, boolean isSelected, boolean disabled, Runnable action) {
        Table card = new Table();
        TextureRegionDrawable baseFrame = new TextureRegionDrawable(new TextureRegion(cardBoardTexture));
        Drawable frame = isSelected ? baseFrame.tint(new Color(0.55f, 0.55f, 0.62f, 1f)) : baseFrame;
        card.setBackground(frame);
        card.pad(10f);

        Drawable memeDrawable = getMemeDrawable(meme);
        if (memeDrawable != null) {
            Image image = new Image(memeDrawable);
            image.setScaling(Scaling.fill);
            card.add(image).expand().fill();
        } else {
            VisLabel placeholder = new VisLabel("Нет превью");
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
        boolean canRoll = myTurn && state.currentPhase == GameState.GamePhase.PLAYING && !state.hasRolledThisTurn;
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
        if (memeBankSkipButton instanceof VisTextButton) {
            ((VisTextButton) memeBankSkipButton).setDisabled(!canUseMemeBank);
        }
        setModalVisible(buyOrAuctionModal, canBuyOrPass);
        setModalVisible(auctionModal, state.currentPhase == GameState.GamePhase.AUCTION);
        setModalVisible(memeBankModal, state.currentPhase == GameState.GamePhase.MEME_BANK_ACTION && canUseMemeBank);

        if (state.currentPhase == GameState.GamePhase.AUCTION) {
            String auctionText = "Аукцион: осталось " + state.currentAuctionTime + " сек. | ход: " + getAuctionTurnName(state) + " | ставок: " + state.auctionBids.size();
            auctionLabel.setText(auctionText);
            auctionModalLabel.setText(auctionText);
        } else if (state.currentPhase == GameState.GamePhase.MEME_BANK_ACTION && canUseMemeBank) {
            int bankBalance = localPlayer == null ? 0 : localPlayer.memeBankBalance;
            String memeBankText = "Meme Bank: на счету " + bankBalance + " | можно вложить до 500";
            auctionLabel.setText(memeBankText);
            memeBankModalLabel.setText(memeBankText);
        } else {
            auctionLabel.setText("");
            auctionModalLabel.setText("");
            memeBankModalLabel.setText("");
        }
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
        boardRenderer.render(boardCells, state);
        refreshUi(state);
        refreshNotification(state, delta);

        if (chatWidget != null) {
            chatWidget.refresh();
        }
        stage.act(delta);
        stage.draw();
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
        if (exitToMenuButtonTexture != null) {
            exitToMenuButtonTexture.dispose();
        }
        if (depositButtonTexture != null) {
            depositButtonTexture.dispose();
        }
        if (withdrawButtonTexture != null) {
            withdrawButtonTexture.dispose();
        }
        if (participateButtonTexture != null) {
            participateButtonTexture.dispose();
        }
        if (declineButtonTexture != null) {
            declineButtonTexture.dispose();
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
        for (Texture cellTexture : cellTextures) {
            cellTexture.dispose();
        }
        for (Texture memeTexture : memeTextureCache.values()) {
            if (memeTexture != null) {
                memeTexture.dispose();
            }
        }
        if (chatWidget != null) {
            chatWidget.dispose();
        }
        stage.dispose();
    }

    private String buildCellMeta(BoardCell currentCell, GameState state) {
        if (currentCell == null) {
            return "Нет активной клетки.";
        }

        Integer ownerId = state.cellOwners.get(currentCell.id);
        String ownerText = ownerId == null ? "Свободна" : "Владелец: " + state.getPlayerById(ownerId).name;
        String headerText = currentCell.name;
        String priceText = currentCell.type == BoardCell.Type.SITUATION ? "Цена: " + currentCell.price + " монет" : "Тип: " + currentCell.type;
        String mortgageText = state.cellMortgaged.getOrDefault(currentCell.id, false) ? " | заложена" : "";
        if (currentCell.type == BoardCell.Type.MEME_BANK) {
            Player localPlayer = state.getPlayerById(game.getClient().getLocalPlayerId());
            int bankBalance = localPlayer == null ? 0 : localPlayer.memeBankBalance;
            return headerText + "\n" + priceText + "\nТвой баланс: " + bankBalance;
        }
        return headerText + "\n" + priceText + "\n" + ownerText + mortgageText;
    }

    private String buildFeedDescription(GameState state, Player current, BoardCell currentCell, Player localPlayer) {
        if (current == null) {
            return "Ожидаем игрока, которому принадлежит следующий ход.";
        }

        StringBuilder builder = new StringBuilder();
        builder.append("Фаза: ").append(state.currentPhase);
        if (localPlayer != null) {
            builder.append("\nТвои деньги: ").append(localPlayer.money).append(" монет");
        }
        if (currentCell != null) {
            builder.append("\nТекущая клетка: ").append(currentCell.name);
        }
        if (state.lastActionLog != null && !state.lastActionLog.isBlank()) {
            builder.append("\n").append(state.lastActionLog);
        }
        return builder.toString();
    }

    private String buildDiceHint(GameState state, Player current, Player localPlayer) {
        if (current == null) {
            return "Ждём первого активного игрока.";
        }
        if (localPlayer != null && current.id == localPlayer.id) {
            return "Твой ход";
        }
        return "Сейчас ходит " + current.name;
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
                RollDiceRequest request = new RollDiceRequest();
                request.playerId = game.getClient().getLocalPlayerId();
                game.getClient().sendRollDice(request);
            }
        });
        return button;
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
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
            textures[i] = new Texture(i + ".png");
            textures[i].setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        }
        return textures;
    }

    private void layoutBoardOverlays() {
        com.badlogic.gdx.math.Rectangle boardBounds = boardRenderer.getBoardBounds();

        // Кнопки хода и подсказка — в центре экрана
        float diceW = 640f;
        float diceH = 160f;
        diceOverlay.setBounds(
            boardBounds.x + (boardBounds.width - diceW) / 2f,
            boardBounds.y + (boardBounds.height - diceH) / 2f,
            diceW,
            diceH
        );
        // CURRENT CELL и FEED временно отключены
        //com.badlogic.gdx.math.Rectangle currentBounds = boardRenderer.getCurrentCellPanelBounds();
        //com.badlogic.gdx.math.Rectangle feedBounds = boardRenderer.getFeedPanelBounds();
        //currentCellOverlay.setBounds(currentBounds.x, currentBounds.y, currentBounds.width, currentBounds.height);
        //feedOverlay.setBounds(feedBounds.x, feedBounds.y, feedBounds.width, feedBounds.height);

        turnNotificationModal.setFillParent(true);
        buyOrAuctionModal.setFillParent(true);
        auctionModal.setFillParent(true);
        memeBankModal.setFillParent(true);
    }

    private void configureModal(Table modal, Texture texture, VisLabel contentLabel, float minWidth, float minHeight, boolean centerText) {
        modal.setVisible(false);
        modal.center();
        Table window = new Table();
        window.setBackground(new TextureRegionDrawable(new TextureRegion(texture)));
        window.pad(24f);
        modal.clearChildren();
        contentLabel.setWrap(true);
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
        Table details = new Table();
        details.defaults().expandX().center();
        details.add(buyModalCellImage).size(300f, 300f).left();
        buyAuctionModalLabel.setFontScale(1.25f);
        details.add().width(36f);
        details.add(buyAuctionModalLabel).width(380f).left();
        window.add(details).expandX().fillX().pad(4f, 18f, 0f, 18f).row();
        Table controls = new Table();
        controls.add().expandX();
        controls.add(buyButton).size(180, COMMON_BUTTON_HEIGHT).padRight(24f);
        controls.add(passButton).size(180, COMMON_BUTTON_HEIGHT).padLeft(24f);
        controls.add().expandX();
        window.add(controls).expandX().fillX().padTop(10f).padBottom(4f);
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

        Table details = new Table();
        details.left().top();
        details.add(auctionModalCellImage).size(150f, 150f).padRight(20f).top();
        auctionModalLabel.setFontScale(1.15f);
        details.add(auctionModalLabel).width(560f).left().top();
        window.add(details).left().top().pad(6f, 18f, 0f, 18f).row();

        Table controls = new Table();
        controls.left();
        controls.add(bidField).width(220f).height(44f).padRight(18f);
        controls.add(placeBidButton).size(190, COMMON_BUTTON_HEIGHT).padRight(18f);
        controls.add(cancelAuctionButton).width(190f).height(COMMON_BUTTON_HEIGHT).row();
        controls.add(auctionErrorLabel).colspan(3).padTop(6f).left();
        window.add(controls).left().pad(14f, 18f, 0f, 18f);
    }

    private void addMemeBankControls() {
        Table window = (Table) memeBankModal.getCells().first().getActor();
        Table controls = new Table();
        controls.center();
        controls.add(memeBankAmountField).width(220f).height(44f).padRight(12f);
        controls.add(memeBankDepositButton).size(150f, 56f).row();
        controls.add(memeBankErrorLabel).colspan(2).center().padTop(6f).row();
        controls.add(memeBankWithdrawButton).size(150f, 56f).padRight(12f);
        controls.add(memeBankSkipButton).size(150f, 56f).row();
        window.add(controls).center().expand().padTop(10f);
    }

    private Actor createOptionalActionButton(Texture texture, String fallbackText) {
        return texture != null ? createActionButton(texture) : new VisTextButton(fallbackText);
    }

    private Actor createExitToMenuButton() {
        Actor button = exitToMenuButtonTexture != null
            ? createActionButton(exitToMenuButtonTexture)
            : new VisTextButton("Выйти в меню");
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
                .append(player.name)
                .append(';');
        }
        return builder.toString();
    }

    private String buildOwnedCellsSignature(GameState state, Player localPlayer, boolean myTurn, GameState.GamePhase currentPhase) {
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

    private String getAuctionTurnName(GameState state) {
        Player player = state.getPlayerById(state.auctionCurrentPlayerId);
        return player == null ? "—" : player.name;
    }
    private String t(String key) {
        boolean ru = language == Language.RU;
        return switch (key) {
            case "waiting_answers" -> ru ? "Ждём ответов игроков..." : "Waiting for players' answers...";
            case "choose_stakes" -> ru ? "Выбери ставку" : "Choose the stakes";
            case "invite_battle" -> ru ? "Тебя приглашают на мем-баттл! Ставка:" : "You are invited to a meme battle! Stakes:";
            case "organizer_chooses" -> ru ? "Организатор выбирает ставку..." : "Organizer is choosing the stakes...";
            case "topic_placeholder" -> ru ? "Тема или ситуация..." : "Write the situation or topic";
            case "waiting_topic" -> ru ? "Ждём тему от организатора..." : "Waiting for the topic...";
            case "choose_funniest" -> ru ? "Выбери самый смешной мем для ситуации" : "CHOOSE THE FUNNIEST CARD FOR THE SITUATION";
            case "vote_hint" -> ru ? "Голосуй за лучший мем!" : "Vote for the best meme!";
            case "already_voted" -> ru ? "Ты уже проголосовал. Ждём остальных..." : "You already voted. Waiting for others...";
            case "battle_finished" -> ru ? "Баттл завершён!" : "Battle finished!";
            default -> key;
        };
    }
}

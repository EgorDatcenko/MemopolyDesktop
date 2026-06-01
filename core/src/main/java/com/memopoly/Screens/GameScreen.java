package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class GameScreen extends BaseScreen {
    private static final float COMMON_BUTTON_HEIGHT = 64f;
    private static final float WORLD_WIDTH = 1920f;
    private static final float WORLD_HEIGHT = 1080f;
    private static final Color BACKGROUND_COLOR = new Color(0.10f, 0.10f, 0.17f, 1f);
    private static final Color PANEL_COLOR = new Color(0.18f, 0.16f, 0.27f, 0.96f);
    private static final Color PANEL_SHADOW = new Color(0.06f, 0.05f, 0.10f, 0.92f);
    private static final Color TITLE_COLOR = new Color(1.00f, 0.83f, 0.25f, 1f);
    private static final Color TEXT_SOFT = Color.WHITE;
    private static final Color ACCENT_GOLD = new Color(0.99f, 0.83f, 0.29f, 1f);
    private static final String DICE_BUTTON_TEXTURE_PATH = "button_dice.png";
    private static final String MONEY_TEXTURE_PATH = "money.png";
    private static final String BUY_BUTTON_TEXTURE_PATH = "buy_btn.png";
    private static final String PASS_BUTTON_TEXTURE_PATH = "auction_btn.png";
    private static final String END_TURN_BUTTON_TEXTURE_PATH = "end_of_turn_btn.png";
    private static final String PLACE_BID_BUTTON_TEXTURE_PATH = "make_a_bet_btn.png";
    private static final String MORTGAGE_BUTTON_TEXTURE_PATH = "mortgage_btn.png";
    private static final String BUY_BACK_BUTTON_TEXTURE_PATH = "reverse_mortgage_btn.png";
    private static final String EXIT_TO_MENU_BUTTON_TEXTURE_PATH = "exit_to_menu.png";
    private static final String NOTIFICATION_WINDOW_TEXTURE_PATH = "notification_window.png";
    private static final String BUY_AND_AUCTION_WINDOW_TEXTURE_PATH = "buy_and_auction_window.png";
    private static final String AUCTION_OR_MEMEBANK_WINDOW_TEXTURE_PATH = "auction_or_memebank_window.png";
    private static final String SIDEBAR_WINDOW_TEXTURE_PATH = "sidebar_window.jpg";
    private static final String MEME_BATTLE_OVERLAY_TEXTURE_PATH = "meme_battle_overlay.jpg";
    private static final String INPUT_TEXTURE_PATH = "input.png";
    private static final String BACKGROUND_TEXTURE_PATH = "background.png";
    private static final float BUY_AND_AUCTION_MODAL_SCALE = 0.50f;
    private static final float AUCTION_OR_MEME_BANK_MODAL_SCALE = 0.50f;
    private static final float NOTIFICATION_MODAL_SCALE = 0.50f;

    private final Stage stage;
    private final BoardRenderer boardRenderer;
    private final List<BoardCell> boardCells;
    private final Texture diceButtonTexture;
    private final Texture moneyTexture;
    private final Texture buyButtonTexture;
    private final Texture passButtonTexture;
    private final Texture endTurnButtonTexture;
    private final Texture placeBidButtonTexture;
    private final Texture mortgageButtonTexture;
    private final Texture buyBackButtonTexture;
    private final Texture exitToMenuButtonTexture;
    private final Texture notificationWindowTexture;
    private final Texture buyAndAuctionWindowTexture;
    private final Texture auctionOrMemeBankWindowTexture;
    private final Texture sidebarWindowTexture;
    private final Texture memeBattleOverlayTexture;
    private final Texture inputTexture;
    private final Texture backgroundTexture;
    private final Texture[] cellTextures;

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
    private final VisTextField bidField;
    private final VisTextField memeBankAmountField;
    private final VisTextButton memeBankDepositButton;
    private final VisTextButton memeBankWithdrawButton;
    private final VisTextButton memeBankSkipButton;
    private String lastPlayersSignature = "";
    private String lastOwnedCellsSignature = "";

    private Table battleOverlay;
    private Table battleWindow;
    private Cell<Table> battleWindowCell;
    private Table sidePanelContainer;
    private Table sideRoot;
    private VisLabel battleTitleLabel;
    private VisLabel battleTimerLabel;
    private VisLabel battleTopicLabel;
    private Table battleContentTable;
    private VisTextButton battleYesButton;
    private VisTextButton battleNoButton;
    private ChatWidget chatWidget;

    private enum BattleContentMode {
        INVITE, MEME_SELECTION, WAITING, VOTING, RESULTS
    }

    public GameScreen(Memopoly game) {
        super(game);
        Language language = game.getLanguageManager().getLanguage();
        boardRenderer = new BoardRenderer(game);
        stage = new Stage(new FitViewport(WORLD_WIDTH, WORLD_HEIGHT));
        boardCells = BoardData.buildCells();
        diceButtonTexture = new Texture(DICE_BUTTON_TEXTURE_PATH);
        diceButtonTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        moneyTexture = new Texture(MONEY_TEXTURE_PATH);
        moneyTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        buyButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(BUY_BUTTON_TEXTURE_PATH, language));
        passButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(PASS_BUTTON_TEXTURE_PATH, language));
        endTurnButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(END_TURN_BUTTON_TEXTURE_PATH, language));
        placeBidButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(PLACE_BID_BUTTON_TEXTURE_PATH, language));
        mortgageButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(MORTGAGE_BUTTON_TEXTURE_PATH, language));
        buyBackButtonTexture = loadTexture(TexturePathResolver.resolveGameScreenTexture(BUY_BACK_BUTTON_TEXTURE_PATH, language));
        exitToMenuButtonTexture = loadTextureIfExists(TexturePathResolver.resolveGameScreenTexture(EXIT_TO_MENU_BUTTON_TEXTURE_PATH, language));
        notificationWindowTexture = loadTexture(NOTIFICATION_WINDOW_TEXTURE_PATH);
        buyAndAuctionWindowTexture = loadTexture(BUY_AND_AUCTION_WINDOW_TEXTURE_PATH);
        auctionOrMemeBankWindowTexture = loadTexture(AUCTION_OR_MEMEBANK_WINDOW_TEXTURE_PATH);
        sidebarWindowTexture = loadTextureWithFallback(SIDEBAR_WINDOW_TEXTURE_PATH, "sidebar_window.png");
        memeBattleOverlayTexture = loadTextureIfExists(MEME_BATTLE_OVERLAY_TEXTURE_PATH);
        inputTexture = loadTexture(INPUT_TEXTURE_PATH);
        backgroundTexture = loadTexture(BACKGROUND_TEXTURE_PATH);
        cellTextures = loadCellTextures();

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
        passButton = createActionButton(passButtonTexture);
        endTurnButton = createActionButton(endTurnButtonTexture);
        placeBidButton = createActionButton(placeBidButtonTexture);
        bidField = new VisTextField();
        memeBankAmountField = new VisTextField();
        memeBankDepositButton = new VisTextButton("Вложить");
        memeBankWithdrawButton = new VisTextButton("Снять");
        memeBankSkipButton = new VisTextButton("Пропустить");
        applyInputFieldStyle(bidField);
        applyInputFieldStyle(memeBankAmountField);

        createUi();
        createBattleOverlay();
        Gdx.input.setInputProcessor(stage);
    }

    private void createUi() {
        Table root = new Table();
        sideRoot = root;
        root.setFillParent(true);
        root.pad(18);
        root.add().expand().fill();

        Table sidePanel = new Table();
        sidePanelContainer = sidePanel;
        sidePanel.top().left();
        sidePanel.defaults().left().growX().padBottom(10);
        sidePanel.setBackground(new TextureRegionDrawable(new TextureRegion(sidebarWindowTexture)));
        sidePanel.pad(18, 20, 18, 20);

        Table sideInner = new Table();
        sideInner.pad(28, 14, 12, 14);
        sideInner.top().left();
        sideInner.defaults().left().growX().padBottom(10);

        diceTitleLabel.setColor(Color.WHITE);
        diceTitleLabel.setFontScale(0.92f);
        diceHintLabel.setWrap(true);
        diceHintLabel.setColor(Color.WHITE);
        diceHintLabel.setFontScale(1.00f);
        diceOverlay.top().left();
        diceOverlay.defaults().left();
        Table diceContent = new Table();
        diceContent.left().top();
        Table diceButtonColumn = new Table();
        diceButtonColumn.add(diceButton).size(150, 84).row();
        diceButtonColumn.add(endTurnButton).size(150, COMMON_BUTTON_HEIGHT).padTop(2f);
        diceContent.add(diceButtonColumn).padLeft(62).padRight(18).top();
        diceContent.add(diceHintLabel).width(405).top().left().padTop(22).padRight(6);
        diceOverlay.add(diceContent).left().padTop(70);

        currentCellTitleLabel.setColor(Color.WHITE);
        currentCellTitleLabel.setFontScale(0.92f);
        currentCellMetaLabel.setWrap(true);
        currentCellMetaLabel.setColor(Color.WHITE);
        currentCellMetaLabel.setFontScale(1.00f);
        currentCellOverlay.top().left();
        currentCellOverlay.defaults().left();
        Table currentCellContent = new Table();
        currentCellContent.left().top();
        currentCellContent.add(currentCellImage).size(138, 138).padLeft(12).padRight(10).top();
        currentCellContent.add(currentCellMetaLabel).width(190).top().left().padTop(-4);
        currentCellOverlay.add(currentCellContent).left().padLeft(10).padTop(-18).padBottom(10);

        feedTitleLabel.setFontScale(0.92f);
        feedDescriptionLabel.setWrap(true);
        feedDescriptionLabel.setColor(Color.WHITE);
        feedDescriptionLabel.setFontScale(0.90f);
        feedOverlay.top().left();
        feedOverlay.defaults().left();
        feedOverlay.add(feedDescriptionLabel).width(235).padLeft(36).padTop(-10).row();
        auctionLabel.setFontScale(0.76f);
        feedOverlay.add(auctionLabel).width(235).padLeft(36).padTop(2);

        titleLabel.setColor(TITLE_COLOR);
        titleLabel.setFontScale(1.05f);
        phaseLabel.setColor(TEXT_SOFT);
        phaseLabel.setWrap(true);
        turnLabel.setColor(Color.WHITE);
        turnLabel.setWrap(true);
        cellLabel.setColor(Color.WHITE);
        cellLabel.setWrap(true);
        logLabel.setWrap(true);
        logLabel.setColor(Color.WHITE);
        applyGameScreenLineSpacing();
        auctionLabel.setWrap(true);
        auctionLabel.setColor(Color.WHITE);
        playersTable.top().left();
        ownedCellsTable.top().left();

        ScrollPane playersScroll = new ScrollPane(playersTable);
        playersScroll.setFadeScrollBars(false);
        playersScroll.setScrollingDisabled(true, false);
        playersScroll.getStyle().background = panel(new Color(0.13f, 0.12f, 0.20f, 0.95f));

        ScrollPane ownedScroll = new ScrollPane(ownedCellsTable);
        ownedScroll.setFadeScrollBars(false);
        ownedScroll.setScrollingDisabled(true, false);
        ownedScroll.getStyle().background = panel(new Color(0.13f, 0.12f, 0.20f, 0.95f));

        VisLabel playersTitle = new VisLabel("Игроки");
        playersTitle.setColor(TITLE_COLOR);
        VisLabel ownedTitle = new VisLabel("Мои клетки");
        ownedTitle.setColor(TITLE_COLOR);
        VisLabel actionTitle = new VisLabel("Действия");
        actionTitle.setColor(TITLE_COLOR);

        sideInner.add(titleLabel).row();
        sideInner.add(phaseLabel).width(340).row();
        sideInner.add(turnLabel).width(340).row();
        sideInner.add(cellLabel).width(340).row();
        sideInner.add(logLabel).width(340).padBottom(8).row();

        sideInner.add(playersTitle).padTop(12).row();
        sideInner.add(playersScroll).width(340).height(170).row();

        sideInner.add(ownedTitle).padTop(12).row();
        sideInner.add(ownedScroll).width(340).height(180).row();

        Table actionsTable = new Table();
        actionsTable.defaults().padRight(8).padBottom(8);

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
                if (bid > 0) {
                    sendAction(GameActionRequest.ActionType.PLACE_AUCTION_BID, 0, bid);
                    bidField.setText("");
                }
            }
        });

        memeBankAmountField.setMessageText("Сумма");
        memeBankDepositButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                int amount = parseAmount(memeBankAmountField);
                if (amount > 0) {
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

        bidField.setMessageText("Ставка");
        Actor exitToMenuButton = createExitToMenuButton();

        sideInner.add(actionTitle).padTop(12).row();
        sideInner.add(actionsTable).row();
        sideInner.add(exitToMenuButton).width(220).padTop(10).row();
        sidePanel.clearChildren();
        sidePanel.add(sideInner).width(360);
        root.add(sidePanel).width(390).top().right();


        configureModal(turnNotificationModal, notificationWindowTexture, turnModalLabel, NOTIFICATION_MODAL_SCALE, true);
        configureModal(buyOrAuctionModal, buyAndAuctionWindowTexture, buyAuctionModalLabel, BUY_AND_AUCTION_MODAL_SCALE, false);
        configureModal(auctionModal, auctionOrMemeBankWindowTexture, auctionModalLabel, AUCTION_OR_MEME_BANK_MODAL_SCALE, false);
        configureModal(memeBankModal, auctionOrMemeBankWindowTexture, memeBankModalLabel, AUCTION_OR_MEME_BANK_MODAL_SCALE, false);
        setupModalControls();

        stage.addActor(root);
        stage.addActor(diceOverlay);
        stage.addActor(currentCellOverlay);
        stage.addActor(feedOverlay);
        stage.addActor(buyOrAuctionModal);
        stage.addActor(auctionModal);
        stage.addActor(memeBankModal);

        Table chatRoot = new Table();
        chatRoot.setFillParent(true);
        chatWidget = new ChatWidget(game);
        chatRoot.add(chatWidget).width(380f).height(210f).expand().left().bottom().padLeft(24f).padBottom(24f);
        stage.addActor(chatRoot);
        layoutBoardOverlays();
    }

    private void createBattleOverlay() {
        battleOverlay = new Table();
        battleOverlay.setFillParent(true);
        battleOverlay.setBackground(panel(new Color(0f, 0f, 0f, 0.55f))); // затемнение

        Table panel = new Table();
        battleWindow = panel;
        if (memeBattleOverlayTexture != null) {
            panel.setBackground(new TextureRegionDrawable(new TextureRegion(memeBattleOverlayTexture)));
        } else {
            panel.setBackground(panel(new Color(0.18f, 0.16f, 0.27f, 0.98f)));
        }
        panel.pad(28f, 34f, 28f, 34f);
        panel.top();

        battleTitleLabel = new VisLabel("Мем-баттл!");
        battleTitleLabel.setColor(TITLE_COLOR);
        battleTitleLabel.setFontScale(1.35f);

        battleTimerLabel = new VisLabel("30");
        battleTimerLabel.setColor(Color.RED);
        battleTimerLabel.setFontScale(1.25f);

        battleTopicLabel = new VisLabel("");
        battleTopicLabel.setColor(Color.WHITE);
        battleTopicLabel.setWrap(true);
        battleTopicLabel.setFontScale(1.05f);

        battleContentTable = new Table();

        battleYesButton = new VisTextButton("Участвовать");
        battleNoButton = new VisTextButton("Отказаться");

        battleYesButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                BattleResponsePacket packet = new BattleResponsePacket();
                packet.playerId = game.getClient().getLocalPlayerId();
                packet.accepted = true;
                game.getClient().sendBattleResponse(packet);
            }
        });

        battleNoButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                BattleResponsePacket packet = new BattleResponsePacket();
                packet.playerId = game.getClient().getLocalPlayerId();
                packet.accepted = false;
                game.getClient().sendBattleResponse(packet);
            }
        });

        panel.add(battleTitleLabel)
            .colspan(2)
            .center()
            .padTop(58f)
            .padBottom(6f)
            .row();

        VisLabel timerTitleLabel = new VisLabel("Осталось:");
        timerTitleLabel.setFontScale(0.95f);

        panel.add(timerTitleLabel).padRight(8f);
        panel.add(battleTimerLabel).left().row();

        panel.add(battleTopicLabel)
            .colspan(2)
            .width(560f)
            .padBottom(10f)
            .row();

        panel.add(battleContentTable)
            .colspan(2)
            .width(610f)
            .row();

        Table buttons = new Table();
        buttons.add(battleYesButton).width(180f).height(COMMON_BUTTON_HEIGHT).padRight(16f);
        buttons.add(battleNoButton).width(180f).height(COMMON_BUTTON_HEIGHT);
        panel.add(buttons).colspan(2).center().padTop(16f);

        battleWindowCell = battleOverlay.add(panel).size(860f, 740f).center();
        battleOverlay.setVisible(false);

        stage.addActor(battleOverlay);
    }

    private void rebuildBattleContent(GameState state, BattleContentMode mode) {
        battleContentTable.clearChildren();
        int localId = game.getClient().getLocalPlayerId();
        Player localPlayer = state.getPlayerById(localId);

        switch (mode) {
            case INVITE:
                VisLabel inviteLabel = new VisLabel("Тебя приглашают на мем-баттл!");
                inviteLabel.setColor(Color.WHITE);
                battleContentTable.add(inviteLabel).center();
                break;

            case WAITING:
                VisLabel waitLabel = new VisLabel("Ждём пока все выберут мемы...");
                waitLabel.setColor(Color.WHITE);
                battleContentTable.add(waitLabel).center();
                break;

            case MEME_SELECTION:
                if (localPlayer == null || localPlayer.handMemes.isEmpty()) {
                    battleContentTable.add(new VisLabel("Нет мемов в руке!")).center();
                    break;
                }
                // Сетка мемов из руки
                for (Meme meme : localPlayer.handMemes) {
                    VisTextButton memeButton = new VisTextButton(meme.description);
                    memeButton.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            sendAction(GameActionRequest.ActionType.SUBMIT_MEME, meme.id, 0);
                        }
                    });
                    battleContentTable.add(memeButton).width(200f).height(COMMON_BUTTON_HEIGHT).pad(8f);
                }
                break;

            case VOTING:
                if (state.battleMemes == null || state.battleMemes.isEmpty()) {
                    battleContentTable.add(new VisLabel("Мемы загружаются...")).center();
                    break;
                }
                boolean hasVoted = state.battleVoters != null && state.battleVoters.contains(localId);
                if (hasVoted) {
                    battleContentTable.add(new VisLabel("Ты уже проголосовал. Ждём остальных...")).center();
                    break;
                }
                // Анонимные карточки мемов
                for (int i = 0; i < state.battleMemes.size(); i++) {
                    Meme meme = state.battleMemes.get(i);
                    final int memeId = meme.id;
                    Table memeCard = new Table();
                    memeCard.setBackground(panel(new Color(0.25f, 0.22f, 0.36f, 1f)));
                    memeCard.pad(12f);
                    VisLabel memeNum = new VisLabel("Мем #" + (i + 1));
                    memeNum.setColor(Color.WHITE);
                    VisTextButton voteButton = new VisTextButton("Голосовать");
                    voteButton.addListener(new ChangeListener() {
                        @Override
                        public void changed(ChangeEvent event, Actor actor) {
                            sendAction(GameActionRequest.ActionType.VOTE_MEME, memeId, 0);
                        }
                    });
                    memeCard.add(memeNum).row();
                    memeCard.add(voteButton).width(140f).height(44f).padTop(8f);
                    battleContentTable.add(memeCard).width(180f).pad(8f);
                }
                break;

            case RESULTS:
                VisLabel resultsLabel = new VisLabel(state.lastActionLog == null ? "Баттл завершён!" : state.lastActionLog);
                resultsLabel.setColor(ACCENT_GOLD);
                resultsLabel.setWrap(true);
                battleContentTable.add(resultsLabel).width(600f).center();
                break;
        }
    }

    private void refreshBattleOverlay(GameState state) {
        if (state == null || state.currentPhase != GameState.GamePhase.MEME_BATTLE) {
            battleOverlay.setVisible(false);
            return;
        }

        battleOverlay.setVisible(true);
        int timerValue = state.battleTimerSeconds > 0 ? state.battleTimerSeconds : state.currentAuctionTime;
        battleTimerLabel.setText(String.valueOf(Math.max(0, timerValue)));
        battleTopicLabel.setText("Тема: " + (state.battleTopic == null ? "—" : state.battleTopic));

        int localId = game.getClient().getLocalPlayerId();
        boolean isParticipant = state.battleParticipants.contains(localId);
        boolean isInvited = state.battleInvited != null && state.battleInvited.contains(localId);

        switch (state.battlePhase) {
            case INVITE:
                battleTitleLabel.setText("Мем-баттл! Ставка: " + state.battleStakes);
                battleYesButton.setVisible(isInvited);
                battleNoButton.setVisible(isInvited);
                rebuildBattleContent(state, BattleContentMode.INVITE);
                break;

            case COLLECTING_MEMES:
                battleTitleLabel.setText("Выбери мем!");
                battleYesButton.setVisible(false);
                battleNoButton.setVisible(false);
                if (isParticipant) {
                    rebuildBattleContent(state, BattleContentMode.MEME_SELECTION);
                } else {
                    rebuildBattleContent(state, BattleContentMode.WAITING);
                }
                break;

            case VOTING:
                battleTitleLabel.setText("Голосование!");
                battleYesButton.setVisible(false);
                battleNoButton.setVisible(false);
                rebuildBattleContent(state, BattleContentMode.VOTING);
                break;

            case RESULTS:
                battleTitleLabel.setText("Результаты!");
                battleYesButton.setVisible(false);
                battleNoButton.setVisible(false);
                rebuildBattleContent(state, BattleContentMode.RESULTS);
                break;

            default:
                break;
        }
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
        GameActionRequest request = new GameActionRequest();
        request.actionType = type;
        request.targetId = targetId;
        request.amount = amount;
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
            buyModalCellImage.setDrawable(null);
            auctionModalCellImage.setDrawable(null);
            diceHintLabel.setText("Кнопка броска появится, когда ход будет твоим.");
            feedDescriptionLabel.setText("Подключаемся к игровой комнате и ждём актуальное состояние матча.");
            rebuildPlayersIfNeeded(null, null, -1);
            rebuildOwnedCellsIfNeeded(null, null, false, GameState.GamePhase.WAITING);
            setButtonsEnabled(false, false, false, false, false);
            auctionLabel.setText("");
            refreshBattleOverlay(state);
            return;
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
        diceHintLabel.setText(buildDiceHint(state, current, localPlayer));
        feedDescriptionLabel.setText(buildFeedDescription(state, current, currentCell, localPlayer));
        turnModalLabel.setText(current == null ? "Ход: -" : "Сейчас ходит: " + current.name);
        buyAuctionModalLabel.setText(buildCellMeta(currentCell, state));

        rebuildPlayersIfNeeded(state, current, localPlayerId);
        rebuildOwnedCellsIfNeeded(state, localPlayer, myTurn, state.currentPhase);
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

    private void rebuildPlayers(GameState state, Player current, int localPlayerId) {
        playersTable.clearChildren();
        for (Player player : state.players) {
            String marker = player.id == localPlayerId ? " (ты)" : "";
            String active = current != null && current.id == player.id ? " <- ход" : "";
            String bankrupt = player.isBankrupt ? " [банкрот]" : "";
            Table row = new Table();
            row.setBackground(panel(new Color(0.22f, 0.20f, 0.33f, 0.95f)));
            row.pad(8, 10, 8, 10);
            VisLabel nameLabel = new VisLabel(player.name + marker + active + bankrupt);
            nameLabel.setColor(Color.WHITE);
            nameLabel.setWrap(true);
            Table moneyCell = createMoneyValue(player.money);
            row.add(nameLabel).width(220).left().padRight(8);
            row.add(moneyCell).right();
            playersTable.add(row).width(332).left().padBottom(8).row();
        }
    }

    private void rebuildOwnedCells(GameState state, Player localPlayer, boolean myTurn, GameState.GamePhase currentPhase) {
        ownedCellsTable.clearChildren();
        if (localPlayer == null || localPlayer.ownedCells.isEmpty()) {
            VisLabel emptyLabel = new VisLabel("Пока нет купленных клеток");
            emptyLabel.setColor(TEXT_SOFT);
            emptyLabel.setWrap(true);
            ownedCellsTable.add(emptyLabel).width(332).left().row();
            return;
        }

        for (int cellId : localPlayer.ownedCells) {
            BoardCell cell = boardCells.get(cellId);
            boolean mortgaged = state.cellMortgaged.getOrDefault(cellId, false);

            Table row = new Table();
            row.setBackground(panel(new Color(0.22f, 0.20f, 0.33f, 0.95f)));
            row.pad(8, 10, 8, 10);
            row.left();
            VisLabel cellInfo = new VisLabel(cell.name + (mortgaged ? " | заложена" : ""));
            cellInfo.setColor(Color.WHITE);
            cellInfo.setWrap(true);
            row.add(cellInfo).width(164).left().padRight(8);
            row.add(createMoneyValue(cell.price)).padRight(8);

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

            row.add(actionButton).size(104, COMMON_BUTTON_HEIGHT);
            ownedCellsTable.add(row).width(332).left().padBottom(8).row();
        }
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

        updateAuctionCellImage(state);

        setButtonsEnabled(canRoll, canBuyOrPass, canBuyOrPass, canEndTurn, canBid);
        diceButton.setVisible(showTurnControls);
        buyButton.setVisible(canBuyOrPass);
        passButton.setVisible(canBuyOrPass);
        endTurnButton.setVisible(canEndTurn);
        bidField.setVisible(canBid);
        placeBidButton.setVisible(canBid);
        memeBankAmountField.setVisible(canDepositToMemeBank);
        memeBankDepositButton.setVisible(canDepositToMemeBank);
        memeBankWithdrawButton.setVisible(canWithdrawFromMemeBank);
        memeBankSkipButton.setVisible(canUseMemeBank);
        memeBankAmountField.setDisabled(!canDepositToMemeBank);
        memeBankDepositButton.setDisabled(!canDepositToMemeBank);
        memeBankWithdrawButton.setDisabled(!canWithdrawFromMemeBank);
        memeBankSkipButton.setDisabled(!canUseMemeBank);

        turnNotificationModal.setVisible(false);
        buyOrAuctionModal.setVisible(canBuyOrPass);
        auctionModal.setVisible(state.currentPhase == GameState.GamePhase.AUCTION);
        memeBankModal.setVisible(state.currentPhase == GameState.GamePhase.MEME_BANK_ACTION && canUseMemeBank);

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

    private void updateAuctionCellImage(GameState state) {
        if (state == null || state.auctionCellId < 0 || state.auctionCellId >= cellTextures.length) {
            auctionModalCellImage.setDrawable(null);
            return;
        }

        auctionModalCellImage.setDrawable(
            new TextureRegionDrawable(new TextureRegion(cellTextures[state.auctionCellId]))
        );
    }

    private void setButtonsEnabled(boolean roll, boolean buy, boolean pass, boolean endTurn, boolean bid) {
        diceButton.setDisabled(!roll);
        buyButton.setDisabled(!buy);
        passButton.setDisabled(!pass);
        endTurnButton.setDisabled(!endTurn);
        placeBidButton.setDisabled(!bid);
        bidField.setDisabled(!bid);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        Gdx.gl.glClearColor(0f, 0f, 0f, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        renderBackground();

        GameState state = game.getLatestGameState();
        boardRenderer.render(boardCells, state);
        refreshUi(state);

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
        passButtonTexture.dispose();
        endTurnButtonTexture.dispose();
        placeBidButtonTexture.dispose();
        mortgageButtonTexture.dispose();
        buyBackButtonTexture.dispose();
        if (exitToMenuButtonTexture != null) {
            exitToMenuButtonTexture.dispose();
        }
        notificationWindowTexture.dispose();
        buyAndAuctionWindowTexture.dispose();
        auctionOrMemeBankWindowTexture.dispose();
        sidebarWindowTexture.dispose();
        if (memeBattleOverlayTexture != null) {
            memeBattleOverlayTexture.dispose();
        }
        inputTexture.dispose();
        backgroundTexture.dispose();
        for (Texture cellTexture : cellTextures) {
            cellTexture.dispose();
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
            return "← Твой ход";
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
        style.imageOver = drawable.tint(new Color(1f, 1f, 1f, 0.97f));
        style.imageDown = drawable.tint(new Color(0.86f, 0.86f, 0.86f, 1f));
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
        com.badlogic.gdx.math.Rectangle diceBounds = boardRenderer.getDicePanelBounds();
        com.badlogic.gdx.math.Rectangle currentBounds = boardRenderer.getCurrentCellPanelBounds();
        com.badlogic.gdx.math.Rectangle feedBounds = boardRenderer.getFeedPanelBounds();
        com.badlogic.gdx.math.Rectangle boardBounds = boardRenderer.getBoardBounds();

        diceOverlay.setBounds(diceBounds.x, diceBounds.y, diceBounds.width, diceBounds.height);
        currentCellOverlay.setBounds(currentBounds.x, currentBounds.y, currentBounds.width, currentBounds.height);
        feedOverlay.setBounds(feedBounds.x, feedBounds.y, feedBounds.width, feedBounds.height);
        if (sidePanelContainer != null) {
            sidePanelContainer.setHeight(boardBounds.height);
            sidePanelContainer.setY(boardBounds.y);
        }

        turnNotificationModal.setFillParent(true);
        buyOrAuctionModal.setFillParent(true);
        auctionModal.setFillParent(true);
        memeBankModal.setFillParent(true);
        if (battleWindowCell != null) {
            com.badlogic.gdx.math.Rectangle battleBounds = boardRenderer.getBoardBounds();
            float battleWidth = battleBounds.width * 0.82f;
            float battleHeight = battleBounds.height * 0.70f;
            battleWindowCell.size(battleWidth, battleHeight);
            battleOverlay.invalidateHierarchy();
        }
    }

    private void configureModal(Table modal, Texture texture, VisLabel contentLabel, float scale, boolean centerText) {
        modal.setVisible(false);
        modal.center();
        Table window = new Table();
        window.setBackground(new TextureRegionDrawable(new TextureRegion(texture)));
        window.pad(20f);
        window.top().left();
        modal.clearChildren();
        float modalWidth = texture.getWidth() * scale;
        float modalHeight = texture.getHeight() * scale;
        contentLabel.setWrap(true);
        if (centerText) {
            contentLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
            window.add(contentLabel).width(modalWidth * 0.8f).expand().center().pad(12f).row();
        } else {
            contentLabel.setAlignment(com.badlogic.gdx.utils.Align.left);
            window.add(contentLabel).width(modalWidth * 0.8f).left().top().pad(12f).row();
        }
        modal.add(window).size(modalWidth, modalHeight).center();
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

        auctionModalLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        window.getCells().first().center();

        Table content = new Table();

        content.add(auctionModalCellImage)
            .size(180f, 180f)
            .left()
            .padRight(28f);

        Table controls = new Table();
        controls.add(bidField)
            .width(280f)
            .height(COMMON_BUTTON_HEIGHT)
            .padRight(12f);

        controls.add(placeBidButton)
            .size(180f, COMMON_BUTTON_HEIGHT);

        content.add(controls)
            .center()
            .padTop(80f);

        window.add(content)
            .expandX()
            .center()
            .padTop(44f)
            .row();
    }

    private void addMemeBankControls() {
        Table window = (Table) memeBankModal.getCells().first().getActor();

        memeBankModalLabel.setAlignment(com.badlogic.gdx.utils.Align.center);
        window.getCells().first().center();

        Table controls = new Table();
        controls.defaults().center();

        controls.add(memeBankAmountField)
            .width(300f)
            .height(COMMON_BUTTON_HEIGHT)
            .colspan(3)
            .center()
            .row();

        controls.add(memeBankDepositButton)
            .width(180f)
            .height(COMMON_BUTTON_HEIGHT)
            .padTop(28f)
            .padRight(10f);

        controls.add(memeBankWithdrawButton)
            .width(180f)
            .height(COMMON_BUTTON_HEIGHT)
            .padTop(28f)
            .padRight(10f);

        controls.add(memeBankSkipButton)
            .width(180f)
            .height(COMMON_BUTTON_HEIGHT)
            .padTop(28f);

        window.add(controls)
            .expandX()
            .center()
            .padTop(72f);
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

        TextureRegionDrawable inputBackground = new TextureRegionDrawable(new TextureRegion(inputTexture));
        inputBackground.setLeftWidth(18f);
        inputBackground.setRightWidth(10f);
        inputBackground.setTopHeight(8f);
        inputBackground.setBottomHeight(8f);
        inputBackground.setMinHeight(COMMON_BUTTON_HEIGHT);

        style.background = inputBackground;
        style.backgroundOver = inputBackground;
        style.focusedBackground = inputBackground;
        style.disabledBackground = inputBackground;

        field.setStyle(style);
        field.setAlignment(com.badlogic.gdx.utils.Align.left);
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

    private String getAuctionTurnName(GameState state) {
        Player player = state.getPlayerById(state.auctionCurrentPlayerId);
        return player == null ? "—" : player.name;
    }

}

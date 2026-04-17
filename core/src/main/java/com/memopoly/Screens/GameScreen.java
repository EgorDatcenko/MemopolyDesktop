package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.memopoly.Memopoly;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.BoardData;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.network.packets.GameActionRequest;
import com.memopoly.network.packets.RollDiceRequest;

import java.util.List;

public class GameScreen extends BaseScreen {
    private final Stage stage;
    private final BoardRenderer boardRenderer;
    private final List<BoardCell> boardCells;

    private final VisLabel titleLabel;
    private final VisLabel phaseLabel;
    private final VisLabel turnLabel;
    private final VisLabel cellLabel;
    private final VisLabel logLabel;
    private final VisLabel auctionLabel;
    private final Table playersTable;
    private final Table ownedCellsTable;

    private final VisTextButton rollButton;
    private final VisTextButton buyButton;
    private final VisTextButton passButton;
    private final VisTextButton endTurnButton;
    private final VisTextButton placeBidButton;
    private final VisTextField bidField;

    public GameScreen(Memopoly game) {
        super(game);
        stage = new Stage(new ScreenViewport());
        boardRenderer = new BoardRenderer(game);
        boardCells = BoardData.buildCells();

        titleLabel = new VisLabel("Мемополия");
        phaseLabel = new VisLabel("Фаза: -");
        turnLabel = new VisLabel("Ход: -");
        cellLabel = new VisLabel("Клетка: -");
        logLabel = new VisLabel("События появятся здесь");
        auctionLabel = new VisLabel("");
        playersTable = new Table();
        ownedCellsTable = new Table();

        rollButton = new VisTextButton("Бросить кубики");
        buyButton = new VisTextButton("Купить");
        passButton = new VisTextButton("Пропустить");
        endTurnButton = new VisTextButton("Закончить ход");
        placeBidButton = new VisTextButton("Сделать ставку");
        bidField = new VisTextField();

        createUi();
        Gdx.input.setInputProcessor(stage);
    }

    private void createUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(18);

        Table sidePanel = new Table();
        sidePanel.top().left();
        sidePanel.defaults().left().growX().padBottom(8);

        sidePanel.add(titleLabel).row();
        sidePanel.add(phaseLabel).row();
        sidePanel.add(turnLabel).row();
        sidePanel.add(cellLabel).row();
        sidePanel.add(logLabel).width(360).row();

        sidePanel.add(new VisLabel("Игроки")).padTop(12).row();
        ScrollPane playersScroll = new ScrollPane(playersTable);
        playersScroll.setFadeScrollBars(false);
        sidePanel.add(playersScroll).width(360).height(170).row();

        sidePanel.add(new VisLabel("Мои клетки")).padTop(12).row();
        ScrollPane ownedScroll = new ScrollPane(ownedCellsTable);
        ownedScroll.setFadeScrollBars(false);
        sidePanel.add(ownedScroll).width(360).height(180).row();

        Table actionsTable = new Table();
        actionsTable.defaults().padRight(8).padBottom(8);

        rollButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                RollDiceRequest request = new RollDiceRequest();
                request.playerId = game.getClient().getLocalPlayerId();
                game.getClient().sendRollDice(request);
            }
        });

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

        actionsTable.add(rollButton);
        actionsTable.add(buyButton);
        actionsTable.row();
        actionsTable.add(passButton);
        actionsTable.add(endTurnButton);
        actionsTable.row();
        actionsTable.add(bidField).width(120);
        actionsTable.add(placeBidButton);

        sidePanel.add(new VisLabel("Действия")).padTop(12).row();
        sidePanel.add(actionsTable).row();
        sidePanel.add(auctionLabel).padTop(6).row();

        root.add().expandX();
        root.add(sidePanel).width(380).top().right();

        stage.addActor(root);
    }

    private int parseBid() {
        try {
            return Integer.parseInt(bidField.getText().trim());
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
            playersTable.clearChildren();
            ownedCellsTable.clearChildren();
            setButtonsEnabled(false, false, false, false, false);
            auctionLabel.setText("");
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

        rebuildPlayers(state, current, localPlayerId);
        rebuildOwnedCells(state, localPlayer);
        refreshActions(state, myTurn, currentCell);
    }

    private void rebuildPlayers(GameState state, Player current, int localPlayerId) {
        playersTable.clearChildren();
        for (Player player : state.players) {
            String marker = player.id == localPlayerId ? " (ты)" : "";
            String active = current != null && current.id == player.id ? " <- ход" : "";
            String bankrupt = player.isBankrupt ? " [банкрот]" : "";
            playersTable.add(new VisLabel(player.name + marker + " | $" + player.money + active + bankrupt)).left().row();
        }
    }

    private void rebuildOwnedCells(GameState state, Player localPlayer) {
        ownedCellsTable.clearChildren();
        if (localPlayer == null || localPlayer.ownedCells.isEmpty()) {
            ownedCellsTable.add(new VisLabel("Пока нет купленных клеток")).left().row();
            return;
        }

        for (int cellId : localPlayer.ownedCells) {
            BoardCell cell = boardCells.get(cellId);
            boolean mortgaged = state.cellMortgaged.getOrDefault(cellId, false);

            Table row = new Table();
            row.left();
            row.add(new VisLabel(cell.name + " | $" + cell.price + (mortgaged ? " | заложена" : ""))).width(210).left().padRight(8);

            VisTextButton actionButton = new VisTextButton(mortgaged ? "Выкупить" : "Заложить");
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

            row.add(actionButton).width(110);
            ownedCellsTable.add(row).left().row();
        }
    }

    private void refreshActions(GameState state, boolean myTurn, BoardCell currentCell) {
        boolean canRoll = myTurn && state.currentPhase == GameState.GamePhase.PLAYING;
        boolean canBuyOrPass = myTurn && state.currentPhase == GameState.GamePhase.PLAYER_ACTION
            && currentCell != null && currentCell.type == BoardCell.Type.SITUATION;
        boolean canEndTurn = myTurn && state.currentPhase == GameState.GamePhase.PLAYING;
        boolean canBid = state.currentPhase == GameState.GamePhase.AUCTION;

        setButtonsEnabled(canRoll, canBuyOrPass, canBuyOrPass, canEndTurn, canBid);

        if (state.currentPhase == GameState.GamePhase.AUCTION) {
            auctionLabel.setText("Аукцион: осталось " + state.currentAuctionTime + " сек. | ставок: " + state.auctionBids.size());
        } else {
            auctionLabel.setText("");
        }
    }

    private void setButtonsEnabled(boolean roll, boolean buy, boolean pass, boolean endTurn, boolean bid) {
        rollButton.setDisabled(!roll);
        buyButton.setDisabled(!buy);
        passButton.setDisabled(!pass);
        endTurnButton.setDisabled(!endTurn);
        placeBidButton.setDisabled(!bid);
        bidField.setDisabled(!bid);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        GameState state = game.getLatestGameState();
        boardRenderer.render(boardCells, state);
        refreshUi(state);

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        boardRenderer.resize(width, height);
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        boardRenderer.dispose();
        stage.dispose();
    }
}

package com.memopoly;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.kotcrab.vis.ui.VisUI;
import com.memopoly.Screens.BaseScreen;
import com.memopoly.Screens.LobbyScreen;
import com.memopoly.Screens.MainMenuScreen;
import com.memopoly.Screens.ScreenManager;
import com.memopoly.game.model.GameState;
import com.memopoly.network.GameClient;
import com.memopoly.network.GameServer;
import com.memopoly.network.NetworkListener;
import com.memopoly.network.packets.RollDiceResponse;

public class Memopoly extends Game implements NetworkListener {
    public SpriteBatch batch;
    public ScreenManager screenManager;

    private GameServer gameServer;
    private GameClient gameClient;
    private boolean isHost;
    private volatile GameState latestGameState;
    private boolean lobbyOpened;

    @Override
    public void create() {
        VisUI.load();
        screenManager = new ScreenManager(this);
        batch = new SpriteBatch();
        gameClient = new GameClient(this);
        screenManager.set(new MainMenuScreen(this));
    }

    @Override
    public void onGameStateUpdated(GameState gameState) {
        latestGameState = gameState;
        Gdx.app.log("Network", "State updated: " + gameState.turnCount + ", phase=" + gameState.currentPhase);

        if (isHost && !lobbyOpened && gameState != null && gameState.players != null && !gameState.players.isEmpty()) {
            openLobby();
            lobbyOpened = true;
        }
    }

    @Override
    public void onDiceRolled(RollDiceResponse response) {
        Gdx.app.log("Network", response.playerId + " rolled " + response.total);
    }

    @Override
    public void onConnected() {
        System.out.println("Подключено успешно!");
        Gdx.app.log("Network", "Connected to server!");
    }

    @Override
    public void onJoinedRoom() {
        openLobby();
        lobbyOpened = true;
    }

    @Override
    public void onDisconnected() {
        Gdx.app.log("Network", "Disconnected from server!");
    }

    @Override
    public void render() {
        super.render();
    }

    public SpriteBatch getBatch() {
        return batch;
    }

    public GameServer getServer() {
        return gameServer;
    }

    public GameClient getClient() {
        return gameClient;
    }

    public boolean isHost() {
        return isHost;
    }

    public GameState getLatestGameState() {
        return latestGameState;
    }

    public void openLobby() {
        screenManager.push(new LobbyScreen(this));
    }

    public void openMenu() {
        screenManager.pop();
        screenManager.push(new MainMenuScreen(this));
    }

    public void leaveRoomToMenu() {
        if (gameServer != null) {
            gameServer.stop();
            gameServer = null;
        }
        if (gameClient != null) {
            gameClient.disconnect();
        }
        gameClient = new GameClient(this);

        latestGameState = null;
        isHost = false;
        lobbyOpened = false;
        openMenu();
    }

    public void startAsHost() {
        isHost = true;
        lobbyOpened = false;
        latestGameState = null;
        gameServer = new GameServer();
    }

    public void connectAsGuest(String ip, int port, String playerName) {
        isHost = false;
        lobbyOpened = false;
        latestGameState = null;
        gameClient.connectAndJoin(ip, port, playerName);
    }

    public void startGameAsHost() {
        if (gameServer != null) {
            gameServer.startGame();
        }
    }

    public String getRoomCode() {
        if (gameServer != null) {
            return gameServer.getRoomCode();
        }
        return "UNKNOWN";
    }

    @Override
    public void dispose() {
        batch.dispose();
        VisUI.dispose();
        if (gameServer != null) {
            gameServer.stop();
        }
        if (gameClient != null) {
            gameClient.disconnect();
        }
        super.dispose();
    }
}

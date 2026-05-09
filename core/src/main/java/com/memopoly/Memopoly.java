package com.memopoly;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.kotcrab.vis.ui.VisUI;
import com.memopoly.Screens.*;
import com.memopoly.game.model.GameState;
import com.memopoly.network.GameClient;
import com.memopoly.network.GameServer;
import com.memopoly.network.NetworkListener;
import com.memopoly.network.packets.RollDiceResponse;
import com.memopoly.network.packets.StartGameRequest;
import com.memopoly.utils.LanguageManager;

public class Memopoly extends Game implements NetworkListener {
    private static final String SETTINGS_PREFS = "memopoly-settings";
    private static final int WINDOWED_WIDTH = 1600;
    private static final int WINDOWED_HEIGHT = 900;

    public SpriteBatch batch;
    public ScreenManager screenManager;

    private GameServer gameServer;
    private GameClient gameClient;
    private boolean isHost;
    private volatile GameState latestGameState;
    private boolean lobbyOpened;
    private LanguageManager languageManager;

    @Override
    public void create() {
        VisUI.load();
        screenManager = new ScreenManager(this);
        batch = new SpriteBatch();
        gameClient = new GameClient(this);
        languageManager = new LanguageManager(getSettingsPreferences());
        applySettings(
            Gdx.app.getPreferences(SETTINGS_PREFS).getFloat("music_volume", 0.7f),
            Gdx.app.getPreferences(SETTINGS_PREFS).getFloat("sfx_volume", 0.85f),
            Gdx.app.getPreferences(SETTINGS_PREFS).getBoolean("fullscreen", false)
        );
        screenManager.set(new MainMenuScreen(this));
    }

    @Override
    public void onGameStateUpdated(GameState gameState) {
        latestGameState = gameState;
        Gdx.app.log("Network", "State updated: " + gameState.turnCount + ", phase=" + gameState.currentPhase);

        if (isHost && !lobbyOpened && gameState != null && gameState.players != null && !gameState.players.isEmpty()) {
            lobbyOpened = true; // ставим флаг ДО postRunnable
            Gdx.app.postRunnable(() -> openLobby());
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
        lobbyOpened = true;
        Gdx.app.postRunnable(() -> openLobby());
    }

    @Override
    public void onDisconnected() {
        Gdx.app.log("Network", "Disconnected from server!");
    }

    @Override
    public void onConnectionFailed(String reason) {}

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
        screenManager.set(new LobbyScreen(this));
    }

    public void openMenu() {
        screenManager.set(new MainMenuScreen(this));
    }
    public void openSettings() {
        screenManager.set(new SettingsScreen(this));
    }
    public void openGame() {
        screenManager.set(new GameScreen(this));
    }
    public void openGameLoading() {
        screenManager.set(new LoadingScreen(this, "Загрузка матча", () -> new GameScreen(this)));
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
        languageManager = new LanguageManager(getSettingsPreferences());

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

    public void startAsHost(String playerName) {
        startAsHost();
        gameClient.connectAndJoin("127.0.0.1", 54555, playerName);
    }

    public void connectAsGuest(String ip, int port, String playerName) {
        isHost = false;
        lobbyOpened = false;
        latestGameState = null;
        gameClient.connectAndJoin(ip, port, playerName);
    }

    public void startGameAsHost() {
        if (gameClient != null) {
            gameClient.sendStartGame(new StartGameRequest());
        }
    }

    public String getRoomCode() {
        if (gameServer != null) {
            return gameServer.getRoomCode();
        }
        return "UNKNOWN";
    }

    public com.badlogic.gdx.Preferences getSettingsPreferences() {
        return Gdx.app.getPreferences(SETTINGS_PREFS);
    }

    public LanguageManager getLanguageManager() {
        return languageManager;
    }

    public void applySettings(float musicVolume, float sfxVolume, boolean fullscreen) {
        com.badlogic.gdx.Preferences preferences = getSettingsPreferences();
        preferences.putFloat("music_volume", musicVolume);
        preferences.putFloat("sfx_volume", sfxVolume);
        preferences.putBoolean("fullscreen", fullscreen);
        preferences.flush();

        if (fullscreen) {
            com.badlogic.gdx.Graphics.DisplayMode displayMode = Gdx.graphics.getDisplayMode();
            Gdx.graphics.setFullscreenMode(displayMode);
        } else {
            Gdx.graphics.setWindowedMode(WINDOWED_WIDTH, WINDOWED_HEIGHT);
        }
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

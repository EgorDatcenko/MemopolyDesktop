package com.memopoly;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.kotcrab.vis.ui.VisUI;
import com.memopoly.Screens.*;
import com.memopoly.game.model.GameState;
import com.memopoly.network.GameClient;
import com.memopoly.network.GameServer;
import com.memopoly.network.NetworkListener;
import com.memopoly.network.packets.RollDiceResponse;
import com.memopoly.network.packets.ChatMessage;
import com.memopoly.network.packets.StartGameRequest;
import com.memopoly.utils.AppLog;
import com.memopoly.utils.LanguageManager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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
    private BitmapFont localizedUiFont;

    @Override
    public void create() {
        VisUI.load();
        screenManager = new ScreenManager(this);
        batch = new SpriteBatch();
        gameClient = new GameClient(this);
        languageManager = new LanguageManager(getSettingsPreferences());
        applyLocalizedFonts();
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
        AppLog.info("Network", "State updated: " + gameState.turnCount + ", phase=" + gameState.currentPhase);

        if (isHost && !lobbyOpened && gameState != null && gameState.players != null && !gameState.players.isEmpty()) {
            lobbyOpened = true; // ставим флаг ДО postRunnable
            Gdx.app.postRunnable(() -> openLobby());
        }
    }

    @Override
    public void onDiceRolled(RollDiceResponse response) {
        AppLog.info("Network", response.playerId + " rolled " + response.total);
    }

    @Override
    public void onConnected() {
        AppLog.info("Network", "Подключено успешно!");
        AppLog.info("Network", "Connected to server!");
    }

    @Override
    public void onJoinedRoom() {
        lobbyOpened = true;
        Gdx.app.postRunnable(() -> openLobby());
    }

    @Override
    public void onDisconnected() {
        AppLog.info("Network", "Disconnected from server!");
    }

    @Override
    public void onConnectionFailed(String reason) {}

    @Override
    public void onActionRejected(String actionType, String reasonCode, String reason) {
        AppLog.warn("Network", "Action rejected: " + actionType + " | " + reasonCode + " | " + reason);
    }

    @Override
    public void onChatMessage(ChatMessage message) {
        if (message == null) {
            return;
        }
        chatMessages.add(message);
        if (chatMessages.size() > 100) {
            chatMessages.remove(0);
        }
    }

    public List<ChatMessage> getChatMessages() {
        return Collections.unmodifiableList(chatMessages);
    }

    public void sendChatMessage(String text) {
        if (gameClient != null) {
            gameClient.sendChatMessage(text);
        }
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
        applyLocalizedFonts();
        screenManager.set(new LobbyScreen(this));
    }

    public void openMenu() {
        applyLocalizedFonts();
        screenManager.set(new MainMenuScreen(this));
    }
    public void openSettings() {
        applyLocalizedFonts();
        screenManager.set(new SettingsScreen(this));
    }
    public void openGame() {
        applyLocalizedFonts();
        screenManager.set(new GameScreen(this));
    }
    public void openGameLoading() {
        applyLocalizedFonts();
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
        chatMessages.clear();
        isHost = false;
        lobbyOpened = false;
        openMenu();
    }

    public void startAsHost() {
        isHost = true;
        lobbyOpened = false;
        latestGameState = null;
        chatMessages.clear();
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
        chatMessages.clear();
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

    private void applyLocalizedFonts() {
        String fontFolderPath = languageManager.getLanguage() == LanguageManager.Language.RU ? "fonts_ru" : "fonts_en";
        BitmapFont newFont = tryLoadBitmapFont(fontFolderPath);
        if (newFont == null) {
            newFont = tryGenerateFontFromTtf(fontFolderPath);
        }
        if (newFont == null) {
            Gdx.app.log("Fonts", "No font in " + fontFolderPath + " (.fnt/.ttf/.otf). Keep default VisUI font.");
            return;
        }
        if (localizedUiFont != null) {
            localizedUiFont.dispose();
        }
        localizedUiFont = newFont;

        Label.LabelStyle labelStyle = VisUI.getSkin().get(Label.LabelStyle.class);
        labelStyle.font = localizedUiFont;
        TextButton.TextButtonStyle textButtonStyle = VisUI.getSkin().get(TextButton.TextButtonStyle.class);
        textButtonStyle.font = localizedUiFont;
        TextField.TextFieldStyle textFieldStyle = VisUI.getSkin().get(TextField.TextFieldStyle.class);
        textFieldStyle.font = localizedUiFont;
        CheckBox.CheckBoxStyle checkBoxStyle = VisUI.getSkin().get(CheckBox.CheckBoxStyle.class);
        checkBoxStyle.font = localizedUiFont;
    }

    private BitmapFont tryLoadBitmapFont(String folderPath) {
        FileHandle folder = Gdx.files.internal(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            return null;
        }
        for (FileHandle file : folder.list()) {
            if ("fnt".equalsIgnoreCase(file.extension())) {
                return new BitmapFont(file, false);
            }
        }
        return null;
    }

    private BitmapFont tryGenerateFontFromTtf(String folderPath) {
        FileHandle folder = Gdx.files.internal(folderPath);
        if (!folder.exists() || !folder.isDirectory()) {
            return null;
        }
        for (FileHandle file : folder.list()) {
            String ext = file.extension().toLowerCase();
            if (!"ttf".equals(ext) && !"otf".equals(ext)) {
                continue;
            }
            FreeTypeFontGenerator generator = new FreeTypeFontGenerator(file);
            FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
            param.size = 30;
            param.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
                + "абвгдеёжзийклмнопрстуфхцчшщъыьэюя№";
            BitmapFont font = generator.generateFont(param);
            generator.dispose();
            return font;
        }
        return null;
    }

    @Override
    public void dispose() {
        batch.dispose();
        if (localizedUiFont != null) {
            localizedUiFont.dispose();
        }
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

package com.memopoly;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.ObjectMap;
import com.kotcrab.vis.ui.VisUI;
import com.memopoly.Screens.*;
import com.memopoly.game.model.GameState;
import com.memopoly.network.GameClient;
import com.memopoly.network.GameServer;
import com.memopoly.network.NetworkListener;
import com.memopoly.network.packets.RollDiceResponse;
import com.memopoly.network.packets.StartGameRequest;
import com.memopoly.utils.AppLog;
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

    private void applyLocalizedFonts() {
        String fontPath = languageManager.getLanguage() == LanguageManager.Language.RU
            ? "fonts_ru/PressStart2P-Regular.ttf"
            : "fonts_EN/Jersey25-Regular.ttf";
        BitmapFont newFont = tryLoadBitmapFont(fontPath);
        if (newFont == null) {
            newFont = tryGenerateFontFromTtf(fontPath);
        }
        if (newFont == null) {
            AppLog.info("Fonts", "No font at " + fontPath + " (.fnt/.ttf/.otf). Keep default VisUI font.");
            return;
        }
        if (localizedUiFont != null) {
            localizedUiFont.dispose();
        }
        localizedUiFont = newFont;
        applyFontToVisUiSkin(localizedUiFont);
    }

    private void applyFontToVisUiSkin(BitmapFont font) {
        Skin skin = VisUI.getSkin();
        skin.add("default-font", font, BitmapFont.class);
        applyFontToStyles(skin.getAll(Label.LabelStyle.class), font);
        applyFontToStyles(skin.getAll(TextButton.TextButtonStyle.class), font);
        applyFontToStyles(skin.getAll(TextField.TextFieldStyle.class), font);
        applyFontToStyles(skin.getAll(CheckBox.CheckBoxStyle.class), font);
    }

    private void applyFontToStyles(ObjectMap<String, ?> styles, BitmapFont font) {
        if (styles == null) {
            return;
        }
        for (ObjectMap.Entry<String, ?> entry : styles.entries()) {
            Object style = entry.value;
            if (style instanceof Label.LabelStyle) {
                ((Label.LabelStyle) style).font = font;
            } else if (style instanceof TextButton.TextButtonStyle) {
                ((TextButton.TextButtonStyle) style).font = font;
            } else if (style instanceof TextField.TextFieldStyle) {
                ((TextField.TextFieldStyle) style).font = font;
            } else if (style instanceof CheckBox.CheckBoxStyle) {
                ((CheckBox.CheckBoxStyle) style).font = font;
            }
        }
    }

    private void applyPixelFontFiltering(BitmapFont font) {
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        font.setUseIntegerPositions(true);
        font.getData().setScale(0.7f);
    }

    private BitmapFont tryLoadBitmapFont(String fontPath) {
        FileHandle file = Gdx.files.internal(fontPath);
        if (!file.exists() || !"fnt".equalsIgnoreCase(file.extension())) {
            return null;
        }
        try {
            BitmapFont font = new BitmapFont(file, false);
            applyPixelFontFiltering(font);
            return font;
        } catch (Throwable throwable) {
            Gdx.app.error("Fonts", "Failed to load bitmap font " + fontPath + ". Keep current/default VisUI font.", throwable);
            return null;
        }
    }

    private BitmapFont tryGenerateFontFromTtf(String fontPath) {
        FileHandle file = Gdx.files.internal(fontPath);
        if (!file.exists()) {
            return null;
        }
        String ext = file.extension().toLowerCase();
        if (!"ttf".equals(ext) && !"otf".equals(ext)) {
            return null;
        }

        FreeTypeFontGenerator generator = null;
        try {
            generator = new FreeTypeFontGenerator(file);
            FreeTypeFontGenerator.FreeTypeFontParameter param = new FreeTypeFontGenerator.FreeTypeFontParameter();
            param.size = 20;
            param.minFilter = Texture.TextureFilter.Nearest;
            param.magFilter = Texture.TextureFilter.Nearest;
            param.mono = true;
            param.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
                + "абвгдеёжзийклмнопрстуфхцчшщъыьэюя№";
            BitmapFont font = generator.generateFont(param);
            applyPixelFontFiltering(font);
            return font;
        } catch (Throwable throwable) {
            Gdx.app.error("Fonts", "Failed to generate FreeType font " + fontPath + ". Keep current/default VisUI font.", throwable);
            return null;
        } finally {
            if (generator != null) {
                generator.dispose();
            }
        }
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

package com.memopoly;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.utils.ObjectMap;
import com.kotcrab.vis.ui.VisUI;
import com.memopoly.Screens.*;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.network.GameClient;
import com.memopoly.network.GameServer;
import com.memopoly.network.NetworkListener;
import com.memopoly.network.packets.RollDiceResponse;
import com.memopoly.network.packets.ChatMessage;
import com.memopoly.steam.SteamAchievementsManager;
import com.memopoly.steam.SteamLobbyManager;
import com.memopoly.steam.SteamManager;
import com.memopoly.utils.AppLog;
import com.memopoly.utils.LanguageManager;
import com.memopoly.utils.UiFonts;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Главный класс игры: создаёт GameClient/GameServer, управляет переключением
 * экранов через ScreenManager, хранит последнее состояние GameState и
 * обрабатывает сетевые события.
 */
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
    private final List<ChatMessage> chatMessages = new ArrayList<>();
    private final java.util.Set<String> processedAchievements = new java.util.HashSet<>();
    private volatile boolean connecting = false;

    @Override
    public void create() {
        VisUI.load();
        patchVisUiSkin();
        screenManager = new ScreenManager(this);
        batch = new SpriteBatch();
        gameClient = new GameClient(this);
        languageManager = new LanguageManager(getSettingsPreferences());
        applyLocalizedFonts();
        applySettings(
                Gdx.app.getPreferences(SETTINGS_PREFS).getFloat("music_volume", 0.7f),
                Gdx.app.getPreferences(SETTINGS_PREFS).getFloat("sfx_volume", 0.85f),
                Gdx.app.getPreferences(SETTINGS_PREFS).getBoolean("fullscreen", false));

        // Автоконнект при принятии инвайта через Steam
        SteamLobbyManager.setJoinCallback((ip, port) -> {
            Gdx.app.postRunnable(() -> {
                connectAsGuest(ip, port);
            });
        });

        screenManager.set(new MainMenuScreen(this));
    }

    @Override
    public void onGameStateUpdated(GameState gameState) {
        latestGameState = gameState;
        AppLog.info("Network", "State updated: " + gameState.turnCount + ", phase=" + gameState.currentPhase);

        if (isHost && !lobbyOpened && gameState != null && gameState.players != null && !gameState.players.isEmpty()) {
            lobbyOpened = true;
            Gdx.app.postRunnable(() -> openLobby());
        }

        if (gameState != null && gameState.achievementEvents != null) {
            int localId = getClient().getLocalPlayerId();
            for (String event : gameState.achievementEvents) {
                if (processedAchievements.contains(event))
                    continue;
                String[] parts = event.split("\\|");
                if (parts.length == 2 && Integer.parseInt(parts[0]) == localId) {
                    processedAchievements.add(event);
                    SteamAchievementsManager.unlock(parts[1]);
                }
            }
        }

        // TYCOON и UNSTOPPABLE при победе
        if (gameState != null && gameState.currentPhase == GameState.GamePhase.GAME_OVER) {
            Player winner = gameState.getWinner();
            int localId = getClient().getLocalPlayerId();
            if (winner != null && winner.id == localId) {
                if (winner.money >= 5000) {
                    SteamAchievementsManager.unlock("TYCOON");
                }
                // UNSTOPPABLE через Preferences
                com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("memopoly-stats");
                int winStreak = prefs.getInteger("win_streak", 0) + 1;
                prefs.putInteger("win_streak", winStreak);
                prefs.flush();
                if (winStreak >= 3) {
                    SteamAchievementsManager.unlock("UNSTOPPABLE");
                }
            } else if (winner != null && winner.id != localId) {
                // Сброс стрика при поражении
                com.badlogic.gdx.Preferences prefs = Gdx.app.getPreferences("memopoly-stats");
                prefs.putInteger("win_streak", 0);
                prefs.flush();
            }
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
        connecting = false;
        lobbyOpened = true;
        Gdx.app.postRunnable(() -> openLobby());
    }

    @Override
    public void onDisconnected() {
        connecting = false;
        AppLog.info("Network", "Disconnected from server!");
    }

    @Override
    public void onConnectionFailed(String reason) {
        connecting = false;
    }

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
        SteamManager.update();
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
        connecting = false;
        lobbyOpened = false;

        // Очищаем Steam-лобби
        SteamLobbyManager.clearLobby();

        openMenu();
    }

    public void startAsHost() {
        if (gameServer != null) {
            AppLog.warn("Server", "startAsHost проигнорирован: сервер уже запущен");
            return;
        }
        isHost = true;
        lobbyOpened = false;
        latestGameState = null;
        chatMessages.clear();
        gameServer = new GameServer();

        // Создаём Steam-лобби и записываем IP/порт
        if (SteamManager.isAvailable()) {
            SteamLobbyManager.createLobby(gameServer.getHostIP(), 54555);
        }

        String playerName = resolvePlayerName();
        gameClient.connectAndJoin("127.0.0.1", 54555, playerName);
    }

    /** Имя из Steam с фолбэком Player_XXXX, если Steam недоступен. */
    private String resolvePlayerName() {
        String steamName = SteamManager.getPersonaName();
        if (steamName != null && !steamName.isBlank()) {
            return steamName;
        }
        return "Player_" + (1000 + new java.util.Random().nextInt(9000));
    }

    public String getRoomCode() {
        if (gameServer != null) {
            return gameServer.getRoomCode();
        }
        if (latestGameState != null && latestGameState.roomCode != null) {
            return latestGameState.roomCode;
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

        boolean nowFullscreen = Gdx.graphics.isFullscreen();
        if (fullscreen && !nowFullscreen) {
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        } else if (!fullscreen && nowFullscreen) {
            Gdx.graphics.setWindowedMode(WINDOWED_WIDTH, WINDOWED_HEIGHT);
        }
    }

    private void applyLocalizedFonts() {
        if (localizedUiFont != null) {
            return;
        }
        String fontPath = "fonts_ru/Rubik-Bold.ttf";
        String source = "ttf";
        BitmapFont newFont = tryLoadBitmapFont(fontPath);
        if (newFont != null) {
            source = "fnt";
        } else {
            newFont = tryGenerateFontFromTtf(fontPath);
        }
        if (newFont == null) {
            AppLog.info("Fonts", "No font at " + fontPath + ". Keep default VisUI font.");
            return;
        }
        localizedUiFont = normalizeFont(newFont, 45f); // приводим ЛЮБОЙ источник к размеру, под который сверстан UI
        AppLog.info("Fonts", "Localized font: source=" + source + ", lineHeight=" + localizedUiFont.getLineHeight());
        applyFontToVisUiSkin(localizedUiFont);
    }

    private BitmapFont normalizeFont(BitmapFont font, float targetLineHeight) {
        float k = targetLineHeight / font.getLineHeight();
        font.getData().setScale(font.getScaleX() * k, font.getScaleY() * k);
        font.setUseIntegerPositions(false);
        font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return font;
    }

    private void patchVisUiSkin() {
        Skin skin = VisUI.getSkin();
        Color darkText = Color.valueOf("000A3E");

        ObjectMap<String, Label.LabelStyle> labelStyles = skin.getAll(Label.LabelStyle.class);
        if (labelStyles != null) {
            for (Label.LabelStyle s : labelStyles.values()) {
                s.fontColor = Color.WHITE;
            }
        }

        ObjectMap<String, TextButton.TextButtonStyle> buttonStyles = skin.getAll(TextButton.TextButtonStyle.class);
        if (buttonStyles != null) {
            for (TextButton.TextButtonStyle s : buttonStyles.values()) {
                s.fontColor = darkText;
                s.downFontColor = darkText;
                s.overFontColor = darkText;
                s.checkedFontColor = darkText;
                s.checkedOverFontColor = darkText;
            }
        }

        ObjectMap<String, TextField.TextFieldStyle> fieldStyles = skin.getAll(TextField.TextFieldStyle.class);
        if (fieldStyles != null) {
            for (TextField.TextFieldStyle s : fieldStyles.values()) {
                s.fontColor = darkText;
                s.messageFontColor = new Color(0f, 10 / 255f, 62 / 255f, 0.55f);
            }
        }
    }

    private void applyFontToVisUiSkin(BitmapFont font) {
        Skin skin = VisUI.getSkin();
        skin.add("default-font", font, BitmapFont.class);

        Color textColor = Color.valueOf("000A3E");

        applyFontToStyles(skin.getAll(Label.LabelStyle.class), font, textColor);
        applyFontToStyles(skin.getAll(TextButton.TextButtonStyle.class), font, textColor);
        applyFontToStyles(skin.getAll(TextField.TextFieldStyle.class), font, textColor);
        applyFontToStyles(skin.getAll(CheckBox.CheckBoxStyle.class), font, textColor);
        applyFontToStyles(skin.getAll(Window.WindowStyle.class), font, textColor);
    }

    private void applyFontToStyles(ObjectMap<String, ?> styles, BitmapFont font, Color textColor) {
        if (styles == null) {
            return;
        }
        for (ObjectMap.Entry<String, ?> entry : styles.entries()) {
            Object style = entry.value;
            if (style instanceof Label.LabelStyle) {
                Label.LabelStyle s = (Label.LabelStyle) style;
                s.font = font;
                s.fontColor = Color.WHITE;
            } else if (style instanceof TextButton.TextButtonStyle) {
                TextButton.TextButtonStyle s = (TextButton.TextButtonStyle) style;
                s.font = font;
                s.fontColor = textColor;
            } else if (style instanceof TextField.TextFieldStyle) {
                TextField.TextFieldStyle s = (TextField.TextFieldStyle) style;
                s.font = font;
                s.fontColor = textColor;
                s.messageFont = font;
                s.messageFontColor = new Color(0f, 10 / 255f, 62 / 255f, 0.55f);
            } else if (style instanceof CheckBox.CheckBoxStyle) {
                CheckBox.CheckBoxStyle s = (CheckBox.CheckBoxStyle) style;
                s.font = font;
                s.fontColor = textColor;
            } else if (style instanceof Window.WindowStyle) {
                Window.WindowStyle s = (Window.WindowStyle) style;
                s.titleFont = font;
                s.titleFontColor = textColor;
            }
        }
    }

    private BitmapFont tryLoadBitmapFont(String fontPath) {
        FileHandle file = Gdx.files.internal(fontPath);
        if (!file.exists() || !"fnt".equalsIgnoreCase(file.extension())) {
            return null;
        }
        try {
            BitmapFont font = new BitmapFont(file, false);
            font.getRegion().getTexture().setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
            font.setUseIntegerPositions(false);
            return font;
        } catch (Throwable throwable) {
            Gdx.app.error("Fonts", "Failed to load bitmap font " + fontPath + ". Keep current/default VisUI font.",
                    throwable);
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
            param.size = 38;
            param.minFilter = Texture.TextureFilter.Linear;
            param.magFilter = Texture.TextureFilter.Linear;
            param.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                    + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯ"
                    + "абвгдеёжзийклмнопрстуфхцчшщъыьэюя№";
            BitmapFont font = generator.generateFont(param);
            return font;
        } catch (Throwable throwable) {
            Gdx.app.error("Fonts",
                    "Failed to generate FreeType font " + fontPath + ". Keep current/default VisUI font.", throwable);
            return null;
        } finally {
            if (generator != null) {
                generator.dispose();
            }
        }
    }

    public void connectAsGuest(String ip, int port) {
        if (connecting) {
            AppLog.warn("Network", "connectAsGuest проигнорирован: уже идёт подключение");
            return;
        }
        connecting = true;
        isHost = false;
        lobbyOpened = false;
        latestGameState = null;
        chatMessages.clear();
        String playerName = resolvePlayerName();
        Thread t = new Thread(() -> {
            try {
                gameClient.connectAndJoin(ip, port, playerName);
            } catch (Exception e) {
                AppLog.warn("Network", "connectAndJoin error: " + e.getMessage());
                connecting = false;
            }
        }, "memopoly-connect");
        t.setDaemon(true);
        t.start();
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
        SteamManager.shutdown();
        super.dispose();
    }
}

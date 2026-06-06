package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.CheckBox;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisSlider;
import com.memopoly.Memopoly;
import com.memopoly.utils.TexturePathResolver;

public class SettingsScreen extends BaseScreen {
    private static final float COMMON_BUTTON_HEIGHT = 48f;
    private static final float SETTINGS_WINDOW_SCALE = 0.5f;
    private static final Color BACKGROUND_COLOR = new Color(0.10f, 0.10f, 0.17f, 1f);
    private static final Color PANEL_COLOR = new Color(0.18f, 0.16f, 0.27f, 0.98f);
    private static final Color PANEL_SHADOW = new Color(0.06f, 0.05f, 0.10f, 0.95f);
    private static final Color TITLE_COLOR = new Color(1.00f, 0.83f, 0.25f, 1f);
    private static final String BACKGROUND_TEXTURE_PATH = "background.png";
    private static final String BACK_BUTTON_TEXTURE_PATH = "back_btn.png";
    private static final String APPLY_BUTTON_TEXTURE_PATH = "apply_btn.png";
    private static final String LOBBY_WINDOW_TEXTURE_PATH = "lobby_window.png";

    private final Stage stage;
    private final Texture backgroundTexture;
    private final Texture backButtonTexture;
    private final Texture applyButtonTexture;
    private final Texture lobbyWindowTexture;
    private final Preferences preferences;

    private final VisSlider musicSlider;
    private final VisSlider sfxSlider;
    private final CheckBox fullscreenCheckBox;
    private final VisLabel musicValueLabel;
    private final VisLabel sfxValueLabel;
    private final VisLabel statusLabel;
    private final com.memopoly.utils.LanguageManager.Language language;

    public SettingsScreen(Memopoly game) {
        super(game);
        stage = new Stage(new ScreenViewport());
        language = game.getLanguageManager().getLanguage();
        backgroundTexture = loadTexture(BACKGROUND_TEXTURE_PATH);
        backButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(BACK_BUTTON_TEXTURE_PATH, language));
        applyButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(APPLY_BUTTON_TEXTURE_PATH, language));
        lobbyWindowTexture = loadTexture(LOBBY_WINDOW_TEXTURE_PATH);
        preferences = game.getSettingsPreferences();
        musicSlider = new VisSlider(0f, 1f, 0.01f, false);
        sfxSlider = new VisSlider(0f, 1f, 0.01f, false);
        fullscreenCheckBox = new CheckBox(" " + t("fullscreen"), VisUI.getSkin());
        musicValueLabel = new VisLabel();
        sfxValueLabel = new VisLabel();
        statusLabel = new VisLabel(t("status_hint"));

        Gdx.input.setInputProcessor(stage);
        createUi();
        loadCurrentValues();
    }

    private void createUi() {
        Table root = new Table();
        root.setFillParent(true);
        root.pad(28f);

        Table panel = new Table();
        panel.setBackground(window(lobbyWindowTexture));
        panel.pad(35f, 35f, 26f, 30f);
        panel.top().left();
        panel.defaults().left().padBottom(16f);

        VisLabel titleLabel = new VisLabel(t("settings"));
        titleLabel.setFontScale(1.8f);
        titleLabel.setColor(TITLE_COLOR);

        VisLabel musicLabel = new VisLabel(t("music"));
        VisLabel sfxLabel = new VisLabel(t("effects"));
        musicValueLabel.setColor(Color.WHITE);
        sfxValueLabel.setColor(Color.WHITE);
        statusLabel.setColor(new Color(0.94f, 0.91f, 0.76f, 1f));

        musicSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
        updateValueLabels();
            }
        });
        sfxSlider.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
        updateValueLabels();
            }
        });

        ImageButton applyButton = createImageButton(applyButtonTexture);
        applyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                saveSettings();
            }
        });

        ImageButton backButton = createImageButton(backButtonTexture);
        backButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.openMenu();
            }
        });

        panel.add(titleLabel).row();

        Table musicRow = new Table();
        musicRow.add(musicLabel).width(180f).left().padRight(14f);
        musicRow.add(musicSlider).width(320f).padRight(14f);
        musicRow.add(musicValueLabel).width(70f).left();
        panel.add(musicRow).row();

        Table sfxRow = new Table();
        sfxRow.add(sfxLabel).width(180f).left().padRight(14f);
        sfxRow.add(sfxSlider).width(320f).padRight(14f);
        sfxRow.add(sfxValueLabel).width(70f).left();
        panel.add(sfxRow).row();

        panel.add(fullscreenCheckBox).left().row();
        panel.add(statusLabel).width(520f).left().padTop(4f).row();

        Table buttonRow = new Table();
        buttonRow.add(applyButton).width(170f).height(COMMON_BUTTON_HEIGHT).padRight(14f);
        buttonRow.add(backButton).width(170f).height(COMMON_BUTTON_HEIGHT);
        panel.add(buttonRow).left().padTop(8f);

        root.add(panel).size(lobbyWindowTexture.getWidth() * SETTINGS_WINDOW_SCALE, lobbyWindowTexture.getHeight() * SETTINGS_WINDOW_SCALE).expand().center();
        stage.addActor(root);
    }

    private void loadCurrentValues() {
        musicSlider.setValue(preferences.getFloat("music_volume", 0.7f));
        sfxSlider.setValue(preferences.getFloat("sfx_volume", 0.85f));
        fullscreenCheckBox.setChecked(preferences.getBoolean("fullscreen", false));
        updateValueLabels();
    }

    private void updateValueLabels() {
        musicValueLabel.setText(Math.round(musicSlider.getValue() * 100f) + "%");
        sfxValueLabel.setText(Math.round(sfxSlider.getValue() * 100f) + "%");
    }

    private void saveSettings() {
        float musicVolume = musicSlider.getValue();
        float sfxVolume = sfxSlider.getValue();
        boolean fullscreen = fullscreenCheckBox.isChecked();

        preferences.putFloat("music_volume", musicVolume);
        preferences.putFloat("sfx_volume", sfxVolume);
        preferences.putBoolean("fullscreen", fullscreen);
        preferences.flush();

        game.applySettings(musicVolume, sfxVolume, fullscreen);
        statusLabel.setText(t("saved"));
        game.openSettings();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().draw(backgroundTexture, 0f, 0f, stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        stage.getBatch().end();
        stage.act(delta);
        stage.draw();
    }

    private Drawable window(Texture texture) {
        return new TextureRegionDrawable(new TextureRegion(texture));
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private Drawable panel(Color color) {
        return VisUI.getSkin().newDrawable("white", color);
    }

    private ImageButton createImageButton(Texture texture) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = drawable;
        style.imageOver = drawable.tint(new Color(1f, 1f, 1f, 0.96f));
        style.imageDown = drawable.tint(new Color(0.86f, 0.86f, 0.86f, 1f));
        Drawable transparent = panel(new Color(1f, 1f, 1f, 0f));
        style.up = transparent;
        style.over = transparent;
        style.down = transparent;
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.stretch);
        button.getImageCell().grow();
        return button;
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        backButtonTexture.dispose();
        applyButtonTexture.dispose();
        lobbyWindowTexture.dispose();
        stage.dispose();
    }

    private String t(String key) {
        boolean ru = language == Language.RU;
        return switch (key) {
            case "fullscreen" -> ru ? "Полноэкранный режим" : "Fullscreen mode";
            case "status_hint" -> ru ? "Изменения сохраняются после нажатия \"Применить\"" : "Changes are saved after pressing \"Apply\"";
            case "settings" -> ru ? "Настройки" : "Settings";
            case "music" -> ru ? "Музыка" : "Music";
            case "effects" -> ru ? "Эффекты" : "Effects";
            case "saved" -> ru ? "Настройки сохранены" : "Settings saved";
            default -> key;
        };
    }
}

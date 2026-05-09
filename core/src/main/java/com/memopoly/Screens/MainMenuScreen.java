package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.memopoly.utils.LanguageManager;
import com.memopoly.Memopoly;
import com.memopoly.utils.ClipboardUtils;
import com.memopoly.utils.RoomCodeGenerator;
import com.memopoly.utils.TexturePathResolver;
import com.memopoly.utils.LanguageManager.Language;

public class MainMenuScreen extends BaseScreen {
    private static final Color BACKGROUND_COLOR = new Color(0.10f, 0.10f, 0.17f, 1f);
    private static final String BACKGROUND_TEXTURE_PATH = "background.png";
    private static final String CREATE_BUTTON_TEXTURE_PATH = "create_game_btn.png";
    private static final String CONNECT_BUTTON_TEXTURE_PATH = "connect_btn.png";
    private static final String SETTINGS_BUTTON_TEXTURE_PATH = "settings_btn.png";
    private static final String EXIT_BUTTON_TEXTURE_PATH = "exit_btn.png";
    private static final String CREATE_DIALOG_BUTTON_TEXTURE_PATH = "create_btn.png";
    private static final String CONNECT_DIALOG_BUTTON_TEXTURE_PATH = "connect_btn_for_window.png";
    private static final String CANCEL_BUTTON_TEXTURE_PATH = "cancel_btn.png";
    private static final String COPY_CODE_BUTTON_TEXTURE_PATH = "copy_the_code_btn.png";
    private static final String CHANGE_LANGUAGE_BUTTON_TEXTURE_PATH = "change_language_btn.png";

    private final Stage stage;
    private final Texture backgroundTexture;
    private final Texture createButtonTexture;
    private final Texture connectButtonTexture;
    private final Texture settingsButtonTexture;
    private final Texture exitButtonTexture;
    private final Texture createDialogButtonTexture;
    private final Texture connectDialogButtonTexture;
    private final Texture cancelButtonTexture;
    private final Texture copyCodeButtonTexture;
    private final Texture changeLanguageButtonTexture;
    private boolean roomCodeShown;

    public MainMenuScreen(Memopoly game) {
        super(game);
        stage = new Stage(new ScreenViewport());
        Language language = game.getLanguageManager().getLanguage();
        backgroundTexture = loadTexture(BACKGROUND_TEXTURE_PATH);
        createButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(CREATE_BUTTON_TEXTURE_PATH, language));
        connectButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(CONNECT_BUTTON_TEXTURE_PATH, language));
        settingsButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(SETTINGS_BUTTON_TEXTURE_PATH, language));
        exitButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(EXIT_BUTTON_TEXTURE_PATH, language));
        createDialogButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CREATE_DIALOG_BUTTON_TEXTURE_PATH, language));
        connectDialogButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CONNECT_DIALOG_BUTTON_TEXTURE_PATH, language));
        cancelButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CANCEL_BUTTON_TEXTURE_PATH, language));
        copyCodeButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(COPY_CODE_BUTTON_TEXTURE_PATH, language));
        changeLanguageButtonTexture = loadTextureIfExists(CHANGE_LANGUAGE_BUTTON_TEXTURE_PATH);
        Gdx.input.setInputProcessor(stage);

        createUI();
    }

    private void createUI() {
        Table root = new Table();
        root.setFillParent(true);

        ImageButton createButton = createImageButton(createButtonTexture);
        createButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showStartGameDialog();
            }
        });

        ImageButton joinButton = createImageButton(connectButtonTexture);
        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showConnectDialog();
            }
        });

        ImageButton settingsButton = createImageButton(settingsButtonTexture);
        settingsButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.openSettings();
            }
        });

        ImageButton exitButton = createImageButton(exitButtonTexture);
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        Table buttonStack = new Table();
        buttonStack.defaults().left().padBottom(18f);
        buttonStack.add(createButton).size(270f, 80f).row();
        buttonStack.add(joinButton).size(270f, 80f).row();
        buttonStack.add(settingsButton).size(270f, 80f).row();
        buttonStack.add(exitButton).size(270f, 80f);

        root.add(buttonStack).expand().center().padTop(120f).row();
        Actor languageButton = createLanguageButton();
        root.add(languageButton).expandX().left().bottom().padLeft(20f).padBottom(20f);
        stage.addActor(root);
    }

    private void showStartGameDialog() {
        VisTextField nameField = new VisTextField();
        nameField.setMessageText("Имя хоста");

        Dialog dialog = new Dialog("Создание игры", VisUI.getSkin()) {
            @Override
            protected void result(Object object) {
                if (!Boolean.TRUE.equals(object)) {
                    return;
                }

                String playerName = nameField.getText().trim();
                if (playerName.isEmpty()) {
                    return;
                }

                try {
                    game.startAsHost(playerName);
                } catch (RuntimeException e) {
                    showErrorDialog("Не удалось создать комнату", e.getMessage());
                }
            }
        };

        dialog.getContentTable().add(new VisLabel("Введите имя для комнаты")).pad(10);
        dialog.row();
        dialog.getContentTable().add(nameField).width(240).pad(10);

        dialog.getButtonTable().clearChildren();
        dialog.getButtonTable().defaults().pad(10f);
        ImageButton createActionButton = createImageButton(createDialogButtonTexture);
        createActionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String playerName = nameField.getText().trim();
                if (playerName.isEmpty()) {
                    return;
                }
                try {
                    game.startAsHost(playerName);
                    dialog.hide();
                } catch (RuntimeException e) {
                    showErrorDialog("Не удалось создать комнату", e.getMessage());
                }
            }
        });
        ImageButton cancelActionButton = createImageButton(cancelButtonTexture);
        cancelActionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(createActionButton).size(176f, 58f);
        dialog.getButtonTable().add(cancelActionButton).size(176f, 58f);
        dialog.show(stage);
    }

    private void showConnectDialog() {
        VisTextField nameField = new VisTextField();
        nameField.setMessageText("Имя игрока");

        VisTextField codeField = new VisTextField();
        codeField.setMessageText("Код комнаты");

        VisLabel statusLabel = new VisLabel("");

        Dialog dialog = new Dialog("Подключение...", VisUI.getSkin()) {
            @Override
            protected void result(Object object) {
                if (!Boolean.TRUE.equals(object)) {
                    return;
                }

                String playerName = nameField.getText().trim();
                String roomCode = codeField.getText().trim();

                if (playerName.isEmpty()) {
                    statusLabel.setText("Введите имя игрока");
                    return;
                }

                String ip = RoomCodeGenerator.decodeRoomCode(roomCode);
                if (ip == null || ip.isEmpty()) {
                    statusLabel.setText("Неверный код комнаты");
                    return;
                }

                statusLabel.setText("Подключение...");
                System.out.println("Расшифрованный IP: " + ip + ", имя=" + playerName);

                game.connectAsGuest(ip, 54555, playerName);
            }
        };

        dialog.getContentTable().add(statusLabel).pad(10);
        dialog.row();
        dialog.getContentTable().add(nameField).width(300).pad(10);
        dialog.row();
        dialog.getContentTable().add(codeField).width(300).pad(10);

        dialog.getButtonTable().clearChildren();
        dialog.getButtonTable().defaults().pad(10f);
        ImageButton connectActionButton = createImageButton(connectDialogButtonTexture);
        connectActionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String playerName = nameField.getText().trim();
                String roomCode = codeField.getText().trim();

                if (playerName.isEmpty()) {
                    statusLabel.setText("Введите имя игрока");
                    return;
                }

                String ip = RoomCodeGenerator.decodeRoomCode(roomCode);
                if (ip == null || ip.isEmpty()) {
                    statusLabel.setText("Неверный код комнаты");
                    return;
                }

                statusLabel.setText("Подключение...");
                game.connectAsGuest(ip, 54555, playerName);
                dialog.hide();
            }
        });
        ImageButton cancelActionButton = createImageButton(cancelButtonTexture);
        cancelActionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(connectActionButton).size(196f, 58f);
        dialog.getButtonTable().add(cancelActionButton).size(176f, 58f);

        dialog.show(stage);
    }

    private void showErrorDialog(String title, String message) {
        Dialog dialog = new Dialog(title, VisUI.getSkin());
        dialog.text(message == null || message.isBlank() ? "Произошла неизвестная ошибка." : message);
        dialog.button("ОК", true);
        dialog.show(stage);
    }

    private void showInfoDialog(String title, String message) {
        Dialog dialog = new Dialog(title, VisUI.getSkin());
        dialog.text(message);
        dialog.button("ОК", true);
        dialog.show(stage);
    }

    private void showRoomCodeDialog(String roomCode) {
        Dialog dialog = new Dialog("Комната создана", VisUI.getSkin());

        VisLabel titleLabel = new VisLabel("Ваш код комнаты:");
        VisTextField codeField = new VisTextField(roomCode);
        codeField.setDisabled(true);
        codeField.selectAll();

        ImageButton copyButton = createImageButton(copyCodeButtonTexture);
        copyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ClipboardUtils.copyToClipboard(roomCode);
                showCopiedNotification();
            }
        });

        dialog.getContentTable().add(titleLabel).pad(10);
        dialog.row();
        dialog.getContentTable().add(codeField).width(220).pad(10);
        dialog.row();
        dialog.getContentTable().add(copyButton).size(220f, 60f).pad(10);
        dialog.getButtonTable().clearChildren();
        ImageButton cancelActionButton = createImageButton(cancelButtonTexture);
        cancelActionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(cancelActionButton).size(176f, 58f).pad(10f);
        dialog.show(stage);
    }

    private void showCopiedNotification() {
        Dialog notification = new Dialog("", VisUI.getSkin()) {
            @Override
            protected void result(Object object) {
                // Просто закрываем уведомление
            }
        };

        notification.getContentTable().add(new VisLabel("Код скопирован!")).pad(20);
        notification.button("ОК", true);
        notification.show(stage);

        Timer.schedule(new Timer.Task() {
            @Override
            public void run() {
                notification.hide();
            }
        }, 2f);
    }

    private Actor createLanguageButton() {
        if (changeLanguageButtonTexture != null) {
            ImageButton button = createImageButton(changeLanguageButtonTexture);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    showLanguageDialog();
                }
            });
            return button;
        }

        VisTextButton button = new VisTextButton("Language");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showLanguageDialog();
            }
        });
        return button;
    }

    private void showLanguageDialog() {
        Dialog dialog = new Dialog("Select language", VisUI.getSkin());

        VisTextButton english = new VisTextButton("English");
        VisTextButton russian = new VisTextButton("Русский");

        english.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getLanguageManager().setLanguage(LanguageManager.Language.EN);
                dialog.hide();
                game.openMenu();
            }
        });
        russian.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                game.getLanguageManager().setLanguage(LanguageManager.Language.RU);
                dialog.hide();
                game.openMenu();
            }
        });

        dialog.getContentTable().add("Select game language").pad(10).row();
        dialog.getButtonTable().add(english).width(160f).pad(8f);
        dialog.getButtonTable().add(russian).width(160f).pad(8f);
        dialog.show(stage);
    }

    private Texture loadTextureIfExists(String path) {
        if (!Gdx.files.internal(path).exists()) {
            return null;
        }
        return loadTexture(path);
    }

    @Override
    public void render(float delta) {
        if (!roomCodeShown && game.isHost() && game.getRoomCode() != null && !"UNKNOWN".equals(game.getRoomCode())) {
            roomCodeShown = true;
            showRoomCodeDialog(game.getRoomCode());
        }

        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().begin();
        stage.getBatch().draw(backgroundTexture, 0f, 0f, stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        stage.getBatch().end();
        stage.act(delta);
        stage.draw();
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private ImageButton createImageButton(Texture texture) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = drawable;
        style.imageOver = drawable.tint(new Color(1f, 1f, 1f, 0.96f));
        style.imageDown = drawable.tint(new Color(0.86f, 0.86f, 0.86f, 1f));
        Drawable transparent = VisUI.getSkin().newDrawable("white", new Color(1f, 1f, 1f, 0f));
        style.up = transparent;
        style.over = transparent;
        style.down = transparent;
        return new ImageButton(style);
    }

    @Override
    public void dispose() {
        backgroundTexture.dispose();
        createButtonTexture.dispose();
        connectButtonTexture.dispose();
        settingsButtonTexture.dispose();
        exitButtonTexture.dispose();
        createDialogButtonTexture.dispose();
        connectDialogButtonTexture.dispose();
        cancelButtonTexture.dispose();
        copyCodeButtonTexture.dispose();
        if (changeLanguageButtonTexture != null) {
            changeLanguageButtonTexture.dispose();
        }
        stage.dispose();
    }
}

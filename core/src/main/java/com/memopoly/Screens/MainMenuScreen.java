package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
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
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.badlogic.gdx.utils.Array;
import com.memopoly.utils.LanguageManager;
import com.memopoly.Memopoly;
import com.memopoly.modding.DeckRepository;
import com.memopoly.game.model.MemeDeck;
import com.memopoly.utils.ClipboardUtils;
import com.memopoly.utils.RoomCodeGenerator;
import com.memopoly.utils.TexturePathResolver;
import com.memopoly.utils.LanguageManager.Language;
import com.memopoly.utils.AppLog;

public class MainMenuScreen extends BaseScreen {
    private static final Color BACKGROUND_COLOR = new Color(0.10f, 0.10f, 0.17f, 1f);
    private static final String BACKGROUND_TEXTURE_PATH = "background.png";
    private static final String CREATE_BUTTON_TEXTURE_PATH = "create_game_btn.png";
    private static final String CONNECT_BUTTON_TEXTURE_PATH = "connect_btn.png";
    private static final String SETTINGS_BUTTON_TEXTURE_PATH = "settings_btn.png";
    private static final String EXIT_BUTTON_TEXTURE_PATH = "exit_btn.png";
    private static final String DECKS_BUTTON_TEXTURE_PATH = "decks.png";
    private static final String CREATE_DIALOG_BUTTON_TEXTURE_PATH = "create_btn.png";
    private static final String CONNECT_DIALOG_BUTTON_TEXTURE_PATH = "connect_btn_for_window.png";
    private static final String CANCEL_BUTTON_TEXTURE_PATH = "cancel_btn.png";
    private static final String COPY_CODE_BUTTON_TEXTURE_PATH = "copy_the_code_btn.png";
    private static final String CHANGE_LANGUAGE_BUTTON_TEXTURE_PATH = "change_language_btn.png";
    private static final String NOTIFICATION_WINDOW_TEXTURE_PATH = "notification_window.png";
    private static final String INPUT_TEXTURE_PATH = "input.png";
    private static final float NOTIFICATION_DIALOG_SCALE = 0.5f;
    private static final String CREATE_DECK_BUTTON_TEXTURE_PATH = "create_deck_btn.png";
    private static final String BACK_BUTTON_TEXTURE_PATH = "back_btn.png";
    private static final String ENGLISH_BUTTON_TEXTURE_PATH = "english.png";
    private static final String RUSSIAN_BUTTON_TEXTURE_PATH = "russian.png";

    private final Stage stage;
    private final Texture backgroundTexture;
    private final Texture createButtonTexture;
    private final Texture connectButtonTexture;
    private final Texture settingsButtonTexture;
    private final Texture exitButtonTexture;
    private final Texture decksButtonTexture;
    private final Texture createDialogButtonTexture;
    private final Texture connectDialogButtonTexture;
    private final Texture cancelButtonTexture;
    private final Texture copyCodeButtonTexture;
    private final Texture changeLanguageButtonTexture;
    private final Texture notificationWindowTexture;
    private final Texture inputTexture;
    private final Texture createDeckButtonTexture;
    private final Texture closeDialogButtonTexture;
    private final Texture englishButtonTexture;
    private final Texture russianButtonTexture;
    private boolean roomCodeShown;
    private final DeckRepository deckRepository = new DeckRepository();

    public MainMenuScreen(Memopoly game) {
        super(game);
        stage = new Stage(new ScreenViewport());
        Language language = game.getLanguageManager().getLanguage();
        backgroundTexture = loadTexture(BACKGROUND_TEXTURE_PATH);
        createButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(CREATE_BUTTON_TEXTURE_PATH, language));
        connectButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(CONNECT_BUTTON_TEXTURE_PATH, language));
        settingsButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(SETTINGS_BUTTON_TEXTURE_PATH, language));
        exitButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(EXIT_BUTTON_TEXTURE_PATH, language));
        Language oppositeLanguage = language == Language.RU ? Language.EN : Language.RU;
        decksButtonTexture = loadTextureIfExists(TexturePathResolver.resolveMenuTexture(DECKS_BUTTON_TEXTURE_PATH, language));
        createDialogButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CREATE_DIALOG_BUTTON_TEXTURE_PATH, language));
        connectDialogButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CONNECT_DIALOG_BUTTON_TEXTURE_PATH, language));
        cancelButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CANCEL_BUTTON_TEXTURE_PATH, language));
        copyCodeButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(COPY_CODE_BUTTON_TEXTURE_PATH, language));
        changeLanguageButtonTexture = loadTextureIfExists(CHANGE_LANGUAGE_BUTTON_TEXTURE_PATH);
        notificationWindowTexture = loadTexture(NOTIFICATION_WINDOW_TEXTURE_PATH);
        inputTexture = loadTexture(INPUT_TEXTURE_PATH);
        createDeckButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CREATE_DECK_BUTTON_TEXTURE_PATH, oppositeLanguage));
        closeDialogButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(BACK_BUTTON_TEXTURE_PATH, oppositeLanguage));
        englishButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(ENGLISH_BUTTON_TEXTURE_PATH, oppositeLanguage));
        russianButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(RUSSIAN_BUTTON_TEXTURE_PATH, oppositeLanguage));
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
        Actor decksButton = createDecksButton();

        Table buttonStack = new Table();
        buttonStack.defaults().left().padBottom(18f);
        buttonStack.add(createButton).size(270f, 80f).row();
        buttonStack.add(joinButton).size(270f, 80f).row();
        buttonStack.add(decksButton).size(270f, 80f).row();
        buttonStack.add(settingsButton).size(270f, 80f).row();
        buttonStack.add(exitButton).size(270f, 80f);

        root.add(buttonStack).expand().center().padTop(120f);
        stage.addActor(root);

        Table languageAnchor = new Table();
        languageAnchor.setFillParent(true);
        Actor languageButton = createLanguageButton();
        languageAnchor.add(languageButton).size(80f, 80f).expand().left().bottom().padLeft(20f).padBottom(20f);
        stage.addActor(languageAnchor);
    }

    private Actor createDecksButton() {
        if (decksButtonTexture != null) {
            ImageButton button = createImageButton(decksButtonTexture);
            button.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    showDecksDialog();
                }
            });
            return button;
        }

        VisTextButton button = new VisTextButton("Колоды");
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showDecksDialog();
            }
        });
        return button;
    }

    private void showDecksDialog() {
        Array<Texture> previewTextures = new Array<>();
        Runnable disposePreviews = () -> {
            for (Texture texture : previewTextures) {
                if (texture != null) {
                    texture.dispose();
                }
            }
            previewTextures.clear();
        };
        Dialog dialog = new Dialog("Колоды", VisUI.getSkin()) {
            private boolean cleanedUp;

            @Override
            public void hide() {
                if (!cleanedUp) {
                    disposePreviews.run();
                    cleanedUp = true;
                }
                super.hide();
            }
        };
        applyDialogTexture(dialog, notificationWindowTexture);
        Table decksTable = new Table();
        Array<MemeDeck> decks = deckRepository.loadDecks();
        if (decks.isEmpty()) {
            decksTable.add(new VisLabel("Пока нет колод")).left().pad(8f);
        } else {
            for (MemeDeck deck : decks) {
                Table row = new Table();
                String previewPath = deck.getPreviewImagePath();
                FileHandle previewFile = resolveDeckImage(previewPath);
                if (previewFile != null && previewFile.exists()) {
                    Texture previewTexture = new Texture(previewFile);
                    previewTextures.add(previewTexture);
                    row.add(new Image(previewTexture)).size(56f, 56f).padRight(10f);
                }
                row.add(new VisLabel(deck.name == null ? "Без названия" : deck.name)).left();
                decksTable.add(row).left().padBottom(8f).row();
            }
        }
        dialog.getContentTable().add(decksTable).left().pad(10f).row();
        ImageButton createDeck = createImageButton(createDeckButtonTexture);
        createDeck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
                showCreateDeckDialog();
            }
        });
        dialog.getButtonTable().add(createDeck).size(220f, 64f).pad(8f);
        ImageButton closeButton = createImageButton(closeDialogButtonTexture);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(closeButton).size(220f, 64f).pad(8f);
        dialog.show(stage);
    }

    private FileHandle resolveDeckImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return null;
        }
        FileHandle localFile = Gdx.files.local(imagePath);
        if (localFile.exists()) {
            return localFile;
        }
        FileHandle absoluteFile = Gdx.files.absolute(imagePath);
        return absoluteFile.exists() ? absoluteFile : null;
    }

    private void showCreateDeckDialog() {
        Dialog dialog = new Dialog("Создание колоды", VisUI.getSkin());
        applyDialogTexture(dialog, notificationWindowTexture);
        VisTextField deckName = new VisTextField();
        deckName.setMessageText("Название колоды");
        Array<String> selectedFiles = new Array<>();
        VisLabel filesCount = new VisLabel("Файлов: 0");
        VisTextButton uploadButton = new VisTextButton("Загрузить изображения");
        uploadButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FileChooser chooser = new FileChooser(FileChooser.Mode.OPEN);
                chooser.setMultiSelectionEnabled(true);
                chooser.setSelectionMode(FileChooser.SelectionMode.FILES);
                chooser.setListener(new FileChooser.Adapter() {
                    @Override
                    public void selected(Array<FileHandle> files) {
                        selectedFiles.clear();
                        for (FileHandle file : files) {
                            selectedFiles.add(file.file().getAbsolutePath());
                        }
                        filesCount.setText("Файлов: " + selectedFiles.size);
                    }
                });
                stage.addActor(chooser.fadeIn());
            }
        });
        dialog.getContentTable().add(new VisLabel("Название:")).left().pad(8f).row();
        dialog.getContentTable().add(deckName).width(280f).pad(8f).row();
        dialog.getContentTable().add(uploadButton).width(280f).pad(8f).row();
        dialog.getContentTable().add(filesCount).left().pad(8f).row();
        VisTextButton saveButton = new VisTextButton("Сохранить");
        saveButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = deckName.getText().trim();
                if (name.isEmpty() || selectedFiles.isEmpty()) {
                    return;
                }
                deckRepository.createDeck(name, selectedFiles);
                dialog.hide();
                showDecksDialog();
            }
        });
        dialog.getButtonTable().add(saveButton).width(150f).pad(8f);
        dialog.button("Отмена", true);
        dialog.show(stage);
    }

    private void showStartGameDialog() {
        VisTextField nameField = new VisTextField();
        nameField.setMessageText("Имя хоста");
        applyInputFieldStyle(nameField);

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

        applyDialogTexture(dialog, notificationWindowTexture, NOTIFICATION_DIALOG_SCALE);
        dialog.getContentTable().add(new VisLabel("Создание игры")).center().padTop(6f).padBottom(4f).row();
        dialog.getContentTable().add(new VisLabel("Введите имя для комнаты")).padBottom(8f);
        dialog.row();
        dialog.getContentTable().add(nameField).width(280).padBottom(8f);

        dialog.getButtonTable().clearChildren();
        dialog.getButtonTable().defaults().padTop(4f).padBottom(4f).padLeft(8f).padRight(8f);
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
        applyInputFieldStyle(nameField);

        VisTextField codeField = new VisTextField();
        codeField.setMessageText("Код комнаты");
        applyInputFieldStyle(codeField);

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
                AppLog.info("Menu", "Расшифрованный IP: " + ip + ", имя=" + playerName);

                game.connectAsGuest(ip, 54555, playerName);
            }
        };

        applyDialogTexture(dialog, notificationWindowTexture, NOTIFICATION_DIALOG_SCALE);
        dialog.getContentTable().add(new VisLabel("Подключение")).center().padTop(6f).padBottom(4f).row();
        dialog.getContentTable().add(statusLabel).padBottom(8f);
        dialog.row();
        dialog.getContentTable().add(nameField).width(300).padBottom(6f);
        dialog.row();
        dialog.getContentTable().add(codeField).width(300).padBottom(8f);

        dialog.getButtonTable().clearChildren();
        dialog.getButtonTable().defaults().padTop(4f).padBottom(4f).padLeft(8f).padRight(8f);
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
        applyDialogTexture(dialog, notificationWindowTexture);
        dialog.text(message == null || message.isBlank() ? "Произошла неизвестная ошибка." : message);
        dialog.button("ОК", true);
        dialog.show(stage);
    }

    private void showInfoDialog(String title, String message) {
        Dialog dialog = new Dialog(title, VisUI.getSkin());
        applyDialogTexture(dialog, notificationWindowTexture);
        dialog.text(message);
        dialog.button("ОК", true);
        dialog.show(stage);
    }

    private void showRoomCodeDialog(String roomCode) {
        Dialog dialog = new Dialog("Комната создана", VisUI.getSkin());

        applyDialogTexture(dialog, notificationWindowTexture);
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

        applyDialogTexture(notification, notificationWindowTexture);
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

        applyDialogTexture(dialog, notificationWindowTexture);
        ImageButton english = createImageButton(englishButtonTexture);
        ImageButton russian = createImageButton(russianButtonTexture);

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
        dialog.getButtonTable().add(english).size(160f, 56f).pad(8f);
        dialog.getButtonTable().add(russian).size(160f, 56f).pad(8f);
        dialog.show(stage);
    }

    private void applyDialogTexture(Dialog dialog, Texture texture) {
        applyDialogTexture(dialog, texture, 1f);
    }

    private void applyDialogTexture(Dialog dialog, Texture texture, float scale) {
        dialog.setBackground(new TextureRegionDrawable(new TextureRegion(texture)));
        dialog.setSize(texture.getWidth() * scale, texture.getHeight() * scale);
    }

    private void applyInputFieldStyle(VisTextField field) {
        VisTextField.VisTextFieldStyle style = new VisTextField.VisTextFieldStyle(field.getStyle());
        TextureRegionDrawable inputBg = new TextureRegionDrawable(new TextureRegion(inputTexture));
        style.background = inputBg;
        style.backgroundOver = inputBg;
        style.focusedBackground = inputBg;
        style.disabledBackground = inputBg;
        field.setStyle(style);
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
        notificationWindowTexture.dispose();
        inputTexture.dispose();
        createDeckButtonTexture.dispose();
        closeDialogButtonTexture.dispose();
        englishButtonTexture.dispose();
        russianButtonTexture.dispose();
        if (changeLanguageButtonTexture != null) {
            changeLanguageButtonTexture.dispose();
        }
        if (decksButtonTexture != null) {
            decksButtonTexture.dispose();
        }
        stage.dispose();
    }
}

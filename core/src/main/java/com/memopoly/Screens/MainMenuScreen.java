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
import com.badlogic.gdx.math.Vector2;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.kotcrab.vis.ui.widget.file.FileChooser;
import com.kotcrab.vis.ui.widget.file.FileChooserListener;
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
    private static final float COMMON_BUTTON_HEIGHT = 64f;
    private static final Color BACKGROUND_COLOR = new Color(0.10f, 0.10f, 0.17f, 1f);
    private static final String BACKGROUND_TEXTURE_PATH = "background.png";
    private static final String CREATE_BUTTON_TEXTURE_PATH = "create_game_btn.png";
    private static final String CONNECT_BUTTON_TEXTURE_PATH = "connect_btn.png";
    private static final String SETTINGS_BUTTON_TEXTURE_PATH = "settings_btn.png";
    private static final String EXIT_BUTTON_TEXTURE_PATH = "exit_btn.png";
    private static final String DECKS_BUTTON_TEXTURE_PATH = "decks_btn.png";
    private static final String CREATE_DIALOG_BUTTON_TEXTURE_PATH = "create_btn.png";
    private static final String CONNECT_DIALOG_BUTTON_TEXTURE_PATH = "connect_btn_for_window.png";
    private static final String CANCEL_BUTTON_TEXTURE_PATH = "cancel_btn.png";
    private static final String COPY_CODE_BUTTON_TEXTURE_PATH = "copy_the_code_btn.png";
    private static final String CHANGE_LANGUAGE_BUTTON_TEXTURE_PATH = "change_language_btn.png";
    private static final String LOBBY_WINDOW_TEXTURE_PATH = "lobby_window.png";
    private static final String INPUT_TEXTURE_PATH = "input.png";
    private static final float MENU_DIALOG_SCALE = 0.88f;
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
    private final Texture lobbyWindowTexture;
    private final Texture inputTexture;
    private final Texture createDeckButtonTexture;
    private final Texture closeDialogButtonTexture;
    private final Texture englishButtonTexture;
    private final Texture russianButtonTexture;
    private final Language language;
    private boolean roomCodeShown;
    private final DeckRepository deckRepository = new DeckRepository();

    public MainMenuScreen(Memopoly game) {
        super(game);
        stage = new Stage(new ScreenViewport());
        language = game.getLanguageManager().getLanguage();
        backgroundTexture = loadTexture(BACKGROUND_TEXTURE_PATH);
        createButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(CREATE_BUTTON_TEXTURE_PATH, language));
        connectButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(CONNECT_BUTTON_TEXTURE_PATH, language));
        settingsButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(SETTINGS_BUTTON_TEXTURE_PATH, language));
        exitButtonTexture = loadTexture(TexturePathResolver.resolveMenuTexture(EXIT_BUTTON_TEXTURE_PATH, language));
        decksButtonTexture = loadTextureIfExists(TexturePathResolver.resolveMenuTexture(DECKS_BUTTON_TEXTURE_PATH, language));
        createDialogButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CREATE_DIALOG_BUTTON_TEXTURE_PATH, language));
        connectDialogButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CONNECT_DIALOG_BUTTON_TEXTURE_PATH, language));
        cancelButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CANCEL_BUTTON_TEXTURE_PATH, language));
        copyCodeButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(COPY_CODE_BUTTON_TEXTURE_PATH, language));
        changeLanguageButtonTexture = loadTextureIfExists(CHANGE_LANGUAGE_BUTTON_TEXTURE_PATH);
        lobbyWindowTexture = loadTexture(LOBBY_WINDOW_TEXTURE_PATH);
        inputTexture = loadTexture(INPUT_TEXTURE_PATH);
        createDeckButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(CREATE_DECK_BUTTON_TEXTURE_PATH, language));
        closeDialogButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(BACK_BUTTON_TEXTURE_PATH, language));
        englishButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(ENGLISH_BUTTON_TEXTURE_PATH, language));
        russianButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(RUSSIAN_BUTTON_TEXTURE_PATH, language));
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

        VisTextButton button = new VisTextButton(t("decks"));
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
        Dialog dialog = new Dialog(t("decks"), VisUI.getSkin()) {
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
        applyDialogTexture(dialog, lobbyWindowTexture, MENU_DIALOG_SCALE);
        Table decksTable = new Table();
        Array<MemeDeck> decks = deckRepository.loadDecks();
        if (decks.isEmpty()) {
            decksTable.add(new VisLabel(t("no_decks"))).left().pad(8f);
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
                row.add(new VisLabel(deck.name == null ? t("unnamed") : deck.name)).left();
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
        showDialog(dialog);
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
        Dialog dialog = new Dialog(t("create_deck"), VisUI.getSkin());
        applyDialogTexture(dialog, lobbyWindowTexture, MENU_DIALOG_SCALE);
        VisTextField deckName = new VisTextField();
        deckName.setMessageText(t("deck_name"));
        Array<String> selectedFiles = new Array<>();
        VisLabel filesCount = new VisLabel(t("files_count") + ": 0");
        VisTextButton uploadButton = new VisTextButton(t("upload_images"));
        uploadButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                FileChooser chooser = new FileChooser(FileChooser.Mode.OPEN);
                chooser.setMultiSelectionEnabled(true);
                chooser.setSelectionMode(FileChooser.SelectionMode.FILES);
                chooser.setListener(new FileChooserListener() {
                    @Override
                    public void selected(Array<FileHandle> files) {
                        selectedFiles.clear();
                        for (FileHandle file : files) {
                            selectedFiles.add(file.file().getAbsolutePath());
                        }
                        filesCount.setText(t("files_count") + ": " + selectedFiles.size);
                    }
                    @Override
                    public void canceled() {}
                });
                stage.addActor(chooser.fadeIn());
            }
        });
        dialog.getContentTable().add(new VisLabel(t("name") + ":")).left().pad(8f).row();
        dialog.getContentTable().add(deckName).width(280f).pad(8f).row();
        dialog.getContentTable().add(uploadButton).width(280f).pad(8f).row();
        dialog.getContentTable().add(filesCount).left().pad(8f).row();
        VisTextButton saveButton = new VisTextButton(t("save"));
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
        dialog.button(t("cancel"), true);
        showDialog(dialog);
    }

    private void showStartGameDialog() {
        VisTextField nameField = new VisTextField();
        nameField.setMessageText(t("host_name"));
        applyInputFieldStyle(nameField);

        Dialog dialog = new Dialog("", VisUI.getSkin()) {
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
                    showErrorDialog(t("create_room_failed"), e.getMessage());
                }
            }
        };

        applyDialogTexture(dialog, lobbyWindowTexture, MENU_DIALOG_SCALE);
        VisLabel title = new VisLabel(t("create_game"));
        title.setColor(new Color(1.00f, 0.83f, 0.25f, 1f));
        title.setFontScale(1.25f);
        dialog.getContentTable().add(title).left().padBottom(90f).padRight(230f).row();
        dialog.getContentTable().add(new VisLabel(t("enter_room_name"))).left().padBottom(120f).padRight(120f).row();
        dialog.row();
        dialog.getContentTable().add(nameField).width(280).height(50).padBottom(120f);

        dialog.getButtonTable().clearChildren();
        dialog.getButtonTable().defaults().padTop(4f).padBottom(40f).padLeft(8f).padRight(8f);
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
                    showErrorDialog(t("create_room_failed"), e.getMessage());
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
        dialog.getButtonTable().add(createActionButton).size(176f, COMMON_BUTTON_HEIGHT);
        dialog.getButtonTable().add(cancelActionButton).size(176f, COMMON_BUTTON_HEIGHT);
        showDialog(dialog);
    }

    private void showConnectDialog() {
        VisTextField nameField = new VisTextField();
        nameField.setMessageText(t("player_name"));
        applyInputFieldStyle(nameField);

        VisTextField codeField = new VisTextField();
        codeField.setMessageText(t("room_code"));
        applyInputFieldStyle(codeField);

        VisLabel statusLabel = new VisLabel("");

        Dialog dialog = new Dialog("", VisUI.getSkin()) {
            @Override
            protected void result(Object object) {
                if (!Boolean.TRUE.equals(object)) {
                    return;
                }

                String playerName = nameField.getText().trim();
                String roomCode = codeField.getText().trim();

                if (playerName.isEmpty()) {
                    statusLabel.setText(t("enter_player_name"));
                    return;
                }

                String ip = RoomCodeGenerator.decodeRoomCode(roomCode);
                if (ip == null || ip.isEmpty()) {
                    statusLabel.setText(t("invalid_room_code"));
                    return;
                }

                statusLabel.setText(t("connecting") + "...");
                AppLog.info("Menu", "Расшифрованный IP: " + ip + ", имя=" + playerName);

                game.connectAsGuest(ip, 54555, playerName);
            }
        };

        applyDialogTexture(dialog, lobbyWindowTexture, MENU_DIALOG_SCALE);
        VisLabel title = new VisLabel(t("connect"));
        title.setColor(new Color(1.00f, 0.83f, 0.25f, 1f));
        title.setFontScale(1.25f);
        dialog.getContentTable().add(title).left().padBottom(60f).padRight(230f).row();
        dialog.getContentTable().add(statusLabel).left().padBottom(24f).padRight(150f).row();
        dialog.row();
        dialog.getContentTable().add(nameField).width(280).height(50).padBottom(40f);
        dialog.row();
        dialog.getContentTable().add(codeField).width(280).height(50).padBottom(40f);

        dialog.getButtonTable().clearChildren();
        dialog.getButtonTable().defaults().padTop(4f).padBottom(40f).padLeft(8f).padRight(8f);
        ImageButton connectActionButton = createImageButton(connectDialogButtonTexture);
        connectActionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String playerName = nameField.getText().trim();
                String roomCode = codeField.getText().trim();

                if (playerName.isEmpty()) {
                    statusLabel.setText(t("enter_player_name"));
                    return;
                }

                String ip = RoomCodeGenerator.decodeRoomCode(roomCode);
                if (ip == null || ip.isEmpty()) {
                    statusLabel.setText(t("invalid_room_code"));
                    return;
                }

                statusLabel.setText(t("connecting") + "...");
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
        dialog.getButtonTable().add(connectActionButton).size(196f, COMMON_BUTTON_HEIGHT);
        dialog.getButtonTable().add(cancelActionButton).size(176f, COMMON_BUTTON_HEIGHT);

        showDialog(dialog);
    }

    private void showErrorDialog(String title, String message) {
        Dialog dialog = new Dialog(title, VisUI.getSkin());
        applyDialogTexture(dialog, lobbyWindowTexture, MENU_DIALOG_SCALE);
        dialog.text(message == null || message.isBlank() ? t("unknown_error") : message);
        dialog.button("OK", true);
        showDialog(dialog);
    }

    private void showInfoDialog(String title, String message) {
        Dialog dialog = new Dialog(title, VisUI.getSkin());
        applyDialogTexture(dialog, lobbyWindowTexture, MENU_DIALOG_SCALE);
        dialog.text(message);
        dialog.button("OK", true);
        showDialog(dialog);
    }

    private void showRoomCodeDialog(String roomCode) {
        Dialog dialog = new Dialog(t("room_created"), VisUI.getSkin());

        applyDialogTexture(dialog, lobbyWindowTexture, MENU_DIALOG_SCALE);
        VisLabel titleLabel = new VisLabel(t("your_room_code") + ":");
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
        dialog.getContentTable().add(copyButton).size(220f, COMMON_BUTTON_HEIGHT).pad(10);
        dialog.getButtonTable().clearChildren();
        ImageButton cancelActionButton = createImageButton(cancelButtonTexture);
        cancelActionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(cancelActionButton).size(176f, COMMON_BUTTON_HEIGHT).pad(10f);
        showDialog(dialog);
    }

    private void showCopiedNotification() {
        Dialog notification = new Dialog("", VisUI.getSkin()) {
            @Override
            protected void result(Object object) {
                // Просто закрываем уведомление
            }
        };

        applyDialogTexture(notification, lobbyWindowTexture, MENU_DIALOG_SCALE);
        notification.getContentTable().add(new VisLabel(t("code_copied"))).pad(20);
        notification.button("OK", true);
        showDialog(notification);

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

        VisTextButton button = new VisTextButton(t("language"));
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showLanguageDialog();
            }
        });
        return button;
    }

    private void showLanguageDialog() {
        Dialog dialog = new Dialog(t("select_language"), VisUI.getSkin());

        applyDialogTexture(dialog, lobbyWindowTexture, MENU_DIALOG_SCALE);
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

        dialog.getContentTable().add(t("select_game_language")).pad(10).row();
        dialog.getButtonTable().add(english).size(160f, COMMON_BUTTON_HEIGHT).pad(8f);
        dialog.getButtonTable().add(russian).size(160f, COMMON_BUTTON_HEIGHT).pad(8f);
        showDialog(dialog);
    }

    private void applyDialogTexture(Dialog dialog, Texture texture) {
        applyDialogTexture(dialog, texture, 1f);
    }

    private void applyDialogTexture(Dialog dialog, Texture texture, float scale) {
        dialog.setBackground(new TextureRegionDrawable(new TextureRegion(texture)));
        dialog.setUserObject(new Vector2(texture.getWidth() * scale, texture.getHeight() * scale));
    }

    private void showDialog(Dialog dialog) {
        dialog.show(stage);
        Object userObject = dialog.getUserObject();
        if (userObject instanceof Vector2 size) {
            dialog.setSize(size.x, size.y);
            dialog.setPosition(
                (stage.getWidth() - size.x) * 0.5f,
                (stage.getHeight() - size.y) * 0.5f
            );
        }
    }

    private void applyInputFieldStyle(VisTextField field) {
        VisTextField.VisTextFieldStyle style = new VisTextField.VisTextFieldStyle(field.getStyle());
        TextureRegionDrawable inputBg = new TextureRegionDrawable(new TextureRegion(inputTexture));
        style.background = inputBg;
        style.backgroundOver = inputBg;
        style.focusedBackground = inputBg;
        style.disabledBackground = inputBg;
        style.selection = VisUI.getSkin().newDrawable("white", new Color(1f, 1f, 1f, 0f));
        style.cursor = VisUI.getSkin().newDrawable("white", new Color(1f, 1f, 1f, 0f));
        style.messageFont = style.font;
        style.messageFontColor = new Color(0.52f, 0.16f, 0.16f, 1f);
        inputBg.setLeftWidth(24f);
        field.setStyle(style);
        field.setTextFieldFilter((textField, c) -> c != '\n' && c != '\r');
        field.setFocusTraversal(false);
    }

    private Texture loadTextureIfExists(String path) {
        if (!Gdx.files.internal(path).exists()) {
            return null;
        }
        return loadTexture(path);
    }

    private String t(String key) {
        boolean ru = language == Language.RU;
        return switch (key) {
            case "decks" -> ru ? "Колоды" : "Decks";
            case "no_decks" -> ru ? "Пока нет колод" : "No decks yet";
            case "unnamed" -> ru ? "Без названия" : "Untitled";
            case "create_deck" -> ru ? "Создание колоды" : "Create deck";
            case "deck_name" -> ru ? "Название колоды" : "Deck name";
            case "files_count" -> ru ? "Файлов" : "Files";
            case "upload_images" -> ru ? "Загрузить изображения" : "Upload images";
            case "name" -> ru ? "Название" : "Name";
            case "save" -> ru ? "Сохранить" : "Save";
            case "cancel" -> ru ? "Отмена" : "Cancel";
            case "host_name" -> ru ? "Имя хоста" : "Host name";
            case "create_game" -> ru ? "Создание игры" : "Create game";
            case "create_room_failed" -> ru ? "Не удалось создать комнату" : "Failed to create room";
            case "enter_room_name" -> ru ? "Введите имя для комнаты" : "Enter room name";
            case "player_name" -> ru ? "Имя игрока" : "Player name";
            case "room_code" -> ru ? "Код комнаты" : "Room code";
            case "connecting" -> ru ? "Подключение" : "Connecting";
            case "connect" -> ru ? "Подключение" : "Connect";
            case "enter_player_name" -> ru ? "Введите имя игрока" : "Enter player name";
            case "invalid_room_code" -> ru ? "Неверный код комнаты" : "Invalid room code";
            case "unknown_error" -> ru ? "Произошла неизвестная ошибка." : "Unknown error occurred.";
            case "room_created" -> ru ? "Комната создана" : "Room created";
            case "your_room_code" -> ru ? "Ваш код комнаты" : "Your room code";
            case "code_copied" -> ru ? "Код скопирован!" : "Code copied!";
            case "language" -> "Language";
            case "select_language" -> ru ? "Выбор языка" : "Select language";
            case "select_game_language" -> ru ? "Выберите язык игры" : "Select game language";
            default -> key;
        };
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
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
        lobbyWindowTexture.dispose();
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

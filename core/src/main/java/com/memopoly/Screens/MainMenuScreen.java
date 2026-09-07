package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.Layout;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.Timer;
import com.badlogic.gdx.utils.Scaling;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.badlogic.gdx.math.Vector2;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.badlogic.gdx.utils.Array;
import com.memopoly.steam.SteamManager;
import com.memopoly.utils.*;
import com.memopoly.Memopoly;
import com.memopoly.modding.DeckRepository;
import com.memopoly.game.model.MemeDeck;
import com.memopoly.utils.LanguageManager.Language;

import java.io.File;

import javax.swing.JFileChooser;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileNameExtensionFilter;

/**
 * Экран главного меню: предоставляет кнопки для создания игры, подключения по коду, настроек и выхода.
 */
public class MainMenuScreen extends BaseScreen {
    private static final float DIALOG_BASE_WIDTH = 1000f;
    private static final float MENU_DIALOG_HEIGHT_SCALE = 0.8f;
    private static final float BUTTON_HEIGHT_MENU = 105f;
    private static final float BUTTON_HEIGHT_WINDOW_RU = 144f;
    private static final float BUTTON_HEIGHT_WINDOW_EN = 144f;
    private static final float CREATE_DECK_INPUT_HEIGHT = 32f;
    private static final float CREATE_DECK_TEXTURE_BUTTON_WIDTH = 232f;
    private static final float CREATE_DECK_TEXTURE_BUTTON_HEIGHT = 52f;
    private static final float CREATE_DECK_ACTION_BUTTON_WIDTH = 128f;
    private static final float CREATE_DECK_ACTION_BUTTON_HEIGHT = 36f;
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
    private static final String CARD_BOARD_TEXTURE_PATH = "card_board.png";
    private static final String INPUT_TEXTURE_PATH = "input.png";
    private static final float MENU_DIALOG_SCALE = 0.7f;
    private static final String CREATE_DECK_BUTTON_TEXTURE_PATH = "create_deck_btn.png";
    private static final String LOAD_IMAGES_BUTTON_TEXTURE_PATH = "load_images_btn.png";
    private static final String SAVE_BUTTON_TEXTURE_PATH = "save_btn.png";
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
    private final Texture loadImagesButtonTexture;
    private final Texture saveButtonTexture;
    private final Texture closeDialogButtonTexture;
    private final Texture englishButtonTexture;
    private final Texture russianButtonTexture;
    private BitmapFont inputFieldFont;
    private BitmapFont inputPlaceholderFont;
    private final Language language;
    private boolean roomCodeShown;
    private final Texture cardBoardTexture;
    private final DeckRepository deckRepository = new DeckRepository();
    private final Array<Dialog> openDialogs = new Array<>();

    private boolean connectInProgress = false;
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
        loadImagesButtonTexture = loadTextureIfExists(TexturePathResolver.resolveScreenTexture(LOAD_IMAGES_BUTTON_TEXTURE_PATH, language));
        saveButtonTexture = loadTextureIfExists(TexturePathResolver.resolveScreenTexture(SAVE_BUTTON_TEXTURE_PATH, language));
        closeDialogButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(BACK_BUTTON_TEXTURE_PATH, language));
        englishButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(ENGLISH_BUTTON_TEXTURE_PATH, language));
        russianButtonTexture = loadTexture(TexturePathResolver.resolveScreenTexture(RUSSIAN_BUTTON_TEXTURE_PATH, language));
        cardBoardTexture = loadTexture(CARD_BOARD_TEXTURE_PATH);
        Gdx.input.setInputProcessor(stage);

        if (inputFieldFont == null) {
            inputFieldFont = loadInputFont();
        }
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
                try {
                    game.startAsHost();
                } catch (RuntimeException e) {
                    showErrorDialog(t("create_room_failed"), e.getMessage());
                }
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
        Actor languageButton = createLanguageButton();

        Table buttonStack = new Table();
        buttonStack.defaults().left().padBottom(2f);
        buttonStack.add(createButton).size(240f, BUTTON_HEIGHT_MENU).row();
        buttonStack.add(joinButton).size(240f, BUTTON_HEIGHT_MENU).padTop(15f).row();
        buttonStack.add(decksButton).size(240f, BUTTON_HEIGHT_MENU).padTop(15f).row();
        buttonStack.add(settingsButton).size(240f, BUTTON_HEIGHT_MENU).padTop(15f).row();
        Table bottomRow = new Table();
        bottomRow.add(languageButton).size(105f, 105f);
        bottomRow.add().expandX();
        bottomRow.add(exitButton).size(105f, 105f);
        buttonStack.add(bottomRow).width(240f).padTop(15f).row();

        root.add(buttonStack).expandX().center().padTop(180f);
        stage.addActor(root);
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
        button.getLabel().setColor(Color.valueOf("000A3E"));
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
        Dialog dialog = new Dialog("", VisUI.getSkin()) {
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
        applyDialogTexture(dialog, lobbyWindowTexture, 940f, 640f); // явный размер — CREATE больше не обрезается

        VisLabel title = new VisLabel(t("decks"));
        title.setFontScale(1.5f);
        title.setColor(Color.valueOf("000A3E"));
        ImageButton closeButton = createImageButton(cancelButtonTexture);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        Table titleRow = new Table();
        titleRow.add().size(55f).padLeft(30f); // зеркало правой стороны — заголовок по центру
        titleRow.add(title).expandX();
        titleRow.add(closeButton).size(55f, 55f).padRight(30f);
        dialog.getContentTable().top();
        dialog.getContentTable().add(titleRow).growX().padTop(24f).padBottom(12f).row();

        Table decksTable = new Table();
        Array<MemeDeck> decks = deckRepository.loadDecks();
        if (decks.isEmpty()) {
            VisLabel noDecks = new VisLabel(t("no_decks"));
            noDecks.setColor(Color.valueOf("000A3E"));
            decksTable.add(noDecks).center().pad(30f);
        } else {
            int index = 0;
            for (MemeDeck deck : decks) {
                decksTable.add(createDeckCard(deck, previewTextures, dialog)).width(200f).pad(12f);
                if (++index % 4 == 0) {
                    decksTable.row();
                }
            }
        }
        ScrollPane decksScroll = new ScrollPane(decksTable, VisUI.getSkin());
        decksScroll.setFadeScrollBars(false);
        decksScroll.setScrollingDisabled(true, false);
        UiScrollStyle.apply(decksScroll, language);
        dialog.getContentTable().add(decksScroll).width(860f).height(400f).center().row();

        ImageButton createDeck = createImageButton(createDeckButtonTexture);
        createDeck.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
                showCreateDeckDialog();
            }
        });
        dialog.getContentTable().add(createDeck).size(220f, 90f).padTop(8f).padBottom(20f);

        showDialog(dialog);
    }

    private Table createDeckCard(MemeDeck deck, Array<Texture> previewTextures, Dialog dialog) {
        Table card = new Table();

        Table frame = new Table();
        frame.pad(8f);
        FileHandle previewFile = resolveDeckImage(deck.getPreviewImagePath());
        if (previewFile != null && previewFile.exists()) {
            Texture previewTexture = new Texture(previewFile);
            previewTextures.add(previewTexture);
            Image image = new Image(previewTexture);
            image.setScaling(Scaling.fit);
            frame.add(image).expand().fill();
        }

        // Превью + кнопка удаления в правом верхнем углу
        Stack previewStack = new Stack();
        previewStack.add(frame);

        boolean isRegular = deck.name != null && deck.name.equalsIgnoreCase("REGULAR");
        if (!isRegular) {
            ImageButton deleteButton = createImageButton(cancelButtonTexture);
            deleteButton.addListener(new ChangeListener() {
                @Override
                public void changed(ChangeEvent event, Actor actor) {
                    deckRepository.deleteDeck(deck.name);
                    dialog.hide();      // задиспозит превью-текстуры через override hide()
                    showDecksDialog();  // пересобрать список
                }
            });
            Table deleteOverlay = new Table();
            deleteOverlay.top().right().padRight(5f);
            deleteOverlay.add(deleteButton).size(40f, 40f);
            previewStack.add(deleteOverlay);
        }
        card.add(previewStack).width(200f).height(150f).row();

        VisLabel nameLabel = new VisLabel(deck.name == null ? t("unnamed") : deck.name);
        nameLabel.setWrap(true);
        nameLabel.setAlignment(Align.center);
        nameLabel.setColor(Color.valueOf("000A3E"));
        card.add(nameLabel).width(200f).center().padTop(8f);
        return card;
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
        final Texture[] previewHolder = new Texture[1];
        Dialog dialog = new Dialog("", VisUI.getSkin()) {
            @Override
            public void hide() {
                if (previewHolder[0] != null) {
                    previewHolder[0].dispose();
                    previewHolder[0] = null;
                }
                super.hide();
            }
        };
        applyDialogTexture(dialog, lobbyWindowTexture, 900f, 620f); // весь контент помещается

        VisLabel title = new VisLabel(t("create_deck"));
        title.setFontScale(1.5f);
        title.setColor(Color.valueOf("000A3E"));
        ImageButton closeButton = createImageButton(cancelButtonTexture);
        closeButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });
        Table titleRow = new Table();
        titleRow.add().size(55f).padLeft(30f);
        titleRow.add(title).expandX();
        titleRow.add(closeButton).size(55f, 55f).padRight(30f);
        dialog.getContentTable().top();
        dialog.getContentTable().add(titleRow).growX().padTop(24f).padBottom(12f).row();

        VisTextField deckName = new VisTextField();
        deckName.setMessageText(t("deck_name"));
        deckName.setMaxLength(7);
        applyInputFieldStyle(deckName);

        Array<String> selectedFiles = new Array<>();
        VisLabel filesCount = new VisLabel(t("files_count") + ": 0");
        filesCount.setColor(Color.valueOf("000A3E"));

        Table previewFrame = new Table();
        previewFrame.setBackground(new TextureRegionDrawable(new TextureRegion(cardBoardTexture)));
        previewFrame.pad(10f);
        Runnable refreshPreview = () -> {
            previewFrame.clearChildren();
            if (previewHolder[0] != null) {
                Image image = new Image(previewHolder[0]);
                image.setScaling(Scaling.fit);
                previewFrame.add(image).expand().fill();
            } else {
                VisLabel hint = new VisLabel(t("preview_hint"));
                hint.setWrap(true);
                hint.setAlignment(Align.center);
                hint.setColor(Color.valueOf("000A3E"));
                previewFrame.add(hint).width(200f).center().expand();
            }
        };
        refreshPreview.run();

        Table leftColumn = new Table();
        Table nameRow = new Table();
        VisLabel nameTitle = new VisLabel(t("name") + ":");
        nameTitle.setColor(Color.valueOf("000A3E"));
        nameRow.add(nameTitle).left().padRight(12f);
        nameRow.add(deckName).width(240f).height(50f);
        leftColumn.add(nameRow).left().padBottom(12f).row();
        leftColumn.add(filesCount).left().row();

        VisLabel formatHint = new VisLabel(t("format_hint"));
        formatHint.setWrap(true);
        formatHint.setFontScale(0.75f);
        formatHint.setColor(Color.valueOf("000A3E"));
        leftColumn.add(formatHint).width(280f).left().padTop(10f).row();

        VisLabel errorLabel = new VisLabel("");
        errorLabel.setColor(new Color(0.85f, 0.15f, 0.15f, 1f));
        errorLabel.setFontScale(0.85f);
        errorLabel.setAlignment(Align.center);

        Table contentRow = new Table();
        contentRow.add(leftColumn).expandX().left().top().padLeft(30f);   // было 60f — слева больше не режет
        contentRow.add(previewFrame).width(300f).height(225f).top().padRight(30f); // было 80f
        dialog.getContentTable().add(contentRow).growX().padTop(6f).row();
        dialog.getContentTable().add(errorLabel).center().padBottom(6f).row();

        Actor uploadButton = createTexturedOrTextButton(loadImagesButtonTexture, t("upload_images"));
        uploadButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                openImageChooser(selectedFiles, filesCount, previewHolder, refreshPreview);
            }
        });

        ImageButton createButton = createImageButton(createDeckButtonTexture);
        createButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                String name = deckName.getText().trim();
                if (name.isEmpty()) {
                    errorLabel.setText(t("enter_deck_name"));
                    return;
                }
                if (selectedFiles.isEmpty()) {
                    errorLabel.setText(t("no_images_selected"));
                    return;
                }
                errorLabel.setText("");
                deckRepository.createDeck(name, selectedFiles);
                dialog.hide();
                showDecksDialog();
            }
        });

        Table bottomRow = new Table();
        bottomRow.add(uploadButton).size(200f, 80f).padRight(30f);
        bottomRow.add(createButton).size(200f, 80f);
        dialog.getContentTable().add(bottomRow).center().padBottom(20f).row();

        showDialog(dialog);
    }

    private void openImageChooser(Array<String> selectedFiles, VisLabel filesCount, Texture[] previewHolder, Runnable refreshPreview) {
        try {
            SwingUtilities.invokeLater(() -> {
                try {
                    JFileChooser chooser = new JFileChooser();
                    chooser.setMultiSelectionEnabled(true);
                    chooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
                    chooser.setDialogTitle(t("upload_images"));
                    chooser.setFileFilter(new FileNameExtensionFilter(
                        "Images (*.png, *.jpg, *.jpeg, *.gif, *.bmp, *.webp)",
                        "png", "jpg", "jpeg", "gif", "bmp", "webp"
                    ));

                    if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                        return;
                    }

                    File[] files = chooser.getSelectedFiles();
                    Gdx.app.postRunnable(() -> {
                        selectedFiles.clear();
                        for (File file : files) {
                            selectedFiles.add(file.getAbsolutePath());
                        }
                        filesCount.setText(t("files_count") + ": " + selectedFiles.size);

                        if (previewHolder[0] != null) {
                            previewHolder[0].dispose();
                            previewHolder[0] = null;
                        }
                        if (!selectedFiles.isEmpty()) {
                            FileHandle first = resolveDeckImage(selectedFiles.get(0));
                            if (first != null && first.exists()) {
                                previewHolder[0] = new Texture(first);
                            }
                        }
                        refreshPreview.run();
                    });
                } catch (RuntimeException exception) {
                    Gdx.app.postRunnable(() -> showErrorDialog(t("upload_images_failed"), exception.getMessage()));
                }
            });
        } catch (RuntimeException exception) {
            showErrorDialog(t("upload_images_failed"), exception.getMessage());
        }
    }

    private void showConnectDialog() {
        connectInProgress = false;
        VisTextField codeField = new VisTextField();
        codeField.setMessageText(t("room_code"));
        applyInputFieldStyle(codeField);

        VisLabel statusLabel = new VisLabel("");
        statusLabel.setColor(Color.valueOf("000A3E"));

        Dialog dialog = new Dialog("", VisUI.getSkin());

        applyDialogTexture(dialog, lobbyWindowTexture, MENU_DIALOG_SCALE);

        VisLabel title = new VisLabel(t("connect"));
        title.setColor(Color.valueOf("000A3E"));
        title.setFontScale(1.5f);

        ImageButton closeActionButton = createImageButton(cancelButtonTexture);
        closeActionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                dialog.hide();
            }
        });

        Table titleRow = new Table();
        titleRow.add().expandX();
        titleRow.add(title);
        titleRow.add().expandX();
        titleRow.add(closeActionButton).size(55f, 55f).padRight(35f);
        dialog.getContentTable().add(titleRow).growX().padLeft(110f).padBottom(60f).row();

        dialog.getContentTable().add(statusLabel).center().padBottom(20f).row();
        dialog.getContentTable().add(codeField).width(322).height(58).padBottom(30f).row();

        dialog.getButtonTable().clearChildren();
        dialog.getButtonTable().defaults().padTop(4f).padBottom(40f).padLeft(8f).padRight(8f);
        ImageButton connectActionButton = createImageButton(connectDialogButtonTexture);
        connectActionButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                if (connectInProgress) {
                    return; // уже подключаемся — повторные клики игнорируем
                }
                String roomCode = codeField.getText().trim();
                if (roomCode.isEmpty()) {
                    statusLabel.setText(t("enter_room_code"));
                    return;
                }
                RoomCodeGenerator.RoomInfo roomInfo = RoomCodeGenerator.decodeRoomCode(roomCode);
                if (roomInfo == null) {
                    statusLabel.setText(t("invalid_room_code"));
                    return;
                }
                connectInProgress = true;
                connectActionButton.setDisabled(true);
                statusLabel.setText(t("connecting") + "...");
                try {
                    game.connectAsGuest(roomInfo.ip, roomInfo.port);
                } catch (RuntimeException e) {
                    connectInProgress = false;
                    connectActionButton.setDisabled(false);
                    showErrorDialog(t("unknown_error"), e.getMessage());
                }
                dialog.hide();
            }
        });
        dialog.getButtonTable().add(connectActionButton).size(180f, 80f);

        showDialog(dialog);
    }

    private void showErrorDialog(String title, String message) {
        Dialog dialog = new Dialog(title, VisUI.getSkin()) {
            @Override
            protected void result(Object object) {
            }
        };
        applyDialogTexture(dialog, lobbyWindowTexture, MENU_DIALOG_SCALE);
        VisLabel msgLabel = new VisLabel(message == null || message.isBlank() ? t("unknown_error") : message);
        msgLabel.setColor(Color.valueOf("000A3E"));
        msgLabel.setWrap(true);
        dialog.getContentTable().add(msgLabel).width(620f).pad(20f).row();
        VisTextButton okButton = new VisTextButton("OK");
        okButton.getLabel().setColor(Color.valueOf("000A3E"));
        dialog.getButtonTable().add(okButton).padBottom(20f);
        showDialog(dialog);
    }

    private void showCopiedNotification() {
        Dialog notification = new Dialog("", VisUI.getSkin()) {
            @Override
            protected void result(Object object) {
            }
        };

        applyDialogTexture(notification, lobbyWindowTexture, MENU_DIALOG_SCALE);
        VisLabel copiedLabel = new VisLabel(t("code_copied"));
        copiedLabel.setColor(Color.valueOf("000A3E"));
        notification.getContentTable().add(copiedLabel).pad(20);
        VisTextButton okButton = new VisTextButton("OK");
        okButton.getLabel().setColor(Color.valueOf("000A3E"));
        notification.getButtonTable().add(okButton).padBottom(20f);
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
        button.getLabel().setColor(Color.valueOf("000A3E"));
        button.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showLanguageDialog();
            }
        });
        return button;
    }

    private void showLanguageDialog() {
        Dialog dialog = new Dialog("", VisUI.getSkin());

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

        VisLabel selectLangLabel = new VisLabel(t("select_game_language"));
        selectLangLabel.setColor(Color.valueOf("000A3E"));
        dialog.getContentTable().add(selectLangLabel).expandX().top().padTop(42f).padBottom(20f).row();
        Table languageButtons = new Table();
        languageButtons.add(english).size(160f, 70f).pad(8f);
        languageButtons.add(russian).size(160f, 70f).pad(8f);
        dialog.getContentTable().add(languageButtons).expand().center().padBottom(20f).row();
        showDialog(dialog);
    }

    private void applyDialogTexture(Dialog dialog, Texture texture, float width, float height) {
        dialog.setBackground(new TextureRegionDrawable(new TextureRegion(texture)));
        dialog.setUserObject(new Vector2(width, height));
    }
    private void applyDialogTexture(Dialog dialog, Texture texture, float scale) {
        dialog.setBackground(new TextureRegionDrawable(new TextureRegion(texture)));
        float base = DIALOG_BASE_WIDTH / texture.getWidth();       // нормализация под базовую ширину
        float w = DIALOG_BASE_WIDTH * scale;
        float h = texture.getHeight() * base * scale * MENU_DIALOG_HEIGHT_SCALE;
        dialog.setUserObject(new Vector2(w, h));
    }

    private void showDialog(Dialog dialog) {
        dialog.show(stage);
        openDialogs.add(dialog);
        if (!openDialogs.contains(dialog, true)) {
            openDialogs.add(dialog);
        }
        centerDialog(dialog);
    }

    private void centerDialog(Dialog dialog) {
        Object userObject = dialog.getUserObject();
        if (userObject instanceof Vector2 size) {
            dialog.setSize(size.x, size.y);
        } else {
            Layout layout = dialog;
            dialog.setSize(layout.getPrefWidth(), layout.getPrefHeight());
        }
        dialog.setPosition(
            (stage.getWidth() - dialog.getWidth()) * 0.5f,
            (stage.getHeight() - dialog.getHeight()) * 0.5f
        );
    }

    private void applyInputFieldStyle(VisTextField field) {
        VisTextField.VisTextFieldStyle style = new VisTextField.VisTextFieldStyle(field.getStyle());
        if (inputFieldFont == null) {
            inputFieldFont = loadInputFont();
        }
        TextureRegionDrawable inputBg = new TextureRegionDrawable(new TextureRegion(inputTexture));
        style.background = inputBg;
        style.backgroundOver = inputBg;
        style.focusedBackground = inputBg;
        style.disabledBackground = inputBg;
        style.focusBorder = null;

        // Прозрачное выделение текста (оставляем как было)
        Drawable transparent = VisUI.getSkin().newDrawable("white", new Color(1f, 1f, 1f, 0f));
        style.selection = transparent;

        if (inputFieldFont != null) {
            style.font = inputFieldFont;
            style.messageFont = inputFieldFont;
        } else {
            style.messageFont = inputFieldFont;
        }

        Color inputColor = Color.valueOf("000A3E");
        style.fontColor = new Color(inputColor);

        style.messageFontColor = new Color(inputColor.r, inputColor.g, inputColor.b, 0.55f);

        Drawable cursorDrawable = VisUI.getSkin().newDrawable("white", new Color(inputColor));
        cursorDrawable.setMinWidth(2f);
        cursorDrawable.setMinHeight(28f);
        style.cursor = cursorDrawable;

        inputBg.setLeftWidth(28f);
        inputBg.setRightWidth(28f);
        field.setStyle(style);
        field.setTextFieldFilter((textField, c) -> c != '\n' && c != '\r');
        field.setFocusTraversal(false);
    }

    private BitmapFont loadInputFont() {
        FileHandle file = Gdx.files.internal("fonts_en/Rubik-Bold.ttf");
        if (!file.exists()) {
            return null;
        }
        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(file);
        try {
            FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
            parameter.size = 174;
            parameter.magFilter = Texture.TextureFilter.Linear;
            parameter.minFilter = Texture.TextureFilter.Linear;
            parameter.characters = FreeTypeFontGenerator.DEFAULT_CHARS
                + "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя№";
            BitmapFont font = generator.generateFont(parameter);
            font.getData().setScale(0.2f);
            font.setUseIntegerPositions(false);
            return font;
        } finally {
            generator.dispose();
        }
    }

    private Texture loadTextureIfExists(String path) {
        if (!Gdx.files.internal(path).exists()) {
            return null;
        }
        return loadTexture(path);
    }

    private float getButtonHeightWindow() {
        return language == Language.RU ? BUTTON_HEIGHT_WINDOW_RU : BUTTON_HEIGHT_WINDOW_EN;
    }

    private String t(String key) {
        boolean ru = language == Language.RU;
        return switch (key) {
            case "decks" -> ru ? "Колоды" : "DECKS";
            case "no_decks" -> ru ? "Пока нет колод" : "No decks yet";
            case "unnamed" -> ru ? "Без названия" : "Untitled";
            case "create_deck" -> ru ? "Создание колоды" : "Create deck";
            case "format_hint" -> ru
                ? "Совет: загружай картинки в формате 4:3 (например 1024×768) — мемы заполнят карточку без пустых полей."
                : "Tip: upload 4:3 images (e.g. 1024×768) so memes fill the card without empty bars.";
            case "deck_name" -> ru ? "Название колоды" : "Deck name";
            case "files_count" -> ru ? "Файлов" : "Files";
            case "preview_hint" -> ru ? "Превью первого изображения" : "PREVIEW FIRST IMAGE";
            case "upload_images" -> ru ? "Загрузить изображения" : "Upload images";
            case "upload_images_failed" -> ru ? "Не удалось открыть выбор изображений" : "Failed to open image chooser";
            case "name" -> ru ? "Название" : "Name";
            case "save" -> ru ? "Сохранить" : "Save";
            case "cancel" -> ru ? "Отмена" : "Cancel";
            case "host_name" -> ru ? "Имя хоста" : "Host name";
            case "create_game" -> ru ? "Создание игры" : "CREATE GAME";
            case "create_room_failed" -> ru ? "Не удалось создать комнату" : "Failed to create room";
            case "enter_room_name" -> ru ? "Введите имя для комнаты" : "Enter room name";
            case "no_images_selected" -> ru ? "Сначала загрузи изображения" : "Upload images first";
            case "enter_deck_name" -> ru ? "Введите название коллоды" : "Enter deck name";
            case "player_name" -> ru ? "Имя игрока" : "Player name";
            case "room_code" -> ru ? "Код комнаты" : "Room code";
            case "connecting" -> ru ? "Подключение" : "Connecting";
            case "connect" -> ru ? "Подключение" : "CONNECT";
            case "enter_player_name" -> ru ? "Введите имя игрока" : "Enter player name";
            case "invalid_room_code" -> ru ? "Неверный код комнаты" : "Invalid room code";
            case "unknown_error" -> ru ? "Произошла неизвестная ошибка." : "Unknown error occurred.";
            case "room_created" -> ru ? "Комната создана" : "Room created";
            case "your_room_code" -> ru ? "Ваш код комнаты" : "Your room code";
            case "code_copied" -> ru ? "Код скопирован!" : "Code copied!";
            case "language" -> "Language";
            case "select_language" -> ru ? "Выбор языка" : "Select language";
            case "select_game_language" -> ru ? "Выберите язык игры" : "SELECT GAME LANGUAGE";
            case "enter_room_code" -> ru ? "Введите код комнаты" : "Enter room code";
            case "ok" -> "OK";
            default -> key;
        };
    }

    private void recenterOpenDialogs() {
        for (int i = openDialogs.size - 1; i >= 0; i--) {
            Dialog d = openDialogs.get(i);
            if (d.getStage() != stage) {
                openDialogs.removeIndex(i);
            } else {
                d.setPosition(
                    (stage.getWidth() - d.getWidth()) * 0.5f,
                    (stage.getHeight() - d.getHeight()) * 0.5f
                );
            }
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
        recenterOpenDialogs();
    }

    @Override
    public void render(float delta) {
        if (!roomCodeShown && game.isHost() && game.getRoomCode() != null && !"UNKNOWN".equals(game.getRoomCode())) {
            roomCodeShown = true;
        }
        Gdx.gl.glClearColor(BACKGROUND_COLOR.r, BACKGROUND_COLOR.g, BACKGROUND_COLOR.b, 1f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getBatch().setColor(Color.WHITE); // сброс остаточного цвета после прошлого кадра
        stage.getBatch().begin();
        stage.getBatch().draw(backgroundTexture, 0f, 0f, stage.getViewport().getWorldWidth(), stage.getViewport().getWorldHeight());
        stage.getBatch().end();
        stage.act(delta);
        stage.draw();
        stage.getBatch().setColor(Color.WHITE); // следующий кадр начинаем с чистого цвета
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }


    private Actor createTexturedOrTextButton(Texture texture, String fallbackText) {
        if (texture != null) {
            return createImageButton(texture);
        }
        VisTextButton fallback = new VisTextButton(fallbackText);
        fallback.getLabel().setColor(Color.valueOf("000A3E"));
        return fallback;
    }

    private ImageButton createImageButton(Texture texture) {
        TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(texture));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp      = drawable;
        style.imageOver    = drawable.tint(new Color(0.82f, 0.82f, 0.82f, 1f));
        style.imageDown    = drawable.tint(new Color(0.70f, 0.70f, 0.70f, 1f));
        style.imageDisabled = drawable.tint(new Color(0.45f, 0.45f, 0.45f, 1f));
        Drawable transparent = VisUI.getSkin().newDrawable("white", new Color(1f, 1f, 1f, 0f));
        style.up = transparent;
        style.over = transparent;
        style.down = transparent;
        ImageButton button = new ImageButton(style);
        button.getImage().setScaling(Scaling.stretch);
        button.getImageCell().grow();
        return button;
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
        cardBoardTexture.dispose();
        createDeckButtonTexture.dispose();
        if (loadImagesButtonTexture != null) {
            loadImagesButtonTexture.dispose();
        }
        if (saveButtonTexture != null) {
            saveButtonTexture.dispose();
        }
        closeDialogButtonTexture.dispose();
        englishButtonTexture.dispose();
        russianButtonTexture.dispose();
        if (inputFieldFont != null) {
            inputFieldFont.dispose();
        }
        if (inputPlaceholderFont != null) {
            inputPlaceholderFont.dispose();
        }
        if (changeLanguageButtonTexture != null) {
            changeLanguageButtonTexture.dispose();
        }
        if (decksButtonTexture != null) {
            decksButtonTexture.dispose();
        }
        stage.dispose();
    }
}

package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.memopoly.Memopoly;
import com.memopoly.network.packets.ChatMessage;

import java.util.List;

/**
 * UI-виджет чата: отображает историю сообщений и поле ввода текста в лобби и во время игры.
 */
public class ChatWidget extends Table {
    private static final Color PANEL_BACKGROUND = new Color(0.06f, 0.05f, 0.10f, 0.82f);
    private static final Color LOG_BACKGROUND = new Color(0.13f, 0.12f, 0.20f, 0.88f);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color SYSTEM_COLOR = new Color(1.00f, 0.83f, 0.25f, 1f);
    private static final float MESSAGES_TOP_PADDING = 14f;
    private static final float MESSAGE_FONT_SCALE = 0.72f;
    private static final float INPUT_ROW_LEFT_PADDING = 8f;
    private static final float INPUT_ROW_TOP_PADDING = 0f;
    private static final float INPUT_ROW_BOTTOM_PADDING = 8f;
    private static final float INPUT_FIELD_WIDTH = 270f;
    private static final float INPUT_CONTROL_HEIGHT = 50f;
    private static final float ENTER_BUTTON_WIDTH = 76f;

    private final Memopoly game;
    private final Texture chatWindowTexture;
    private final Texture inputTexture;
    private final Texture enterButtonTexture;
    private final Table messagesTable;
    private final ScrollPane scrollPane;
    private final VisTextField inputField;
    private final float contentWidth;
    private int renderedMessages = -1;

    public ChatWidget(Memopoly game) {
        this(game, INPUT_FIELD_WIDTH + ENTER_BUTTON_WIDTH + INPUT_ROW_LEFT_PADDING + 14f);
    }

    public ChatWidget(Memopoly game, float contentWidth) {
        this.game = game;
        this.contentWidth = Math.max(220f, contentWidth);
        chatWindowTexture = loadTextureIfExists("chat_window.png");
        inputTexture = loadTextureIfExists("input.png");
        enterButtonTexture = loadTextureIfExists("enter_btn.png");
        setBackground(chatWindowTexture != null ? new TextureRegionDrawable(new TextureRegion(chatWindowTexture)) : drawable(PANEL_BACKGROUND));
        pad(8f);
        top().left();

        messagesTable = new Table();
        messagesTable.top().left();
        messagesTable.padTop(MESSAGES_TOP_PADDING);
        scrollPane = new ScrollPane(messagesTable, VisUI.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.getStyle().background = drawable(LOG_BACKGROUND);

        inputField = new VisTextField();
        inputField.setMessageText("Чат");
        applyInputFieldStyle(inputField);
        Actor sendButton = createSendButton();
        sendButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                sendCurrentMessage();
            }
        });
        inputField.setTextFieldListener((textField, c) -> {
            if (c == '\n' || c == '\r') {
                sendCurrentMessage();
            }
        });

        add(scrollPane).colspan(2).grow().row();
        float sendButtonWidth = Math.min(ENTER_BUTTON_WIDTH, Math.max(54f, this.contentWidth * 0.22f));
        float fieldWidth = Math.max(120f, this.contentWidth - sendButtonWidth - INPUT_ROW_LEFT_PADDING - 14f);

        add(inputField)
            .width(fieldWidth)
            .height(INPUT_CONTROL_HEIGHT)
            .padTop(INPUT_ROW_TOP_PADDING)
            .padBottom(INPUT_ROW_BOTTOM_PADDING)
            .padLeft(INPUT_ROW_LEFT_PADDING)
            .padRight(6f)
            .left();
        add(sendButton)
            .width(sendButtonWidth)
            .height(INPUT_CONTROL_HEIGHT)
            .padTop(INPUT_ROW_TOP_PADDING)
            .padBottom(INPUT_ROW_BOTTOM_PADDING)
            .left();
    }

    public void refresh() {
        List<ChatMessage> messages = game.getChatMessages();
        if (messages.size() == renderedMessages) {
            return;
        }
        renderedMessages = messages.size();
        messagesTable.clearChildren();
        int start = Math.max(0, messages.size() - 40);
        for (int i = start; i < messages.size(); i++) {
            ChatMessage message = messages.get(i);
            VisLabel label = new VisLabel(format(message));
            label.setFontScale(MESSAGE_FONT_SCALE);
            label.setWrap(true);
            label.setColor(message.isSystem ? SYSTEM_COLOR : TEXT_COLOR);
            messagesTable.add(label).width(Math.max(160f, contentWidth - 18f)).left().pad(3f, 5f, 3f, 5f).row();
        }
        scrollPane.layout();
        scrollPane.setScrollPercentY(1f);
    }

    private String format(ChatMessage message) {
        String text = message.message == null ? "" : message.message;
        if (message.isSystem) {
            return text;
        }
        String name = message.playerName == null || message.playerName.isBlank() ? "Игрок" : message.playerName;
        return name + ": " + text;
    }

    private void sendCurrentMessage() {
        String text = inputField.getText().trim();
        if (text.isEmpty()) {
            return;
        }
        game.sendChatMessage(text);
        inputField.setText("");
    }

    private Actor createSendButton() {
        if (enterButtonTexture == null) {
            return new VisTextButton("OK");
        }
        TextureRegionDrawable buttonDrawable = new TextureRegionDrawable(new TextureRegion(enterButtonTexture));
        ImageButton.ImageButtonStyle style = new ImageButton.ImageButtonStyle();
        style.imageUp = buttonDrawable;
        style.imageOver = buttonDrawable.tint(new Color(1f, 1f, 1f, 0.96f));
        style.imageDown = buttonDrawable.tint(new Color(0.86f, 0.86f, 0.86f, 1f));
        style.up = drawable(new Color(1f, 1f, 1f, 0f));
        style.over = style.up;
        style.down = style.up;
        ImageButton button = new ImageButton(style);
        button.getImageCell().grow();
        return button;
    }

    private void applyInputFieldStyle(VisTextField field) {
        if (inputTexture == null) {
            return;
        }
        VisTextField.VisTextFieldStyle style = new VisTextField.VisTextFieldStyle(field.getStyle());
        TextureRegionDrawable background = new TextureRegionDrawable(new TextureRegion(inputTexture));
        style.background = background;
        style.backgroundOver = background;
        style.focusedBackground = background;
        style.disabledBackground = background;
        field.setStyle(style);
    }

    private Texture loadTextureIfExists(String path) {
        if (!Gdx.files.internal(path).exists()) {
            return null;
        }
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    public void dispose() {
        if (chatWindowTexture != null) {
            chatWindowTexture.dispose();
        }
        if (inputTexture != null) {
            inputTexture.dispose();
        }
        if (enterButtonTexture != null) {
            enterButtonTexture.dispose();
        }
    }

    private Drawable drawable(Color color) {
        return VisUI.getSkin().newDrawable("white", color);
    }
}

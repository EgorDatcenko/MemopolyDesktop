package com.memopoly.Screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.memopoly.Memopoly;
import com.memopoly.network.packets.ChatMessage;

import java.util.List;

public class ChatWidget extends Table {
    private static final Color PANEL_BACKGROUND = new Color(0.06f, 0.05f, 0.10f, 0.82f);
    private static final Color LOG_BACKGROUND = new Color(0.13f, 0.12f, 0.20f, 0.88f);
    private static final Color TEXT_COLOR = Color.WHITE;
    private static final Color SYSTEM_COLOR = new Color(1.00f, 0.83f, 0.25f, 1f);

    private final Memopoly game;
    private final Table messagesTable;
    private final ScrollPane scrollPane;
    private final VisTextField inputField;
    private int renderedMessages = -1;

    public ChatWidget(Memopoly game) {
        this.game = game;
        setBackground(drawable(PANEL_BACKGROUND));
        pad(8f);
        top().left();

        messagesTable = new Table();
        messagesTable.top().left();
        scrollPane = new ScrollPane(messagesTable, VisUI.getSkin());
        scrollPane.setFadeScrollBars(false);
        scrollPane.setScrollingDisabled(true, false);
        scrollPane.getStyle().background = drawable(LOG_BACKGROUND);

        inputField = new VisTextField();
        inputField.setMessageText("Чат");
        VisTextButton sendButton = new VisTextButton("OK");
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
        add(inputField).growX().height(34f).padTop(6f).padRight(6f);
        add(sendButton).width(58f).height(34f).padTop(6f);
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
            label.setWrap(true);
            label.setColor(message.isSystem ? SYSTEM_COLOR : TEXT_COLOR);
            messagesTable.add(label).width(300f).left().pad(3f, 5f, 3f, 5f).row();
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

    private Drawable drawable(Color color) {
        return VisUI.getSkin().newDrawable("white", color);
    }
}

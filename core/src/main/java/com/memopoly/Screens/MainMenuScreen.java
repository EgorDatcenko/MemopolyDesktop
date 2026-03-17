package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Dialog;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.kotcrab.vis.ui.VisUI;
import com.kotcrab.vis.ui.widget.VisLabel;
import com.kotcrab.vis.ui.widget.VisTextButton;
import com.kotcrab.vis.ui.widget.VisTextField;
import com.memopoly.Memopoly;
import com.memopoly.utils.ClipboardUtils;
import com.memopoly.utils.RoomCodeGenerator;

public class MainMenuScreen extends BaseScreen {
    private final Stage stage;

    public MainMenuScreen(Memopoly game) {
        super(game);
        stage = new Stage();
        Gdx.input.setInputProcessor(stage);

        createUI();
    }

    private void createUI() {
        Table table = new Table();
        table.setFillParent(true);

        VisTextButton createButton = new VisTextButton("Создать игру");
        createButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                System.out.println("Кнопка 'Создать игру' нажата!");
                game.startAsHost();
                showStartGameDialog();
            }
        });

        VisTextButton joinButton = new VisTextButton("Подключиться");
        joinButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                showConnectDialog();
            }
        });

        VisTextButton exitButton = new VisTextButton("Выход");
        exitButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                Gdx.app.exit();
            }
        });

        table.add(createButton).pad(10);
        table.row();
        table.add(joinButton).pad(10);
        table.row();
        table.add(exitButton).pad(10);

        stage.addActor(table);
    }

    private void showStartGameDialog() {
        String roomCode = game.getRoomCode();

        Dialog dialog = new Dialog("Создание игры", VisUI.getSkin()) {
            @Override
            protected void result(Object object) {
                if (object.equals(true)) {
                    System.out.println("🎮 Начинаем игру как хост!");
                    // TODO: Переключиться на игровой экран
                }
            }
        };

        VisLabel titleLabel = new VisLabel("Ваш код комнаты:");
        VisTextField codeField = new VisTextField(roomCode);
        codeField.setDisabled(true);
        codeField.selectAll();

        VisTextButton copyButton = new VisTextButton("Копировать");
        copyButton.addListener(new ChangeListener() {
            @Override
            public void changed(ChangeEvent event, Actor actor) {
                ClipboardUtils.copyToClipboard(roomCode);
                showCopiedNotification();
            }
        });

        dialog.getContentTable().add(titleLabel).pad(10);
        dialog.row();
        dialog.getContentTable().add(codeField).width(200).pad(10);
        dialog.row();
        dialog.getContentTable().add(copyButton).pad(10);
        dialog.row();

        dialog.button("Отмена", false);

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
                if (!object.equals(true)) {
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

        dialog.button("Подключиться", true);
        dialog.button("Отмена", false);

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

        java.util.Timer timer = new java.util.Timer();
        timer.schedule(new java.util.TimerTask() {
            @Override
            public void run() {
                notification.hide();
                timer.cancel();
            }
        }, 2000);

        notification.show(stage);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.15f, 0.15f, 0.2f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }
}

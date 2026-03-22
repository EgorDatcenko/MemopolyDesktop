package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.memopoly.game.model.BoardCell;

import java.awt.*;

public class BoardRenderer{
    private OrthographicCamera camera;
    private FitViewport viewport;
    private ShapeRenderer shapeRenderer;

    public static final int WORLD_SIZE  = 800;
    public static final int BOARD_X     = 60;
    public static final int BOARD_Y     = 60;
    public static final int CORNER_SIZE = 70;
    public static final int CELL_WIDTH   = 60;
    public static final int CELL_DEPTH  = 70;

    public BoardRenderer() {
        camera = new OrthographicCamera();
        viewport = new FitViewport(800, 800, camera);
        viewport.apply();
        shapeRenderer = new ShapeRenderer();
    }

    public static Vector2 getCellPosition(int index){
        if (index < 0 || index > 39) {
            Gdx.app.error("BoardCell", "Неверный индекс: " + index);
            return new Vector2(0, 0);
        }
        int x = 0;
        int y = 0;
        if(index <= 10){
            if(index == 10) {
                x = BOARD_X;
            }else {
                x = BOARD_X + ((10 - index) * CELL_WIDTH) + CORNER_SIZE - CELL_WIDTH;
            }
            y += BOARD_Y;
        }else if(index <= 20){
            y = BOARD_Y + ((index - 10) * CELL_WIDTH) + CORNER_SIZE - CELL_WIDTH;
            x += BOARD_X;
        }else if(index <= 30){
            x = BOARD_X + ((index - 20) * CELL_WIDTH) + CORNER_SIZE - CELL_WIDTH;
            y = BOARD_Y + (10 * CELL_WIDTH) + CORNER_SIZE - CELL_WIDTH;
        }else{
            x = BOARD_X + (10 * CELL_WIDTH) + CORNER_SIZE - CELL_WIDTH;
            y = BOARD_Y + ((40 - index) * CELL_WIDTH) + CORNER_SIZE - CELL_WIDTH;
        }
        return new Vector2(x, y);
    }
    public void resize(int width, int height) {
        viewport.update(width, height, true); // true = центрировать камеру
    }
    public void render(float delta) {
        camera.update();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < 40; i++) {
            //shapeRenderer.setColor(getCellColor(i));
            Vector2 pos = getCellPosition(i);
            Vector2 size = getCellSize(i);
            shapeRenderer.rect(pos.x, pos.y, size.x, size.y);
        }
        shapeRenderer.end();
    }
    public static Vector2 getCellSize(int index) {
        if (index == 0 || index == 10 || index == 20 || index == 30) {
            return new Vector2(70, 70);
        } else if (index < 10 || (index > 20 && index < 30)){
            return new Vector2(60, 70);
        }
        return new Vector2(70, 60);
    }

    // Возвращает цвет группы для клетки
    public static Color getCellColor(BoardCell cell) {
        switch (cell.group) {
            case REDDIT: return Color.ORANGE;
            case TIKTOK: return Color.BLACK;
            case INSTAGRAM: return Color.PINK;
            case TELEGRAM: return Color.SKY;
            case X: return Color.WHITE;
            case DISCORD: return Color.BLUE;
            case TWITCH: return Color.PURPLE;
        }
        return Color.NAVY;
    }
}

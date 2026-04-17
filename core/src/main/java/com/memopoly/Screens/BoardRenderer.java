package com.memopoly.Screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.memopoly.Memopoly;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BoardRenderer {
    private static final String MAP_TEXTURE_PATH = "map.png";
    private static final float WORLD_WIDTH = 1920f;
    private static final float WORLD_HEIGHT = 1080f;
    private static final float BOARD_TOP_BOTTOM_MARGIN = 10f;
    private static final float BOARD_SIZE = WORLD_HEIGHT - BOARD_TOP_BOTTOM_MARGIN * 2f;
    private static final float[][] CELL_LAYOUT = createCellLayout();
    private static final Color[] PLAYER_COLORS = {
        new Color(0.97f, 0.37f, 0.31f, 1f),
        new Color(0.24f, 0.75f, 0.97f, 1f),
        new Color(0.99f, 0.83f, 0.29f, 1f),
        new Color(0.45f, 0.90f, 0.48f, 1f),
        new Color(0.78f, 0.48f, 1f, 1f),
        new Color(1f, 0.60f, 0.22f, 1f)
    };

    private final Memopoly game;
    private final Texture boardTexture;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Rectangle boardBounds;

    public BoardRenderer(Memopoly game) {
        this.game = game;
        boardTexture = new Texture(MAP_TEXTURE_PATH);
        boardTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();

        shapeRenderer = new ShapeRenderer();
        boardBounds = new Rectangle(
            (WORLD_WIDTH - BOARD_SIZE) / 2f,
            BOARD_TOP_BOTTOM_MARGIN,
            BOARD_SIZE,
            BOARD_SIZE
        );
    }

    public void resize(int width, int height) {
        viewport.update(width, height, true);
    }

    public void render(List<BoardCell> boardCells, GameState gameState) {
        camera.update();

        SpriteBatch batch = game.getBatch();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(boardTexture, boardBounds.x, boardBounds.y, boardBounds.width, boardBounds.height);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        renderOwnedCells(boardCells, gameState);
        renderCurrentCell(gameState);
        renderPlayers(gameState);
        shapeRenderer.end();
    }

    private void renderOwnedCells(List<BoardCell> boardCells, GameState gameState) {
        if (gameState == null) {
            return;
        }

        for (BoardCell cell : boardCells) {
            Rectangle bounds = getCellBounds(cell.id);
            if (gameState.cellOwners.containsKey(cell.id)) {
                int ownerId = gameState.cellOwners.get(cell.id);
                drawOutline(bounds, getPlayerColor(ownerId), Math.max(3f, bounds.width * 0.016f), 0.82f);
            }

            if (gameState.cellMortgaged.getOrDefault(cell.id, false)) {
                shapeRenderer.setColor(new Color(0.06f, 0.06f, 0.10f, 0.45f));
                float inset = Math.max(6f, Math.min(bounds.width, bounds.height) * 0.08f);
                shapeRenderer.rect(bounds.x + inset, bounds.y + inset, bounds.width - inset * 2f, bounds.height - inset * 2f);
            }
        }
    }

    private void renderCurrentCell(GameState gameState) {
        if (gameState == null || gameState.getCurrentPlayer() == null) {
            return;
        }

        Rectangle bounds = getCellBounds(gameState.getCurrentPlayer().position);
        drawOutline(bounds, new Color(1f, 1f, 1f, 1f), Math.max(2f, bounds.width * 0.012f), 0.45f);
    }

    private void renderPlayers(GameState gameState) {
        if (gameState == null || gameState.players == null) {
            return;
        }

        Map<Integer, List<Player>> playersByPosition = new HashMap<>();
        for (Player player : gameState.players) {
            playersByPosition.computeIfAbsent(player.position, key -> new ArrayList<>()).add(player);
        }

        for (Map.Entry<Integer, List<Player>> entry : playersByPosition.entrySet()) {
            Rectangle bounds = getCellBounds(entry.getKey());
            List<Player> playersOnCell = entry.getValue();

            float tokenRadius = Math.max(9f, Math.min(bounds.width, bounds.height) * 0.09f);
            float orbitRadius = playersOnCell.size() == 1 ? 0f : Math.max(tokenRadius * 1.35f, Math.min(bounds.width, bounds.height) * 0.16f);
            float centerX = bounds.x + bounds.width / 2f;
            float centerY = bounds.y + bounds.height / 2f;

            if (entry.getKey() == 0 || entry.getKey() == 10 || entry.getKey() == 20 || entry.getKey() == 30) {
                centerY += tokenRadius * 0.15f;
            }

            for (int i = 0; i < playersOnCell.size(); i++) {
                Player player = playersOnCell.get(i);
                float angle = playersOnCell.size() == 1 ? 0f : (float) ((Math.PI * 2 * i / playersOnCell.size()) - Math.PI / 2f);
                float tokenX = centerX + (float) Math.cos(angle) * orbitRadius;
                float tokenY = centerY + (float) Math.sin(angle) * orbitRadius;

                shapeRenderer.setColor(new Color(0.06f, 0.06f, 0.10f, 0.85f));
                shapeRenderer.circle(tokenX, tokenY, tokenRadius + 3f);

                shapeRenderer.setColor(getPlayerColor(player.id));
                shapeRenderer.circle(tokenX, tokenY, tokenRadius);
            }
        }
    }

    private void drawOutline(Rectangle bounds, Color baseColor, float thickness, float alpha) {
        float inset = Math.max(1.5f, thickness);
        Rectangle inner = new Rectangle(
            bounds.x + inset,
            bounds.y + inset,
            bounds.width - inset * 2f,
            bounds.height - inset * 2f
        );

        if (inner.width <= 0f || inner.height <= 0f) {
            inner = bounds;
        }

        Color color = new Color(baseColor);
        color.a = alpha;
        shapeRenderer.setColor(color);

        shapeRenderer.rect(inner.x, inner.y, inner.width, thickness);
        shapeRenderer.rect(inner.x, inner.y + inner.height - thickness, inner.width, thickness);
        shapeRenderer.rect(inner.x, inner.y, thickness, inner.height);
        shapeRenderer.rect(inner.x + inner.width - thickness, inner.y, thickness, inner.height);
    }

    private Rectangle getCellBounds(int index) {
        float[] layout = CELL_LAYOUT[index];
        return new Rectangle(
            boardBounds.x + boardBounds.width * layout[0],
            boardBounds.y + boardBounds.height * layout[1],
            boardBounds.width * layout[2],
            boardBounds.height * layout[3]
        );
    }

    private Color getPlayerColor(int playerId) {
        return PLAYER_COLORS[Math.abs(playerId) % PLAYER_COLORS.length];
    }

    public void dispose() {
        boardTexture.dispose();
        shapeRenderer.dispose();
    }

    private static float[][] createCellLayout() {
        float[][] layout = new float[40][4];

        float cornerWidth = 0.126f;
        float cornerHeight = 0.126f;
        float horizontalCellWidth = (1f - 2f * cornerWidth) / 9f;
        float verticalCellHeight = (1f - 2f * cornerHeight) / 9f;

        // Bottom row
        layout[0] = rect(1f - cornerWidth, 0f, cornerWidth, cornerHeight);
        for (int i = 1; i < 10; i++) {
            layout[i] = rect(1f - cornerWidth - horizontalCellWidth * i, 0f, horizontalCellWidth, cornerHeight);
        }

        // Left column
        layout[10] = rect(0f, 0f, cornerWidth, cornerHeight);
        for (int i = 11; i < 20; i++) {
            layout[i] = rect(0f, cornerHeight + verticalCellHeight * (i - 11), cornerWidth, verticalCellHeight);
        }

        // Top row
        layout[20] = rect(0f, 1f - cornerHeight, cornerWidth, cornerHeight);
        for (int i = 21; i < 30; i++) {
            layout[i] = rect(cornerWidth + horizontalCellWidth * (i - 21), 1f - cornerHeight, horizontalCellWidth, cornerHeight);
        }

        // Right column
        layout[30] = rect(1f - cornerWidth, 1f - cornerHeight, cornerWidth, cornerHeight);
        for (int i = 31; i < 40; i++) {
            layout[i] = rect(1f - cornerWidth, 1f - cornerHeight - verticalCellHeight * (i - 30), cornerWidth, verticalCellHeight);
        }

        // First manual calibration pass for the real art:
        // corners are visually larger than regular cells, and side strips need
        // a bit more breathing room from the decorative inner frame.
        layout[0] = rect(0.878f, 0.004f, 0.118f, 0.118f);
        layout[10] = rect(0.002f, 0.000f, 0.124f, 0.126f);
        layout[20] = rect(0.002f, 0.874f, 0.124f, 0.126f);
        layout[30] = rect(0.874f, 0.874f, 0.126f, 0.126f);

        // Bottom side: align overlays tighter to the visible cell borders.
        layout[1] = rect(0.768f, 0.000f, 0.106f, 0.126f);
        layout[2] = rect(0.678f, 0.000f, 0.090f, 0.126f);
        layout[3] = rect(0.584f, 0.000f, 0.094f, 0.126f);
        layout[4] = rect(0.490f, 0.000f, 0.094f, 0.126f);
        layout[5] = rect(0.393f, 0.000f, 0.097f, 0.126f);
        layout[6] = rect(0.300f, 0.000f, 0.093f, 0.126f);
        layout[7] = rect(0.207f, 0.000f, 0.093f, 0.126f);
        layout[8] = rect(0.113f, 0.000f, 0.094f, 0.126f);
        layout[9] = rect(0.000f, 0.000f, 0.126f, 0.126f);

        // Right side
        layout[31] = rect(0.874f, 0.778f, 0.126f, 0.096f);
        layout[32] = rect(0.874f, 0.683f, 0.126f, 0.095f);
        layout[33] = rect(0.874f, 0.589f, 0.126f, 0.094f);
        layout[34] = rect(0.874f, 0.494f, 0.126f, 0.095f);
        layout[35] = rect(0.874f, 0.398f, 0.126f, 0.096f);
        layout[36] = rect(0.874f, 0.304f, 0.126f, 0.094f);
        layout[37] = rect(0.874f, 0.210f, 0.126f, 0.094f);
        layout[38] = rect(0.874f, 0.116f, 0.126f, 0.094f);
        layout[39] = rect(0.874f, 0.000f, 0.126f, 0.126f);

        // Left side
        layout[11] = rect(0.003f, 0.126f, 0.123f, 0.093f);
        layout[12] = rect(0.003f, 0.219f, 0.123f, 0.093f);
        layout[13] = rect(0.003f, 0.312f, 0.123f, 0.094f);
        layout[14] = rect(0.006f, 0.409f, 0.120f, 0.091f);
        layout[15] = rect(0.006f, 0.501f, 0.120f, 0.091f);
        layout[16] = rect(0.006f, 0.592f, 0.120f, 0.090f);
        layout[17] = rect(0.003f, 0.689f, 0.123f, 0.093f);
        layout[18] = rect(0.003f, 0.782f, 0.123f, 0.092f);
        layout[19] = rect(0.002f, 0.874f, 0.124f, 0.126f);

        // Top side
        layout[21] = rect(0.126f, 0.874f, 0.093f, 0.126f);
        layout[22] = rect(0.219f, 0.874f, 0.093f, 0.126f);
        layout[23] = rect(0.312f, 0.874f, 0.093f, 0.126f);
        layout[24] = rect(0.405f, 0.874f, 0.093f, 0.126f);
        layout[25] = rect(0.498f, 0.874f, 0.093f, 0.126f);
        layout[26] = rect(0.591f, 0.874f, 0.093f, 0.126f);
        layout[27] = rect(0.684f, 0.874f, 0.093f, 0.126f);
        layout[28] = rect(0.777f, 0.874f, 0.097f, 0.126f);
        layout[29] = rect(0.874f, 0.874f, 0.126f, 0.126f);

        return layout;
    }

    private static float[] rect(float x, float y, float width, float height) {
        return new float[] {x, y, width, height};
    }
}

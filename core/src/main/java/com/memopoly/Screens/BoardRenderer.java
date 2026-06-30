package com.memopoly.Screens;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.memopoly.Memopoly;
import com.memopoly.utils.TexturePathResolver;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Отрисовщик игрового поля: рисует карту, рамки владения, фишки игроков и анимации перемещения на поле.
 */
public class BoardRenderer {
    private static final String MAP_TEXTURE_PATH = "map.png";
    private static final String ACTIVE_OUTLINE_SQUARE_TEXTURE_PATH = "green(1x1).png";
    private static final String ACTIVE_OUTLINE_HORIZONTAL_TEXTURE_PATH = "green(horizontal).png";
    private static final String ACTIVE_OUTLINE_VERTICAL_TEXTURE_PATH = "green(vertical).png";
    private static final String MORTGAGE_CELL_HORIZONTAL_TEXTURE_PATH = "mortgage_cell_horizontal.png";
    private static final String MORTGAGE_CELL_VERTICAL_TEXTURE_PATH = "mortgage_cell_vertical.png";
    private static final float WORLD_WIDTH = 1920f;
    private static final float WORLD_HEIGHT = 1080f;
    private static final float BOARD_TOP_BOTTOM_MARGIN = 10f;
    private static final float BOARD_SIZE = WORLD_HEIGHT - BOARD_TOP_BOTTOM_MARGIN * 2f;
    private static final float MORTGAGE_OVERLAY_SCALE = 1.10f;
    private static final float[][] CELL_LAYOUT = createCellLayout();

    private final Memopoly game;
    private final Texture boardTexture;
    private final Texture activeOutlineSquareTexture;
    private final Texture activeOutlineHorizontalTexture;
    private final Texture activeOutlineVerticalTexture;
    private final Texture mortgageCellHorizontalTexture;
    private final Texture mortgageCellVerticalTexture;
    private final OutlineSet[] playerOutlineSets;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Rectangle boardBounds;

    public BoardRenderer(Memopoly game) {
        this.game = game;
        boardTexture = new Texture(TexturePathResolver.resolveBoardMapTexture(game.getLanguageManager().getLanguage()));
        boardTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        activeOutlineSquareTexture = loadTexture(ACTIVE_OUTLINE_SQUARE_TEXTURE_PATH);
        activeOutlineHorizontalTexture = loadTexture(ACTIVE_OUTLINE_HORIZONTAL_TEXTURE_PATH);
        activeOutlineVerticalTexture = loadTexture(ACTIVE_OUTLINE_VERTICAL_TEXTURE_PATH);
        mortgageCellHorizontalTexture = loadTexture(MORTGAGE_CELL_HORIZONTAL_TEXTURE_PATH);
        mortgageCellVerticalTexture = loadTexture(MORTGAGE_CELL_VERTICAL_TEXTURE_PATH);
        playerOutlineSets = new OutlineSet[] {
            createOutlineSet("red", new Color(0.95f, 0.34f, 0.28f, 1f)),
            createOutlineSet("blue", new Color(0.23f, 0.70f, 0.98f, 1f)),
            createOutlineSet("yellow", new Color(1.00f, 0.84f, 0.31f, 1f)),
            createOutlineSet("orange", new Color(1.00f, 0.57f, 0.16f, 1f)),
            createOutlineSet("purple", new Color(0.77f, 0.48f, 0.98f, 1f))
        };

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

    public FitViewport getViewport() {
        return viewport;
    }

    public Rectangle getBoardBounds() {
        return new Rectangle(boardBounds);
    }

    public Rectangle getDicePanelBounds() {
        return getOverlayBounds(0.132f, 0.698f, 0.688f, 0.180f);
    }

    public Rectangle getCurrentCellPanelBounds() {
        return getOverlayBounds(0.126f, 0.106f, 0.292f, 0.170f);
    }

    public Rectangle getFeedPanelBounds() {
        return getOverlayBounds(0.505f, 0.106f, 0.292f, 0.170f);
    }

    public void render(List<BoardCell> boardCells, GameState gameState) {
        camera.update();

        SpriteBatch batch = game.getBatch();
        batch.setProjectionMatrix(camera.combined);
        batch.begin();
        batch.draw(boardTexture, boardBounds.x, boardBounds.y, boardBounds.width, boardBounds.height);
        renderOwnedCells(batch, boardCells, gameState);
        renderMortgagedCells(batch, boardCells, gameState);
        renderCurrentCell(batch, gameState);
        batch.end();

        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        renderPlayers(gameState);
        shapeRenderer.end();
    }

    private void renderCurrentCell(SpriteBatch batch, GameState gameState) {
        if (gameState == null || gameState.getCurrentPlayer() == null) {
            return;
        }

        int currentCellIndex = gameState.getCurrentPlayer().position;
        Rectangle bounds = getCellBounds(currentCellIndex);
        drawOutlineTexture(batch, getActiveOutlineTexture(currentCellIndex), bounds, 0.96f);
    }

    private void renderOwnedCells(SpriteBatch batch, List<BoardCell> boardCells, GameState gameState) {
        if (gameState == null || gameState.cellOwners == null) {
            return;
        }

        for (BoardCell cell : boardCells) {
            Integer ownerId = gameState.cellOwners.get(cell.id);
            if (ownerId == null) {
                continue;
            }

            Rectangle bounds = getCellBounds(cell.id);
            drawOutlineTexture(batch, getPlayerOutlineTexture(ownerId, cell.id), bounds, 0.90f);
        }
    }

    private void renderMortgagedCells(SpriteBatch batch, List<BoardCell> boardCells, GameState gameState) {
        if (gameState == null) {
            return;
        }

        for (BoardCell cell : boardCells) {
            Rectangle bounds = getCellBounds(cell.id);
            if (gameState.cellMortgaged.getOrDefault(cell.id, false)) {
                Texture mortgageTexture = isHorizontalCell(cell.id) ? mortgageCellHorizontalTexture : mortgageCellVerticalTexture;

                // tuning point #1: overlay insets (X/Y) and scale by orientation
                float insetX = isHorizontalCell(cell.id) ? bounds.width * 0.08f : bounds.width * 0.18f;
                float insetY = isHorizontalCell(cell.id) ? bounds.height * 0.18f : bounds.height * 0.08f;

                // tuning point #2: final draw rect (x/y/width/height) for precise alignment
                float drawX = bounds.x + insetX;
                float drawY = bounds.y + insetY;
                float drawWidth = bounds.width - insetX * 2f;
                float drawHeight = bounds.height - insetY * 2f;
                float widthGrowth = drawWidth * (MORTGAGE_OVERLAY_SCALE - 1f);
                float heightGrowth = drawHeight * (MORTGAGE_OVERLAY_SCALE - 1f);

                batch.draw(
                    mortgageTexture,
                    drawX - widthGrowth / 2f,
                    drawY - heightGrowth / 2f,
                    drawWidth + widthGrowth,
                    drawHeight + heightGrowth
                );
            }
        }
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

            float tokenRadius = Math.max(9f, Math.min(bounds.width, bounds.height) * 0.085f);
            List<Vector2> tokenPositions = getTokenPositions(entry.getKey(), bounds, playersOnCell.size(), tokenRadius);

            for (int i = 0; i < playersOnCell.size(); i++) {
                Player player = playersOnCell.get(i);
                Vector2 position = tokenPositions.get(i);
                float tokenX = position.x;
                float tokenY = position.y;

                shapeRenderer.setColor(new Color(0.06f, 0.06f, 0.10f, 0.85f));
                shapeRenderer.circle(tokenX, tokenY, tokenRadius + 3f);

                shapeRenderer.setColor(getPlayerColor(player.id));
                shapeRenderer.circle(tokenX, tokenY, tokenRadius);
            }
        }
    }

    private void drawOutlineTexture(SpriteBatch batch, Texture texture, Rectangle bounds, float alpha) {
        Color previousColor = batch.getColor();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(texture, bounds.x, bounds.y, bounds.width, bounds.height);
        batch.setColor(previousColor);
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
        return playerOutlineSets[Math.abs(playerId) % playerOutlineSets.length].tokenColor;
    }

    public void dispose() {
        boardTexture.dispose();
        activeOutlineSquareTexture.dispose();
        activeOutlineHorizontalTexture.dispose();
        activeOutlineVerticalTexture.dispose();
        mortgageCellHorizontalTexture.dispose();
        mortgageCellVerticalTexture.dispose();
        for (OutlineSet outlineSet : playerOutlineSets) {
            outlineSet.dispose();
        }
        shapeRenderer.dispose();
    }

    private Texture getActiveOutlineTexture(int cellIndex) {
        if (isCornerCell(cellIndex)) {
            return activeOutlineSquareTexture;
        }
        if (isVerticalCardCell(cellIndex)) {
            return activeOutlineVerticalTexture;
        }
        return activeOutlineHorizontalTexture;
    }

    private Texture getPlayerOutlineTexture(int playerId, int cellIndex) {
        OutlineSet outlineSet = playerOutlineSets[Math.abs(playerId) % playerOutlineSets.length];
        if (isCornerCell(cellIndex)) {
            return outlineSet.square;
        }
        if (isVerticalCardCell(cellIndex)) {
            return outlineSet.vertical;
        }
        return outlineSet.horizontal;
    }

    private boolean isCornerCell(int cellIndex) {
        return cellIndex == 0 || cellIndex == 10 || cellIndex == 20 || cellIndex == 30;
    }

    private boolean isHorizontalCell(int cellIndex) {
        return (cellIndex >= 11 && cellIndex <= 19) || (cellIndex >= 31 && cellIndex <= 39);
    }

    private boolean isVerticalCardCell(int cellIndex) {
        return (cellIndex >= 1 && cellIndex <= 9) || (cellIndex >= 21 && cellIndex <= 29);
    }

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        return texture;
    }

    private Rectangle getOverlayBounds(float x, float y, float width, float height) {
        return new Rectangle(
            boardBounds.x + boardBounds.width * x,
            boardBounds.y + boardBounds.height * y,
            boardBounds.width * width,
            boardBounds.height * height
        );
    }

    private List<Vector2> getTokenPositions(int cellIndex, Rectangle bounds, int playerCount, float tokenRadius) {
        List<Vector2> positions = new ArrayList<>();

        if (playerCount <= 0) {
            return positions;
        }

        if (isLeftSideCell(cellIndex) && playerCount <= 2) {
            float x = bounds.x + bounds.width * 0.72f;
            if (playerCount == 1) {
                positions.add(new Vector2(x, bounds.y + bounds.height * 0.50f));
            } else {
                positions.add(new Vector2(x, bounds.y + bounds.height * 0.38f));
                positions.add(new Vector2(x, bounds.y + bounds.height * 0.62f));
            }
            return positions;
        }

        if (isRightSideCell(cellIndex) && playerCount <= 2) {
            float x = bounds.x + bounds.width * 0.28f;
            if (playerCount == 1) {
                positions.add(new Vector2(x, bounds.y + bounds.height * 0.50f));
            } else {
                positions.add(new Vector2(x, bounds.y + bounds.height * 0.38f));
                positions.add(new Vector2(x, bounds.y + bounds.height * 0.62f));
            }
            return positions;
        }

        if (isVerticalCardCell(cellIndex) && playerCount <= 4) {
            addSidePositions(bounds, playerCount, positions);
            return positions;
        }

        if (isHorizontalCell(cellIndex) && playerCount <= 4) {
            addSidePositions(bounds, playerCount, positions);
            return positions;
        }

        if (isCornerCell(cellIndex) && playerCount <= 4) {
            addSidePositions(bounds, playerCount, positions);
            return positions;
        }

        float centerX = bounds.x + bounds.width * 0.5f;
        float centerY = bounds.y + bounds.height * 0.5f;
        float orbitRadius = playerCount == 1 ? 0f : Math.max(tokenRadius * 1.18f, Math.min(bounds.width, bounds.height) * 0.12f);

        if (isLeftSideCell(cellIndex)) {
            centerX = bounds.x + bounds.width * 0.58f;
        } else if (isRightSideCell(cellIndex)) {
            centerX = bounds.x + bounds.width * 0.42f;
        }

        for (int i = 0; i < playerCount; i++) {
            float angle = playerCount == 1 ? 0f : (float) ((Math.PI * 2 * i / playerCount) - Math.PI / 2f);
            positions.add(new Vector2(
                centerX + (float) Math.cos(angle) * orbitRadius,
                centerY + (float) Math.sin(angle) * orbitRadius
            ));
        }
        return positions;
    }

    private void addSidePositions(Rectangle bounds, int playerCount, List<Vector2> positions) {
        float leftX = bounds.x + bounds.width * 0.28f;
        float rightX = bounds.x + bounds.width * 0.72f;
        float topY = bounds.y + bounds.height * 0.64f;
        float bottomY = bounds.y + bounds.height * 0.36f;
        Vector2[] slots = new Vector2[] {
            new Vector2(leftX, topY),
            new Vector2(leftX, bottomY),
            new Vector2(rightX, topY),
            new Vector2(rightX, bottomY)
        };
        for (int i = 0; i < playerCount && i < slots.length; i++) {
            positions.add(slots[i]);
        }
    }

    private OutlineSet createOutlineSet(String baseName, Color tokenColor) {
        String horizontalPath = switch (baseName) {
            case "yellow" -> baseName + "(horizont).png";
            default -> baseName + "(horizontal).png";
        };
        return new OutlineSet(
            loadTexture(baseName + "(1x1).png"),
            loadTexture(horizontalPath),
            loadTexture(baseName + "(vertical).png"),
            tokenColor
        );
    }

    private static float[][] createCellLayout() {
        float[][] layout = new float[40][4];

        float cornerWidth = 0.126f;
        float cornerHeight = 0.126f;
        float horizontalCellWidth = (1f - 2f * cornerWidth) / 9f;
        float verticalCellHeight = (1f - 2f * cornerHeight) / 9f;

        layout[0] = rect(1f - cornerWidth, 0f, cornerWidth, cornerHeight);
        for (int i = 1; i < 10; i++) {
            layout[i] = rect(1f - cornerWidth - horizontalCellWidth * i, 0f, horizontalCellWidth, cornerHeight);
        }

        layout[10] = rect(0f, 0f, cornerWidth, cornerHeight);
        for (int i = 11; i < 20; i++) {
            layout[i] = rect(0f, cornerHeight + verticalCellHeight * (i - 11), cornerWidth, verticalCellHeight);
        }

        layout[20] = rect(0f, 1f - cornerHeight, cornerWidth, cornerHeight);
        for (int i = 21; i < 30; i++) {
            layout[i] = rect(cornerWidth + horizontalCellWidth * (i - 21), 1f - cornerHeight, horizontalCellWidth, cornerHeight);
        }

        layout[30] = rect(1f - cornerWidth, 1f - cornerHeight, cornerWidth, cornerHeight);
        for (int i = 31; i < 40; i++) {
            layout[i] = rect(1f - cornerWidth, 1f - cornerHeight - verticalCellHeight * (i - 30), cornerWidth, verticalCellHeight);
        }

        // First manual calibration pass for the real art:
        layout[0] = rect(0.870f, 0f, 0.130f, 0.132f);
        layout[10] = rect(0.000f, 0.000f, 0.130f, 0.132f);
        layout[20] = rect(0.000f, 0.868f, 0.130f, 0.132f);
        layout[30] = rect(0.870f, 0.868f, 0.130f, 0.132f);


        layout[9] = rect(0.12810000f, 0.000f, 0.08702222222222222f, 0.132f);
        layout[8] = rect(0.2113222222222222f, 0.000f, 0.08702222222222222f, 0.132f);
        layout[7] = rect(0.2935444444444444f, 0.000f, 0.08702222222222222f, 0.132f);
        layout[6] = rect(0.3757666666666667f, 0.000f, 0.08702222222222222f, 0.132f);
        layout[5] = rect(0.4579888888888889f, 0.000f, 0.08702222222222222f, 0.132f);
        layout[4] = rect(0.5402111111111111f, 0.000f, 0.08702222222222222f, 0.132f);
        layout[3] = rect(0.6224333333333333f, 0.000f, 0.08702222222222222f, 0.132f);
        layout[2] = rect(0.7046555555555555f, 0.000f, 0.08702222222222222f, 0.132f);
        layout[1] = rect(0.7868777777777778f, 0.000f, 0.08702222222222222f, 0.132f);

        layout[31] = rect(0.870f, 0.7858889f, 0.132f, 0.08702222222222222f);
        layout[32] = rect(0.870f, 0.7036667f, 0.132f, 0.08702222222222222f);
        layout[33] = rect(0.870f, 0.6214444f, 0.132f, 0.08702222222222222f);
        layout[34] = rect(0.870f, 0.5392222f, 0.132f, 0.08702222222222222f);
        layout[35] = rect(0.870f, 0.4570000f, 0.132f, 0.08702222222222222f);
        layout[36] = rect(0.870f, 0.3747778f, 0.132f, 0.08702222222222222f);
        layout[37] = rect(0.870f, 0.2925556f, 0.132f, 0.08702222222222222f);
        layout[38] = rect(0.870f, 0.2103333f, 0.132f, 0.08702222222222222f);
        layout[39] = rect(0.870f, 0.1281111f, 0.132f, 0.08702222222222222f);

        layout[11] = rect(0.000f, 0.1281111f, 0.130f, 0.08702222222222222f);
        layout[12] = rect(0.000f, 0.2103333f, 0.130f, 0.08702222222222222f);
        layout[13] = rect(0.000f, 0.2925556f, 0.130f, 0.08702222222222222f);
        layout[14] = rect(0.000f, 0.3747778f, 0.130f, 0.08702222222222222f);
        layout[15] = rect(0.000f, 0.4570000f, 0.130f, 0.08702222222222222f);
        layout[16] = rect(0.000f, 0.5392222f, 0.130f, 0.08702222222222222f);
        layout[17] = rect(0.000f, 0.6214444f, 0.130f, 0.08702222222222222f);
        layout[18] = rect(0.000f, 0.7036667f, 0.130f, 0.08702222222222222f);
        layout[19] = rect(0.000f, 0.7858889f, 0.130f, 0.08702222222222222f);

        layout[21] = rect(0.12710000f, 0.868f, 0.08702222222222222f, 0.132f);
        layout[22] = rect(0.2103222222222222f, 0.868f, 0.08702222222222222f, 0.132f);
        layout[23] = rect(0.2925444444444444f, 0.868f, 0.08702222222222222f, 0.132f);
        layout[24] = rect(0.3747666666666667f, 0.868f, 0.08702222222222222f, 0.132f);
        layout[25] = rect(0.4569888888888889f, 0.868f, 0.08702222222222222f, 0.132f);
        layout[26] = rect(0.5392111111111111f, 0.868f, 0.08702222222222222f, 0.132f);
        layout[27] = rect(0.6214333333333333f, 0.868f, 0.08702222222222222f, 0.132f);
        layout[28] = rect(0.7036555555555555f, 0.868f, 0.08702222222222222f, 0.132f);
        layout[29] = rect(0.7858777777777778f, 0.868f, 0.08702222222222222f, 0.132f);

        return layout;
    }

    private static float[] rect(float x, float y, float width, float height) {
        return new float[] {x, y, width, height};
    }

    private static class OutlineSet {
        private final Texture square;
        private final Texture horizontal;
        private final Texture vertical;
        private final Color tokenColor;

        private OutlineSet(Texture square, Texture horizontal, Texture vertical, Color tokenColor) {
            this.square = square;
            this.horizontal = horizontal;
            this.vertical = vertical;
            this.tokenColor = tokenColor;
        }

        private void dispose() {
            square.dispose();
            horizontal.dispose();
            vertical.dispose();
        }
    }

    private boolean isLeftSideCell(int cellIndex) {
        return cellIndex >= 11 && cellIndex <= 19;
    }

    private boolean isRightSideCell(int cellIndex) {
        return cellIndex >= 31 && cellIndex <= 39;
    }
}

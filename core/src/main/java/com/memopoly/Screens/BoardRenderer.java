package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.memopoly.Memopoly;
import com.memopoly.utils.TexturePathResolver;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;

import java.util.*;

/**
 * Отрисовщик игрового поля: рисует карту, рамки владения, фишки игроков и анимации перемещения на поле.
 */
public class BoardRenderer {
    private static final String ACTIVE_OUTLINE_SQUARE_TEXTURE_PATH = "green(1x1).png";
    private static final String ACTIVE_OUTLINE_HORIZONTAL_TEXTURE_PATH = "green(horizontal).png";
    private static final String ACTIVE_OUTLINE_VERTICAL_TEXTURE_PATH = "green(vertical).png";
    private static final String MORTGAGE_CELL_HORIZONTAL_TEXTURE_PATH = "mortgage_cell_horizontal.png";
    private static final String MORTGAGE_CELL_VERTICAL_TEXTURE_PATH = "mortgage_cell_vertical.png";
    private static final float WORLD_WIDTH = 1920f;
    private static final float WORLD_HEIGHT = 1080f;
    private static final float BOARD_TOP_BOTTOM_MARGIN = 0f;
    private static final float BOARD_SIZE = WORLD_HEIGHT;
    private static final float MORTGAGE_OVERLAY_SCALE = 1.5f;
    private static final float[][] CELL_LAYOUT = createCellLayout();

    private final Memopoly game;
    private final Texture boardTexture;
    private final Texture activeOutlineSquareTexture;
    private final Texture activeOutlineHorizontalTexture;
    private final Texture activeOutlineVerticalTexture;
    private final Texture mortgageCellHorizontalTexture;
    private final Texture mortgageCellVerticalTexture;
    private static final float STRIP_THICKNESS = 0.016f;// толщина плашки, ОДИНАКОВАЯ для всех рядов
    private static final float CORNER_W = 0.130f; // ширина угловой клетки
    private static final float CORNER_H = 0.132f;
    private static final float TRAP_SLANT_INSET = 0.004f;
    private static final float RING_H = 0.128f;
    private static final float RING_W = 0.128f;

    private static final float[][] STRIP_LAYOUT = createStripLayout();

    private static float[][] createStripLayout() {
        float[][] s = new float[40][4];
        float t = STRIP_THICKNESS;

        for (int idx = 0; idx < 40; idx++) {
            float[] c = CELL_LAYOUT[idx];
            if (idx >= 1 && idx <= 9) {          // низ: ячейка НАД клеткой
                s[idx] = rect(c[0], c[1] + c[3], c[2], t);
            } else if (idx >= 21 && idx <= 29) { // верх: ячейка ПОД клеткой
                s[idx] = rect(c[0], c[1] - t, c[2], t);
            } else if (idx >= 11 && idx <= 19) { // лево: ячейка СПРАВА от клетки
                s[idx] = rect(c[0] + c[2], c[1], t, c[3]);
            } else if (idx >= 31 && idx <= 39) { // право: ячейка СЛЕВА от клетки
                s[idx] = rect(c[0] - t, c[1], t, c[3]);
            }
        }
        return s;
    }

    private Rectangle getTrapezoidBounds(int cellIndex, Rectangle s) {
        float i = boardBounds.width * TRAP_SLANT_INSET;
        switch (cellIndex) {
            case 1:  return new Rectangle(s.x, s.y, s.width - i, s.height);      // скос справа, левый край впритык
            case 9:  return new Rectangle(s.x + i, s.y, s.width - i, s.height);  // скос слева, правый впритык
            case 21: return new Rectangle(s.x + i, s.y, s.width - i, s.height);  // скос слева
            case 29: return new Rectangle(s.x, s.y, s.width - i, s.height);      // скос справа
            case 11: return new Rectangle(s.x, s.y + i, s.width, s.height - i);  // скос снизу, верх впритык
            case 19: return new Rectangle(s.x, s.y, s.width, s.height - i);      // скос сверху
            case 31: return new Rectangle(s.x, s.y, s.width, s.height - i);      // скос сверху
            case 39: return new Rectangle(s.x, s.y + i, s.width, s.height - i);  // скос снизу
            default: return s;
        }
    }

    private boolean debugGrid = false;

    private void renderDebugGrid() {
        if (!debugGrid) return;
        shapeRenderer.setColor(1f, 0f, 0f, 1f);          // клетки — красным
        for (int i = 0; i < 40; i++) {
            Rectangle c = getCellBounds(i);
            shapeRenderer.rect(c.x, c.y, c.width, c.height);
        }
        shapeRenderer.setColor(0f, 1f, 0f, 1f);          // ячейки плашек — зелёным
        for (int i = 0; i < 40; i++) {
            Rectangle s = getOwnerStripBounds(i);
            shapeRenderer.rect(s.x, s.y, s.width, s.height);
        }
    }

    private final PlaqueSet[] playerPlaqueSets;
    private final ShapeRenderer shapeRenderer;
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Rectangle boardBounds;
    private Texture battleBoardTexture;
    private boolean battleMode;

    // ===== Система анимации перемещения фишек =====
    private final Map<Integer, MovementAnimation> activeAnimations = new HashMap<>();

    private static class MovementAnimation {
        final int playerId;
        final List<Integer> path; // последовательность клеток от старта до финиша
        int currentIndex;         // текущий индекс в path
        float timer;
        final float stepDelay;    // время на одну клетку (секунды)

        MovementAnimation(int playerId, List<Integer> path, float stepDelay) {
            this.playerId = playerId;
            this.path = path;
            this.currentIndex = 0;
            this.timer = 0;
            this.stepDelay = stepDelay;
        }

        int getCurrentCell() {
            return path.get(Math.min(currentIndex, path.size() - 1));
        }

        boolean isFinished() {
            return currentIndex >= path.size() - 1;
        }
    }

    /**
     * Запускает пошаговую анимацию перемещения фишки по доске.
     * @param playerId ID игрока
     * @param fromCell начальная клетка
     * @param toCell конечная клетка
     */
    public void animateMovement(int playerId, int fromCell, int toCell) {
        // Строим путь по часовой стрелке от fromCell до toCell
        List<Integer> path = new ArrayList<>();
        int pos = fromCell;
        path.add(pos);
        int safety = 0;
        while (pos != toCell && safety < 40) {
            pos = (pos + 1) % 40;
            path.add(pos);
            safety++;
        }
        if (path.size() <= 1) return; // уже на месте
        activeAnimations.put(playerId, new MovementAnimation(playerId, path, 0.24f));
    }

    /**
     * Вызывается каждый кадр для обновления активных анимаций.
     */
    public void update(float delta) {
        Iterator<Map.Entry<Integer, MovementAnimation>> it = activeAnimations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, MovementAnimation> entry = it.next();
            MovementAnimation anim = entry.getValue();
            anim.timer += delta;
            while (anim.timer >= anim.stepDelay && !anim.isFinished()) {
                anim.timer -= anim.stepDelay;
                anim.currentIndex++;
            }
            if (anim.isFinished()) {
                it.remove();
            }
        }
    }

    /**
     * Возвращает анимируемую клетку игрока, если есть активная анимация, иначе fallback.
     */
    public int getAnimatedCell(int playerId, int fallbackCell) {
        MovementAnimation anim = activeAnimations.get(playerId);
        if (anim != null && !anim.isFinished()) {
            return anim.getCurrentCell();
        }
        return fallbackCell;
    }

    /**
     * Проверяет, анимируется ли сейчас фишка игрока.
     */
    public boolean isAnimating(int playerId) {
        MovementAnimation anim = activeAnimations.get(playerId);
        return anim != null && !anim.isFinished();
    }

    public BoardRenderer(Memopoly game) {
        this.game = game;
        boardTexture = new Texture(TexturePathResolver.resolveBoardMapTexture(game.getLanguageManager().getLanguage()));
        boardTexture.setFilter(Texture.TextureFilter.Nearest, Texture.TextureFilter.Nearest);
        battleBoardTexture = loadTextureIfExists(TexturePathResolver.resolveBattleBoardMapTexture(game.getLanguageManager().getLanguage()));
        activeOutlineSquareTexture = loadTexture(ACTIVE_OUTLINE_SQUARE_TEXTURE_PATH);
        activeOutlineHorizontalTexture = loadTexture(ACTIVE_OUTLINE_HORIZONTAL_TEXTURE_PATH);
        activeOutlineVerticalTexture = loadTexture(ACTIVE_OUTLINE_VERTICAL_TEXTURE_PATH);
        mortgageCellHorizontalTexture = loadTexture(MORTGAGE_CELL_HORIZONTAL_TEXTURE_PATH);
        mortgageCellVerticalTexture = loadTexture(MORTGAGE_CELL_VERTICAL_TEXTURE_PATH);
        playerPlaqueSets = new PlaqueSet[] {
            createPlaqueSet("red", new Color(0.95f, 0.34f, 0.28f, 1f)),
            createPlaqueSet("blue", new Color(0.23f, 0.70f, 0.98f, 1f)),
            createPlaqueSet("yellow", new Color(1.00f, 0.84f, 0.31f, 1f)),
            createPlaqueSet("orange", new Color(1.00f, 0.57f, 0.16f, 1f)),
            createPlaqueSet("purple", new Color(0.77f, 0.48f, 0.98f, 1f))
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
        Texture map = (battleMode && battleBoardTexture != null) ? battleBoardTexture : boardTexture;
        batch.draw(map, boardBounds.x, boardBounds.y, boardBounds.width, boardBounds.height);
        renderOwnedCells(batch, boardCells, gameState);
        renderMortgagedCells(batch, boardCells, gameState);
        renderCurrentCell(batch, gameState);
        batch.end();

        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        renderDebugGrid();
        shapeRenderer.end();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        renderPlayers(gameState);
        shapeRenderer.end();
    }

    private void renderCurrentCell(SpriteBatch batch, GameState gameState) {
        if (gameState == null || gameState.getCurrentPlayer() == null) {
            return;
        }
        Player current = gameState.getCurrentPlayer();
        // Если фишка текущего игрока анимируется — рамка движется вместе с ней
        int currentCellIndex = getAnimatedCell(current.id, current.position);
        Rectangle bounds = getCellBounds(currentCellIndex);
        drawOutlineTexture(batch, getActiveOutlineTexture(currentCellIndex), bounds, 0.96f);
    }
    public void setBattleMode(boolean enabled) {
        this.battleMode = enabled;
    }

    private Texture loadTextureIfExists(String path) {
        return Gdx.files.internal(path).exists() ? loadTexture(path) : null;
    }

    private void renderOwnedCells(SpriteBatch batch, List<BoardCell> boardCells, GameState gameState) {
        if (gameState == null || gameState.cellOwners == null) {
            return;
        }

        for (BoardCell cell : boardCells) {
            Integer ownerId = gameState.cellOwners.get(cell.id);
            if (ownerId == null || isCornerCell(cell.id)) {
                continue; // угловые клетки — плашки не рисуем
            }

            Rectangle strip = getOwnerStripBounds(cell.id);
            if (isCornerAdjacentCell(cell.id)) {
                strip = getTrapezoidBounds(cell.id, strip);
            }

            PlaqueSet set = playerPlaqueSets[Math.abs(ownerId) % playerPlaqueSets.length];
            float rotation = getSideRotation(cell.id);

            TextureRegion region;
            if (isCornerAdjacentCell(cell.id)) {
                region = needsFlip(cell.id) ? set.trapFlipped : set.trap;
            } else {
                region = set.plaque;
            }
            drawPlaque(batch, region, strip, rotation, 0.90f);
        }
    }

    // Полоса у внутреннего края клетки (к центру поля)
    private Rectangle getOwnerStripBounds(int cellIndex) {
        float[] r = STRIP_LAYOUT[cellIndex];
        return new Rectangle(
            boardBounds.x + boardBounds.width * r[0],
            boardBounds.y + boardBounds.height * r[1],
            boardBounds.width * r[2],
            boardBounds.height * r[3]
        );
    }

    // Поворот по стороне: низ 0, лево 90, верх 180, право 270
// (если на тесте плашка смотрит не внутрь — поменяй знак у 90/270, это одна таблица)
    private float getSideRotation(int cellIndex) {
        if (cellIndex >= 11 && cellIndex <= 19) return 270f; // левый ряд (было 90, +180)
        if (cellIndex >= 21 && cellIndex <= 29) return 180f; // верхний ряд
        if (cellIndex >= 31 && cellIndex <= 39) return 90f;  // правый ряд (было 270, +180)
        return 0f;                                           // нижний ряд
    }

    private boolean isCornerAdjacentCell(int i) {
        return i == 1 || i == 9 || i == 11 || i == 19 || i == 21 || i == 29 || i == 31 || i == 39;
    }

    private boolean needsFlip(int i) {
        return i == 1 || i == 11 || i == 21 || i == 31;
    }

    // Рисует регион, повёрнутый вокруг центра целевой полосы
    private void drawPlaque(SpriteBatch batch, TextureRegion region, Rectangle strip, float rotationDeg, float alpha) {
        float cx = strip.x + strip.width / 2f;
        float cy = strip.y + strip.height / 2f;
        boolean quarterTurn = (Math.round(rotationDeg / 90f) % 2) != 0;
        float w = quarterTurn ? strip.height : strip.width;
        float h = quarterTurn ? strip.width : strip.height;

        Color prev = batch.getColor();
        batch.setColor(1f, 1f, 1f, alpha);
        batch.draw(region, cx - w / 2f, cy - h / 2f, w / 2f, h / 2f, w, h, 1f, 1f, rotationDeg);
        batch.setColor(prev);
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
            int effectivePos = getAnimatedCell(player.id, player.position);
            playersByPosition.computeIfAbsent(effectivePos, key -> new ArrayList<>()).add(player);
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
        return playerPlaqueSets[Math.abs(playerId) % playerPlaqueSets.length].tokenColor;
    }

    public void dispose() {
        if (battleBoardTexture != null) battleBoardTexture.dispose();
        boardTexture.dispose();
        activeOutlineSquareTexture.dispose();
        activeOutlineHorizontalTexture.dispose();
        activeOutlineVerticalTexture.dispose();
        mortgageCellHorizontalTexture.dispose();
        mortgageCellVerticalTexture.dispose();
        for (PlaqueSet plaqueSet : playerPlaqueSets) {
            plaqueSet.dispose();
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

    private static float[][] createCellLayout() {
        float[][] layout = new float[40][4];

        // Угловые клетки — производные от размеров кольца
        layout[0]  = rect(1f - RING_W, 0f, RING_W, RING_H);
        layout[10] = rect(0f, 0f, RING_W, RING_H);
        layout[20] = rect(0f, 1f - RING_H, RING_W, RING_H);
        layout[30] = rect(1f - RING_W, 1f - RING_H, RING_W, RING_H);

        // Общие границы внутренних клеток: делением, соседние делят одну линию
        float hx0 = RING_W;          // левая граница (от углов 10/20)
        float hx1 = 1f - RING_W;     // правая граница (до углов 0/30)
        float cw = (hx1 - hx0) / 9f;

        float vy0 = RING_H;          // нижняя граница (от углов 10/0)
        float vy1 = 1f - RING_H;     // верхняя граница (до углов 20/30)
        float ch = (vy1 - vy0) / 9f;

        for (int i = 1; i <= 9; i++) {        // низ, клетка 1 справа
            layout[i] = rect(hx1 - cw * i, 0f, cw, RING_H);
        }
        for (int i = 11; i <= 19; i++) {      // лево, клетка 11 снизу
            layout[i] = rect(0f, vy0 + ch * (i - 11), RING_W, ch);
        }
        for (int i = 21; i <= 29; i++) {      // верх, клетка 21 слева
            layout[i] = rect(hx0 + cw * (i - 21), 1f - RING_H, cw, RING_H);
        }
        for (int i = 31; i <= 39; i++) {      // право, клетка 31 сверху
            layout[i] = rect(1f - RING_W, vy1 - ch * (i - 31 + 1), RING_W, ch);
        }

        return layout;
    }
    private static float[] rect(float x, float y, float width, float height) {
        return new float[] {x, y, width, height};
    }

    private static class PlaqueSet {
        private final Texture plaqueTexture;
        private final Texture trapTexture;
        private final TextureRegion plaque;
        private final TextureRegion trap;
        private final TextureRegion trapFlipped;
        private final Color tokenColor;

        private PlaqueSet(Texture plaqueTexture, Texture trapTexture, Color tokenColor) {
            this.plaqueTexture = plaqueTexture;
            this.trapTexture = trapTexture;
            this.plaque = new TextureRegion(plaqueTexture);
            this.trap = new TextureRegion(trapTexture);
            this.trapFlipped = new TextureRegion(trapTexture);
            this.trapFlipped.flip(true, false);
            this.tokenColor = tokenColor;
        }

        private void dispose() {
            plaqueTexture.dispose();
            trapTexture.dispose();
        }
    }

    private PlaqueSet createPlaqueSet(String baseName, Color tokenColor) {
        return new PlaqueSet(
            loadTexture(baseName + ".png"),
            loadTexture(baseName + "(1x1).png"),
            tokenColor
        );
    }

    private boolean isLeftSideCell(int cellIndex) {
        return cellIndex >= 11 && cellIndex <= 19;
    }

    private boolean isRightSideCell(int cellIndex) {
        return cellIndex >= 31 && cellIndex <= 39;
    }
}

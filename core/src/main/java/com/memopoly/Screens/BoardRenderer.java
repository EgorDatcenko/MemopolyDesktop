package com.memopoly.Screens;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.Interpolation;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.memopoly.Memopoly;
import com.memopoly.game.model.Role;
import com.memopoly.utils.RoleInfo;
import com.memopoly.utils.TexturePathResolver;
import com.memopoly.game.model.BoardCell;
import com.memopoly.game.model.GameState;
import com.memopoly.game.model.Player;
import com.memopoly.utils.MonopolyUtils;
import com.memopoly.utils.UiFonts;
import com.memopoly.utils.UiShapes;

import java.util.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Отрисовщик игрового поля: рисует карту, рамки владения, фишки игроков и
 * анимации перемещения.
 */
public class BoardRenderer {
    // ==================== КОНСТАНТЫ ====================

    // Рамка ТЕКУЩЕЙ клетки (новые имена, корень assets, Linear)
    private static final String CURRENT_OUTLINE_SQUARE_TEXTURE_PATH = "current(1x1).png";
    private static final String CURRENT_OUTLINE_VERTICAL_TEXTURE_PATH = "vertical_current.png";
    private static final String CURRENT_OUTLINE_HORIZONTAL_TEXTURE_PATH = "horizont_current.png";

    // Плашки владения (ипотека)
    private static final String MORTGAGE_CELL_HORIZONTAL_TEXTURE_PATH = "mortgage_cell_horizontal.png";
    private static final String MORTGAGE_CELL_VERTICAL_TEXTURE_PATH = "mortgage_cell_vertical.png";

    private static final float WORLD_WIDTH = 1920f;
    private static final float WORLD_HEIGHT = 1080f;
    private static final float BOARD_TOP_BOTTOM_MARGIN = 0f;
    private static final float BOARD_SIZE = WORLD_HEIGHT;
    private static final float MORTGAGE_OVERLAY_SCALE = 1.5f;
    private static final float CAM_ZOOM_FOCUS = 1.25f; // было 1.35f
    private static final float CAM_ZOOM_FOLLOW = 1.22f; // было 1.30f
    // Геометрические константы сетки
    private static final float STRIP_THICKNESS = 0.016f; // толщина плашки, одинаковая для всех рядов
    private static final float CORNER_W = 0.130f;
    private static final float CORNER_H = 0.132f;
    private static final float TRAP_SLANT_INSET = 0.004f;
    private static final float RING_H = 0.128f;
    private static final float RING_W = 0.128f;

    // Предвычисленные таблицы геометрии
    private static final float[][] CELL_LAYOUT = createCellLayout();
    private static final float[][] STRIP_LAYOUT = createStripLayout();

    private int cameraState = 0; // 0=IDLE, 1=FOCUS, 2=FOLLOW
    private float zoom = 1f, targetZoom = 1f;
    private float camTargetX = WORLD_WIDTH / 2f, camTargetY = WORLD_HEIGHT / 2f;
    private int lastCurrentPlayerId = -1;
    private float holdTimer = 0f;
    private boolean cameraLocked = false;

    // ==================== ПОЛЯ ====================

    private final Memopoly game;

    // Текстуры
    private final Texture boardTexture;
    private Texture battleBoardTexture;
    private final Texture mortgageCellHorizontalTexture;
    private final Texture mortgageCellVerticalTexture;
    // ==================== НОВЫЕ ПОЛЯ/КОНСТАНТЫ (блок объявлений)
    // ====================

    // Фигурки ролей (корень assets, Linear)
    private static final String ARROW_TEXTURE_PATH = "arrow_current.png";
    private static final float TOKEN_SIZE = 52f; // одиночная фигурка
    private static final float TOKEN_SIZE_GROUP = 26f; // фигурка в группе
    private static final float SLOT_OFFSET = 14f; // шаг мини-сетки 2x2 от центра клетки
    private static final float HOP_HEIGHT = 22f; // высота хоппа
    private static final float SQUASH_DURATION = 0.12f; // длительность squash&stretch
    private static final Color SHADOW_COLOR = new Color(0.06f, 0.06f, 0.10f, 0.45f);
    private static final Color TOKEN_BASE_COLOR = new Color(0.06f, 0.06f, 0.10f, 0.85f);
    private static final Color ARROW_COLOR = new Color(1.00f, 0.83f, 0.25f, 1f);

    // Явный маппинг Role -> текстура фигурки (null = фолбэк на круглую фишку)
    private final Map<Role, Texture> roleTokenTextures = new EnumMap<>(Role.class);
    private Texture arrowTexture; // nullable: нет файла — рисуем треугольник кодом

    // Время для idle/пульса/стрелки
    private float renderTime;
    // playerId -> оставшееся время сквоша после приземления (живёт после удаления
    // анимации)
    private final Map<Integer, Float> landingSquash = new HashMap<>();

    private final List<TokenView> tokenViews = new ArrayList<>();
    private Texture greenHouseTexture;
    private Texture redHouseTexture;
    private final Map<Integer, Integer> lastHouseCounts = new HashMap<>();
    private final Map<Integer, Float> houseSpawnTime = new HashMap<>();

    public void setCameraLocked(boolean locked) {
        this.cameraLocked = locked;
    }

    /** Срез данных для отрисовки одной фишки (тень + фигурка + стрелка). */
    private static class TokenView {
        Player player;
        float x; // центр по X
        float groundY; // точка на земле (для тени)
        float y; // центр фигурки (idle + хопп)
        float size;
        float scaleX;
        float scaleY;
        float hop; // 0..1 (нормированная высота полёта)
        boolean active;
        Texture texture; // nullable
        Color color; // цвет фолбэк-фишки
    }

    // Камера и viewport
    private final OrthographicCamera camera;
    private final FitViewport viewport;
    private final Rectangle boardBounds;

    // Рендеринг
    private final ShapeRenderer shapeRenderer;
    private boolean battleMode;
    private boolean debugGrid = false;

    // Плашки владения по цвету роли
    private final PlaqueSet[] playerPlaqueSets;
    private final Map<Integer, Float> plaqueAnimTimers = new HashMap<>();
    private final Map<Integer, Integer> knownCellOwners = new HashMap<>();

    // Анимации фишек
    private final Map<Integer, MovementAnimation> activeAnimations = new HashMap<>();
    private final Map<Integer, Integer> visualPositions = new HashMap<>();

    // Режим сделки
    private Set<Integer> tradeSelectableCells = null;

    // Режим выбора ACTIONS & GIF анимации клеток
    public enum PendingActionMode {
        NONE, MORTGAGE, UNMORTGAGE, BUILD_SELL, SELL_HOUSE
    }

    private NinePatch rectMoneyPatch;
    private NinePatch longRectMoneyPatch;
    private Texture moneyIconTexture;
    private Texture houseIconTexture;
    private BitmapFont plaqueFont;

    private PendingActionMode pendingActionMode = PendingActionMode.NONE;
    private int localPlayerId = -1;
    private Map<Integer, com.badlogic.gdx.graphics.g2d.Animation<TextureRegion>> cellAnimations = new HashMap<>();
    private float boardAnimTime = 0f;

    public void setPendingActionMode(PendingActionMode mode, int localPlayerId) {
        this.pendingActionMode = mode != null ? mode : PendingActionMode.NONE;
        this.localPlayerId = localPlayerId;
    }

    public void setCellAnimations(Map<Integer, com.badlogic.gdx.graphics.g2d.Animation<TextureRegion>> cellAnimations) {
        this.cellAnimations = cellAnimations;
    }

    public Map<Integer, com.badlogic.gdx.graphics.g2d.Animation<TextureRegion>> getCellAnimations() {
        return cellAnimations;
    }

    // ==================== ВНУТРЕННИЕ КЛАССЫ ====================

    /** Пошаговая анимация перемещения фишки по доске. */
    private static class MovementAnimation {
        final int playerId;
        final List<Integer> path;
        int currentIndex;
        float timer;
        final float stepDelay;

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
     * Набор текстур плашки владения для одного цвета роли.
     * Хранит TextureRegion для возможности поворота через SpriteBatch.draw(...) и
     * tintColor (null = своя текстура, без тонировки; != null = тонировать region).
     */
    private static class PlaqueSet {
        final Texture[] ownedTextures; // диспозим только свои текстуры
        final TextureRegion plaqueRegion;
        final TextureRegion trapRegion;
        final TextureRegion trapFlippedRegion;
        final TextureRegion verticalStripRegion; // vertical_{color}.png, nullable
        final TextureRegion horizontalStripRegion; // horizont_{color}.png, nullable
        final Color tintColor; // null = не тонировать
        final Color tokenColor; // для фишек/отладки

        PlaqueSet(Texture[] ownedTextures,
                TextureRegion plaqueRegion,
                TextureRegion trapRegion,
                TextureRegion trapFlippedRegion,
                TextureRegion verticalStripRegion,
                TextureRegion horizontalStripRegion,
                Color tintColor,
                Color tokenColor) {
            this.ownedTextures = ownedTextures;
            this.plaqueRegion = plaqueRegion;
            this.trapRegion = trapRegion;
            this.trapFlippedRegion = trapFlippedRegion;
            this.verticalStripRegion = verticalStripRegion;
            this.horizontalStripRegion = horizontalStripRegion;
            this.tintColor = tintColor;
            this.tokenColor = tokenColor;
        }

        void dispose() {
            for (Texture t : ownedTextures) {
                t.dispose();
            }
        }
    }

    // ==================== КОНСТРУКТОР ====================

    public BoardRenderer(Memopoly game) {
        this.game = game;

        boardTexture = new Texture(
                Gdx.files
                        .internal(TexturePathResolver.resolveBoardMapTexture(game.getLanguageManager().getLanguage())));
        boardTexture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);

        battleBoardTexture = loadTextureIfExistsMipMap(
                TexturePathResolver.resolveBattleBoardMapTexture(game.getLanguageManager().getLanguage()));
        greenHouseTexture = loadTextureIfExistsLinear("green_house.png");
        redHouseTexture = loadTextureIfExistsLinear("red_house.png");
        mortgageCellHorizontalTexture = loadTexture(MORTGAGE_CELL_HORIZONTAL_TEXTURE_PATH);
        mortgageCellVerticalTexture = loadTexture(MORTGAGE_CELL_VERTICAL_TEXTURE_PATH);

        Texture rmt = loadTextureIfExistsLinear("rect_for_money.png");
        if (rmt != null) {
            rectMoneyPatch = new NinePatch(rmt, 224, 224, 0, 0);
            float plaqueHeight = 19f;
            float patchScale = plaqueHeight / (float) rmt.getHeight();
            rectMoneyPatch.scale(patchScale, patchScale);
        }
        Texture lrmt = loadTextureIfExistsLinear("long_rect_for_money.png");
        if (lrmt != null) {
            longRectMoneyPatch = new NinePatch(lrmt, 266, 266, 0, 0);
            float plaqueHeight = 19f;
            float patchScale = plaqueHeight / (float) lrmt.getHeight();
            longRectMoneyPatch.scale(patchScale, patchScale);
        }
        moneyIconTexture = loadTextureIfExistsLinear("money.png");
        houseIconTexture = loadTextureIfExistsLinear("build_branch.png");
        if (Gdx.files.internal("fonts_ru/Rubik-Bold.ttf").exists()) {
            plaqueFont = UiFonts.create("fonts_ru/Rubik-Bold.ttf", 14);
        }

        playerPlaqueSets = new PlaqueSet[] {
                createPlaqueSet("red", RoleInfo.color(Role.MODERATOR)),
                createPlaqueSet("blue", RoleInfo.color(Role.SMM)),
                createPlaqueSet("yellow", RoleInfo.color(Role.SCAMMER)),
                createPlaqueSet("orange", RoleInfo.color(Role.MONOPOLIST)),
                createPlaqueSet("purple", RoleInfo.color(Role.MEMOLOG)),
                createPlaqueSetWithFallback("green", "blue", RoleInfo.color(Role.DOGE))
        };

        roleTokenTextures.put(Role.DOGE, loadTextureIfExistsLinear("token_doge.png"));
        roleTokenTextures.put(Role.SCAMMER, loadTextureIfExistsLinear("token_scammer.png"));
        roleTokenTextures.put(Role.MONOPOLIST, loadTextureIfExistsLinear("token_monopolist.png"));
        roleTokenTextures.put(Role.SMM, loadTextureIfExistsLinear("token_smm.png"));
        roleTokenTextures.put(Role.MEMOLOG, loadTextureIfExistsLinear("token_memolog.png"));
        roleTokenTextures.put(Role.MODERATOR, loadTextureIfExistsLinear("token_moderator.png"));
        arrowTexture = loadTextureIfExistsLinear(ARROW_TEXTURE_PATH);

        camera = new OrthographicCamera();
        viewport = new FitViewport(WORLD_WIDTH, WORLD_HEIGHT, camera);
        viewport.apply();

        shapeRenderer = new ShapeRenderer();
        boardBounds = new Rectangle(
                (WORLD_WIDTH - BOARD_SIZE) / 2f,
                BOARD_TOP_BOTTOM_MARGIN,
                BOARD_SIZE,
                BOARD_SIZE);
    }

    private Texture loadTextureIfExistsMipMap(String path) {
        if (!Gdx.files.internal(path).exists()) {
            return null;
        }
        Texture texture = new Texture(Gdx.files.internal(path));
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    public void updateCamera(float delta, GameState state) {
        Player current = state == null ? null : state.getCurrentPlayer();
        boolean blocked = cameraLocked || state == null || current == null
                || state.currentPhase != GameState.GamePhase.PLAYING
                || state.isInBattle || state.isInAuction;

        if (blocked) {
            cameraState = 0;
            targetZoom = 1f;
            camTargetX = WORLD_WIDTH / 2f;
            camTargetY = WORLD_HEIGHT / 2f;
        } else {
            if (current.id != lastCurrentPlayerId) {
                lastCurrentPlayerId = current.id;
                cameraState = 1;
                targetZoom = CAM_ZOOM_FOCUS;
                holdTimer = 0f;
            }
            int cellIndex = getAnimatedCell(current.id, current.position);
            Rectangle bounds = getCellBounds(cellIndex);
            float tx = bounds.x + bounds.width * 0.5f;
            float ty = bounds.y + bounds.height * 0.5f;

            if (isAnimating(current.id)) {
                cameraState = 2;
                targetZoom = CAM_ZOOM_FOLLOW;
                holdTimer = 0.7f;
            }

            if (cameraState == 2) {
                camTargetX = tx;
                camTargetY = WORLD_HEIGHT / 2f + (ty - WORLD_HEIGHT / 2f) * 0.4f;
                if (!isAnimating(current.id)) {
                    holdTimer -= delta;
                    if (holdTimer <= 0f) {
                        cameraState = 0;
                        targetZoom = 1f;
                        camTargetX = WORLD_WIDTH / 2f;
                        camTargetY = WORLD_HEIGHT / 2f;
                    }
                }
            } else if (cameraState == 1) {
                camTargetX = tx;
                camTargetY = WORLD_HEIGHT / 2f + (ty - WORLD_HEIGHT / 2f) * 0.4f;
            }
        }

        float kz = 1f - (float) Math.exp(-5f * delta);
        zoom += (targetZoom - zoom) * kz;
        float kp = 1f - (float) Math.exp(-4f * delta);
        camera.position.x += (camTargetX - camera.position.x) * kp;
        camera.position.y += (camTargetY - camera.position.y) * kp;

        zoom = Math.max(1f, Math.min(1.3f, zoom));

        float dx = camTargetX - camera.position.x;
        float dy = camTargetY - camera.position.y;
        if (Math.abs(zoom - 1f) < 0.01f && (dx * dx + dy * dy) < 4f) {
            zoom = 1f;
            camera.position.x = WORLD_WIDTH / 2f;
            camera.position.y = WORLD_HEIGHT / 2f;
        }

        camera.zoom = 1f / zoom;
        clampCameraPosition();
    }

    // ==================== ПУБЛИЧНЫЕ МЕТОДЫ ====================

    public void setTradeDimming(Set<Integer> selectable) {
        this.tradeSelectableCells = selectable;
    }

    public int getCellAt(float worldX, float worldY) {
        for (int i = 0; i < 40; i++) {
            Rectangle r = getCellBounds(i);
            if (r.contains(worldX, worldY))
                return i;
        }
        return -1;
    }

    public void animateMovement(int playerId, int fromCell, int toCell) {
        animateMovement(playerId, fromCell, toCell, false);
    }

    public void animateMovement(int playerId, int fromCell, int toCell, boolean counterClockwise) {
        List<Integer> path = new ArrayList<>();
        int pos = fromCell;
        path.add(pos);
        if (counterClockwise) {
            int safety = 0;
            while (pos != toCell && safety < 40) {
                pos = (pos + 39) % 40;
                path.add(pos);
                safety++;
            }
        } else {
            int steps = (toCell - fromCell + 40) % 40;
            for (int i = 1; i <= steps; i++) {
                path.add((fromCell + i) % 40);
            }
        }
        if (path.size() <= 1)
            return;
        visualPositions.put(playerId, fromCell);
        activeAnimations.put(playerId, new MovementAnimation(playerId, path, 0.24f));
    }

    public interface MovementStepListener {
        void onStep(int playerId, int cellIndex);
    }
    private MovementStepListener movementStepListener;

    public void setMovementStepListener(MovementStepListener listener) {
        this.movementStepListener = listener;
    }

    public void update(float delta) {
        renderTime += delta;
        boardAnimTime += delta;

        Iterator<Map.Entry<Integer, MovementAnimation>> it = activeAnimations.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Integer, MovementAnimation> entry = it.next();
            MovementAnimation anim = entry.getValue();
            anim.timer += delta;
            boolean stepped = false;
            while (anim.timer >= anim.stepDelay && !anim.isFinished()) {
                anim.timer -= anim.stepDelay;
                anim.currentIndex++;
                stepped = true;
                if (movementStepListener != null) {
                    movementStepListener.onStep(anim.playerId, anim.getCurrentCell());
                }
            }
            // Приземление на клетку: запускаем squash&stretch.
            // Срабатывает и на финальном шаге — таймер живёт отдельно от анимации.
            if (stepped) {
                landingSquash.put(anim.playerId, SQUASH_DURATION);
            }
            if (anim.isFinished()) {
                visualPositions.put(anim.playerId, anim.getCurrentCell());
                it.remove();
            }
        }

        Iterator<Map.Entry<Integer, Float>> sq = landingSquash.entrySet().iterator();
        while (sq.hasNext()) {
            Map.Entry<Integer, Float> e = sq.next();
            float v = e.getValue() - delta;
            if (v <= 0f)
                sq.remove();
            else
                e.setValue(v);
        }
    }

    public boolean hasActiveAnimations() {
        return !activeAnimations.isEmpty() || !landingSquash.isEmpty();
    }

    /**
     * Клампы позиции камеры под zoom >= 1; при zoom == 1 видим весь мир, позиция —
     * точный центр.
     */
    private void clampCameraPosition() {
        float halfW = (WORLD_WIDTH / zoom) / 2f;
        float halfH = (WORLD_HEIGHT / zoom) / 2f;
        if (2f * halfW <= WORLD_WIDTH) {
            camera.position.x = Math.max(halfW, Math.min(WORLD_WIDTH - halfW, camera.position.x));
        } else {
            camera.position.x = WORLD_WIDTH / 2f;
        }
        if (2f * halfH <= WORLD_HEIGHT) {
            camera.position.y = Math.max(halfH, Math.min(WORLD_HEIGHT - halfH, camera.position.y));
        } else {
            camera.position.y = WORLD_HEIGHT / 2f;
        }
    }

    public void setVisualPosition(int playerId, int cellIndex) {
        visualPositions.put(playerId, cellIndex);
    }

    public void syncVisualPosition(int playerId, int serverPosition, boolean movementActive) {
        if (!visualPositions.containsKey(playerId)) {
            visualPositions.put(playerId, serverPosition);
        } else if (!movementActive && !isAnimating(playerId)) {
            visualPositions.put(playerId, serverPosition);
        }
    }

    public int getVisualPosition(int playerId, int fallbackCell) {
        return visualPositions.getOrDefault(playerId, fallbackCell);
    }

    public int getAnimatedCell(int playerId, int fallbackCell) {
        MovementAnimation anim = activeAnimations.get(playerId);
        if (anim != null && !anim.isFinished()) {
            return anim.getCurrentCell();
        }
        return visualPositions.getOrDefault(playerId, fallbackCell);
    }

    public boolean isAnimating(int playerId) {
        MovementAnimation anim = activeAnimations.get(playerId);
        return anim != null && !anim.isFinished();
    }

    public boolean isMovementActive() {
        return hasActiveAnimations();
    }

    public void setBattleMode(boolean enabled) {
        this.battleMode = enabled;
    }

    public FitViewport getViewport() {
        return viewport;
    }

    public OrthographicCamera getCamera() {
        return camera;
    }

    public Vector2 screenToWorld(float screenX, float screenY) {
        com.badlogic.gdx.math.Vector3 v = viewport.unproject(new com.badlogic.gdx.math.Vector3(screenX, screenY, 0));
        return new Vector2(v.x, v.y);
    }

    public Vector2 worldToStageCoordinates(float worldX, float worldY, com.badlogic.gdx.scenes.scene2d.Stage stage) {
        com.badlogic.gdx.math.Vector3 v = viewport.project(new com.badlogic.gdx.math.Vector3(worldX, worldY, 0));
        Vector2 screen = new Vector2(v.x, Gdx.graphics.getHeight() - v.y);
        return stage.screenToStageCoordinates(screen);
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
        renderMortgageOverlays(batch, boardCells, gameState);
        renderCellAnimations(batch);
        renderHouses(batch, gameState);
        renderCellRentBadges(batch, boardCells, gameState);
        batch.end();
        shapeRenderer.setProjectionMatrix(camera.combined);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
        renderTradeDimming();
        renderPendingActionDimming(boardCells, gameState);
        shapeRenderer.end();
        computeTokenViews(gameState);
        batch.begin();
        renderPlayerTokens(batch);

        if (battleMode && battleBoardTexture != null) {
            batch.setColor(Color.WHITE);
            batch.draw(battleBoardTexture, boardBounds.x, boardBounds.y, boardBounds.width, boardBounds.height);
        }
        batch.end();
        if (arrowTexture == null) {
            shapeRenderer.begin(ShapeRenderer.ShapeType.Filled);
            renderTurnArrowTriangle();
            shapeRenderer.end();
        }
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        renderDebugGrid();
        shapeRenderer.end();
    }

    private void computeTokenViews(GameState gameState) {
        tokenViews.clear();
        if (gameState == null || gameState.players == null)
            return;
        Player current = gameState.getCurrentPlayer();
        Map<Integer, List<Player>> byPosition = new HashMap<>();
        for (Player player : gameState.players) {
            int pos = getAnimatedCell(player.id, player.position);
            byPosition.computeIfAbsent(pos, key -> new ArrayList<>()).add(player);
        }
        for (Player player : gameState.players) {
            int pos = getAnimatedCell(player.id, player.position);
            List<Player> group = byPosition.get(pos);
            int idx = group.indexOf(player) % 4;
            Rectangle bounds = getCellBounds(pos);
            float cx = bounds.x + bounds.width * 0.5f;
            float cy = bounds.y + bounds.height * 0.5f;
            float size = TOKEN_SIZE;
            float offset = 0f;
            int groupSize = group.size();
            if (groupSize == 2) {
                size = 44f;
                offset = 16f;
            } else if (groupSize == 3) {
                size = 40f;
                offset = 15f;
            } else if (groupSize >= 4) {
                size = 36f;
                offset = 14f;
            }
            if (groupSize > 1) {
                cx += (idx % 2 == 0 ? -offset : offset);
                cy += (idx < 2 ? offset : -offset);
            }
            TokenView view = new TokenView();
            view.player = player;
            view.x = cx;
            view.groundY = cy;
            view.size = size;
            float idleY = (float) Math.sin(renderTime * 3f + player.id) * 2.5f;
            MovementAnimation anim = activeAnimations.get(player.id);
            if (anim != null && !anim.isFinished()) {
                float progress = Math.min(anim.timer / anim.stepDelay, 1f);
                view.hop = (float) Math.sin(progress * Math.PI);
            }
            Float squash = landingSquash.get(player.id);
            float k = squash == null ? 0f : Math.max(0f, squash / SQUASH_DURATION);
            view.scaleX = 1f + 0.15f * k;
            view.scaleY = 1f - 0.15f * k;
            view.active = current != null && current.id == player.id;
            if (view.active) {
                float pulse = 1f + 0.06f * (float) Math.sin(renderTime * 6f);
                view.scaleX *= pulse;
                view.scaleY *= pulse;
            }
            view.y = cy + idleY + view.hop * HOP_HEIGHT;
            Role role = Role.of(gameState.playerRoles == null ? null : gameState.playerRoles.get(player.id));
            if (role == null) {
                int slotIndex = 0;
                if (gameState.players != null) {
                    for (int s = 0; s < gameState.players.size(); s++) {
                        if (gameState.players.get(s).id == player.id) {
                            slotIndex = s;
                            break;
                        }
                    }
                }
                role = NO_ROLE_FALLBACK_ORDER[slotIndex % 4];
            }
            view.texture = roleTokenTextures.get(role);
            view.color = getPlayerColor(player, gameState);
            tokenViews.add(view);
        }
        tokenViews.sort((a, b) -> Boolean.compare(a.active, b.active));
    }

    /**
     * Тень -> фигурка роли (или фолбэк-круги) -> стрелка активного. Всё в
     * batch-пассе.
     */
    private void renderPlayerTokens(SpriteBatch batch) {
        Texture shadowTex = UiShapes.circle(24, SHADOW_COLOR);
        Color prev = batch.getColor();
        batch.setColor(1f, 1f, 1f, 1f);

        for (TokenView view : tokenViews) {
            // 1) Тень: полупрозрачный эллипс на земле; в полёте сжимается
            float shadowScale = 1f - 0.4f * view.hop;
            float sw = view.size * 0.85f * shadowScale * view.scaleX;
            float sh = sw * 0.42f * view.scaleY;
            batch.draw(shadowTex,
                    view.x - sw / 2f, view.groundY - sh / 2f - view.size * 0.12f,
                    sw, sh);

            // 2) Фигурка роли ~52x52 (в группе 26) либо фолбэк без текстур.
            // Для Texture используем перегрузку со src-параметрами
            // (9-аргументная версия с origin/scale/rotation есть только у TextureRegion).
            if (view.texture != null) {
                float w = view.size;
                float h = view.size;
                batch.draw(view.texture,
                        view.x - w / 2f, view.y - h / 2f,
                        w / 2f, h / 2f,
                        w, h,
                        view.scaleX, view.scaleY,
                        0f,
                        0, 0, view.texture.getWidth(), view.texture.getHeight(),
                        false, false);
            } else {
                Texture base = UiShapes.circle(18, TOKEN_BASE_COLOR);
                Texture top = UiShapes.circle(14, view.color);
                float bs = view.size * 0.62f;
                float ts = view.size * 0.48f;
                batch.draw(base,
                        view.x - bs / 2f, view.y - bs / 2f,
                        bs / 2f, bs / 2f,
                        bs, bs,
                        view.scaleX, view.scaleY,
                        0f,
                        0, 0, base.getWidth(), base.getHeight(),
                        false, false);
                batch.draw(top,
                        view.x - ts / 2f, view.y - ts / 2f,
                        ts / 2f, ts / 2f,
                        ts, ts,
                        view.scaleX, view.scaleY,
                        0f,
                        0, 0, top.getWidth(), top.getHeight(),
                        false, false);
            }

            // 3) Стрелка активного (текстура): покачивание ±4px
            if (view.active && arrowTexture != null && !battleMode) {
                float bob = (float) Math.sin(renderTime * 4f) * 4f;
                float aw = 26f;
                float ah = 26f;
                batch.draw(arrowTexture, view.x - aw / 2f, view.y + view.size * 0.5f + 6f + bob, aw, ah);
            }
        }
        batch.setColor(prev);
    }

    private void renderTurnArrowTriangle() {
        if (battleMode)
            return;
        shapeRenderer.setColor(ARROW_COLOR);
        for (TokenView view : tokenViews) {
            if (!view.active)
                continue;
            float bob = (float) Math.sin(renderTime * 4f) * 4f;
            float tipY = view.y + view.size * 0.5f + 6f + bob; // остриё вниз, к фишке
            float halfW = 9f;
            float height = 12f;
            shapeRenderer.triangle(
                    view.x - halfW, tipY + height,
                    view.x + halfW, tipY + height,
                    view.x, tipY);
        }
    }

    public void resize(int width, int height) {
        viewport.update(width, height, false);
        camera.zoom = 1f / zoom;
        clampCameraPosition();
    }

    public void dispose() {
        if (battleBoardTexture != null)
            battleBoardTexture.dispose();
        boardTexture.dispose();
        mortgageCellHorizontalTexture.dispose();
        mortgageCellVerticalTexture.dispose();
        for (PlaqueSet plaqueSet : playerPlaqueSets) {
            plaqueSet.dispose();
        }
        for (Texture tokenTexture : roleTokenTextures.values()) {
            if (tokenTexture != null) {
                tokenTexture.dispose();
            }
        }
        if (arrowTexture != null) {
            arrowTexture.dispose();
        }
        shapeRenderer.dispose();
        if (greenHouseTexture != null)
            greenHouseTexture.dispose();
        if (redHouseTexture != null)
            redHouseTexture.dispose();
        if (plaqueFont != null) {
            plaqueFont.dispose();
            plaqueFont = null;
        }
    }

    // ==================== РЕНДЕРИНГ (приватные) ====================
    private void renderOwnedCells(SpriteBatch batch, List<BoardCell> boardCells, GameState gameState) {
        if (gameState == null || gameState.cellOwners == null)
            return;
        Color prev = batch.getColor();
        batch.setColor(1f, 1f, 1f, 1f);

        for (BoardCell cell : boardCells) {
            Integer ownerId = gameState.cellOwners.get(cell.id);
            Integer prevOwner = knownCellOwners.get(cell.id);
            if (ownerId != null && (prevOwner == null || !prevOwner.equals(ownerId))) {
                plaqueAnimTimers.put(cell.id, 0f);
                knownCellOwners.put(cell.id, ownerId);
            } else if (ownerId == null) {
                knownCellOwners.remove(cell.id);
                plaqueAnimTimers.remove(cell.id);
            }
        }

        for (BoardCell cell : boardCells) {
            Integer ownerId = gameState.cellOwners.get(cell.id);
            if (ownerId == null || isCornerCell(cell.id))
                continue;

            Float timer = plaqueAnimTimers.get(cell.id);
            float progress = 1f;
            if (timer != null) {
                timer += Gdx.graphics.getDeltaTime();
                if (timer >= 0.55f) {
                    plaqueAnimTimers.remove(cell.id);
                } else {
                    plaqueAnimTimers.put(cell.id, timer);
                    progress = Interpolation.pow2Out.apply(Math.min(1f, timer / 0.55f));
                }
            }

            Rectangle strip = getOwnerStripBounds(cell.id);
            if (isCornerAdjacentCell(cell.id)) {
                strip = getTrapezoidBounds(cell.id, strip);
            }

            if (progress < 1f) {
                int id = cell.id;
                if (id >= 1 && id <= 9) { // Нижний ряд: выдвигается вверх
                    strip.height *= progress;
                } else if (id >= 21 && id <= 29) { // Верхний ряд: выдвигается вниз
                    float targetH = strip.height;
                    strip.height = targetH * progress;
                    strip.y += targetH * (1f - progress);
                } else if (id >= 11 && id <= 19) { // Левый ряд: выдвигается вправо
                    strip.width *= progress;
                } else if (id >= 31 && id <= 39) { // Правый ряд: выдвигается влево
                    float targetW = strip.width;
                    strip.width = targetW * progress;
                    strip.x += targetW * (1f - progress);
                }
            }

            PlaqueSet set = getPlaqueSetFor(ownerId, gameState);
            float rotation = getSideRotation(cell.id);

            TextureRegion region;
            Color tint;
            if (isCornerAdjacentCell(cell.id)) {
                region = needsFlip(cell.id) ? set.trapFlippedRegion : set.trapRegion;
                tint = set.tintColor;
            } else if (isLeftSideCell(cell.id) || isRightSideCell(cell.id)) {
                if (set.verticalStripRegion != null) {
                    region = set.verticalStripRegion;
                    tint = null;
                    rotation = 0f;
                } else {
                    region = set.plaqueRegion;
                    tint = set.tintColor;
                }
            } else {
                if (set.horizontalStripRegion != null) {
                    region = set.horizontalStripRegion;
                    tint = null;
                    rotation = 0f;
                } else {
                    region = set.plaqueRegion;
                    tint = set.tintColor;
                }
            }
            if (region == null)
                continue; // нет ни своей, ни фолбэк-текстуры — не рисуем
            drawPlaque(batch, region, strip, rotation, 0.90f, tint);
        }
        batch.setColor(prev);
    }

    private void renderMortgageOverlays(SpriteBatch batch, List<BoardCell> boardCells, GameState gameState) {
        if (gameState == null || gameState.cellMortgaged == null)
            return;
        Color prev = batch.getColor();
        batch.setColor(1f, 1f, 1f, 1f);
        for (BoardCell cell : boardCells) {
            if (cell == null || isCornerCell(cell.id))
                continue;
            if (!gameState.cellMortgaged.getOrDefault(cell.id, false))
                continue;

            Rectangle bounds = getCellBounds(cell.id);
            Rectangle stripBounds = getOwnerStripBounds(cell.id);

            float drawX, drawY, drawWidth, drawHeight;
            int id = cell.id;
            if (id >= 1 && id <= 9) { // Нижний ряд: полоска наверху
                drawX = bounds.x;
                drawY = bounds.y;
                drawWidth = bounds.width;
                drawHeight = Math.max(1f, stripBounds.y - bounds.y);
            } else if (id >= 11 && id <= 19) { // Левый ряд: полоска справа
                drawX = bounds.x;
                drawY = bounds.y;
                drawWidth = Math.max(1f, stripBounds.x - bounds.x);
                drawHeight = bounds.height;
            } else if (id >= 21 && id <= 29) { // Верхний ряд: полоска внизу
                drawX = bounds.x;
                drawY = stripBounds.y + stripBounds.height;
                drawWidth = bounds.width;
                drawHeight = Math.max(1f, (bounds.y + bounds.height) - drawY);
            } else if (id >= 31 && id <= 39) { // Правый ряд: полоска слева
                drawX = stripBounds.x + stripBounds.width;
                drawY = bounds.y;
                drawWidth = Math.max(1f, (bounds.x + bounds.width) - drawX);
                drawHeight = bounds.height;
            } else {
                drawX = bounds.x;
                drawY = bounds.y;
                drawWidth = bounds.width;
                drawHeight = bounds.height;
            }

            Texture mortgageTexture = isHorizontalCell(id)
                    ? mortgageCellHorizontalTexture
                    : mortgageCellVerticalTexture;

            batch.draw(mortgageTexture, drawX, drawY, drawWidth, drawHeight);
        }
        batch.setColor(prev);
    }

    private void renderHouses(SpriteBatch batch, GameState gameState) {
        if (gameState == null || gameState.cellHouses == null)
            return;
        for (Map.Entry<Integer, Integer> entry : gameState.cellHouses.entrySet()) {
            int cellId = entry.getKey();
            int houses = entry.getValue() == null ? 0 : entry.getValue();
            if (cellId < 0 || cellId >= 40 || houses <= 0)
                continue;
            Integer prev = lastHouseCounts.get(cellId);
            if (prev == null || prev < houses) {
                houseSpawnTime.put(cellId, renderTime);
            } else if (prev != houses) {
                houseSpawnTime.remove(cellId);
            }
            lastHouseCounts.put(cellId, houses);
            Texture tex;
            int count;
            if (houses >= 5) {
                tex = redHouseTexture;
                count = 1;
            } else {
                tex = greenHouseTexture;
                count = houses;
            }
            if (tex == null)
                continue;
            Rectangle bounds = getCellBounds(cellId);
            float baseSize = Math.max(26f, Math.min(bounds.width, bounds.height) * 0.20f);
            float size = (houses >= 5) ? baseSize : (baseSize / 1.5f);
            float gap = size * 0.2f;
            if (!isHorizontalCell(cellId)) {
                float total = count * size + (count - 1) * gap;
                float startX = bounds.x + (bounds.width - total) / 2f;
                float y;
                if (cellId >= 1 && cellId <= 9) {
                    // Нижний ряд вертикальных клеток (1–9): приподнимаем над ценой клетки
                    y = bounds.y + bounds.height * 0.17f;
                } else if (cellId >= 21 && cellId <= 29) {
                    // Верхний ряд вертикальных клеток (21–29): смещаем выше в чистую зону
                    y = bounds.y + bounds.height * 0.16f;
                } else {
                    y = bounds.y + bounds.height * 0.06f;
                }
                for (int i = 0; i < count; i++) {
                    float s = (i == count - 1) ? spawnScale(cellId) * size : size;
                    float cx = startX + i * (size + gap) + size / 2f;
                    batch.draw(tex, cx - s / 2f, y + size / 2f - s / 2f, s, s);
                }
            } else {
                float x = isLeftSideCell(cellId)
                        ? bounds.x + bounds.width - size - bounds.width * 0.06f
                        : bounds.x + bounds.width * 0.06f;
                if (houses >= 5) {
                    float s = spawnScale(cellId) * size;
                    float cy = bounds.y + (bounds.height - size) / 2f + size / 2f;
                    batch.draw(tex, x + size / 2f - s / 2f, cy - s / 2f, s, s);
                } else {
                    float bottomY = bounds.y + bounds.height * 0.10f;
                    for (int i = 0; i < count; i++) {
                        float s = (i == count - 1) ? spawnScale(cellId) * size : size;
                        float cy = bottomY + i * (size + gap) + size / 2f;
                        batch.draw(tex, x + size / 2f - s / 2f, cy - s / 2f, s, s);
                    }
                }
            }
        }
    }

    private float spawnScale(int cellId) {
        Float t0 = houseSpawnTime.get(cellId);
        if (t0 == null)
            return 1f;
        float t = (renderTime - t0) / 0.25f;
        if (t >= 1f) {
            houseSpawnTime.remove(cellId);
            return 1f;
        }
        return Math.max(0.01f, t);
    }

    private void renderPlayers(GameState gameState) {
        if (gameState == null || gameState.players == null)
            return;

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
                shapeRenderer.setColor(new Color(0.06f, 0.06f, 0.10f, 0.85f));
                shapeRenderer.circle(position.x, position.y, tokenRadius + 3f);
                shapeRenderer.setColor(getPlayerColor(player, gameState));
                shapeRenderer.circle(position.x, position.y, tokenRadius);
            }
        }
    }

    private void renderTradeDimming() {
        if (tradeSelectableCells == null)
            return;
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setColor(0.05f, 0.05f, 0.10f, 0.40f);
        for (int i = 0; i < 40; i++) {
            if (tradeSelectableCells.contains(i))
                continue;
            Rectangle r = getCellBounds(i);
            shapeRenderer.rect(r.x, r.y, r.width, r.height);
        }
    }

    private void renderCellAnimations(SpriteBatch batch) {
        if (cellAnimations == null || cellAnimations.isEmpty())
            return;
        for (Map.Entry<Integer, com.badlogic.gdx.graphics.g2d.Animation<TextureRegion>> entry : cellAnimations
                .entrySet()) {
            int cellId = entry.getKey();
            com.badlogic.gdx.graphics.g2d.Animation<TextureRegion> anim = entry.getValue();
            if (anim != null) {
                Rectangle bounds = getCellBounds(cellId);
                TextureRegion frame = anim.getKeyFrame(boardAnimTime, true);
                batch.draw(frame, bounds.x, bounds.y, bounds.width, bounds.height);
            }
        }
    }

    private void renderPendingActionDimming(List<BoardCell> boardCells, GameState gameState) {
        if (pendingActionMode == PendingActionMode.NONE || gameState == null || localPlayerId <= 0)
            return;
        Player player = gameState.getPlayerById(localPlayerId);
        if (player == null)
            return;

        Set<Integer> validCells = new HashSet<>();
        for (int cellId : player.ownedCells) {
            boolean mortgaged = gameState.cellMortgaged.getOrDefault(cellId, false);
            int houses = gameState.cellHouses.getOrDefault(cellId, 0);
            BoardCell cell = (boardCells != null && cellId < boardCells.size()) ? boardCells.get(cellId) : null;
            if (pendingActionMode == PendingActionMode.MORTGAGE) {
                if (!mortgaged && houses == 0)
                    validCells.add(cellId);
            } else if (pendingActionMode == PendingActionMode.UNMORTGAGE) {
                if (mortgaged)
                    validCells.add(cellId);
            } else if (pendingActionMode == PendingActionMode.BUILD_SELL) {
                if (cell != null && cell.group != null
                        && MonopolyUtils.hasFullGroup(player, cell.group, boardCells)
                        && houses < 5) {
                    validCells.add(cellId);
                }
            } else if (pendingActionMode == PendingActionMode.SELL_HOUSE) {
                if (houses > 0 && cell != null && cell.group != null
                        && MonopolyUtils.hasFullGroup(player, cell.group, boardCells)) {
                    validCells.add(cellId);
                }
            }
        }

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.setColor(0.05f, 0.05f, 0.10f, 0.55f);
        for (int i = 0; i < 40; i++) {
            if (!validCells.contains(i)) {
                Rectangle r = getCellBounds(i);
                shapeRenderer.rect(r.x, r.y, r.width, r.height);
            }
        }
    }

    private void renderCellRentBadges(SpriteBatch batch, List<BoardCell> boardCells, GameState gameState) {
        if (gameState == null || gameState.cellOwners == null || boardCells == null)
            return;
        if (plaqueFont == null && Gdx.files.internal("fonts_ru/Rubik-Bold.ttf").exists()) {
            plaqueFont = UiFonts.create("fonts_ru/Rubik-Bold.ttf", 14);
        }
        if (rectMoneyPatch == null || plaqueFont == null)
            return;

        for (int cellId = 0; cellId < 40; cellId++) {
            if (isCornerCell(cellId))
                continue;
            Integer ownerId = gameState.cellOwners.get(cellId);
            if (ownerId == null || ownerId < 0)
                continue;
            if (gameState.cellMortgaged != null && Boolean.TRUE.equals(gameState.cellMortgaged.get(cellId)))
                continue;
            BoardCell cell = (cellId < boardCells.size()) ? boardCells.get(cellId) : null;
            if (cell == null || cell.getEntranceFee() == 0)
                continue;

            int rent = MonopolyUtils.calculateRent(cell, gameState, boardCells);
            String text = String.valueOf(rent);

            Rectangle bounds = getCellBounds(cellId);
            float plaqueH = 19f;
            float iconSize = 12f;

            plaqueFont.getData().setScale(0.38f);
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(plaqueFont, text);
            float glyphWidth = layout.width;
            float contentWidth = glyphWidth + iconSize + 3f;
            float plaqueW = Math.min(bounds.width - 4f, Math.max(contentWidth + 14f, 40f));

            Rectangle plaqueRect = getRentPlaqueBounds(cellId, plaqueW, plaqueH);
            float plaqueX = plaqueRect.x;
            float plaqueY = plaqueRect.y;

            batch.setColor(Color.WHITE);
            rectMoneyPatch.draw(batch, plaqueX, plaqueY, plaqueW, plaqueH);

            plaqueFont.setColor(Color.GOLD);
            float curX = plaqueX + Math.max(2f, (plaqueW - contentWidth) / 2f);
            float centerY = plaqueY + plaqueH / 2f;

            plaqueFont.draw(batch, text, curX, centerY + layout.height / 2f);
            curX += layout.width + 3f;

            if (moneyIconTexture != null) {
                batch.draw(moneyIconTexture, curX, centerY - iconSize / 2f, iconSize, iconSize);
            }
            plaqueFont.setColor(Color.WHITE);
        }
    }

    private void renderPendingActionPlaques(SpriteBatch batch, List<BoardCell> boardCells, GameState gameState) {
        if (pendingActionMode == PendingActionMode.NONE || gameState == null || localPlayerId <= 0)
            return;
        Player player = gameState.getPlayerById(localPlayerId);
        if (player == null)
            return;

        if (plaqueFont == null && Gdx.files.internal("fonts_ru/Rubik-Bold.ttf").exists()) {
            plaqueFont = UiFonts.create("fonts_ru/Rubik-Bold.ttf", 14);
        }
        if (plaqueFont == null)
            return;

        for (int cellId : player.ownedCells) {
            boolean mortgaged = gameState.cellMortgaged.getOrDefault(cellId, false);
            int houses = gameState.cellHouses.getOrDefault(cellId, 0);
            BoardCell cell = (boardCells != null && cellId < boardCells.size()) ? boardCells.get(cellId) : null;
            if (cell == null)
                continue;

            boolean isValid = false;
            String text = "";
            boolean isLongPlaque = false;

            boolean canAfford = true;
            if (pendingActionMode == PendingActionMode.MORTGAGE) {
                if (!mortgaged && houses == 0) {
                    isValid = true;
                    text = "+ " + MonopolyUtils.getMortgageValue(cell);
                } else if (mortgaged) {
                    isValid = true;
                    int unmortgageCost = MonopolyUtils.getUnmortgagePrice(cell);
                    text = "- " + unmortgageCost;
                    canAfford = player.money >= unmortgageCost;
                }
            } else if (pendingActionMode == PendingActionMode.UNMORTGAGE) {
                if (mortgaged) {
                    isValid = true;
                    int unmortgageCost = MonopolyUtils.getUnmortgagePrice(cell);
                    text = "- " + unmortgageCost;
                    canAfford = player.money >= unmortgageCost;
                }
            } else if (pendingActionMode == PendingActionMode.BUILD_SELL) {
                if (cell.group != null && MonopolyUtils.hasFullGroup(player, cell.group, boardCells) && houses < 5) {
                    isValid = true;
                    isLongPlaque = true;
                    Role role = Role.of(gameState.playerRoles != null ? gameState.playerRoles.get(player.id) : null);
                    int houseCost = MonopolyUtils.getHousePrice(cell, role);
                    text = "- " + houseCost;
                    canAfford = player.money >= houseCost;
                }
            } else if (pendingActionMode == PendingActionMode.SELL_HOUSE) {
                if (houses > 0 && cell.group != null && MonopolyUtils.hasFullGroup(player, cell.group, boardCells)) {
                    isValid = true;
                    isLongPlaque = true;
                    Role role = Role.of(gameState.playerRoles != null ? gameState.playerRoles.get(player.id) : null);
                    text = "+ " + (MonopolyUtils.getHousePrice(cell, role) / 2);
                }
            }

            if (!isValid)
                continue;

            Rectangle bounds = getCellBounds(cellId);
            Rectangle strip = getOwnerStripBounds(cellId);
            float plaqueH = 19f;
            float iconSize = 12f;
            float houseIconSize = 12f;

            float glyphWidth = 0f;
            com.badlogic.gdx.graphics.g2d.GlyphLayout layout = null;
            if (plaqueFont != null) {
                plaqueFont.getData().setScale(0.38f);
                layout = new com.badlogic.gdx.graphics.g2d.GlyphLayout(plaqueFont, text);
                glyphWidth = layout.width;
            }

            float contentWidth = glyphWidth + iconSize + 3f;
            if (isLongPlaque && houseIconTexture != null) {
                contentWidth += houseIconSize + 3f;
            }

            float plaqueW = Math.min(bounds.width - 4f, Math.max(contentWidth + 14f, 44f));
            float plaqueX, plaqueY;
            if (cellId >= 1 && cellId <= 9) { // Нижний ряд: НАД цветной полосой
                plaqueX = bounds.x + (bounds.width - plaqueW) / 2f;
                plaqueY = strip.y + strip.height + 2f;
            } else if (cellId >= 21 && cellId <= 29) { // Верхний ряд: ПОД цветной полосой
                plaqueX = bounds.x + (bounds.width - plaqueW) / 2f;
                plaqueY = strip.y - plaqueH - 2f;
            } else if (cellId >= 11 && cellId <= 19) { // Левый ряд: СПРАВА от цветной полосы
                plaqueX = strip.x + strip.width + 2f;
                plaqueY = bounds.y + (bounds.height - plaqueH) / 2f;
            } else if (cellId >= 31 && cellId <= 39) { // Правый ряд: СЛЕВА от цветной полосы
                plaqueX = strip.x - plaqueW - 2f;
                plaqueY = bounds.y + (bounds.height - plaqueH) / 2f;
            } else {
                plaqueX = bounds.x + (bounds.width - plaqueW) / 2f;
                plaqueY = bounds.y + 2f;
            }

            if (!canAfford) {
                batch.setColor(1f, 1f, 1f, 0.55f);
            }

            if (isLongPlaque && longRectMoneyPatch != null) {
                longRectMoneyPatch.draw(batch, plaqueX, plaqueY, plaqueW, plaqueH);
            } else if (rectMoneyPatch != null) {
                rectMoneyPatch.draw(batch, plaqueX, plaqueY, plaqueW, plaqueH);
            }

            if (plaqueFont != null && layout != null) {
                plaqueFont.setColor(canAfford ? Color.GOLD : new Color(0.9f, 0.4f, 0.4f, 1f));
                float curX = plaqueX + Math.max(2f, (plaqueW - contentWidth) / 2f);
                float centerY = plaqueY + plaqueH / 2f;

                if (isLongPlaque && houseIconTexture != null) {
                    batch.draw(houseIconTexture, curX, centerY - houseIconSize / 2f, houseIconSize, houseIconSize);
                    curX += houseIconSize + 3f;
                }

                plaqueFont.draw(batch, text, curX, centerY + layout.height / 2f);
                curX += layout.width + 3f;

                if (moneyIconTexture != null) {
                    batch.draw(moneyIconTexture, curX, centerY - iconSize / 2f, iconSize, iconSize);
                }
                plaqueFont.setColor(Color.WHITE);
            }
            batch.setColor(Color.WHITE);
        }
    }

    private void renderDebugGrid() {
        if (!debugGrid)
            return;
        shapeRenderer.setColor(1f, 0f, 0f, 1f);
        for (int i = 0; i < 40; i++) {
            Rectangle c = getCellBounds(i);
            shapeRenderer.rect(c.x, c.y, c.width, c.height);
        }
        shapeRenderer.setColor(0f, 1f, 0f, 1f);
        for (int i = 0; i < 40; i++) {
            Rectangle s = getOwnerStripBounds(i);
            shapeRenderer.rect(s.x, s.y, s.width, s.height);
        }
    }

    // ==================== ХЕЛПЕРЫ ОТРИСОВКИ ====================

    /**
     * Рисует TextureRegion с поворотом вокруг центра целевой полосы.
     * tintColor = null: без тонировки; != null: умножается на цвет region.
     */
    private void drawPlaque(SpriteBatch batch, TextureRegion region, Rectangle strip,
            float rotationDeg, float alpha, Color tintColor) {
        float cx = strip.x + strip.width / 2f;
        float cy = strip.y + strip.height / 2f;
        boolean quarterTurn = (Math.round(rotationDeg / 90f) % 2) != 0;
        float w = quarterTurn ? strip.height : strip.width;
        float h = quarterTurn ? strip.width : strip.height;

        Color prev = batch.getColor();
        if (tintColor != null) {
            batch.setColor(tintColor.r, tintColor.g, tintColor.b, alpha);
        } else {
            batch.setColor(1f, 1f, 1f, alpha);
        }
        batch.draw(region,
                cx - w / 2f, cy - h / 2f,
                w / 2f, h / 2f,
                w, h,
                1f, 1f,
                rotationDeg);
        batch.setColor(prev);
    }

    // ==================== ГЕОМЕТРИЯ ====================

    public Rectangle getCellBounds(int index) {
        float[] layout = CELL_LAYOUT[index];
        return new Rectangle(
                boardBounds.x + boardBounds.width * layout[0],
                boardBounds.y + boardBounds.height * layout[1],
                boardBounds.width * layout[2],
                boardBounds.height * layout[3]);
    }

    public Rectangle getOwnerStripBounds(int cellIndex) {
        float[] r = STRIP_LAYOUT[cellIndex];
        return new Rectangle(
                boardBounds.x + boardBounds.width * r[0],
                boardBounds.y + boardBounds.height * r[1],
                boardBounds.width * r[2],
                boardBounds.height * r[3]);
    }

    public Rectangle getRentPlaqueBounds(int cellId, float plaqueW, float plaqueH) {
        Rectangle bounds = getCellBounds(cellId);
        float plaqueX, plaqueY;
        if ((cellId >= 11 && cellId <= 19) || (cellId >= 31 && cellId <= 39)) {
            // Горизонтальные клетки (левый ряд 11–19, правый ряд 31–39):
            // Опускаем ниже по Y (bounds.y + 1f) и смещаем правее на 2px (+ 2f),
            // чтобы не наползать на арт в центре клетки и гарантированно закрывать цену
            plaqueX = bounds.x + (bounds.width - plaqueW) / 2f + 2f;
            plaqueY = bounds.y + 1f;
        } else if (cellId >= 21 && cellId <= 29) {
            // Верхний ряд (21–29): опускаем в самый низ клетки поверх напечатанной цены
            plaqueX = bounds.x + (bounds.width - plaqueW) / 2f;
            plaqueY = bounds.y + 2f;
        } else {
            // Нижний ряд (1–9): строго внизу клетки, центрировано
            plaqueX = bounds.x + (bounds.width - plaqueW) / 2f;
            plaqueY = bounds.y + 2f;
        }
        return new Rectangle(plaqueX, plaqueY, plaqueW, plaqueH);
    }

    public Rectangle getRentPlaqueBounds(int cellId) {
        Rectangle bounds = getCellBounds(cellId);
        float plaqueH = 19f;
        float plaqueW = Math.min(bounds.width - 4f, 48f);
        return getRentPlaqueBounds(cellId, plaqueW, plaqueH);
    }

    public int getHoveredRentCell(float worldX, float worldY, GameState state, List<BoardCell> boardCells) {
        if (state == null || state.cellOwners == null || boardCells == null)
            return -1;
        for (int i = 0; i < 40; i++) {
            if (isCornerCell(i))
                continue;
            Integer ownerId = state.cellOwners.get(i);
            if (ownerId == null || ownerId < 0)
                continue;
            if (state.cellMortgaged != null && Boolean.TRUE.equals(state.cellMortgaged.get(i)))
                continue;
            BoardCell cell = (i < boardCells.size()) ? boardCells.get(i) : null;
            if (cell == null || cell.getEntranceFee() == 0)
                continue;

            Rectangle plaque = getRentPlaqueBounds(i);
            if (worldX >= plaque.x - 4f && worldX <= plaque.x + plaque.width + 4f
                    && worldY >= plaque.y - 4f && worldY <= plaque.y + plaque.height + 4f) {
                return i;
            }
        }
        return -1;
    }

    private Rectangle getTrapezoidBounds(int cellIndex, Rectangle s) {
        float i = boardBounds.width * TRAP_SLANT_INSET;
        switch (cellIndex) {
            case 1:
                return new Rectangle(s.x, s.y, s.width - i, s.height);
            case 9:
                return new Rectangle(s.x + i, s.y, s.width - i, s.height);
            case 21:
                return new Rectangle(s.x + i, s.y, s.width - i, s.height);
            case 29:
                return new Rectangle(s.x, s.y, s.width - i, s.height);
            case 11:
                return new Rectangle(s.x, s.y + i, s.width, s.height - i);
            case 19:
                return new Rectangle(s.x, s.y, s.width, s.height - i);
            case 31:
                return new Rectangle(s.x, s.y, s.width, s.height - i);
            case 39:
                return new Rectangle(s.x, s.y + i, s.width, s.height - i);
            default:
                return s;
        }
    }

    private Rectangle getOverlayBounds(float x, float y, float width, float height) {
        return new Rectangle(
                boardBounds.x + boardBounds.width * x,
                boardBounds.y + boardBounds.height * y,
                boardBounds.width * width,
                boardBounds.height * height);
    }

    private static float[][] createStripLayout() {
        float[][] s = new float[40][4];
        float t = STRIP_THICKNESS;
        for (int idx = 0; idx < 40; idx++) {
            float[] c = CELL_LAYOUT[idx];
            if (idx >= 1 && idx <= 9)
                s[idx] = rect(c[0], c[1] + c[3], c[2], t);
            else if (idx >= 21 && idx <= 29)
                s[idx] = rect(c[0], c[1] - t, c[2], t);
            else if (idx >= 11 && idx <= 19)
                s[idx] = rect(c[0] + c[2], c[1], t, c[3]);
            else if (idx >= 31 && idx <= 39)
                s[idx] = rect(c[0] - t, c[1], t, c[3]);
        }
        return s;
    }

    private static float[][] createCellLayout() {
        float[][] layout = new float[40][4];

        layout[0] = rect(1f - RING_W, 0f, RING_W, RING_H);
        layout[10] = rect(0f, 0f, RING_W, RING_H);
        layout[20] = rect(0f, 1f - RING_H, RING_W, RING_H);
        layout[30] = rect(1f - RING_W, 1f - RING_H, RING_W, RING_H);

        float hx0 = RING_W;
        float hx1 = 1f - RING_W;
        float cw = (hx1 - hx0) / 9f;
        float vy0 = RING_H;
        float vy1 = 1f - RING_H;
        float ch = (vy1 - vy0) / 9f;

        for (int i = 1; i <= 9; i++)
            layout[i] = rect(hx1 - cw * i, 0f, cw, RING_H);
        for (int i = 11; i <= 19; i++)
            layout[i] = rect(0f, vy0 + ch * (i - 11), RING_W, ch);
        for (int i = 21; i <= 29; i++)
            layout[i] = rect(hx0 + cw * (i - 21), 1f - RING_H, cw, RING_H);
        for (int i = 31; i <= 39; i++)
            layout[i] = rect(1f - RING_W, vy1 - ch * (i - 31 + 1), RING_W, ch);

        return layout;
    }

    private static float[] rect(float x, float y, float width, float height) {
        return new float[] { x, y, width, height };
    }

    // ==================== ХЕЛПЕРЫ КЛЕТОК ====================

    private float getSideRotation(int cellIndex) {
        if (cellIndex >= 11 && cellIndex <= 19)
            return 270f;
        if (cellIndex >= 21 && cellIndex <= 29)
            return 180f;
        if (cellIndex >= 31 && cellIndex <= 39)
            return 90f;
        return 0f;
    }

    private boolean isCornerCell(int cellIndex) {
        return cellIndex == 0 || cellIndex == 10 || cellIndex == 20 || cellIndex == 30;
    }

    private boolean isCornerAdjacentCell(int i) {
        return i == 1 || i == 9 || i == 11 || i == 19 || i == 21 || i == 29 || i == 31 || i == 39;
    }

    private boolean isHorizontalCell(int cellIndex) {
        return (cellIndex >= 11 && cellIndex <= 19) || (cellIndex >= 31 && cellIndex <= 39);
    }

    private boolean isVerticalCardCell(int cellIndex) {
        return (cellIndex >= 1 && cellIndex <= 9) || (cellIndex >= 21 && cellIndex <= 29);
    }

    private boolean isLeftSideCell(int cellIndex) {
        return cellIndex >= 11 && cellIndex <= 19;
    }

    private boolean isRightSideCell(int cellIndex) {
        return cellIndex >= 31 && cellIndex <= 39;
    }

    private boolean needsFlip(int i) {
        return i == 1 || i == 11 || i == 21 || i == 31;
    }

    // ==================== PLAQUE SETS ====================

    private static final Role[] NO_ROLE_FALLBACK_ORDER = { Role.MODERATOR, Role.MONOPOLIST, Role.DOGE, Role.MEMOLOG };

    private PlaqueSet getPlaqueSetFor(int ownerId, GameState gameState) {
        Role role = Role.of(gameState == null || gameState.playerRoles == null
                ? null
                : gameState.playerRoles.get(ownerId));
        if (role == null) {
            int slotIndex = 0;
            if (gameState != null && gameState.players != null) {
                for (int i = 0; i < gameState.players.size(); i++) {
                    if (gameState.players.get(i).id == ownerId) {
                        slotIndex = i;
                        break;
                    }
                }
            } else {
                slotIndex = Math.abs(ownerId) % 4;
            }
            role = NO_ROLE_FALLBACK_ORDER[slotIndex % 4];
        }
        return playerPlaqueSets[RoleInfo.plaqueIndex(role)];
    }

    private Color getPlayerColor(Player player, GameState gameState) {
        Role role = Role.of(gameState == null || gameState.playerRoles == null
                ? null
                : gameState.playerRoles.get(player.id));
        if (role == null) {
            int slotIndex = 0;
            if (gameState != null && gameState.players != null) {
                for (int i = 0; i < gameState.players.size(); i++) {
                    if (gameState.players.get(i).id == player.id) {
                        slotIndex = i;
                        break;
                    }
                }
            } else {
                slotIndex = Math.abs(player.id) % 4;
            }
            role = NO_ROLE_FALLBACK_ORDER[slotIndex % 4];
        }
        return RoleInfo.color(role);
    }

    private PlaqueSet createPlaqueSetWithFallback(String baseName, String fallbackName, Color tokenColor) {
        return buildPlaqueSet(baseName, fallbackName, tokenColor);
    }

    /**
     * Создаёт набор плашек для цвета роли.
     * Фолбэк: если своей текстуры нет — берётся current_* текстура и тонируется
     * цветом роли.
     */
    // BoardRenderer — плашки БЕЗ ссылок на current_*
    private PlaqueSet createPlaqueSet(String baseName, Color tokenColor) {
        return buildPlaqueSet(baseName, null, tokenColor);
    }

    /**
     * Набор плашек цвета роли. Если своих {color}.png / {color}(1x1).png нет —
     * берём текстуры фолбэк-цвета и тонируем цветом роли (tintColor).
     * Полосы vertical_/horizont_{color} опциональны.
     */
    private PlaqueSet buildPlaqueSet(String baseName, String fallbackName, Color tokenColor) {
        List<Texture> owned = new ArrayList<>();

        Texture plaqueTex = loadTextureIfExistsLinear(baseName + ".png");
        Texture trapTex = loadTextureIfExistsLinear(baseName + "(1x1).png");
        boolean ownPlaque = plaqueTex != null;
        boolean ownTrap = trapTex != null;
        if (plaqueTex == null && fallbackName != null) {
            plaqueTex = loadTextureIfExistsLinear(fallbackName + ".png");
        }
        if (trapTex == null && fallbackName != null) {
            trapTex = loadTextureIfExistsLinear(fallbackName + "(1x1).png");
        }
        if (plaqueTex != null)
            owned.add(plaqueTex);
        if (trapTex != null)
            owned.add(trapTex);

        Texture verticalTex = loadTextureIfExistsLinear("vertical_" + baseName + ".png");
        Texture horizontalTex = loadTextureIfExistsLinear("horizont_" + baseName + ".png");
        if (verticalTex != null)
            owned.add(verticalTex);
        if (horizontalTex != null)
            owned.add(horizontalTex);

        // Чужие текстуры (фолбэк) тонируем цветом роли; свои — без тонировки
        Color tint = (ownPlaque && ownTrap) ? null : tokenColor;

        TextureRegion plaqueRegion = plaqueTex != null ? new TextureRegion(plaqueTex) : null;
        TextureRegion trapRegion = trapTex != null ? new TextureRegion(trapTex) : null;
        TextureRegion trapFlippedRegion = trapTex != null ? new TextureRegion(trapTex) : null;
        if (trapFlippedRegion != null) {
            trapFlippedRegion.flip(true, false);
        }
        TextureRegion verticalStripRegion = verticalTex != null ? new TextureRegion(verticalTex) : null;
        TextureRegion horizontalStripRegion = horizontalTex != null ? new TextureRegion(horizontalTex) : null;

        return new PlaqueSet(
                owned.toArray(new Texture[0]),
                plaqueRegion, trapRegion, trapFlippedRegion,
                verticalStripRegion, horizontalStripRegion,
                tint, tokenColor);
    }

    // ==================== ЗАГРУЗКА ТЕКСТУР ====================

    private Texture loadTexture(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private Texture loadTextureLinear(String path) {
        Texture texture = new Texture(path);
        texture.setFilter(Texture.TextureFilter.Linear, Texture.TextureFilter.Linear);
        return texture;
    }

    private Texture loadTextureIfExists(String path) {
        return Gdx.files.internal(path).exists() ? loadTexture(path) : null;
    }

    private Texture loadTextureIfExistsLinear(String path) {
        return Gdx.files.internal(path).exists() ? loadTextureLinear(path) : null;
    }

    // ==================== ПОЗИЦИИ ФИШЕК ====================

    private List<Vector2> getTokenPositions(int cellIndex, Rectangle bounds, int playerCount, float tokenRadius) {
        List<Vector2> positions = new ArrayList<>();
        if (playerCount <= 0)
            return positions;

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
        if ((isVerticalCardCell(cellIndex) || isHorizontalCell(cellIndex) || isCornerCell(cellIndex))
                && playerCount <= 4) {
            addSidePositions(bounds, playerCount, positions);
            return positions;
        }

        float centerX = bounds.x + bounds.width * 0.5f;
        float centerY = bounds.y + bounds.height * 0.5f;
        float orbitRadius = playerCount == 1 ? 0f
                : Math.max(tokenRadius * 1.18f, Math.min(bounds.width, bounds.height) * 0.12f);

        if (isLeftSideCell(cellIndex))
            centerX = bounds.x + bounds.width * 0.58f;
        else if (isRightSideCell(cellIndex))
            centerX = bounds.x + bounds.width * 0.42f;

        for (int i = 0; i < playerCount; i++) {
            float angle = playerCount == 1 ? 0f
                    : (float) ((Math.PI * 2 * i / playerCount) - Math.PI / 2f);
            positions.add(new Vector2(
                    centerX + (float) Math.cos(angle) * orbitRadius,
                    centerY + (float) Math.sin(angle) * orbitRadius));
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
}

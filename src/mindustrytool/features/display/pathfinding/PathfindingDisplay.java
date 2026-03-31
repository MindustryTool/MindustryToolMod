package mindustrytool.features.display.pathfinding;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.scene.ui.Dialog;
import arc.struct.IntSet;
import arc.struct.IntSet.IntSetIterator;
import arc.util.Interval;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.ai.Pathfinder;
import mindustry.game.Team;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.world.Tile;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

import java.util.Arrays;
import java.util.Optional;

import static mindustry.Vars.*;

public class PathfindingDisplay implements Feature {
    private static final int MAX_STEPS_VERY_HIGH = 50;
    private static final int MAX_STEPS_HIGH = 100;
    private static final int MAX_STEPS_MEDIUM = 150;
    private static final int MAX_STEPS_LOW = 250;

    private static final int MAX_UPDATES_PER_FRAME = 3;
    private static final float CACHE_UPDATE_INTERVAL_UNIT = 15f;
    private static final float CACHE_CLEANUP_AGE_UNIT = 60f;
    private static final float CACHE_CLEANUP_AGE_SPAWN = 60f;
    private static final float CACHE_UPDATE_INTERVAL_SPAWN = 60f;

    private static final int CULLING_THRESHOLD = 300;
    private static final float CULLING_GROW = 500f;

    private static final int MAX_SPAWN_PATH_STEPS = 1000;

    private static final int TIMER_CLEANUP = 0;
    private static final float CLEANUP_SCHEDULE_FRAMES = 60f;

    private final PathfindingCacheManager pathCache = new PathfindingCacheManager();
    private final PathfindingCacheManager spawnPathCache = new PathfindingCacheManager();

    private final IntSet updateActiveTeams = new IntSet();
    private final IntSet drawActiveTeams = new IntSet();
    private final Interval timer = new Interval(1);

    private boolean isEnabled;
    private final PathfindingSettingsUI settingsUI = new PathfindingSettingsUI();

    private int currentFrameUpdates;
    private float currentOpacity;

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.pathfinding-visualizer.name")
                .description("@feature.pathfinding-visualizer.description")
                .icon(Utils.icons("pathfinding.png"))
                .order(0)
                .enabledByDefault(true)
                .quickAccess(true)
                .build();
    }

    @Override
    public void init() {
        PathfindingConfig.load();
        Events.run(Trigger.draw, this::draw);
        Events.run(Trigger.update, this::update);

        Events.on(WorldLoadEvent.class, e -> reset());
    }

    public void reset() {
        spawnPathCache.clear();
    }

    @Override
    public void onEnable() {
        isEnabled = true;
    }

    @Override
    public void onDisable() {
        isEnabled = false;
        pathCache.clear();
        spawnPathCache.clear();
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.of(settingsUI.getDialog());
    }

    private void update() {
        if (!isEnabled || !state.isGame()) {
            return;
        }

        if (timer.get(TIMER_CLEANUP, CLEANUP_SCHEDULE_FRAMES)) {
            float time = Time.time;
            pathCache.cleanup(time, CACHE_CLEANUP_AGE_UNIT);
            spawnPathCache.cleanup(time, CACHE_CLEANUP_AGE_SPAWN);
        }

        if (PathfindingConfig.isDrawSpawnPointPath()) {
            updateSpawnPointPaths();
        }

        if (PathfindingConfig.isDrawUnitPath()) {
            updateUnitPaths();
        }
    }

    private void draw() {
        if (!isEnabled || !state.isGame() || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown) {
            return;
        }

        float zoomThreshold = PathfindingConfig.getZoomThreshold();
        float currentZoom = renderer.getScale();

        if (currentZoom < zoomThreshold) {
            return;
        }

        currentOpacity = PathfindingConfig.getOpacity();

        if (PathfindingConfig.isDrawSpawnPointPath()) {
            drawSpawnPointPaths();
        }

        if (PathfindingConfig.isDrawUnitPath()) {
            drawUnitPaths();
        }
    }

    private void updateUnitPaths() {
        int totalUnits = Groups.unit.size();
        int maxSteps = (totalUnits > 2000) ? MAX_STEPS_VERY_HIGH
                : (totalUnits > 1000) ? MAX_STEPS_HIGH : (totalUnits > 500) ? MAX_STEPS_MEDIUM : MAX_STEPS_LOW;

        boolean useCulling = totalUnits > CULLING_THRESHOLD;
        Rect cullBounds = useCulling ? Core.camera.bounds(Tmp.r1).grow(CULLING_GROW) : null;
        float currentTime = Time.time;
        currentFrameUpdates = 0;

        if (useCulling) {
            Groups.unit.intersect(cullBounds.x, cullBounds.y, cullBounds.width, cullBounds.height, unit -> {
                updateProcessUnitPath(unit, currentTime, maxSteps);
            });
        } else {
            for (Unit unit : Groups.unit) {
                updateProcessUnitPath(unit, currentTime, maxSteps);
            }
        }
    }

    private void updateProcessUnitPath(Unit unit, float currentTime, int maxSteps) {
        if (unit.team == player.team()) {
            return;
        }

        long packedPosition = Point2.pack(unit.tileX(), unit.tileY());
        long cacheKey = (((long) packedPosition) << 32) | ((long) unit.type.flowfieldPathType << 8)
                | (long) unit.team.id;

        PathfindingCache cacheEntry = pathCache.get(cacheKey);

        if (cacheEntry == null || (currentTime - cacheEntry.lastUpdateTime) > CACHE_UPDATE_INTERVAL_UNIT) {
            if (cacheEntry == null) {
                cacheEntry = new PathfindingCache();
                cacheEntry.data = new float[MAX_STEPS_LOW * 2];
                pathCache.put(cacheKey, cacheEntry);
            }

            if (currentFrameUpdates < MAX_UPDATES_PER_FRAME) {
                cacheEntry.size = 0;
                recalculatePath(unit, cacheEntry, maxSteps);
                cacheEntry.lastUpdateTime = currentTime + Mathf.random(3f, 8f);
                currentFrameUpdates++;
            }
        }
    }

    private void drawUnitPaths() {
        Draw.z(Layer.overlayUI);

        int totalUnits = Groups.unit.size();
        int maxSteps = (totalUnits > 2000) ? MAX_STEPS_VERY_HIGH
                : (totalUnits > 1000) ? MAX_STEPS_HIGH : (totalUnits > 500) ? MAX_STEPS_MEDIUM : MAX_STEPS_LOW;

        boolean useCulling = totalUnits > CULLING_THRESHOLD;
        Rect cullBounds = useCulling ? Core.camera.bounds(Tmp.r1).grow(CULLING_GROW) : null;
        float currentTime = Time.time;

        if (useCulling) {
            Groups.unit.intersect(cullBounds.x, cullBounds.y, cullBounds.width, cullBounds.height, unit -> {
                drawProcessUnitPath(unit, currentTime, maxSteps);
            });
        } else {
            for (Unit unit : Groups.unit) {
                drawProcessUnitPath(unit, currentTime, maxSteps);
            }
        }
    }

    private void drawProcessUnitPath(Unit unit, float currentTime, int maxSteps) {
        if (unit.team == player.team()) {
            return;
        }

        long packedPosition = Point2.pack(unit.tileX(), unit.tileY());
        long cacheKey = (((long) packedPosition) << 32) | ((long) unit.type.flowfieldPathType << 8)
                | (long) unit.team.id;

        PathfindingCache cacheEntry = pathCache.get(cacheKey);

        if (cacheEntry == null) {
            return;
        }

        if (cacheEntry.lastUsedTime == currentTime) {
            return;
        }

        cacheEntry.lastUsedTime = currentTime;

        if (cacheEntry.size >= 2) {
            cacheEntry.data[0] = unit.x;
            cacheEntry.data[1] = unit.y;
        }

        drawFromCache(cacheEntry, unit.team.color, maxSteps);
    }

    private void recalculatePath(Unit unit, PathfindingCache cacheEntry, int maxSteps) {
        if (pathfinder == null) {
            return;
        }

        Tile tile = unit.tileOn();
        if (tile == null) {
            return;
        }

        int costType = unit.type.flowfieldPathType;
        int fieldType = Pathfinder.fieldCore;
        Pathfinder.Flowfield field = pathfinder.getField(unit.team, costType, fieldType);

        if (field == null) {
            cacheEntry.size = 0;
            return;
        }

        Tile currentTile = tile;
        cacheEntry.data[0] = unit.x;
        cacheEntry.data[1] = unit.y;
        int dataIndex = 2;

        for (int i = 0; i < maxSteps; i++) {
            Tile nextTile = pathfinder.getTargetTile(currentTile, field);
            if (nextTile == null || nextTile == currentTile) {
                break;
            }

            if (dataIndex >= cacheEntry.data.length - 2) {
                break;
            }

            cacheEntry.data[dataIndex++] = nextTile.worldx();
            cacheEntry.data[dataIndex++] = nextTile.worldy();
            currentTile = nextTile;
        }
        cacheEntry.size = dataIndex;
    }

    private void drawFromCache(PathfindingCache cacheEntry, Color pathColor, int maxSteps) {
        if (cacheEntry.size < 4) {
            return;
        }

        Lines.stroke(1f);

        float currentX = cacheEntry.data[0];
        float currentY = cacheEntry.data[1];
        int totalSegments = (cacheEntry.size / 2) - 1;

        if (totalSegments <= 0) {
            return;
        }

        for (int i = 0; i < totalSegments; i++) {
            float nextX = cacheEntry.data[(i + 1) * 2];
            float nextY = cacheEntry.data[(i + 1) * 2 + 1];

            Draw.color(pathColor, (1f - ((float) i / maxSteps)) * currentOpacity);
            Lines.line(currentX, currentY, nextX, nextY);

            currentX = nextX;
            currentY = nextY;
        }
        Draw.reset();
    }

    private void updateSpawnPointPaths() {
        if (pathfinder == null) {
            return;
        }

        float currentTime = Time.time;

        updateActiveTeams.clear();
        for (var spawnPoint : Vars.state.rules.spawns) {
            var team = spawnPoint.team == null ? Vars.state.rules.waveTeam : spawnPoint.team;
            if (team != player.team()) {
                updateActiveTeams.add(team.id);
            }
        }

        for (IntSetIterator it = updateActiveTeams.iterator(); it.hasNext;) {
            int teamId = it.next();
            Team team = Team.get(teamId);

            for (var costType = 0; costType < Pathfinder.costTypes.size; costType++) {
                if (!PathfindingConfig.isCostTypeEnabled(costType)) {
                    continue;
                }

                for (var spawnTile : Vars.spawner.getSpawns()) {
                    long key = ((long) spawnTile.pos() << 32) | ((long) costType << 16) | (long) team.id;
                    PathfindingCache cache = spawnPathCache.get(key);

                    if (cache == null) {
                        cache = new PathfindingCache();
                        cache.data = new float[2048];
                        spawnPathCache.put(key, cache);
                    }

                    if ((currentTime - cache.lastUpdateTime) > CACHE_UPDATE_INTERVAL_SPAWN) {
                        updateSpawnPathCache(cache, spawnTile, team, costType);
                        cache.lastUpdateTime = currentTime + Mathf.random(0f, 20f);
                    }
                }
            }
        }
    }

    private void drawSpawnPointPaths() {
        if (pathfinder == null) {
            return;
        }

        Draw.z(Layer.overlayUI);

        float currentTime = Time.time;

        drawActiveTeams.clear();
        for (var spawnPoint : Vars.state.rules.spawns) {
            var team = spawnPoint.team == null ? Vars.state.rules.waveTeam : spawnPoint.team;
            if (team != player.team()) {
                drawActiveTeams.add(team.id);
            }
        }

        for (IntSetIterator it = drawActiveTeams.iterator(); it.hasNext;) {
            int teamId = it.next();
            Team team = Team.get(teamId);

            for (var costType = 0; costType < Pathfinder.costTypes.size; costType++) {
                if (!PathfindingConfig.isCostTypeEnabled(costType)) {
                    continue;
                }

                for (var spawnTile : Vars.spawner.getSpawns()) {
                    long key = ((long) spawnTile.pos() << 32) | ((long) costType << 16) | (long) team.id;
                    PathfindingCache cache = spawnPathCache.get(key);

                    if (cache != null) {
                        cache.lastUsedTime = currentTime;

                        if (cache.size > 0) {
                            drawSpawnPathFromCache(cache, team.color);
                        }
                    }
                }
            }
        }

        Draw.reset();
    }

    private void updateSpawnPathCache(PathfindingCache cache, Tile startTile, Team team, int costType) {
        int fieldType = Pathfinder.fieldCore;
        Pathfinder.Flowfield field = pathfinder.getField(team, costType, fieldType);

        if (field == null) {
            cache.size = 0;
            return;
        }

        Tile currentTile = startTile;
        float segmentStartX = startTile.worldx();
        float segmentStartY = startTile.worldy();

        int lastDx = -2, lastDy = -2;
        int dataIndex = 0;

        if (dataIndex + 2 > cache.data.length) {
            cache.data = Arrays.copyOf(cache.data, cache.data.length * 2);
        }

        cache.data[dataIndex++] = segmentStartX;
        cache.data[dataIndex++] = segmentStartY;

        for (int i = 0; i < MAX_SPAWN_PATH_STEPS; i++) {
            Tile nextTile = pathfinder.getTargetTile(currentTile, field);
            if (nextTile == null) {
                break;
            }

            if (nextTile == currentTile) {
                break;
            }

            int dx = nextTile.x - currentTile.x;
            int dy = nextTile.y - currentTile.y;

            if (dx != lastDx || dy != lastDy) {
                if (i > 0) {
                    if (dataIndex + 2 > cache.data.length)
                        cache.data = Arrays.copyOf(cache.data, cache.data.length * 2);
                    cache.data[dataIndex++] = currentTile.worldx();
                    cache.data[dataIndex++] = currentTile.worldy();
                }
                lastDx = dx;
                lastDy = dy;
            }
            currentTile = nextTile;
        }

        if (dataIndex + 2 > cache.data.length)
            cache.data = Arrays.copyOf(cache.data, cache.data.length * 2);
        cache.data[dataIndex++] = currentTile.worldx();
        cache.data[dataIndex++] = currentTile.worldy();

        cache.size = dataIndex;
    }

    private void drawSpawnPathFromCache(PathfindingCache cache, Color color) {
        if (cache.size < 4) {
            return;
        }

        Draw.color(color, currentOpacity);
        Lines.stroke(1f);

        for (int i = 0; i < cache.size - 2; i += 2) {
            Lines.line(cache.data[i], cache.data[i + 1], cache.data[i + 2], cache.data[i + 3]);
        }
    }
}

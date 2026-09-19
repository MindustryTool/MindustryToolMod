package mindustrytool.features.pathfinding;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Point2;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.scene.Element;
import arc.struct.IntSet;
import arc.struct.IntSet.IntSetIterator;
import arc.util.Interval;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.Tmp;
import java.util.Arrays;
import mindustry.Vars;
import mindustry.ai.Pathfinder;
import mindustry.ai.types.CommandAI;
import mindustry.ai.types.LogicAI;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.game.SpawnGroup;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.logic.LUnitControl;
import mindustry.world.Tile;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;

public class PathfindingFeature extends Feature {

    public static final int COST_COUNT = 6;

    private static final int MAX_STEPS_VERY_HIGH = 50;
    private static final int MAX_STEPS_HIGH = 100;
    private static final int MAX_STEPS_MEDIUM = 150;
    private static final int MAX_STEPS_LOW = 250;

    private static final int MAX_UPDATES_PER_FRAME = 4;
    private static final float CACHE_UPDATE_INTERVAL_UNIT = 15f;
    private static final float CACHE_CLEANUP_AGE = 60f;
    private static final float CACHE_UPDATE_INTERVAL_SPAWN = 60f;

    private static final int CULLING_THRESHOLD = 300;
    private static final float CULLING_GROW = 500f;
    private static final int MAX_SPAWN_PATH_STEPS = 1000;

    private static final int TIMER_CLEANUP = 0;
    private static final float CLEANUP_SCHEDULE_FRAMES = 60f;

    public final ConfigGroup config;
    public final ConfigValue<Boolean> drawUnitPathConfig;
    public final ConfigValue<Boolean> drawSpawnPathConfig;
    public final ConfigValue<Boolean> drawAlliesConfig;
    public final ConfigValue<Float> zoomThresholdConfig;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Boolean>[] costTypeConfigs;

    private final PathfindingCacheManager pathCache = new PathfindingCacheManager();
    private final PathfindingCacheManager spawnPathCache = new PathfindingCacheManager();
    private final ClientPathfinder clientPathfinder = new ClientPathfinder();

    private final IntSet updateActiveTeams = new IntSet();
    private final IntSet drawActiveTeams = new IntSet();
    private final Interval timer = new Interval(1);
    private final Vec2 logicPathfindTarget = new Vec2();

    private @Nullable PathfindingSettingsDialog settingsDialog;

    // Fast primitive cached configuration
    private boolean cachedDrawUnitPath = true;
    private boolean cachedDrawSpawnPath = true;
    private boolean cachedDrawAllies = false;
    private float cachedZoomThreshold = 0.5f;
    private float cachedOpacity = 1.0f;
    private final boolean[] cachedCostTypes = new boolean[COST_COUNT];

    private int currentFrameUpdates;

    private final Cons<Unit> unitUpdateProcessor = this::updateProcessUnitPath;
    private final Cons<Unit> unitDrawProcessor = this::drawProcessUnitPath;

    @SuppressWarnings("unchecked")
    public PathfindingFeature() {
        super(FeatureMetadata.builder()
                .id("pathfinding")
                .icon(FileIcon.of("pathfinding.png"))
                .order(12)
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        config = configGroup();
        drawUnitPathConfig = config.boolValue("draw-unit-path", true);
        drawSpawnPathConfig = config.boolValue("draw-spawn-path", true);
        drawAlliesConfig = config.boolValue("draw-allies", false);
        zoomThresholdConfig = config.floatValue("zoom-threshold", 0.5f);
        opacityConfig = config.floatValue("opacity", 1.0f);

        costTypeConfigs = new ConfigValue[COST_COUNT];
        for (int i = 0; i < COST_COUNT; i++) {
            costTypeConfigs[i] = config.boolValue("cost-type." + i, true);
        }

        drawUnitPathConfig.signal().subscribe(v -> cachedDrawUnitPath = v != null ? v : true);
        drawSpawnPathConfig.signal().subscribe(v -> cachedDrawSpawnPath = v != null ? v : true);
        drawAlliesConfig.signal().subscribe(v -> cachedDrawAllies = v != null ? v : false);
        zoomThresholdConfig.signal().subscribe(v -> cachedZoomThreshold = v != null ? v : 0.5f);
        opacityConfig.signal().subscribe(v -> cachedOpacity = v != null ? v : 1.0f);

        for (int i = 0; i < COST_COUNT; i++) {
            final int index = i;
            costTypeConfigs[i].signal().subscribe(v -> cachedCostTypes[index] = v != null ? v : true);
        }

        syncConfigCache();

        bindToggle("pathfindingToggle", KeyCode.unset);
        bindDialog("pathfindingSettings", KeyCode.unset, getSettingDialog(), false);

        Events.run(Trigger.update, this::update);
        Events.run(Trigger.draw, this::draw);
        Events.on(WorldLoadEvent.class, e -> reset());
    }

    private void syncConfigCache() {
        Boolean dup = drawUnitPathConfig.get();
        cachedDrawUnitPath = dup != null ? dup : true;

        Boolean dsp = drawSpawnPathConfig.get();
        cachedDrawSpawnPath = dsp != null ? dsp : true;

        Boolean da = drawAlliesConfig.get();
        cachedDrawAllies = da != null ? da : false;

        Float zt = zoomThresholdConfig.get();
        cachedZoomThreshold = zt != null ? zt : 0.5f;

        Float op = opacityConfig.get();
        cachedOpacity = op != null ? op : 1.0f;

        for (int i = 0; i < COST_COUNT; i++) {
            Boolean c = costTypeConfigs[i].get();
            cachedCostTypes[i] = c != null ? c : true;
        }
    }

    public void resetToDefaults() {
        drawUnitPathConfig.reset();
        drawSpawnPathConfig.reset();
        drawAlliesConfig.reset();
        zoomThresholdConfig.reset();
        opacityConfig.reset();
        for (int i = 0; i < COST_COUNT; i++) {
            costTypeConfigs[i].reset();
        }
        syncConfigCache();
    }

    public void reset() {
        pathCache.clear();
        spawnPathCache.clear();
        clientPathfinder.clear();
    }

    @Override
    public void onDisable() {
        reset();
    }

    @Override
    public void onQuickAccessClick(@Nullable Element anchor) {
        setEnabled(!isEnabled());
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new PathfindingSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    public boolean isCostTypeEnabled(int costType) {
        return costType >= 0 && costType < COST_COUNT && cachedCostTypes[costType];
    }

    private void update() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame()) {
            return;
        }

        if (timer.get(TIMER_CLEANUP, CLEANUP_SCHEDULE_FRAMES)) {
            float time = Time.time;
            pathCache.cleanup(time, CACHE_CLEANUP_AGE);
            spawnPathCache.cleanup(time, CACHE_CLEANUP_AGE);
        }

        if (cachedDrawSpawnPath) {
            updateSpawnPointPaths();
        }

        if (cachedDrawUnitPath) {
            updateUnitPaths();
        }
    }

    private void draw() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.ui == null || Core.camera == null) {
            return;
        }

        if (Vars.ui.hudfrag != null && !Vars.ui.hudfrag.shown) {
            return;
        }

        if (cachedZoomThreshold > 0.01f && Vars.renderer != null && Vars.renderer.getScale() < cachedZoomThreshold) {
            return;
        }

        float z = Draw.z();
        Draw.z(Layer.overlayUI);

        if (cachedDrawSpawnPath) {
            drawSpawnPointPaths();
        }

        if (cachedDrawUnitPath) {
            drawUnitPaths();
        }

        Draw.z(z);
        Draw.reset();
    }

    private void updateUnitPaths() {
        int totalUnits = Groups.unit.size();
        boolean useCulling = totalUnits > CULLING_THRESHOLD;
        Rect cullBounds = useCulling ? Core.camera.bounds(Tmp.r1).grow(CULLING_GROW) : null;
        currentFrameUpdates = 0;

        if (useCulling && cullBounds != null) {
            Groups.unit.intersect(cullBounds.x, cullBounds.y, cullBounds.width, cullBounds.height, unitUpdateProcessor);
        } else {
            for (Unit unit : Groups.unit) {
                updateProcessUnitPath(unit);
            }
        }
    }

    private void updateProcessUnitPath(Unit unit) {
        if (unit == null || unit.type == null || unit.dead()) {
            return;
        }

        if (unit.isPlayer()) {
            return;
        }

        if (isSkippedLogicUnit(unit)) {
            return;
        }

        boolean isAlly = Vars.player != null && unit.team == Vars.player.team();
        if (isAlly && !cachedDrawAllies) {
            return;
        }

        int costType = unit.type.flowfieldPathType;
        if (!isCostTypeEnabled(costType)) {
            return;
        }

        Vec2 commandTarget = getCommandTarget(unit);
        boolean isCommanded = commandTarget != null;

        long packedPos = Point2.pack(unit.tileX(), unit.tileY());
        long cacheKey = isCommanded
                ? (((long) unit.id) << 32) | ((long) Point2.pack((int) (commandTarget.x / 8), (int) (commandTarget.y / 8)))
                : (((long) packedPos) << 32) | ((long) costType << 8) | (long) unit.team.id;

        PathfindingCache cacheEntry = pathCache.get(cacheKey);
        float currentTime = Time.time;

        if (cacheEntry == null || (currentTime - cacheEntry.lastUpdateTime) > CACHE_UPDATE_INTERVAL_UNIT) {
            if (cacheEntry == null) {
                cacheEntry = new PathfindingCache(MAX_STEPS_LOW * 2);
                pathCache.put(cacheKey, cacheEntry);
            }

            if (currentFrameUpdates < MAX_UPDATES_PER_FRAME) {
                cacheEntry.size = 0;
                int maxSteps = getMaxStepsForUnitCount();
                if (isCommanded) {
                    recalculateCommandedPath(unit, commandTarget, cacheEntry, maxSteps);
                } else {
                    recalculateWavePath(unit, cacheEntry, maxSteps);
                }
                cacheEntry.lastUpdateTime = currentTime + Mathf.random(3f, 8f);
                currentFrameUpdates++;
            }
        }
    }

    private @Nullable Vec2 getCommandTarget(Unit unit) {
        if (unit.controller() instanceof CommandAI) {
            CommandAI cai = (CommandAI) unit.controller();
            if (cai.hasCommand() && cai.targetPos != null) {
                return cai.targetPos;
            }
        }
        if (unit.controller() instanceof LogicAI) {
            LogicAI logic = (LogicAI) unit.controller();
            if (logic.control == LUnitControl.pathfind) {
                return logicPathfindTarget.set(logic.moveX, logic.moveY);
            }
        }
        return null;
    }

    private boolean isSkippedLogicUnit(Unit unit) {
        if (!(unit.controller() instanceof LogicAI)) {
            return false;
        }
        LogicAI logic = (LogicAI) unit.controller();
        return logic.control != LUnitControl.pathfind && logic.control != LUnitControl.autoPathfind;
    }

    private int getMaxStepsForUnitCount() {
        int totalUnits = Groups.unit.size();
        return (totalUnits > 2000) ? MAX_STEPS_VERY_HIGH
                : (totalUnits > 1000) ? MAX_STEPS_HIGH
                : (totalUnits > 500) ? MAX_STEPS_MEDIUM
                : MAX_STEPS_LOW;
    }

    private void recalculateWavePath(Unit unit, PathfindingCache cacheEntry, int maxSteps) {
        Tile tile = unit.tileOn();
        if (tile == null) {
            cacheEntry.size = 0;
            return;
        }

        int costType = unit.type.flowfieldPathType;
        Tile currentTile = tile;

        ensureCapacity(cacheEntry, (maxSteps + 1) * 2);
        cacheEntry.data[0] = unit.x;
        cacheEntry.data[1] = unit.y;
        int dataIndex = 2;

        boolean isClient = Vars.net.client();
        Pathfinder.Flowfield hostField = null;
        ClientPathfinder.ClientFlowfield clientField = null;

        if (isClient) {
            clientField = clientPathfinder.getOrUpdateFlowfield(unit.team, costType);
            if (clientField == null) {
                cacheEntry.size = 0;
                return;
            }
        } else {
            if (Vars.pathfinder == null) {
                cacheEntry.size = 0;
                return;
            }
            hostField = Vars.pathfinder.getField(unit.team, costType, Pathfinder.fieldCore);
            if (hostField == null) {
                cacheEntry.size = 0;
                return;
            }
        }

        for (int i = 0; i < maxSteps; i++) {
            Tile nextTile = isClient
                    ? clientPathfinder.getTargetTile(currentTile, clientField)
                    : Vars.pathfinder.getTargetTile(currentTile, hostField);

            if (nextTile == null || nextTile == currentTile) {
                break;
            }

            if (dataIndex >= cacheEntry.data.length - 2) {
                ensureCapacity(cacheEntry, cacheEntry.data.length * 2);
            }

            cacheEntry.data[dataIndex++] = nextTile.worldx();
            cacheEntry.data[dataIndex++] = nextTile.worldy();
            currentTile = nextTile;
        }

        cacheEntry.size = dataIndex;
    }

    private void recalculateCommandedPath(Unit unit, Vec2 targetPos, PathfindingCache cacheEntry, int maxSteps) {
        clientPathfinder.findCommandedPath(unit, targetPos, maxSteps, cacheEntry);
    }

    private void drawUnitPaths() {
        int totalUnits = Groups.unit.size();
        boolean useCulling = totalUnits > CULLING_THRESHOLD;
        Rect cullBounds = useCulling ? Core.camera.bounds(Tmp.r1).grow(CULLING_GROW) : null;

        if (useCulling && cullBounds != null) {
            Groups.unit.intersect(cullBounds.x, cullBounds.y, cullBounds.width, cullBounds.height, unitDrawProcessor);
        } else {
            for (Unit unit : Groups.unit) {
                drawProcessUnitPath(unit);
            }
        }
    }

    private void drawProcessUnitPath(Unit unit) {
        if (unit == null || unit.dead()) {
            return;
        }

        if (unit.isPlayer()) {
            return;
        }

        if (isSkippedLogicUnit(unit)) {
            return;
        }

        boolean isAlly = Vars.player != null && unit.team == Vars.player.team();
        if (isAlly && !cachedDrawAllies) {
            return;
        }

        Vec2 commandTarget = getCommandTarget(unit);
        boolean isCommanded = commandTarget != null;

        long packedPos = Point2.pack(unit.tileX(), unit.tileY());
        long cacheKey = isCommanded
                ? (((long) unit.id) << 32) | ((long) Point2.pack((int) (commandTarget.x / 8), (int) (commandTarget.y / 8)))
                : (((long) packedPos) << 32) | ((long) unit.type.flowfieldPathType << 8) | (long) unit.team.id;

        PathfindingCache cacheEntry = pathCache.get(cacheKey);
        if (cacheEntry == null || cacheEntry.size < 4) {
            return;
        }

        float currentTime = Time.time;
        if (cacheEntry.lastUsedTime == currentTime) {
            return;
        }
        cacheEntry.lastUsedTime = currentTime;

        // Keep root of path attached to live unit position
        cacheEntry.data[0] = unit.x;
        cacheEntry.data[1] = unit.y;

        drawPathFromCache(cacheEntry, unit.team.color);
    }

    private void updateSpawnPointPaths() {
        if (Vars.state == null || Vars.state.rules == null || Vars.spawner == null) {
            return;
        }

        float currentTime = Time.time;
        updateActiveTeams.clear();

        for (int i = 0; i < Vars.state.rules.spawns.size; i++) {
            SpawnGroup spawnPoint = Vars.state.rules.spawns.get(i);
            Team team = spawnPoint.team == null ? Vars.state.rules.waveTeam : spawnPoint.team;
            if (team != null && (Vars.player == null || team != Vars.player.team() || cachedDrawAllies)) {
                updateActiveTeams.add(team.id);
            }
        }

        for (IntSetIterator it = updateActiveTeams.iterator(); it.hasNext;) {
            int teamId = it.next();
            Team team = Team.get(teamId);
            if (team == null) {
                continue;
            }

            for (int costType = 0; costType < COST_COUNT; costType++) {
                if (!isCostTypeEnabled(costType)) {
                    continue;
                }

                for (Tile spawnTile : Vars.spawner.getSpawns()) {
                    if (spawnTile == null) {
                        continue;
                    }
                    long key = ((long) spawnTile.pos() << 32) | ((long) costType << 16) | (long) team.id;
                    PathfindingCache cache = spawnPathCache.get(key);

                    if (cache == null) {
                        cache = new PathfindingCache(2048);
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

    private void updateSpawnPathCache(PathfindingCache cache, Tile startTile, Team team, int costType) {
        boolean isClient = Vars.net.client();
        Pathfinder.Flowfield hostField = null;
        ClientPathfinder.ClientFlowfield clientField = null;

        if (isClient) {
            clientField = clientPathfinder.getOrUpdateFlowfield(team, costType);
            if (clientField == null) {
                cache.size = 0;
                return;
            }
        } else {
            if (Vars.pathfinder == null) {
                cache.size = 0;
                return;
            }
            hostField = Vars.pathfinder.getField(team, costType, Pathfinder.fieldCore);
            if (hostField == null) {
                cache.size = 0;
                return;
            }
        }

        Tile currentTile = startTile;
        float segmentStartX = startTile.worldx();
        float segmentStartY = startTile.worldy();

        int lastDx = -2, lastDy = -2;
        int dataIndex = 0;

        ensureCapacity(cache, 256);
        cache.data[dataIndex++] = segmentStartX;
        cache.data[dataIndex++] = segmentStartY;

        for (int i = 0; i < MAX_SPAWN_PATH_STEPS; i++) {
            Tile nextTile = isClient
                    ? clientPathfinder.getTargetTile(currentTile, clientField)
                    : Vars.pathfinder.getTargetTile(currentTile, hostField);

            if (nextTile == null || nextTile == currentTile) {
                break;
            }

            int dx = nextTile.x - currentTile.x;
            int dy = nextTile.y - currentTile.y;

            if (dx != lastDx || dy != lastDy) {
                if (i > 0) {
                    if (dataIndex + 2 > cache.data.length) {
                        ensureCapacity(cache, cache.data.length * 2);
                    }
                    cache.data[dataIndex++] = currentTile.worldx();
                    cache.data[dataIndex++] = currentTile.worldy();
                }
                lastDx = dx;
                lastDy = dy;
            }
            currentTile = nextTile;
        }

        if (dataIndex + 2 > cache.data.length) {
            ensureCapacity(cache, cache.data.length * 2);
        }
        cache.data[dataIndex++] = currentTile.worldx();
        cache.data[dataIndex++] = currentTile.worldy();
        cache.size = dataIndex;
    }

    private void drawSpawnPointPaths() {
        if (Vars.state == null || Vars.state.rules == null || Vars.spawner == null) {
            return;
        }

        float currentTime = Time.time;
        drawActiveTeams.clear();

        for (int i = 0; i < Vars.state.rules.spawns.size; i++) {
            SpawnGroup spawnPoint = Vars.state.rules.spawns.get(i);
            Team team = spawnPoint.team == null ? Vars.state.rules.waveTeam : spawnPoint.team;
            if (team != null && (Vars.player == null || team != Vars.player.team() || cachedDrawAllies)) {
                drawActiveTeams.add(team.id);
            }
        }

        for (IntSetIterator it = drawActiveTeams.iterator(); it.hasNext;) {
            int teamId = it.next();
            Team team = Team.get(teamId);
            if (team == null) {
                continue;
            }

            for (int costType = 0; costType < COST_COUNT; costType++) {
                if (!isCostTypeEnabled(costType)) {
                    continue;
                }

                for (Tile spawnTile : Vars.spawner.getSpawns()) {
                    if (spawnTile == null) {
                        continue;
                    }
                    long key = ((long) spawnTile.pos() << 32) | ((long) costType << 16) | (long) team.id;
                    PathfindingCache cache = spawnPathCache.get(key);

                    if (cache != null && cache.size >= 4) {
                        cache.lastUsedTime = currentTime;
                        drawPathFromCache(cache, team.color);
                    }
                }
            }
        }
    }

    private void drawPathFromCache(PathfindingCache cache, Color color) {
        if (cache.size < 4) {
            return;
        }

        Draw.color(color, cachedOpacity);
        Lines.stroke(1f);

        for (int i = 0; i < cache.size - 2; i += 2) {
            Lines.line(cache.data[i], cache.data[i + 1], cache.data[i + 2], cache.data[i + 3]);
        }
    }

    private static void ensureCapacity(PathfindingCache cache, int requiredCapacity) {
        if (cache.data.length < requiredCapacity) {
            cache.data = Arrays.copyOf(cache.data, Math.max(cache.data.length * 2, requiredCapacity));
        }
    }
}

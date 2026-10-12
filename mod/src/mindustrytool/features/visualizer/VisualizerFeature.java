package mindustrytool.features.visualizer;

import java.lang.reflect.Field;
import java.util.BitSet;
import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.GlyphLayout;
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.struct.IntMap;
import arc.struct.IntSeq;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import arc.util.pooling.Pool.Poolable;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.Trigger;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Building;
import mindustry.gen.Healthc;
import mindustry.gen.Posc;
import mindustry.gen.TimeItem;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.ui.Fonts;
import mindustry.world.Block;
import mindustry.world.ItemBuffer;
import mindustry.world.blocks.defense.BuildTurret;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.ItemTurret.ItemTurretBuild;
import mindustry.world.blocks.defense.turrets.Turret.TurretBuild;
import mindustry.world.blocks.distribution.BufferedItemBridge;
import mindustry.world.blocks.distribution.BufferedItemBridge.BufferedItemBridgeBuild;
import mindustry.world.blocks.distribution.DirectionBridge.DirectionBridgeBuild;
import mindustry.world.blocks.distribution.DirectionLiquidBridge;
import mindustry.world.blocks.distribution.DuctBridge;
import mindustry.world.blocks.distribution.DuctBridge.DuctBridgeBuild;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.ItemBridge.ItemBridgeBuild;
import mindustry.world.blocks.liquid.LiquidBridge.LiquidBridgeBuild;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;
import solim.reactive.Signal;

/**
 * Unified feature responsible for rendering real-time tactical visualizations:
 * - Items & fluids in motion across bridge conveyors, duct bridges, and liquid bridges.
 * - Real in-game bridge throughput flow rate tracking.
 * - Turret tactical data: loaded ammo badges with fill gauge and target pointer lines with health reticles.
 *
 * Optimized for 60 FPS real-time rendering:
 * - Zero allocations in draw() loop.
 * - Pre-allocated predicate and consumer delegates.
 * - Primitive configuration cache to eliminate boxing/unboxing and signal overhead.
 * - Viewport frustum culling.
 */
public class VisualizerFeature extends Feature {

    public final ConfigGroup config;

    // Bridge Configurations
    public final ConfigValue<Boolean> showItemBridgesConfig;
    public final ConfigValue<Boolean> showDuctBridgesConfig;
    public final ConfigValue<Boolean> showLiquidBridgesConfig;
    public final ConfigValue<Float> bridgeItemScaleConfig;
    public final ConfigValue<Float> bridgeOpacityConfig;
    public final ConfigValue<Boolean> showBridgeFlowRateConfig;
    public final ConfigValue<Boolean> hideIdleBridgeFlowConfig;
    public final ConfigValue<Float> bridgeFlowRateScaleConfig;

    // Turret Configurations
    public final ConfigValue<Boolean> showAmmoBadgeConfig;
    public final ConfigValue<Boolean> showTargetLineConfig;
    public final ConfigValue<Boolean> targetLineAllyConfig;
    public final ConfigValue<Boolean> targetLineEnemyConfig;
    public final ConfigValue<Boolean> onlyWhenShootingConfig;
    public final ConfigValue<Float> turretBadgeScaleConfig;
    public final ConfigValue<Float> targetLineOpacityConfig;

    private final Rect viewBounds = new Rect();
    private final Color colorScratch = new Color();
    private final Color healthColorScratch = new Color();
    private final GlyphLayout glyphLayout = new GlyphLayout();
    private @Nullable VisualizerSettingsDialog settingsDialog;

    // Per-turret enabled bitset indexed by block.id for zero-allocation O(1) checks during 60 FPS draw
    private @Nullable BitSet turretEnabled;
    private final ObjectMap<String, Signal<Boolean>> turretSignals = new ObjectMap<>();

    // Flow tracking per bridge position
    private final IntMap<BridgeFlowState> bridgeFlowStates = new IntMap<>();
    private final Seq<BridgeFlowState> statePool = new Seq<>(256);
    private final IntSeq staleKeys = new IntSeq();
    private float lastCleanupTime = 0f;

    private static final String ZERO_RATE_TEXT = "0/s";
    private static final String OVER_100_RATE_TEXT = ">100/s";
    private static final String[] INT_RATE_CACHE = new String[101];
    private static final String[] TENTHS_RATE_CACHE = new String[100];

    static {
        for (int i = 0; i <= 100; i++) {
            INT_RATE_CACHE[i] = i + "/s";
        }
        for (int i = 0; i < 100; i++) {
            TENTHS_RATE_CACHE[i] = (i / 10) + "." + (i % 10) + "/s";
        }
    }

    // Pre-allocated functional interfaces for zero GC allocation during eachBlock
    private static final Boolf<Building> IS_VISUALIZED = b -> b instanceof ItemBridgeBuild
            || b instanceof LiquidBridgeBuild
            || b instanceof DirectionBridgeBuild
            || b instanceof TurretBuild;
    private final Cons<Building> buildingDrawer = this::drawBuilding;

    // Cached primitive configurations to avoid repeated getter calls, boxing, and unboxing in draw()
    private boolean cachedShowItemBridges = true;
    private boolean cachedShowDuctBridges = true;
    private boolean cachedShowLiquidBridges = true;
    private float cachedBridgeItemScale = 1.0f;
    private float cachedBridgeOpacity = 1.0f;
    private boolean cachedShowBridgeFlowRate = true;
    private boolean cachedHideIdleBridgeFlow = false;
    private float cachedBridgeFlowRateScale = 1.0f;

    private boolean cachedShowAmmoBadge = true;
    private boolean cachedShowTargetLine = true;
    private boolean cachedTargetLineAlly = true;
    private boolean cachedTargetLineEnemy = false;
    private boolean cachedOnlyWhenShooting = false;
    private float cachedTurretBadgeScale = 1.0f;
    private float cachedTargetLineOpacity = 0.6f;

    private static @Nullable Field bufferField;
    private static @Nullable Field itemBufferArrayField;
    private static @Nullable Field itemBufferIndexField;

    static {
        try {
            bufferField = BufferedItemBridgeBuild.class.getDeclaredField("buffer");
            bufferField.setAccessible(true);
        } catch (Throwable t) {
            bufferField = null;
        }

        try {
            itemBufferArrayField = ItemBuffer.class.getDeclaredField("buffer");
            itemBufferArrayField.setAccessible(true);
            itemBufferIndexField = ItemBuffer.class.getDeclaredField("index");
            itemBufferIndexField.setAccessible(true);
        } catch (Throwable t) {
            itemBufferArrayField = null;
            itemBufferIndexField = null;
        }
    }

    public VisualizerFeature() {
        super(FeatureMetadata.builder()
                .id("visualizer")
                .icon(FileIcon.of("network.png"))
                .order(5)
                .enabledByDefault(false)
                .quickAccess(false)
                .displayGroup(true)
                .build());

        config = configGroup();

        // Bridges
        showItemBridgesConfig = config.boolValue("show-item-bridges", true);
        showDuctBridgesConfig = config.boolValue("show-duct-bridges", true);
        showLiquidBridgesConfig = config.boolValue("show-liquid-bridges", true);
        bridgeItemScaleConfig = config.floatValue("bridge-item-scale", 1.0f);
        bridgeOpacityConfig = config.floatValue("bridge-opacity", 1.0f);
        showBridgeFlowRateConfig = config.boolValue("show-bridge-flow-rate", true);
        hideIdleBridgeFlowConfig = config.boolValue("hide-idle-bridge-flow", false);
        bridgeFlowRateScaleConfig = config.floatValue("bridge-flow-rate-scale", 1.0f);

        // Turrets
        showAmmoBadgeConfig = config.boolValue("show-ammo-badge", true);
        showTargetLineConfig = config.boolValue("show-target-line", true);
        targetLineAllyConfig = config.boolValue("target-line-ally", true);
        targetLineEnemyConfig = config.boolValue("target-line-enemy", false);
        onlyWhenShootingConfig = config.boolValue("only-when-shooting", false);
        turretBadgeScaleConfig = config.floatValue("turret-badge-scale", 1.0f);
        targetLineOpacityConfig = config.floatValue("target-line-opacity", 0.6f);

        // Synchronize primitive caches on reactive signal changes
        showItemBridgesConfig.signal().subscribe(v -> cachedShowItemBridges = v != null ? v : true);
        showDuctBridgesConfig.signal().subscribe(v -> cachedShowDuctBridges = v != null ? v : true);
        showLiquidBridgesConfig.signal().subscribe(v -> cachedShowLiquidBridges = v != null ? v : true);
        bridgeItemScaleConfig.signal().subscribe(v -> cachedBridgeItemScale = v != null ? v : 1.0f);
        bridgeOpacityConfig.signal().subscribe(v -> cachedBridgeOpacity = v != null ? v : 1.0f);
        showBridgeFlowRateConfig.signal().subscribe(v -> cachedShowBridgeFlowRate = v != null ? v : true);
        hideIdleBridgeFlowConfig.signal().subscribe(v -> cachedHideIdleBridgeFlow = v != null ? v : false);
        bridgeFlowRateScaleConfig.signal().subscribe(v -> cachedBridgeFlowRateScale = v != null ? v : 1.0f);

        showAmmoBadgeConfig.signal().subscribe(v -> cachedShowAmmoBadge = v != null ? v : true);
        showTargetLineConfig.signal().subscribe(v -> cachedShowTargetLine = v != null ? v : true);
        targetLineAllyConfig.signal().subscribe(v -> cachedTargetLineAlly = v != null ? v : true);
        targetLineEnemyConfig.signal().subscribe(v -> cachedTargetLineEnemy = v != null ? v : false);
        onlyWhenShootingConfig.signal().subscribe(v -> cachedOnlyWhenShooting = v != null ? v : false);
        turretBadgeScaleConfig.signal().subscribe(v -> cachedTurretBadgeScale = v != null ? v : 1.0f);
        targetLineOpacityConfig.signal().subscribe(v -> cachedTargetLineOpacity = v != null ? v : 0.6f);

        // Initialize cache values
        syncConfigCache();

        bindToggle("visualizer", KeyCode.unset);
        bindDialog("visualizerSettings", KeyCode.unset, getSettingDialog(), false);

        Events.on(ClientLoadEvent.class, e -> rebuildBitSet());
        Events.on(WorldLoadEvent.class, e -> clearFlowStates());
        Events.on(ResetEvent.class, e -> clearFlowStates());
        Events.run(Trigger.draw, this::draw);
    }

    public static String turretSettingKey(Block block) {
        return "mindustrytool.features.visualizer.turret." + block.name;
    }

    public boolean isTurretBlock(Block block) {
        return block instanceof BaseTurret && !(block instanceof BuildTurret);
    }

    public void rebuildBitSet() {
        if (Vars.content == null || Vars.content.blocks() == null) {
            return;
        }
        int maxId = 0;
        for (Block block : Vars.content.blocks()) {
            if (block != null && isTurretBlock(block) && block.id > maxId) {
                maxId = block.id;
            }
        }
        BitSet bitSet = new BitSet(maxId + 1);
        for (Block block : Vars.content.blocks()) {
            if (block != null && isTurretBlock(block)) {
                boolean enabled = Core.settings.getBool(turretSettingKey(block), true);
                bitSet.set(block.id, enabled);
            }
        }
        this.turretEnabled = bitSet;
    }

    public Signal<Boolean> getTurretSignal(Block block) {
        Signal<Boolean> signal = turretSignals.get(block.name);
        if (signal == null) {
            boolean initial = Core.settings.getBool(turretSettingKey(block), true);
            signal = Signal.of(initial);
            signal.subscribe(enabled -> {
                Core.settings.put(turretSettingKey(block), enabled);
                if (turretEnabled == null && Vars.content != null && Vars.content.blocks() != null) {
                    rebuildBitSet();
                } else if (turretEnabled != null && block.id >= 0) {
                    turretEnabled.set(block.id, enabled);
                }
            });
            turretSignals.put(block.name, signal);
        }
        return signal;
    }

    public boolean isTurretEnabled(Block block) {
        if (block == null) {
            return false;
        }
        return turretEnabled != null && block.id >= 0 && block.id < turretEnabled.size()
                ? turretEnabled.get(block.id)
                : Core.settings.getBool(turretSettingKey(block), true);
    }

    public void setTurretEnabled(Block block, boolean enabled) {
        if (block == null) {
            return;
        }
        getTurretSignal(block).set(enabled);
    }

    public void setAllTurretsEnabled(boolean enabled) {
        if (Vars.content == null || Vars.content.blocks() == null) {
            return;
        }
        for (Block block : Vars.content.blocks()) {
            if (block != null && isTurretBlock(block)) {
                getTurretSignal(block).set(enabled);
            }
        }
    }

    private void syncConfigCache() {
        Boolean showItems = showItemBridgesConfig.get();
        cachedShowItemBridges = showItems != null ? showItems : true;

        Boolean showDucts = showDuctBridgesConfig.get();
        cachedShowDuctBridges = showDucts != null ? showDucts : true;

        Boolean showLiquids = showLiquidBridgesConfig.get();
        cachedShowLiquidBridges = showLiquids != null ? showLiquids : true;

        Float bScale = bridgeItemScaleConfig.get();
        cachedBridgeItemScale = bScale != null ? bScale : 1.0f;

        Float bOpacity = bridgeOpacityConfig.get();
        cachedBridgeOpacity = bOpacity != null ? bOpacity : 1.0f;

        Boolean showFlow = showBridgeFlowRateConfig.get();
        cachedShowBridgeFlowRate = showFlow != null ? showFlow : true;

        Boolean hideIdle = hideIdleBridgeFlowConfig.get();
        cachedHideIdleBridgeFlow = hideIdle != null ? hideIdle : false;

        Float fScale = bridgeFlowRateScaleConfig.get();
        cachedBridgeFlowRateScale = fScale != null ? fScale : 1.0f;

        Boolean showAmmo = showAmmoBadgeConfig.get();
        cachedShowAmmoBadge = showAmmo != null ? showAmmo : true;

        Boolean showLine = showTargetLineConfig.get();
        cachedShowTargetLine = showLine != null ? showLine : true;

        Boolean lineAlly = targetLineAllyConfig.get();
        cachedTargetLineAlly = lineAlly != null ? lineAlly : true;

        Boolean lineEnemy = targetLineEnemyConfig.get();
        cachedTargetLineEnemy = lineEnemy != null ? lineEnemy : false;

        Boolean onlyShooting = onlyWhenShootingConfig.get();
        cachedOnlyWhenShooting = onlyShooting != null ? onlyShooting : false;

        Float tScale = turretBadgeScaleConfig.get();
        cachedTurretBadgeScale = tScale != null ? tScale : 1.0f;

        Float tOpacity = targetLineOpacityConfig.get();
        cachedTargetLineOpacity = tOpacity != null ? tOpacity : 0.6f;
    }

    public void resetToDefaults() {
        showItemBridgesConfig.reset();
        showDuctBridgesConfig.reset();
        showLiquidBridgesConfig.reset();
        bridgeItemScaleConfig.reset();
        bridgeOpacityConfig.reset();
        showBridgeFlowRateConfig.reset();
        hideIdleBridgeFlowConfig.reset();
        bridgeFlowRateScaleConfig.reset();

        showAmmoBadgeConfig.reset();
        showTargetLineConfig.reset();
        targetLineAllyConfig.reset();
        targetLineEnemyConfig.reset();
        onlyWhenShootingConfig.reset();
        turretBadgeScaleConfig.reset();
        targetLineOpacityConfig.reset();

        for (ObjectMap.Entry<String, Signal<Boolean>> entry : turretSignals.entries()) {
            entry.value.set(true);
        }

        if (Vars.content != null && Vars.content.blocks() != null) {
            for (Block block : Vars.content.blocks()) {
                if (block != null && isTurretBlock(block)) {
                    Core.settings.put(turretSettingKey(block), true);
                }
            }
            rebuildBitSet();
        }

        clearFlowStates();
        syncConfigCache();
    }

    private void clearFlowStates() {
        IntMap.Entries<BridgeFlowState> entries = bridgeFlowStates.entries();
        while (entries.hasNext()) {
            IntMap.Entry<BridgeFlowState> entry = entries.next();
            if (entry.value != null) {
                statePool.add(entry.value);
            }
        }
        bridgeFlowStates.clear();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new VisualizerSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    private void draw() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.ui == null || Core.camera == null) {
            return;
        }

        Core.camera.bounds(viewBounds);

        float z = Draw.z();
        Draw.z(Layer.overlayUI);

        float cx = viewBounds.x + viewBounds.width / 2f;
        float cy = viewBounds.y + viewBounds.height / 2f;
        float range = Math.max(viewBounds.width, viewBounds.height) * 0.75f + 160f;

        // Periodic cleanup of stale bridge flow states to avoid memory retention
        if (Time.time - lastCleanupTime > 300f) {
            lastCleanupTime = Time.time;
            if (bridgeFlowStates.size > 500) {
                staleKeys.clear();
                IntMap.Entries<BridgeFlowState> entries = bridgeFlowStates.entries();
                while (entries.hasNext()) {
                    IntMap.Entry<BridgeFlowState> entry = entries.next();
                    if (entry.value == null || Time.time - entry.value.lastSampleTime > 300f) {
                        staleKeys.add(entry.key);
                    }
                }
                for (int i = 0; i < staleKeys.size; i++) {
                    BridgeFlowState state = bridgeFlowStates.remove(staleKeys.get(i));
                    if (state != null) {
                        statePool.add(state);
                    }
                }
                staleKeys.clear();
            }
        }

        Vars.indexer.eachBlock(null, cx, cy, range, IS_VISUALIZED, buildingDrawer);

        Draw.z(z);
        Draw.reset();
    }

    private void drawBuilding(Building build) {
        if (build == null) {
            return;
        }

        if (build instanceof TurretBuild) {
            drawTurret((TurretBuild) build);
        } else {
            drawBridge(build);
        }
    }

    // ─── Bridge Rendering ──────────────────────────────────────────

    private void drawBridge(Building build) {
        if (build instanceof BufferedItemBridgeBuild) {
            if (cachedShowItemBridges) {
                drawBufferedItemBridge((BufferedItemBridgeBuild) build);
            }
        } else if (build instanceof LiquidBridgeBuild) {
            if (cachedShowLiquidBridges) {
                drawLiquidBridge((LiquidBridgeBuild) build);
            }
        } else if (build instanceof ItemBridgeBuild) {
            if (cachedShowItemBridges) {
                drawItemBridge((ItemBridgeBuild) build);
            }
        } else if (build instanceof DirectionBridgeBuild) {
            DirectionBridgeBuild dirBuild = (DirectionBridgeBuild) build;
            if (build.block instanceof DirectionLiquidBridge) {
                if (cachedShowLiquidBridges) {
                    drawDirectionLiquidBridge(dirBuild);
                }
            } else if (build instanceof DuctBridgeBuild && cachedShowDuctBridges) {
                drawDuctBridge((DuctBridgeBuild) build);
            }
        }

        if (cachedShowBridgeFlowRate) {
            drawBridgeFlow(build);
        }
    }

    private boolean isBridgeVisible(float x1, float y1, float x2, float y2) {
        float minX = x1 < x2 ? x1 : x2;
        float maxX = x1 > x2 ? x1 : x2;
        float minY = y1 < y2 ? y1 : y2;
        float maxY = y1 > y2 ? y1 : y2;
        return maxX + 16f >= viewBounds.x && minX - 16f <= viewBounds.x + viewBounds.width
                && maxY + 16f >= viewBounds.y && minY - 16f <= viewBounds.y + viewBounds.height;
    }

    private boolean isBadgeVisible(float mx, float my) {
        return mx + 48f >= viewBounds.x && mx - 48f <= viewBounds.x + viewBounds.width
                && my + 48f >= viewBounds.y && my - 48f <= viewBounds.y + viewBounds.height;
    }

    private void drawBufferedItemBridge(BufferedItemBridgeBuild build) {
        if (build.link == -1) {
            return;
        }

        Building linked = Vars.world.build(build.link);
        if (linked == null || !isBridgeVisible(build.x, build.y, linked.x, linked.y)) {
            return;
        }

        float x1 = build.x;
        float y1 = build.y;
        float x2 = linked.x;
        float y2 = linked.y;

        boolean drawnFromBuffer = false;

        if (bufferField != null && itemBufferArrayField != null && itemBufferIndexField != null) {
            try {
                ItemBuffer itemBuffer = (ItemBuffer) bufferField.get(build);
                if (itemBuffer != null) {
                    long[] rawBuffer = (long[]) itemBufferArrayField.get(itemBuffer);
                    int count = itemBufferIndexField.getInt(itemBuffer);

                    if (rawBuffer != null && count > 0) {
                        drawnFromBuffer = true;
                        float speed = ((BufferedItemBridge) build.block).speed;
                        float safeSpeed = speed > 0.001f ? speed : 1f;

                        float prevProgress = 1.0f;
                        for (int i = 0; i < count; i++) {
                            long entry = rawBuffer[i];
                            short itemId = TimeItem.item(entry);
                            Item item = Vars.content.item(itemId);
                            if (item == null) {
                                continue;
                            }

                            float entryTime = TimeItem.time(entry);
                            float elapsed = Time.time - entryTime;
                            float rawProgress = Mathf.clamp(elapsed / safeSpeed, 0f, 1f);

                            float progress = i == 0 ? rawProgress : Math.min(rawProgress, prevProgress - 0.12f);
                            progress = Mathf.clamp(progress, 0f, 1f);
                            prevProgress = progress;

                            drawItemAt(x1, y1, x2, y2, item.fullIcon != null ? item.fullIcon : item.uiIcon, progress);
                        }
                    }
                }
            } catch (Throwable t) {
                Log.err("BufferedItemBridge reflection failed", t);
            }
        }

        if (!drawnFromBuffer && build.items != null && !build.items.empty()) {
            drawInventoryFlow(build, linked);
        }
    }

    private void drawItemBridge(ItemBridgeBuild build) {
        if (build.link == -1 || build.items == null) {
            return;
        }

        int total = build.items.total();
        if (total <= 0) {
            return;
        }

        Building linked = Vars.world.build(build.link);
        if (linked == null || !isBridgeVisible(build.x, build.y, linked.x, linked.y)) {
            return;
        }

        ItemBridge block = (ItemBridge) build.block;
        float transportTime = block.transportTime > 0.001f ? block.transportTime : 1f;
        float progress = Mathf.clamp(build.transportCounter / transportTime, 0f, 1f);

        float x1 = build.x;
        float y1 = build.y;
        float x2 = linked.x;
        float y2 = linked.y;

        int itemTypes = build.items.length();
        int index = 0;
        for (int id = 0; id < itemTypes; id++) {
            int amount = build.items.get(id);
            if (amount <= 0) {
                continue;
            }
            Item item = Vars.content.item(id);
            if (item == null) {
                continue;
            }
            TextureRegion icon = item.fullIcon != null ? item.fullIcon : item.uiIcon;
            for (int i = 0; i < amount; i++) {
                float p = Math.max(0f, progress - (index * 0.18f));
                drawItemAt(x1, y1, x2, y2, icon, p);
                index++;
            }
        }
    }

    private void drawDuctBridge(DuctBridgeBuild build) {
        if (build.items == null) {
            return;
        }

        int total = build.items.total();
        if (total <= 0) {
            return;
        }

        Building linked = build.findLink();
        if (linked == null || !isBridgeVisible(build.x, build.y, linked.x, linked.y)) {
            return;
        }

        float speed = ((DuctBridge) build.block).speed;
        float safeSpeed = speed > 0.001f ? speed : 1f;
        float progress = Mathf.clamp(build.progress / safeSpeed, 0f, 1f);

        float x1 = build.x;
        float y1 = build.y;
        float x2 = linked.x;
        float y2 = linked.y;

        int itemTypes = build.items.length();
        int index = 0;
        for (int id = 0; id < itemTypes; id++) {
            int amount = build.items.get(id);
            if (amount <= 0) {
                continue;
            }
            Item item = Vars.content.item(id);
            if (item == null) {
                continue;
            }
            TextureRegion icon = item.fullIcon != null ? item.fullIcon : item.uiIcon;
            for (int i = 0; i < amount; i++) {
                float p = Math.max(0f, progress - (index * 0.2f));
                drawItemAt(x1, y1, x2, y2, icon, p);
                index++;
            }
        }
    }

    private void drawLiquidBridge(LiquidBridgeBuild build) {
        if (build.link == -1 || build.liquids == null) {
            return;
        }

        Building linked = Vars.world.build(build.link);
        if (linked == null || !isBridgeVisible(build.x, build.y, linked.x, linked.y)) {
            return;
        }

        Liquid current = build.liquids.current();
        float amount = build.liquids.currentAmount();
        if (current == null || amount < 0.01f) {
            return;
        }

        if (build.warmup < 0.05f && !build.moved) {
            return;
        }

        drawFluidFlow(build.x, build.y, linked.x, linked.y, current);
    }

    private void drawDirectionLiquidBridge(DirectionBridgeBuild build) {
        Building linked = build.findLink();
        if (linked == null || !isBridgeVisible(build.x, build.y, linked.x, linked.y) || build.liquids == null) {
            return;
        }

        Liquid current = build.liquids.current();
        float amount = build.liquids.currentAmount();
        if (current == null || amount < 0.01f) {
            return;
        }

        drawFluidFlow(build.x, build.y, linked.x, linked.y, current);
    }

    private void drawInventoryFlow(Building build, Building linked) {
        if (build.items == null) {
            return;
        }

        int total = build.items.total();
        if (total <= 0) {
            return;
        }

        int itemTypes = build.items.length();
        int index = total;
        for (int id = 0; id < itemTypes; id++) {
            int amount = build.items.get(id);
            if (amount <= 0) {
                continue;
            }
            Item item = Vars.content.item(id);
            if (item == null) {
                continue;
            }
            TextureRegion icon = item.fullIcon != null ? item.fullIcon : item.uiIcon;
            for (int i = 0; i < amount; i++) {
                float progress = (float) index / total;
                drawItemAt(build.x, build.y, linked.x, linked.y, icon, progress);
                index--;
            }
        }
    }

    private void drawFluidFlow(float x1, float y1, float x2, float y2, Liquid liquid) {
        float dist = Mathf.dst(x1, y1, x2, y2);

        Color fluidColor = liquid.gas && liquid.gasColor != null ? liquid.gasColor : liquid.color;
        if (fluidColor != null) {
            Draw.color(fluidColor, cachedBridgeOpacity * 0.7f);
            Lines.stroke(3.2f * cachedBridgeItemScale);
            Lines.line(x1, y1, x2, y2, false);
        }

        int drops = Math.max(1, (int) (dist / 10f));
        float timeOffset = (Time.time * 0.035f) % 1f;

        for (int d = 0; d < drops; d++) {
            float progress = (timeOffset + (float) d / drops) % 1f;
            drawLiquidAt(x1, y1, x2, y2, liquid, progress);
        }
    }

    private void drawItemAt(float x1, float y1, float x2, float y2, TextureRegion icon, float progress) {
        if (icon == null) {
            return;
        }
        float p = Mathf.clamp(progress, 0f, 1f);
        float lx = Mathf.lerp(x1, x2, p);
        float ly = Mathf.lerp(y1, y2, p);

        float baseSize = 5.5f * cachedBridgeItemScale;

        Draw.color(1f, 1f, 1f, cachedBridgeOpacity);
        Draw.rect(icon, lx, ly, baseSize, baseSize);
    }

    private void drawLiquidAt(float x1, float y1, float x2, float y2, Liquid liquid, float progress) {
        TextureRegion icon = liquid.uiIcon != null ? liquid.uiIcon : liquid.fullIcon;
        if (icon == null) {
            return;
        }

        float p = Mathf.clamp(progress, 0f, 1f);
        float lx = Mathf.lerp(x1, x2, p);
        float ly = Mathf.lerp(y1, y2, p);

        float baseSize = 5.2f * cachedBridgeItemScale;
        float aspect = icon.height > 0 ? (float) icon.width / (float) icon.height : 1.0f;
        float width = aspect >= 1.0f ? baseSize : baseSize * aspect;
        float height = aspect >= 1.0f ? baseSize / aspect : baseSize;

        Draw.color(1f, 1f, 1f, cachedBridgeOpacity);
        Draw.rect(icon, lx, ly, width, height);
    }

    // ─── Bridge Flow Rate Rendering ────────────────────────────────

    public BridgeFlowState getOrCreateBridgeFlowState(int pos) {
        BridgeFlowState state = bridgeFlowStates.get(pos);
        if (state == null) {
            state = statePool.size > 0 ? statePool.pop() : new BridgeFlowState();
            state.reset();
            bridgeFlowStates.put(pos, state);
        }
        return state;
    }

    public float getBridgeFlowRate(Building build) {
        if (build == null || build.tile == null) {
            return 0f;
        }
        BridgeFlowState state = bridgeFlowStates.get(build.pos());
        return state != null ? state.calculateRate() : 0f;
    }

    public void recordBridgeItemTransfer(Building build, float count) {
        if (build == null || build.tile == null) {
            return;
        }
        BridgeFlowState state = getOrCreateBridgeFlowState(build.pos());
        state.addItem(count);
    }

    private void drawBridgeFlow(Building build) {
        if (build.tile == null || (Vars.renderer != null && Vars.renderer.getScale() < 0.6f)) {
            return;
        }

        if (build instanceof BufferedItemBridgeBuild) {
            drawBufferedItemBridgeFlow((BufferedItemBridgeBuild) build);
        } else if (build instanceof LiquidBridgeBuild) {
            drawLiquidBridgeFlow((LiquidBridgeBuild) build);
        } else if (build instanceof ItemBridgeBuild) {
            drawItemBridgeFlow((ItemBridgeBuild) build);
        } else if (build instanceof DirectionBridgeBuild) {
            DirectionBridgeBuild dirBuild = (DirectionBridgeBuild) build;
            if (build.block instanceof DirectionLiquidBridge) {
                drawDirectionLiquidBridgeFlow(dirBuild);
            } else if (build instanceof DuctBridgeBuild) {
                drawDuctBridgeFlow((DuctBridgeBuild) build);
            }
        }
    }

    private void drawBufferedItemBridgeFlow(BufferedItemBridgeBuild build) {
        if (build.link == -1) {
            return;
        }
        Building linked = Vars.world.build(build.link);
        if (linked == null) {
            return;
        }

        float mx = (build.x + linked.x) / 2f;
        float my = (build.y + linked.y) / 2f;

        if (!isBadgeVisible(mx, my)) {
            return;
        }

        Item item = null;
        float rate = 0f;

        if (bufferField != null && itemBufferArrayField != null && itemBufferIndexField != null) {
            try {
                ItemBuffer itemBuffer = (ItemBuffer) bufferField.get(build);
                if (itemBuffer != null) {
                    long[] rawBuffer = (long[]) itemBufferArrayField.get(itemBuffer);
                    int count = itemBufferIndexField.getInt(itemBuffer);
                    if (rawBuffer != null && count > 0) {
                        short itemId = TimeItem.item(rawBuffer[count - 1]);
                        item = Vars.content != null ? Vars.content.item(itemId) : null;
                    }

                    BridgeFlowState state = getOrCreateBridgeFlowState(build.pos());
                    state.step(Time.time);

                    if (state.initialized) {
                        float dt = Time.time - state.lastSampleTime;
                        if (dt > 0.001f && dt < 120f) {
                            if (state.lastCount > 0) {
                                if (count == 0) {
                                    state.addItem(state.lastCount);
                                } else if (rawBuffer != null && rawBuffer[0] != state.lastHeadEntry) {
                                    int popped = Math.max(1, state.lastCount - count);
                                    state.addItem(popped);
                                }
                            }
                        }
                    }

                    state.lastHeadEntry = (count > 0 && rawBuffer != null) ? rawBuffer[0] : 0L;
                    state.lastCount = count;
                    state.lastSampleTime = Time.time;
                    state.initialized = true;

                    rate = state.calculateRate();
                }
            } catch (Throwable ignored) {
            }
        }

        if (item == null && build.items != null && !build.items.empty()) {
            item = build.items.first();
        }

        if (rate < 0.05f && cachedHideIdleBridgeFlow) {
            return;
        }

        BridgeFlowState state = bridgeFlowStates.get(build.pos());
        String text = state != null ? state.formatRate(rate) : formatFlowRate(rate);
        drawBridgeFlowBadge(mx, my, item, state, text, rate);
    }

    private void drawItemBridgeFlow(ItemBridgeBuild build) {
        if (build.link == -1) {
            return;
        }
        Building linked = Vars.world.build(build.link);
        if (linked == null) {
            return;
        }

        float mx = (build.x + linked.x) / 2f;
        float my = (build.y + linked.y) / 2f;

        if (!isBadgeVisible(mx, my)) {
            return;
        }

        Item item = build.items != null && !build.items.empty() ? build.items.first() : null;

        BridgeFlowState state = getOrCreateBridgeFlowState(build.pos());
        state.step(Time.time);

        if (state.initialized) {
            float dt = Time.time - state.lastSampleTime;
            if (dt > 0.001f && dt < 120f) {
                float transportTime = ((ItemBridge) build.block).transportTime;
                float safeTime = transportTime > 0.001f ? transportTime : 1f;

                boolean cycleCompleted = build.transportCounter < state.lastCounter
                        || (state.lastCounter < safeTime && build.transportCounter >= safeTime);
                boolean isActivelyMoving = (build.moved || build.wasMoved) && build.warmup > 0.01f;

                if (cycleCompleted && isActivelyMoving && item != null) {
                    state.addItem(1f);
                }
            }
        }

        state.lastCounter = build.transportCounter;
        state.lastSampleTime = Time.time;
        state.initialized = true;

        float rate = state.calculateRate();

        if (rate < 0.05f && cachedHideIdleBridgeFlow) {
            return;
        }

        String text = state.formatRate(rate);
        drawBridgeFlowBadge(mx, my, item, state, text, rate);
    }

    private void drawDuctBridgeFlow(DuctBridgeBuild build) {
        Building linked = build.findLink();
        if (linked == null) {
            return;
        }

        float mx = (build.x + linked.x) / 2f;
        float my = (build.y + linked.y) / 2f;

        if (!isBadgeVisible(mx, my)) {
            return;
        }

        Item item = build.items != null && !build.items.empty() ? build.items.first() : null;

        BridgeFlowState state = getOrCreateBridgeFlowState(build.pos());
        state.step(Time.time);

        float speed = ((DuctBridge) build.block).speed;
        float safeSpeed = speed > 0.001f ? speed : 1f;

        if (state.initialized) {
            float dt = Time.time - state.lastSampleTime;
            if (dt > 0.001f && dt < 120f) {
                boolean canAccept = linked.items != null && linked.block != null
                        && linked.items.total() < linked.block.itemCapacity;

                if (canAccept && item != null && build.efficiency > 0.01f) {
                    float deltaProgress = build.progress - state.lastProgress;
                    if (deltaProgress < 0f) {
                        float cycles = 1f + Mathf.floor((state.lastProgress - build.progress) / safeSpeed);
                        deltaProgress += cycles * safeSpeed;
                    }
                    if (deltaProgress > 0f) {
                        float itemsMoved = deltaProgress / safeSpeed;
                        state.addItem(itemsMoved);
                    }
                }
            }
        }

        state.lastProgress = build.progress;
        state.lastSampleTime = Time.time;
        state.initialized = true;

        float rate = state.calculateRate();

        if (rate < 0.05f && cachedHideIdleBridgeFlow) {
            return;
        }

        String text = state.formatRate(rate);
        drawBridgeFlowBadge(mx, my, item, state, text, rate);
    }

    private void drawLiquidBridgeFlow(LiquidBridgeBuild build) {
        if (build.link == -1) {
            return;
        }
        Building linked = Vars.world.build(build.link);
        if (linked == null) {
            return;
        }
        boolean isFlowing = build.warmup >= 0.05f && (build.wasMoved || build.moved);
        drawLiquidBridgeFlowInternal(build, linked, isFlowing);
    }

    private void drawDirectionLiquidBridgeFlow(DirectionBridgeBuild build) {
        Building linked = build.findLink();
        if (linked == null) {
            return;
        }
        boolean isFlowing = build.efficiency > 0.05f && build.liquids != null && build.liquids.currentAmount() > 0.01f;
        drawLiquidBridgeFlowInternal(build, linked, isFlowing);
    }

    private void drawLiquidBridgeFlowInternal(Building build, Building linked, boolean isFlowing) {
        float mx = (build.x + linked.x) / 2f;
        float my = (build.y + linked.y) / 2f;

        if (!isBadgeVisible(mx, my)) {
            return;
        }

        Liquid current = build.liquids != null ? build.liquids.current() : null;
        if (current == null && linked.liquids != null) {
            current = linked.liquids.current();
        }

        float rate = calculateLiquidFlowRate(build, linked, current, isFlowing);

        if (rate < 0.05f && cachedHideIdleBridgeFlow) {
            return;
        }

        BridgeFlowState state = getOrCreateBridgeFlowState(build.pos());
        String text = state.formatRate(rate);
        drawBridgeFlowBadge(mx, my, current, state, text, rate);
    }

    public float calculateLiquidFlowRate(Building build, @Nullable Building linked, @Nullable Liquid current, boolean isFlowing) {
        if (!isFlowing || current == null || linked == null || build.liquids == null || linked.liquids == null
                || linked.block == null || !linked.block.hasLiquids || build.block == null || !build.block.hasLiquids) {
            return 0f;
        }

        if (build.block.liquidCapacity <= 0.001f || linked.block.liquidCapacity <= 0.001f) {
            return 0f;
        }

        if (build.liquids.get(current) <= 0.001f) {
            return 0f;
        }

        if (!linked.acceptLiquid(build, current)) {
            return 0f;
        }

        float sourceFrac = (build.liquids.get(current) / build.block.liquidCapacity) * build.block.liquidPressure;
        float destFrac = linked.liquids.get(current) / linked.block.liquidCapacity;

        if (sourceFrac <= destFrac) {
            return 0f;
        }

        float diff = sourceFrac - destFrac;
        float flow = Math.min(diff * build.block.liquidCapacity, build.liquids.get(current));
        flow = Math.min(flow, linked.block.liquidCapacity - linked.liquids.get(current));

        if (flow <= 0.0001f) {
            return 0f;
        }

        float eff = build.efficiency > 0f ? build.efficiency : 1f;
        return flow * 60f * build.timeScale() * eff;
    }

    public static String formatFlowRate(float rate) {
        if (rate <= 0.001f) {
            return ZERO_RATE_TEXT;
        }
        if (rate >= 9.95f) {
            int r = Math.round(rate);
            return r <= 100 ? INT_RATE_CACHE[r] : OVER_100_RATE_TEXT;
        }
        int idx = Math.round(rate * 10f);
        if (idx < 0) {
            return ZERO_RATE_TEXT;
        }
        if (idx >= 100) {
            return INT_RATE_CACHE[10];
        }
        return TENTHS_RATE_CACHE[idx];
    }

    private void drawBridgeFlowBadge(float mx, float my, @Nullable UnlockableContent content,
            @Nullable BridgeFlowState state, String text, float rate) {
        float s = cachedBridgeFlowRateScale;
        float fontScale = 0.22f * s;
        float iconSize = 5.5f * s;
        TextureRegion icon = content != null ? (content.uiIcon != null ? content.uiIcon : content.fullIcon) : null;

        float rawWidth;
        float rawHeight;
        if (state != null) {
            if (state.layoutDirty || state.cachedTextWidth <= 0f) {
                glyphLayout.setText(Fonts.outline, text);
                state.cachedTextWidth = glyphLayout.width;
                state.cachedTextHeight = glyphLayout.height;
                state.layoutDirty = false;
            }
            rawWidth = state.cachedTextWidth;
            rawHeight = state.cachedTextHeight;
        } else {
            glyphLayout.setText(Fonts.outline, text);
            rawWidth = glyphLayout.width;
            rawHeight = glyphLayout.height;
        }

        float textWidth = rawWidth * fontScale;
        float textHeight = rawHeight * fontScale;

        float padX = 2.5f * s;
        float padY = 1.8f * s;
        float badgeWidth = (icon != null ? iconSize + 1.5f * s : 0f) + textWidth + padX * 2f;
        float badgeHeight = Math.max(iconSize, textHeight) + padY * 2f;

        // Background pill
        Draw.color(0f, 0f, 0f, 0.7f * cachedBridgeOpacity);
        Fill.rect(mx, my, badgeWidth, badgeHeight);

        // Icon
        if (icon != null) {
            float iconX = mx - badgeWidth / 2f + padX + iconSize / 2f;
            Draw.color(Color.white, cachedBridgeOpacity);
            Draw.rect(icon, iconX, my, iconSize, iconSize);
        }

        // Text
        Color textColor = rate <= 0.001f ? Color.lightGray : (rate >= 10f ? Pal.heal : Pal.accent);
        colorScratch.set(textColor);
        colorScratch.a = cachedBridgeOpacity;

        float textX = icon != null ? mx - badgeWidth / 2f + padX + iconSize + 1.5f * s : mx - textWidth / 2f;
        float textY = my + textHeight / 2f;
        Fonts.outline.draw(text, textX, textY, colorScratch, fontScale, false, Align.left);
        Draw.reset();
    }

    // ─── Turret Rendering ──────────────────────────────────────────

    private void drawTurret(TurretBuild turret) {
        if (!isTurretEnabled(turret.block)) {
            return;
        }

        float sizePx = turret.block.size * Vars.tilesize;
        float half = sizePx / 2f;
        boolean turretOnScreen = turret.x + half >= viewBounds.x && turret.x - half <= viewBounds.x + viewBounds.width
                && turret.y + half >= viewBounds.y && turret.y - half <= viewBounds.y + viewBounds.height;

        boolean isAlly = Vars.player == null || turret.team == Vars.player.team();

        if (cachedShowAmmoBadge && isAlly && turretOnScreen) {
            drawAmmoBadge(turret);
        }

        if (cachedShowTargetLine) {
            boolean shouldDrawLine = isAlly ? cachedTargetLineAlly : cachedTargetLineEnemy;
            if (shouldDrawLine) {
                drawTargetLine(turret, isAlly);
            }
        }
    }

    private void drawAmmoBadge(TurretBuild turret) {
        UnlockableContent ammoContent = turret.getAmmoContent();
        if (ammoContent == null) {
            return;
        }

        TextureRegion icon = ammoContent.uiIcon != null ? ammoContent.uiIcon : ammoContent.fullIcon;
        if (icon == null) {
            return;
        }

        float sizePx = turret.block.size * Vars.tilesize;
        float half = sizePx / 2f;
        float badgeSize = calculateBadgeSize(turret.block.size);

        // Position badge at bottom-right of turret footprint with adaptive margin
        float margin = Math.min(1.2f, turret.block.size * 0.5f);
        float bx = turret.x + half - badgeSize * 0.5f - margin;
        float by = turret.y - half + badgeSize * 0.5f + margin;

        float gaugeRadius = badgeSize * 0.62f;

        // Dark circular backing for contrast against busy backgrounds
        Draw.color(0f, 0f, 0f, 0.65f);
        Fill.circle(bx, by, badgeSize * 0.55f);

        // Background gauge track
        Draw.color(0f, 0f, 0f, 0.7f);
        Lines.stroke(1.2f * cachedTurretBadgeScale);
        Lines.circle(bx, by, gaugeRadius);

        // Real in-game ammo fill gauge
        float ammoFrac = calculateAmmoFraction(turret);
        if (ammoFrac > 0.001f) {
            if (ammoFrac < 0.25f) {
                float pulse = Mathf.absin(Time.time, 6f, 1f);
                colorScratch.set(Pal.remove).lerp(Color.white, pulse * 0.35f);
            } else {
                colorScratch.set(Pal.accent);
            }
            Draw.color(colorScratch);
            Lines.stroke(1.2f * cachedTurretBadgeScale);
            Lines.arc(bx, by, gaugeRadius, ammoFrac, 90f);
        }

        // Render official ammo icon
        Draw.color(Color.white);
        Draw.rect(icon, bx, by, badgeSize, badgeSize);
        Draw.reset();
    }

    public float calculateAmmoFraction(TurretBuild turret) {
        if (turret == null) {
            return 1f;
        }
        float ammoFrac;
        if (turret instanceof ItemTurretBuild) {
            ItemTurretBuild itemTurret = (ItemTurretBuild) turret;
            ItemTurret block = (ItemTurret) itemTurret.block;
            ammoFrac = block.maxAmmo > 0 ? (float) itemTurret.totalAmmo / block.maxAmmo : itemTurret.getAmmoFraction();
        } else if (turret.liquids != null && turret.block.liquidCapacity > 0) {
            ammoFrac = turret.liquids.currentAmount() / turret.block.liquidCapacity;
        } else {
            ammoFrac = turret.getAmmoFraction();
        }
        return Mathf.clamp(ammoFrac, 0f, 1f);
    }

    public float calculateBadgeSize(int blockSize) {
        float baseBadgeSize = Math.min(11f, 2.4f + blockSize * 1.5f);
        return baseBadgeSize * cachedTurretBadgeScale;
    }

    private void drawTargetLine(TurretBuild turret, boolean isAlly) {
        Posc target = turret.target;
        if (target == null) {
            return;
        }

        if (target instanceof Healthc && !((Healthc) target).isValid()) {
            return;
        }

        if (target instanceof Unit && ((Unit) target).dead()) {
            return;
        }

        if (cachedOnlyWhenShooting && !turret.isShooting()) {
            return;
        }

        float tx = target.getX();
        float ty = target.getY();

        float minX = turret.x < tx ? turret.x : tx;
        float maxX = turret.x > tx ? turret.x : tx;
        float minY = turret.y < ty ? turret.y : ty;
        float maxY = turret.y > ty ? turret.y : ty;
        if (maxX < viewBounds.x || minX > viewBounds.x + viewBounds.width
                || maxY < viewBounds.y || minY > viewBounds.y + viewBounds.height) {
            return;
        }

        Color baseColor = isAlly
                ? (turret.team != null ? turret.team.color : Pal.accent)
                : Pal.remove;
        colorScratch.set(baseColor != null ? baseColor : Pal.accent);
        colorScratch.a = cachedTargetLineOpacity;

        Drawf.dashLine(colorScratch, turret.x, turret.y, tx, ty);

        boolean targetOnScreen = tx + 16f >= viewBounds.x && tx - 16f <= viewBounds.x + viewBounds.width
                && ty + 16f >= viewBounds.y && ty - 16f <= viewBounds.y + viewBounds.height;

        if (targetOnScreen) {
            // Real target health indicator around reticle
            if (target instanceof Healthc) {
                float healthFrac = getTargetHealthFraction(target);
                float reticleRadius = 8.5f;

                // Background subtle track
                Draw.color(0f, 0f, 0f, colorScratch.a * 0.6f);
                Lines.stroke(1.2f);
                Lines.circle(tx, ty, reticleRadius);

                // Real target health arc
                if (healthFrac > 0.001f) {
                    if (healthFrac > 0.5f) {
                        healthColorScratch.set(Pal.health);
                    } else if (healthFrac > 0.25f) {
                        healthColorScratch.set(Pal.accent);
                    } else {
                        healthColorScratch.set(Pal.remove);
                    }
                    healthColorScratch.a = colorScratch.a;
                    Draw.color(healthColorScratch);
                    Lines.stroke(1.2f);
                    Lines.arc(tx, ty, reticleRadius, healthFrac, 90f);
                }
            }

            Drawf.target(tx, ty, 6f, colorScratch);
        }

        Draw.reset();
    }

    public float getTargetHealthFraction(Posc target) {
        if (target instanceof Healthc) {
            return Mathf.clamp(((Healthc) target).healthf(), 0f, 1f);
        }
        return 1f;
    }

    /**
     * Internal state per active bridge to track real-time flow rate using a rolling window.
     */
    public static class BridgeFlowState implements Poolable {
        public static final int BUCKET_COUNT = 6;
        public static final float BUCKET_TICKS = 10f; // 6 buckets * 10 ticks = 60 ticks (1.0 sec)

        public final float[] buckets = new float[BUCKET_COUNT];
        public int currentBucket = 0;
        public float lastBucketTime = 0f;
        public float lastSampleTime = 0f;

        // Bridge specific state for delta detection
        public boolean initialized = false;
        public long lastHeadEntry = 0L;
        public int lastCount = 0;
        public float lastCounter = 0f;
        public float lastProgress = 0f;

        // Formatting and layout cache
        public float lastFormattedRate = -1f;
        public String cachedRateString = ZERO_RATE_TEXT;
        public float cachedTextWidth = 0f;
        public float cachedTextHeight = 0f;
        public boolean layoutDirty = true;

        @Override
        public void reset() {
            for (int i = 0; i < BUCKET_COUNT; i++) {
                buckets[i] = 0f;
            }
            currentBucket = 0;
            lastBucketTime = 0f;
            lastSampleTime = 0f;
            initialized = false;
            lastHeadEntry = 0L;
            lastCount = 0;
            lastCounter = 0f;
            lastProgress = 0f;
            lastFormattedRate = -1f;
            cachedRateString = ZERO_RATE_TEXT;
            cachedTextWidth = 0f;
            cachedTextHeight = 0f;
            layoutDirty = true;
        }

        public void step(float time) {
            if (lastBucketTime <= 0f) {
                lastBucketTime = time;
                return;
            }
            float elapsed = time - lastBucketTime;
            if (elapsed >= BUCKET_TICKS) {
                int shift = (int) (elapsed / BUCKET_TICKS);
                if (shift >= BUCKET_COUNT) {
                    for (int i = 0; i < BUCKET_COUNT; i++) {
                        buckets[i] = 0f;
                    }
                    currentBucket = 0;
                } else {
                    for (int s = 0; s < shift; s++) {
                        currentBucket = (currentBucket + 1) % BUCKET_COUNT;
                        buckets[currentBucket] = 0f;
                    }
                }
                lastBucketTime += shift * BUCKET_TICKS;
            }
        }

        public void addItem(float count) {
            if (count > 0f) {
                buckets[currentBucket] += count;
            }
        }

        public float calculateRate() {
            float total = 0f;
            for (int i = 0; i < BUCKET_COUNT; i++) {
                total += buckets[i];
            }
            return total;
        }

        public String formatRate(float rate) {
            if (rate <= 0.001f) {
                if (cachedRateString != ZERO_RATE_TEXT) {
                    cachedRateString = ZERO_RATE_TEXT;
                    layoutDirty = true;
                }
                return ZERO_RATE_TEXT;
            }
            if (Math.abs(rate - lastFormattedRate) < 0.05f) {
                return cachedRateString;
            }
            lastFormattedRate = rate;
            String newRateString = formatFlowRate(rate);
            if (!newRateString.equals(cachedRateString)) {
                cachedRateString = newRateString;
                layoutDirty = true;
            }
            return cachedRateString;
        }
    }
}

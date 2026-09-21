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
import arc.graphics.g2d.Lines;
import arc.graphics.g2d.TextureRegion;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.struct.ObjectMap;
import arc.struct.ObjectMap.Entry;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.ctype.UnlockableContent;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.game.EventType.Trigger;
import mindustry.gen.Building;
import mindustry.gen.Healthc;
import mindustry.gen.Posc;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.type.Item;
import mindustry.type.Liquid;
import mindustry.world.Block;
import mindustry.world.blocks.defense.BuildTurret;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.defense.turrets.Turret.TurretBuild;
import mindustry.world.blocks.distribution.BufferedItemBridge;
import mindustry.world.blocks.distribution.BufferedItemBridge.BufferedItemBridgeBuild;
import mindustry.world.blocks.distribution.DirectionBridge.DirectionBridgeBuild;
import mindustry.world.blocks.distribution.DuctBridge;
import mindustry.world.blocks.distribution.DuctBridge.DuctBridgeBuild;
import mindustry.world.blocks.distribution.ItemBridge;
import mindustry.world.blocks.distribution.ItemBridge.ItemBridgeBuild;
import mindustry.gen.TimeItem;
import mindustry.world.ItemBuffer;
import mindustry.world.blocks.distribution.DirectionLiquidBridge;
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
 * - Turret tactical data: loaded ammo badges and target pointer lines with reticle indicators.
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
    private @Nullable VisualizerSettingsDialog settingsDialog;

    // Per-turret enabled bitset indexed by block.id for zero-allocation O(1) checks during 60 FPS draw
    private @Nullable BitSet turretEnabled;
    private final ObjectMap<String, Signal<Boolean>> turretSignals = new ObjectMap<>();

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
                .enabledByDefault(true)
                .quickAccess(true)
                .build());

        config = configGroup();

        // Bridges
        showItemBridgesConfig = config.boolValue("show-item-bridges", true);
        showDuctBridgesConfig = config.boolValue("show-duct-bridges", true);
        showLiquidBridgesConfig = config.boolValue("show-liquid-bridges", true);
        bridgeItemScaleConfig = config.floatValue("bridge-item-scale", 1.0f);
        bridgeOpacityConfig = config.floatValue("bridge-opacity", 1.0f);

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

        showAmmoBadgeConfig.reset();
        showTargetLineConfig.reset();
        targetLineAllyConfig.reset();
        targetLineEnemyConfig.reset();
        onlyWhenShootingConfig.reset();
        turretBadgeScaleConfig.reset();
        targetLineOpacityConfig.reset();

        for (Entry<String, Signal<Boolean>> entry : turretSignals.entries()) {
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

        syncConfigCache();
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
    }

    private void drawBufferedItemBridge(BufferedItemBridgeBuild build) {
        if (build.link == -1) {
            return;
        }

        Building linked = Vars.world.build(build.link);
        if (linked == null) {
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
        if (linked == null) {
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
        if (linked == null) {
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
        if (linked == null) {
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
        if (linked == null || build.liquids == null) {
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

    // ─── Turret Rendering ──────────────────────────────────────────

    private void drawTurret(TurretBuild turret) {
        if (!isTurretEnabled(turret.block)) {
            return;
        }
        boolean isAlly = Vars.player == null || turret.team == Vars.player.team();

        if (cachedShowAmmoBadge && isAlly) {
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

        // Dark circular backing for contrast against busy backgrounds
        Draw.color(0f, 0f, 0f, 0.65f);
        Fill.circle(bx, by, badgeSize * 0.55f);

        // Render official ammo icon
        Draw.color(Color.white);
        Draw.rect(icon, bx, by, badgeSize, badgeSize);
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

        Color baseColor = isAlly
                ? (turret.team != null ? turret.team.color : Pal.accent)
                : Pal.remove;
        colorScratch.set(baseColor != null ? baseColor : Pal.accent);
        colorScratch.a = cachedTargetLineOpacity;

        float tx = target.getX();
        float ty = target.getY();

        Drawf.dashLine(colorScratch, turret.x, turret.y, tx, ty);
        Drawf.target(tx, ty, 6f, colorScratch);
    }
}

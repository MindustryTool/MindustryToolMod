package mindustrytool.features.rangedisplay;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.geom.Rect;
import arc.struct.ObjectMap;
import arc.struct.ObjectMap.Entry;
import arc.util.Nullable;
import java.util.BitSet;
import mindustry.Vars;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Layer;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.BuildTurret;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.ForceProjector.ForceBuild;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.defense.OverdriveProjector.OverdriveBuild;
import mindustry.world.blocks.defense.RegenProjector;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.defense.turrets.Turret.TurretBuild;
import mindustry.world.blocks.distribution.MassDriver;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.rangedisplay.ui.RangeDisplaySettingsDialog;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;
import solim.reactive.Signal;

/**
 * Feature responsible for rendering real-time range visualizations for turrets,
 * units, support blocks (menders, overdrives, mass drivers, shields), and enemy drop zones.
 *
 * Highly optimized for smooth performance and zero GC allocations during the frame render loop:
 * - Zoom threshold gating (skips rendering when zoomed out past threshold, matching HealthBar).
 * - Dynamic max range calculation and bounded camera frustum queries (replaces arbitrary 1400f radius).
 * - O(1) BitSet lookups in block predicate without multi-class instanceof checks.
 * - Team-targeted queries to avoid iterating opposing teams when toggled off.
 * - Viewport frustum culling.
 * - Frame-cached configuration snapshots via signal peek to avoid reactive overhead in hot loops.
 * - Draw state caching to eliminate redundant color and stroke changes.
 */
public class RangeDisplayFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> zoomThresholdConfig;
    public final ConfigValue<Boolean> drawTurretRangeAllyConfig;
    public final ConfigValue<Boolean> drawTurretRangeEnemyConfig;
    public final ConfigValue<Boolean> drawUnitRangeAllyConfig;
    public final ConfigValue<Boolean> drawUnitRangeEnemyConfig;
    public final ConfigValue<Boolean> drawBlockRangeAllyConfig;
    public final ConfigValue<Boolean> drawBlockRangeEnemyConfig;
    public final ConfigValue<Boolean> drawSpawnerRangeConfig;
    public final ConfigValue<Boolean> dashedConfig;

    private final Cons<Unit> unitDrawer = this::drawUnitRange;
    private final Cons<Building> buildingDrawer = this::drawBuildingRange;
    private final Boolf<Building> buildingPredicate = this::filterBuilding;

    private final Rect viewBounds = new Rect();
    private final Color colorScratch = new Color();
    private final Color lastColor = new Color();
    private boolean hasLastColor = false;
    private @Nullable RangeDisplaySettingsDialog settingsDialog;

    // Per-block enabled bitset indexed by block.id for zero-allocation O(1) checks during draw
    private @Nullable BitSet blockEnabled;
    private @Nullable BitSet rangeBlockMask;
    private float cachedMaxBlockRange = 550f;
    private final ObjectMap<String, Signal<Boolean>> blockSignals = new ObjectMap<>();

    // Per-frame scratch values read once per draw frame
    private float frameOpacity = 1.0f;
    private float frameZoomThreshold = 0.5f;
    private boolean frameDrawTurretRangeAlly = true;
    private boolean frameDrawTurretRangeEnemy = true;
    private boolean frameDrawUnitRangeAlly = true;
    private boolean frameDrawUnitRangeEnemy = true;
    private boolean frameDrawBlockRangeAlly = true;
    private boolean frameDrawBlockRangeEnemy = true;
    private boolean frameDrawSpawnerRange = true;
    private boolean frameDashed = true;
    private @Nullable Team framePlayerTeam;

    public RangeDisplayFeature() {
        super(FeatureMetadata.builder()
                .id("range-display")
                .icon(FileIcon.of("range-display.png"))
                .order(5)
                .quickAccess(true)
                .enabledByDefault(true)
                .development(false)
                .build());

        config = configGroup();
        opacityConfig = config.floatValue("opacity", 1.0f);
        zoomThresholdConfig = config.floatValue("zoom-threshold", 0.5f);
        drawTurretRangeAllyConfig = config.boolValue("draw-turret-range-ally", true);
        drawTurretRangeEnemyConfig = config.boolValue("draw-turret-range-enemy", true);
        drawUnitRangeAllyConfig = config.boolValue("draw-unit-range-ally", true);
        drawUnitRangeEnemyConfig = config.boolValue("draw-unit-range-enemy", true);
        drawBlockRangeAllyConfig = config.boolValue("draw-block-range-ally", true);
        drawBlockRangeEnemyConfig = config.boolValue("draw-block-range-enemy", true);
        drawSpawnerRangeConfig = config.boolValue("draw-spawner-range", true);
        dashedConfig = config.boolValue("dashed", true);

        bindToggle("rangeDisplay", KeyCode.unset);
        bindDialog("rangeDisplaySettings", KeyCode.unset, getSettingDialog(), false);

        Events.run(Trigger.draw, this::draw);
    }

    public static String blockSettingKey(Block block) {
        return "mindustrytool.features.range-display.block." + block.name;
    }

    public boolean isTurretBlock(Block block) {
        return block instanceof BaseTurret && !(block instanceof BuildTurret);
    }

    public boolean isSupportBlock(Block block) {
        return block instanceof OverdriveProjector
                || block instanceof MassDriver
                || block instanceof BuildTurret
                || block instanceof MendProjector
                || block instanceof RegenProjector
                || block instanceof ForceProjector;
    }

    public boolean isRangeBlock(Block block) {
        return isTurretBlock(block) || isSupportBlock(block);
    }

    public float getBlockBaseRange(Block block) {
        if (block instanceof BaseTurret) {
            return ((BaseTurret) block).range;
        } else if (block instanceof OverdriveProjector) {
            OverdriveProjector op = (OverdriveProjector) block;
            return op.range + op.phaseRangeBoost;
        } else if (block instanceof MassDriver) {
            return ((MassDriver) block).range;
        } else if (block instanceof BuildTurret) {
            return ((BuildTurret) block).range;
        } else if (block instanceof MendProjector) {
            return ((MendProjector) block).range;
        } else if (block instanceof RegenProjector) {
            return ((RegenProjector) block).range * Vars.tilesize;
        } else if (block instanceof ForceProjector) {
            ForceProjector fp = (ForceProjector) block;
            return fp.radius + fp.phaseRadiusBoost;
        }
        return 0f;
    }

    public void rebuildBitSet() {
        if (Vars.content == null || Vars.content.blocks() == null) {
            return;
        }

        int max = Vars.content.blocks().size;
        BitSet bitSet = new BitSet(Math.max(max, 256));
        BitSet mask = new BitSet(Math.max(max, 256));
        float maxRange = 500f;

        for (Block block : Vars.content.blocks()) {
            if (block != null && isRangeBlock(block)) {
                mask.set(block.id);
                boolean enabled = Core.settings.getBool(blockSettingKey(block), true);
                bitSet.set(block.id, enabled);

                float r = getBlockBaseRange(block);
                if (r > maxRange) {
                    maxRange = r;
                }
            }
        }
        this.blockEnabled = bitSet;
        this.rangeBlockMask = mask;
        this.cachedMaxBlockRange = maxRange + 120f;
    }

    public Signal<Boolean> getBlockSignal(Block block) {
        Signal<Boolean> signal = blockSignals.get(block.name);
        if (signal == null) {
            boolean initial = Core.settings.getBool(blockSettingKey(block), true);
            signal = Signal.of(initial);
            signal.subscribe(enabled -> {
                Core.settings.put(blockSettingKey(block), enabled);
                if (blockEnabled == null && Vars.content != null && Vars.content.blocks() != null) {
                    rebuildBitSet();
                } else if (blockEnabled != null && block.id >= 0) {
                    blockEnabled.set(block.id, enabled);
                }
            });
            blockSignals.put(block.name, signal);
        }
        return signal;
    }

    public boolean isBlockEnabled(Block block) {
        if (block == null) {
            return false;
        }
        return blockEnabled != null && block.id >= 0 && block.id < blockEnabled.size()
                ? blockEnabled.get(block.id)
                : Core.settings.getBool(blockSettingKey(block), true);
    }

    public void setBlockEnabled(Block block, boolean enabled) {
        if (block == null) {
            return;
        }
        getBlockSignal(block).set(enabled);
    }

    public void setCategoryEnabled(boolean isTurret, boolean enabled) {
        if (Vars.content == null || Vars.content.blocks() == null) {
            return;
        }
        for (Block block : Vars.content.blocks()) {
            if (block == null) {
                continue;
            }
            if (isTurret ? isTurretBlock(block) : isSupportBlock(block)) {
                getBlockSignal(block).set(enabled);
            }
        }
    }

    public void resetToDefaults() {
        opacityConfig.reset();
        zoomThresholdConfig.reset();
        drawTurretRangeAllyConfig.reset();
        drawTurretRangeEnemyConfig.reset();
        drawUnitRangeAllyConfig.reset();
        drawUnitRangeEnemyConfig.reset();
        drawBlockRangeAllyConfig.reset();
        drawBlockRangeEnemyConfig.reset();
        drawSpawnerRangeConfig.reset();
        dashedConfig.reset();

        for (Entry<String, Signal<Boolean>> entry : blockSignals.entries()) {
            entry.value.set(true);
        }

        if (Vars.content != null && Vars.content.blocks() != null) {
            for (Block b : Vars.content.blocks()) {
                if (b != null && isRangeBlock(b)) {
                    Core.settings.remove(blockSettingKey(b));
                }
            }
        }
        rebuildBitSet();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> settingsDialog != null ? settingsDialog : (settingsDialog = new RangeDisplaySettingsDialog(this));
    }

    private boolean filterBuilding(Building b) {
        if (b == null || !b.isValid() || b.team == Team.derelict || b.block == null) {
            return false;
        }
        int id = b.block.id;
        if (rangeBlockMask == null || id < 0 || id >= rangeBlockMask.size() || !rangeBlockMask.get(id)) {
            return false;
        }
        if (blockEnabled != null && !blockEnabled.get(id)) {
            return false;
        }
        boolean isAlly = framePlayerTeam != null && b.team == framePlayerTeam;
        if (isAlly) {
            return frameDrawTurretRangeAlly || frameDrawBlockRangeAlly;
        } else {
            return frameDrawTurretRangeEnemy || frameDrawBlockRangeEnemy;
        }
    }

    private void draw() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.ui == null
                || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown || Core.camera == null) {
            return;
        }

        Float zt = zoomThresholdConfig.signal().peek();
        frameZoomThreshold = zt != null ? zt : 0.5f;

        float zoom = Vars.renderer != null ? Vars.renderer.getScale() : 1f;
        if (frameZoomThreshold > 0.01f && zoom < frameZoomThreshold) {
            return;
        }

        if (blockEnabled == null && Vars.content != null && Vars.content.blocks() != null) {
            rebuildBitSet();
        }

        Float op = opacityConfig.signal().peek();
        frameOpacity = op != null ? op : 1.0f;
        if (frameOpacity <= 0.001f) {
            return;
        }

        Boolean dta = drawTurretRangeAllyConfig.signal().peek();
        frameDrawTurretRangeAlly = dta != null ? dta : true;

        Boolean dte = drawTurretRangeEnemyConfig.signal().peek();
        frameDrawTurretRangeEnemy = dte != null ? dte : true;

        Boolean dua = drawUnitRangeAllyConfig.signal().peek();
        frameDrawUnitRangeAlly = dua != null ? dua : true;

        Boolean due = drawUnitRangeEnemyConfig.signal().peek();
        frameDrawUnitRangeEnemy = due != null ? due : true;

        Boolean dba = drawBlockRangeAllyConfig.signal().peek();
        frameDrawBlockRangeAlly = dba != null ? dba : true;

        Boolean dbe = drawBlockRangeEnemyConfig.signal().peek();
        frameDrawBlockRangeEnemy = dbe != null ? dbe : true;

        Boolean dsp = drawSpawnerRangeConfig.signal().peek();
        frameDrawSpawnerRange = dsp != null ? dsp : true;

        Boolean dsh = dashedConfig.signal().peek();
        frameDashed = dsh != null ? dsh : true;

        framePlayerTeam = Vars.player != null ? Vars.player.team() : null;

        Core.camera.bounds(viewBounds);

        float z = Draw.z();
        Draw.z(Layer.overlayUI);
        Lines.stroke(1f);
        hasLastColor = false;

        // 1. Spawner Drop Zones
        if (frameDrawSpawnerRange && Vars.spawner != null && Vars.spawner.getSpawns() != null
                && Vars.state.rules != null) {
            float dropRadius = Vars.state.rules.dropZoneRadius;
            Color spawnerColor = Vars.state.rules.waveTeam != null ? Vars.state.rules.waveTeam.color : Color.scarlet;
            for (Tile tile : Vars.spawner.getSpawns()) {
                if (tile == null) {
                    continue;
                }
                float x = tile.worldx();
                float y = tile.worldy();
                if (x + dropRadius >= viewBounds.x && x - dropRadius <= viewBounds.x + viewBounds.width
                        && y + dropRadius >= viewBounds.y && y - dropRadius <= viewBounds.y + viewBounds.height) {
                    drawCircle(x, y, dropRadius, spawnerColor);
                }
            }
        }

        // 2. Unit Weapon Ranges
        if (frameDrawUnitRangeAlly || frameDrawUnitRangeEnemy) {
            float margin = 440f;
            float cx = Core.camera.position.x;
            float cy = Core.camera.position.y;
            float cw = Core.camera.width;
            float ch = Core.camera.height;
            Groups.unit.intersect(cx - cw / 2f - margin, cy - ch / 2f - margin, cw + margin * 2f, ch + margin * 2f,
                    unitDrawer);
        }

        // 3. Turret and Support Block Ranges
        boolean hasAlly = frameDrawTurretRangeAlly || frameDrawBlockRangeAlly;
        boolean hasEnemy = frameDrawTurretRangeEnemy || frameDrawBlockRangeEnemy;
        if (hasAlly || hasEnemy) {
            float cx = Core.camera.position.x;
            float cy = Core.camera.position.y;
            float cw = Core.camera.width;
            float ch = Core.camera.height;
            float radius = Math.max(cw, ch) * 0.75f + cachedMaxBlockRange;
            if (hasAlly && !hasEnemy && framePlayerTeam != null) {
                Vars.indexer.eachBlock(framePlayerTeam, cx, cy, radius, buildingPredicate, buildingDrawer);
            } else {
                Vars.indexer.eachBlock(null, cx, cy, radius, buildingPredicate, buildingDrawer);
            }
        }

        Draw.z(z);
        hasLastColor = false;
        Draw.reset();
    }

    private void drawUnitRange(Unit unit) {
        if (!unit.isValid()) {
            return;
        }

        if (Vars.player != null && unit == Vars.player.unit()) {
            return;
        }

        boolean isAlly = framePlayerTeam != null && unit.team == framePlayerTeam;
        if (isAlly ? !frameDrawUnitRangeAlly : !frameDrawUnitRangeEnemy) {
            return;
        }

        float range = unit.range();
        if (range <= 0f) {
            return;
        }

        float x = unit.x;
        float y = unit.y;
        if (x + range < viewBounds.x || x - range > viewBounds.x + viewBounds.width
                || y + range < viewBounds.y || y - range > viewBounds.y + viewBounds.height) {
            return;
        }

        drawCircle(x, y, range, unit.team.color);
    }

    private void drawBuildingRange(Building build) {
        boolean isTurret = isTurretBlock(build.block);
        boolean isAlly = framePlayerTeam != null && build.team == framePlayerTeam;

        if (isTurret) {
            if (isAlly ? !frameDrawTurretRangeAlly : !frameDrawTurretRangeEnemy) {
                return;
            }
        } else {
            if (isAlly ? !frameDrawBlockRangeAlly : !frameDrawBlockRangeEnemy) {
                return;
            }
        }

        float range = 0f;
        boolean circle = true;

        if (build instanceof TurretBuild) {
            range = ((TurretBuild) build).range();
        } else if (build.block instanceof BaseTurret) {
            range = ((BaseTurret) build.block).range;
        } else if (build instanceof OverdriveBuild && build.block instanceof OverdriveProjector) {
            OverdriveBuild ob = (OverdriveBuild) build;
            OverdriveProjector op = (OverdriveProjector) build.block;
            range = op.range + ob.phaseHeat * op.phaseRangeBoost;
        } else if (build.block instanceof MassDriver) {
            range = ((MassDriver) build.block).range;
        } else if (build.block instanceof BuildTurret) {
            range = ((BuildTurret) build.block).range;
        } else if (build.block instanceof MendProjector) {
            range = ((MendProjector) build.block).range;
        } else if (build.block instanceof RegenProjector) {
            range = ((RegenProjector) build.block).range * Vars.tilesize;
            circle = false;
        } else if (build instanceof ForceBuild && build.block instanceof ForceProjector) {
            range = ((ForceBuild) build).realRadius();
        }

        if (range <= 0f) {
            return;
        }

        float x = build.x;
        float y = build.y;
        if (x + range < viewBounds.x || x - range > viewBounds.x + viewBounds.width
                || y + range < viewBounds.y || y - range > viewBounds.y + viewBounds.height) {
            return;
        }

        Color color;
        if (isTurret) {
            boolean canShoot = !(build instanceof TurretBuild) || ((TurretBuild) build).hasAmmo();
            color = canShoot ? build.team.color : Color.gray;
        } else {
            color = build.team.color;
        }

        if (circle) {
            drawCircle(x, y, range, color);
        } else {
            drawSquare(x, y, range, color);
        }
    }

    private void applyColor(Color color) {
        colorScratch.set(color).a(frameOpacity);
        if (!hasLastColor || !lastColor.equals(colorScratch)) {
            Draw.color(colorScratch);
            lastColor.set(colorScratch);
            hasLastColor = true;
        }
    }

    private void drawCircle(float x, float y, float range, Color color) {
        applyColor(color);
        if (frameDashed) {
            Lines.dashCircle(x, y, range);
        } else {
            Lines.circle(x, y, range);
        }
    }

    private void drawSquare(float x, float y, float range, Color color) {
        applyColor(color);
        Lines.rect(x - range / 2f, y - range / 2f, range, range);
    }
}

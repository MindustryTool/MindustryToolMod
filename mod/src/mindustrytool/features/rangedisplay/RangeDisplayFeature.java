package mindustrytool.features.rangedisplay;

import arc.Core;
import arc.Events;
import arc.func.Boolf;
import arc.func.Cons;
import arc.func.Prov;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Lines;
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
import mindustry.graphics.Drawf;
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
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.power.LightBlock;
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
 * Optimized for high performance and zero GC allocations during the frame render loop:
 * - Pre-allocated drawer delegates and predicates.
 * - Viewport frustum culling.
 * - Granular per-block range toggles backed by BitSet indexed by block.id.
 * - Frame-cached configuration snapshots to avoid reactive overhead in hot loops.
 */
public class RangeDisplayFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Float> opacityConfig;
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

    private static final Boolf<Building> RANGE_BUILDING_PREDICATE = b -> {
        if (b == null || !b.isValid() || b.team == Team.derelict || b.block == null) {
            return false;
        }
        return b.block instanceof BaseTurret
                || b.block instanceof OverdriveProjector
                || b.block instanceof MassDriver
                || b.block instanceof BuildTurret
                || b.block instanceof MendProjector
                || b.block instanceof RegenProjector
                || b.block instanceof ForceProjector;
    };

    private final Rect viewBounds = new Rect();
    private final Color colorScratch = new Color();
    private @Nullable RangeDisplaySettingsDialog settingsDialog;

    // Per-block enabled bitset indexed by block.id for zero-allocation O(1) checks during 60 FPS draw
    private @Nullable BitSet blockEnabled;
    private final ObjectMap<String, Signal<Boolean>> blockSignals = new ObjectMap<>();

    // Per-frame scratch values read once per draw frame
    private float frameOpacity = 1.0f;
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
        drawTurretRangeAllyConfig = config.boolValue("draw-turret-range-ally", true);
        drawTurretRangeEnemyConfig = config.boolValue("draw-turret-range-enemy", true);
        drawUnitRangeAllyConfig = config.boolValue("draw-unit-range-ally", true);
        drawUnitRangeEnemyConfig = config.boolValue("draw-unit-range-enemy", true);
        drawBlockRangeAllyConfig = config.boolValue("draw-block-range-ally", true);
        drawBlockRangeEnemyConfig = config.boolValue("draw-block-range-enemy", true);
        drawSpawnerRangeConfig = config.boolValue("draw-spawner-range", true);
        dashedConfig = config.boolValue("dashed", true);

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

    public void rebuildBitSet() {
        if (Vars.content == null || Vars.content.blocks() == null) {
            return;
        }

        int max = Vars.content.blocks().size;
        BitSet bitSet = new BitSet(Math.max(max, 256));

        for (Block block : Vars.content.blocks()) {
            if (block != null && isRangeBlock(block)) {
                boolean enabled = Core.settings.getBool(blockSettingKey(block), true);
                bitSet.set(block.id, enabled);
            }
        }
        this.blockEnabled = bitSet;
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

    private void draw() {
        if (!isEnabled() || Vars.state == null || !Vars.state.isGame() || Vars.ui == null
                || Vars.ui.hudfrag == null || !Vars.ui.hudfrag.shown || Core.camera == null) {
            return;
        }

        if (blockEnabled == null && Vars.content != null && Vars.content.blocks() != null) {
            rebuildBitSet();
        }

        Float op = opacityConfig.get();
        frameOpacity = op != null ? op : 1.0f;
        if (frameOpacity <= 0.001f) {
            return;
        }

        Boolean dta = drawTurretRangeAllyConfig.get();
        frameDrawTurretRangeAlly = dta != null ? dta : true;

        Boolean dte = drawTurretRangeEnemyConfig.get();
        frameDrawTurretRangeEnemy = dte != null ? dte : true;

        Boolean dua = drawUnitRangeAllyConfig.get();
        frameDrawUnitRangeAlly = dua != null ? dua : true;

        Boolean due = drawUnitRangeEnemyConfig.get();
        frameDrawUnitRangeEnemy = due != null ? due : true;

        Boolean dba = drawBlockRangeAllyConfig.get();
        frameDrawBlockRangeAlly = dba != null ? dba : true;

        Boolean dbe = drawBlockRangeEnemyConfig.get();
        frameDrawBlockRangeEnemy = dbe != null ? dbe : true;

        Boolean dsp = drawSpawnerRangeConfig.get();
        frameDrawSpawnerRange = dsp != null ? dsp : true;

        Boolean dsh = dashedConfig.get();
        frameDashed = dsh != null ? dsh : true;

        framePlayerTeam = Vars.player != null ? Vars.player.team() : null;

        Core.camera.bounds(viewBounds);

        float z = Draw.z();
        Draw.z(Layer.overlayUI);

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
            float margin = 1000f;
            float cx = Core.camera.position.x;
            float cy = Core.camera.position.y;
            float cw = Core.camera.width;
            float ch = Core.camera.height;
            Groups.unit.intersect(cx - cw / 2f - margin, cy - ch / 2f - margin, cw + margin * 2f, ch + margin * 2f,
                    unitDrawer);
        }

        // 3. Turret and Support Block Ranges
        if (frameDrawTurretRangeAlly || frameDrawTurretRangeEnemy
                || frameDrawBlockRangeAlly || frameDrawBlockRangeEnemy) {
            float cx = Core.camera.position.x;
            float cy = Core.camera.position.y;
            float cw = Core.camera.width;
            float ch = Core.camera.height;
            float radius = Math.max(cw, ch) * 0.75f + 1400f;
            Vars.indexer.eachBlock(null, cx, cy, radius, RANGE_BUILDING_PREDICATE, buildingDrawer);
        }

        Draw.z(z);
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

        if (unit.x + range < viewBounds.x || unit.x - range > viewBounds.x + viewBounds.width
                || unit.y + range < viewBounds.y || unit.y - range > viewBounds.y + viewBounds.height) {
            return;
        }

        drawCircle(unit.x, unit.y, range, unit.team.color);
    }

    private void drawBuildingRange(Building build) {
        if (build == null || !build.isValid() || build.team == Team.derelict || build.block == null) {
            return;
        }

        if (blockEnabled != null && build.block.id >= 0 && !blockEnabled.get(build.block.id)) {
            return;
        }

        if (build.block instanceof LightBlock || build.block instanceof LogicBlock) {
            return;
        }

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

    private void drawCircle(float x, float y, float range, Color color) {
        colorScratch.set(color).a(frameOpacity);
        Lines.stroke(1f, colorScratch);
        if (frameDashed) {
            Lines.dashCircle(x, y, range);
        } else {
            Lines.circle(x, y, range);
        }
    }

    private void drawSquare(float x, float y, float range, Color color) {
        colorScratch.set(color).a(frameOpacity);
        Lines.stroke(1f, colorScratch);
        if (frameDashed) {
            Drawf.dashSquareBasic(x, y, range);
        } else {
            Lines.rect(x - range / 2f, y - range / 2f, range, range);
        }
    }
}

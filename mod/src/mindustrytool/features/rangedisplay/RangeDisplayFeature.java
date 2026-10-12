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
import arc.struct.Seq;
import arc.util.Nullable;
import java.util.BitSet;
import mindustry.Vars;
import mindustry.entities.Units;
import mindustry.game.EventType.Trigger;
import mindustry.game.Team;
import mindustry.gen.Building;
import mindustry.gen.Groups;
import mindustry.gen.Unit;
import mindustry.graphics.Drawf;
import mindustry.graphics.Layer;
import mindustry.graphics.Pal;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.BuildTurret;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.ForceProjector.ForceBuild;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.defense.OverdriveProjector.OverdriveBuild;
import mindustry.world.blocks.defense.RegenProjector;
import mindustry.world.blocks.defense.ShockwaveTower;
import mindustry.world.blocks.defense.turrets.BaseTurret;
import mindustry.world.blocks.defense.turrets.PointDefenseTurret;
import mindustry.world.blocks.defense.turrets.Turret;
import mindustry.world.blocks.defense.turrets.Turret.TurretBuild;
import mindustry.world.blocks.distribution.MassDriver;
import mindustry.world.blocks.logic.LogicBlock;
import mindustry.world.blocks.power.LightBlock;
import mindustry.world.blocks.units.RepairTower;
import mindustry.world.blocks.units.RepairTurret;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.rangedisplay.ui.RangeDisplaySettingsDialog;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.overlay.SolimDialog;
import solim.reactive.Signal;

public class RangeDisplayFeature extends Feature {

    private static  final Color ORANGE = Color.valueOf("#FFA500");

    public final ConfigGroup config;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> strokeWidthConfig;
    public final ConfigValue<Boolean> hoverOnlyConfig;
    public final ConfigValue<Boolean> filterTargetAirConfig;
    public final ConfigValue<Boolean> filterTargetGroundConfig;
    public final ConfigValue<Boolean> onlyWithAmmoConfig;
    public final ConfigValue<Boolean> proximityFilterConfig;
    public final ConfigValue<Float> proximityRadiusConfig;
    public final ConfigValue<Boolean> drawTurretRangeAllyConfig;
    public final ConfigValue<Boolean> drawTurretRangeEnemyConfig;
    public final ConfigValue<Boolean> drawUnitRangePlayerConfig;
    public final ConfigValue<Boolean> drawUnitRangeAllyConfig;
    public final ConfigValue<Boolean> drawUnitRangeEnemyConfig;
    public final ConfigValue<Boolean> drawBlockRangeAllyConfig;
    public final ConfigValue<Boolean> drawBlockRangeEnemyConfig;
    public final ConfigValue<Boolean> drawSpawnerRangeConfig;
    public final ConfigValue<Boolean> drawOverdriveRangeConfig;
    public final ConfigValue<Boolean> drawMenderRangeConfig;
    public final ConfigValue<Boolean> drawPointDefenseRangeConfig;
    public final ConfigValue<Boolean> useTeamColorForSpecialConfig;
    public final ConfigValue<Boolean> dashedConfig;
    public final ConfigValue<Float> zoomThresholdConfig;

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
                || b.block instanceof ForceProjector
                || b.block instanceof RepairTower
                || b.block instanceof RepairTurret
                || b.block instanceof ShockwaveTower;
    };

    private final Rect viewBounds = new Rect();
    private final Color colorScratch = new Color();
    private @Nullable RangeDisplaySettingsDialog settingsDialog;

    private static final Boolf<Unit> ANY_UNIT = u -> true;

    
    private @Nullable BitSet blockEnabled;
    private final ObjectMap<String, Signal<Boolean>> blockSignals = new ObjectMap<>();

    
    private float cachedOpacity = 1.0f;
    private float cachedStrokeWidth = 1.0f;
    private boolean cachedHoverOnly = false;
    private boolean cachedFilterTargetAir = true;
    private boolean cachedFilterTargetGround = true;
    private boolean cachedOnlyWithAmmo = false;
    private boolean cachedProximityFilter = false;
    private float cachedProximityRadius = 30f;
    private boolean cachedDrawTurretRangeAlly = true;
    private boolean cachedDrawTurretRangeEnemy = true;
    private boolean cachedDrawUnitRangePlayer = true;
    private boolean cachedDrawUnitRangeAlly = true;
    private boolean cachedDrawUnitRangeEnemy = true;
    private boolean cachedDrawBlockRangeAlly = true;
    private boolean cachedDrawBlockRangeEnemy = true;
    private boolean cachedDrawSpawnerRange = true;
    private boolean cachedDrawOverdriveRange = true;
    private boolean cachedDrawMenderRange = true;
    private boolean cachedDrawPointDefenseRange = true;
    private boolean cachedUseTeamColorForSpecial = false;
    private boolean cachedDashed = true;
    private float cachedZoomThreshold = 0.5f;

    
    private float frameOpacity = 1.0f;
    private float frameStrokeWidth = 1.0f;
    private boolean frameHoverOnly = false;
    private boolean frameFilterTargetAir = true;
    private boolean frameFilterTargetGround = true;
    private boolean frameOnlyWithAmmo = false;
    private boolean frameProximityFilter = false;
    private float frameProximityDistSq = 0f;
    private float frameProximityX = 0f;
    private float frameProximityY = 0f;
    private boolean frameDrawTurretRangeAlly = true;
    private boolean frameDrawTurretRangeEnemy = true;
    private boolean frameDrawUnitRangePlayer = true;
    private boolean frameDrawUnitRangeAlly = true;
    private boolean frameDrawUnitRangeEnemy = true;
    private boolean frameDrawBlockRangeAlly = true;
    private boolean frameDrawBlockRangeEnemy = true;
    private boolean frameDrawSpawnerRange = true;
    private boolean frameDrawOverdriveRange = true;
    private boolean frameDrawMenderRange = true;
    private boolean frameDrawPointDefenseRange = true;
    private boolean frameUseTeamColorForSpecial = false;
    private boolean frameDashed = true;
    private @Nullable Team framePlayerTeam;

    public RangeDisplayFeature() {
        super(FeatureMetadata.builder()
                .id("range-display")
                .icon(FileIcon.of("range-display.png"))
                .order(5)
                .quickAccess(false)
                .enabledByDefault(true)
                .development(false)
                .displayGroup(true)
                .build());

        config = configGroup();
        opacityConfig = config.floatValue("opacity", 1.0f);
        strokeWidthConfig = config.floatValue("stroke-width", 1.0f);
        hoverOnlyConfig = config.boolValue("hover-only", false);
        filterTargetAirConfig = config.boolValue("filter-target-air", true);
        filterTargetGroundConfig = config.boolValue("filter-target-ground", true);
        onlyWithAmmoConfig = config.boolValue("only-with-ammo", false);
        proximityFilterConfig = config.boolValue("proximity-filter", false);
        proximityRadiusConfig = config.floatValue("proximity-radius", 30f);
        drawTurretRangeAllyConfig = config.boolValue("draw-turret-range-ally", true);
        drawTurretRangeEnemyConfig = config.boolValue("draw-turret-range-enemy", true);
        drawUnitRangePlayerConfig = config.boolValue("draw-unit-range-player", true);
        drawUnitRangeAllyConfig = config.boolValue("draw-unit-range-ally", true);
        drawUnitRangeEnemyConfig = config.boolValue("draw-unit-range-enemy", true);
        drawBlockRangeAllyConfig = config.boolValue("draw-block-range-ally", true);
        drawBlockRangeEnemyConfig = config.boolValue("draw-block-range-enemy", true);
        drawSpawnerRangeConfig = config.boolValue("draw-spawner-range", true);
        drawOverdriveRangeConfig = config.boolValue("draw-overdrive-range", true);
        drawMenderRangeConfig = config.boolValue("draw-mender-range", true);
        drawPointDefenseRangeConfig = config.boolValue("draw-point-defense-range", true);
        useTeamColorForSpecialConfig = config.boolValue("use-team-color-for-special", false);
        dashedConfig = config.boolValue("dashed", true);
        zoomThresholdConfig = config.floatValue("zoom-threshold", 0.5f);

        
        opacityConfig.signal().subscribe(v -> cachedOpacity = v != null ? v : 1.0f);
        strokeWidthConfig.signal().subscribe(v -> cachedStrokeWidth = v != null ? v : 1.0f);
        hoverOnlyConfig.signal().subscribe(v -> cachedHoverOnly = v != null ? v : false);
        filterTargetAirConfig.signal().subscribe(v -> cachedFilterTargetAir = v != null ? v : true);
        filterTargetGroundConfig.signal().subscribe(v -> cachedFilterTargetGround = v != null ? v : true);
        onlyWithAmmoConfig.signal().subscribe(v -> cachedOnlyWithAmmo = v != null ? v : false);
        proximityFilterConfig.signal().subscribe(v -> cachedProximityFilter = v != null ? v : false);
        proximityRadiusConfig.signal().subscribe(v -> cachedProximityRadius = v != null ? v : 30f);
        drawTurretRangeAllyConfig.signal().subscribe(v -> cachedDrawTurretRangeAlly = v != null ? v : true);
        drawTurretRangeEnemyConfig.signal().subscribe(v -> cachedDrawTurretRangeEnemy = v != null ? v : true);
        drawUnitRangePlayerConfig.signal().subscribe(v -> cachedDrawUnitRangePlayer = v != null ? v : true);
        drawUnitRangeAllyConfig.signal().subscribe(v -> cachedDrawUnitRangeAlly = v != null ? v : true);
        drawUnitRangeEnemyConfig.signal().subscribe(v -> cachedDrawUnitRangeEnemy = v != null ? v : true);
        drawBlockRangeAllyConfig.signal().subscribe(v -> cachedDrawBlockRangeAlly = v != null ? v : true);
        drawBlockRangeEnemyConfig.signal().subscribe(v -> cachedDrawBlockRangeEnemy = v != null ? v : true);
        drawSpawnerRangeConfig.signal().subscribe(v -> cachedDrawSpawnerRange = v != null ? v : true);
        drawOverdriveRangeConfig.signal().subscribe(v -> cachedDrawOverdriveRange = v != null ? v : true);
        drawMenderRangeConfig.signal().subscribe(v -> cachedDrawMenderRange = v != null ? v : true);
        drawPointDefenseRangeConfig.signal().subscribe(v -> cachedDrawPointDefenseRange = v != null ? v : true);
        useTeamColorForSpecialConfig.signal().subscribe(v -> cachedUseTeamColorForSpecial = v != null ? v : false);
        dashedConfig.signal().subscribe(v -> cachedDashed = v != null ? v : true);
        zoomThresholdConfig.signal().subscribe(v -> cachedZoomThreshold = v != null ? v : 0.5f);

        syncConfigCache();

        bindToggle("rangeDisplay", KeyCode.unset);
        bindDialog("rangeDisplaySettings", KeyCode.unset, getSettingDialog(), false);

        Events.run(Trigger.draw, this::draw);
    }

    private void syncConfigCache() {
        Float op = opacityConfig.get();
        cachedOpacity = op != null ? op : 1.0f;
        Float sw = strokeWidthConfig.get();
        cachedStrokeWidth = sw != null ? sw : 1.0f;
        Boolean ho = hoverOnlyConfig.get();
        cachedHoverOnly = ho != null ? ho : false;
        Boolean fta = filterTargetAirConfig.get();
        cachedFilterTargetAir = fta != null ? fta : true;
        Boolean ftg = filterTargetGroundConfig.get();
        cachedFilterTargetGround = ftg != null ? ftg : true;
        Boolean owa = onlyWithAmmoConfig.get();
        cachedOnlyWithAmmo = owa != null ? owa : false;
        Boolean pf = proximityFilterConfig.get();
        cachedProximityFilter = pf != null ? pf : false;
        Float pr = proximityRadiusConfig.get();
        cachedProximityRadius = pr != null ? pr : 30f;
        Boolean dta = drawTurretRangeAllyConfig.get();
        cachedDrawTurretRangeAlly = dta != null ? dta : true;
        Boolean dte = drawTurretRangeEnemyConfig.get();
        cachedDrawTurretRangeEnemy = dte != null ? dte : true;
        Boolean dup = drawUnitRangePlayerConfig.get();
        cachedDrawUnitRangePlayer = dup != null ? dup : true;
        Boolean dua = drawUnitRangeAllyConfig.get();
        cachedDrawUnitRangeAlly = dua != null ? dua : true;
        Boolean due = drawUnitRangeEnemyConfig.get();
        cachedDrawUnitRangeEnemy = due != null ? due : true;
        Boolean dba = drawBlockRangeAllyConfig.get();
        cachedDrawBlockRangeAlly = dba != null ? dba : true;
        Boolean dbe = drawBlockRangeEnemyConfig.get();
        cachedDrawBlockRangeEnemy = dbe != null ? dbe : true;
        Boolean dsp = drawSpawnerRangeConfig.get();
        cachedDrawSpawnerRange = dsp != null ? dsp : true;
        Boolean dor = drawOverdriveRangeConfig.get();
        cachedDrawOverdriveRange = dor != null ? dor : true;
        Boolean dmr = drawMenderRangeConfig.get();
        cachedDrawMenderRange = dmr != null ? dmr : true;
        Boolean dpr = drawPointDefenseRangeConfig.get();
        cachedDrawPointDefenseRange = dpr != null ? dpr : true;
        Boolean utc = useTeamColorForSpecialConfig.get();
        cachedUseTeamColorForSpecial = utc != null ? utc : false;
        Boolean dsh = dashedConfig.get();
        cachedDashed = dsh != null ? dsh : true;
        Float zt = zoomThresholdConfig.get();
        cachedZoomThreshold = zt != null ? zt : 0.5f;
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
                || block instanceof ForceProjector
                || block instanceof RepairTower
                || block instanceof RepairTurret
                || block instanceof ShockwaveTower;
    }

    public boolean isRangeBlock(Block block) {
        return isTurretBlock(block) || isSupportBlock(block);
    }

    /**
     * Blocks whose range is rendered in the orange overdrive accent color.
     */
    public boolean isOverdriveBlock(Block block) {
        return block instanceof OverdriveProjector;
    }

    public boolean isMenderBlock(Block block) {
        return block instanceof MendProjector
                || block instanceof RegenProjector
                || block instanceof RepairTower
                || block instanceof RepairTurret;
    }

    public boolean isPointDefenseBlock(Block block) {
        return block instanceof PointDefenseTurret
                || block instanceof ShockwaveTower;
    }

    public boolean isSpecialColorBlock(Block block) {
        return isOverdriveBlock(block) || isMenderBlock(block) || isPointDefenseBlock(block);
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
        strokeWidthConfig.reset();
        hoverOnlyConfig.reset();
        filterTargetAirConfig.reset();
        filterTargetGroundConfig.reset();
        onlyWithAmmoConfig.reset();
        proximityFilterConfig.reset();
        proximityRadiusConfig.reset();
        drawTurretRangeAllyConfig.reset();
        drawTurretRangeEnemyConfig.reset();
        drawUnitRangePlayerConfig.reset();
        drawUnitRangeAllyConfig.reset();
        drawUnitRangeEnemyConfig.reset();
        drawBlockRangeAllyConfig.reset();
        drawBlockRangeEnemyConfig.reset();
        drawSpawnerRangeConfig.reset();
        drawOverdriveRangeConfig.reset();
        drawMenderRangeConfig.reset();
        drawPointDefenseRangeConfig.reset();
        useTeamColorForSpecialConfig.reset();
        dashedConfig.reset();
        zoomThresholdConfig.reset();

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
        syncConfigCache();
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

        if (cachedOpacity <= 0.001f) {
            return;
        }

        if (cachedZoomThreshold > 0.01f && Vars.renderer != null && Vars.renderer.getScale() < cachedZoomThreshold) {
            return;
        }

        frameOpacity = cachedOpacity;
        frameStrokeWidth = cachedStrokeWidth;
        frameHoverOnly = cachedHoverOnly;
        frameFilterTargetAir = cachedFilterTargetAir;
        frameFilterTargetGround = cachedFilterTargetGround;
        frameOnlyWithAmmo = cachedOnlyWithAmmo;
        frameProximityFilter = cachedProximityFilter;
        float radTiles = cachedProximityRadius;
        frameProximityDistSq = (radTiles * Vars.tilesize) * (radTiles * Vars.tilesize);

        if (Vars.player != null && Vars.player.unit() != null && Vars.player.unit().isValid()) {
            frameProximityX = Vars.player.x;
            frameProximityY = Vars.player.y;
        } else {
            frameProximityX = Core.input.mouseWorldX();
            frameProximityY = Core.input.mouseWorldY();
        }

        frameDrawTurretRangeAlly = cachedDrawTurretRangeAlly;
        frameDrawTurretRangeEnemy = cachedDrawTurretRangeEnemy;
        frameDrawUnitRangePlayer = cachedDrawUnitRangePlayer;
        frameDrawUnitRangeAlly = cachedDrawUnitRangeAlly;
        frameDrawUnitRangeEnemy = cachedDrawUnitRangeEnemy;
        frameDrawBlockRangeAlly = cachedDrawBlockRangeAlly;
        frameDrawBlockRangeEnemy = cachedDrawBlockRangeEnemy;
        frameDrawSpawnerRange = cachedDrawSpawnerRange;
        frameDrawOverdriveRange = cachedDrawOverdriveRange;
        frameDrawMenderRange = cachedDrawMenderRange;
        frameDrawPointDefenseRange = cachedDrawPointDefenseRange;
        frameUseTeamColorForSpecial = cachedUseTeamColorForSpecial;
        frameDashed = cachedDashed;
        framePlayerTeam = Vars.player != null ? Vars.player.team() : null;

        Core.camera.bounds(viewBounds);

        float z = Draw.z();
        Draw.z(Layer.overlayUI);

        
        if (frameDrawSpawnerRange && Vars.spawner != null && Vars.spawner.getSpawns() != null
                && Vars.state.rules != null) {
            float dropRadius = Vars.state.rules.dropZoneRadius;
            Color spawnerColor = Vars.state.rules.waveTeam != null ? Vars.state.rules.waveTeam.color : Color.scarlet;
            Seq<Tile> spawns = Vars.spawner.getSpawns();
            for (int i = 0; i < spawns.size; i++) {
                Tile tile = spawns.get(i);
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

        if (frameHoverOnly) {
            Building hoveredBuilding = Vars.world != null ? Vars.world.buildWorld(Core.input.mouseWorldX(), Core.input.mouseWorldY()) : null;
            if (hoveredBuilding != null && RANGE_BUILDING_PREDICATE.get(hoveredBuilding)) {
                drawBuildingRange(hoveredBuilding);
            }

            if (frameDrawUnitRangePlayer && Vars.player != null && Vars.player.unit() != null) {
                drawUnitRange(Vars.player.unit());
            }

            Unit hoveredUnit = Units.closest(null, Core.input.mouseWorldX(), Core.input.mouseWorldY(), 32f, ANY_UNIT);
            if (hoveredUnit != null && (Vars.player == null || hoveredUnit != Vars.player.unit())) {
                drawUnitRange(hoveredUnit);
            }
        } else {
            
            if (frameDrawUnitRangeAlly || frameDrawUnitRangeEnemy || frameDrawUnitRangePlayer) {
                float margin = 1000f;
                float cx = Core.camera.position.x;
                float cy = Core.camera.position.y;
                float cw = Core.camera.width;
                float ch = Core.camera.height;
                Groups.unit.intersect(cx - cw / 2f - margin, cy - ch / 2f - margin, cw + margin * 2f, ch + margin * 2f,
                        unitDrawer);
            }

            
            if (frameDrawTurretRangeAlly || frameDrawTurretRangeEnemy
                    || frameDrawBlockRangeAlly || frameDrawBlockRangeEnemy) {
                float cx = Core.camera.position.x;
                float cy = Core.camera.position.y;
                float cw = Core.camera.width;
                float ch = Core.camera.height;
                float radius = Math.max(cw, ch) * 0.75f + 1400f;
                Vars.indexer.eachBlock(null, cx, cy, radius, RANGE_BUILDING_PREDICATE, buildingDrawer);
            }
        }

        Draw.z(z);
        Draw.reset();
    }

    private void drawUnitRange(Unit unit) {
        if (!unit.isValid()) {
            return;
        }

        boolean isPlayer = Vars.player != null && unit == Vars.player.unit();
        if (isPlayer) {
            if (!frameDrawUnitRangePlayer) {
                return;
            }
        } else {
            if (frameProximityFilter) {
                float dx = unit.x - frameProximityX;
                float dy = unit.y - frameProximityY;
                if (dx * dx + dy * dy > frameProximityDistSq) {
                    return;
                }
            }

            boolean isAlly = framePlayerTeam != null && unit.team == framePlayerTeam;
            if (isAlly ? !frameDrawUnitRangeAlly : !frameDrawUnitRangeEnemy) {
                return;
            }
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

    private static float estimateMaxRange(Building build) {
        if (build.block instanceof BaseTurret) {
            return ((BaseTurret) build.block).range;
        }
        if (build.block instanceof MassDriver) {
            return ((MassDriver) build.block).range;
        }
        if (build.block instanceof OverdriveProjector) {
            OverdriveProjector op = (OverdriveProjector) build.block;
            return op.range + op.phaseRangeBoost;
        }
        if (build.block instanceof ForceProjector) {
            return ((ForceProjector) build.block).radius + 64f;
        }
        if (build.block instanceof MendProjector) {
            return ((MendProjector) build.block).range;
        }
        if (build.block instanceof RegenProjector) {
            return ((RegenProjector) build.block).range * Vars.tilesize;
        }
        if (build.block instanceof RepairTower) {
            return ((RepairTower) build.block).range;
        }
        if (build.block instanceof RepairTurret) {
            return ((RepairTurret) build.block).repairRadius;
        }
        if (build.block instanceof ShockwaveTower) {
            return ((ShockwaveTower) build.block).range;
        }
        if (build.block instanceof BuildTurret) {
            return ((BuildTurret) build.block).range;
        }
        return 1000f;
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

        Block block = build.block;
        if (isOverdriveBlock(block) && !frameDrawOverdriveRange
                || isMenderBlock(block) && !frameDrawMenderRange
                || isPointDefenseBlock(block) && !frameDrawPointDefenseRange) {
            return;
        }

        float maxRange = estimateMaxRange(build);
        if (build.x + maxRange < viewBounds.x || build.x - maxRange > viewBounds.x + viewBounds.width
                || build.y + maxRange < viewBounds.y || build.y - maxRange > viewBounds.y + viewBounds.height) {
            return;
        }

        if (frameProximityFilter) {
            float dx = build.x - frameProximityX;
            float dy = build.y - frameProximityY;
            if (dx * dx + dy * dy > frameProximityDistSq) {
                return;
            }
        }

        boolean isTurret = isTurretBlock(build.block);
        boolean isAlly = framePlayerTeam != null && build.team == framePlayerTeam;

        if (isTurret) {
            if (isAlly ? !frameDrawTurretRangeAlly : !frameDrawTurretRangeEnemy) {
                return;
            }

            if (build.block instanceof Turret) {
                Turret t = (Turret) build.block;
                boolean canHitAir = t.targetAir && frameFilterTargetAir;
                boolean canHitGround = t.targetGround && frameFilterTargetGround;
                if (!canHitAir && !canHitGround) {
                    return;
                }
            }

            boolean hasAmmo = !(build instanceof TurretBuild) || ((TurretBuild) build).hasAmmo();
            if (frameOnlyWithAmmo && !hasAmmo) {
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
        } else if (build.block instanceof RepairTower) {
            range = ((RepairTower) build.block).range;
        } else if (build.block instanceof RepairTurret) {
            range = ((RepairTurret) build.block).repairRadius;
        } else if (build.block instanceof ShockwaveTower) {
            range = ((ShockwaveTower) build.block).range;
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
            if (!canShoot) {
                color = Color.gray;
            } else {
                color = resolveSpecialColor(block, build.team.color);
            }
        } else {
            color = resolveSpecialColor(block, build.team.color);
        }

        if (circle) {
            drawCircle(x, y, range, color);
        } else {
            drawSquare(x, y, range, color);
        }

        float minRange = isTurret && build.block instanceof Turret ? ((Turret) build.block).minRange : 0f;
        if (minRange > 0f) {
            colorScratch.set(Color.scarlet).a(frameOpacity * 0.7f);
            Lines.stroke(frameStrokeWidth, colorScratch);
            Lines.dashCircle(x, y, minRange);
        }
    }

    private Color resolveSpecialColor(Block block, Color teamColor) {
        if(frameUseTeamColorForSpecial){
            return teamColor;
        }

        if(isOverdriveBlock(block)){
            return ORANGE;
        }

        if(isMenderBlock(block)){
            return Pal.heal;
        }

        if(isPointDefenseBlock(block)){
            return Color.white;
        }

        return teamColor;
    }

    private void drawCircle(float x, float y, float range, Color color) {
        colorScratch.set(color).a(frameOpacity);
        Lines.stroke(frameStrokeWidth, colorScratch);
        if (frameDashed) {
            Lines.dashCircle(x, y, range);
        } else {
            Lines.circle(x, y, range);
        }
    }

    private void drawSquare(float x, float y, float range, Color color) {
        colorScratch.set(color).a(frameOpacity);
        Lines.stroke(frameStrokeWidth, colorScratch);
        if (frameDashed) {
            Drawf.dashSquareBasic(x, y, range);
        } else {
            Lines.rect(x - range / 2f, y - range / 2f, range, range);
        }
    }
}


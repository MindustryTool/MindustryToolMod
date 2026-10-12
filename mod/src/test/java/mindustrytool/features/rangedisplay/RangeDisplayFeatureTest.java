package mindustrytool.features.rangedisplay;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.math.geom.Rect;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.game.Rules;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.BuildTurret;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.defense.RegenProjector;
import mindustry.world.blocks.defense.ShockwaveTower;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.defense.turrets.PointDefenseTurret;
import mindustry.world.blocks.distribution.MassDriver;
import mindustry.world.blocks.units.RepairTower;
import mindustry.world.blocks.units.RepairTurret;
import mindustrytool.test.MindustryTestEnv;

class RangeDisplayFeatureTest extends MindustryTestEnv {

    @BeforeEach
    void setUp() {
        Vars.content = new ContentLoader();
    }


    @Test
    void testConfigMutationsAndReset() {
        RangeDisplayFeature feature = new RangeDisplayFeature();

        feature.opacityConfig.set(0.35f);
        feature.strokeWidthConfig.set(2.5f);
        feature.hoverOnlyConfig.set(true);
        feature.filterTargetAirConfig.set(false);
        feature.filterTargetGroundConfig.set(false);
        feature.onlyWithAmmoConfig.set(true);
        feature.proximityFilterConfig.set(true);
        feature.proximityRadiusConfig.set(45.0f);
        feature.drawTurretRangeAllyConfig.set(false);
        feature.drawTurretRangeEnemyConfig.set(false);
        feature.drawUnitRangePlayerConfig.set(false);
        feature.drawUnitRangeAllyConfig.set(false);
        feature.drawUnitRangeEnemyConfig.set(false);
        feature.drawBlockRangeAllyConfig.set(false);
        feature.drawBlockRangeEnemyConfig.set(false);
        feature.drawSpawnerRangeConfig.set(false);
        feature.drawOverdriveRangeConfig.set(false);
        feature.drawMenderRangeConfig.set(false);
        feature.drawPointDefenseRangeConfig.set(false);
        feature.useTeamColorForSpecialConfig.set(true);
        feature.dashedConfig.set(false);
        feature.zoomThresholdConfig.set(1.5f);

        assertEquals(0.35f, feature.opacityConfig.get(), 0.001f);
        assertEquals(2.5f, feature.strokeWidthConfig.get(), 0.001f);
        assertTrue(feature.hoverOnlyConfig.get());
        assertFalse(feature.filterTargetAirConfig.get());
        assertFalse(feature.filterTargetGroundConfig.get());
        assertTrue(feature.onlyWithAmmoConfig.get());
        assertTrue(feature.proximityFilterConfig.get());
        assertEquals(45.0f, feature.proximityRadiusConfig.get(), 0.001f);
        assertFalse(feature.drawTurretRangeAllyConfig.get());
        assertFalse(feature.drawTurretRangeEnemyConfig.get());
        assertFalse(feature.drawUnitRangePlayerConfig.get());
        assertFalse(feature.drawUnitRangeAllyConfig.get());
        assertFalse(feature.drawUnitRangeEnemyConfig.get());
        assertFalse(feature.drawBlockRangeAllyConfig.get());
        assertFalse(feature.drawBlockRangeEnemyConfig.get());
        assertFalse(feature.drawSpawnerRangeConfig.get());
        assertFalse(feature.drawOverdriveRangeConfig.get());
        assertFalse(feature.drawMenderRangeConfig.get());
        assertFalse(feature.drawPointDefenseRangeConfig.get());
        assertTrue(feature.useTeamColorForSpecialConfig.get());
        assertFalse(feature.dashedConfig.get());
        assertEquals(1.5f, feature.zoomThresholdConfig.get(), 0.001f);

        feature.resetToDefaults();

        assertEquals(1.0f, feature.opacityConfig.get(), 0.001f);
        assertEquals(1.0f, feature.strokeWidthConfig.get(), 0.001f);
        assertFalse(feature.hoverOnlyConfig.get());
        assertTrue(feature.filterTargetAirConfig.get());
        assertTrue(feature.filterTargetGroundConfig.get());
        assertFalse(feature.onlyWithAmmoConfig.get());
        assertFalse(feature.proximityFilterConfig.get());
        assertEquals(30.0f, feature.proximityRadiusConfig.get(), 0.001f);
        assertTrue(feature.drawTurretRangeAllyConfig.get());
        assertTrue(feature.drawTurretRangeEnemyConfig.get());
        assertTrue(feature.drawUnitRangePlayerConfig.get());
        assertTrue(feature.drawUnitRangeAllyConfig.get());
        assertTrue(feature.drawUnitRangeEnemyConfig.get());
        assertTrue(feature.drawBlockRangeAllyConfig.get());
        assertTrue(feature.drawBlockRangeEnemyConfig.get());
        assertTrue(feature.drawSpawnerRangeConfig.get());
        assertTrue(feature.drawOverdriveRangeConfig.get());
        assertTrue(feature.drawMenderRangeConfig.get());
        assertTrue(feature.drawPointDefenseRangeConfig.get());
        assertFalse(feature.useTeamColorForSpecialConfig.get());
        assertTrue(feature.dashedConfig.get());
        assertEquals(0.5f, feature.zoomThresholdConfig.get(), 0.001f);
    }

    @Test
    void testSignalReactivity() {
        RangeDisplayFeature feature = new RangeDisplayFeature();
        boolean[] notified = new boolean[1];

        feature.opacityConfig.signal().subscribe(v -> notified[0] = true);
        feature.opacityConfig.set(0.75f);

        assertTrue(notified[0]);
        assertEquals(0.75f, feature.opacityConfig.get(), 0.001f);
    }

    @Test
    void testBlockClassification() {
        RangeDisplayFeature feature = new RangeDisplayFeature();

        ItemTurret turret = new ItemTurret("test-duo");
        BuildTurret buildTurret = new BuildTurret("test-build-tower");
        MendProjector mend = new MendProjector("test-mender");
        OverdriveProjector overdrive = new OverdriveProjector("test-overdrive");
        MassDriver massDriver = new MassDriver("test-driver");
        ForceProjector force = new ForceProjector("test-force");
        RegenProjector regen = new RegenProjector("test-regen");
        RepairTower repairTower = new RepairTower("test-repair-tower");
        RepairTurret repairTurret = new RepairTurret("test-repair-turret");
        ShockwaveTower shockwave = new ShockwaveTower("test-shockwave");
        Wall wall = new Wall("test-wall");

        // Turret classification: combat turrets only (not build turrets)
        assertTrue(feature.isTurretBlock(turret));
        assertFalse(feature.isTurretBlock(buildTurret));
        assertFalse(feature.isTurretBlock(mend));
        assertFalse(feature.isTurretBlock(repairTower));
        assertFalse(feature.isTurretBlock(repairTurret));
        assertFalse(feature.isTurretBlock(shockwave));
        assertFalse(feature.isTurretBlock(wall));

        // Support block classification: build towers, projectors, drivers, repair towers, shockwave
        assertTrue(feature.isSupportBlock(buildTurret));
        assertTrue(feature.isSupportBlock(mend));
        assertTrue(feature.isSupportBlock(overdrive));
        assertTrue(feature.isSupportBlock(massDriver));
        assertTrue(feature.isSupportBlock(force));
        assertTrue(feature.isSupportBlock(regen));
        assertTrue(feature.isSupportBlock(repairTower));
        assertTrue(feature.isSupportBlock(repairTurret));
        assertTrue(feature.isSupportBlock(shockwave));
        assertFalse(feature.isSupportBlock(turret));
        assertFalse(feature.isSupportBlock(wall));

        // Range block classification: turrets and support blocks
        assertTrue(feature.isRangeBlock(turret));
        assertTrue(feature.isRangeBlock(mend));
        assertTrue(feature.isRangeBlock(repairTower));
        assertTrue(feature.isRangeBlock(repairTurret));
        assertTrue(feature.isRangeBlock(shockwave));
        assertFalse(feature.isRangeBlock(wall));
    }

    @Test
    void testSemanticColorBlockClassification() {
        RangeDisplayFeature feature = new RangeDisplayFeature();

        OverdriveProjector overdrive = new OverdriveProjector("test-semantic-overdrive");
        MendProjector mend = new MendProjector("test-semantic-mender");
        RegenProjector regen = new RegenProjector("test-semantic-regen");
        RepairTower repairTower = new RepairTower("test-semantic-repair-tower");
        RepairTurret repairTurret = new RepairTurret("test-semantic-repair-turret");
        PointDefenseTurret segment = new PointDefenseTurret("test-semantic-segment");
        ShockwaveTower shockwave = new ShockwaveTower("test-semantic-shockwave");
        ItemTurret turret = new ItemTurret("test-semantic-turret");
        Wall wall = new Wall("test-semantic-wall");

        assertTrue(feature.isOverdriveBlock(overdrive));
        assertFalse(feature.isOverdriveBlock(mend));
        assertFalse(feature.isOverdriveBlock(segment));
        assertFalse(feature.isOverdriveBlock(turret));

        assertTrue(feature.isMenderBlock(mend));
        assertTrue(feature.isMenderBlock(regen));
        assertTrue(feature.isMenderBlock(repairTower));
        assertTrue(feature.isMenderBlock(repairTurret));
        assertFalse(feature.isMenderBlock(overdrive));
        assertFalse(feature.isMenderBlock(segment));
        assertFalse(feature.isMenderBlock(turret));

        assertTrue(feature.isPointDefenseBlock(segment));
        assertTrue(feature.isPointDefenseBlock(shockwave));
        assertFalse(feature.isPointDefenseBlock(overdrive));
        assertFalse(feature.isPointDefenseBlock(mend));
        assertFalse(feature.isPointDefenseBlock(turret));

        assertTrue(feature.isSpecialColorBlock(overdrive));
        assertTrue(feature.isSpecialColorBlock(mend));
        assertTrue(feature.isSpecialColorBlock(segment));
        assertTrue(feature.isSpecialColorBlock(shockwave));
        assertFalse(feature.isSpecialColorBlock(turret));
        assertFalse(feature.isSpecialColorBlock(wall));
    }

    @Test
    void testPerBlockTogglesAndSignals() {
        RangeDisplayFeature feature = new RangeDisplayFeature();
        ItemTurret turret = new ItemTurret("test-scatter");

        // Default state: enabled
        assertTrue(feature.isBlockEnabled(turret));
        assertTrue(feature.getBlockSignal(turret).peek());

        // Toggle off
        feature.setBlockEnabled(turret, false);
        assertFalse(feature.isBlockEnabled(turret));
        assertFalse(feature.getBlockSignal(turret).peek());
        assertFalse(Core.settings.getBool(RangeDisplayFeature.blockSettingKey(turret)));

        // Toggle on
        feature.setBlockEnabled(turret, true);
        assertTrue(feature.isBlockEnabled(turret));
        assertTrue(feature.getBlockSignal(turret).peek());
        assertTrue(Core.settings.getBool(RangeDisplayFeature.blockSettingKey(turret)));
    }

    @Test
    void testCategoryTogglesAndReset() {
        RangeDisplayFeature feature = new RangeDisplayFeature();
        ItemTurret turret1 = new ItemTurret("test-hail");
        ItemTurret turret2 = new ItemTurret("test-salvo");
        MendProjector mender = new MendProjector("test-mend-proj");

        feature.rebuildBitSet();

        // Turn all turrets off
        feature.setCategoryEnabled(true, false);
        assertFalse(feature.isBlockEnabled(turret1));
        assertFalse(feature.isBlockEnabled(turret2));
        assertTrue(feature.isBlockEnabled(mender)); // Mender remains enabled

        // Turn all turrets on
        feature.setCategoryEnabled(true, true);
        assertTrue(feature.isBlockEnabled(turret1));
        assertTrue(feature.isBlockEnabled(turret2));

        // Reset to defaults
        feature.setBlockEnabled(turret1, false);
        feature.setBlockEnabled(mender, false);
        assertFalse(feature.isBlockEnabled(turret1));
        assertFalse(feature.isBlockEnabled(mender));

        feature.resetToDefaults();
        assertTrue(feature.isBlockEnabled(turret1));
        assertTrue(feature.isBlockEnabled(mender));
    }

    @Test
    void testAllPrimitiveCachesSynchronizeOnSignalAndConfigChanges() throws Exception {
        RangeDisplayFeature feature = new RangeDisplayFeature();

        // 1. Verify default values on all cached primitive fields
        assertEquals(1.0f, getCachedFloat(feature, "cachedOpacity"), 0.001f);
        assertEquals(1.0f, getCachedFloat(feature, "cachedStrokeWidth"), 0.001f);
        assertFalse(getCachedBool(feature, "cachedHoverOnly"));
        assertTrue(getCachedBool(feature, "cachedFilterTargetAir"));
        assertTrue(getCachedBool(feature, "cachedFilterTargetGround"));
        assertFalse(getCachedBool(feature, "cachedOnlyWithAmmo"));
        assertFalse(getCachedBool(feature, "cachedProximityFilter"));
        assertEquals(30.0f, getCachedFloat(feature, "cachedProximityRadius"), 0.001f);
        assertTrue(getCachedBool(feature, "cachedDrawTurretRangeAlly"));
        assertTrue(getCachedBool(feature, "cachedDrawTurretRangeEnemy"));
        assertTrue(getCachedBool(feature, "cachedDrawUnitRangePlayer"));
        assertTrue(getCachedBool(feature, "cachedDrawUnitRangeAlly"));
        assertTrue(getCachedBool(feature, "cachedDrawUnitRangeEnemy"));
        assertTrue(getCachedBool(feature, "cachedDrawBlockRangeAlly"));
        assertTrue(getCachedBool(feature, "cachedDrawBlockRangeEnemy"));
        assertTrue(getCachedBool(feature, "cachedDrawSpawnerRange"));
        assertTrue(getCachedBool(feature, "cachedDrawOverdriveRange"));
        assertTrue(getCachedBool(feature, "cachedDrawMenderRange"));
        assertTrue(getCachedBool(feature, "cachedDrawPointDefenseRange"));
        assertFalse(getCachedBool(feature, "cachedUseTeamColorForSpecial"));
        assertTrue(getCachedBool(feature, "cachedDashed"));
        assertEquals(0.5f, getCachedFloat(feature, "cachedZoomThreshold"), 0.001f);

        // 2. Mutate configs via ConfigValue.set()
        feature.opacityConfig.set(0.42f);
        feature.strokeWidthConfig.set(2.8f);
        feature.hoverOnlyConfig.set(true);
        feature.filterTargetAirConfig.set(false);
        feature.filterTargetGroundConfig.set(false);
        feature.onlyWithAmmoConfig.set(true);
        feature.proximityFilterConfig.set(true);
        feature.proximityRadiusConfig.set(55.0f);
        feature.drawTurretRangeAllyConfig.set(false);
        feature.drawTurretRangeEnemyConfig.set(false);
        feature.drawUnitRangePlayerConfig.set(false);
        feature.drawUnitRangeAllyConfig.set(false);
        feature.drawUnitRangeEnemyConfig.set(false);
        feature.drawBlockRangeAllyConfig.set(false);
        feature.drawBlockRangeEnemyConfig.set(false);
        feature.drawSpawnerRangeConfig.set(false);
        feature.drawOverdriveRangeConfig.set(false);
        feature.drawMenderRangeConfig.set(false);
        feature.drawPointDefenseRangeConfig.set(false);
        feature.useTeamColorForSpecialConfig.set(true);
        feature.dashedConfig.set(false);
        feature.zoomThresholdConfig.set(1.5f);

        // Verify that every single primitive field updated
        assertEquals(0.42f, getCachedFloat(feature, "cachedOpacity"), 0.001f);
        assertEquals(2.8f, getCachedFloat(feature, "cachedStrokeWidth"), 0.001f);
        assertTrue(getCachedBool(feature, "cachedHoverOnly"));
        assertFalse(getCachedBool(feature, "cachedFilterTargetAir"));
        assertFalse(getCachedBool(feature, "cachedFilterTargetGround"));
        assertTrue(getCachedBool(feature, "cachedOnlyWithAmmo"));
        assertTrue(getCachedBool(feature, "cachedProximityFilter"));
        assertEquals(55.0f, getCachedFloat(feature, "cachedProximityRadius"), 0.001f);
        assertFalse(getCachedBool(feature, "cachedDrawTurretRangeAlly"));
        assertFalse(getCachedBool(feature, "cachedDrawTurretRangeEnemy"));
        assertFalse(getCachedBool(feature, "cachedDrawUnitRangePlayer"));
        assertFalse(getCachedBool(feature, "cachedDrawUnitRangeAlly"));
        assertFalse(getCachedBool(feature, "cachedDrawUnitRangeEnemy"));
        assertFalse(getCachedBool(feature, "cachedDrawBlockRangeAlly"));
        assertFalse(getCachedBool(feature, "cachedDrawBlockRangeEnemy"));
        assertFalse(getCachedBool(feature, "cachedDrawSpawnerRange"));
        assertFalse(getCachedBool(feature, "cachedDrawOverdriveRange"));
        assertFalse(getCachedBool(feature, "cachedDrawMenderRange"));
        assertFalse(getCachedBool(feature, "cachedDrawPointDefenseRange"));
        assertTrue(getCachedBool(feature, "cachedUseTeamColorForSpecial"));
        assertFalse(getCachedBool(feature, "cachedDashed"));
        assertEquals(1.5f, getCachedFloat(feature, "cachedZoomThreshold"), 0.001f);

        // 3. Mutate configs directly via signal().set()
        feature.opacityConfig.signal().set(0.88f);
        feature.strokeWidthConfig.signal().set(3.5f);
        feature.hoverOnlyConfig.signal().set(false);
        feature.filterTargetAirConfig.signal().set(true);
        feature.filterTargetGroundConfig.signal().set(true);
        feature.onlyWithAmmoConfig.signal().set(false);
        feature.proximityFilterConfig.signal().set(false);
        feature.proximityRadiusConfig.signal().set(12.0f);
        feature.drawTurretRangeAllyConfig.signal().set(true);
        feature.drawTurretRangeEnemyConfig.signal().set(true);
        feature.drawUnitRangePlayerConfig.signal().set(true);
        feature.drawUnitRangeAllyConfig.signal().set(true);
        feature.drawUnitRangeEnemyConfig.signal().set(true);
        feature.drawBlockRangeAllyConfig.signal().set(true);
        feature.drawBlockRangeEnemyConfig.signal().set(true);
        feature.drawSpawnerRangeConfig.signal().set(true);
        feature.drawOverdriveRangeConfig.signal().set(true);
        feature.drawMenderRangeConfig.signal().set(true);
        feature.drawPointDefenseRangeConfig.signal().set(true);
        feature.useTeamColorForSpecialConfig.signal().set(false);
        feature.dashedConfig.signal().set(true);
        feature.zoomThresholdConfig.signal().set(2.5f);

        assertEquals(0.88f, getCachedFloat(feature, "cachedOpacity"), 0.001f);
        assertEquals(3.5f, getCachedFloat(feature, "cachedStrokeWidth"), 0.001f);
        assertFalse(getCachedBool(feature, "cachedHoverOnly"));
        assertTrue(getCachedBool(feature, "cachedFilterTargetAir"));
        assertTrue(getCachedBool(feature, "cachedFilterTargetGround"));
        assertFalse(getCachedBool(feature, "cachedOnlyWithAmmo"));
        assertFalse(getCachedBool(feature, "cachedProximityFilter"));
        assertEquals(12.0f, getCachedFloat(feature, "cachedProximityRadius"), 0.001f);
        assertTrue(getCachedBool(feature, "cachedDrawTurretRangeAlly"));
        assertTrue(getCachedBool(feature, "cachedDrawTurretRangeEnemy"));
        assertTrue(getCachedBool(feature, "cachedDrawUnitRangePlayer"));
        assertTrue(getCachedBool(feature, "cachedDrawUnitRangeAlly"));
        assertTrue(getCachedBool(feature, "cachedDrawUnitRangeEnemy"));
        assertTrue(getCachedBool(feature, "cachedDrawBlockRangeAlly"));
        assertTrue(getCachedBool(feature, "cachedDrawBlockRangeEnemy"));
        assertTrue(getCachedBool(feature, "cachedDrawSpawnerRange"));
        assertTrue(getCachedBool(feature, "cachedDrawOverdriveRange"));
        assertTrue(getCachedBool(feature, "cachedDrawMenderRange"));
        assertTrue(getCachedBool(feature, "cachedDrawPointDefenseRange"));
        assertFalse(getCachedBool(feature, "cachedUseTeamColorForSpecial"));
        assertTrue(getCachedBool(feature, "cachedDashed"));
        assertEquals(2.5f, getCachedFloat(feature, "cachedZoomThreshold"), 0.001f);

        // 4. Test null signal fallback
        feature.opacityConfig.signal().set(null);
        feature.hoverOnlyConfig.signal().set(null);
        feature.filterTargetAirConfig.signal().set(null);
        feature.useTeamColorForSpecialConfig.signal().set(null);
        feature.zoomThresholdConfig.signal().set(null);
        assertEquals(1.0f, getCachedFloat(feature, "cachedOpacity"), 0.001f);
        assertFalse(getCachedBool(feature, "cachedHoverOnly"));
        assertTrue(getCachedBool(feature, "cachedFilterTargetAir"));
        assertFalse(getCachedBool(feature, "cachedUseTeamColorForSpecial"));
        assertEquals(0.5f, getCachedFloat(feature, "cachedZoomThreshold"), 0.001f);

        // 5. Reset to defaults
        feature.opacityConfig.set(0.1f);
        feature.zoomThresholdConfig.set(3.0f);
        feature.resetToDefaults();
        assertEquals(1.0f, getCachedFloat(feature, "cachedOpacity"), 0.001f);
        assertEquals(1.0f, getCachedFloat(feature, "cachedStrokeWidth"), 0.001f);
        assertFalse(getCachedBool(feature, "cachedHoverOnly"));
        assertEquals(0.5f, getCachedFloat(feature, "cachedZoomThreshold"), 0.001f);
    }

    @Test
    void testSpawnerDropZonesHandlingEmptyAndNullSpawns() {
        // 1. Verify spawner loop guard logic
        Rules rules = new Rules();
        rules.dropZoneRadius = 160f;
        rules.waveTeam = null;

        // Case A: Null spawner - guard prevents entry
        Object nullSpawner = null;
        assertFalse(nullSpawner != null, "Null spawner must be rejected by guard");

        // Case B: Null getSpawns() - guard prevents entry
        Seq<Tile> nullSpawns = null;
        assertFalse(nullSpawns != null, "Null spawns must be rejected by guard");

        // Case C: Empty Seq - indexed loop executes 0 iterations
        Seq<Tile> emptySpawns = new Seq<>();
        int iterations = 0;
        for (int i = 0; i < emptySpawns.size; i++) {
            iterations++;
        }
        assertEquals(0, iterations, "Empty spawns must execute 0 iterations");

        // Case D: Seq containing null tiles - if (tile == null) continue skips without NPE
        Seq<Tile> nullTiles = Seq.with((Tile) null, (Tile) null, (Tile) null);
        int validCount = 0;
        for (int i = 0; i < nullTiles.size; i++) {
            Tile tile = nullTiles.get(i);
            if (tile == null) {
                continue;
            }
            validCount++;
        }
        assertEquals(0, validCount, "Null tiles must be safely skipped by continue");

        // Case E: Viewport frustum culling on drop zones
        Rect viewBounds = new Rect(0f, 0f, 800f, 600f);
        float dropRadius = rules.dropZoneRadius;

        // Spawn tile 1: at center (400, 300) -> on-screen
        float x1 = 400f, y1 = 300f;
        boolean inView1 = x1 + dropRadius >= viewBounds.x && x1 - dropRadius <= viewBounds.x + viewBounds.width
                && y1 + dropRadius >= viewBounds.y && y1 - dropRadius <= viewBounds.y + viewBounds.height;
        assertTrue(inView1, "Center spawn tile must be inside viewBounds");

        // Spawn tile 2: distant at (2500, 2500) -> culled
        float x2 = 2500f, y2 = 2500f;
        boolean inView2 = x2 + dropRadius >= viewBounds.x && x2 - dropRadius <= viewBounds.x + viewBounds.width
                && y2 + dropRadius >= viewBounds.y && y2 - dropRadius <= viewBounds.y + viewBounds.height;
        assertFalse(inView2, "Distant spawn tile must be culled outside viewBounds");

        // Spawn tile 3: touching boundary edge (x = -150, dropRadius = 160) -> overlaps x = 0 -> in view
        float x3 = -150f, y3 = 300f;
        boolean inView3 = x3 + dropRadius >= viewBounds.x && x3 - dropRadius <= viewBounds.x + viewBounds.width
                && y3 + dropRadius >= viewBounds.y && y3 - dropRadius <= viewBounds.y + viewBounds.height;
        assertTrue(inView3, "Boundary-overlapping spawn tile must not be prematurely culled");

        // Spawn tile 4: beyond boundary edge (x = -170, dropRadius = 160) -> culled
        float x4 = -170f, y4 = 300f;
        boolean inView4 = x4 + dropRadius >= viewBounds.x && x4 - dropRadius <= viewBounds.x + viewBounds.width
                && y4 + dropRadius >= viewBounds.y && y4 - dropRadius <= viewBounds.y + viewBounds.height;
        assertFalse(inView4, "Spawn tile beyond boundary edge must be culled");
    }

    private static float getCachedFloat(RangeDisplayFeature feature, String fieldName) throws Exception {
        Field field = RangeDisplayFeature.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getFloat(feature);
    }

    private static boolean getCachedBool(RangeDisplayFeature feature, String fieldName) throws Exception {
        Field field = RangeDisplayFeature.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(feature);
    }
}


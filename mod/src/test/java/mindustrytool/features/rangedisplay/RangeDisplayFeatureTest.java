package mindustrytool.features.rangedisplay;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.world.blocks.defense.BuildTurret;
import mindustry.world.blocks.defense.ForceProjector;
import mindustry.world.blocks.defense.MendProjector;
import mindustry.world.blocks.defense.OverdriveProjector;
import mindustry.world.blocks.defense.RegenProjector;
import mindustry.world.blocks.defense.Wall;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustry.world.blocks.distribution.MassDriver;
import mindustrytool.features.FeatureMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RangeDisplayFeatureTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
    }

    @Test
    void testMetadata() {
        RangeDisplayFeature feature = new RangeDisplayFeature();
        FeatureMetadata meta = feature.getMetadata();

        assertNotNull(meta);
        assertEquals("range-display", meta.getId());
        assertEquals(5, meta.getOrder());
        assertTrue(meta.isQuickAccess());
        assertTrue(meta.isEnabledByDefault());
        assertFalse(meta.isDevelopment());
    }

    @Test
    void testInitialConfigDefaults() {
        RangeDisplayFeature feature = new RangeDisplayFeature();

        assertEquals(1.0f, feature.opacityConfig.get(), 0.001f);
        assertTrue(feature.drawTurretRangeAllyConfig.get());
        assertTrue(feature.drawTurretRangeEnemyConfig.get());
        assertTrue(feature.drawUnitRangeAllyConfig.get());
        assertTrue(feature.drawUnitRangeEnemyConfig.get());
        assertTrue(feature.drawBlockRangeAllyConfig.get());
        assertTrue(feature.drawBlockRangeEnemyConfig.get());
        assertTrue(feature.drawSpawnerRangeConfig.get());
        assertTrue(feature.dashedConfig.get());
    }

    @Test
    void testConfigMutationsAndReset() {
        RangeDisplayFeature feature = new RangeDisplayFeature();

        feature.opacityConfig.set(0.35f);
        feature.drawTurretRangeAllyConfig.set(false);
        feature.drawTurretRangeEnemyConfig.set(false);
        feature.drawUnitRangeAllyConfig.set(false);
        feature.drawUnitRangeEnemyConfig.set(false);
        feature.drawBlockRangeAllyConfig.set(false);
        feature.drawBlockRangeEnemyConfig.set(false);
        feature.drawSpawnerRangeConfig.set(false);
        feature.dashedConfig.set(false);

        assertEquals(0.35f, feature.opacityConfig.get(), 0.001f);
        assertFalse(feature.drawTurretRangeAllyConfig.get());
        assertFalse(feature.drawTurretRangeEnemyConfig.get());
        assertFalse(feature.drawUnitRangeAllyConfig.get());
        assertFalse(feature.drawUnitRangeEnemyConfig.get());
        assertFalse(feature.drawBlockRangeAllyConfig.get());
        assertFalse(feature.drawBlockRangeEnemyConfig.get());
        assertFalse(feature.drawSpawnerRangeConfig.get());
        assertFalse(feature.dashedConfig.get());

        feature.resetToDefaults();

        assertEquals(1.0f, feature.opacityConfig.get(), 0.001f);
        assertTrue(feature.drawTurretRangeAllyConfig.get());
        assertTrue(feature.drawTurretRangeEnemyConfig.get());
        assertTrue(feature.drawUnitRangeAllyConfig.get());
        assertTrue(feature.drawUnitRangeEnemyConfig.get());
        assertTrue(feature.drawBlockRangeAllyConfig.get());
        assertTrue(feature.drawBlockRangeEnemyConfig.get());
        assertTrue(feature.drawSpawnerRangeConfig.get());
        assertTrue(feature.dashedConfig.get());
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
        Wall wall = new Wall("test-wall");

        // Turret classification: combat turrets only (not build turrets)
        assertTrue(feature.isTurretBlock(turret));
        assertFalse(feature.isTurretBlock(buildTurret));
        assertFalse(feature.isTurretBlock(mend));
        assertFalse(feature.isTurretBlock(wall));

        // Support block classification: build towers, projectors, drivers
        assertTrue(feature.isSupportBlock(buildTurret));
        assertTrue(feature.isSupportBlock(mend));
        assertTrue(feature.isSupportBlock(overdrive));
        assertTrue(feature.isSupportBlock(massDriver));
        assertTrue(feature.isSupportBlock(force));
        assertTrue(feature.isSupportBlock(regen));
        assertFalse(feature.isSupportBlock(turret));
        assertFalse(feature.isSupportBlock(wall));

        // Range block classification: turrets and support blocks
        assertTrue(feature.isRangeBlock(turret));
        assertTrue(feature.isRangeBlock(mend));
        assertFalse(feature.isRangeBlock(wall));
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
}

package mindustrytool.features.rangedisplay;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import mindustrytool.features.FeatureMetadata;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RangeDisplayFeatureTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
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
}

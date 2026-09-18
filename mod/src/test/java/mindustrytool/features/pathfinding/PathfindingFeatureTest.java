package mindustrytool.features.pathfinding;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.util.I18NBundle;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class PathfindingFeatureTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
        if (Core.bundle == null) {
            Core.bundle = I18NBundle.createEmptyBundle();
        }
    }

    @Test
    void testCacheManagerOperationsAndCleanup() {
        PathfindingCacheManager manager = new PathfindingCacheManager();
        long key1 = 100L;
        long key2 = 200L;

        PathfindingCache cache1 = new PathfindingCache(16);
        cache1.lastUsedTime = 10f;
        cache1.size = 4;

        PathfindingCache cache2 = new PathfindingCache(16);
        cache2.lastUsedTime = 50f;
        cache2.size = 4;

        manager.put(key1, cache1);
        manager.put(key2, cache2);

        assertNotNull(manager.get(key1));
        assertNotNull(manager.get(key2));

        // Cleanup with currentTime=70, maxAge=30 -> key1 (age 60) removed, key2 (age 20) preserved
        manager.cleanup(70f, 30f);
        assertNull(manager.get(key1));
        assertNotNull(manager.get(key2));

        manager.clear();
        assertNull(manager.get(key2));
    }

    @Test
    void testFeatureDefaultConfigs() {
        PathfindingFeature feature = new PathfindingFeature();

        assertFalse(feature.isEnabled());
        assertTrue(feature.drawUnitPathConfig.get());
        assertTrue(feature.drawSpawnPathConfig.get());
        assertFalse(feature.drawAlliesConfig.get());
        assertEquals(0.5f, feature.zoomThresholdConfig.get(), 0.001f);
        assertEquals(1.0f, feature.opacityConfig.get(), 0.001f);

        for (int i = 0; i < PathfindingFeature.COST_COUNT; i++) {
            assertTrue(feature.costTypeConfigs[i].get());
            assertTrue(feature.isCostTypeEnabled(i));
        }
    }

    @Test
    void testConfigMutationsAndReset() {
        PathfindingFeature feature = new PathfindingFeature();

        feature.drawUnitPathConfig.set(false);
        feature.drawSpawnPathConfig.set(false);
        feature.drawAlliesConfig.set(true);
        feature.zoomThresholdConfig.set(1.5f);
        feature.opacityConfig.set(0.6f);
        feature.costTypeConfigs[0].set(false);

        assertFalse(feature.drawUnitPathConfig.get());
        assertFalse(feature.drawSpawnPathConfig.get());
        assertTrue(feature.drawAlliesConfig.get());
        assertEquals(1.5f, feature.zoomThresholdConfig.get(), 0.001f);
        assertEquals(0.6f, feature.opacityConfig.get(), 0.001f);
        assertFalse(feature.costTypeConfigs[0].get());
        assertFalse(feature.isCostTypeEnabled(0));

        feature.resetToDefaults();

        assertTrue(feature.drawUnitPathConfig.get());
        assertTrue(feature.drawSpawnPathConfig.get());
        assertFalse(feature.drawAlliesConfig.get());
        assertEquals(0.5f, feature.zoomThresholdConfig.get(), 0.001f);
        assertEquals(1.0f, feature.opacityConfig.get(), 0.001f);
        assertTrue(feature.costTypeConfigs[0].get());
        assertTrue(feature.isCostTypeEnabled(0));
    }

    @Test
    void testClientPathfinderClearAndSafeHandling() {
        ClientPathfinder clientPathfinder = new ClientPathfinder();

        clientPathfinder.clear();
        assertNull(clientPathfinder.getTargetTile(null, null));
    }
}

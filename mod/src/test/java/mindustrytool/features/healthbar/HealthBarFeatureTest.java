package mindustrytool.features.healthbar;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class HealthBarFeatureTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
    }

    @Test
    void testResetToDefaults() {
        HealthBarFeature feature = new HealthBarFeature();

        feature.showFriendlyUnitsConfig.set(false);
        feature.showEnemyUnitsConfig.set(false);
        feature.showFriendlyBlocksConfig.set(true);
        feature.showEnemyBlocksConfig.set(true);
        feature.zoomThresholdConfig.set(1.5f);
        feature.opacityConfig.set(0.2f);
        feature.scaleConfig.set(1.4f);
        feature.widthConfig.set(1.8f);

        assertTrue(feature.showFriendlyBlocksConfig.get());
        assertEquals(1.5f, feature.zoomThresholdConfig.get(), 0.001f);

        feature.resetToDefaults();

        assertTrue(feature.showFriendlyUnitsConfig.get());
        assertTrue(feature.showEnemyUnitsConfig.get());
        assertFalse(feature.showFriendlyBlocksConfig.get());
        assertFalse(feature.showEnemyBlocksConfig.get());
        assertEquals(0.5f, feature.zoomThresholdConfig.get(), 0.001f);
        assertEquals(1.0f, feature.opacityConfig.get(), 0.001f);
        assertEquals(1.0f, feature.scaleConfig.get(), 0.001f);
        assertEquals(1.0f, feature.widthConfig.get(), 0.001f);
    }
}

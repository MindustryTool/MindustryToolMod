package mindustrytool.features.healthbar;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import mindustrytool.test.MindustryTestEnv;

class HealthBarFeatureTest extends MindustryTestEnv {

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

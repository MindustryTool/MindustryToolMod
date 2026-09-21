package mindustrytool.features.bridgevisualizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.graphics.g2d.TextureRegion;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;
import mindustrytool.test.MindustryTestEnv;

class BridgeVisualizerFeatureTest extends MindustryTestEnv {

    @BeforeEach
    void setUp() {
        if (Icon.distribution == null) {
            Icon.distribution = new TextureRegionDrawable(new TextureRegion());
        }
    }

    @Test
    void testDefaultConfigs() {
        BridgeVisualizerFeature feature = new BridgeVisualizerFeature();

        assertTrue(feature.showItemBridgesConfig.get());
        assertTrue(feature.showDuctBridgesConfig.get());
        assertTrue(feature.showLiquidBridgesConfig.get());
        assertEquals(1.0f, feature.itemScaleConfig.get(), 0.001f);
        assertEquals(1.0f, feature.opacityConfig.get(), 0.001f);
    }

    @Test
    void testConfigMutationsAndReset() {
        BridgeVisualizerFeature feature = new BridgeVisualizerFeature();

        feature.showItemBridgesConfig.set(false);
        feature.showDuctBridgesConfig.set(false);
        feature.showLiquidBridgesConfig.set(false);
        feature.itemScaleConfig.set(1.5f);
        feature.opacityConfig.set(0.7f);

        assertFalse(feature.showItemBridgesConfig.get());
        assertFalse(feature.showDuctBridgesConfig.get());
        assertFalse(feature.showLiquidBridgesConfig.get());
        assertEquals(1.5f, feature.itemScaleConfig.get(), 0.001f);
        assertEquals(0.7f, feature.opacityConfig.get(), 0.001f);

        feature.resetToDefaults();

        assertTrue(feature.showItemBridgesConfig.get());
        assertTrue(feature.showDuctBridgesConfig.get());
        assertTrue(feature.showLiquidBridgesConfig.get());
        assertEquals(1.0f, feature.itemScaleConfig.get(), 0.001f);
        assertEquals(1.0f, feature.opacityConfig.get(), 0.001f);
    }
}

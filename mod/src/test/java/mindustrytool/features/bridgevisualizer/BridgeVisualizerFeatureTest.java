package mindustrytool.features.bridgevisualizer;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.mock.MockSettings;
import arc.scene.style.TextureRegionDrawable;
import mindustry.gen.Icon;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class BridgeVisualizerFeatureTest {

    @BeforeEach
    void setUp() {
        Core.app = new MockApplication();
        Core.graphics = new MockGraphics();
        Core.settings = new MockSettings();
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

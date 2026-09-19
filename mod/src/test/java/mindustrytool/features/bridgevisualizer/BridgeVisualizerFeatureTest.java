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
        assertTrue(feature.showSpeedConfig.get());
        assertTrue(feature.showItemSpeedConfig.get());
        assertEquals(1.0f, feature.itemScaleConfig.get(), 0.001f);
        assertEquals(1.0f, feature.opacityConfig.get(), 0.001f);
    }

    @Test
    void testConfigMutationsAndReset() {
        BridgeVisualizerFeature feature = new BridgeVisualizerFeature();

        feature.showItemBridgesConfig.set(false);
        feature.showDuctBridgesConfig.set(false);
        feature.showLiquidBridgesConfig.set(false);
        feature.showSpeedConfig.set(false);
        feature.itemScaleConfig.set(1.5f);
        feature.opacityConfig.set(0.7f);

        assertFalse(feature.showItemBridgesConfig.get());
        assertFalse(feature.showDuctBridgesConfig.get());
        assertFalse(feature.showLiquidBridgesConfig.get());
        assertFalse(feature.showSpeedConfig.get());
        assertFalse(feature.showItemSpeedConfig.get());
        assertEquals(1.5f, feature.itemScaleConfig.get(), 0.001f);
        assertEquals(0.7f, feature.opacityConfig.get(), 0.001f);

        feature.resetToDefaults();

        assertTrue(feature.showItemBridgesConfig.get());
        assertTrue(feature.showDuctBridgesConfig.get());
        assertTrue(feature.showLiquidBridgesConfig.get());
        assertTrue(feature.showSpeedConfig.get());
        assertTrue(feature.showItemSpeedConfig.get());
        assertEquals(1.0f, feature.itemScaleConfig.get(), 0.001f);
        assertEquals(1.0f, feature.opacityConfig.get(), 0.001f);
    }

    @Test
    void testCalculateRate() {
        // Standard item bridge: 10 items, normal speed (40 ticks transport time)
        assertEquals(15.0f, BridgeVisualizerFeature.calculateRate(10, 1.0f, 40f), 0.001f);

        // Boosted by overdrive (1.5x timescale): 15.0 * 1.5 = 22.5/s
        assertEquals(22.5f, BridgeVisualizerFeature.calculateRate(10, 1.5f, 40f), 0.001f);

        // Duct bridge: 1 item, speed 5 ticks -> 12 items/s
        assertEquals(12.0f, BridgeVisualizerFeature.calculateRate(1, 1.0f, 5f), 0.001f);

        // Partial batch: 2 items
        assertEquals(3.0f, BridgeVisualizerFeature.calculateRate(2, 1.0f, 40f), 0.001f);

        // Empty bridge: 0 items
        assertEquals(0f, BridgeVisualizerFeature.calculateRate(0, 1.0f, 40f), 0.001f);
        assertEquals(0f, BridgeVisualizerFeature.calculateRate(-1, 1.0f, 40f), 0.001f);

        // Invalid / zero speed or transport time
        assertEquals(0f, BridgeVisualizerFeature.calculateRate(5, 1.0f, 0f), 0.001f);
        assertEquals(0f, BridgeVisualizerFeature.calculateRate(5, 1.0f, -10f), 0.001f);
    }

    @Test
    void testCalculateLiquidRate() {
        // Standard liquid bridge: 16.0 capacity, 16.0 amount, full warmup -> 16.0/s
        assertEquals(16.0f, BridgeVisualizerFeature.calculateLiquidRate(16f, 16f, 1.0f, 1.0f, false), 0.001f);

        // Phase conduit (pulse = true): 16.0 capacity, 16.0 amount -> 32.0/s
        assertEquals(32.0f, BridgeVisualizerFeature.calculateLiquidRate(16f, 16f, 1.0f, 1.0f, true), 0.001f);

        // Reinforced bridge conduit: 20.0 capacity, 20.0 amount -> 20.0/s
        assertEquals(20.0f, BridgeVisualizerFeature.calculateLiquidRate(20f, 20f, 1.0f, 1.0f, false), 0.001f);

        // Overdrive boosted conduit: 16.0 * 1.5 = 24.0/s
        assertEquals(24.0f, BridgeVisualizerFeature.calculateLiquidRate(16f, 16f, 1.5f, 1.0f, false), 0.001f);

        // Half filled conduit: 8.0 amount
        assertEquals(8.0f, BridgeVisualizerFeature.calculateLiquidRate(8f, 16f, 1.0f, 1.0f, false), 0.001f);

        // Empty conduit
        assertEquals(0f, BridgeVisualizerFeature.calculateLiquidRate(0f, 16f, 1.0f, 1.0f, false), 0.001f);
        assertEquals(0f, BridgeVisualizerFeature.calculateLiquidRate(-1f, 16f, 1.0f, 1.0f, false), 0.001f);

        // Invalid capacity
        assertEquals(0f, BridgeVisualizerFeature.calculateLiquidRate(10f, 0f, 1.0f, 1.0f, false), 0.001f);
    }
}

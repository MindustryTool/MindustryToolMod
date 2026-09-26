package mindustrytool.features.smartdrill;

import mindustry.world.Tile;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SmartDrillFeatureTest extends MindustryTestEnv {

    private SmartDrillFeature feature;

    @BeforeEach
    void setUp() {
        feature = new SmartDrillFeature();
    }

    @Test
    void testMetadataAndDefaultState() {
        assertEquals("smart-drill", feature.getMetadata().getId());
        assertEquals(11, feature.getMetadata().getOrder());
        assertTrue(feature.getMetadata().isQuickAccess());
        assertFalse(feature.getMetadata().isDevelopment());
        assertFalse(feature.isEnabled(), "Smart Drill must be disabled by default so it does not interfere with conveyor placement");
    }

    @Test
    void testConfigsAndReset() {
        assertEquals(100, feature.maxTilesConfig.get());
        assertTrue(feature.autoDisableConfig.get());

        feature.maxTilesConfig.set(250);
        feature.autoDisableConfig.set(false);
        assertEquals(250, feature.maxTilesConfig.get());
        assertFalse(feature.autoDisableConfig.get());

        feature.resetToDefaults();
        assertEquals(100, feature.maxTilesConfig.get());
        assertTrue(feature.autoDisableConfig.get());
    }

    @Test
    void testDirectionMath() {
        assertEquals(SmartDrillFeature.Direction.LEFT, SmartDrillFeature.Direction.RIGHT.opposite());
        assertEquals(SmartDrillFeature.Direction.RIGHT, SmartDrillFeature.Direction.LEFT.opposite());
        assertEquals(SmartDrillFeature.Direction.DOWN, SmartDrillFeature.Direction.UP.opposite());
        assertEquals(SmartDrillFeature.Direction.UP, SmartDrillFeature.Direction.DOWN.opposite());

        assertTrue(SmartDrillFeature.Direction.RIGHT.horizontal());
        assertTrue(SmartDrillFeature.Direction.LEFT.horizontal());
        assertFalse(SmartDrillFeature.Direction.UP.horizontal());
        assertFalse(SmartDrillFeature.Direction.DOWN.horizontal());

        assertTrue(SmartDrillFeature.Direction.UP.vertical());
        assertTrue(SmartDrillFeature.Direction.DOWN.vertical());
        assertFalse(SmartDrillFeature.Direction.LEFT.vertical());
        assertFalse(SmartDrillFeature.Direction.RIGHT.vertical());

        assertEquals(0, SmartDrillFeature.Direction.RIGHT.rotation);
        assertEquals(1, SmartDrillFeature.Direction.UP.rotation);
        assertEquals(2, SmartDrillFeature.Direction.LEFT.rotation);
        assertEquals(3, SmartDrillFeature.Direction.DOWN.rotation);
    }

    @Test
    void testSettingsDialogProviderNotNull() {
        assertNotNull(feature.getSettingDialog());
    }

    @Test
    void testGridPatterns() {
        // isBridgeTile is true whenever x % 3 == 0 and y % 3 == 0
        Tile bridgeTile = new Tile(0, 0);
        assertTrue(feature.isBridgeTile(bridgeTile));

        Tile nonBridgeTile = new Tile(1, 1);
        assertFalse(feature.isBridgeTile(nonBridgeTile));

        // isDrillTile checks 2x2 drill placement math
        Tile drillTile1 = new Tile(0, 1);
        assertTrue(feature.isDrillTile(drillTile1));

        Tile drillTile2 = new Tile(1, 3);
        assertTrue(feature.isDrillTile(drillTile2));
    }

    @Test
    void testEnableDisableLifecycle() {
        assertFalse(feature.isEnabled());

        feature.enable();
        assertTrue(feature.isEnabled());

        feature.disable();
        assertFalse(feature.isEnabled());
    }
}

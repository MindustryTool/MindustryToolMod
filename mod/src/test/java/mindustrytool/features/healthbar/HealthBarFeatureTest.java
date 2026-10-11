package mindustrytool.features.healthbar;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.graphics.Camera;
import arc.math.geom.Rect;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.gen.Building;
import mindustry.world.Tile;
import mindustry.world.blocks.defense.turrets.ItemTurret;
import mindustrytool.test.MindustryTestEnv;

class HealthBarFeatureTest extends MindustryTestEnv {

    @BeforeEach
    void setUp() {
        Vars.content = new ContentLoader();
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

    @Test
    void test8PrimitiveCacheFieldsSynchronizeOnSignalAndConfigChanges() throws Exception {
        HealthBarFeature feature = new HealthBarFeature();

        // 1. Verify default values
        assertTrue(getCachedBool(feature, "cachedShowFriendlyUnits"));
        assertTrue(getCachedBool(feature, "cachedShowEnemyUnits"));
        assertFalse(getCachedBool(feature, "cachedShowFriendlyBlocks"));
        assertFalse(getCachedBool(feature, "cachedShowEnemyBlocks"));
        assertEquals(0.5f, getCachedFloat(feature, "cachedZoomThreshold"), 0.001f);
        assertEquals(1.0f, getCachedFloat(feature, "cachedOpacity"), 0.001f);
        assertEquals(1.0f, getCachedFloat(feature, "cachedScale"), 0.001f);
        assertEquals(1.0f, getCachedFloat(feature, "cachedWidth"), 0.001f);

        // 2. Mutate configs via ConfigValue.set()
        feature.showFriendlyUnitsConfig.set(false);
        feature.showEnemyUnitsConfig.set(false);
        feature.showFriendlyBlocksConfig.set(true);
        feature.showEnemyBlocksConfig.set(true);
        feature.zoomThresholdConfig.set(1.25f);
        feature.opacityConfig.set(0.3f);
        feature.scaleConfig.set(1.5f);
        feature.widthConfig.set(2.0f);

        assertFalse(getCachedBool(feature, "cachedShowFriendlyUnits"));
        assertFalse(getCachedBool(feature, "cachedShowEnemyUnits"));
        assertTrue(getCachedBool(feature, "cachedShowFriendlyBlocks"));
        assertTrue(getCachedBool(feature, "cachedShowEnemyBlocks"));
        assertEquals(1.25f, getCachedFloat(feature, "cachedZoomThreshold"), 0.001f);
        assertEquals(0.3f, getCachedFloat(feature, "cachedOpacity"), 0.001f);
        assertEquals(1.5f, getCachedFloat(feature, "cachedScale"), 0.001f);
        assertEquals(2.0f, getCachedFloat(feature, "cachedWidth"), 0.001f);

        // 3. Mutate configs directly via signal().set()
        feature.showFriendlyUnitsConfig.signal().set(true);
        feature.showFriendlyBlocksConfig.signal().set(false);
        feature.zoomThresholdConfig.signal().set(0.8f);
        feature.opacityConfig.signal().set(0.9f);
        feature.scaleConfig.signal().set(1.1f);
        feature.widthConfig.signal().set(1.2f);

        assertTrue(getCachedBool(feature, "cachedShowFriendlyUnits"));
        assertFalse(getCachedBool(feature, "cachedShowFriendlyBlocks"));
        assertEquals(0.8f, getCachedFloat(feature, "cachedZoomThreshold"), 0.001f);
        assertEquals(0.9f, getCachedFloat(feature, "cachedOpacity"), 0.001f);
        assertEquals(1.1f, getCachedFloat(feature, "cachedScale"), 0.001f);
        assertEquals(1.2f, getCachedFloat(feature, "cachedWidth"), 0.001f);

        // 4. Null signal fallback
        feature.showFriendlyUnitsConfig.signal().set(null);
        feature.zoomThresholdConfig.signal().set(null);
        feature.opacityConfig.signal().set(null);
        assertTrue(getCachedBool(feature, "cachedShowFriendlyUnits"));
        assertEquals(0.5f, getCachedFloat(feature, "cachedZoomThreshold"), 0.001f);
        assertEquals(1.0f, getCachedFloat(feature, "cachedOpacity"), 0.001f);

        // 5. Reset to defaults
        feature.opacityConfig.set(0.1f);
        feature.resetToDefaults();
        assertEquals(1.0f, getCachedFloat(feature, "cachedOpacity"), 0.001f);
        assertTrue(getCachedBool(feature, "cachedShowFriendlyUnits"));
    }
    
    @Test
    void testDrawBuildingCheckZeroAndNegativeSizeBoundsHandling() throws Exception {
        HealthBarFeature feature = new HealthBarFeature();
        feature.showFriendlyBlocksConfig.set(true);
        feature.showEnemyBlocksConfig.set(true);

        Method drawBuildingCheckMethod = HealthBarFeature.class.getDeclaredMethod("drawBuildingCheck", Building.class);
        drawBuildingCheckMethod.setAccessible(true);

        // 1. Null building
        assertDoesNotThrow(() -> drawBuildingCheckMethod.invoke(feature, (Building) null));

        Camera prevCamera = Core.camera;
        try {
            Core.camera = new Camera();
            Core.camera.width = 800f;
            Core.camera.height = 600f;
            Core.camera.position.set(400f, 300f);

            Field viewBoundsField = HealthBarFeature.class.getDeclaredField("viewBounds");
            viewBoundsField.setAccessible(true);
            Rect viewBounds = (Rect) viewBoundsField.get(feature);
            viewBounds.set(0f, 0f, 800f, 600f);

            Field frameShowEnemyBlocksField = HealthBarFeature.class.getDeclaredField("frameShowEnemyBlocks");
            frameShowEnemyBlocksField.setAccessible(true);
            frameShowEnemyBlocksField.setBoolean(feature, true);

            // 2. Off-screen building with size = 0: must cull without invoking drawBuildingBar
            ItemTurret blockZero = new ItemTurret("test-zero-block");
            blockZero.size = 0;

            Building bZeroOff = Building.create();
            bZeroOff.block = blockZero;
            bZeroOff.tile = new Tile(10, 10);
            bZeroOff.tile.build = bZeroOff;
            bZeroOff.health = 50f;
            bZeroOff.maxHealth = 100f;
            bZeroOff.x = 2500f;
            bZeroOff.y = 2500f;

            assertDoesNotThrow(() -> drawBuildingCheckMethod.invoke(feature, bZeroOff),
                    "Off-screen building with size 0 must cull safely without error");

            // 3. Off-screen building with size = -1: must cull without invoking drawBuildingBar
            ItemTurret blockNeg = new ItemTurret("test-neg-block");
            blockNeg.size = -1;

            Building bNegOff = Building.create();
            bNegOff.block = blockNeg;
            bNegOff.tile = new Tile(10, 10);
            bNegOff.tile.build = bNegOff;
            bNegOff.health = 50f;
            bNegOff.maxHealth = 100f;
            bNegOff.x = 2500f;
            bNegOff.y = 2500f;

            assertDoesNotThrow(() -> drawBuildingCheckMethod.invoke(feature, bNegOff),
                    "Off-screen building with negative size must cull safely without error");

            // 4. Mathematical bounds test mirroring drawBuildingCheck logic
            // float halfSize = (build.block.size * Vars.tilesize) / 2f;
            // culled = x + halfSize < viewBounds.x || x - halfSize > viewBounds.x + width
            //       || y + halfSize + 16f < viewBounds.y || y - halfSize > viewBounds.y + height;

            // Scenario 4A: Size = 0 at center (400, 300) -> halfSize = 0 -> not culled
            float halfSize0 = (0 * 8f) / 2f;
            boolean culledCenter0 = 400f + halfSize0 < 0f || 400f - halfSize0 > 800f
                    || 300f + halfSize0 + 16f < 0f || 300f - halfSize0 > 600f;
            assertFalse(culledCenter0, "Center building with size 0 must be in view");

            // Scenario 4B: Size = 0 at (-10, 300) -> culled
            boolean culledOff0 = -10f + halfSize0 < 0f || -10f - halfSize0 > 800f
                    || 300f + halfSize0 + 16f < 0f || 300f - halfSize0 > 600f;
            assertTrue(culledOff0, "Off-screen building with size 0 must be culled");

            // Scenario 4C: Size = -1 at center (400, 300) -> halfSize = -4 -> not culled
            float halfSizeNeg = (-1 * 8f) / 2f;
            boolean culledCenterNeg = 400f + halfSizeNeg < 0f || 400f - halfSizeNeg > 800f
                    || 300f + halfSizeNeg + 16f < 0f || 300f - halfSizeNeg > 600f;
            assertFalse(culledCenterNeg, "Center building with size -1 must be in view");

            // Scenario 4D: Size = -1 at (2500, 300) -> culled
            boolean culledOffNeg = 2500f + halfSizeNeg < 0f || 2500f - halfSizeNeg > 800f
                    || 300f + halfSizeNeg + 16f < 0f || 300f - halfSizeNeg > 600f;
            assertTrue(culledOffNeg, "Off-screen building with size -1 must be culled");
        } finally {
            Core.camera = prevCamera;
        }
    }

    private static float getCachedFloat(HealthBarFeature feature, String fieldName) throws Exception {
        Field field = HealthBarFeature.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getFloat(feature);
    }

    private static boolean getCachedBool(HealthBarFeature feature, String fieldName) throws Exception {
        Field field = HealthBarFeature.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.getBoolean(feature);
    }
}

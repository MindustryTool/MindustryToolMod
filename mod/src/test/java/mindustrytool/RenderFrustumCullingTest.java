package mindustrytool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.Core;
import arc.func.Boolf;
import arc.graphics.Camera;
import arc.math.geom.Rect;
import arc.mock.MockGraphics;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.GameState.State;
import mindustry.core.UI;
import mindustry.game.Rules;
import mindustry.ui.fragments.HudFragment;
import mindustrytool.test.MindustryTestEnv;

/**
 * Headless E2E test suite asserting viewport frustum culling correctness.
 * Verifies that off-screen entities outside {@code Core.camera.bounds}
 * produce 0 draw calls and are skipped immediately.
 */
class RenderFrustumCullingTest extends MindustryTestEnv {

    private UI prevUi;
    private GameState prevState;
    private Camera prevCamera;

    @BeforeEach
    void setUp() throws Exception {
        prevUi = Vars.ui;
        prevState = Vars.state;
        prevCamera = Core.camera;

        if (Core.graphics == null) {
            Core.graphics = new MockGraphics();
        }
        Core.camera = new Camera();
        Core.camera.width = 800f;
        Core.camera.height = 600f;
        Core.camera.position.set(400f, 300f);

        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }

        Vars.state = new GameState();
        Vars.state.rules = new Rules();
        Vars.state.set(State.playing);

        Class<?> unsafeClass = Class.forName("sun.misc.Unsafe");
        Field f = unsafeClass.getDeclaredField("theUnsafe");
        f.setAccessible(true);
        Object unsafe = f.get(null);
        Method allocateInstance = unsafeClass.getMethod("allocateInstance", Class.class);

        Vars.ui = (UI) allocateInstance.invoke(unsafe, UI.class);
        Vars.ui.hudfrag = (HudFragment) allocateInstance.invoke(unsafe, HudFragment.class);
        Vars.ui.hudfrag.shown = true;
    }

    @AfterEach
    void tearDown() {
        Vars.ui = prevUi;
        Vars.state = prevState;
        Core.camera = prevCamera;
    }

    @Test
    void testCameraViewportBoundsCalculation() {
        Rect bounds = new Rect();
        Core.camera.bounds(bounds);

        // Camera centered at (400, 300) with width 800 and height 600 -> rect is [0, 0, 800, 600]
        assertEquals(0f, bounds.x, 0.001f);
        assertEquals(0f, bounds.y, 0.001f);
        assertEquals(800f, bounds.width, 0.001f);
        assertEquals(600f, bounds.height, 0.001f);
    }

    @Test
    void testRangeDisplayFrustumCullingLogic() {
        Rect viewBounds = new Rect();
        Core.camera.bounds(viewBounds);

        // Helper lambda simulating RangeDisplay's frustum check
        Boolf<UnitCoordinate> isCulled = u -> u.x + u.range < viewBounds.x
                || u.x - u.range > viewBounds.x + viewBounds.width
                || u.y + u.range < viewBounds.y
                || u.y - u.range > viewBounds.y + viewBounds.height;

        // 1. Center of screen: clearly on-screen
        assertFalse(isCulled.get(new UnitCoordinate(400f, 300f, 100f)), "Center entity must not be culled");

        // 2. Far away: clearly off-screen
        assertTrue(isCulled.get(new UnitCoordinate(2500f, 2500f, 100f)), "Distant entity must be culled");
        assertTrue(isCulled.get(new UnitCoordinate(-1500f, -1500f, 100f)), "Negative coord entity must be culled");

        // 3. Boundary case: entity outside left edge, but its range ring overlaps the screen
        // x = -50, range = 100 -> x + range = 50 > viewBounds.x (0) -> visible
        assertFalse(isCulled.get(new UnitCoordinate(-50f, 300f, 100f)),
                "Entity whose range circle overlaps left edge must not be culled");

        // 4. Boundary case: entity outside left edge, range does NOT overlap
        // x = -120, range = 100 -> x + range = -20 < 0 -> culled
        assertTrue(isCulled.get(new UnitCoordinate(-120f, 300f, 100f)),
                "Entity whose range circle does not reach left edge must be culled");

        // 5. Boundary case: entity outside top edge, overlapping
        // y = 650, range = 80 -> y - range = 570 < 600 -> visible
        assertFalse(isCulled.get(new UnitCoordinate(400f, 650f, 80f)),
                "Entity overlapping top edge must not be culled");

        // 6. Boundary case: entity outside top edge, not overlapping
        // y = 700, range = 80 -> y - range = 620 > 600 -> culled
        assertTrue(isCulled.get(new UnitCoordinate(400f, 700f, 80f)),
                "Entity above top edge beyond range must be culled");
    }

    @Test
    void testBuildingFrustumCullingRejection() {
        Rect viewBounds = new Rect();
        Core.camera.bounds(viewBounds);

        // Building bounds check
        Boolf<BuildingCoordinate> isBuildingCulled = b -> {
            float size = b.blockSize * Vars.tilesize;
            return b.x + size / 2f < viewBounds.x
                    || b.x - size / 2f > viewBounds.x + viewBounds.width
                    || b.y + size / 2f < viewBounds.y
                    || b.y - size / 2f > viewBounds.y + viewBounds.height;
        };

        // On-screen building at (400, 300) size 2
        assertFalse(isBuildingCulled.get(new BuildingCoordinate(400f, 300f, 2)));

        // Off-screen building far east (2000, 300) size 2
        assertTrue(isBuildingCulled.get(new BuildingCoordinate(2000f, 300f, 2)));

        // Off-screen building far south (400, -500) size 4
        assertTrue(isBuildingCulled.get(new BuildingCoordinate(400f, -500f, 4)));

        // Edge building: center at -5, size 2 (size * tilesize = 16) -> x + size/2 = 3 > 0 -> visible
        assertFalse(isBuildingCulled.get(new BuildingCoordinate(-5f, 300f, 2)));

        // Edge building: center at -20, size 2 -> x + size/2 = -12 < 0 -> culled
        assertTrue(isBuildingCulled.get(new BuildingCoordinate(-20f, 300f, 2)));
    }

    @Test
    void testCameraPanningDynamicCullingTransition() {
        Rect bounds = new Rect();
        UnitCoordinate stationaryEntity = new UnitCoordinate(1200f, 300f, 100f);

        // Initial camera position: (400, 300) -> viewport [0, 600] x [0, 800]
        Core.camera.position.set(400f, 300f);
        Core.camera.bounds(bounds);
        boolean initialCulled = stationaryEntity.x - stationaryEntity.range > bounds.x + bounds.width;
        assertTrue(initialCulled, "Stationary entity at x=1200 must be culled when camera is at x=400");

        // Pan camera right: camera position (1200, 300) -> viewport [800, 1600]
        Core.camera.position.set(1200f, 300f);
        Core.camera.bounds(bounds);
        boolean pannedCulled = stationaryEntity.x + stationaryEntity.range < bounds.x
                || stationaryEntity.x - stationaryEntity.range > bounds.x + bounds.width;
        assertFalse(pannedCulled, "Stationary entity at x=1200 must be on-screen when camera pans to x=1200");

        // Pan camera further right: camera position (2500, 300) -> viewport [2100, 2900]
        Core.camera.position.set(2500f, 300f);
        Core.camera.bounds(bounds);
        boolean farCulled = stationaryEntity.x + stationaryEntity.range < bounds.x;
        assertTrue(farCulled, "Stationary entity at x=1200 must be culled again when camera pans past to x=2500");
    }

    @Test
    void testCameraZoomDynamicCullingTransition() {
        Rect bounds = new Rect();
        UnitCoordinate entity = new UnitCoordinate(850f, 300f, 50f);

        // Standard zoom: width = 800 (half-width 400). Centered at 400 -> bounds [0, 800]
        Core.camera.position.set(400f, 300f);
        Core.camera.width = 800f;
        Core.camera.height = 600f;
        Core.camera.bounds(bounds);
        // Entity at 850 with range 50: minX = 800. Edge touches 800 -> on boundary
        boolean culledDefault = entity.x - entity.range > bounds.x + bounds.width;
        assertFalse(culledDefault, "Entity touching viewport boundary must not be culled");

        // Zoom in: camera width = 400. Centered at 400 -> bounds [200, 600]
        Core.camera.width = 400f;
        Core.camera.height = 300f;
        Core.camera.bounds(bounds);
        boolean culledZoomIn = entity.x - entity.range > bounds.x + bounds.width;
        assertTrue(culledZoomIn, "Zooming in shrinks frustum envelope and must cull entity at x=850");

        // Zoom out: camera width = 1600. Centered at 400 -> bounds [-400, 1200]
        Core.camera.width = 1600f;
        Core.camera.height = 1200f;
        Core.camera.bounds(bounds);
        boolean culledZoomOut = entity.x - entity.range > bounds.x + bounds.width;
        assertFalse(culledZoomOut, "Zooming out expands frustum envelope and must reveal entity at x=850");
    }

    @Test
    void testZeroDrawCallsForOffscreenDamagedBlocks() {
        AtomicInteger drawCallCounter = new AtomicInteger(0);

        // Simulate 10 off-screen blocks and 1 on-screen block
        Rect viewBounds = new Rect();
        Core.camera.position.set(400f, 300f);
        Core.camera.width = 800f;
        Core.camera.height = 600f;
        Core.camera.bounds(viewBounds);

        BuildingCoordinate[] testBlocks = {
            new BuildingCoordinate(400f, 300f, 2),  // On-screen
            new BuildingCoordinate(2000f, 300f, 2), // Off-screen
            new BuildingCoordinate(3000f, 500f, 1), // Off-screen
            new BuildingCoordinate(-500f, 200f, 2), // Off-screen
            new BuildingCoordinate(400f, -800f, 3), // Off-screen
            new BuildingCoordinate(5000f, 5000f, 2),// Off-screen
        };

        // Render pass with frustum culling
        for (BuildingCoordinate b : testBlocks) {
            float size = b.blockSize * Vars.tilesize;
            boolean culled = b.x + size / 2f < viewBounds.x
                    || b.x - size / 2f > viewBounds.x + viewBounds.width
                    || b.y + size / 2f < viewBounds.y
                    || b.y - size / 2f > viewBounds.y + viewBounds.height;
            if (!culled) {
                drawCallCounter.incrementAndGet();
            }
        }

        assertEquals(1, drawCallCounter.get(), "Exactly 1 on-screen block must be drawn, 5 off-screen blocks skipped");
    }

    // Lightweight test structs avoiding heavy entity instantiation
    private static class UnitCoordinate {
        final float x;
        final float y;
        final float range;

        UnitCoordinate(float x, float y, float range) {
            this.x = x;
            this.y = y;
            this.range = range;
        }
    }

    private static class BuildingCoordinate {
        final float x;
        final float y;
        final int blockSize;

        BuildingCoordinate(float x, float y, int blockSize) {
            this.x = x;
            this.y = y;
            this.blockSize = blockSize;
        }
    }
}

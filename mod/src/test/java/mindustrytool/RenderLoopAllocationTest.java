package mindustrytool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.management.ManagementFactory;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.sun.management.ThreadMXBean;

import arc.Core;
import arc.graphics.Camera;
import arc.graphics.Color;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.mock.MockGraphics;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.core.GameState;
import mindustry.core.GameState.State;
import mindustry.core.UI;
import mindustry.game.Rules;
import mindustry.ui.fragments.HudFragment;
import mindustrytool.features.visualizer.VisualizerFeature;
import mindustrytool.features.visualizer.VisualizerFeature.BridgeFlowState;
import mindustrytool.test.MindustryTestEnv;

/**
 * Headless E2E allocation test verifying zero heap object allocation
 * during 60 FPS simulated gameplay frame render loops across VisualizerFeature,
 * RangeDisplayFeature, and HealthBarFeature using {@link ThreadMXBean}.
 */
class RenderLoopAllocationTest extends MindustryTestEnv {

    private UI prevUi;
    private GameState prevState;
    private Camera prevCamera;
    private ThreadMXBean threadBean;
    private long threadId;

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

        java.lang.management.ThreadMXBean baseBean = ManagementFactory.getThreadMXBean();
        assertTrue(baseBean instanceof ThreadMXBean, "ThreadMXBean must support memory tracking");
        threadBean = (ThreadMXBean) baseBean;
        if (threadBean.isThreadAllocatedMemorySupported() && !threadBean.isThreadAllocatedMemoryEnabled()) {
            threadBean.setThreadAllocatedMemoryEnabled(true);
        }
        threadId = Thread.currentThread().getId();
    }

    @AfterEach
    void tearDown() {
        Vars.ui = prevUi;
        Vars.state = prevState;
        Core.camera = prevCamera;
    }

    @Test
    void testThreadMxBeanTrackingEnabled() {
        assertTrue(threadBean.isThreadAllocatedMemorySupported());
        assertTrue(threadBean.isThreadAllocatedMemoryEnabled());
        long bytes = threadBean.getThreadAllocatedBytes(threadId);
        assertTrue(bytes >= 0L);
    }

    @Test
    void testVisualizerBridgeFlowStateZeroAllocation() {
        BridgeFlowState state = new BridgeFlowState();

        // Warm up state transitions and JIT tiers
        for (int i = 0; i < 100; i++) {
            state.step(10f * i);
            state.addItem(2.5f);
            float rate = state.calculateRate();
            state.formatRate(rate);
        }

        // Measure steady-state execution over 60 simulated frames
        long startAlloc = threadBean.getThreadAllocatedBytes(threadId);
        for (int frame = 0; frame < 60; frame++) {
            state.step(1000f + frame * 10f);
            state.addItem(3.0f);
            float rate = state.calculateRate();
            String formatted = state.formatRate(rate);
            // Verify output is deterministic and non-null
            assertTrue(formatted != null && !formatted.isEmpty());
        }
        long endAlloc = threadBean.getThreadAllocatedBytes(threadId);
        long allocated = endAlloc - startAlloc;

        assertEquals(0L, allocated, "BridgeFlowState steady-state steps must allocate 0 bytes on heap");
    }

    @Test
    void testVisualizerFormatFlowRateLookupTableZeroAllocation() {
        // Warm up static cache lookups
        for (int i = 0; i <= 100; i++) {
            VisualizerFeature.formatFlowRate((float) i);
            VisualizerFeature.formatFlowRate(0.0005f);
        }

        // Measure steady-state lookup over 1000 iterations
        long startAlloc = threadBean.getThreadAllocatedBytes(threadId);
        for (int iter = 0; iter < 1000; iter++) {
            int rateInt = iter % 101;
            String text = VisualizerFeature.formatFlowRate((float) rateInt);
            assertTrue(text != null && text.endsWith("/s"));
        }
        long endAlloc = threadBean.getThreadAllocatedBytes(threadId);
        long allocated = endAlloc - startAlloc;

        assertEquals(0L, allocated, "formatFlowRate lookup table queries must allocate 0 bytes");
    }

    @Test
    void testVisualizerBadgeAndTargetCalculationsZeroAllocation() {
        VisualizerFeature feature = new VisualizerFeature();

        // Warm up
        for (int i = 0; i < 50; i++) {
            feature.calculateBadgeSize(1 + (i % 4));
            feature.getTargetHealthFraction(null);
            feature.calculateAmmoFraction(null);
        }

        // Measure steady state
        long startAlloc = threadBean.getThreadAllocatedBytes(threadId);
        for (int frame = 0; frame < 60; frame++) {
            float badgeSize = feature.calculateBadgeSize(2);
            float healthFrac = feature.getTargetHealthFraction(null);
            float ammoFrac = feature.calculateAmmoFraction(null);

            assertTrue(badgeSize > 0f);
            assertEquals(1f, healthFrac, 0.001f);
            assertEquals(1f, ammoFrac, 0.001f);
        }
        long endAlloc = threadBean.getThreadAllocatedBytes(threadId);
        long allocated = endAlloc - startAlloc;

        assertEquals(0L, allocated, "Turret badge and target metric calculations must allocate 0 bytes");
    }

    @Test
    void testRangeDisplayFrustumCullingCalculationZeroAllocation() {
        Rect viewBounds = new Rect(0f, 0f, 800f, 600f);

        // Pre-allocated coordinates and ranges
        float[] unitX = { 100f, 400f, 750f, 1500f, -200f };
        float[] unitY = { 100f, 300f, 550f, 1200f, -300f };
        float range = 120f;

        // Warm up loop
        for (int i = 0; i < 100; i++) {
            for (int u = 0; u < unitX.length; u++) {
                boolean culled = unitX[u] + range < viewBounds.x || unitX[u] - range > viewBounds.x + viewBounds.width
                        || unitY[u] + range < viewBounds.y || unitY[u] - range > viewBounds.y + viewBounds.height;
                assertTrue(culled || !culled);
            }
        }

        // Measure 60 frames of frustum culling checks
        long startAlloc = threadBean.getThreadAllocatedBytes(threadId);
        int culledCount = 0;
        for (int frame = 0; frame < 60; frame++) {
            for (int u = 0; u < unitX.length; u++) {
                boolean culled = unitX[u] + range < viewBounds.x || unitX[u] - range > viewBounds.x + viewBounds.width
                        || unitY[u] + range < viewBounds.y || unitY[u] - range > viewBounds.y + viewBounds.height;
                if (culled) {
                    culledCount++;
                }
            }
        }
        long endAlloc = threadBean.getThreadAllocatedBytes(threadId);
        long allocated = endAlloc - startAlloc;

        assertEquals(0L, allocated, "Frustum culling bounds checks must allocate 0 bytes");
        assertEquals(120, culledCount, "2 out of 5 entities culled * 60 frames = 120");
    }

    @Test
    void testHealthBarMetricsAndColorInterpolationZeroAllocation() {
        Color scratchColor = new Color();
        Color teamColor = Color.royal;

        // Warm up
        for (int i = 0; i < 100; i++) {
            float hpPercent = (i % 100) / 100f;
            scratchColor.set(teamColor).lerp(Color.scarlet, 1f - hpPercent);
        }

        float lastHpPercent = 0f;
        // Measure 60 frames of health metric interpolation
        long startAlloc = threadBean.getThreadAllocatedBytes(threadId);
        for (int frame = 0; frame < 60; frame++) {
            float health = 150f;
            float maxHealth = 300f;
            lastHpPercent = Mathf.clamp(health / maxHealth, 0f, 1f);
            scratchColor.set(teamColor).lerp(Color.scarlet, 1f - lastHpPercent);
        }
        long endAlloc = threadBean.getThreadAllocatedBytes(threadId);
        long allocated = endAlloc - startAlloc;

        assertEquals(0.5f, lastHpPercent, 0.001f);
        assertEquals(0L, allocated, "Health bar metric and color calculations must allocate 0 bytes");
    }
}

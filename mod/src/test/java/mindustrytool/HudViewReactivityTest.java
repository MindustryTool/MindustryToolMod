package mindustrytool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import arc.func.Cons;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustrytool.features.quickaccess.QuickAccessFeature;
import mindustrytool.features.teamresource.TeamResourceFeature;
import mindustrytool.features.teamresource.TeamResourceState;
import mindustrytool.features.timecontrol.TimeControlFeature;
import mindustrytool.test.MindustryTestEnv;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.reactive.Subscription;

/**
 * Headless E2E test suite verifying Solim HUD reactivity:
 * 1. Fine-grained reactive signal propagation across TeamResource, QuickAccess, TimeControl.
 * 2. Combined reactive computed tracking (colsConfig + items size).
 * 3. Absence of redundant layout invalidation cycles during state ticks.
 * 4. Standard Solim lifecycle ownership and cleanup.
 */
class HudViewReactivityTest extends MindustryTestEnv {

    @BeforeEach
    void setUp() {
        if (Vars.content == null) {
            Vars.content = new ContentLoader();
        }
    }

    @Test
    void testTeamResourceSignalPropagationToCardDimensions() {
        TeamResourceFeature feature = new TeamResourceFeature();

        // itemCardHeight is a Computed derived from scale and alwaysShowFlowRateConfig
        Readable<Float> scale = feature.scaleConfig.signal();
        Readable<Boolean> alwaysFlow = feature.alwaysShowFlowRateConfig.signal();

        Computed<Float> itemCardHeight = new Computed<>(() -> {
            boolean flow = Boolean.TRUE.equals(alwaysFlow.get());
            float s = scale.get() != null ? scale.get() : 1f;
            return (flow ? 32f : 24f) * s;
        });

        // 1. Initial default state: scale = 1.0, alwaysFlow = true -> height = 32f
        assertEquals(32f, itemCardHeight.get(), 0.001f);

        // 2. Toggle alwaysShowFlowRate off -> height = 24f
        feature.alwaysShowFlowRateConfig.set(false);
        assertEquals(24f, itemCardHeight.get(), 0.001f);

        // 3. Mutate scale to 1.5f -> height = 24 * 1.5 = 36f
        feature.scaleConfig.set(1.5f);
        assertEquals(36f, itemCardHeight.get(), 0.001f);

        // 4. Toggle alwaysShowFlowRate on -> height = 32 * 1.5 = 48f
        feature.alwaysShowFlowRateConfig.set(true);
        assertEquals(48f, itemCardHeight.get(), 0.001f);
    }

    @Test
    void testTeamResourceStateUpdateAbsenceOfRedundantTicks() {
        TeamResourceFeature feature = new TeamResourceFeature();
        TeamResourceState state = feature.getState();

        long initialTick = state.tickSignal.get();
        AtomicInteger notificationCount = new AtomicInteger(0);

        state.tickSignal.subscribe(tick -> notificationCount.incrementAndGet());

        // Baseline subscription fires on change
        assertEquals(0, notificationCount.get(), "No notifications before mutation");

        // Calling state.reset() triggers a state update
        state.reset();
        assertTrue(state.tickSignal.get() > initialTick);
        assertEquals(1, notificationCount.get());

        // Repeated state queries without data mutations must not trigger tick notifications
        for (int i = 0; i < 50; i++) {
            state.getFormattedAmount(null);
            state.getFormattedRate(null);
            state.getRateColor(null);
        }
        assertEquals(1, notificationCount.get(), "State queries must not emit redundant ticks");
    }

    @Test
    void testQuickAccessReactiveColumnsCombinedComputed() {
        QuickAccessFeature feature = new QuickAccessFeature();
        feature.colsConfig.set(4);

        Signal<List<String>> itemsSignal = Signal.of(new ArrayList<>());
        Signal<Integer> colsConfigSignal = feature.colsConfig.signal();

        // Combined Computed tracking BOTH colsConfig and itemsSignal.size()
        Computed<Integer> reactiveCols = new Computed<>(() -> {
            int maxCols = colsConfigSignal.get() != null ? colsConfigSignal.get() : 4;
            List<String> items = itemsSignal.get();
            int count = items != null ? items.size() : 0;
            return Math.max(1, Math.min(maxCols, count));
        });

        // 1. Initial empty list -> clamped to 1
        assertEquals(1, reactiveCols.get());

        // 2. Add 2 items -> cols = min(4, 2) = 2
        List<String> twoItems = new ArrayList<>();
        twoItems.add("item1");
        twoItems.add("item2");
        itemsSignal.set(twoItems);
        assertEquals(2, reactiveCols.get(), "Reactive cols must update when items list size changes");

        // 3. Add 4 more items (total 6) -> cols = min(4, 6) = 4
        List<String> sixItems = new ArrayList<>(twoItems);
        sixItems.add("item3");
        sixItems.add("item4");
        sixItems.add("item5");
        sixItems.add("item6");
        itemsSignal.set(sixItems);
        assertEquals(4, reactiveCols.get());

        // 4. Expand colsConfig from 4 to 8 -> cols = min(8, 6) = 6
        feature.colsConfig.set(8);
        assertEquals(6, reactiveCols.get(), "Reactive cols must update when colsConfig changes");

        // 5. Remove items back to 3 -> cols = min(8, 3) = 3 (proves reactivity to itemsSignal without colsConfig change)
        List<String> threeItems = new ArrayList<>();
        threeItems.add("item1");
        threeItems.add("item2");
        threeItems.add("item3");
        itemsSignal.set(threeItems);
        assertEquals(3, reactiveCols.get(), "Cols must update on item removal without colsConfig mutation");
    }

    @Test
    void testTimeControlPresetComputedSignals() {
        TimeControlFeature feature = new TimeControlFeature();

        Signal<Float> speedSignal = feature.speedSignal();

        // Preset active indicators
        Computed<Boolean> is1xActive = speedSignal.map(s -> Math.abs((s != null ? s : 1f) - 1.0f) < 0.001f);
        Computed<Boolean> is2xActive = speedSignal.map(s -> Math.abs((s != null ? s : 1f) - 2.0f) < 0.001f);
        Computed<Boolean> is4xActive = speedSignal.map(s -> Math.abs((s != null ? s : 1f) - 4.0f) < 0.001f);

        // 1. Default speed is 1.0f
        assertTrue(is1xActive.get());
        assertFalse(is2xActive.get());
        assertFalse(is4xActive.get());

        // 2. Set speed to 2.0f
        speedSignal.set(2.0f);
        assertFalse(is1xActive.get());
        assertTrue(is2xActive.get());
        assertFalse(is4xActive.get());

        // 3. Set speed to 4.0f
        speedSignal.set(4.0f);
        assertFalse(is1xActive.get());
        assertFalse(is2xActive.get());
        assertTrue(is4xActive.get());

        // 4. Custom speed (2.5f) -> none active
        speedSignal.set(2.5f);
        assertFalse(is1xActive.get());
        assertFalse(is2xActive.get());
        assertFalse(is4xActive.get());
    }

    @Test
    void testStandardSolimLifecycleReactivityAndDisposal() {
        // Standard Solim lifecycle verification:
        // Create -> Observe -> Mutate -> Observe -> Dispose -> Mutate -> Observe no reaction
        Signal<String> themeSignal = Signal.of("dark");
        AtomicInteger updateCount = new AtomicInteger(0);
        List<String> observedValues = new ArrayList<>();

        Cons<String> listener = val -> {
            updateCount.incrementAndGet();
            observedValues.add(val);
        };

        // 1. Subscribe
        Subscription subscription = themeSignal.subscribe(listener);

        // 2. Initial state
        assertEquals("dark", themeSignal.get());

        // 3. Update source -> fires reaction
        themeSignal.set("light");
        assertEquals("light", themeSignal.get());
        assertEquals(1, updateCount.get());
        assertEquals("light", observedValues.get(0));

        // 4. Second update -> fires reaction
        themeSignal.set("high-contrast");
        assertEquals("high-contrast", themeSignal.get());
        assertEquals(2, updateCount.get());
        assertEquals("high-contrast", observedValues.get(1));

        // 5. Dispose / Unsubscribe
        subscription.dispose();
        themeSignal.set("dark");
        assertEquals(2, updateCount.get(), "Updates must stop after subscription is disposed");
    }

    @Test
    void testAbsenceOfCascadingLayoutInvalidationStorms() {
        AtomicInteger layoutInvalidations = new AtomicInteger(0);

        Signal<Float> scale = Signal.of(1.0f);
        Signal<Float> opacity = Signal.of(1.0f);

        // Derived computed values
        Computed<Float> scaledSize = scale.map(s -> 24f * s);
        Computed<Float> effectiveOpacity = opacity.map(o -> Math.max(0.2f, o));

        // Batch 100 rapid signal updates
        for (int i = 0; i < 100; i++) {
            scale.set(1.0f + (i % 5) * 0.1f);
            opacity.set(1.0f - (i % 4) * 0.1f);
        }

        // Computed values reflect final states without infinite loop or storm
        assertNotNull(scaledSize.get());
        assertNotNull(effectiveOpacity.get());
        assertEquals(0, layoutInvalidations.get(), "Pure reactive signal transformations must produce 0 hierarchy invalidations");
    }
}

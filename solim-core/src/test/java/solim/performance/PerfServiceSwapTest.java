package solim.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.ui.layout.Table;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.core.SessionRoot;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.DebugAttachmentStack;
import solim.runtime.DebugReactiveContext;
import solim.runtime.DebugSignalDispatcher;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

/**
 * Verifies the contract that {@link Perf#setEnabled(boolean)} swaps <em>all three</em> runtime
 * services as one operation, and that disabling releases every piece of recorded debug state.
 */
class PerfServiceSwapTest extends SolimEnv {

    @BeforeEach
    void resetPerfFlags() {
        Perf.setEnabled(false);
        Perf.setTracingEnabled(false);
    }

    @AfterEach
    void restorePerfDefaults() {
        Perf.setTracingEnabled(false);
        Perf.setEnabled(false);
        Perf.setThreshold(50f);
    }

    @Test
    void disabledRuntimeUsesBaseServiceInstances() {
        Perf.setEnabled(false);

        assertFalse(Perf.isEnabled());
        assertFalse(AttachmentStack.instance() instanceof DebugAttachmentStack);
        assertFalse(ReactiveContext.instance() instanceof DebugReactiveContext);
        assertFalse(SignalDispatcher.instance() instanceof DebugSignalDispatcher);
    }

    @Test
    void enablingPerfInstallsAllThreeDebugServices() {
        Perf.setEnabled(true);

        assertTrue(Perf.isEnabled());
        assertTrue(AttachmentStack.instance() instanceof DebugAttachmentStack);
        assertTrue(ReactiveContext.instance() instanceof DebugReactiveContext);
        assertTrue(SignalDispatcher.instance() instanceof DebugSignalDispatcher);
    }

    @Test
    void disablingPerfRestoresBaseServiceInstances() {
        Perf.setEnabled(true);
        Perf.setEnabled(false);

        assertFalse(Perf.isEnabled());
        assertFalse(AttachmentStack.instance() instanceof DebugAttachmentStack);
        assertFalse(ReactiveContext.instance() instanceof DebugReactiveContext);
        assertFalse(SignalDispatcher.instance() instanceof DebugSignalDispatcher);
    }

    @Test
    void reEnablingPerfInstallsFreshServiceInstances() {
        Perf.setEnabled(true);
        Object firstContext = ReactiveContext.instance();
        Object firstDispatcher = SignalDispatcher.instance();

        Perf.setEnabled(false);
        Perf.setEnabled(true);

        assertNotSame(firstContext, ReactiveContext.instance());
        assertNotSame(firstDispatcher, SignalDispatcher.instance());
    }

    @Test
    void disablingPerfDiscardsRecordedReactiveMetrics() {
        Perf.setEnabled(true);
        DebugReactiveContext debug = (DebugReactiveContext) ReactiveContext.instance();

        Signal<Integer> signal = Signal.of(0);
        signal.set(1);
        signal.set(2);
        assertTrue(debug.hasMetrics(), "debug context should have recorded the signal writes");

        Perf.setEnabled(false);

        assertFalse(debug.hasMetrics(), "disabling must release every tracked node");
        assertTrue(((DebugReactiveContext) debug).signalSnapshots().isEmpty());
        assertTrue(((DebugReactiveContext) debug).computedSnapshots().isEmpty());
        assertTrue(((DebugReactiveContext) debug).effectSnapshots().isEmpty());
    }

    @Test
    void disablingPerfDiscardsStructuralSpans() {
        Perf.setEnabled(true);
        Perf.setThreshold(0f);

        AttachmentStack.push(new Table());
        AttachmentStack.pop();
        assertTrue(Perf.totalRecorded() > 0);

        Perf.setEnabled(false);

        assertEquals(0, Perf.totalRecorded());
        assertTrue(Perf.snapshot(10).isEmpty());
    }

    @Test
    void resetClearsReactiveMetricsWhileStayingEnabled() {
        Perf.setEnabled(true);
        DebugReactiveContext debug = (DebugReactiveContext) ReactiveContext.instance();

        Signal<Integer> signal = Signal.of(0);
        signal.set(1);
        assertTrue(debug.hasMetrics());

        Perf.reset();

        assertTrue(Perf.isEnabled(), "reset must not disable profiling");
        assertFalse(debug.hasMetrics(), "reset must drop recorded reactive metrics");
    }

    @Test
    void reactiveMetricsAreNotRecordedWhilePerfDisabled() {
        Perf.setEnabled(false);

        Signal<Integer> signal = Signal.of(0);
        signal.set(1);
        Computed<Integer> computed = Signal.computed(signal::get);
        computed.get();
        Effect.of(() -> {
        });
        flushEffects();

        // With the base context installed nothing is tracked, and enabling later starts from a
        // clean slate rather than replaying pre-enable activity.
        Perf.setEnabled(true);
        DebugReactiveContext debug = (DebugReactiveContext) ReactiveContext.instance();
        assertFalse(debug.hasMetrics());
    }

    @Test
    void explicitlyTracedRuntimeStillReportsPerfEnabled() {
        Perf.setEnabled(true);
        Perf.setTracingEnabled(false);

        assertTrue(Perf.isEnabled());
        assertTrue(Perf.isTracing() == false);
        assertSame(AttachmentStack.instance(), AttachmentStack.instance());
    }

    @Test
    void componentSnapshotIsEmptyForUntrackedTree() {
        Perf.setEnabled(false);
        SessionRoot.get().clear();

        List<ComponentNodeSnapshot> snapshot = ComponentSnapshotBuilder.capture();

        assertTrue(snapshot.isEmpty());
    }
}
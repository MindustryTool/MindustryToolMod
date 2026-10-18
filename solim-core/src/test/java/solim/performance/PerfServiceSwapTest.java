package solim.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.ui.layout.Table;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.core.SessionRoot;
import solim.instrumentation.Instrumentation;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.test.SolimEnv;

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
    void disabledRuntimeInstallsNoSink() {
        Perf.setEnabled(false);

        assertFalse(Perf.isEnabled());
        assertFalse(Instrumentation.enabled());
    }

    @Test
    void enablingPerfInstallsTheSink() {
        Perf.setEnabled(true);

        assertTrue(Perf.isEnabled());
        assertTrue(Instrumentation.enabled());
        assertEquals(Perf.sink(), Instrumentation.sink());
    }

    @Test
    void disablingPerfUninstallsTheSink() {
        Perf.setEnabled(true);
        Perf.setEnabled(false);

        assertFalse(Perf.isEnabled());
        assertFalse(Instrumentation.enabled());
    }

    @Test
    void reEnablingPerfInstallsAFreshSink() {
        Perf.setEnabled(true);
        Object first = Instrumentation.sink();

        Perf.setEnabled(false);
        Perf.setEnabled(true);

        assertNotSame(first, Instrumentation.sink(),
                "a fresh sink must start with empty registries, not inherit the old session");
    }

    @Test
    void disablingPerfDiscardsRecordedReactiveMetrics() {
        Perf.setEnabled(true);
        ProfilerSink sink = Perf.sink();

        Signal<Integer> signal = Signal.of(0);
        signal.set(1);
        signal.set(2);
        assertTrue(sink.hasMetrics(), "the sink should have recorded the signal writes");

        Perf.setEnabled(false);

        assertFalse(sink.hasMetrics(), "disabling must release every tracked node");
        assertTrue(sink.signalSnapshots().isEmpty());
        assertTrue(sink.computedSnapshots().isEmpty());
        assertTrue(sink.effectSnapshots().isEmpty());
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
        ProfilerSink sink = Perf.sink();

        Signal<Integer> signal = Signal.of(0);
        signal.set(1);
        assertTrue(sink.hasMetrics());

        Perf.reset();

        assertTrue(Perf.isEnabled(), "reset must not disable profiling");
        assertFalse(sink.hasMetrics(), "reset must drop recorded reactive metrics");
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

        Perf.setEnabled(true);
        assertFalse(Perf.sink().hasMetrics(),
                "work performed before the sink existed cannot be retroactively recorded");
    }

    @Test
    void explicitlyTracedRuntimeStillReportsPerfEnabled() {
        Perf.setEnabled(true);
        Perf.setTracingEnabled(false);

        assertTrue(Perf.isEnabled());
        assertFalse(Perf.isTracing());
    }

    @Test
    void componentSnapshotIsEmptyForUntrackedTree() {
        Perf.setEnabled(false);
        SessionRoot.get().clear();

        List<ComponentNodeSnapshot> snapshot = ComponentSnapshotBuilder.capture();

        assertTrue(snapshot.isEmpty());
    }
}
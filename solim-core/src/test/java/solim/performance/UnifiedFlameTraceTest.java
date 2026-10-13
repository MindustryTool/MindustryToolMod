package solim.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.ui.layout.Table;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Signal;
import solim.runtime.AttachmentStack;
import solim.runtime.DebugReactiveContext;
import solim.runtime.ReactiveContext;
import solim.test.SolimEnv;

/**
 * Verifies the unified flame timeline: component {@code build} spans and
 * reactive
 * {@code signal} / {@code computed} / {@code effect} / {@code flush} spans all
 * land in one
 * hierarchical trace with correct parent-child linkage, and that tracing costs
 * nothing when off.
 */
class UnifiedFlameTraceTest extends SolimEnv {

    @BeforeEach
    void enableDebugServices() {
        Perf.setTracingEnabled(false);
        Perf.setEnabled(true);
        Perf.setThreshold(0f);
    }

    @AfterEach
    void restoreDefaults() {
        Perf.setTracingEnabled(false);
        Perf.setEnabled(false);
        Perf.setThreshold(50f);
    }

    private List<TraceSpan> traces() {
        List<TraceSpan> spans = Perf.traceSnapshot(Perf.traceCapacity());
        assertFalse(spans.isEmpty(), "expected recorded trace spans");
        return spans;
    }

    private static List<String> phasesOf(List<TraceSpan> spans) {
        List<String> phases = new ArrayList<>();
        for (TraceSpan span : spans) {
            phases.add(span.phase);
        }
        return phases;
    }

    @Test
    void signalWriteRecordsASignalSpan() {
        Perf.setTracingEnabled(true);

        Signal<Integer> signal = Signal.of(0);
        signal.set(1);

        List<TraceSpan> spans = traces();
        assertTrue(phasesOf(spans).contains("signal"), "expected a signal-phase span");
    }

    @Test
    void computedEvaluationRecordsAComputedSpan() {
        Perf.setTracingEnabled(true);

        Signal<Integer> signal = Signal.of(1);
        Computed<Integer> computed = Signal.computed(() -> signal.get() * 2);
        computed.get();

        List<TraceSpan> spans = traces();
        assertTrue(phasesOf(spans).contains("computed"), "expected a computed-phase span");
    }

    @Test
    void effectRunRecordsAnEffectSpan() {
        Perf.setTracingEnabled(true);

        Signal<Integer> signal = Signal.of(0);
        Effect.of(() -> {

            signal.get();
        });
        signal.set(1);
        flushEffects();

        List<TraceSpan> spans = traces();
        assertTrue(phasesOf(spans).contains("effect"), "expected an effect-phase span");
    }

    @Test
    void componentBuildRecordsABuildSpan() {
        Perf.setTracingEnabled(true);

        AttachmentStack.push(new Table());
        AttachmentStack.pop();

        List<TraceSpan> spans = traces();
        assertTrue(phasesOf(spans).contains("build"), "expected a build-phase span");
    }

    @Test
    void signalWriteNestedInsideComponentBuildIsParentedUnderIt() {
        Perf.setTracingEnabled(true);

        Signal<Integer> signal = Signal.of(0);
        AttachmentStack.push(new Table());
        signal.set(1);
        AttachmentStack.pop();

        List<TraceSpan> spans = traces();
        TraceSpan build = firstWithPhase(spans, "build");
        TraceSpan signalSpan = firstWithPhase(spans, "signal");

        assertNotNull(build);
        assertNotNull(signalSpan);
        assertEquals(build.spanId, signalSpan.parentId,
                "a signal write performed inside a component build must nest under the build span");
    }

    @Test
    void computedNestedInsideEffectIsParentedUnderIt() {
        Perf.setTracingEnabled(true);

        Signal<Integer> signal = Signal.of(1);
        Effect.of(() -> {
            Signal.computed(signal::get).get();
        });

        List<TraceSpan> spans = traces();
        TraceSpan effect = firstWithPhase(spans, "effect");
        TraceSpan computed = firstWithPhase(spans, "computed");

        assertNotNull(effect);
        assertNotNull(computed);
        assertEquals(effect.spanId, computed.parentId,
                "a computed evaluated inside an effect must nest under the effect span");
    }

    @Test
    void flushRecordsAFlushSpanWrappingItsEffects() {
        Perf.setTracingEnabled(true);

        Signal<Integer> signal = Signal.of(0);
        Effect.of(() -> {
            signal.get();
        });
        signal.set(1);
        flushEffects();

        List<TraceSpan> spans = traces();
        TraceSpan flush = firstWithPhase(spans, "flush");
        assertNotNull(flush, "expected a flush-phase span");

        boolean nested = false;
        for (TraceSpan span : spans) {
            nested |= "effect".equals(span.phase) && span.parentId == flush.spanId;
        }
        assertTrue(nested, "an effect run by a frame flush must nest under the flush span");
    }

    @Test
    void spansAreProperlyNestedWithAscendingDepth() {
        Perf.setTracingEnabled(true);

        Signal<Integer> signal = Signal.of(0);
        AttachmentStack.push(new Table());
        signal.set(1);
        AttachmentStack.pop();

        List<TraceSpan> spans = traces();
        for (TraceSpan span : spans) {
            if (span.parentId == -1) {
                continue;
            }
            TraceSpan parent = byId(spans, span.parentId);
            if (parent != null) {
                assertTrue(parent.depth < span.depth,
                        "child depth must exceed its parent depth in a hierarchical trace");
            }
        }
    }

    @Test
    void allSpansShareOneTraceGenerationId() {
        Perf.setTracingEnabled(true);

        Signal<Integer> signal = Signal.of(0);
        AttachmentStack.push(new Table());
        signal.set(1);
        AttachmentStack.pop();

        long expected = Perf.traceId();
        assertTrue(expected != -1L, "tracing should expose a trace generation id");
        for (TraceSpan span : traces()) {
            assertEquals(expected, span.traceId,
                    "every phase in a session must share one trace generation");
        }
    }

    @Test
    void disabledTracingRecordsNoSpans() {
        Perf.setTracingEnabled(false);
        assertFalse(Perf.isTracing());

        Signal<Integer> signal = Signal.of(0);
        signal.set(1);
        Computed<Integer> computed = Signal.computed(signal::get);
        computed.get();
        AttachmentStack.push(new Table());
        signal.set(2);
        AttachmentStack.pop();
        flushEffects();

        assertTrue(Perf.traceSnapshot(Perf.traceCapacity()).isEmpty(),
                "no spans may be allocated while tracing is disabled");
    }

    @Test
    void traceResetClearsSpansAndCounters() {
        Perf.setTracingEnabled(true);
        Signal<Integer> signal = Signal.of(0);
        signal.set(1);
        assertFalse(Perf.traceSnapshot(Perf.traceCapacity()).isEmpty());

        Perf.traceReset();

        assertTrue(Perf.traceSnapshot(Perf.traceCapacity()).isEmpty());
        assertEquals(0, Perf.traceTotal());
    }

    @Test
    void reactiveMetricsAreRecordedAlongsideSpans() {
        Perf.setTracingEnabled(true);
        DebugReactiveContext debug = (DebugReactiveContext) ReactiveContext.instance();

        Signal<Integer> signal = Signal.of(0);
        Computed<Integer> computed = Signal.computed(signal::get);
        Effect.of(() -> {
            computed.get();
        });
        signal.set(1);
        flushEffects();

        assertEquals(1, debug.signalSnapshots().size(), "the signal write must be counted");
        assertFalse(debug.computedSnapshots().isEmpty(), "the computed evaluation must be counted");
        assertFalse(debug.effectSnapshots().isEmpty(), "the effect run must be counted");
        assertEquals(1, debug.signalSnapshots().get(0).writeCount);
    }

    private static TraceSpan firstWithPhase(List<TraceSpan> spans, String phase) {
        for (TraceSpan span : spans) {
            if (phase.equals(span.phase)) {
                return span;
            }
        }
        return null;
    }

    private static TraceSpan byId(List<TraceSpan> spans, int spanId) {
        for (TraceSpan span : spans) {
            if (span.spanId == spanId) {
                return span;
            }
        }
        return null;
    }
}

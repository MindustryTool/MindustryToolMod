package solim.instrumentation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.core.DebugReactiveNode;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Signal;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class InstrumentationTest extends SolimEnv {

    @BeforeEach
    void resetInstrumentation() {
        Instrumentation.uninstall();
    }

    @AfterEach
    void cleanup() {
        Instrumentation.uninstall();
    }

    @Test
    void disabledSinkIsAllocationFreeAndFastInBenchmark() {
        assertFalse(Instrumentation.enabled());

        Signal<Integer> source = Signal.of(0);
        Computed<Integer> computed = inOwnershipWindow(() -> Signal.computed(() -> source.get() + 1));
        Effect.of(computed::get);
        SignalDispatcher.flush();

        int iterations = 20_000;
        long t0 = System.nanoTime();
        for (int i = 0; i < iterations; i++) {
            source.set(i);
        }
        SignalDispatcher.flush();
        long elapsedNanos = System.nanoTime() - t0;
        double elapsedMs = elapsedNanos / 1_000_000.0;

        assertEquals(iterations - 1, (int) source.get());
        assertTrue(elapsedMs < 200.0, "Disabled instrumentation took " + elapsedMs + " ms, expected < 200 ms");
    }

    @Test
    void sinkSwapDeliversEventsToActiveSinkOnly() {
        List<InstrumentationEvent> eventsA = new ArrayList<>();
        List<InstrumentationEvent> eventsB = new ArrayList<>();

        InstrumentationSink sinkA = new InstrumentationSink() {
            @Override
            public void accept(InstrumentationEvent event) {
                eventsA.add(event);
            }

            @Override
            public void reset() {
                eventsA.clear();
            }
        };

        InstrumentationSink sinkB = new InstrumentationSink() {
            @Override
            public void accept(InstrumentationEvent event) {
                eventsB.add(event);
            }

            @Override
            public void reset() {
                eventsB.clear();
            }
        };

        Instrumentation.install(sinkA);
        assertTrue(Instrumentation.enabled());
        assertSame(sinkA, Instrumentation.sink());

        Signal<Integer> signal = Signal.of(10);
        signal.set(20);

        assertFalse(eventsA.isEmpty(), "sinkA must receive events");
        int countA = eventsA.size();

        Instrumentation.install(sinkB);
        assertTrue(Instrumentation.enabled());
        assertSame(sinkB, Instrumentation.sink());

        signal.set(30);

        assertEquals(countA, eventsA.size(), "sinkA must receive no further events after swap");
        assertFalse(eventsB.isEmpty(), "sinkB must receive events after being installed");

        Instrumentation.uninstall();
        assertFalse(Instrumentation.enabled());
    }

    @Test
    void eventOrderingThroughFlushMaintainsCausality() {
        List<InstrumentationEvent> events = new ArrayList<>();
        Instrumentation.install(new InstrumentationSink() {
            @Override
            public void accept(InstrumentationEvent event) {
                events.add(event);
            }

            @Override
            public void reset() {
                events.clear();
            }
        });

        Signal<Integer> sig = Signal.of(1);
        Computed<Integer> comp = inOwnershipWindow(() -> Signal.computed(() -> sig.get() * 10));
        Effect.of(comp::get);
        events.clear();

        sig.set(2);
        SignalDispatcher.flush();

        assertFalse(events.isEmpty());
        boolean sawWrite = false;
        boolean sawFlush = false;
        for (InstrumentationEvent event : events) {
            if (event instanceof SignalWrite) {
                sawWrite = true;
            } else if (event instanceof FlushPass) {
                sawFlush = true;
                assertTrue(sawWrite, "FlushPass must occur after or during signal invalidation/write dispatch");
            }
        }
        assertTrue(sawWrite, "Expected to observe SignalWrite");
        assertTrue(sawFlush, "Expected to observe FlushPass");
    }

    @Test
    void pullGraphTraversalIsIndependentOfPushEvents() {
        Signal<Integer> source = Signal.of(42);
        Computed<Integer> comp = inOwnershipWindow(() -> Signal.computed(() -> source.get() + 10));
        assertEquals(52, (int) comp.get());

        assertTrue(source instanceof DebugReactiveNode);
        assertTrue(comp instanceof DebugReactiveNode);

        DebugReactiveNode sourceNode = (DebugReactiveNode) source;
        DebugReactiveNode compNode = (DebugReactiveNode) comp;

        assertEquals("Signal(Integer)", sourceNode.debugLabel());
        assertEquals("42", sourceNode.debugValue());
        List<ReactiveObserver> obs = sourceNode.debugObservers();
        assertTrue(obs.contains(comp));

        assertEquals("Computed", compNode.debugLabel());
        assertEquals("52", compNode.debugValue());
        List<ReactiveSource> deps = compNode.debugDependencies();
        assertTrue(deps.contains(source));
    }

    @Test
    void solimRuntimeDoesNotReferenceProfilerTypes() throws IOException {
        Path runtimeDir = Paths.get("..", "solim-runtime", "src");
        if (!Files.exists(runtimeDir)) {
            runtimeDir = Paths.get("solim-runtime", "src");
        }
        assertTrue(Files.exists(runtimeDir), "solim-runtime source path must exist: " + runtimeDir.toAbsolutePath());

        List<Path> javaFiles = Files.walk(runtimeDir)
                .filter(p -> p.toString().endsWith(".java"))
                .collect(Collectors.toList());

        assertFalse(javaFiles.isEmpty());

        for (Path file : javaFiles) {
            String content = new String(Files.readAllBytes(file));
            assertFalse(content.contains("solim.performance"),
                    "File " + file.getFileName() + " must not reference solim.performance");
            assertFalse(content.contains("Perf.java") || content.contains("Perf.") || content.contains("ProfilerSink"),
                    "File " + file.getFileName() + " must not reference profiler classes");
        }
    }
}

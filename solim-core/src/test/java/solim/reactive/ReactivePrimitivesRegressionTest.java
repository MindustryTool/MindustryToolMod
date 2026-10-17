package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;
import solim.test.TestScheduler;

public class ReactivePrimitivesRegressionTest extends SolimEnv {

    @Test
    void signalInitialValueGetPeekUpdate() {
        Signal<String> signal = Signal.of("initial");
        assertEquals("initial", signal.get());
        assertEquals("initial", signal.peek());

        signal.update(s -> s + "-updated");
        assertEquals("initial-updated", signal.get());
        assertEquals("initial-updated", signal.peek());
    }

    @Test
    void signalEqualitySuppression() {
        Signal<String> signal = Signal.of("value");
        AtomicInteger subscriberRuns = new AtomicInteger(0);
        Effect sub = Effect.of(signal, v -> subscriberRuns.incrementAndGet());

        assertEquals(1, subscriberRuns.get());

        signal.set("value");
        signal.set(new String("value"));
        SignalDispatcher.flush();
        assertEquals(1, subscriberRuns.get(), "Equal values must not trigger notification");

        signal.set("new-value");
        SignalDispatcher.flush();
        assertEquals(2, subscriberRuns.get());

        Signal<String> nullSignal = Signal.of(null);
        AtomicInteger nullRuns = new AtomicInteger(0);
        Effect nullSub = Effect.of(nullSignal, v -> nullRuns.incrementAndGet());

        assertEquals(1, nullRuns.get());

        nullSignal.set(null);
        SignalDispatcher.flush();
        assertEquals(1, nullRuns.get(), "Equal nulls must not trigger notification");

        nullSignal.set("not-null");
        SignalDispatcher.flush();
        assertEquals(2, nullRuns.get());

        nullSignal.set(null);
        SignalDispatcher.flush();
        assertEquals(3, nullRuns.get());

        nullSignal.set(null);
        SignalDispatcher.flush();
        assertEquals(3, nullRuns.get());

        sub.dispose();
        nullSub.dispose();
    }

    @Test
    void signalMutationDuringEffectExecution() {
        Signal<Integer> sigA = Signal.of(1);
        Signal<Integer> sigB = Signal.of(100);

        List<Integer> bObserved = new ArrayList<>();

        Effect effectA = Effect.of(() -> {
            int a = sigA.get();
            sigB.set(a * 10);
        });

        Effect effectB = Effect.of(() -> {
            bObserved.add(sigB.get());
        });

        assertEquals(Arrays.asList(10), bObserved);

        sigA.set(2);
        TestScheduler.flush();

        assertEquals(Arrays.asList(10, 20), bObserved);

        effectA.dispose();
        effectB.dispose();
    }

    @Test
    void computedLazyMemoizationAndInvalidation() {
        Signal<Integer> count = Signal.of(5);
        AtomicInteger calculations = new AtomicInteger(0);

        Computed<Integer> doubled = Signal.computed(() -> {
            calculations.incrementAndGet();
            return count.get() * 2;
        });

        assertEquals(0, calculations.get());

        assertEquals(10, (int) doubled.get());
        assertEquals(1, calculations.get());

        assertEquals(10, (int) doubled.get());
        assertEquals(10, (int) doubled.get());
        assertEquals(1, calculations.get());

        count.set(6);
        assertEquals(1, calculations.get());

        assertEquals(12, (int) doubled.get());
        assertEquals(2, calculations.get());

        doubled.dispose();
    }

    @Test
    void computedDynamicDependencyBranchSwitching() {
        Signal<Boolean> usePrimary = Signal.of(true);
        Signal<String> primary = Signal.of("primary-val");
        Signal<String> fallback = Signal.of("fallback-val");

        AtomicInteger evaluations = new AtomicInteger(0);
        Computed<String> choice = Signal.computed(() -> {
            evaluations.incrementAndGet();
            return usePrimary.get() ? primary.get() : fallback.get();
        });

        assertEquals("primary-val", choice.get());
        assertEquals(1, evaluations.get());

        fallback.set("new-fallback");
        assertEquals("primary-val", choice.get());
        assertEquals(1, evaluations.get(), "Mutating inactive branch must not re-evaluate");

        usePrimary.set(false);
        assertEquals("new-fallback", choice.get());
        assertEquals(2, evaluations.get());

        primary.set("ignored-primary");
        assertEquals("new-fallback", choice.get());
        assertEquals(2, evaluations.get(), "Former branch must be pruned from dependencies");

        choice.dispose();
    }

    @Test
    void computedCycleDetectionDirectAndTransitive() {

        AtomicReference<Computed<Integer>> self = new AtomicReference<>();
        self.set(Signal.computed(() -> self.get().get() + 1));

        assertThrows(IllegalStateException.class, () -> self.get().get());

        AtomicReference<Computed<Integer>> a = new AtomicReference<>();
        AtomicReference<Computed<Integer>> b = new AtomicReference<>();
        a.set(Signal.computed(() -> b.get() != null ? b.get().get() + 1 : 0));
        b.set(Signal.computed(() -> a.get().get() + 1));

        assertThrows(IllegalStateException.class, () -> a.get().get());
    }

    @Test
    void computedExceptionRestoresReactiveContext() {
        Signal<Integer> sig = Signal.of(42);
        Computed<Integer> faulty = Signal.computed(() -> {
            sig.get();
            throw new RuntimeException("Simulated computed failure");
        });

        RuntimeException thrown = assertThrows(RuntimeException.class, faulty::get);
        assertEquals("Simulated computed failure", thrown.getMessage());

        assertEquals(0, ReactiveContext.size(), "ReactiveContext stack must be clean after exception");
        assertNull(ReactiveContext.current());

        faulty.dispose();
    }

    @Test
    void effectImmediateExecutionAndDeduplication() {
        Signal<Integer> sig = Signal.of(1);
        AtomicInteger runs = new AtomicInteger(0);

        Effect effect = Effect.of(() -> {
            runs.incrementAndGet();
            sig.get();
        });

        assertEquals(1, runs.get(), "Effect must execute immediately on construction");

        sig.set(2);
        sig.set(3);
        sig.set(4);
        assertEquals(1, TestScheduler.pendingCount(), "Multiple mutations must deduplicate to single queue entry");

        TestScheduler.flush();
        assertEquals(2, runs.get(), "Effect must execute exactly once per flush pass");

        effect.dispose();
    }

    @Test
    void effectDisposedWhileQueuedIsSkipped() {
        Signal<Integer> sig = Signal.of(10);
        AtomicInteger runs = new AtomicInteger(0);

        Effect effect = Effect.of(() -> {
            runs.incrementAndGet();
            sig.get();
        });

        assertEquals(1, runs.get());

        sig.set(20);
        assertEquals(1, TestScheduler.pendingCount());

        effect.dispose();
        assertTrue(effect.isDisposed());

        TestScheduler.flush();
        assertEquals(1, runs.get(), "Disposed effect queued in dispatcher must be skipped during flush");
    }

    @Test
    void effectMultipleCleanupsRunBeforeRerunAndOnDisposal() {
        Signal<Integer> sig = Signal.of(1);
        List<String> logs = new ArrayList<>();

        Effect effect = Effect.of(cleanup -> {
            int val = sig.get();
            logs.add("run:" + val);
            cleanup.add(() -> logs.add("cleanA:" + val));
            cleanup.add(() -> logs.add("cleanB:" + val));
        });

        assertEquals(Arrays.asList("run:1"), logs);

        sig.set(2);
        TestScheduler.flush();

        assertEquals(
                Arrays.asList("run:1", "cleanA:1", "cleanB:1", "run:2"),
                logs,
                "Cleanups must run before subsequent execution");

        effect.dispose();

        assertEquals(
                Arrays.asList("run:1", "cleanA:1", "cleanB:1", "run:2", "cleanA:2", "cleanB:2"),
                logs,
                "Cleanups must run upon effect disposal");
    }
}

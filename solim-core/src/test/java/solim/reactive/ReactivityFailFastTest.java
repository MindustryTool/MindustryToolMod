package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import arc.Application;
import arc.Core;
import arc.mock.MockApplication;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class ReactivityFailFastTest extends SolimEnv {

    @Test
    void computedThrowsWhenSupplierFails() {
        Signal<Integer> count = Signal.of(1);
        Computed<String> computed = Signal.computed(() -> {
            if (count.get() < 0) {
                throw new IllegalArgumentException("Negative count not allowed: " + count.get());
            }
            return "Count: " + count.get();
        });

        assertEquals("Count: 1", computed.get());

        count.set(-5);
        IllegalArgumentException thrown = assertThrows(IllegalArgumentException.class, computed::get);
        assertEquals("Negative count not allowed: -5", thrown.getMessage());
    }

    @Test
    void signalPropagatesObserverException() {
        Signal<String> signal = Signal.of("initial");
        signal.addObserver(new ReactiveObserver() {
            @Override
            public void addDependency(solim.core.ReactiveSource observable) {
            }

            @Override
            public void invalidate() {
                if ("bad".equals(signal.peek())) {
                    throw new IllegalStateException("Bad value: bad");
                }
            }
        });

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () -> signal.set("bad"));
        assertEquals("Bad value: bad", thrown.getMessage());
    }

    @Test
    void mapSignalPropagatesObserverException() {
        MapSignal<String, Integer> mapSignal = new MapSignal<>();
        mapSignal.put("k1", 10);
        Readable<Integer> item = mapSignal.readable("k1");

        Computed<String> derived = Signal.computed(() -> {
            Integer v = item.get();
            if (v != null && v > 50) {
                throw new RuntimeException("Overflow value: " + v);
            }
            return "val:" + v;
        });

        assertEquals("val:10", derived.get());
        mapSignal.put("k1", 100);

        RuntimeException thrown = assertThrows(RuntimeException.class, derived::get);
        assertEquals("Overflow value: 100", thrown.getMessage());
    }

    @Test
    void mutationPropagatesOnSuccessException() {
        Application originalApp = Core.app;
        AtomicReference<Throwable> postedError = new AtomicReference<>();
        Core.app = new MockApplication() {
            @Override
            public void post(Runnable r) {
                try {
                    r.run();
                } catch (Throwable t) {
                    postedError.set(t);
                    throw t;
                }
            }
        };

        try {
            CompletableFuture<String> future = new CompletableFuture<>();
            Mutation<String, String> mutation = Mutation.<String, String>of(input -> future)
                    .onSuccess((res, ctx) -> {
                        throw new IllegalStateException("Success callback failed for " + res);
                    });

            mutation.mutate("test");
            future.complete("test_result");

            assertEquals(IllegalStateException.class, postedError.get().getClass());
            assertEquals("Success callback failed for test_result", postedError.get().getMessage());
        } finally {
            Core.app = originalApp;
        }
    }

    @Test
    void mutationPropagatesOnErrorException() {
        Application originalApp = Core.app;
        AtomicReference<Throwable> postedError = new AtomicReference<>();
        Core.app = new MockApplication() {
            @Override
            public void post(Runnable r) {
                try {
                    r.run();
                } catch (Throwable t) {
                    postedError.set(t);
                    throw t;
                }
            }
        };

        try {
            CompletableFuture<String> future = new CompletableFuture<>();
            Mutation<String, String> mutation = Mutation.<String, String>of(input -> future)
                    .onError((err, ctx) -> {
                        throw new IllegalStateException("Error callback failed");
                    });

            mutation.mutate("test");
            future.completeExceptionally(new RuntimeException("Root error"));

            assertEquals(IllegalStateException.class, postedError.get().getClass());
            assertEquals("Error callback failed", postedError.get().getMessage());
        } finally {
            Core.app = originalApp;
        }
    }

    @Test
    void effectBodyErrorPropagatesOutOfTheSynchronousInitialRun() {
        assertThrows(IllegalStateException.class, () -> Effect.of(() -> {
            throw new IllegalStateException("effect init failed");
        }), "Effect.of must propagate a body exception from its initial synchronous run");
    }

    @Test
    void effectBodyErrorPropagatesOutOfRunPending() {
        Signal<Integer> trigger = Signal.of(0);
        Effect.of(() -> {
            int v = trigger.get();
            if (v > 0) {
                throw new IllegalStateException("effect run failed: " + v);
            }
        });

        trigger.set(1);
        IllegalStateException thrown = assertThrows(IllegalStateException.class, SignalDispatcher::flush);
        assertEquals("effect run failed: 1", thrown.getMessage());
    }

    @Test
    void effectOwningAComponentBodyPropagatesComponentFailure() {
        Signal<Integer> trigger = Signal.of(0);
        Effect.of(() -> {
            if (trigger.get() > 0) {

                throw new IllegalStateException("component build failed");
            }
        });

        trigger.set(1);
        assertThrows(IllegalStateException.class, SignalDispatcher::flush);
    }

    @Test
    void unprocessedEffectsSurviveAnAbortedFlush() {
        Signal<Integer> trigger = Signal.of(0);
        AtomicInteger firstRuns = new AtomicInteger();
        AtomicInteger secondRuns = new AtomicInteger();
        AtomicInteger thirdRuns = new AtomicInteger();

        Effect first = Effect.of(() -> {
            int v = trigger.get();
            firstRuns.incrementAndGet();
            if (v == 1) {
                throw new IllegalStateException("first effect fails");
            }
        });
        Effect second = Effect.of(() -> {
            trigger.get();
            secondRuns.incrementAndGet();
        });
        Effect third = Effect.of(() -> {
            trigger.get();
            thirdRuns.incrementAndGet();
        });

        firstRuns.set(0);
        secondRuns.set(0);
        thirdRuns.set(0);

        trigger.set(1);
        assertThrows(IllegalStateException.class, SignalDispatcher::flush);

        assertFalse(SignalDispatcher.isFlushing(), "Dispatcher must be reusable after an aborted flush");
        assertEquals(1, firstRuns.get(), "The throwing effect did run");
        assertEquals(0, secondRuns.get(), "Effects after the throwing one must not have run");
        assertEquals(0, thirdRuns.get());
        assertEquals(2, SignalDispatcher.size(), "Unprocessed effects must stay queued with pending set");

        SignalDispatcher.flush();
        assertEquals(1, secondRuns.get(), "Surviving effects must execute on the next flush");
        assertEquals(1, thirdRuns.get());
        assertEquals(0, SignalDispatcher.size());

        first.dispose();
        second.dispose();
        third.dispose();
    }

    @Test
    void throwingEffectRetainsItsPreviousDependencySet() {
        Signal<Integer> kept = Signal.of(0);
        AtomicInteger runs = new AtomicInteger();

        Effect effect = Effect.of(() -> {
            runs.incrementAndGet();
            int v = kept.get();
            if (v > 0) {
                throw new IllegalStateException("boom " + v);
            }
        });
        runs.set(0);

        kept.set(1);
        assertThrows(IllegalStateException.class, SignalDispatcher::flush);
        assertEquals(1, kept.observerCount(),
                "A throwing run must not unsubscribe: the effect stays live and fails again");

        kept.set(2);
        assertThrows(IllegalStateException.class, SignalDispatcher::flush);
        assertEquals(2, runs.get(), "Effect must still be subscribed after a failed run");

        effect.dispose();
        assertEquals(0, kept.observerCount());
    }
}

package solim.reactive;

import solim.runtime.SignalDispatcher;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import java.util.ArrayList;
import java.util.List;
import solim.test.SolimEnv;

class SignalTest extends SolimEnv {

    @Test
    void equalityGuardedNotification() {
        Signal<Integer> c = Signal.of(0);
        AtomicInteger calls = new AtomicInteger(0);
        Effect e = Effect.of(c, v -> calls.incrementAndGet());
        assertEquals(1, calls.get());
        c.set(0);
        SignalDispatcher.flush();
        assertEquals(1, calls.get(), "Equal value should not notify");

        Signal.computed(() -> "val:" + c.get());
        AtomicInteger compRuns = new AtomicInteger(0);
        Computed<String> comp2 = Signal.computed(() -> {
            compRuns.incrementAndGet();
            return "val:" + c.get();
        });
        comp2.get();
        assertEquals(1, compRuns.get());
        c.set(0);
        comp2.get();
        assertEquals(1, compRuns.get(), "Equal set should not dirty computed");
        e.dispose();
    }

    @Test
    void effectBindingDisposeStops() {
        Signal<Integer> count = Signal.of(1);
        AtomicInteger seen = new AtomicInteger(-1);
        Effect s = Effect.of(count, v -> seen.set(v));
        assertEquals(1, seen.get());
        count.set(2);
        SignalDispatcher.flush();
        assertEquals(2, seen.get());
        s.dispose();
        count.set(3);
        SignalDispatcher.flush();
        assertEquals(2, seen.get(), "Disposed effect should not receive");

        assertDoesNotThrow(s::dispose);
        assertTrue(s.isDisposed());
    }

    @Test
    void mapCreatesComputed() {
        Signal<Boolean> enabled = Signal.of(false);
        Computed<String> t = enabled.map(v -> v ? "Enabled" : "Disabled");
        assertEquals("Disabled", t.get());
        enabled.set(true);
        assertEquals("Enabled", t.get());

        t.dispose();
        assertTrue(t.isDisposed());
        assertEquals(0, enabled.observerCount());
        assertNull(t.get());
    }

    @Test
    void getTracksInComputedAndEffect() {
        Signal<Integer> count = Signal.of(1);
        Computed<String> c = Signal.computed(() -> count.get() + "");
        assertEquals("1", c.get());
        count.set(2);
        assertEquals("2", c.get(), "Computed should auto-track Signal");

        Signal<String> s = Signal.of("a");
        AtomicReference<String> effectVal = new AtomicReference<>("");
        Effect e = Effect.of(() -> effectVal.set(s.get()));
        assertEquals("a", effectVal.get());
        s.set("b");
        SignalDispatcher.flush();
        assertEquals("b", effectVal.get(), "Effect should auto-track Signal");
        e.dispose();
        s.set("c");
        SignalDispatcher.flush();
        assertEquals("b", effectVal.get(), "Disposed effect should not re-run");
    }

    @Test
    void effectDisposeIdempotentAndNoLeak() {
        Signal<Integer> s = Signal.of(0);
        Effect sub = Effect.of(s, v -> {
        });
        assertEquals(1, s.observerCount());
        sub.dispose();
        sub.dispose();
        assertTrue(sub.isDisposed());
        assertEquals(0, s.observerCount());
    }

    @Test
    void fromCallbackRecalculatesOnTrigger() {
        int[] val = new int[] { 100 };
        List<Runnable> callbacks = new ArrayList<>();

        Signal<Integer> s = Signal.fromCallback(callbacks::add, () -> val[0]);
        assertEquals(100, s.get());

        val[0] = 200;
        for (Runnable r : callbacks)
            r.run();
        assertEquals(200, s.get());
    }
}

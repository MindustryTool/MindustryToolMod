package solim.reactive;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class ComputedTest extends SolimEnv {

    @Test
    void lazyRecomputationOnlyOnGet() {
        Signal<Integer> count = Signal.of(0);
        AtomicInteger runs = new AtomicInteger(0);
        Computed<String> title = Signal.computed(() -> {
            runs.incrementAndGet();
            return "Count: " + count.get();
        });
        assertEquals(0, runs.get(), "Should not compute until first get");
        assertEquals("Count: 0", title.get());
        assertEquals(1, runs.get());
        title.get();
        assertEquals(1, runs.get());
        count.set(10);

        assertEquals(1, runs.get(), "Should not recompute until get");
        assertEquals("Count: 10", title.get());
        assertEquals(2, runs.get());
    }

    @Test
    void dynamicDependenciesCleanup() {
        Signal<Boolean> darkMode = Signal.of(true);
        Signal<String> username = Signal.of("Alice");
        Signal<String> email = Signal.of("alice@example.com");
        Computed<String> v = Signal.computed(() -> darkMode.get() ? username.get() : email.get());
        assertEquals("Alice", v.get());
        assertEquals(2, v.dependencyCount());
        darkMode.set(false);
        assertEquals("alice@example.com", v.get());
        assertEquals(2, v.dependencyCount());
        username.set("Bob");

        assertFalse(v.isDirty(), "Should not be dirty after old dep change");
        assertEquals("alice@example.com", v.get());

        email.set("new@example.com");
        assertTrue(v.isDirty());
        assertEquals("new@example.com", v.get());
    }

    @Test
    void cycleDetection() {
        AtomicReference<Computed<Integer>> a = new AtomicReference<>();
        AtomicReference<Computed<Integer>> b = new AtomicReference<>();
        a.set(Signal.computed(() -> b.get() != null ? b.get().get() + 1 : 1));
        b.set(Signal.computed(() -> a.get().get() + 1));
        assertThrows(IllegalStateException.class, () -> a.get().get());
    }

    @Test
    void invalidationPropagatesToDependentComputeds() {
        Signal<Integer> a = Signal.of(1);
        Computed<Integer> b = a.map(v -> v + 1);
        Computed<Integer> c = Signal.computed(() -> b.get() + 1);
        assertEquals(2, b.get());
        assertEquals(3, c.get());
        a.set(5);

        assertTrue(b.isDirty());
        assertTrue(c.isDirty());
        assertEquals(6, b.get());
        assertEquals(7, c.get());
    }

    @Test
    void effectObservationAndDispose() {
        Signal<Integer> s = Signal.of(1);
        Computed<Integer> comp = Signal.computed(() -> s.get() * 2);
        comp.get();
        AtomicInteger seen = new AtomicInteger(-1);
        Effect sub = Effect.of(comp, v -> seen.set(v));

        assertEquals(2, seen.get());
        s.set(2);
        SignalDispatcher.flush();

        assertEquals(4, seen.get());
        comp.dispose();
        assertTrue(comp.isDisposed());
        assertEquals(0, comp.dependencyCount());
        s.set(3);
        SignalDispatcher.flush();

        assertEquals(4, seen.get());

        assertDoesNotThrow(comp::dispose);
        sub.dispose();
    }

    @Test
    void mapReactiveUpdates() {
        Signal<Integer> count = Signal.of(1);
        Computed<String> mapped = count.map(v -> "Count:" + v);
        assertEquals("Count:1", mapped.get());
        count.set(2);
        assertEquals("Count:2", mapped.get());
    }

    @Test
    void computedMapChain() {
        Signal<Integer> a = Signal.of(1);
        Computed<Integer> b = a.map(v -> v + 1);
        Computed<Integer> c = b.map(v -> v * 2);
        assertEquals(4, c.get());
        a.set(2);
        assertEquals(6, c.get());
    }

    @Test
    void noLeakAfterDispose() {
        Signal<Integer> s = Signal.of(1);
        Computed<Integer> c = Signal.computed(() -> s.get() + 1);
        c.get();
        assertEquals(1, s.observerCount());
        assertEquals(1, c.dependencyCount());
        c.dispose();
        assertEquals(0, s.observerCount());
        assertEquals(0, c.dependencyCount());
    }
}

package solim.signal;

import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;

class ComputedTest {

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
        title.get(); // second get without change should not rerun
        assertEquals(1, runs.get());
        count.set(10);
        // after set, should NOT have run yet if lazy
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
        // v should still be email value, not dirty
        assertFalse(v.isDirty(), "Should not be dirty after old dep change");
        assertEquals("alice@example.com", v.get());
        // email change should dirty
        email.set("new@example.com");
        assertTrue(v.isDirty());
        assertEquals("new@example.com", v.get());
    }

    @Test
    @SuppressWarnings("unchecked")
    void cycleDetection() {
        Computed<Integer>[] a = new Computed[1];
        Computed<Integer>[] b = new Computed[1];
        a[0] = Signal.computed(() -> b[0] != null ? b[0].get() + 1 : 1);
        b[0] = Signal.computed(() -> a[0].get() + 1);
        assertThrows(IllegalStateException.class, () -> a[0].get());
    }

    @Test
    void invalidationPropagatesToDependentComputeds() {
        Signal<Integer> a = Signal.of(1);
        Computed<Integer> b = a.map(v -> v + 1);
        Computed<Integer> c = Signal.computed(() -> b.get() + 1);
        assertEquals(2, b.get());
        assertEquals(3, c.get());
        a.set(5);
        // both should be dirty before get
        assertTrue(b.isDirty());
        assertTrue(c.isDirty());
        assertEquals(6, b.get());
        assertEquals(7, c.get());
    }

    @Test
    void subscriptionAndDispose() {
        Signal<Integer> s = Signal.of(1);
        Computed<Integer> comp = Signal.computed(() -> s.get() * 2);
        comp.get(); // prime dependencies
        AtomicInteger seen = new AtomicInteger(-1);
        Subscription sub = comp.subscribe(v -> seen.set(v));
        // trigger change
        s.set(2);
        // subscriber should be notified (eager recompute)
        assertEquals(4, seen.get());
        comp.dispose();
        assertTrue(comp.isDisposed());
        assertEquals(0, comp.dependencyCount());
        assertEquals(0, comp.listenerCount());
        s.set(3);
        // after dispose, no notification
        assertEquals(4, seen.get());
        // dispose idempotent
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
        assertEquals(4, c.get()); // (1+1)*2
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

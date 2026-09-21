package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import solim.runtime.ComponentContext;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

class MapSignalTest extends SolimEnv {

    @Test
    void wholeMapReactivity() {
        MapSignal<String, Integer> map = MapSignal.of();
        AtomicInteger runs = new AtomicInteger(0);
        AtomicReference<Map<String, Integer>> seen = new AtomicReference<>();
        Effect effect = Effect.of(() -> {
            runs.incrementAndGet();
            seen.set(map.get());
        });
        assertEquals(1, runs.get());
        assertTrue(seen.get().isEmpty());

        map.put("a", 1);
        SignalDispatcher.flush();
        assertEquals(2, runs.get());
        assertEquals(1, (int) seen.get().get("a"));

        map.put("b", 2);
        SignalDispatcher.flush();
        assertEquals(3, runs.get());
        assertEquals(2, seen.get().size());

        effect.dispose();
    }

    @Test
    void peekDoesNotTrack() {
        MapSignal<String, Integer> map = MapSignal.of();
        map.put("a", 1);
        AtomicInteger runs = new AtomicInteger(0);
        Effect effect = Effect.of(() -> {
            runs.incrementAndGet();
            map.peek();
        });
        assertEquals(1, runs.get());
        map.put("b", 2);
        SignalDispatcher.flush();
        assertEquals(1, runs.get(), "peek() must not subscribe");
        effect.dispose();
    }

    @Test
    void keyLevelIsolation() {
        MapSignal<String, Integer> map = MapSignal.of();
        map.put("a", 1);
        map.put("b", 10);

        AtomicInteger runsA = new AtomicInteger(0);
        AtomicInteger runsB = new AtomicInteger(0);
        AtomicReference<Integer> seenA = new AtomicReference<>();
        AtomicReference<Integer> seenB = new AtomicReference<>();

        Effect effectA = Effect.of(() -> {
            runsA.incrementAndGet();
            seenA.set(map.readable("a").get());
        });
        Effect effectB = Effect.of(() -> {
            runsB.incrementAndGet();
            seenB.set(map.readable("b").get());
        });
        assertEquals(1, runsA.get());
        assertEquals(1, runsB.get());

        map.put("a", 2);
        SignalDispatcher.flush();
        assertEquals(2, runsA.get());
        assertEquals(2, (int) seenA.get());
        assertEquals(1, runsB.get(), "Observers of other keys must not be notified");
        assertEquals(10, (int) seenB.get());

        effectA.dispose();
        effectB.dispose();
    }

    @Test
    void missingKeyBecomesReactiveOnInsert() {
        MapSignal<String, String> map = MapSignal.of();
        AtomicInteger runs = new AtomicInteger(0);
        AtomicReference<String> seen = new AtomicReference<>("sentinel");
        Effect effect = Effect.of(() -> {
            runs.incrementAndGet();
            seen.set(map.readable("missing").get());
        });
        assertEquals(1, runs.get());
        assertNull(seen.get());

        map.put("missing", "now-here");
        SignalDispatcher.flush();
        assertEquals(2, runs.get());
        assertEquals("now-here", seen.get());

        effect.dispose();
    }

    @Test
    void equalitySuppressesNotifications() {
        MapSignal<String, String> map = MapSignal.of();
        map.put("k", "v");

        AtomicInteger mapRuns = new AtomicInteger(0);
        AtomicInteger keyRuns = new AtomicInteger(0);
        Effect mapEffect = Effect.of(() -> {
            mapRuns.incrementAndGet();
            map.get();
        });
        Effect keyEffect = Effect.of(() -> {
            keyRuns.incrementAndGet();
            map.readable("k").get();
        });
        assertEquals(1, mapRuns.get());
        assertEquals(1, keyRuns.get());

        map.put("k", "v");
        SignalDispatcher.flush();
        assertEquals(1, mapRuns.get(), "Equal put must not notify map observers");
        assertEquals(1, keyRuns.get(), "Equal put must not notify key observers");

        map.put("k", new String("v"));
        SignalDispatcher.flush();
        assertEquals(1, mapRuns.get());
        assertEquals(1, keyRuns.get());

        mapEffect.dispose();
        keyEffect.dispose();
    }

    @Test
    void batchPutAllNotifiesOnlyChangedKeysOnce() {
        MapSignal<String, Integer> map = MapSignal.of();
        map.put("a", 1);
        map.put("b", 2);

        AtomicInteger runsA = new AtomicInteger(0);
        AtomicInteger runsB = new AtomicInteger(0);
        AtomicInteger runsC = new AtomicInteger(0);
        AtomicInteger mapRuns = new AtomicInteger(0);
        Effect effectA = Effect.of(() -> {
            runsA.incrementAndGet();
            map.readable("a").get();
        });
        Effect effectB = Effect.of(() -> {
            runsB.incrementAndGet();
            map.readable("b").get();
        });
        Effect effectC = Effect.of(() -> {
            runsC.incrementAndGet();
            map.readable("c").get();
        });
        Effect mapEffect = Effect.of(() -> {
            mapRuns.incrementAndGet();
            map.get();
        });

        Map<String, Integer> batch = new HashMap<>();
        batch.put("a", 1);
        batch.put("b", 20);
        batch.put("c", 30);
        map.putAll(batch);
        SignalDispatcher.flush();

        assertEquals(1, runsA.get(), "Unchanged key must not be notified");
        assertEquals(2, runsB.get());
        assertEquals(2, runsC.get());
        assertEquals(2, mapRuns.get(), "Map observers notified once per batch");

        Map<String, Integer> noop = new HashMap<>();
        noop.put("a", 1);
        noop.put("b", 20);
        map.putAll(noop);
        SignalDispatcher.flush();
        assertEquals(1, runsA.get());
        assertEquals(2, runsB.get());
        assertEquals(2, mapRuns.get(), "No-op batch must not notify");

        effectA.dispose();
        effectB.dispose();
        effectC.dispose();
        mapEffect.dispose();
    }

    @Test
    void removeAndClear() {
        MapSignal<String, Integer> map = MapSignal.of();
        map.put("a", 1);
        map.put("b", 2);

        AtomicInteger runsA = new AtomicInteger(0);
        AtomicReference<Integer> seenA = new AtomicReference<>(-1);
        AtomicInteger mapRuns = new AtomicInteger(0);
        Effect effectA = Effect.of(() -> {
            runsA.incrementAndGet();
            seenA.set(map.readable("a").get());
        });
        Effect mapEffect = Effect.of(() -> {
            mapRuns.incrementAndGet();
            map.get();
        });

        assertNull(map.remove("missing"));
        SignalDispatcher.flush();
        assertEquals(1, runsA.get());
        assertEquals(1, mapRuns.get(), "Removing missing key must not notify");

        assertEquals(1, (int) map.remove("a"));
        SignalDispatcher.flush();
        assertEquals(2, runsA.get());
        assertNull(seenA.get(), "Removed key reads as null");
        assertEquals(2, mapRuns.get());

        map.clear();
        SignalDispatcher.flush();
        assertEquals(3, mapRuns.get());

        int before = mapRuns.get();
        map.clear();
        SignalDispatcher.flush();
        assertEquals(before, mapRuns.get(), "Clearing empty map must not notify");

        effectA.dispose();
        mapEffect.dispose();
    }

    @Test
    void computedTracksKeyAndMap() {
        MapSignal<String, Integer> map = MapSignal.of();
        map.put("a", 1);
        map.put("b", 2);

        Computed<Integer> keyComputed = Signal.computed(() -> {
            Integer value = map.readable("a").get();
            return value == null ? -1 : value * 2;
        });
        assertEquals(2, (int) keyComputed.get());
        map.put("b", 99);
        assertEquals(2, (int) keyComputed.get(), "Other key change must not dirty key computed");
        assertFalse(keyComputed.isDirty());
        map.put("a", 5);
        assertTrue(keyComputed.isDirty());
        assertEquals(10, (int) keyComputed.get());

        Computed<Integer> sizeComputed = Signal.computed(() -> map.get().size());
        assertEquals(2, (int) sizeComputed.get());
        map.put("c", 3);
        assertTrue(sizeComputed.isDirty());
        assertEquals(3, (int) sizeComputed.get());

        keyComputed.dispose();
        sizeComputed.dispose();
    }

    @Test
    void doesNotRegisterWithComponentContext() {
        List<Object> registered = new ArrayList<>();
        ComponentContext.push(registered::add);
        try {
            int before = ComponentContext.size();
            MapSignal<String, Integer> map = MapSignal.of();
            map.readable("k");
            map.readable("k").peek();
            assertTrue(registered.isEmpty(), "MapSignal must not auto-register with ComponentContext");
            assertEquals(before, ComponentContext.size());
        } finally {
            ComponentContext.pop();
        }
        assertEquals(0, ComponentContext.size());
    }

    @Test
    void survivesComponentUnmounts() {
        MapSignal<String, String> map = MapSignal.of();
        map.put("user1", "Alice");

        List<Object> registered = new ArrayList<>();
        ComponentContext.push(registered::add);
        Effect transientEffect;
        try {
            transientEffect = Effect.of(() -> map.readable("user1").get());
        } finally {
            ComponentContext.pop();
        }
        transientEffect.dispose();
        assertEquals(0, map.keyObserverCount("user1"), "Disposed transient effect must detach");

        AtomicInteger runs = new AtomicInteger(0);
        AtomicReference<String> seen = new AtomicReference<>();
        Effect survivor = Effect.of(() -> {
            runs.incrementAndGet();
            seen.set(map.readable("user1").get());
        });
        assertEquals("Alice", seen.get());

        map.put("user1", "Bob");
        SignalDispatcher.flush();
        assertEquals(2, runs.get());
        assertEquals("Bob", seen.get(), "MapSignal must stay live after transient unmount");

        survivor.dispose();
        assertEquals(0, map.keyObserverCount("user1"));
        map.put("user1", "Carol");
        assertEquals("Carol", map.readable("user1").peek());
    }

    @Test
    void nullKeyReturnsStaticReadable() {
        MapSignal<String, String> map = MapSignal.of();
        assertNull(map.readable(null).get());
        assertNull(map.readable(null).peek());
        map.put(null, "nil");
        assertNull(map.readable(null).get(), "Null key observation stays non-reactive");
        assertEquals("nil", map.peek().get(null));
    }

    @Test
    void sizeAndEmptyTrackMapObservers() {
        MapSignal<String, Integer> map = MapSignal.of();
        AtomicInteger runs = new AtomicInteger(0);
        Effect effect = Effect.of(() -> {
            runs.incrementAndGet();
            map.size();
            map.isEmpty();
        });
        assertEquals(1, runs.get());
        map.put("a", 1);
        SignalDispatcher.flush();
        assertEquals(2, runs.get());
        effect.dispose();
    }

    @Test
    void unmodifiableSnapshotAndInitialMap() {
        Map<String, Integer> initial = new HashMap<>();
        initial.put("a", 1);
        MapSignal<String, Integer> map = MapSignal.of(initial);
        assertEquals(1, (int) map.peek().get("a"));

        Map<String, Integer> snapshot = map.get();
        assertEquals(1, snapshot.size());
        boolean threw = false;
        try {
            snapshot.put("b", 2);
        } catch (UnsupportedOperationException expected) {
            threw = true;
        }
        assertTrue(threw, "get() must return unmodifiable view");
        assertFalse(map.peek().containsKey("b"));

        MapSignal<String, Integer> empty = MapSignal.of(null);
        assertTrue(empty.peek().isEmpty());
        MapSignal<String, Integer> fromEmpty = MapSignal.of(Collections.emptyMap());
        assertTrue(fromEmpty.peek().isEmpty());
    }

    @Test
    void reactiveContextUntracked() {
        MapSignal<String, Integer> map = MapSignal.of();
        map.put("a", 1);
        AtomicInteger runs = new AtomicInteger(0);
        Effect effect = Effect.of(() -> {
            runs.incrementAndGet();
            ReactiveContext.untracked(() -> map.get());
            ReactiveContext.untracked(() -> map.readable("a").get());
        });
        assertEquals(1, runs.get());
        map.put("a", 2);
        SignalDispatcher.flush();
        assertEquals(1, runs.get(), "Untracked reads must not subscribe");
        effect.dispose();
    }

    @Test
    void mixedSourceEffectAndDisposalParity() {
        Signal<Integer> signal = Signal.of(1);
        Computed<Integer> computed = signal.map(v -> v * 2);
        MapSignal<String, Integer> map = MapSignal.of();
        map.put("k", 10);

        AtomicInteger runs = new AtomicInteger(0);
        Effect effect = Effect.of(() -> {
            runs.incrementAndGet();
            signal.get();
            computed.get();
            map.readable("k").get();
        });
        assertEquals(1, runs.get());
        assertEquals(3, effect.dependencyCount());

        signal.set(2);
        SignalDispatcher.flush();
        assertEquals(2, runs.get());

        map.put("k", 11);
        SignalDispatcher.flush();
        assertEquals(3, runs.get());

        map.put("other", 99);
        SignalDispatcher.flush();
        assertEquals(3, runs.get(), "Other key must not trigger mixed effect");

        effect.dispose();
        assertEquals(1, signal.observerCount(), "Computed remains attached to signal");
        assertEquals(0, computed.observerCount());
        assertEquals(0, map.keyObserverCount("k"));
        assertEquals(0, effect.dependencyCount());

        signal.set(3);
        map.put("k", 12);
        SignalDispatcher.flush();
        assertEquals(3, runs.get(), "Disposed effect must stay silent");
        computed.dispose();
        assertEquals(0, signal.observerCount());
    }
}

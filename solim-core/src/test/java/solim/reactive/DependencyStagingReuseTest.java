package solim.reactive;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.runtime.ReactiveContext;
import solim.runtime.SignalDispatcher;
import solim.test.SolimEnv;

/**
 * Verifies that a stable dependency set produces no observer churn. Both
 * primitives reuse one
 * staging buffer and diff it in place, so an unchanged set must reach neither
 * {@code addObserver} nor {@code removeObserver}.
 */
class DependencyStagingReuseTest extends SolimEnv {

    static final class CountingSource implements ReactiveSource {
        final List<ReactiveObserver> added = new ArrayList<>();
        final List<ReactiveObserver> removed = new ArrayList<>();

        @Override
        public void addObserver(ReactiveObserver observer) {
            added.add(observer);
        }

        @Override
        public void removeObserver(ReactiveObserver observer) {
            removed.add(observer);
        }
    }

    @Test
    void effectWithUnchangedDependenciesIssuesNoObserverChurn() {
        CountingSource a = new CountingSource();
        CountingSource b = new CountingSource();
        Signal<Integer> tick = Signal.of(0);
        int[] runs = new int[] { 0 };

        Effect effect = Effect.of(() -> {
            tick.get();
            ReactiveContext.track(a);
            ReactiveContext.track(b);
            runs[0]++;
        });

        assertEquals(1, a.added.size());
        assertEquals(1, b.added.size());

        tick.set(1);
        SignalDispatcher.flush();
        tick.set(2);
        SignalDispatcher.flush();
        tick.set(3);
        SignalDispatcher.flush();

        assertEquals(4, runs[0], "The effect must actually have re-run");
        assertEquals(1, a.added.size(), "An unchanged dependency set must not re-subscribe");
        assertEquals(1, b.added.size());
        assertEquals(0, a.removed.size(), "An unchanged dependency set must not unsubscribe");
        assertEquals(0, b.removed.size());

        effect.dispose();
        assertEquals(1, a.removed.size());
        assertEquals(1, b.removed.size());
    }

    @Test
    void computedWithUnchangedDependenciesIssuesNoObserverChurn() {
        CountingSource a = new CountingSource();
        CountingSource b = new CountingSource();
        Signal<Integer> tick = Signal.of(0);

        Computed<String> computed = Signal.computed(() -> {
            ReactiveContext.track(a);
            ReactiveContext.track(b);
            return "" + tick.get();
        });
        computed.get();
        tick.set(1);
        computed.get();
        tick.set(2);
        computed.get();
        tick.set(3);
        computed.get();

        assertEquals(1, a.added.size(), "Repeated evaluation of a stable set must not re-subscribe");
        assertEquals(1, b.added.size());
        assertEquals(0, a.removed.size());
        assertEquals(0, b.removed.size());

        computed.dispose();
        assertEquals(1, a.removed.size());
        assertEquals(1, b.removed.size());
    }

    @Test
    void changingDependencySetStillSwapsSubscriptions() {
        CountingSource a = new CountingSource();
        CountingSource b = new CountingSource();
        Signal<Boolean> useB = Signal.of(false);

        Effect effect = Effect.of(() -> {
            if (useB.get()) {
                ReactiveContext.track(b);
            } else {
                ReactiveContext.track(a);
            }
        });

        assertEquals(1, a.added.size());
        assertEquals(0, b.added.size());

        useB.set(true);
        SignalDispatcher.flush();

        assertEquals(1, a.removed.size(), "A dropped source must be unsubscribed");
        assertEquals(1, b.added.size(), "A new source must be subscribed");

        useB.set(false);
        SignalDispatcher.flush();

        assertEquals(2, a.added.size(), "Returning to the original source must subscribe again");
        assertEquals(1, b.removed.size());

        effect.dispose();
    }
}

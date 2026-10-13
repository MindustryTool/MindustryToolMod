package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.core.SolimAssert;
import solim.reactive.Signal;
import solim.test.SolimCoreEnv;
import solim.test.TestObserver;

/**
 * Verifies that Solim's main-thread guard is unconditional, and that dependency
 * tracking is only guarded on the branch where a dependency is actually
 * recorded.
 */
class ThreadEnforcementTest extends SolimCoreEnv {

    @AfterEach
    void releaseMainThread() {
        SolimAssert.setMainThread(null);
    }

    @Test
    void offThreadSignalSetThrows() {
        SolimAssert.setMainThread(Thread.currentThread());
        Signal<Integer> signal = Signal.of(0);

        Throwable thrown = runOffThread(() -> signal.set(1));

        assertInstanceOf(IllegalStateException.class, thrown);
        assertEquals(0, signal.get(), "A rejected off-thread write must not mutate the signal");
    }

    @Test
    void offThreadReadWithoutActiveObserverIsPermitted() {
        SolimAssert.setMainThread(Thread.currentThread());
        Signal<Integer> signal = Signal.of(7);

        Throwable thrown = runOffThread(() -> {
            assertEquals(7, signal.get());
            assertEquals(7, signal.peek());
        });

        assertNull(thrown, "An off-thread read that records no dependency must be allowed");
    }

    @Test
    void offThreadDependencyRegistrationThrows() {
        SolimAssert.setMainThread(Thread.currentThread());
        Signal<Integer> signal = Signal.of(1);
        TestObserver observer = new TestObserver("off-thread-observer");
        ReactiveContext.push(observer);

        try {
            Throwable thrown = runOffThread(() -> signal.get());
            assertInstanceOf(IllegalStateException.class, thrown);
            assertTrue(observer.getDependencies().isEmpty(),
                    "The dependency must not be recorded once the thread violation is detected");
        } finally {
            ReactiveContext.pop();
        }
    }

    @Test
    void untrackedRestoresDepthAfterBalancedBlock() {
        TestObserver outer = new TestObserver("outer");
        ReactiveContext.push(outer);
        int entryDepth = ReactiveContext.size();

        ReactiveContext.untracked(() -> {
            ReactiveContext.push(new TestObserver("inner"));
            assertEquals(1, ReactiveContext.size() - entryDepth);
            ReactiveContext.pop();
        });

        assertEquals(entryDepth, ReactiveContext.size(), "Balanced block must not change stack depth");
        assertSame(outer, ReactiveContext.current());
        ReactiveContext.pop();
    }

    @Test
    void untrackedRestoresDepthAfterUnbalancedBlock() {
        TestObserver outer = new TestObserver("outer");
        ReactiveContext.push(outer);
        int entryDepth = ReactiveContext.size();

        ReactiveContext.untracked(() -> ReactiveContext.push(new TestObserver("leaked")));

        assertEquals(entryDepth, ReactiveContext.size(),
                "A block that pushes without popping must be truncated back to the entry depth");
        assertSame(outer, ReactiveContext.current(), "The enclosing observer must survive an unbalanced block");
        ReactiveContext.pop();
    }

    @Test
    void untrackedRestoresDepthWhenBlockThrows() {
        ReactiveContext.push(new TestObserver("outer"));
        int entryDepth = ReactiveContext.size();

        try {
            ReactiveContext.untracked(() -> {
                ReactiveContext.push(new TestObserver("inner"));
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException ignored) {
            // expected
        }

        assertEquals(entryDepth, ReactiveContext.size());
        ReactiveContext.pop();
    }

    private static Throwable runOffThread(Runnable action) {
        AtomicReference<Throwable> thrown = new AtomicReference<>();
        Thread worker = new Thread(() -> {
            try {
                action.run();
            } catch (Throwable t) {
                thrown.set(t);
            }
        }, "solim-off-main");
        worker.start();
        try {
            worker.join();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        }
        return thrown.get();
    }
}

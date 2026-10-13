package solim.runtime;

import arc.Events;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.Queue;
import mindustry.game.EventType.Trigger;
import solim.core.SchedulableEffect;
import solim.core.SolimAssert;

/**
 * Internal single-threaded dispatcher that batches and deduplicates reactive
 * effect
 * invalidations to run at most once per Mindustry frame.
 *
 * <p>
 * All static entry points delegate to a singleton instance, so {@code Perf} can
 * install a
 * profiling-enabled subclass without the base class carrying flush
 * instrumentation.
 */
public class SignalDispatcher {

    private static volatile SignalDispatcher INSTANCE = new SignalDispatcher();

    protected static final int MAX_CASCADE_DEPTH = 100;

    protected static boolean registered = false;

    protected final Queue<SchedulableEffect> queue = new ArrayDeque<>();
    protected boolean flushing = false;

    protected SignalDispatcher() {
    }

    public static SignalDispatcher instance() {
        return INSTANCE;
    }

    public static void install(@Nullable SignalDispatcher dispatcher) {
        INSTANCE = dispatcher != null ? dispatcher : new SignalDispatcher();
    }

    /**
     * Registers the frame flush callback with Mindustry's {@link Trigger#update}
     * event.
     */
    public static void register() {
        if (registered)
            return;
        registered = true;
        if (SolimAssert.getMainThread() == null) {
            SolimAssert.setMainThread(Thread.currentThread());
        }
        // Resolves INSTANCE at call time rather than capturing it, so swapping the
        // singleton via
        // install(...) takes effect on the next frame without re-registering the event
        // listener.
        Events.run(Trigger.update, (Runnable) () -> flush());
    }

    /**
     * Ensures that the dispatcher is hooked into the frame lifecycle.
     */
    public static void ensureRegistered() {
        if (!registered) {
            register();
        }
    }

    /**
     * Enqueues an invalidated effect for batched execution on the next frame flush.
     */
    public static void enqueue(@Nullable SchedulableEffect effect) {
        if (effect == null || effect.isDisposed())
            return;
        ensureRegistered();
        INSTANCE.doEnqueue(effect);
    }

    /**
     * Flushes and executes all pending effects in FIFO order on the caller's
     * thread.
     */
    public static void flush() {
        INSTANCE.doFlush();
    }

    public static int size() {
        return INSTANCE.queue.size();
    }

    public static boolean isFlushing() {
        return INSTANCE.flushing;
    }

    public static boolean isRegistered() {
        return registered;
    }

    /**
     * Resets dispatcher state for isolated test execution.
     */
    public static void resetForTests() {
        INSTANCE.doResetForTests();
    }

    protected void doEnqueue(SchedulableEffect effect) {
        queue.add(effect);
    }

    protected void doFlush() {
        SolimAssert.checkMainThread();
        if (flushing)
            return;
        flushing = true;
        try {
            int pass = 0;
            while (!queue.isEmpty()) {
                if (++pass > MAX_CASCADE_DEPTH) {
                    Log.err("[Solim] Infinite reactive loop detected in SignalDispatcher (exceeded @ cascade iterations). Clearing @ pending effects.",
                            MAX_CASCADE_DEPTH, queue.size());
                    for (SchedulableEffect remaining : queue) {
                        remaining.clearPending();
                    }
                    queue.clear();
                    break;
                }

                int batchSize = queue.size();
                for (int i = 0; i < batchSize; i++) {
                    SchedulableEffect effect = queue.poll();
                    if (effect == null)
                        continue;

                    if (effect.isDisposed()) {
                        effect.clearPending();
                        continue;
                    }

                    effect.runPending();
                }
            }
        } finally {
            flushing = false;
        }
    }

    protected void doResetForTests() {
        for (SchedulableEffect remaining : queue) {
            remaining.clearPending();
        }
        queue.clear();
        flushing = false;
    }
}

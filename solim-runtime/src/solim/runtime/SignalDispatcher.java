package solim.runtime;

import arc.Events;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.Queue;
import mindustry.game.EventType.Trigger;
import solim.core.SchedulableEffect;
import solim.core.SolimAssert;

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

    public static void register() {
        if (registered)
            return;
        registered = true;
        if (SolimAssert.getMainThread() == null) {
            SolimAssert.setMainThread(Thread.currentThread());
        }

        Events.run(Trigger.update, (Runnable) () -> flush());
    }

    public static void ensureRegistered() {
        if (!registered) {
            register();
        }
    }

    public static void enqueue(@Nullable SchedulableEffect effect) {
        if (effect == null || effect.isDisposed())
            return;
        ensureRegistered();
        INSTANCE.doEnqueue(effect);
    }

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

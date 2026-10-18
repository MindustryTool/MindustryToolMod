package solim.runtime;

import arc.Events;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.Set;
import mindustry.game.EventType.Trigger;
import solim.core.SchedulableEffect;
import solim.core.SolimAssert;
import solim.instrumentation.FlushPass;
import solim.instrumentation.Instrumentation;

public final class SignalDispatcher {

    private static final int MAX_CASCADE_DEPTH = 100;

    private static boolean registered = false;

    private static final Deque<SchedulableEffect> queue = new ArrayDeque<>();
    private static final Set<SchedulableEffect> queued = Collections
            .newSetFromMap(new IdentityHashMap<SchedulableEffect, Boolean>());
    private static boolean flushing = false;

    private SignalDispatcher() {
    }

    public static void register() {
        if (registered) {
            return;
        }
        registered = true;
        Events.run(Trigger.update, (Runnable) () -> flush());
    }

    public static void ensureRegistered() {
        if (!registered) {
            register();
        }
    }

    public static void enqueue(@Nullable SchedulableEffect effect) {
        if (effect == null || effect.isDisposed()) {
            return;
        }
        ensureRegistered();
        if (queued.add(effect)) {
            queue.addLast(effect);
        }
    }

    public static void flush() {
        SolimAssert.checkMainThread();
        if (flushing) {
            return;
        }
        flushing = true;
        long startNs = Instrumentation.enabled() ? System.nanoTime() : 0L;
        try {
            int pass = 0;
            while (!queue.isEmpty()) {
                if (++pass > MAX_CASCADE_DEPTH) {
                    Log.err(
                            "[Solim] Infinite reactive loop detected in SignalDispatcher (exceeded @ cascade iterations). Clearing @ pending effects.",
                            MAX_CASCADE_DEPTH, queue.size());
                    for (SchedulableEffect remaining : queue) {
                        remaining.clearPending();
                    }
                    clear();
                    break;
                }

                int batchSize = queue.size();
                for (int i = 0; i < batchSize; i++) {
                    SchedulableEffect effect = queue.pollFirst();
                    if (effect == null) {
                        continue;
                    }
                    queued.remove(effect);
                    if (effect.isDisposed()) {
                        effect.clearPending();
                        continue;
                    }
                    effect.runPending();
                }
            }
        } finally {
            flushing = false;
            if (Instrumentation.enabled()) {
                Instrumentation.emit(new FlushPass(startNs, System.nanoTime() - startNs));
            }
        }
    }

    public static int size() {
        return queue.size();
    }

    public static boolean isFlushing() {
        return flushing;
    }

    public static boolean isRegistered() {
        return registered;
    }

    public static void resetForTests() {
        for (SchedulableEffect remaining : queue) {
            remaining.clearPending();
        }
        clear();
        flushing = false;
    }

    private static void clear() {
        queue.clear();
        queued.clear();
    }
}
package solim.performance;

import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.PerfSink;
import solim.runtime.ParentStack;

/**
 * Opt-in per-component slow tracker for Solim UI.
 * Records timed spans for slow builds and phases with threshold-based
 * warnings and a bounded in-process ring buffer.
 *
 * <p>When disabled (the default), all recording hooks are zero-overhead
 * no-ops: callers must guard {@code nanoTime} with {@link #isEnabled()}
 * and {@code record} methods return immediately.
 */
public final class SlowTracker {

    public static volatile boolean enabled = false;
    public static volatile float buildThresholdMs = 16f;
    public static volatile float phaseThresholdMs = 50f;

    private static final int BUFFER_SIZE = 256;
    private static final SlowSpan[] buffer = new SlowSpan[BUFFER_SIZE];
    private static int head = 0;
    private static int totalRecorded = 0;

    private static final PerfSink ENGINE_SINK = new PerfSink() {
        @Override
        public void record(String component, String phase, float durationMs, String detail) {
            recordForced(component, phase, durationMs, detail);
        }
    };

    private SlowTracker() {
    }

    public static void setEnabled(boolean state) {
        enabled = state;
        if (state) {
            ParentStack.slowSubtreeThresholdMs = phaseThresholdMs;
            ParentStack.setPerfSink(ENGINE_SINK);
        } else {
            ParentStack.setPerfSink(null);
            reset();
        }
    }

    public static boolean isEnabled() {
        return enabled;
    }

    public static void setThresholds(float buildMs, float phaseMs) {
        buildThresholdMs = Math.max(0f, buildMs);
        phaseThresholdMs = Math.max(0f, phaseMs);
        ParentStack.slowSubtreeThresholdMs = phaseThresholdMs;
    }

    public static synchronized void reset() {
        head = 0;
        totalRecorded = 0;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer[i] = null;
        }
        ParentStack.resetPerfStats();
    }

    public static void recordBuild(String component, float durationMs) {
        if (!enabled) {
            return;
        }
        if (durationMs < buildThresholdMs) {
            return;
        }
        record(component, "build", durationMs, null);
    }

    public static void recordPhase(String component, String phase, float durationMs) {
        recordPhase(component, phase, durationMs, null);
    }

    public static void recordPhase(String component, String phase, float durationMs,
            @Nullable String detail) {
        if (!enabled) {
            return;
        }
        if (durationMs < phaseThresholdMs) {
            return;
        }
        record(component, phase, durationMs, detail);
    }

    /**
     * Records a span regardless of thresholds. Reserved for explicit phase
     * spans whose caller already decided the span is worth keeping.
     */
    public static void recordForced(String component, String phase, float durationMs,
            @Nullable String detail) {
        if (!enabled) {
            return;
        }
        record(component, phase, durationMs, detail);
    }

    private static synchronized void record(String component, String phase, float durationMs,
            @Nullable String detail) {
        SlowSpan span = new SlowSpan(component, phase, durationMs, System.currentTimeMillis(), detail);
        buffer[head] = span;
        head = (head + 1) % BUFFER_SIZE;
        if (totalRecorded < BUFFER_SIZE) {
            totalRecorded++;
        }
        Log.warn("[Solim] Slow component: @", span.toString());
    }

    public static synchronized List<SlowSpan> snapshot(int max) {
        List<SlowSpan> out = new ArrayList<>();
        int n = Math.min(Math.max(0, max), totalRecorded);
        for (int i = 0; i < n; i++) {
            int idx = (head - 1 - i + BUFFER_SIZE) % BUFFER_SIZE;
            SlowSpan span = buffer[idx];
            if (span != null) {
                out.add(span);
            }
        }
        return out;
    }

    public static synchronized int totalRecorded() {
        return totalRecorded;
    }
}

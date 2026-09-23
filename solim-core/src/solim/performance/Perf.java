package solim.performance;

import java.util.List;
import solim.runtime.DebugParentStack;
import solim.runtime.ParentStack;

/**
 * Public profiling facade for Solim UI. Delegates to the active {@link ParentStack} singleton so
 * that profiling can be enabled and queried without exposing the runtime module.
 */
public final class Perf {

    private static boolean profilingRequested = false;
    private static boolean tracingRequested = false;
    private static float thresholdMs = 50f;

    private Perf() {
    }

    /**
     * Enables or disables structural profiling. Enabling installs a fresh profiling-enabled stack
     * variant; disabling reverts to the base stack and discards all recorded spans.
     * Tracing implies slow tracking; disabling profiling while tracing keeps the debug variant.
     */
    public static void setEnabled(boolean state) {
        profilingRequested = state;
        if (tracingRequested) {
            return;
        }
        ParentStack.install(state ? new DebugParentStack() : null);
        if (state) {
            ParentStack.setThreshold(thresholdMs);
        }
    }

    public static boolean isEnabled() {
        return ParentStack.instance() instanceof DebugParentStack;
    }

    /** Sets the slow threshold in milliseconds applied to all structural spans. */
    public static void setThreshold(float ms) {
        thresholdMs = ms < 0f ? 0f : ms;
        ParentStack.setThreshold(thresholdMs);
    }

    /** Returns recent spans in recency order, up to the buffer bound. */
    public static List<PerfSpan> snapshot(int max) {
        return ParentStack.snapshot(max);
    }

    public static int totalRecorded() {
        return ParentStack.totalRecorded();
    }

    /** Clears recorded spans and the maximum observed depth. */
    public static void reset() {
        ParentStack.reset();
    }

    public static int maxDepthObserved() {
        return ParentStack.maxDepthObserved();
    }

    /**
     * Enables or disables hierarchical flame tracing. Enabling starts a fresh trace generation
     * on the debug variant, preserving slow spans; disabling clears traces and reverts to
     * profiling or base.
     */
    public static void setTracingEnabled(boolean state) {
        tracingRequested = state;
        if (state) {
            if (!(ParentStack.instance() instanceof DebugParentStack)) {
                ParentStack.install(new DebugParentStack());
                ParentStack.setThreshold(thresholdMs);
            }
            ParentStack.setTracingEnabled(true);
        } else {
            if (ParentStack.instance() instanceof DebugParentStack) {
                ParentStack.setTracingEnabled(false);
            }
            if (!profilingRequested) {
                ParentStack.install(null);
            }
        }
    }

    public static boolean isTracing() {
        return ParentStack.isTracing();
    }

    /** Returns trace spans in document order, up to the buffer bound. */
    public static List<TraceSpan> traceSnapshot(int max) {
        return ParentStack.traceSnapshot(max);
    }

    /** Clears recorded trace spans and overflow counters, preserving enablement. */
    public static void traceReset() {
        ParentStack.traceReset();
    }

    /** Returns the bounded ring capacity (4096). */
    public static int traceCapacity() {
        return DebugParentStack.TRACE_CAPACITY;
    }

    /** Returns the number of oldest spans evicted on overflow. */
    public static int traceDroppedCount() {
        return ParentStack.traceDroppedCount();
    }

    /** Returns total appended spans including evicted ones. */
    public static int traceTotal() {
        return ParentStack.traceTotal();
    }

    /** Returns the current trace generation id, or -1 when not tracing. */
    public static long traceId() {
        return ParentStack.traceId();
    }
}

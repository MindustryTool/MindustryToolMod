package solim.performance;

import java.util.List;
import solim.runtime.DebugAttachmentStack;
import solim.runtime.AttachmentStack;

/**
 * Public profiling facade for Solim UI. Delegates to the active {@link AttachmentStack} singleton so
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
        AttachmentStack.install(state ? new DebugAttachmentStack() : null);
        if (state) {
            AttachmentStack.setThreshold(thresholdMs);
        }
    }

    public static boolean isEnabled() {
        return AttachmentStack.instance() instanceof DebugAttachmentStack;
    }

    /** Sets the slow threshold in milliseconds applied to all structural spans. */
    public static void setThreshold(float ms) {
        thresholdMs = ms < 0f ? 0f : ms;
        AttachmentStack.setThreshold(thresholdMs);
    }

    /** Returns recent spans in recency order, up to the buffer bound. */
    public static List<PerfSpan> snapshot(int max) {
        return AttachmentStack.snapshot(max);
    }

    public static int totalRecorded() {
        return AttachmentStack.totalRecorded();
    }

    /** Clears recorded spans and the maximum observed depth. */
    public static void reset() {
        AttachmentStack.reset();
    }

    public static int maxDepthObserved() {
        return AttachmentStack.maxDepthObserved();
    }

    /**
     * Enables or disables hierarchical flame tracing. Enabling starts a fresh trace generation
     * on the debug variant, preserving slow spans; disabling clears traces and reverts to
     * profiling or base.
     */
    public static void setTracingEnabled(boolean state) {
        tracingRequested = state;
        if (state) {
            if (!(AttachmentStack.instance() instanceof DebugAttachmentStack)) {
                AttachmentStack.install(new DebugAttachmentStack());
                AttachmentStack.setThreshold(thresholdMs);
            }
            AttachmentStack.setTracingEnabled(true);
        } else {
            if (AttachmentStack.instance() instanceof DebugAttachmentStack) {
                AttachmentStack.setTracingEnabled(false);
            }
            if (!profilingRequested) {
                AttachmentStack.install(null);
            }
        }
    }

    public static boolean isTracing() {
        return AttachmentStack.isTracing();
    }

    /** Returns trace spans in document order, up to the buffer bound. */
    public static List<TraceSpan> traceSnapshot(int max) {
        return AttachmentStack.traceSnapshot(max);
    }

    /** Clears recorded trace spans and overflow counters, preserving enablement. */
    public static void traceReset() {
        AttachmentStack.traceReset();
    }

    /** Returns the bounded ring capacity (4096). */
    public static int traceCapacity() {
        return DebugAttachmentStack.TRACE_CAPACITY;
    }

    /** Returns the number of oldest spans evicted on overflow. */
    public static int traceDroppedCount() {
        return AttachmentStack.traceDroppedCount();
    }

    /** Returns total appended spans including evicted ones. */
    public static int traceTotal() {
        return AttachmentStack.traceTotal();
    }

    /** Returns the current trace generation id, or -1 when not tracing. */
    public static long traceId() {
        return AttachmentStack.traceId();
    }
}

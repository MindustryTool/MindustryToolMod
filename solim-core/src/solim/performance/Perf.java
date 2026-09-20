package solim.performance;

import java.util.List;
import solim.runtime.DebugParentStack;
import solim.runtime.ParentStack;

/**
 * Public profiling facade for Solim UI. Delegates to the active {@link ParentStack} singleton so
 * that profiling can be enabled and queried without exposing the runtime module.
 */
public final class Perf {

    private Perf() {
    }

    /**
     * Enables or disables structural profiling. Enabling installs a fresh profiling-enabled stack
     * variant; disabling reverts to the base stack and discards all recorded spans.
     */
    public static void setEnabled(boolean state) {
        ParentStack.install(state ? new DebugParentStack() : null);
    }

    public static boolean isEnabled() {
        return ParentStack.instance() instanceof DebugParentStack;
    }

    /** Sets the slow threshold in milliseconds applied to all structural spans. */
    public static void setThreshold(float ms) {
        ParentStack.setThreshold(ms);
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
}

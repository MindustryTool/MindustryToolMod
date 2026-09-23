package solim.performance;

import arc.util.Nullable;

/**
 * Immutable value describing a single hierarchical build span recorded while
 * flame tracing is enabled.
 */
public final class TraceSpan {
    public final long traceId;
    public final int spanId;
    public final int parentId;
    public final String name;
    public final String phase;
    public final long startNs;
    public final long endNs;
    public final int depth;
    public final int childCount;

    public TraceSpan(long traceId, int spanId, int parentId, @Nullable String name,
            @Nullable String phase, long startNs, long endNs, int depth, int childCount) {
        this.traceId = traceId;
        this.spanId = spanId;
        this.parentId = parentId;
        this.name = name != null && !name.trim().isEmpty() ? name : "Component-element";
        this.phase = phase != null && !phase.trim().isEmpty() ? phase : "build";
        this.startNs = startNs;
        this.endNs = endNs;
        this.depth = depth < 0 ? 0 : depth;
        this.childCount = childCount < 0 ? 0 : childCount;
    }

    /** Duration in nanoseconds, derived as {@code endNs - startNs}. */
    public long durationNs() {
        long duration = endNs - startNs;
        return duration >= 0 ? duration : 0;
    }

    /** Duration in milliseconds as a float. */
    public float durationMs() {
        return durationNs() / 1000000f;
    }

    @Override
    public String toString() {
        return name + "[" + phase + "] " + durationMs() + "ms depth=" + depth
                + " children=" + childCount + " span=" + spanId + " parent=" + parentId;
    }
}

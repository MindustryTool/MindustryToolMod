package solim.performance;

import arc.util.Nullable;

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

    public long durationNs() {
        long duration = endNs - startNs;
        return duration >= 0 ? duration : 0;
    }

    public float durationMs() {
        return durationNs() / 1000000f;
    }

    @Override
    public String toString() {
        return name + "[" + phase + "] " + durationMs() + "ms depth=" + depth
                + " children=" + childCount + " span=" + spanId + " parent=" + parentId;
    }
}

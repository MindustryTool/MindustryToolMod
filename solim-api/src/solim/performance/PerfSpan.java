package solim.performance;

import arc.util.Nullable;

/**
 * Immutable value describing a single slow structural span recorded by the
 * debug parent stack.
 */
public final class PerfSpan {
    public final String component;
    public final String phase;
    public final float durationMs;
    public final long timestamp;
    public final @Nullable String detail;

    public PerfSpan(String component, String phase, float durationMs, long timestamp,
            @Nullable String detail) {
        this.component = component != null ? component : "unknown";
        this.phase = phase != null ? phase : "subtree";
        this.durationMs = durationMs;
        this.timestamp = timestamp;
        this.detail = detail;
    }

    @Override
    public String toString() {
        return detail != null
                ? component + "[" + phase + "] " + durationMs + "ms (" + detail + ")"
                : component + "[" + phase + "] " + durationMs + "ms";
    }
}

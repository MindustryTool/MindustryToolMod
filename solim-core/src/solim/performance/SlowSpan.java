package solim.performance;

import arc.util.Nullable;

/**
 * Immutable value describing a single slow component build or phase.
 */
public final class SlowSpan {
    public final String component;
    public final String phase;
    public final float durationMs;
    public final long timestamp;
    public final @Nullable String detail;

    public SlowSpan(String component, String phase, float durationMs, long timestamp,
            @Nullable String detail) {
        this.component = component != null ? component : "unknown";
        this.phase = phase != null ? phase : "build";
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

package solim.performance;

import java.util.Collections;
import java.util.List;

/**
 * Immutable debug snapshot of a single {@code Computed}'s runtime metrics, captured by the profiling
 * session when the report is written.
 *
 * <p>Durations are nanoseconds. Dependency labels are truncated textual summaries of the tracked
 * {@code ReactiveSource}s rather than references, so the snapshot stays detached from the live graph.
 */
public final class ComputedSnapshot {

    /** Monotonic per-session identifier assigned by the debug runtime. */
    public final int id;
    /** Truncated {@code toString()} summary of the cached value at capture time. */
    public final String value;
    /** Number of recomputations performed during the session. */
    public final int recomputeCount;
    /** Summed nanoseconds spent inside {@code Prov.get()} across all recomputations. */
    public final long totalNs;
    /** Nanoseconds spent inside the most recent recomputation. */
    public final long lastNs;
    /** Whether the computed was still dirty at capture time. */
    public final boolean dirty;
    /** Truncated labels of the currently tracked dependency sources. */
    public final List<String> dependencies;

    public ComputedSnapshot(int id, String value, int recomputeCount, long totalNs, long lastNs,
            boolean dirty, List<String> dependencies) {
        this.id = id;
        this.value = value != null ? value : "";
        this.recomputeCount = recomputeCount < 0 ? 0 : recomputeCount;
        this.totalNs = totalNs < 0L ? 0L : totalNs;
        this.lastNs = lastNs < 0L ? 0L : lastNs;
        this.dirty = dirty;
        this.dependencies = dependencies != null ? dependencies : Collections.<String>emptyList();
    }

    /** Mean recomputation duration in nanoseconds, or {@code 0} when never recomputed. */
    public long averageNs() {
        return recomputeCount == 0 ? 0L : totalNs / recomputeCount;
    }

    @Override
    public String toString() {
        return "Computed#" + id + "[recomputes=" + recomputeCount + " deps=" + dependencies.size() + "] " + value;
    }
}
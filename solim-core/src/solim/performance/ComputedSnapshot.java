package solim.performance;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ComputedSnapshot {

    public final int id;

    public final String value;

    public final int recomputeCount;

    public final long totalNs;

    public final long lastNs;

    public final boolean dirty;

    public final List<String> dependencies;

    public ComputedSnapshot(int id, String value, int recomputeCount, long totalNs, long lastNs,
            boolean dirty, List<String> dependencies) {
        this.id = id;
        this.value = value != null ? value : "";
        this.recomputeCount = recomputeCount < 0 ? 0 : recomputeCount;
        this.totalNs = totalNs < 0L ? 0L : totalNs;
        this.lastNs = lastNs < 0L ? 0L : lastNs;
        this.dirty = dirty;
        this.dependencies = dependencies != null
                ? Collections.unmodifiableList(new ArrayList<>(dependencies))
                : Collections.<String>emptyList();
    }

    public long averageNs() {
        return recomputeCount == 0 ? 0L : totalNs / recomputeCount;
    }

    @Override
    public String toString() {
        return "Computed#" + id + "[recomputes=" + recomputeCount + " deps=" + dependencies.size() + "] " + value;
    }
}

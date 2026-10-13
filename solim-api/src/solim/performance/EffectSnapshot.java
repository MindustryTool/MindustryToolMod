package solim.performance;

import java.util.Collections;
import java.util.List;

public final class EffectSnapshot {

    public final int id;

    public final int runCount;

    public final long totalNs;

    public final long lastNs;

    public final List<String> dependencies;

    public EffectSnapshot(int id, int runCount, long totalNs, long lastNs, List<String> dependencies) {
        this.id = id;
        this.runCount = runCount < 0 ? 0 : runCount;
        this.totalNs = totalNs < 0L ? 0L : totalNs;
        this.lastNs = lastNs < 0L ? 0L : lastNs;
        this.dependencies = dependencies != null ? dependencies : Collections.<String>emptyList();
    }

    public long averageNs() {
        return runCount == 0 ? 0L : totalNs / runCount;
    }

    @Override
    public String toString() {
        return "Effect#" + id + "[runs=" + runCount + " deps=" + dependencies.size() + "]";
    }
}

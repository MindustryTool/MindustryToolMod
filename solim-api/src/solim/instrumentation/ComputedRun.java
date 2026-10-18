package solim.instrumentation;

public final class ComputedRun implements InstrumentationEvent {

    public final Object computed;
    public final long startNs;
    public final long durationNs;
    public final boolean changed;

    public ComputedRun(Object computed, long startNs, long durationNs, boolean changed) {
        this.computed = computed;
        this.startNs = startNs;
        this.durationNs = durationNs < 0L ? 0L : durationNs;
        this.changed = changed;
    }

    public long endNs() {
        return startNs + durationNs;
    }
}
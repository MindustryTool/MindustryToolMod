package solim.instrumentation;

public final class FlushPass implements InstrumentationEvent {

    public final long startNs;
    public final long durationNs;

    public FlushPass(long startNs, long durationNs) {
        this.startNs = startNs;
        this.durationNs = durationNs < 0L ? 0L : durationNs;
    }

    public long endNs() {
        return startNs + durationNs;
    }
}
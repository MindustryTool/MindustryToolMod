package solim.instrumentation;

import arc.util.Nullable;

public final class SignalWrite implements InstrumentationEvent {

    public final Object signal;
    public final @Nullable Object oldValue;
    public final @Nullable Object newValue;
    public final long startNs;

    public SignalWrite(Object signal, @Nullable Object oldValue, @Nullable Object newValue, long startNs) {
        this.signal = signal;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.startNs = startNs;
    }

    public long endNs() {
        return System.nanoTime();
    }

    public long durationNs() {
        return endNs() - startNs;
    }
}
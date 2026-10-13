package solim.performance;

/**
 * Immutable debug snapshot of a single {@code Signal}'s runtime metrics,
 * captured by the profiling
 * session when the report is written.
 *
 * <p>
 * The live signal value is never held here — only a truncated textual summary —
 * so exporting a
 * profile cannot retain large or cyclic object graphs.
 */
public final class SignalSnapshot {

    public final int id;

    public final String value;

    public final int writeCount;

    public final int observerCount;

    public final int listenerCount;

    public SignalSnapshot(int id, String value, int writeCount, int observerCount, int listenerCount) {
        this.id = id;
        this.value = value != null ? value : "";
        this.writeCount = writeCount < 0 ? 0 : writeCount;
        this.observerCount = observerCount < 0 ? 0 : observerCount;
        this.listenerCount = listenerCount < 0 ? 0 : listenerCount;
    }

    @Override
    public String toString() {
        return "Signal#" + id + "[writes=" + writeCount + " observers=" + observerCount + "] " + value;
    }
}

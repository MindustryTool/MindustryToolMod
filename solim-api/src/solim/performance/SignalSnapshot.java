package solim.performance;

/**
 * Immutable debug snapshot of a single {@code Signal}'s runtime metrics, captured by the profiling
 * session when the report is written.
 *
 * <p>The live signal value is never held here — only a truncated textual summary — so exporting a
 * profile cannot retain large or cyclic object graphs.
 */
public final class SignalSnapshot {

    /** Monotonic per-session identifier assigned by the debug runtime. */
    public final int id;
    /** Truncated {@code toString()} summary of the value at capture time. */
    public final String value;
    /** Number of times {@code set(...)} changed the value during the session. */
    public final int writeCount;
    /** Number of {@code ReactiveObserver}s currently attached. */
    public final int observerCount;
    /** Number of plain listeners currently subscribed. */
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
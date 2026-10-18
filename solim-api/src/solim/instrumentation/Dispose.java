package solim.instrumentation;

public final class Dispose implements InstrumentationEvent {

    public final Object component;
    public final int childCount;
    public final int ownedCount;

    public Dispose(Object component, int childCount, int ownedCount) {
        this.component = component;
        this.childCount = childCount < 0 ? 0 : childCount;
        this.ownedCount = ownedCount < 0 ? 0 : ownedCount;
    }
}
package solim.instrumentation;

public final class ReconcilePass implements InstrumentationEvent {

    public final Object reconciler;
    public final int created;
    public final int reused;
    public final int total;

    public ReconcilePass(Object reconciler, int created, int reused, int total) {
        this.reconciler = reconciler;
        this.created = created;
        this.reused = reused;
        this.total = total;
    }
}
package solim.instrumentation;

public final class ReactiveCreated implements InstrumentationEvent {

    public final Object node;

    public ReactiveCreated(Object node) {
        this.node = node;
    }
}
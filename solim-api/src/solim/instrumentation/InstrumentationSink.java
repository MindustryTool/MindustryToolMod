package solim.instrumentation;

public interface InstrumentationSink {

    void accept(InstrumentationEvent event);

    void reset();
}
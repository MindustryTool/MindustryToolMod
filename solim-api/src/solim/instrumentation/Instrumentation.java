package solim.instrumentation;

import arc.util.Nullable;

public final class Instrumentation {

    private static volatile boolean enabled = false;
    private static volatile @Nullable InstrumentationSink sink = null;

    private Instrumentation() {
    }

    public static boolean enabled() {
        return enabled;
    }

    public static @Nullable InstrumentationSink sink() {
        return sink;
    }

    public static void install(@Nullable InstrumentationSink next) {
        sink = next;
        enabled = next != null;
    }

    public static void uninstall() {
        install(null);
    }

    public static void emit(InstrumentationEvent event) {
        InstrumentationSink current = sink;
        if (current != null) {
            current.accept(event);
        }
    }
}
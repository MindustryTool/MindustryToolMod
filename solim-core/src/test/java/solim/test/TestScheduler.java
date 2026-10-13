package solim.test;

import solim.runtime.SignalDispatcher;

public final class TestScheduler {

    private TestScheduler() {
    }

    public static void flush() {
        SignalDispatcher.flush();
    }

    public static int pendingCount() {
        return SignalDispatcher.size();
    }

    public static boolean isFlushing() {
        return SignalDispatcher.isFlushing();
    }

    public static void clear() {
        SignalDispatcher.resetForTests();
    }
}

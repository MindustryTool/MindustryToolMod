package solim.core;

public interface SchedulableEffect {
    boolean isDisposed();

    void clearPending();

    void runPending();
}

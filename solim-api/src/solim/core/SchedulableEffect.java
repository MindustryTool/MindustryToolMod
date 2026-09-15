package solim.core;

/** Internal abstraction for reactive effects scheduled by SignalDispatcher. */
public interface SchedulableEffect {
	boolean isDisposed();
	void clearPending();
	void runPending();
}

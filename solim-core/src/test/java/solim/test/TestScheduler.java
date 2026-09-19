package solim.test;

import solim.runtime.SignalDispatcher;

/**
 * Deterministic test scheduler utility for fine-grained control over reactive effect dispatching.
 */
public final class TestScheduler {

	private TestScheduler() {}

	/**
	 * Synchronously flushes all pending dirty effects in deterministic FIFO order.
	 */
	public static void flush() {
		SignalDispatcher.flush();
	}

	/**
	 * Returns the number of currently enqueued effects.
	 */
	public static int pendingCount() {
		return SignalDispatcher.size();
	}

	/**
	 * Returns whether the dispatcher is currently executing a flush.
	 */
	public static boolean isFlushing() {
		return SignalDispatcher.isFlushing();
	}

	/**
	 * Clears any pending effects without executing them.
	 */
	public static void clear() {
		SignalDispatcher.resetForTests();
	}
}

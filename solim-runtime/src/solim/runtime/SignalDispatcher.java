package solim.runtime;

import arc.Events;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.Queue;
import mindustry.game.EventType.Trigger;
import solim.core.SchedulableEffect;

/**
 * Internal single-threaded dispatcher that batches and deduplicates reactive effect
 * invalidations to run at most once per Mindustry frame.
 */
public final class SignalDispatcher {
	private static final int MAX_CASCADE_DEPTH = 100;
	private static final Queue<SchedulableEffect> queue = new ArrayDeque<>();
	private static boolean registered = false;
	private static boolean flushing = false;

	static {
		register();
	}

	private SignalDispatcher() {}

	/**
	 * Registers the frame flush callback with Mindustry's {@link Trigger#update} event.
	 */
	public static void register() {
		if (registered) return;
		registered = true;
		Events.run(Trigger.update, SignalDispatcher::flush);
	}

	/**
	 * Ensures that the dispatcher is hooked into the frame lifecycle.
	 */
	public static void ensureRegistered() {
		if (!registered) {
			register();
		}
	}

	/**
	 * Enqueues an invalidated effect for batched execution on the next frame flush.
	 */
	public static void enqueue(@Nullable SchedulableEffect effect) {
		if (effect == null || effect.isDisposed()) return;
		ensureRegistered();
		queue.add(effect);
	}

	/**
	 * Flushes and executes all pending effects in FIFO order on the caller's thread.
	 */
	public static void flush() {
		if (flushing) return;
		flushing = true;
		try {
			int pass = 0;
			while (!queue.isEmpty()) {
				if (++pass > MAX_CASCADE_DEPTH) {
					Log.err("[Solim] Infinite reactive loop detected in SignalDispatcher (exceeded @ cascade iterations). Clearing @ pending effects.",
							MAX_CASCADE_DEPTH, queue.size());
					for (SchedulableEffect remaining : queue) {
						remaining.clearPending();
					}
					queue.clear();
					break;
				}

				int batchSize = queue.size();
				for (int i = 0; i < batchSize; i++) {
					SchedulableEffect effect = queue.poll();
					if (effect == null) continue;

					if (effect.isDisposed()) {
						effect.clearPending();
						continue;
					}

					try {
						effect.runPending();
					} catch (Throwable t) {
						Log.err("[Solim] Error executing reactive effect", t);
					}
				}
			}
		} finally {
			flushing = false;
		}
	}

	/** Returns the number of currently pending effects in the queue. */
	public static int size() {
		return queue.size();
	}

	/** Returns true if the dispatcher is currently executing a flush cycle. */
	public static boolean isFlushing() {
		return flushing;
	}

	/** Returns whether the frame callback has been registered. */
	public static boolean isRegistered() {
		return registered;
	}

	/**
	 * Resets dispatcher state for isolated test execution.
	 */
	public static void resetForTests() {
		for (SchedulableEffect remaining : queue) {
			remaining.clearPending();
		}
		queue.clear();
		flushing = false;
	}
}

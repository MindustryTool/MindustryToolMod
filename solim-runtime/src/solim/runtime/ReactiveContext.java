package solim.runtime;

import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import solim.core.OwnershipContext;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import solim.core.SolimAssert;
import arc.func.Prov;

/** Stack-based dependency tracking for single-threaded UI. No ThreadLocal by design. */
public final class ReactiveContext {
	private static final Deque<ReactiveObserver> stack = new ArrayDeque<>();
	/**
	 * Depth at which tracking is suspended: observers at or below it are invisible to
	 * {@link #current()}, observers pushed above it (inside the untracked block) still collect.
	 * Zero when nothing is suspended.
	 */
	private static int suspendFloor = 0;

	private ReactiveContext() {}

	public static void push(ReactiveObserver observer) {
		SolimAssert.checkMainThread();
		stack.push(observer);
	}

	public static void pop() {
		if (!stack.isEmpty()) stack.pop();
	}

	/**
	 * Returns the observer currently collecting dependencies, or {@code null} when none is
	 * evaluating or every observer present was suspended by an enclosing {@code untracked} block.
	 */
	public static ReactiveObserver current() {
		return stack.size() > suspendFloor ? stack.peek() : null;
	}

	/**
	 * Called from reactive sources to register dependency with current observer.
	 *
	 * <p>The thread check is deliberately inside the registration branch rather than at the top:
	 * a background read that finds no active observer records nothing and is therefore harmless,
	 * while a background read that attaches itself to an observer mid-evaluation corrupts the
	 * tracking stack and must fail.
	 */
	public static void track(@Nullable ReactiveSource observable) {
		ReactiveObserver cur = current();
		if (cur != null && observable != null) {
			SolimAssert.checkMainThread();
			cur.addDependency(observable);
		}
	}

	/**
	 * Shared tracking path for reactive reads. Emits a build-time warning when called
	 * during {@code build()} without an active reactive context, then tracks the source.
	 */
	public static void trackWithWarning(ReactiveSource source, String warning) {
		if (current() == null && OwnershipContext.current() != null) {
			Log.debug(warning);
		}
		track(source);
	}

	/** For tests: clear stack */
	public static void clear() {
		stack.clear();
		suspendFloor = 0;
	}

	/** For tests: size */
	public static int size() {
		return stack.size();
	}

	/**
	 * Temporarily suspends active dependency tracking while executing the Prov.
	 *
	 * <p>Suspending raises a floor rather than emptying the stack, so reads are still attributed
	 * to any observer pushed <em>inside</em> the block — a component built inside a factory runs
	 * its own effects there, and those effects must still track their own sources. Restoration is
	 * by recorded depth rather than by copying the observer stack, so a block that pushes an
	 * observer without popping it still leaves the stack at its entry depth.
	 */
	public static <T> T untracked(Prov<T> Prov) {
		int depth = stack.size();
		int floor = suspendFloor;
		suspendFloor = Math.max(floor, depth);
		try {
			return Prov.get();
		} finally {
			suspendFloor = floor;
			truncate(depth);
		}
	}

	/** Temporarily suspends active dependency tracking while executing the runnable. */
	public static void untracked(Runnable runnable) {
		int depth = stack.size();
		int floor = suspendFloor;
		suspendFloor = Math.max(floor, depth);
		try {
			runnable.run();
		} finally {
			suspendFloor = floor;
			truncate(depth);
		}
	}

	/** Pops observers until the stack is exactly {@code depth} deep. */
	private static void truncate(int depth) {
		while (stack.size() > depth) {
			stack.pop();
		}
	}
}

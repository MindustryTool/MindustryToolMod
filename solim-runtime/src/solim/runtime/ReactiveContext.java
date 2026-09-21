package solim.runtime;

import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;
import solim.core.ReactiveObserver;
import solim.core.ReactiveSource;
import arc.func.Prov;

/** Stack-based dependency tracking for single-threaded UI. No ThreadLocal by design. */
public final class ReactiveContext {
	private static final Deque<ReactiveObserver> stack = new ArrayDeque<>();

	private ReactiveContext() {}

	public static void push(ReactiveObserver observer) {
		stack.push(observer);
	}

	public static void pop() {
		if (!stack.isEmpty()) stack.pop();
	}

	public static ReactiveObserver current() {
		return stack.peek();
	}

	/** Called from reactive sources to register dependency with current observer. */
	public static void track(@Nullable ReactiveSource observable) {
		ReactiveObserver cur = current();
		if (cur != null && observable != null) {
			cur.addDependency(observable);
		}
	}

	/**
	 * Shared tracking path for reactive reads. Emits a build-time warning when called
	 * during {@code build()} without an active reactive context, then tracks the source.
	 */
	public static void trackWithWarning(ReactiveSource source, String warning) {
		if (current() == null && ComponentContext.current() != null) {
			Log.debug(warning);
		}
		track(source);
	}

	/** For tests: clear stack */
	public static void clear() {
		stack.clear();
	}

	/** For tests: size */
	public static int size() {
		return stack.size();
	}

	/** Temporarily suspends active dependency tracking while executing the Prov. */
	public static <T> T untracked(Prov<T> Prov) {
		if (stack.isEmpty()) {
			return Prov.get();
		}
		Deque<ReactiveObserver> saved = new ArrayDeque<>(stack);
		stack.clear();
		try {
			return Prov.get();
		} finally {
			stack.clear();
			stack.addAll(saved);
		}
	}

	/** Temporarily suspends active dependency tracking while executing the runnable. */
	public static void untracked(Runnable runnable) {
		if (stack.isEmpty()) {
			runnable.run();
			return;
		}
		Deque<ReactiveObserver> saved = new ArrayDeque<>(stack);
		stack.clear();
		try {
			runnable.run();
		} finally {
			stack.clear();
			stack.addAll(saved);
		}
	}
}

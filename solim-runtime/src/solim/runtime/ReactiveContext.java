package solim.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import solim.core.ReactiveObserver;
import java.util.function.Supplier;

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

	/** Called from Signal.get() / Computed.get() to register dependency with current observer. */
	public static void track(Object observable) {
		ReactiveObserver cur = current();
		if (cur != null) {
			cur.addDependency(observable);
		}
	}

	/** For tests: clear stack */
	public static void clear() {
		stack.clear();
	}

	/** For tests: size */
	public static int size() {
		return stack.size();
	}

	/** Temporarily suspends active dependency tracking while executing the supplier. */
	public static <T> T untracked(Supplier<T> supplier) {
		if (stack.isEmpty()) {
			return supplier.get();
		}
		Deque<ReactiveObserver> saved = new ArrayDeque<>(stack);
		stack.clear();
		try {
			return supplier.get();
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

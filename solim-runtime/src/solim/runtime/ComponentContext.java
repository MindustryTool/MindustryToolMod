package solim.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.function.Consumer;
import solim.core.Component;
import solim.core.Disposable;

/**
 * Ambient component build context tracking the active building component. Allows child components,
 * disposables, and reactive bindings instantiated during build() to be automatically registered and
 * owned without manual own() calls.
 */
public final class ComponentContext {
	private static final Deque<Consumer<Disposable>> stack = new ArrayDeque<>();
	private static int paused = 0;

	private ComponentContext() {}

	public static void push(Consumer<Disposable> registrar) {
		if (registrar != null) {
			stack.push(registrar);
		}
	}

	public static Consumer<Disposable> pop() {
		if (!stack.isEmpty()) {
			return stack.pop();
		}
		return null;
	}

	public static Consumer<Disposable> current() {
		if (paused > 0) {
			return null;
		}
		return stack.peek();
	}

	/**
	 * Suspends automatic ownership registration for resources created within the given action.
	 * Auto-ownership is guaranteed to be restored after {@code action} returns, even if it throws.
	 */
	public static void withoutAutoOwnership(Runnable action) {
		paused++;
		try {
			action.run();
		} finally {
			if (paused > 0) {
				paused--;
			}
		}
	}

	public static void clear() {
		stack.clear();
		paused = 0;
	}

	public static int size() {
		return stack.size();
	}

	/** Registers a disposable with the currently active building component, if any. */
	public static <T extends Disposable> T register(T disposable) {
		if (paused > 0 || disposable == null) {
			return disposable;
		}
		Consumer<Disposable> current = stack.peek();
		if (current != null) {
			current.accept(disposable);
		}
		return disposable;
	}

	/** Registers a child component with the currently active building component, if any. */
	public static <T extends Component> T registerChild(T child) {
		if (paused > 0 || child == null) {
			return child;
		}
		Consumer<Disposable> current = stack.peek();
		if (current != null) {
			current.accept(child);
		}
		return child;
	}
}

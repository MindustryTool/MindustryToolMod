package solim.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import arc.func.Cons;
import solim.core.Component;
import solim.core.Disposable;

/**
 * Ambient component build context tracking the active building component. Allows child components,
 * disposables, and reactive bindings instantiated during build() to be automatically registered and
 * owned without manual own() calls.
 */
public final class ComponentContext {
	private static final Deque<Cons<Disposable>> stack = new ArrayDeque<>();

	private ComponentContext() {}

	public static void push(Cons<Disposable> registrar) {
		SolimAssert.checkMainThread();
		if (registrar != null) {
			stack.push(registrar);
		}
	}

	public static Cons<Disposable> pop() {
		SolimAssert.checkMainThread();
		if (!stack.isEmpty()) {
			return stack.pop();
		}
		return null;
	}

	public static Cons<Disposable> current() {
		return stack.peek();
	}

	/**
	 * Suspends automatic ownership registration for resources created within the given action.
	 * Nested component scopes instantiated inside action will manage their own ownership.
	 * Auto-ownership is guaranteed to be restored after {@code action} returns, even if it throws.
	 */
	public static void withoutAutoOwnership(Runnable action) {
		if (action == null) {
			return;
		}
		Deque<Cons<Disposable>> saved = new ArrayDeque<>(stack);
		stack.clear();
		try {
			action.run();
		} finally {
			stack.clear();
			stack.addAll(saved);
		}
	}

	public static void clear() {
		stack.clear();
	}

	public static int size() {
		return stack.size();
	}

	/** Registers a disposable with the currently active building component, if any. */
	public static <T extends Disposable> T register(T disposable) {
		if (disposable == null) {
			return null;
		}
		Cons<Disposable> current = stack.peek();
		if (current != null) {
			current.get(disposable);
		}
		return disposable;
	}

	/** Registers a child component with the currently active building component, if any. */
	public static <T extends Component> T registerChild(T child) {
		if (child == null) {
			return null;
		}
		Cons<Disposable> current = stack.peek();
		if (current != null) {
			current.get(child);
		}
		return child;
	}
}

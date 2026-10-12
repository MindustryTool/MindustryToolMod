package solim.core;

import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.Deque;

/**
 * Ambient component build context tracking the active building component. Allows child components,
 * disposables, and reactive bindings instantiated during build() to be automatically registered and
 * owned without manual own() calls.
 *
 * <p>The stack holds owners directly rather than a registration callback, so pushing one costs
 * nothing. Registration is not filtered by type: every {@link Disposable} handed to the active
 * owner is collected. Suspending collection is expressed by the runtime's {@code OwnershipScope},
 * which keeps what is registered and decides who ends up owning it — never by clearing this stack.
 */
public final class OwnershipContext {
	private static final Deque<DisposableOwner> stack = new ArrayDeque<>();

	private OwnershipContext() {}

	public static void push(@Nullable DisposableOwner owner) {
		SolimAssert.checkMainThread();
		if (owner != null) {
			stack.push(owner);
		}
	}

	public static @Nullable DisposableOwner pop() {
		SolimAssert.checkMainThread();
		if (!stack.isEmpty()) {
			return stack.pop();
		}
		return null;
	}

	public static @Nullable DisposableOwner current() {
		return stack.peek();
	}

	public static void clear() {
		stack.clear();
	}

	public static int size() {
		return stack.size();
	}

	/** Registers a disposable with the currently active building component, if any. */
	public static <T extends Disposable> T register(@Nullable T disposable) {
		if (disposable == null) {
			return null;
		}
		DisposableOwner current = stack.peek();
		if (current != null) {
			current.adopt(disposable);
		}
		return disposable;
	}

	/** Registers a child component with the currently active building component, or SessionRoot if unparented. */
	public static <T extends Component> T registerChild(@Nullable T child) {
		if (child == null) {
			return null;
		}
		DisposableOwner current = stack.peek();
		if (current != null) {
			current.adopt(child);
		} else {
			SessionRoot.get().adopt(child);
		}
		return child;
	}
}

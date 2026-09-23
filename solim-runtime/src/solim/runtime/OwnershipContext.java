package solim.runtime;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import arc.func.Cons;
import solim.core.Component;
import solim.core.Disposable;

/**
 * Ambient component build context tracking the active building component. Allows child components,
 * disposables, and reactive bindings instantiated during build() to be automatically registered and
 * owned without manual own() calls.
 */
public final class OwnershipContext {
	private static final Deque<Cons<Disposable>> stack = new ArrayDeque<>();
	private static final ThreadLocal<Deque<CaptureRegistrar>> REGISTRAR_POOL =
			ThreadLocal.withInitial(ArrayDeque::new);

	private OwnershipContext() {}

	private static final class CaptureRegistrar implements Cons<Disposable> {
		private List<Component> captured;

		void setTarget(List<Component> captured) {
			this.captured = captured;
		}

		@Override
		public void get(Disposable disposable) {
			List<Component> roots = captured;
			if (roots != null && disposable instanceof Component) {
				roots.add((Component) disposable);
			}
		}
	}

	private static CaptureRegistrar takeRegistrar() {
		Deque<CaptureRegistrar> pool = REGISTRAR_POOL.get();
		CaptureRegistrar registrar = pool.pollFirst();
		return registrar != null ? registrar : new CaptureRegistrar();
	}

	private static void releaseRegistrar(CaptureRegistrar registrar) {
		registrar.setTarget(null);
		REGISTRAR_POOL.get().push(registrar);
	}

	private static void releaseRegistrars(Deque<Cons<Disposable>> registrars) {
		for (Cons<Disposable> registrar : registrars) {
			if (registrar instanceof CaptureRegistrar) {
				releaseRegistrar((CaptureRegistrar) registrar);
			}
		}
	}

	/** Pushes a reusable registrar that captures component roots in creation order. */
	static void pushCapture(List<Component> captured) {
		SolimAssert.checkMainThread();
		if (captured == null) {
			return;
		}
		CaptureRegistrar registrar = takeRegistrar();
		registrar.setTarget(captured);
		stack.push(registrar);
	}

	public static void push(Cons<Disposable> registrar) {
		SolimAssert.checkMainThread();
		if (registrar != null) {
			stack.push(registrar);
		}
	}

	public static Cons<Disposable> pop() {
		SolimAssert.checkMainThread();
		if (!stack.isEmpty()) {
			Cons<Disposable> registrar = stack.pop();
			if (registrar instanceof CaptureRegistrar) {
				releaseRegistrar((CaptureRegistrar) registrar);
			}
			return registrar;
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
			for (Cons<Disposable> registrar : stack) {
				if (registrar instanceof CaptureRegistrar && !saved.contains(registrar)) {
					releaseRegistrar((CaptureRegistrar) registrar);
				}
			}
			stack.clear();
			stack.addAll(saved);
		}
	}

	public static void clear() {
		releaseRegistrars(stack);
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

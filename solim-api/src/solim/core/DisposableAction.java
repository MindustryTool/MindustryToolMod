package solim.core;

/**
 * Idempotent Disposable implementation wrapping a single Runnable action.
 */
public final class DisposableAction implements Disposable {
	private final Runnable action;
	private boolean disposed = false;

	public static final DisposableAction EMPTY = new DisposableAction(null);

	public DisposableAction(Runnable action) {
		this.action = action;
	}

	public static DisposableAction of(Runnable action) {
		return action != null ? new DisposableAction(action) : EMPTY;
	}

	public static DisposableAction empty() {
		return EMPTY;
	}

	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		if (action != null) {
			action.run();
		}
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}
}

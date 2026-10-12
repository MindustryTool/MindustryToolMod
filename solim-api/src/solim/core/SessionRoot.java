package solim.core;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Permanent session-lifetime root owner for unparented top-level components.
 *
 * <p>When a component is constructed with an empty ambient {@link OwnershipContext} stack,
 * it is adopted by this root so its {@link Component#owner()} is non-null.
 *
 * <p>To prevent memory leaks for ephemeral top-level widgets (such as tooltips, popups,
 * and closed dialogs), when a component is disposed it unregisters itself via
 * {@link #remove(Component)}.
 */
public final class SessionRoot implements DisposableOwner {

	private static final SessionRoot INSTANCE = new SessionRoot();

	private final List<Component> children = new ArrayList<>();
	private final List<Disposable> disposables = new ArrayList<>();
	private boolean disposed = false;

	private SessionRoot() {
	}

	public static SessionRoot get() {
		return INSTANCE;
	}

	@Override
	public synchronized void adopt(@Nullable Disposable resource) {
		if (resource == null || disposed) {
			return;
		}
		if (resource instanceof Component) {
			Component child = (Component) resource;
			if (!children.contains(child)) {
				children.add(child);
			}
			return;
		}
		if (!disposables.contains(resource)) {
			disposables.add(resource);
		}
	}

	/**
	 * Removes a component from the session root when it is disposed or reparented.
	 */
	public synchronized boolean remove(@Nullable Component component) {
		if (component == null) {
			return false;
		}
		return children.remove(component);
	}

	public synchronized List<Component> children() {
		return Collections.unmodifiableList(new ArrayList<>(children));
	}

	public synchronized List<Disposable> disposables() {
		return Collections.unmodifiableList(new ArrayList<>(disposables));
	}

	public synchronized boolean contains(@Nullable Component component) {
		return component != null && children.contains(component);
	}

	public synchronized void clear() {
		children.clear();
		disposables.clear();
	}

	@Override
	public synchronized void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		Throwable firstFailure = null;
		for (int i = children.size() - 1; i >= 0; i--) {
			try {
				children.get(i).dispose();
			} catch (Throwable t) {
				if (firstFailure == null) {
					firstFailure = t;
				}
			}
		}
		children.clear();
		for (int i = disposables.size() - 1; i >= 0; i--) {
			try {
				disposables.get(i).dispose();
			} catch (Throwable t) {
				if (firstFailure == null) {
					firstFailure = t;
				}
			}
		}
		disposables.clear();
		if (firstFailure != null) {
			if (firstFailure instanceof RuntimeException) {
				throw (RuntimeException) firstFailure;
			}
			if (firstFailure instanceof Error) {
				throw (Error) firstFailure;
			}
			throw new RuntimeException("Error disposing SessionRoot", firstFailure);
		}
	}

	@Override
	public synchronized boolean isDisposed() {
		return disposed;
	}
}

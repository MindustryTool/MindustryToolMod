package solim.core;

/**
 * Observer in the reactive dependency graph (Computed, Effect) notified when a dependency changes.
 */
public interface ReactiveObserver {

	/**
	 * Tracks a dependency observed during computation.
	 *
	 * @param observable the reactive source being observed
	 */
	void addDependency(ReactiveSource observable);

	/**
	 * Called when an observed dependency changes its value.
	 */
	void invalidate();
}

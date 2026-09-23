package solim.runtime;

import arc.util.Log;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import arc.func.Func;
import solim.core.Component;
import solim.core.Disposable;

/**
 * Shared keyed reconciliation manager for structural reactive components.
 *
 * <p>Preserves component identity across updates:
 * <ul>
 *   <li>Existing keys: preserves the existing component instance without rebuilding.</li>
 *   <li>New keys: instantiates a new component in an isolated context and eagerly builds its element.</li>
 *   <li>Removed keys: disposes the removed component cleanly after committing state.</li>
 * </ul>
 *
 * @param <K> the key type identifying each item
 * @param <C> the component type
 */
public final class StructuralReconciler<K, C extends Component> implements Disposable {
	private final Map<K, C> activeComponents = new LinkedHashMap<>();
	private boolean disposed = false;
	private volatile int lastNewCount = 0;
	private volatile int lastReuseCount = 0;
	private volatile int lastTotalCount = 0;

	/**
	 * Reconciles the given items against currently active components with transactional rollback.
	 *
	 * @throws IllegalArgumentException if duplicate keys are detected in the item collection.
	 */
	public <T> Map<K, C> reconcile(
			Iterable<T> items,
			Func<T, K> keyExtractor,
			Func<T, C> factory) {
		final Iterable<T> effectiveItems = items != null ? items : Collections.<T>emptyList();

		// Phase 1: Extract and validate keys (detect duplicates)
		List<T> itemList = new ArrayList<>();
		List<K> keyList = new ArrayList<>();
		Set<K> seenKeys = new HashSet<>();
		for (T item : effectiveItems) {
			K key = keyExtractor.get(item);
			if (!seenKeys.add(key)) {
				throw new IllegalArgumentException("Duplicate key '" + key + "' in reconciler");
			}
			itemList.add(item);
			keyList.add(key);
		}

		// Phase 2: Create / reuse components in isolated context
		Map<K, C> nextComponents = new LinkedHashMap<>();
		List<C> newlyCreated = new ArrayList<>();

		try {
			OwnershipContext.withoutAutoOwnership(() -> {
				for (int i = 0; i < itemList.size(); i++) {
					T item = itemList.get(i);
					K key = keyList.get(i);

					C comp = activeComponents.get(key);
					if (comp == null) {
						comp = ReactiveContext.untracked(() ->
							AttachmentStack.isolate(() -> {
								C c = factory.get(item);
								if (c != null) {
									c.element();
								}
								return c;
							})
						);
						if (comp != null) {
							newlyCreated.add(comp);
						}
					}
					if (comp != null) {
						nextComponents.put(key, comp);
					}
				}
			});
		} catch (Throwable t) {
			// Rollback: dispose only newly created components; preserve activeComponents
			for (C c : newlyCreated) {
				try {
					c.dispose();
				} catch (Throwable ex) {
					Log.err("Error disposing newly created component during reconciliation rollback", ex);
				}
			}
			throw t;
		}

		// Phase 3: Commit next state
		Set<K> removedKeys = new LinkedHashSet<>(activeComponents.keySet());
		removedKeys.removeAll(nextComponents.keySet());

		List<C> toDispose = new ArrayList<>();
		for (K removedKey : removedKeys) {
			C comp = activeComponents.get(removedKey);
			if (comp != null) {
				toDispose.add(comp);
			}
		}

		activeComponents.clear();
		activeComponents.putAll(nextComponents);

		// Phase 4: Dispose removed components (after commit)
		for (C comp : toDispose) {
			try {
				comp.dispose();
			} catch (Throwable t) {
				Log.err("Error disposing component during reconciliation", t);
			}
		}

		lastNewCount = newlyCreated.size();
		lastTotalCount = nextComponents.size();
		lastReuseCount = Math.max(0, lastTotalCount - lastNewCount);

		return activeComponents;
	}

	/** Returns the currently active components map. */
	public Map<K, C> activeComponents() {
		return activeComponents;
	}

	/** Returns whether there are no active components. */
	public boolean isEmpty() {
		return activeComponents.isEmpty();
	}

	/** Number of newly created components in the last reconcile. */
	public int getLastNewCount() {
		return lastNewCount;
	}

	/** Number of reused components in the last reconcile. */
	public int getLastReuseCount() {
		return lastReuseCount;
	}

	/** Total item count in the last reconcile. */
	public int getLastTotalCount() {
		return lastTotalCount;
	}

	/** Disposes all active components and clears the state. */
	@Override
	public void dispose() {
		if (disposed) {
			return;
		}
		disposed = true;
		for (C comp : activeComponents.values()) {
			try {
				comp.dispose();
			} catch (Throwable t) {
				Log.err("Error disposing component in reconciler", t);
			}
		}
		activeComponents.clear();
	}

	@Override
	public boolean isDisposed() {
		return disposed;
	}
}

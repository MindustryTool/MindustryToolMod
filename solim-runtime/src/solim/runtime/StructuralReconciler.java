package solim.runtime;

import arc.util.Log;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import solim.core.Component;
import solim.core.Disposable;

/**
 * Shared keyed reconciliation manager for structural reactive components.
 *
 * <p>Preserves component identity across updates:
 * <ul>
 *   <li>Existing keys: preserves the existing component instance without rebuilding.</li>
 *   <li>New keys: instantiates a new component in an isolated context and eagerly builds its element.</li>
 *   <li>Removed keys: disposes the removed component cleanly.</li>
 * </ul>
 *
 * @param <K> the key type identifying each item
 * @param <C> the component type
 */
public final class StructuralReconciler<K, C extends Component> implements Disposable {
	private final Map<K, C> activeComponents = new LinkedHashMap<>();

	/**
	 * Reconciles the given items against currently active components.
	 */
	public <T> Map<K, C> reconcile(
			Iterable<T> items,
			Function<T, K> keyExtractor,
			Function<T, C> factory) {
		final Iterable<T> effectiveItems = items != null ? items : Collections.<T>emptyList();
		Map<K, C> nextComponents = new LinkedHashMap<>();
		Set<K> currentKeys = new HashSet<>();

		ComponentContext.withoutAutoOwnership(() -> {
			for (T item : effectiveItems) {
				K key = keyExtractor.apply(item);
				currentKeys.add(key);

				C comp = activeComponents.get(key);
				if (comp == null) {
					comp = ReactiveContext.untracked(() ->
						ParentStack.isolate(() -> {
							C c = factory.apply(item);
							if (c != null) {
								c.element();
							}
							return c;
						})
					);
				}
				if (comp != null) {
					nextComponents.put(key, comp);
				}
			}
		});

		// Dispose components no longer present in collection
		for (Map.Entry<K, C> entry : activeComponents.entrySet()) {
			if (!currentKeys.contains(entry.getKey())) {
				try {
					entry.getValue().dispose();
				} catch (Throwable t) {
					Log.err("Error disposing component during reconciliation", t);
				}
			}
		}

		activeComponents.clear();
		activeComponents.putAll(nextComponents);
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

	/** Disposes all active components and clears the state. */
	@Override
	public void dispose() {
		for (C comp : activeComponents.values()) {
			try {
				comp.dispose();
			} catch (Throwable t) {
				Log.err("Error disposing component in reconciler", t);
			}
		}
		activeComponents.clear();
	}
}

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
import solim.instrumentation.Instrumentation;
import solim.instrumentation.ReconcilePass;

public final class StructuralReconciler<K, C extends Component> implements Disposable {
    private final Map<K, C> activeComponents = new LinkedHashMap<>();
    private boolean disposed = false;

    public <T> Map<K, C> reconcile(
            Iterable<T> items,
            Func<T, K> keyExtractor,
            Func<T, C> factory) {
        final Iterable<T> effectiveItems = items != null ? items : Collections.<T>emptyList();

        List<T> itemList = new ArrayList<>();
        List<K> keyList = new ArrayList<>();
        Set<K> seenKeys = new HashSet<>();
        for (T item : effectiveItems) {
            K key = keyExtractor.get(item);
            if (!seenKeys.add(key)) {
                Log.warn("Duplicate key '@' in reconciler. Ignoring duplicate item.", key);
                continue;
            }
            itemList.add(item);
            keyList.add(key);
        }

        Map<K, C> nextComponents = new LinkedHashMap<>();
        List<C> newlyCreated = new ArrayList<>();

        try {
            for (int i = 0; i < itemList.size(); i++) {
                T item = itemList.get(i);
                K key = keyList.get(i);

                C comp = activeComponents.get(key);
                if (comp == null) {
                    comp = create(item, factory);
                    if (comp != null) {
                        newlyCreated.add(comp);
                    }
                }
                if (comp != null) {
                    nextComponents.put(key, comp);
                }
            }
        } catch (Throwable t) {

            for (C c : newlyCreated) {
                try {
                    c.dispose();
                } catch (Throwable ex) {
                    Log.err("Error disposing newly created component during reconciliation rollback", ex);
                }
            }
            throw t;
        }

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

        for (C comp : toDispose) {
            try {
                comp.dispose();
            } catch (Throwable t) {
                Log.err("Error disposing component during reconciliation", t);
            }
        }

        if (Instrumentation.enabled()) {
            int total = nextComponents.size();
            int created = newlyCreated.size();
            Instrumentation.emit(new ReconcilePass(this, created, Math.max(0, total - created), total));
        }

        return activeComponents;
    }

    private <T> C create(T item, Func<T, C> factory) {
        return OwnershipScope.build(() -> factory.get(item));
    }

    public Map<K, C> activeComponents() {
        return activeComponents;
    }

    public boolean isEmpty() {
        return activeComponents.isEmpty();
    }

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

package solim.runtime;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import solim.core.Component;
import solim.core.DebugReactiveNode;
import solim.core.DisposableOwner;
import solim.core.OwnershipContext;
import solim.core.ReactiveSource;
import solim.performance.ComputedSnapshot;
import solim.performance.EffectSnapshot;
import solim.performance.SignalSnapshot;

public class DebugReactiveContext extends ReactiveContext {

    public static final int MAX_TRACKED_NODES = 4096;

    public static final int MAX_SUMMARY_CHARS = 120;

    private final Map<Object, SignalMetrics> signals = new LinkedHashMap<>(64, 0.75f, true);
    private final Map<Object, ComputedMetrics> computeds = new LinkedHashMap<>(64, 0.75f, true);
    private final Map<Object, EffectMetrics> effects = new LinkedHashMap<>(64, 0.75f, true);

    private final Map<Component, Set<Object>> creationOwned = new IdentityHashMap<>();

    private int nextId = 1;

    private static final class SignalMetrics {
        final int id;
        int writeCount;

        SignalMetrics(int id) {
            this.id = id;
        }
    }

    private static final class ComputedMetrics {
        final int id;
        int recomputeCount;
        long totalNs;
        long lastNs;

        ComputedMetrics(int id) {
            this.id = id;
        }
    }

    private static final class EffectMetrics {
        final int id;
        int runCount;
        long totalNs;
        long lastNs;

        EffectMetrics(int id) {
            this.id = id;
        }
    }

    @Override
    protected void onSignalCreated(@Nullable Object signal) {
        if (signal == null) {
            return;
        }
        signalMetrics(signal);
        DisposableOwner owner = OwnershipContext.current();
        if (owner instanceof Component) {
            createdBy((Component) owner, signal);
        }
    }

    @Override
    protected void onSignalWriteStart(@Nullable Object signal, @Nullable Object oldValue,
            @Nullable Object newValue) {
        if (signal == null) {
            return;
        }
        SignalMetrics metrics = signalMetrics(signal);
        if (metrics != null) {
            metrics.writeCount++;
        }
        AttachmentStack.beginSpan(labelOf(signal), "signal");
    }

    @Override
    protected void onSignalWriteEnd(@Nullable Object signal) {
        if (signal != null) {
            AttachmentStack.endSpan();
        }
    }

    @Override
    protected void onComputedStart(@Nullable Object computed) {
        if (computed != null) {
            AttachmentStack.beginSpan(labelOf(computed), "computed");
        }
    }

    @Override
    protected void onComputedEnd(@Nullable Object computed, boolean changed) {
        if (computed != null) {
            AttachmentStack.endSpan();
        }
    }

    @Override
    protected void onEffectStart(@Nullable Object effect) {
        if (effect != null) {
            AttachmentStack.beginSpan(labelOf(effect), "effect");
        }
    }

    @Override
    protected void onEffectEnd(@Nullable Object effect) {
        if (effect != null) {
            AttachmentStack.endSpan();
        }
    }

    public void recordComputedRun(@Nullable Object computed, long durationNs) {
        ComputedMetrics metrics = computedMetrics(computed);
        if (metrics == null) {
            return;
        }
        metrics.recomputeCount++;
        metrics.lastNs = Math.max(0L, durationNs);
        metrics.totalNs += metrics.lastNs;
    }

    public void recordEffectRun(@Nullable Object effect, long durationNs) {
        EffectMetrics metrics = effectMetrics(effect);
        if (metrics == null) {
            return;
        }
        metrics.runCount++;
        metrics.lastNs = Math.max(0L, durationNs);
        metrics.totalNs += metrics.lastNs;
    }

    private void createdBy(Component component, Object node) {
        Set<Object> nodes = creationOwned.get(component);
        if (nodes == null) {
            nodes = newIdentitySet();
            creationOwned.put(component, nodes);
        }
        nodes.add(node);
    }

    public Set<Object> createdNodes(@Nullable Component component) {
        Set<Object> nodes = component != null ? creationOwned.get(component) : null;
        return nodes != null ? nodes : Collections.<Object>emptySet();
    }

    private @Nullable SignalMetrics signalMetrics(@Nullable Object signal) {
        if (signal == null) {
            return null;
        }
        SignalMetrics metrics = signals.get(signal);
        if (metrics == null) {
            if (signals.size() >= MAX_TRACKED_NODES) {
                return null;
            }
            metrics = new SignalMetrics(nextId++);
            signals.put(signal, metrics);
        }
        return metrics;
    }

    private @Nullable ComputedMetrics computedMetrics(@Nullable Object computed) {
        if (computed == null) {
            return null;
        }
        ComputedMetrics metrics = computeds.get(computed);
        if (metrics == null) {
            if (computeds.size() >= MAX_TRACKED_NODES) {
                return null;
            }
            metrics = new ComputedMetrics(nextId++);
            computeds.put(computed, metrics);
        }
        return metrics;
    }

    private @Nullable EffectMetrics effectMetrics(@Nullable Object effect) {
        if (effect == null) {
            return null;
        }
        EffectMetrics metrics = effects.get(effect);
        if (metrics == null) {
            if (effects.size() >= MAX_TRACKED_NODES) {
                return null;
            }
            metrics = new EffectMetrics(nextId++);
            effects.put(effect, metrics);
        }
        return metrics;
    }

    public void observeComputed(@Nullable Object computed) {
        computedMetrics(computed);
    }

    public void observeEffect(@Nullable Object effect) {
        effectMetrics(effect);
    }

    public List<SignalSnapshot> signalSnapshots() {
        List<SignalSnapshot> out = new ArrayList<>(signals.size());
        for (Map.Entry<Object, SignalMetrics> entry : signals.entrySet()) {
            SignalMetrics metrics = entry.getValue();
            out.add(new SignalSnapshot(metrics.id, valueOf(entry.getKey()), metrics.writeCount,
                    observerCount(entry.getKey()), listenerCount(entry.getKey())));
        }
        Collections.sort(out, new Comparator<SignalSnapshot>() {
            @Override
            public int compare(SignalSnapshot a, SignalSnapshot b) {
                return b.writeCount - a.writeCount;
            }
        });
        return out;
    }

    public List<ComputedSnapshot> computedSnapshots() {
        List<ComputedSnapshot> out = new ArrayList<>(computeds.size());
        for (Map.Entry<Object, ComputedMetrics> entry : computeds.entrySet()) {
            ComputedMetrics metrics = entry.getValue();
            out.add(new ComputedSnapshot(metrics.id, valueOf(entry.getKey()), metrics.recomputeCount,
                    metrics.totalNs, metrics.lastNs, isDirty(entry.getKey()),
                    dependencyLabels(entry.getKey())));
        }
        Collections.sort(out, byTotalTime());
        return out;
    }

    public List<EffectSnapshot> effectSnapshots() {
        List<EffectSnapshot> out = new ArrayList<>(effects.size());
        for (Map.Entry<Object, EffectMetrics> entry : effects.entrySet()) {
            EffectMetrics metrics = entry.getValue();
            out.add(new EffectSnapshot(metrics.id, metrics.runCount, metrics.totalNs, metrics.lastNs,
                    dependencyLabels(entry.getKey())));
        }
        Collections.sort(out, new Comparator<EffectSnapshot>() {
            @Override
            public int compare(EffectSnapshot a, EffectSnapshot b) {
                if (a.totalNs != b.totalNs) {
                    return a.totalNs > b.totalNs ? -1 : 1;
                }
                return b.runCount - a.runCount;
            }
        });
        return out;
    }

    private static Comparator<ComputedSnapshot> byTotalTime() {
        return new Comparator<ComputedSnapshot>() {
            @Override
            public int compare(ComputedSnapshot a, ComputedSnapshot b) {
                if (a.totalNs != b.totalNs) {
                    return a.totalNs > b.totalNs ? -1 : 1;
                }
                return b.recomputeCount - a.recomputeCount;
            }
        };
    }

    public boolean hasMetrics() {
        return !signals.isEmpty() || !computeds.isEmpty() || !effects.isEmpty();
    }

    public void clearState() {
        signals.clear();
        computeds.clear();
        effects.clear();
        creationOwned.clear();
        nextId = 1;
    }

    private static @Nullable DebugReactiveNode nodeOf(@Nullable Object node) {
        return node instanceof DebugReactiveNode ? (DebugReactiveNode) node : null;
    }

    private static String valueOf(@Nullable Object node) {
        DebugReactiveNode introspectable = nodeOf(node);
        if (introspectable == null) {
            return Integer.toHexString(System.identityHashCode(node));
        }
        try {
            return truncate(introspectable.debugValue());
        } catch (Throwable ignored) {
            return "<toString failed>";
        }
    }

    private static List<String> dependencyLabels(@Nullable Object node) {
        DebugReactiveNode introspectable = nodeOf(node);
        if (introspectable == null) {
            return Collections.emptyList();
        }
        List<ReactiveSource> deps;
        try {
            deps = introspectable.debugDependencies();
        } catch (Throwable ignored) {
            return Collections.emptyList();
        }
        List<String> labels = new ArrayList<>(deps.size());
        for (ReactiveSource dep : deps) {
            labels.add(labelOf(dep));
        }
        return labels;
    }

    private static int observerCount(@Nullable Object node) {
        DebugReactiveNode introspectable = nodeOf(node);
        if (introspectable == null) {
            return 0;
        }
        try {
            return introspectable.debugObservers().size();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static int listenerCount(@Nullable Object node) {
        DebugReactiveNode introspectable = nodeOf(node);
        if (introspectable == null) {
            return 0;
        }
        try {
            return introspectable.debugListenerCount();
        } catch (Throwable ignored) {
            return 0;
        }
    }

    private static boolean isDirty(@Nullable Object node) {
        DebugReactiveNode introspectable = nodeOf(node);
        if (introspectable == null) {
            return false;
        }
        try {
            return introspectable.debugDirty();
        } catch (Throwable ignored) {
            return false;
        }
    }

    public static String labelOf(@Nullable Object node) {
        if (node == null) {
            return "reactive";
        }
        DebugReactiveNode introspectable = nodeOf(node);
        if (introspectable != null) {
            try {
                String label = introspectable.debugLabel();
                if (label != null && !label.trim().isEmpty()) {
                    return truncate(label);
                }
            } catch (Throwable ignored) {

            }
        }
        return node.getClass().getSimpleName();
    }

    public static String truncate(@Nullable String value) {
        if (value == null) {
            return "";
        }
        return value.length() <= MAX_SUMMARY_CHARS ? value : value.substring(0, MAX_SUMMARY_CHARS) + "…";
    }

    public static Set<Object> reachableSources(@Nullable Object node) {
        Set<Object> visited = newIdentitySet();
        collectSources(node, visited);
        return visited;
    }

    private static void collectSources(@Nullable Object node, Set<Object> visited) {
        if (node == null || !visited.add(node)) {
            return;
        }
        DebugReactiveNode introspectable = nodeOf(node);
        if (introspectable == null) {
            return;
        }
        List<ReactiveSource> deps;
        try {
            deps = introspectable.debugDependencies();
        } catch (Throwable ignored) {
            return;
        }
        for (ReactiveSource dep : deps) {
            collectSources(dep, visited);
        }
    }

    public static Set<Object> newIdentitySet() {
        return Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
    }

    public @Nullable SignalSnapshot signalSnapshot(@Nullable Object signal) {
        SignalMetrics metrics = signal != null ? signals.get(signal) : null;
        return metrics == null ? null
                : new SignalSnapshot(metrics.id, valueOf(signal),
                        metrics.writeCount, observerCount(signal), listenerCount(signal));
    }

    public @Nullable ComputedSnapshot computedSnapshot(@Nullable Object computed) {
        ComputedMetrics metrics = computed != null ? computeds.get(computed) : null;
        return metrics == null ? null
                : new ComputedSnapshot(metrics.id, valueOf(computed),
                        metrics.recomputeCount, metrics.totalNs, metrics.lastNs, isDirty(computed),
                        dependencyLabels(computed));
    }

    public @Nullable EffectSnapshot effectSnapshot(@Nullable Object effect) {
        EffectMetrics metrics = effect != null ? effects.get(effect) : null;
        return metrics == null ? null
                : new EffectSnapshot(metrics.id, metrics.runCount,
                        metrics.totalNs, metrics.lastNs, dependencyLabels(effect));
    }
}

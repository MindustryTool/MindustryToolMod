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

/**
 * Profiling-enabled {@link ReactiveContext} variant recording metrics for every reactive node that
 * runs while it is installed.
 *
 * <p>Two kinds of component association are tracked, because neither alone is sufficient:
 * <ul>
 * <li><b>Creation ownership</b> — a node constructed while {@link OwnershipContext#current()} is a
 * {@link Component} is attributed to that component, capturing signals created directly in
 * {@code build()}.</li>
 * <li><b>Dependency discovery</b> — at snapshot time, each component's owned {@code Computed}s and
 * {@code Effect}s are walked transitively by {@link #reachableSources} so external state driving a
 * component is attributed to it as well.</li>
 * </ul>
 *
 * <p>Maps use access-order {@link LinkedHashMap} over identity semantics, because reactive nodes do
 * not override {@code equals()}: the key is the node instance itself, so ordering is by access for
 * stable hot-first reporting. Every registry is released by {@link #clearState()}, which
 * {@code Perf} calls on disable and at the start of each profiling session.
 */
public class DebugReactiveContext extends ReactiveContext {

	/** Cap on retained nodes per category so a long session cannot grow the registries unbounded. */
	public static final int MAX_TRACKED_NODES = 4096;

	/** Truncation bound for value and label summaries, guarding against large or cyclic values. */
	public static final int MAX_SUMMARY_CHARS = 120;

	private final Map<Object, SignalMetrics> signals = new LinkedHashMap<>(64, 0.75f, true);
	private final Map<Object, ComputedMetrics> computeds = new LinkedHashMap<>(64, 0.75f, true);
	private final Map<Object, EffectMetrics> effects = new LinkedHashMap<>(64, 0.75f, true);

	/** Component → reactive nodes created while that component was the active owner. */
	private final Map<Component, Set<Object>> creationOwned = new IdentityHashMap<>();

	private int nextId = 1;

	// --- per-node metric accumulators -------------------------------------------------------------

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

	// --- lifecycle hooks ---------------------------------------------------------------------------

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

	/**
	 * Records one completed computed evaluation. Called by the primitive itself rather than derived
	 * from {@link #onComputedEnd} so the recorded duration covers only the producer body.
	 */
	public void recordComputedRun(@Nullable Object computed, long durationNs) {
		ComputedMetrics metrics = computedMetrics(computed);
		if (metrics == null) {
			return;
		}
		metrics.recomputeCount++;
		metrics.lastNs = Math.max(0L, durationNs);
		metrics.totalNs += metrics.lastNs;
	}

	/** Records one completed effect execution with its measured body duration. */
	public void recordEffectRun(@Nullable Object effect, long durationNs) {
		EffectMetrics metrics = effectMetrics(effect);
		if (metrics == null) {
			return;
		}
		metrics.runCount++;
		metrics.lastNs = Math.max(0L, durationNs);
		metrics.totalNs += metrics.lastNs;
	}

	// --- creation ownership --------------------------------------------------------------------

	private void createdBy(Component component, Object node) {
		Set<Object> nodes = creationOwned.get(component);
		if (nodes == null) {
			nodes = newIdentitySet();
			creationOwned.put(component, nodes);
		}
		nodes.add(node);
	}

	/** Returns the reactive nodes created while {@code component} was the active owner. */
	public Set<Object> createdNodes(@Nullable Component component) {
		Set<Object> nodes = component != null ? creationOwned.get(component) : null;
		return nodes != null ? nodes : Collections.<Object>emptySet();
	}

	// --- registries ----------------------------------------------------------------------------

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

	/**
	 * Registers a reactive node that was constructed before this context was installed, or whose
	 * creation hook was not reached, so it still appears in a report once it runs.
	 */
	public void observeComputed(@Nullable Object computed) {
		computedMetrics(computed);
	}

	/** Registers an effect that was constructed before this context was installed. */
	public void observeEffect(@Nullable Object effect) {
		effectMetrics(effect);
	}

	// --- snapshots -----------------------------------------------------------------------------

	/** Returns signal snapshots ordered by write count, hot signals first. */
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

	/** Returns computed snapshots ordered by total evaluation time, hottest first. */
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

	/** Returns effect snapshots ordered by total execution time, hottest first. */
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

	/** Returns whether any reactive metrics have been recorded. */
	public boolean hasMetrics() {
		return !signals.isEmpty() || !computeds.isEmpty() || !effects.isEmpty();
	}

	/** Releases every registry so disabling profiling immediately drops all retained nodes. */
	public void clearState() {
		signals.clear();
		computeds.clear();
		effects.clear();
		creationOwned.clear();
		nextId = 1;
	}

	// --- reactive node access --------------------------------------------------------------------

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

	/** Returns a short, stable label for a reactive node, for report display only. */
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
				// fall through to the class name
			}
		}
		return node.getClass().getSimpleName();
	}

	/** Truncates a summary to {@link #MAX_SUMMARY_CHARS}, never returning {@code null}. */
	public static String truncate(@Nullable String value) {
		if (value == null) {
			return "";
		}
		return value.length() <= MAX_SUMMARY_CHARS ? value : value.substring(0, MAX_SUMMARY_CHARS) + "…";
	}

	/**
	 * Walks the dependency graph of a reactive node transitively, returning every distinct node
	 * reachable through it. Cycles are handled by the visited set.
	 */
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

	/** Returns a fresh identity-based set for de-duplicating discovered nodes. */
	public static Set<Object> newIdentitySet() {
		return Collections.newSetFromMap(new IdentityHashMap<Object, Boolean>());
	}

	// --- per-node snapshots, keyed by the node instance -------------------------------------------

	/** Returns the snapshot for a tracked signal, or {@code null} when it was never observed. */
	public @Nullable SignalSnapshot signalSnapshot(@Nullable Object signal) {
		SignalMetrics metrics = signal != null ? signals.get(signal) : null;
		return metrics == null ? null : new SignalSnapshot(metrics.id, valueOf(signal),
				metrics.writeCount, observerCount(signal), listenerCount(signal));
	}

	/** Returns the snapshot for a tracked computed, or {@code null} when it was never observed. */
	public @Nullable ComputedSnapshot computedSnapshot(@Nullable Object computed) {
		ComputedMetrics metrics = computed != null ? computeds.get(computed) : null;
		return metrics == null ? null : new ComputedSnapshot(metrics.id, valueOf(computed),
				metrics.recomputeCount, metrics.totalNs, metrics.lastNs, isDirty(computed),
				dependencyLabels(computed));
	}

	/** Returns the snapshot for a tracked effect, or {@code null} when it was never observed. */
	public @Nullable EffectSnapshot effectSnapshot(@Nullable Object effect) {
		EffectMetrics metrics = effect != null ? effects.get(effect) : null;
		return metrics == null ? null : new EffectSnapshot(metrics.id, metrics.runCount,
				metrics.totalNs, metrics.lastNs, dependencyLabels(effect));
	}
}
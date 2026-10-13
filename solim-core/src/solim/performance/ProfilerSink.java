package solim.performance;

import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
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
import solim.instrumentation.AttachSubtree;
import solim.instrumentation.ComputedRun;
import solim.instrumentation.EffectRun;
import solim.instrumentation.FlushPass;
import solim.instrumentation.InstrumentationEvent;
import solim.instrumentation.InstrumentationSink;
import solim.instrumentation.ReactiveCreated;
import solim.instrumentation.ReconcilePass;
import solim.instrumentation.SignalWrite;
import solim.reactive.Computed;
import solim.reactive.Effect;

public final class ProfilerSink implements InstrumentationSink {

    public static final int MAX_TRACKED_NODES = 4096;
    public static final int MAX_SUMMARY_CHARS = 120;
    public static final int TRACE_CAPACITY = 4096;

    private static final int BUFFER_SIZE = 256;
    private static final int ADOPTION_SCAN_LIMIT = 512;

    private static long nextTraceId = 1L;

    private final Map<Object, SignalMetrics> signals = new LinkedHashMap<>(64, 0.75f, true);
    private final Map<Object, ComputedMetrics> computeds = new LinkedHashMap<>(64, 0.75f, true);
    private final Map<Object, EffectMetrics> effects = new LinkedHashMap<>(64, 0.75f, true);

    private final Map<Component, Set<Object>> creationOwned = new IdentityHashMap<>();

    private final PerfSpan[] buffer = new PerfSpan[BUFFER_SIZE];
    private final SpanRecord[] traceBuffer = new SpanRecord[TRACE_CAPACITY];
    private final Deque<OpenSpan> openStack = new ArrayDeque<>();

    private int head = 0;
    private int totalRecorded = 0;
    private float thresholdMs = 50f;
    private int maxDepthObserved = 0;

    private boolean tracingEnabled = false;
    private long traceId = -1L;
    private int traceHead = 0;
    private long traceTotalAppended = 0L;
    private int nextSpanId = 0;
    private int traceMaxDepth = 0;

    private int lastCreated = 0;
    private int lastReused = 0;
    private int lastTotal = 0;
    private int nextId = 1;

    private static final class OpenSpan {
        final int spanId;
        final int parentId;
        final int attachDepth;
        final long startNs;
        final @Nullable String name;
        final String phase;
        int childCount;

        OpenSpan(int spanId, int parentId, int attachDepth, long startNs, @Nullable String name,
                String phase) {
            this.spanId = spanId;
            this.parentId = parentId;
            this.attachDepth = attachDepth;
            this.startNs = startNs;
            this.name = name;
            this.phase = phase;
            this.childCount = 0;
        }
    }

    private static final class SpanRecord {
        final int spanId;
        int parentId;
        final long startNs;
        final long endNs;
        final String name;
        final String phase;
        int childCount;

        SpanRecord(int spanId, int parentId, long startNs, long endNs, String name, String phase) {
            this.spanId = spanId;
            this.parentId = parentId;
            this.startNs = startNs;
            this.endNs = endNs;
            this.name = name;
            this.phase = phase;
            this.childCount = 0;
        }
    }

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
    public void accept(InstrumentationEvent event) {
        if (event instanceof SignalWrite) {
            onSignalWrite((SignalWrite) event);
        } else if (event instanceof ComputedRun) {
            onComputedRun((ComputedRun) event);
        } else if (event instanceof EffectRun) {
            onEffectRun((EffectRun) event);
        } else if (event instanceof AttachSubtree) {
            onAttach((AttachSubtree) event);
        } else if (event instanceof ReactiveCreated) {
            onReactiveCreated((ReactiveCreated) event);
        } else if (event instanceof ReconcilePass) {
            onReconcile((ReconcilePass) event);
        } else if (event instanceof FlushPass) {
            onFlush((FlushPass) event);
        }
    }

    private void onFlush(FlushPass event) {
        if (!tracingEnabled) {
            return;
        }
        SpanRecord flush = new SpanRecord(nextSpanId++, -1, event.startNs, event.endNs(), "flush", "flush");
        append(flush);
        noteDepth(1);
    }

    private void onReactiveCreated(ReactiveCreated event) {
        observe(event.node);
        DisposableOwner owner = OwnershipContext.current();
        if (owner instanceof Component) {
            createdBy((Component) owner, event.node);
        }
    }

    private void onSignalWrite(SignalWrite event) {
        SignalMetrics metrics = signalMetrics(event.signal);
        if (metrics != null) {
            metrics.writeCount++;
        }
        recordChildSpan(labelOf(event.signal), "signal", event.startNs, event.durationNs());
    }

    private void onComputedRun(ComputedRun event) {
        ComputedMetrics metrics = computedMetrics(event.computed);
        if (metrics != null) {
            metrics.recomputeCount++;
            metrics.lastNs = event.durationNs;
            metrics.totalNs += event.durationNs;
        }
        recordChildSpan(labelOf(event.computed), "computed", event.startNs, event.durationNs);
    }

    private void onEffectRun(EffectRun event) {
        EffectMetrics metrics = effectMetrics(event.effect);
        if (metrics != null) {
            metrics.runCount++;
            metrics.lastNs = event.durationNs;
            metrics.totalNs += event.durationNs;
        }
        recordChildSpan(labelOf(event.effect), "effect", event.startNs, event.durationNs);
    }

    private void onReconcile(ReconcilePass event) {
        lastCreated = event.created;
        lastReused = event.reused;
        lastTotal = event.total;
    }

    private void onAttach(AttachSubtree event) {
        if (event.depth > maxDepthObserved) {
            maxDepthObserved = event.depth;
        }
        if (event.begin) {
            if (!tracingEnabled) {
                return;
            }
            openStack.push(new OpenSpan(nextSpanId++, parentId(), event.depth,
                    event.timestampNs, event.owner, "build"));
            noteDepth(openStack.size());
            return;
        }
        recordSubtree(event);
    }

    private void recordSubtree(AttachSubtree event) {
        float ms = (System.nanoTime() - event.timestampNs) / 1_000_000f;
        String name = event.owner != null ? event.owner : "table";
        if (ms >= thresholdMs) {
            record("subtree", ms,
                    "depth=" + event.depth + " children=" + event.childCount + " name=" + name);
        }
        if (!tracingEnabled) {
            return;
        }
        while (!openStack.isEmpty() && openStack.peek().attachDepth > event.depth) {
            closeOrphan(openStack.pollFirst());
        }
        OpenSpan open = !openStack.isEmpty() && openStack.peek().attachDepth == event.depth
                ? openStack.pollFirst()
                : null;
        append(new SpanRecord(open != null ? open.spanId : nextSpanId++,
                open != null ? open.parentId : parentId(),
                open != null ? open.startNs : event.timestampNs, event.timestampNs, name, "build"));
        countChild(open != null ? open.parentId : parentId());
    }

    private void closeOrphan(OpenSpan open) {
        append(new SpanRecord(open.spanId, open.parentId, open.startNs, System.nanoTime(),
                open.name != null ? open.name : "table", open.phase));
        countChild(open.parentId);
    }

    private void recordChildSpan(String name, String phase, long startNs, long durationNs) {
        if (!tracingEnabled) {
            return;
        }
        long endNs = startNs + durationNs;
        int parentId = parentId();
        append(new SpanRecord(nextSpanId++, parentId, startNs, endNs, name, phase));
        countChild(parentId);
        noteDepth(openStack.size() + 1);
    }

    private void append(SpanRecord record) {
        if (record.parentId < 0) {
            adoptOrphans(record);
        }
        traceBuffer[traceHead] = record;
        traceHead = (traceHead + 1) % TRACE_CAPACITY;
        traceTotalAppended++;
    }

    private void adoptOrphans(SpanRecord outer) {
        int live = (int) Math.min(traceTotalAppended, (long) TRACE_CAPACITY);
        int scan = Math.min(live, ADOPTION_SCAN_LIMIT);
        for (int i = scan - 1; i >= 0; i--) {
            int idx = (traceHead - 1 - i + TRACE_CAPACITY * 2) % TRACE_CAPACITY;
            SpanRecord candidate = traceBuffer[idx];
            if (candidate != null && candidate.parentId < 0 && candidate.spanId != outer.spanId
                    && outer.startNs <= candidate.startNs && candidate.endNs <= outer.endNs) {
                candidate.parentId = outer.spanId;
                outer.childCount++;
            }
        }
    }

    private int parentId() {
        OpenSpan top = openStack.peekFirst();
        return top != null ? top.spanId : -1;
    }

    private void countChild(int parentId) {
        if (parentId < 0) {
            return;
        }
        for (OpenSpan span : openStack) {
            if (span.spanId == parentId) {
                span.childCount++;
                return;
            }
        }
    }

    private void noteDepth(int depth) {
        if (depth > traceMaxDepth) {
            traceMaxDepth = depth;
        }
    }

    public void observe(@Nullable Object node) {
        if (node instanceof Effect) {
            effectMetrics(node);
        } else if (node instanceof Computed) {
            computedMetrics(node);
        } else if (node != null) {
            signalMetrics(node);
        }
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
        Collections.sort(out, new Comparator<ComputedSnapshot>() {
            @Override
            public int compare(ComputedSnapshot a, ComputedSnapshot b) {
                if (a.totalNs != b.totalNs) {
                    return a.totalNs > b.totalNs ? -1 : 1;
                }
                return b.recomputeCount - a.recomputeCount;
            }
        });
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

    public boolean hasMetrics() {
        return !signals.isEmpty() || !computeds.isEmpty() || !effects.isEmpty();
    }

    public int lastCreatedCount() {
        return lastCreated;
    }

    public int lastReusedCount() {
        return lastReused;
    }

    public int lastTotalCount() {
        return lastTotal;
    }

    @Override
    public synchronized void reset() {
        head = 0;
        totalRecorded = 0;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer[i] = null;
        }
        maxDepthObserved = 0;
        clearReactiveState();
    }

    public void clearReactiveState() {
        signals.clear();
        computeds.clear();
        effects.clear();
        creationOwned.clear();
        nextId = 1;
    }

    public synchronized List<PerfSpan> snapshot(int max) {
        List<PerfSpan> out = new ArrayList<>();
        int n = Math.min(Math.max(0, max), totalRecorded);
        for (int i = 0; i < n; i++) {
            int idx = (head - 1 - i + BUFFER_SIZE) % BUFFER_SIZE;
            PerfSpan span = buffer[idx];
            if (span != null) {
                out.add(span);
            }
        }
        return out;
    }

    public synchronized int totalRecorded() {
        return totalRecorded;
    }

    public int maxDepthObserved() {
        return Math.max(maxDepthObserved, traceMaxDepth);
    }

    public synchronized void setThreshold(float ms) {
        thresholdMs = Math.max(0f, ms);
    }

    public boolean isTracing() {
        return tracingEnabled;
    }

    public void setTracingEnabled(boolean enabled) {
        tracingEnabled = enabled;
        traceId = enabled ? takeTraceId() : -1L;
        clearTrace();
    }

    public void traceReset() {
        clearTrace();
    }

    private void clearTrace() {
        openStack.clear();
        traceHead = 0;
        traceTotalAppended = 0L;
        nextSpanId = 0;
        traceMaxDepth = 0;
        for (int i = 0; i < TRACE_CAPACITY; i++) {
            traceBuffer[i] = null;
        }
    }

    public int traceCapacity() {
        return TRACE_CAPACITY;
    }

    public long traceId() {
        return tracingEnabled ? traceId : -1L;
    }

    public synchronized int traceTotal() {
        return (int) Math.min(traceTotalAppended, (long) Integer.MAX_VALUE);
    }

    public synchronized int traceDroppedCount() {
        long dropped = traceTotalAppended - TRACE_CAPACITY;
        return dropped > 0 ? (int) Math.min(dropped, (long) Integer.MAX_VALUE) : 0;
    }

    public synchronized List<TraceSpan> traceSnapshot(int max) {
        int live = (int) Math.min(traceTotalAppended, (long) TRACE_CAPACITY);
        int n = Math.min(Math.max(0, max), live);
        if (n <= 0) {
            return new ArrayList<>();
        }
        int oldest = (int) ((traceHead - live + TRACE_CAPACITY * 2L) % TRACE_CAPACITY);
        int skip = live - n;

        Map<Integer, SpanRecord> byId = new HashMap<>(live * 2);
        for (int i = 0; i < live; i++) {
            SpanRecord record = traceBuffer[(oldest + i) % TRACE_CAPACITY];
            if (record != null) {
                byId.put(record.spanId, record);
            }
        }

        Map<SpanRecord, Integer> depths = new IdentityHashMap<>();
        List<TraceSpan> out = new ArrayList<>(n);
        for (int i = skip; i < live; i++) {
            SpanRecord record = traceBuffer[(oldest + i) % TRACE_CAPACITY];
            if (record == null) {
                continue;
            }
            SpanRecord parent = record.parentId < 0 ? null : byId.get(record.parentId);
            out.add(new TraceSpan(traceId, record.spanId, record.parentId, record.name, record.phase,
                    record.startNs, record.endNs, depthOf(record, parent, byId, depths), record.childCount));
        }
        Collections.sort(out, new Comparator<TraceSpan>() {
            @Override
            public int compare(TraceSpan a, TraceSpan b) {
                if (a == b) {
                    return 0;
                }
                if (a == null) {
                    return 1;
                }
                if (b == null) {
                    return -1;
                }
                if (a.spanId < b.spanId) {
                    return -1;
                }
                return a.spanId > b.spanId ? 1 : 0;
            }
        });
        return out;
    }

    private static int depthOf(SpanRecord record, @Nullable SpanRecord parent,
            Map<Integer, SpanRecord> byId, Map<SpanRecord, Integer> depths) {
        Integer cached = depths.get(record);
        if (cached != null) {
            return cached;
        }
        int depth = parent == null || parent == record ? 0
                : depthOf(parent,
                        parent.parentId < 0 ? null : byId.get(parent.parentId), byId, depths) + 1;
        depths.put(record, depth);
        return depth;
    }

    private synchronized void record(String phase, float durationMs, String detail) {
        PerfSpan span = new PerfSpan("AttachmentStack", phase, durationMs, System.currentTimeMillis(), detail);
        buffer[head] = span;
        head = (head + 1) % BUFFER_SIZE;
        if (totalRecorded < BUFFER_SIZE) {
            totalRecorded++;
        }
        Log.warn("[Solim] Slow component: @", span.toString());
    }

    private static synchronized long takeTraceId() {
        long id = nextTraceId;
        nextTraceId++;
        return id;
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
}

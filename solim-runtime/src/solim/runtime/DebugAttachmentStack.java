package solim.runtime;

import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.List;
import arc.func.Prov;
import solim.core.Component;
import solim.performance.PerfSpan;
import solim.performance.TraceSpan;

/**
 * Profiling-enabled {@link AttachmentStack} variant. Records threshold-gated slow spans
 * and, when tracing is enabled, full hierarchical flame spans with no threshold.
 *
 * <p>Push timestamps and open flame spans are kept in parallel deques so the base
 * class carries no profiling state. Every mutator that bypasses {@link #doPush} /
 * {@link #doPop} ({@link #doIsolate}, {@link #doCapture}, {@link #doClear}) is
 * overridden to keep both deques in lockstep with the stack.
 */
public class DebugAttachmentStack extends AttachmentStack {

    private static final int BUFFER_SIZE = 256;
    public static final int TRACE_CAPACITY = 4096;

    private static long nextTraceId = 1L;

    private static synchronized long takeTraceId() {
        long id = nextTraceId;
        nextTraceId++;
        return id;
    }

    private static final class OpenSpan {
        final int spanId;
        final int parentId;
        final int depth;
        final long startNs;
        final String pushName;
        int childCount;

        OpenSpan(int spanId, int parentId, int depth, long startNs, String pushName) {
            this.spanId = spanId;
            this.parentId = parentId;
            this.depth = depth;
            this.startNs = startNs;
            this.pushName = pushName;
            this.childCount = 0;
        }
    }

    private final Deque<Long> pushTimes = new ArrayDeque<>();
    private final PerfSpan[] buffer = new PerfSpan[BUFFER_SIZE];
    private int head = 0;
    private int totalRecorded = 0;
    private float thresholdMs = 50f;
    private int maxDepthObserved = 0;

    private boolean tracingEnabled = false;
    private long traceId = -1L;
    private final Deque<OpenSpan> openStack = new ArrayDeque<>();
    private final TraceSpan[] traceBuffer = new TraceSpan[TRACE_CAPACITY];
    private int traceHead = 0;
    private long traceTotalAppended = 0L;
    private int nextSpanId = 0;
    private int traceMaxDepth = 0;

    public DebugAttachmentStack() {
    }

    @Override
    protected void doPush(Table parent, Attacher attacher) {
        super.doPush(parent, attacher);
        if (parent != null) {
            pushTimes.push(System.nanoTime());
            int depth = doSize();
            if (depth > maxDepthObserved) {
                maxDepthObserved = depth;
            }
            if (tracingEnabled) {
                int parentId = openStack.isEmpty() ? -1 : openStack.peekFirst().spanId;
                OpenSpan open = new OpenSpan(nextSpanId++, parentId, openStack.size(),
                        System.nanoTime(), parent.name);
                openStack.push(open);
                int openDepth = openStack.size();
                if (openDepth > traceMaxDepth) {
                    traceMaxDepth = openDepth;
                }
            }
        }
    }

    @Override
    protected Table doPop() {
        Long pushTime = pushTimes.pollFirst();
        OpenSpan open = tracingEnabled ? openStack.peekFirst() : null;
        Table popped = super.doPop();
        if (pushTime != null && popped != null) {
            float ms = (System.nanoTime() - pushTime) / 1_000_000f;
            if (ms >= thresholdMs) {
                int depth = doSize() + 1;
                String name = popped.name != null ? popped.name : "table";
                record("subtree", ms,
                        "depth=" + depth + " children=" + popped.getChildren().size + " name=" + name);
            }
        }
        if (tracingEnabled && open != null && popped != null) {
            openStack.pollFirst();
            long endNs = System.nanoTime();
            String name = popped.name != null ? popped.name
                    : open.pushName != null ? open.pushName : "table";
            recordTrace(new TraceSpan(traceId, open.spanId, open.parentId, name, "build",
                    open.startNs, endNs, open.depth, open.childCount));
            OpenSpan parent = openStack.peekFirst();
            if (parent != null) {
                parent.childCount++;
            }
        }
        return popped;
    }

    @Override
    protected void doAttachPendingComponents(Entry entry) {
        if (entry == null || entry.pendingComponents.isEmpty()) {
            return;
        }
        int childCount = entry.pendingComponents.size();
        long t0 = System.nanoTime();
        super.doAttachPendingComponents(entry);
        float ms = (System.nanoTime() - t0) / 1_000_000f;
        if (ms >= thresholdMs) {
            String name = entry.table != null && entry.table.name != null ? entry.table.name : "table";
            record("attach", ms, "children=" + childCount + " parent=" + name);
        }
    }

    @Override
    protected <T> T doIsolate(Prov<T> Prov) {
        if (Prov == null) {
            return null;
        }
        Deque<Long> savedTimes = new ArrayDeque<>(pushTimes);
        pushTimes.clear();
        Deque<OpenSpan> savedOpen = tracingEnabled ? new ArrayDeque<>(openStack) : null;
        if (savedOpen != null) {
            openStack.clear();
        }
        try {
            return super.doIsolate(Prov);
        } finally {
            pushTimes.clear();
            pushTimes.addAll(savedTimes);
            if (savedOpen != null) {
                openStack.clear();
                openStack.addAll(savedOpen);
            }
        }
    }

    @Override
    protected void doIsolate(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        Deque<Long> savedTimes = new ArrayDeque<>(pushTimes);
        pushTimes.clear();
        Deque<OpenSpan> savedOpen = tracingEnabled ? new ArrayDeque<>(openStack) : null;
        if (savedOpen != null) {
            openStack.clear();
        }
        try {
            super.doIsolate(runnable);
        } finally {
            pushTimes.clear();
            pushTimes.addAll(savedTimes);
            if (savedOpen != null) {
                openStack.clear();
                openStack.addAll(savedOpen);
            }
        }
    }

    @Override
    protected void doCapture(Runnable runnable, List<Component> captured) {
        Deque<OpenSpan> savedOpen = tracingEnabled && !openStack.isEmpty()
                ? new ArrayDeque<>(openStack)
                : null;
        if (tracingEnabled) {
            openStack.clear();
        }
        try {
            super.doCapture(runnable, captured);
        } finally {
            if (tracingEnabled) {
                openStack.clear();
                if (savedOpen != null) {
                    openStack.addAll(savedOpen);
                }
            }
        }
    }

    @Override
    protected void doClear() {
        super.doClear();
        pushTimes.clear();
        openStack.clear();
    }

    @Override
    protected synchronized List<PerfSpan> doSnapshot(int max) {
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

    @Override
    protected synchronized int doTotalRecorded() {
        return totalRecorded;
    }

    @Override
    protected synchronized void doReset() {
        head = 0;
        totalRecorded = 0;
        for (int i = 0; i < BUFFER_SIZE; i++) {
            buffer[i] = null;
        }
        maxDepthObserved = 0;
    }

    @Override
    protected void doSetThreshold(float ms) {
        thresholdMs = Math.max(0f, ms);
    }

    @Override
    protected int doMaxDepthObserved() {
        return maxDepthObserved > traceMaxDepth ? maxDepthObserved : traceMaxDepth;
    }

    @Override
    protected boolean doIsTracing() {
        return tracingEnabled;
    }

    @Override
    protected void doSetTracingEnabled(boolean enabled) {
        if (enabled) {
            tracingEnabled = true;
            traceId = takeTraceId();
            openStack.clear();
            traceHead = 0;
            traceTotalAppended = 0L;
            nextSpanId = 0;
            traceMaxDepth = 0;
            for (int i = 0; i < TRACE_CAPACITY; i++) {
                traceBuffer[i] = null;
            }
        } else {
            tracingEnabled = false;
            traceId = -1L;
            openStack.clear();
            traceHead = 0;
            traceTotalAppended = 0L;
            nextSpanId = 0;
            traceMaxDepth = 0;
            for (int i = 0; i < TRACE_CAPACITY; i++) {
                traceBuffer[i] = null;
            }
        }
    }

    @Override
    protected void doTraceLeaf(@Nullable String name, @Nullable String phase, long startNs, long endNs) {
        if (!tracingEnabled) {
            return;
        }
        int parentId = openStack.isEmpty() ? -1 : openStack.peekFirst().spanId;
        int depth = openStack.size();
        String safeName = name != null && !name.trim().isEmpty() ? name : "Component-element";
        String safePhase = phase != null && !phase.trim().isEmpty() ? phase : "build";
        recordTrace(new TraceSpan(traceId, nextSpanId++, parentId, safeName, safePhase,
                startNs, endNs, depth, 0));
        OpenSpan parent = openStack.peekFirst();
        if (parent != null) {
            parent.childCount++;
        }
        if (depth + 1 > traceMaxDepth) {
            traceMaxDepth = depth + 1;
        }
    }

    @Override
    protected synchronized List<TraceSpan> doTraceSnapshot(int max) {
        List<TraceSpan> out = new ArrayList<>();
        int retained = (int) Math.min(traceTotalAppended, (long) TRACE_CAPACITY);
        int n = Math.min(Math.max(0, max), retained);
        if (n <= 0) {
            return out;
        }
        int oldest = (int) ((traceHead - retained + TRACE_CAPACITY * 2L) % TRACE_CAPACITY);
        int skip = retained - n;
        for (int i = skip; i < retained; i++) {
            int idx = (oldest + i) % TRACE_CAPACITY;
            TraceSpan span = traceBuffer[idx];
            if (span != null) {
                out.add(span);
            }
        }
        sortBySpanId(out);
        return out;
    }

    @Override
    protected synchronized int doTraceDroppedCount() {
        long dropped = traceTotalAppended - (long) TRACE_CAPACITY;
        return dropped > 0 ? (int) Math.min(dropped, (long) Integer.MAX_VALUE) : 0;
    }

    @Override
    protected long doTraceId() {
        return tracingEnabled ? traceId : -1L;
    }

    @Override
    protected int doTraceCapacity() {
        return TRACE_CAPACITY;
    }

    @Override
    protected synchronized int doTraceTotal() {
        return (int) Math.min(traceTotalAppended, (long) Integer.MAX_VALUE);
    }

    @Override
    protected synchronized void doTraceReset() {
        traceHead = 0;
        traceTotalAppended = 0L;
        nextSpanId = 0;
        traceMaxDepth = openStack.isEmpty() ? 0 : openStack.size();
        for (int i = 0; i < TRACE_CAPACITY; i++) {
            traceBuffer[i] = null;
        }
    }

    private void sortBySpanId(List<TraceSpan> out) {
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

    private synchronized void recordTrace(TraceSpan span) {
        if (span == null) {
            return;
        }
        traceBuffer[traceHead] = span;
        traceHead = (traceHead + 1) % TRACE_CAPACITY;
        traceTotalAppended++;
    }
}

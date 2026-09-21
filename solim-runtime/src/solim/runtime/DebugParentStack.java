package solim.runtime;

import arc.scene.ui.layout.Table;
import arc.util.Log;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import arc.func.Prov;
import solim.performance.PerfSpan;

/**
 * Profiling-enabled {@link ParentStack} variant. Records threshold-gated structural spans for
 * container subtree completion and pending-component attach batches, and tracks the maximum
 * observed stack depth.
 *
 * <p>Push timestamps are kept in a parallel deque so the base class carries no profiling state.
 * Every mutator that bypasses {@link #doPush} / {@link #doPop} ({@link #doIsolate} and
 * {@link #doClear}) is overridden to keep that deque in lockstep with the stack.
 */
public final class DebugParentStack extends ParentStack {

    private static final int BUFFER_SIZE = 256;

    private final Deque<Long> pushTimes = new ArrayDeque<>();
    private final PerfSpan[] buffer = new PerfSpan[BUFFER_SIZE];
    private int head = 0;
    private int totalRecorded = 0;
    private float thresholdMs = 50f;
    private int maxDepthObserved = 0;

    public DebugParentStack() {
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
        }
    }

    @Override
    protected Table doPop() {
        Long pushTime = pushTimes.pollFirst();
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
        Deque<Long> saved = new ArrayDeque<>(pushTimes);
        pushTimes.clear();
        try {
            return super.doIsolate(Prov);
        } finally {
            pushTimes.clear();
            pushTimes.addAll(saved);
        }
    }

    @Override
    protected void doIsolate(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        Deque<Long> saved = new ArrayDeque<>(pushTimes);
        pushTimes.clear();
        try {
            super.doIsolate(runnable);
        } finally {
            pushTimes.clear();
            pushTimes.addAll(saved);
        }
    }

    @Override
    protected void doClear() {
        super.doClear();
        pushTimes.clear();
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
        return maxDepthObserved;
    }

    private synchronized void record(String phase, float durationMs, String detail) {
        PerfSpan span = new PerfSpan("ParentStack", phase, durationMs, System.currentTimeMillis(), detail);
        buffer[head] = span;
        head = (head + 1) % BUFFER_SIZE;
        if (totalRecorded < BUFFER_SIZE) {
            totalRecorded++;
        }
        Log.warn("[Solim] Slow component: @", span.toString());
    }
}

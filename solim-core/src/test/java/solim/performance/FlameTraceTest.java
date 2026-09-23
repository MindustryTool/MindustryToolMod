package solim.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.ui.layout.Table;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.runtime.AttachmentStack;
import solim.test.SolimEnv;
import solim.test.TestComponent;

class FlameTraceTest extends SolimEnv {

    @AfterEach
    void tearDown() {
        try {
            Perf.setTracingEnabled(false);
        } catch (Throwable ignored) {
        }
        Perf.setEnabled(false);
        Perf.setThreshold(50f);
        AttachmentStack.clear();
        AttachmentStack.install(null);
        AttachmentStack.reset();
        AttachmentStack.traceReset();
    }

    @Test
    void nestedBuildRecordsParentChildInDocumentOrderWithLeaf() {
        Perf.setTracingEnabled(true);
        Perf.traceReset();
        assertTrue(Perf.isTracing());

        Table outer = new Table();
        outer.name = "outer";
        Table inner = new Table();
        inner.name = "inner";

        AttachmentStack.push(outer);
        AttachmentStack.push(inner);
        TestComponent leaf = new TestComponent("leaf-1");
        leaf.element();
        AttachmentStack.pop();
        AttachmentStack.pop();

        List<TraceSpan> spans = Perf.traceSnapshot(100);
        assertEquals(3, spans.size());

        Map<Integer, TraceSpan> byId = new HashMap<>();
        for (TraceSpan span : spans) {
            byId.put(span.spanId, span);
        }

        TraceSpan outerSpan = null;
        TraceSpan innerSpan = null;
        TraceSpan leafSpan = null;
        for (TraceSpan span : spans) {
            if ("outer".equals(span.name)) {
                outerSpan = span;
            } else if ("inner".equals(span.name)) {
                innerSpan = span;
            } else if ("leaf-1".equals(span.name)) {
                leafSpan = span;
            }
        }

        assertTrue(outerSpan != null, "outer container span must be recorded");
        assertTrue(innerSpan != null, "inner container span must be recorded");
        assertTrue(leafSpan != null, "leaf build span must be recorded");

        assertEquals(-1, outerSpan.parentId);
        assertEquals(0, outerSpan.depth);
        assertEquals(outerSpan.spanId, innerSpan.parentId);
        assertEquals(1, innerSpan.depth);
        assertEquals(innerSpan.spanId, leafSpan.parentId);
        assertEquals(0, leafSpan.childCount);
        assertTrue(leafSpan.endNs >= leafSpan.startNs);
        assertTrue(innerSpan.childCount >= 1);
        assertTrue(outerSpan.childCount >= 1);

        assertTrue(spans.get(0).startNs <= spans.get(1).startNs);
        assertTrue(spans.get(1).startNs <= spans.get(2).startNs);
        assertEquals("outer", spans.get(0).name);

        leaf.dispose();
    }

    @Test
    void isolateAndCapturePreserveParentChains() {
        Perf.setTracingEnabled(true);
        Perf.traceReset();

        Table outer = new Table();
        outer.name = "outer";
        AttachmentStack.push(outer);
        AttachmentStack.isolate(() -> {
            Table inner = new Table();
            inner.name = "isolated-inner";
            AttachmentStack.push(inner);
            AttachmentStack.pop();
        });
        AttachmentStack.pop();

        List<TraceSpan> spans = Perf.traceSnapshot(100);
        assertEquals(2, spans.size());

        TraceSpan outerSpan = null;
        TraceSpan innerSpan = null;
        for (TraceSpan span : spans) {
            if ("outer".equals(span.name)) {
                outerSpan = span;
            } else if ("isolated-inner".equals(span.name)) {
                innerSpan = span;
            }
        }

        assertTrue(outerSpan != null);
        assertTrue(innerSpan != null);
        assertEquals(-1, outerSpan.parentId);
        assertEquals(-1, innerSpan.parentId);

        Perf.traceReset();
        Table captureOuter = new Table();
        captureOuter.name = "capture-outer";
        AttachmentStack.push(captureOuter);
        List<solim.core.Component> captured = AttachmentStack.capture(() -> {
            Table inner = new Table();
            inner.name = "captured-inner";
            AttachmentStack.push(inner);
            AttachmentStack.pop();
        });
        AttachmentStack.pop();
        for (solim.core.Component c : captured) {
            c.dispose();
        }

        List<TraceSpan> after = Perf.traceSnapshot(100);
        assertEquals(2, after.size());
        TraceSpan capOuter = null;
        TraceSpan capInner = null;
        for (TraceSpan span : after) {
            if ("capture-outer".equals(span.name)) {
                capOuter = span;
            } else if ("captured-inner".equals(span.name)) {
                capInner = span;
            }
        }
        assertTrue(capOuter != null);
        assertTrue(capInner != null);
        assertEquals(-1, capOuter.parentId);
        assertEquals(-1, capInner.parentId);
    }

    @Test
    void disabledByDefaultRecordsNothingWithBaseSingleton() {
        Perf.setTracingEnabled(false);
        Perf.setEnabled(false);
        AttachmentStack.install(null);

        assertFalse(Perf.isTracing());
        assertFalse(Perf.isEnabled());
        assertTrue(Perf.traceSnapshot(10).isEmpty());
        assertEquals(0, Perf.traceDroppedCount());
        assertEquals(AttachmentStack.class, AttachmentStack.instance().getClass());
        assertEquals(0, AttachmentStack.traceCapacity());

        Table table = new Table();
        AttachmentStack.push(table);
        AttachmentStack.pop();
        TestComponent leaf = new TestComponent("disabled-leaf");
        leaf.element();
        leaf.dispose();

        assertTrue(Perf.traceSnapshot(10).isEmpty());
        assertEquals(0, Perf.traceDroppedCount());
        assertEquals(AttachmentStack.class, AttachmentStack.instance().getClass());
    }

    @Test
    void overflowEvictsOldestAndReportsDropped() {
        Perf.setTracingEnabled(true);
        Perf.traceReset();
        assertEquals(4096, Perf.traceCapacity());

        int total = 5000;
        for (int i = 0; i < total; i++) {
            Table table = new Table();
            table.name = "t-" + i;
            AttachmentStack.push(table);
            AttachmentStack.pop();
        }

        assertEquals(total, Perf.traceTotal());
        assertEquals(total - 4096, Perf.traceDroppedCount());
        List<TraceSpan> spans = Perf.traceSnapshot(5000);
        assertEquals(4096, spans.size());
        assertEquals(total - 4096, spans.get(0).spanId);
        assertEquals(total - 1, spans.get(spans.size() - 1).spanId);
    }
}

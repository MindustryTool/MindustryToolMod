package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.ui.layout.Table;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.performance.PerfSpan;

class DebugParentStackTest {

    @AfterEach
    void cleanUp() {
        ParentStack.clear();
        ParentStack.install(null);
        ParentStack.reset();
    }

    private static void pushNamed(String name) {
        Table table = new Table();
        table.name = name;
        ParentStack.push(table);
        ParentStack.pop();
    }

    private static String nameOf(PerfSpan span) {
        if (span.detail == null) {
            return null;
        }
        int idx = span.detail.indexOf("name=");
        return idx < 0 ? null : span.detail.substring(idx + 5);
    }

    @Test
    void disabledBaseRecordsNothing() {
        ParentStack.install(null);
        ParentStack.push(new Table());
        ParentStack.pop();
        assertEquals(0, ParentStack.totalRecorded());
        assertTrue(ParentStack.snapshot(10).isEmpty());
    }

    @Test
    void thresholdSuppressesFastSpans() {
        ParentStack.install(new DebugParentStack());
        ParentStack.setThreshold(10_000f);
        pushNamed("fast");
        assertTrue(ParentStack.snapshot(10).isEmpty());
    }

    @Test
    void bufferBoundsAndSnapshotIsNewestFirst() {
        ParentStack.install(new DebugParentStack());
        ParentStack.setThreshold(0f);
        for (int i = 0; i < 300; i++) {
            pushNamed("t-" + i);
        }
        assertEquals(256, ParentStack.totalRecorded());
        List<PerfSpan> spans = ParentStack.snapshot(256);
        assertEquals(256, spans.size());
        assertEquals("t-299", nameOf(spans.get(0)));
        assertEquals("t-44", nameOf(spans.get(255)));
    }

    @Test
    void disablingClearsBuffer() {
        ParentStack.install(new DebugParentStack());
        ParentStack.setThreshold(0f);
        pushNamed("card");
        assertEquals(1, ParentStack.totalRecorded());
        ParentStack.install(null);
        assertEquals(0, ParentStack.totalRecorded());
        assertTrue(ParentStack.snapshot(10).isEmpty());
    }

    @Test
    void isolateRestoresTimingAfterThrow() {
        ParentStack.install(new DebugParentStack());
        ParentStack.setThreshold(0f);

        Table outer = new Table();
        outer.name = "outer";
        ParentStack.push(outer);
        try {
            ParentStack.isolate(() -> {
                ParentStack.push(new Table());
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException ignored) {
        }
        ParentStack.pop();

        boolean found = false;
        for (PerfSpan span : ParentStack.snapshot(10)) {
            if ("subtree".equals(span.phase) && "outer".equals(nameOf(span))) {
                found = true;
            }
        }
        assertTrue(found, "outer subtree span must survive an aborted isolate");
    }
}
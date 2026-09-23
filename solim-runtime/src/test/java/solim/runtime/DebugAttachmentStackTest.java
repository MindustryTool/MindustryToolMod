package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.ui.layout.Table;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.performance.PerfSpan;

class DebugAttachmentStackTest {

    @AfterEach
    void cleanUp() {
        AttachmentStack.clear();
        AttachmentStack.install(null);
        AttachmentStack.reset();
    }

    private static void pushNamed(String name) {
        Table table = new Table();
        table.name = name;
        AttachmentStack.push(table);
        AttachmentStack.pop();
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
        AttachmentStack.install(null);
        AttachmentStack.push(new Table());
        AttachmentStack.pop();
        assertEquals(0, AttachmentStack.totalRecorded());
        assertTrue(AttachmentStack.snapshot(10).isEmpty());
    }

    @Test
    void thresholdSuppressesFastSpans() {
        AttachmentStack.install(new DebugAttachmentStack());
        AttachmentStack.setThreshold(10_000f);
        pushNamed("fast");
        assertTrue(AttachmentStack.snapshot(10).isEmpty());
    }

    @Test
    void bufferBoundsAndSnapshotIsNewestFirst() {
        AttachmentStack.install(new DebugAttachmentStack());
        AttachmentStack.setThreshold(0f);
        for (int i = 0; i < 300; i++) {
            pushNamed("t-" + i);
        }
        assertEquals(256, AttachmentStack.totalRecorded());
        List<PerfSpan> spans = AttachmentStack.snapshot(256);
        assertEquals(256, spans.size());
        assertEquals("t-299", nameOf(spans.get(0)));
        assertEquals("t-44", nameOf(spans.get(255)));
    }

    @Test
    void disablingClearsBuffer() {
        AttachmentStack.install(new DebugAttachmentStack());
        AttachmentStack.setThreshold(0f);
        pushNamed("card");
        assertEquals(1, AttachmentStack.totalRecorded());
        AttachmentStack.install(null);
        assertEquals(0, AttachmentStack.totalRecorded());
        assertTrue(AttachmentStack.snapshot(10).isEmpty());
    }

    @Test
    void isolateRestoresTimingAfterThrow() {
        AttachmentStack.install(new DebugAttachmentStack());
        AttachmentStack.setThreshold(0f);

        Table outer = new Table();
        outer.name = "outer";
        AttachmentStack.push(outer);
        try {
            AttachmentStack.isolate(() -> {
                AttachmentStack.push(new Table());
                throw new IllegalStateException("boom");
            });
        } catch (IllegalStateException ignored) {
        }
        AttachmentStack.pop();

        boolean found = false;
        for (PerfSpan span : AttachmentStack.snapshot(10)) {
            if ("subtree".equals(span.phase) && "outer".equals(nameOf(span))) {
                found = true;
            }
        }
        assertTrue(found, "outer subtree span must survive an aborted isolate");
    }
}
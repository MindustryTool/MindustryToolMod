package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.instrumentation.AttachSubtree;
import solim.instrumentation.Instrumentation;
import solim.instrumentation.InstrumentationEvent;
import solim.instrumentation.InstrumentationSink;

class AttachmentStackPerfTest {

    private static final class RecordingSink implements InstrumentationSink {

        private int opened;
        private int closed;
        private AttachSubtree lastClosed;

        @Override
        public void accept(InstrumentationEvent event) {
            if (!(event instanceof AttachSubtree)) {
                return;
            }
            AttachSubtree attach = (AttachSubtree) event;
            if (attach.begin) {
                opened++;
            } else {
                closed++;
                lastClosed = attach;
            }
        }

        @Override
        public void reset() {
        }
    }

    private RecordingSink sink;

    @BeforeEach
    void installSink() {
        sink = new RecordingSink();
        Instrumentation.install(sink);
    }

    @AfterEach
    void cleanUp() {
        AttachmentStack.clear();
        Instrumentation.uninstall();
    }

    @Test
    void enabledFlagTracksSinkInstallation() {
        assertTrue(Instrumentation.enabled());

        Instrumentation.uninstall();

        assertFalse(Instrumentation.enabled());
    }

    @Test
    void popEmitsSubtreeEventCarryingDepthAndChildCount() {
        Table root = new Table();
        root.name = "root";
        AttachmentStack.push(root);
        AttachmentStack.add(new Element());
        AttachmentStack.pop();

        assertEquals(1, sink.opened);
        assertEquals(1, sink.closed);
        assertEquals("root", sink.lastClosed.owner);
        assertEquals(1, sink.lastClosed.depth);
        assertEquals(1, sink.lastClosed.childCount);
    }

    @Test
    void nestedScopesReportDescendingDepth() {
        AttachmentStack.push(new Table());
        AttachmentStack.push(new Table());
        AttachmentStack.pop();
        AttachmentStack.pop();

        assertEquals(2, sink.opened);
        assertEquals(2, sink.closed);
    }

    @Test
    void isolateTruncatesUnbalancedPushes() {
        Table outer = new Table();
        AttachmentStack.push(outer);

        AttachmentStack.isolate(() -> AttachmentStack.push(new Table()));

        assertEquals(1, AttachmentStack.size(),
                "a block that pushes without popping must be truncated back to the entry depth");
        assertEquals(outer, AttachmentStack.current());
        AttachmentStack.pop();
    }

    @Test
    void noEmissionWhenDisabled() {
        Instrumentation.uninstall();
        sink.opened = 0;
        sink.closed = 0;

        AttachmentStack.push(new Table());
        AttachmentStack.pop();

        assertEquals(0, sink.opened);
        assertEquals(0, sink.closed);
    }
}
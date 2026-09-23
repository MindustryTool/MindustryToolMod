package solim.runtime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.core.Component;
import solim.performance.PerfSpan;

class AttachmentStackPerfTest {

    static Component elementComponent() {
        return new Component() {
            @Override
            public Element element() {
                return new Element();
            }
        };
    }

    @AfterEach
    void cleanUp() {
        AttachmentStack.clear();
        AttachmentStack.install(null);
        AttachmentStack.reset();
    }

    @Test
    void noSpansWhenDisabled() {
        AttachmentStack.install(null);
        Table root = new Table();
        AttachmentStack.push(root);
        AttachmentStack.add(new Element());
        AttachmentStack.pop();
        assertTrue(AttachmentStack.snapshot(10).isEmpty());
        assertEquals(0, AttachmentStack.maxDepthObserved());
    }

    @Test
    void popEmitsSubtreeSpanAndTracksDepth() {
        AttachmentStack.install(new DebugAttachmentStack());
        AttachmentStack.setThreshold(0f);

        Table root = new Table();
        AttachmentStack.push(root);
        AttachmentStack.push(new Table());
        AttachmentStack.push(new Table());
        AttachmentStack.pop();
        AttachmentStack.pop();
        AttachmentStack.pop();

        assertEquals(3, AttachmentStack.maxDepthObserved());
        List<PerfSpan> spans = AttachmentStack.snapshot(10);
        assertEquals(3, spans.size());
        for (PerfSpan span : spans) {
            assertEquals("AttachmentStack", span.component);
            assertEquals("subtree", span.phase);
            assertTrue(span.detail.contains("depth="));
            assertTrue(span.detail.contains("children="));
        }
    }

    @Test
    void thresholdSuppressesFastPops() {
        AttachmentStack.install(new DebugAttachmentStack());
        AttachmentStack.setThreshold(10_000f);

        Table root = new Table();
        AttachmentStack.push(root);
        AttachmentStack.pop();

        assertTrue(AttachmentStack.snapshot(10).isEmpty());
    }

    @Test
    void attachBatchEmitsSpanWithParentName() {
        AttachmentStack.install(new DebugAttachmentStack());
        AttachmentStack.setThreshold(0f);

        Table root = new Table();
        root.name = "test-parent";
        AttachmentStack.push(root);
        AttachmentStack.registerPendingComponent(elementComponent(), root);
        AttachmentStack.registerPendingComponent(elementComponent(), root);
        AttachmentStack.pop();

        assertEquals(2, root.getChildren().size);
        boolean foundAttach = false;
        for (PerfSpan span : AttachmentStack.snapshot(10)) {
            if ("attach".equals(span.phase)) {
                foundAttach = true;
                assertTrue(span.detail.contains("children=2"));
                assertTrue(span.detail.contains("test-parent"));
            }
        }
        assertTrue(foundAttach);
    }

    @Test
    void resetClearsDepth() {
        AttachmentStack.install(new DebugAttachmentStack());
        AttachmentStack.push(new Table());
        AttachmentStack.push(new Table());
        AttachmentStack.pop();
        AttachmentStack.pop();
        assertEquals(2, AttachmentStack.maxDepthObserved());
        AttachmentStack.reset();
        assertEquals(0, AttachmentStack.maxDepthObserved());
    }
}
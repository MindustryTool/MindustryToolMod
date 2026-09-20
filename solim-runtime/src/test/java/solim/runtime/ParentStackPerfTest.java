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

class ParentStackPerfTest {

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
        ParentStack.clear();
        ParentStack.install(null);
        ParentStack.reset();
    }

    @Test
    void noSpansWhenDisabled() {
        ParentStack.install(null);
        Table root = new Table();
        ParentStack.push(root);
        ParentStack.add(new Element());
        ParentStack.pop();
        assertTrue(ParentStack.snapshot(10).isEmpty());
        assertEquals(0, ParentStack.maxDepthObserved());
    }

    @Test
    void popEmitsSubtreeSpanAndTracksDepth() {
        ParentStack.install(new DebugParentStack());
        ParentStack.setThreshold(0f);

        Table root = new Table();
        ParentStack.push(root);
        ParentStack.push(new Table());
        ParentStack.push(new Table());
        ParentStack.pop();
        ParentStack.pop();
        ParentStack.pop();

        assertEquals(3, ParentStack.maxDepthObserved());
        List<PerfSpan> spans = ParentStack.snapshot(10);
        assertEquals(3, spans.size());
        for (PerfSpan span : spans) {
            assertEquals("ParentStack", span.component);
            assertEquals("subtree", span.phase);
            assertTrue(span.detail.contains("depth="));
            assertTrue(span.detail.contains("children="));
        }
    }

    @Test
    void thresholdSuppressesFastPops() {
        ParentStack.install(new DebugParentStack());
        ParentStack.setThreshold(10_000f);

        Table root = new Table();
        ParentStack.push(root);
        ParentStack.pop();

        assertTrue(ParentStack.snapshot(10).isEmpty());
    }

    @Test
    void attachBatchEmitsSpanWithParentName() {
        ParentStack.install(new DebugParentStack());
        ParentStack.setThreshold(0f);

        Table root = new Table();
        root.name = "test-parent";
        ParentStack.push(root);
        ParentStack.registerPendingComponent(elementComponent(), root);
        ParentStack.registerPendingComponent(elementComponent(), root);
        ParentStack.pop();

        assertEquals(2, root.getChildren().size);
        boolean foundAttach = false;
        for (PerfSpan span : ParentStack.snapshot(10)) {
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
        ParentStack.install(new DebugParentStack());
        ParentStack.push(new Table());
        ParentStack.push(new Table());
        ParentStack.pop();
        ParentStack.pop();
        assertEquals(2, ParentStack.maxDepthObserved());
        ParentStack.reset();
        assertEquals(0, ParentStack.maxDepthObserved());
    }
}
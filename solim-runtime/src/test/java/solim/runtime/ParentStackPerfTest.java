package solim.runtime;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.core.Component;
import solim.core.PerfSink;

class ParentStackPerfTest {

    static final class Span {
        final String component;
        final String phase;
        final float durationMs;
        final String detail;

        Span(String component, String phase, float durationMs, String detail) {
            this.component = component;
            this.phase = phase;
            this.durationMs = durationMs;
            this.detail = detail;
        }
    }

    static final class FakeSink implements PerfSink {
        final List<Span> spans = new ArrayList<>();

        @Override
        public void record(String component, String phase, float durationMs, String detail) {
            spans.add(new Span(component, phase, durationMs, detail));
        }
    }

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
        ParentStack.setPerfSink(null);
        ParentStack.slowSubtreeThresholdMs = 50f;
        ParentStack.resetPerfStats();
    }

    @Test
    void noSpansWithoutSink() {
        ParentStack.setPerfSink(null);
        Table root = new Table();
        ParentStack.push(root);
        ParentStack.add(new Element());
        ParentStack.pop();
        assertEquals(0, ParentStack.maxDepthObserved());
    }

    @Test
    void popEmitsSubtreeSpanAndTracksDepth() {
        FakeSink sink = new FakeSink();
        ParentStack.setPerfSink(sink);
        ParentStack.slowSubtreeThresholdMs = 0f;

        Table root = new Table();
        ParentStack.push(root);
        ParentStack.push(new Table());
        ParentStack.push(new Table());
        ParentStack.pop();
        ParentStack.pop();
        ParentStack.pop();

        assertEquals(3, ParentStack.maxDepthObserved());
        assertEquals(3, sink.spans.size());
        for (Span span : sink.spans) {
            assertEquals("ParentStack", span.component);
            assertEquals("subtree", span.phase);
            assertTrue(span.detail.contains("depth="));
            assertTrue(span.detail.contains("children="));
        }
    }

    @Test
    void thresholdSuppressesFastPops() {
        FakeSink sink = new FakeSink();
        ParentStack.setPerfSink(sink);
        ParentStack.slowSubtreeThresholdMs = 10_000f;

        Table root = new Table();
        ParentStack.push(root);
        ParentStack.pop();

        assertTrue(sink.spans.isEmpty());
    }

    @Test
    void attachBatchEmitsSpanWithParentName() {
        FakeSink sink = new FakeSink();
        ParentStack.setPerfSink(sink);
        ParentStack.slowSubtreeThresholdMs = 0f;

        Table root = new Table();
        root.name = "test-parent";
        ParentStack.push(root);
        ParentStack.registerPendingComponent(elementComponent(), root);
        ParentStack.registerPendingComponent(elementComponent(), root);
        ParentStack.pop();

        assertEquals(2, root.getChildren().size);
        boolean foundAttach = false;
        for (Span span : sink.spans) {
            if ("attach".equals(span.phase)) {
                foundAttach = true;
                assertTrue(span.detail.contains("children=2"));
                assertTrue(span.detail.contains("test-parent"));
            }
        }
        assertTrue(foundAttach);
    }

    @Test
    void resetPerfStatsClearsDepth() {
        ParentStack.setPerfSink(new FakeSink());
        ParentStack.push(new Table());
        ParentStack.push(new Table());
        ParentStack.pop();
        ParentStack.pop();
        assertEquals(2, ParentStack.maxDepthObserved());
        ParentStack.resetPerfStats();
        assertEquals(0, ParentStack.maxDepthObserved());
    }
}

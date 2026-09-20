package solim.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.ui.layout.Table;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.runtime.ParentStack;

class SlowTrackerTest {

    @AfterEach
    void tearDown() {
        SlowTracker.setEnabled(false);
        SlowTracker.setThresholds(16f, 50f);
        ParentStack.clear();
    }

    @Test
    void disabledTrackerRecordsNothing() {
        SlowTracker.setEnabled(false);
        SlowTracker.recordForced("Card", "build", 100f, null);
        SlowTracker.recordBuild("Card", 100f);
        assertEquals(0, SlowTracker.totalRecorded());
        assertTrue(SlowTracker.snapshot(10).isEmpty());
    }

    @Test
    void slowBuildOverThresholdIsRecorded() {
        SlowTracker.setEnabled(true);
        SlowTracker.setThresholds(16f, 50f);
        SlowTracker.recordBuild("SchematicCard", 42f);
        List<SlowSpan> spans = SlowTracker.snapshot(10);
        assertEquals(1, spans.size());
        assertEquals("SchematicCard", spans.get(0).component);
        assertEquals("build", spans.get(0).phase);
    }

    @Test
    void fastBuildBelowThresholdIsIgnored() {
        SlowTracker.setEnabled(true);
        SlowTracker.setThresholds(16f, 50f);
        SlowTracker.recordBuild("FastCard", 1f);
        SlowTracker.recordPhase("Grid", "reflow", 1f, null);
        assertTrue(SlowTracker.snapshot(10).isEmpty());
    }

    @Test
    void bufferBoundsAndSnapshotIsNewestFirst() {
        SlowTracker.setEnabled(true);
        SlowTracker.setThresholds(0f, 0f);
        for (int i = 0; i < 300; i++) {
            SlowTracker.recordForced("Comp-" + i, "build", i, null);
        }
        assertEquals(256, SlowTracker.totalRecorded());
        List<SlowSpan> spans = SlowTracker.snapshot(256);
        assertEquals(256, spans.size());
        assertEquals("Comp-299", spans.get(0).component);
        assertEquals("Comp-44", spans.get(255).component);
    }

    @Test
    void disablingClearsBuffer() {
        SlowTracker.setEnabled(true);
        SlowTracker.setThresholds(0f, 0f);
        SlowTracker.recordForced("Card", "build", 10f, null);
        assertEquals(1, SlowTracker.totalRecorded());
        SlowTracker.setEnabled(false);
        assertEquals(0, SlowTracker.totalRecorded());
        assertTrue(SlowTracker.snapshot(10).isEmpty());
    }

    @Test
    void enablingWiresEngineSink() {
        SlowTracker.setEnabled(false);
        assertNull(ParentStack.perfSink());
        SlowTracker.setThresholds(16f, 50f);
        SlowTracker.setEnabled(true);
        assertTrue(ParentStack.perfSink() != null);
        assertEquals(50f, ParentStack.slowSubtreeThresholdMs, 0.001f);
        SlowTracker.setEnabled(false);
        assertNull(ParentStack.perfSink());
    }

    @Test
    void engineSpansLandInBuffer() {
        SlowTracker.setThresholds(0f, 0f);
        SlowTracker.setEnabled(true);
        ParentStack.push(new Table());
        ParentStack.pop();
        List<SlowSpan> spans = SlowTracker.snapshot(120);
        boolean found = false;
        for (SlowSpan span : spans) {
            if ("ParentStack".equals(span.component) && "subtree".equals(span.phase)) {
                found = true;
            }
        }
        assertTrue(found);
    }
}

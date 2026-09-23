package solim.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import solim.runtime.AttachmentStack;
import solim.test.SolimEnv;

class PerfTest extends SolimEnv {

    @AfterEach
    void tearDown() {
        Perf.setEnabled(false);
        Perf.setThreshold(50f);
        AttachmentStack.clear();
    }

    @Test
    void disabledByDefaultRecordsNothing() {
        Perf.setEnabled(false);
        assertFalse(Perf.isEnabled());
        assertEquals(0, Perf.totalRecorded());
        assertTrue(Perf.snapshot(10).isEmpty());
    }

    @Test
    void enableStartsRecordingAndDisableClears() {
        Perf.setEnabled(true);
        Perf.setThreshold(0f);
        assertTrue(Perf.isEnabled());

        Table table = new Table();
        AttachmentStack.push(table);
        AttachmentStack.pop();

        assertTrue(Perf.totalRecorded() >= 1);
        assertFalse(Perf.snapshot(10).isEmpty());

        Perf.setEnabled(false);
        assertFalse(Perf.isEnabled());
        assertEquals(0, Perf.totalRecorded());
        assertTrue(Perf.snapshot(10).isEmpty());
    }

    @Test
    void resetClearsRecordedSpans() {
        Perf.setEnabled(true);
        Perf.setThreshold(0f);

        Table table = new Table();
        AttachmentStack.push(table);
        AttachmentStack.pop();
        assertTrue(Perf.totalRecorded() >= 1);

        Perf.reset();
        assertEquals(0, Perf.totalRecorded());
        assertTrue(Perf.snapshot(10).isEmpty());
    }
}
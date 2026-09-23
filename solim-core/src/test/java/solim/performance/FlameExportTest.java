package solim.performance;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class FlameExportTest {

    private static List<TraceSpan> syntheticTrace() {
        List<TraceSpan> spans = new ArrayList<>();
        long base = 1000000000L;
        spans.add(new TraceSpan(1L, 0, -1, "root", "build", base, base + 10000000L, 0, 2));
        spans.add(new TraceSpan(1L, 1, 0, "child-a", "build", base + 1000000L, base + 7000000L, 1, 0));
        spans.add(new TraceSpan(1L, 2, 0, "child-b", "build", base + 7000000L, base + 7100000L, 1, 0));
        return spans;
    }

    @Test
    void orderingPruningAndSelfTime() {
        List<TraceSpan> spans = syntheticTrace();
        List<TraceSpan> ordered = FlameExporter.ordered(spans);
        assertEquals("root", ordered.get(0).name);
        assertEquals(3, ordered.size());

        List<TraceSpan> kept = FlameExporter.pruned(ordered, 1.0);
        assertEquals(2, kept.size());
        assertEquals(3, spans.size());

        Map<Integer, Long> self = FlameExporter.selfTimes(ordered);
        long rootSelf = self.get(0);
        long childA = 6000000L;
        long childB = 100000L;
        assertEquals(10000000L - childA - childB, rootSelf);
        assertEquals(childA, (long) self.get(1));
    }

    @Test
    void dualPayloadsAgreeAndHtmlIsSelfContained() {
        List<TraceSpan> spans = syntheticTrace();
        String chrome = FlameExporter.chromePayload(spans, 0.0, 0, 1L);
        String speedscope = FlameExporter.speedscopePayload(spans, 0.0, 0, 1L);

        assertTrue(chrome.contains("traceEvents"));
        assertTrue(chrome.contains("\"ph\":\"X\""));
        assertTrue(chrome.contains("child-a"));
        assertTrue(speedscope.contains("speedscope.app"));
        assertTrue(speedscope.contains("child-a"));
        assertTrue(speedscope.contains("\"events\""));

        int chromeEvents = chrome.split("\"ph\":\"X\"", -1).length - 1;
        assertEquals(3, chromeEvents);

        String html = FlameExporter.exportHtml(spans, 0.0, 0, 1L);
        assertFalse(html.trim().isEmpty());
        assertTrue(html.contains("traceEvents"));
        assertTrue(html.contains("speedscope"));
        assertTrue(html.contains("Solim Flame Trace"));
        assertFalse(html.contains("src=\"http"));
    }

    @Test
    void minMsPruningHidesSmallSpansButKeepsStoredTrace() {
        List<TraceSpan> spans = syntheticTrace();
        String chromeAll = FlameExporter.chromePayload(spans, 0.0, 0, 1L);
        String chromePruned = FlameExporter.chromePayload(spans, 1.0, 0, 1L);

        assertTrue(chromeAll.contains("child-b"));
        assertFalse(chromePruned.contains("child-b"));
        assertTrue(chromePruned.contains("child-a"));
        assertEquals(3, spans.size());

        String html = FlameExporter.exportHtml(spans, 1.0, 0, 1L);
        assertTrue(html.contains("child-a"));
        assertFalse(html.contains("child-b"));
    }

    @Test
    void labelingNeverBlankWithFallback() {
        List<TraceSpan> spans = new ArrayList<>();
        spans.add(new TraceSpan(1L, 0, -1, "", "build", 0L, 1000000L, 0, 0));
        spans.add(new TraceSpan(1L, 1, 0, null, "build", 0L, 500000L, 1, 0));

        String chrome = FlameExporter.chromePayload(spans, 0.0, 0, 1L);
        assertFalse(chrome.contains("\"name\":\"\""));
        assertTrue(chrome.contains("Component-element"));

        String html = FlameExporter.exportHtml(spans, 0.0, 0, 1L);
        assertFalse(html.contains("unknown"));
    }
}

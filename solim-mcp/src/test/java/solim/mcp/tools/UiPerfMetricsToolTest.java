package solim.mcp.tools;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import solim.mcp.introspection.SnapshotRoot;
import solim.perf.PerfTable;
import solim.perf.UiProfiler;

class UiPerfMetricsToolTest {

    @Test
    void toolNameAndSchema() {
        UiPerfMetricsTool tool = new UiPerfMetricsTool(SnapshotRoot.fixed(null));
        assertEquals("get_ui_perf_metrics", tool.name());
        ObjectNode schema = tool.inputSchema();
        assertEquals("object", schema.path("type").asText());
        assertTrue(schema.path("properties").has("frames"));
        assertTrue(schema.path("properties").has("enabled"));
    }

    @Test
    void executeReturnsMetrics() {
        Table root = new Table();
        PerfTable child = new PerfTable();
        child.setAutoCommit(true);
        root.add(child);
        child.add(new Element());

        UiPerfMetricsTool tool = new UiPerfMetricsTool(SnapshotRoot.fixed(root));
        ObjectNode args = JsonNodeFactory.instance.objectNode();
        args.put("frames", 10);
        args.put("enabled", true);

        // Simulate frame execution
        UiProfiler.setEnabled(true);
        child.act(0.016f);
        child.validate();
        child.draw();

        ObjectNode result = tool.execute(args);
        assertNotNull(result);
        assertTrue(result.path("profilerEnabled").asBoolean());
        assertTrue(result.path("totalElements").asInt() >= 3);
        assertTrue(result.has("chatHud"));
        ObjectNode chatHud = (ObjectNode) result.get("chatHud");
        assertTrue(chatHud.has("actMs"));
        assertTrue(chatHud.has("layoutMs"));
        assertTrue(chatHud.has("drawMs"));
        assertTrue(chatHud.has("layoutPassesPerFrame"));

        UiProfiler.setEnabled(false);
    }
}

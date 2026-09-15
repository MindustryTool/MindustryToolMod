package solim.mcp.tools;

import arc.Core;
import arc.scene.Element;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import solim.mcp.introspection.SnapshotRoot;
import solim.perf.PerfFrame;
import solim.perf.UiProfiler;

/**
 * MCP tool that queries real-time UI performance metrics and phase breakdowns
 * (act, layout, draw) for Solim UI overlays.
 */
public final class UiPerfMetricsTool implements McpTool {

    private final SnapshotRoot snapshotRoot;

    public UiPerfMetricsTool(SnapshotRoot snapshotRoot) {
        this.snapshotRoot = snapshotRoot;
    }

    @Override
    public String name() {
        return "get_ui_perf_metrics";
    }

    @Override
    public String description() {
        return "Returns live Solim UI performance metrics, frame times, element counts, and act/layout/draw breakdowns.";
    }

    @Override
    public ObjectNode inputSchema() {
        ObjectNode schema = JsonNodeFactory.instance.objectNode();
        schema.put("type", "object");
        ObjectNode props = schema.putObject("properties");
        props.putObject("frames")
                .put("type", "integer")
                .put("description", "Number of recent frames to average (default 60, max 120).");
        props.putObject("enabled")
                .put("type", "boolean")
                .put("description", "Whether to enable the UI profiler if not already active (default true).");
        return schema;
    }

    @Override
    public ObjectNode execute(ObjectNode args) {
        boolean shouldEnable = !args.has("enabled") || args.get("enabled").asBoolean(true);
        int frames = args.has("frames") ? Math.max(1, Math.min(120, args.get("frames").asInt(60))) : 60;

        if (shouldEnable && !UiProfiler.isEnabled()) {
            UiProfiler.setEnabled(true);
        }

        ObjectNode result = JsonNodeFactory.instance.objectNode();
        result.put("profilerEnabled", UiProfiler.isEnabled());

        Element root = snapshotRoot.get();
        if (root == null && Core.scene != null) {
            root = Core.scene.root;
        }

        int[] elementCounts = UiProfiler.countElements(root);
        result.put("totalElements", elementCounts[0]);
        result.put("visibleElements", elementCounts[1]);

        PerfFrame avg = UiProfiler.snapshotAverages(frames);
        if (avg != null) {
            result.put("fps", Math.round(avg.fps * 10f) / 10f);
            ObjectNode hudMetrics = result.putObject("chatHud");
            hudMetrics.put("actMs", Math.round(avg.actMs * 100f) / 100f);
            hudMetrics.put("layoutMs", Math.round(avg.layoutMs * 100f) / 100f);
            hudMetrics.put("drawMs", Math.round(avg.drawMs * 100f) / 100f);
            hudMetrics.put("drawCallCount", avg.drawCallCount);
            hudMetrics.put("layoutPassesPerFrame", avg.layoutPasses);
        } else {
            result.put("message", "Profiler recently enabled. Accumulating frames... Please re-query in a few moments.");
        }

        return result;
    }
}

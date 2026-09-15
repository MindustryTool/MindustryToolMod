package solim.mcp.tools;

import arc.scene.Element;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.LinkedHashMap;
import java.util.Map;
import solim.mcp.introspection.SnapshotRoot;
import solim.mcp.introspection.UiSnapshot;

/**
 * Registers and dispatches the debug tools. Tools are stateless and safe to call concurrently.
 */
public final class ToolRegistry {
	private final Map<String, McpTool> tools = new LinkedHashMap<>();
	private final SnapshotRoot snapshotRoot;

	public ToolRegistry(SnapshotRoot snapshotRoot) {
		this(snapshotRoot, null);
	}

	public ToolRegistry(SnapshotRoot snapshotRoot, Runnable exitAction) {
		this.snapshotRoot = snapshotRoot;
		register(new ComponentTreeTool(snapshotRoot));
		register(new SignalValuesTool(snapshotRoot));
		register(new BindingsTool(snapshotRoot));
		register(new LayoutTool(snapshotRoot));
		register(new FindElementsTool(snapshotRoot));
		register(new ClickElementTool(snapshotRoot));
		register(new ScreenshotTool());
		register(new ExecuteJsTool());
		register(new UiPerfMetricsTool(snapshotRoot));
		register(exitAction != null ? new StopTool(exitAction) : new StopTool());
	}

	private void register(McpTool tool) {
		tools.put(tool.name(), tool);
	}

	public McpTool get(String name) {
		McpTool tool = tools.get(name);
		if (tool == null && "stop_mindustry".equals(name)) {
			return tools.get("stop");
		}
		return tool;
	}

	public ArrayNode list() {
		ArrayNode out = JsonNodeFactory.instance.arrayNode();
		for (McpTool tool : tools.values()) {
			ObjectNode def = out.addObject();
			def.put("name", tool.name());
			def.put("description", tool.description());
			def.set("inputSchema", tool.inputSchema());
		}
		return out;
	}

	/** Root element provided to tools (scene root in the live game, injected in tests). */
	public Element root() {
		return snapshotRoot.get();
	}

	public Element find(String target) {
		return UiSnapshot.lookup(root(), target);
	}
}
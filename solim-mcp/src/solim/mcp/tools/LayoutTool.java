package solim.mcp.tools;

import arc.scene.Element;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import solim.mcp.introspection.LayoutInspector;
import solim.mcp.introspection.SnapshotRoot;
import solim.mcp.introspection.UiSnapshot;

/** Returns layout metrics for a target element (required). */
public final class LayoutTool implements McpTool {
	private final SnapshotRoot snapshotRoot;

	public LayoutTool(SnapshotRoot snapshotRoot) {
		this.snapshotRoot = snapshotRoot;
	}

	@Override
	public String name() {
		return "get_layout";
	}

	@Override
	public String description() {
		return "Returns x/y/width/height and expansion flags for a named element.";
	}

	@Override
	public ObjectNode inputSchema() {
		ObjectNode schema = JsonNodeFactory.instance.objectNode();
		schema.put("type", "object");
		ObjectNode props = schema.putObject("properties");
		props.putObject("target").put("type", "string")
			.put("description", "Element name, simple class name, or fully qualified class name.");
		schema.putArray("required").add("target");
		return schema;
	}

	@Override
	public ObjectNode execute(ObjectNode args) throws MCPException {
		String target = args.path("target").isTextual() ? args.path("target").asText() : "";
		if (target.isEmpty()) {
			throw new MCPException("get_layout requires a 'target' element name");
		}
		Element element = UiSnapshot.lookup(snapshotRoot.get(), target);
		if (element == null) {
			throw new MCPException("No element matches '" + target + "'");
		}
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.put("target", target);
		result.set("layout", LayoutInspector.node(element));
		return result;
	}
}
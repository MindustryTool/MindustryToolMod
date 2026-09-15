package solim.mcp.tools;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import solim.mcp.introspection.SnapshotRoot;
import solim.mcp.introspection.UiSnapshot;

/** Returns the live component/element tree, optionally scoped to a matching subtree. */
public final class ComponentTreeTool implements McpTool {
	private final SnapshotRoot snapshotRoot;

	public ComponentTreeTool(SnapshotRoot snapshotRoot) {
		this.snapshotRoot = snapshotRoot;
	}

	@Override
	public String name() {
		return "get_component_tree";
	}

	@Override
	public String description() {
		return "Returns the live Arc/Solim element tree (name, type, layout, children).";
	}

	@Override
	public ObjectNode inputSchema() {
		ObjectNode schema = JsonNodeFactory.instance.objectNode();
		schema.put("type", "object");
		ObjectNode props = schema.putObject("properties");
		props.putObject("target").put("type", "string")
			.put("description", "Optional element name or class to scope the snapshot to a subtree.");
		return schema;
	}

	@Override
	public ObjectNode execute(ObjectNode args) throws MCPException {
		String target = args.path("target").isTextual() ? args.path("target").asText() : "";
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		try {
			result.set("tree", UiSnapshot.tree(snapshotRoot.get(), target));
		} catch (IllegalArgumentException e) {
			throw new MCPException(e.getMessage());
		}
		return result;
	}
}

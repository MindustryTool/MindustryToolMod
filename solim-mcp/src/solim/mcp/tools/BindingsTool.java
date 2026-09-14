package solim.mcp.tools;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import solim.mcp.introspection.ReactiveKind;
import solim.mcp.introspection.ReactiveRef;
import solim.mcp.introspection.SnapshotRoot;
import solim.mcp.introspection.UiSnapshot;
import solim.mcp.introspection.BindingInspector;

/** Reports reactive bindings/effects attached to the inspected UI, with their subscribed state. */
public final class BindingsTool implements McpTool {
	private final SnapshotRoot snapshotRoot;

	public BindingsTool(SnapshotRoot snapshotRoot) {
		this.snapshotRoot = snapshotRoot;
	}

	@Override
	public String name() {
		return "get_bindings";
	}

	@Override
	public String description() {
		return "Returns reactive effects/bindings discovered on components and elements.";
	}

	@Override
	public ObjectNode inputSchema() {
		ObjectNode schema = JsonNodeFactory.instance.objectNode();
		schema.put("type", "object");
		ObjectNode props = schema.putObject("properties");
		props.putObject("query").put("type", "string")
			.put("description", "Optional case-insensitive substring filter on binding target/location.");
		return schema;
	}

	@Override
	public ObjectNode execute(ObjectNode args) throws MCPException {
		String query = args.path("query").isTextual() ? args.path("query").asText().toLowerCase() : "";
		ArrayNode bindings = JsonNodeFactory.instance.arrayNode();
		UiSnapshot snapshot = UiSnapshot.capture(snapshotRoot.get());
		for (ReactiveRef ref : snapshot.reactiveRefs) {
			if (ref.kind != ReactiveKind.EFFECT) continue;
			String location = ref.location.toLowerCase();
			String element = ref.sourceElement != null ? ref.sourceElement.toLowerCase() : "";
			if (!query.isEmpty() && !location.contains(query) && !element.contains(query)) continue;
			bindings.add(BindingInspector.snapshot(ref));
		}
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.set("bindings", bindings);
		result.put("count", bindings.size());
		return result;
	}
}
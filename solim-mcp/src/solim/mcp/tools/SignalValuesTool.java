package solim.mcp.tools;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import solim.mcp.introspection.ReactiveKind;
import solim.mcp.introspection.ReactiveRef;
import solim.mcp.introspection.SignalIntrospector;
import solim.mcp.introspection.SnapshotRoot;
import solim.mcp.introspection.UiSnapshot;

/**
 * Returns current values and counts for discovered signal/computed instances. When a query is given
 * (case-insensitive substring), only matching location/id entries are returned.
 */
public final class SignalValuesTool implements McpTool {
	private final SnapshotRoot snapshotRoot;

	public SignalValuesTool(SnapshotRoot snapshotRoot) {
		this.snapshotRoot = snapshotRoot;
	}

	@Override
	public String name() {
		return "get_signal_values";
	}

	@Override
	public String description() {
		return "Returns live Signal/Computed values with listener/observer/dependency counts.";
	}

	@Override
	public ObjectNode inputSchema() {
		ObjectNode schema = JsonNodeFactory.instance.objectNode();
		schema.put("type", "object");
		ObjectNode props = schema.putObject("properties");
		props.putObject("query").put("type", "string")
			.put("description", "Optional case-insensitive substring filter on signal location/id.");
		return schema;
	}

	@Override
	public ObjectNode execute(ObjectNode args) throws MCPException {
		String query = args.path("query").isTextual() ? args.path("query").asText().toLowerCase() : "";
		ArrayNode signals = JsonNodeFactory.instance.arrayNode();
		UiSnapshot snapshot = UiSnapshot.capture(snapshotRoot.get());
		for (ReactiveRef ref : snapshot.reactiveRefs) {
			if (ref.kind != ReactiveKind.SIGNAL && ref.kind != ReactiveKind.COMPUTED) continue;
			if (!query.isEmpty() && !ref.location.toLowerCase().contains(query)) continue;
			signals.add(SignalIntrospector.snapshot(ref, JsonNodeFactory.instance.objectNode()));
		}
		ObjectNode result = JsonNodeFactory.instance.objectNode();
		result.set("signals", signals);
		result.put("count", signals.size());
		return result;
	}
}
package solim.mcp.tools;

import arc.scene.Element;
import arc.scene.Group;
import arc.scene.ui.Label;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.IdentityHashMap;
import java.util.Map;
import solim.mcp.introspection.LayoutInspector;
import solim.mcp.introspection.SnapshotRoot;

/**
 * Searches the live element hierarchy for elements matching a query by name, class, or label text.
 */
public final class FindElementsTool implements McpTool {
	private final SnapshotRoot snapshotRoot;

	public FindElementsTool(SnapshotRoot snapshotRoot) {
		this.snapshotRoot = snapshotRoot;
	}

	@Override
	public String name() {
		return "find_elements";
	}

	@Override
	public String description() {
		return "Searches the live UI hierarchy for elements matching a query by name, class, or label text.";
	}

	@Override
	public ObjectNode inputSchema() {
		ObjectNode schema = JsonNodeFactory.instance.objectNode();
		schema.put("type", "object");
		ObjectNode props = schema.putObject("properties");
		props.putObject("query")
			.put("type", "string")
			.put("description", "Substring to search for in element names, class names, or label text (case-insensitive).");
		props.putObject("maxResults")
			.put("type", "integer")
			.put("description", "Maximum number of matching elements to return (default 25).");
		schema.putArray("required").add("query");
		return schema;
	}

	@Override
	public ObjectNode execute(ObjectNode args) throws MCPException {
		if (!args.hasNonNull("query")) {
			throw new MCPException("Missing required argument: 'query'");
		}
		String query = args.get("query").asText().toLowerCase();
		int maxResults = args.path("maxResults").asInt(25);
		if (maxResults <= 0) maxResults = 25;

		Element root = snapshotRoot.get();
		ArrayNode matches = JsonNodeFactory.instance.arrayNode();
		if (root != null) {
			final int max = maxResults;
			search(root, query, matches, max, 0, new IdentityHashMap<>());
		}

		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.set("elements", matches);
		out.put("count", matches.size());
		return out;
	}

	private static void search(Element element, String query, ArrayNode matches, int max, int depth, Map<Element, Boolean> visited) {
		if (element == null || visited.put(element, Boolean.TRUE) != null || depth > 32 || matches.size() >= max) {
			return;
		}

		String name = element.name != null ? element.name : "";
		String simpleClass = element.getClass().getSimpleName();
		String labelText = element instanceof Label ? ((Label) element).getText().toString() : "";

		boolean match = name.toLowerCase().contains(query)
			|| simpleClass.toLowerCase().contains(query)
			|| (!labelText.isEmpty() && labelText.toLowerCase().contains(query));

		if (match) {
			ObjectNode node = matches.addObject();
			node.put("name", name);
			node.put("type", simpleClass);
			node.put("className", element.getClass().getName());
			if (!labelText.isEmpty()) {
				node.put("text", labelText);
			}
			ObjectNode layout = LayoutInspector.node(element);
			if (layout != null) {
				node.set("layout", layout);
			}
		}

		if (element instanceof Group) {
			for (Element child : ((Group) element).getChildren()) {
				search(child, query, matches, max, depth + 1, visited);
				if (matches.size() >= max) break;
			}
		}
	}
}

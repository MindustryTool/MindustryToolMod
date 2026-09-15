package solim.mcp.introspection;

import arc.scene.Element;
import arc.scene.Group;
import arc.util.Nullable;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.IdentityHashMap;
import java.util.Map;

/**
 * Snapshots the live Arc Scene2D element tree into a stable, jackson-backed DTO. The walk uses the
 * public Arc API and never mutates the tree.
 */
public final class ComponentTreeInspector {
	private static final int MAX_DEPTH = 32;

	private ComponentTreeInspector() {}

	public static ObjectNode tree(Element root) {
		return tree(root, MAX_DEPTH);
	}

	public static ObjectNode tree(@Nullable Element root, int maxDepth) {
		Map<Element, Boolean> visited = new IdentityHashMap<>();
		return buildNode(root, maxDepth, 0, visited);
	}

	private static ObjectNode buildNode(@Nullable Element element, int maxDepth, int depth,
			Map<Element, Boolean> visited) {
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		if (element == null) {
			node.putNull("name");
			return node;
		}
		if (visited.put(element, Boolean.TRUE) != null) {
			node.put("name", name(element));
			node.put("cycle", true);
			return node;
		}
		node.put("name", name(element));
		node.put("type", element.getClass().getSimpleName());
		node.put("className", element.getClass().getName());
		ObjectNode layout = LayoutInspector.node(element);
		if (layout != null) node.set("layout", layout);
		if (depth < maxDepth && element instanceof Group) {
			ArrayNode children = node.putArray("children");
			for (Element child : ((Group) element).getChildren()) {
				children.add(buildNode(child, maxDepth, depth + 1, visited));
			}
		}
		return node;
	}

	private static String name(Element element) {
		return element.name != null && !element.name.isEmpty() ? element.name : element.getClass().getSimpleName();
	}
}
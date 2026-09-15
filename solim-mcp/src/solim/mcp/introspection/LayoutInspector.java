package solim.mcp.introspection;

import arc.scene.Element;
import arc.util.Nullable;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/** Reads layout metrics of an Arc element via its public API. */
public final class LayoutInspector {
	private LayoutInspector() {}

	public static @Nullable ObjectNode node(@Nullable Element element) {
		if (element == null) return null;
		ObjectNode node = JsonNodeFactory.instance.objectNode();
		node.put("x", element.x);
		node.put("y", element.y);
		node.put("width", element.getWidth());
		node.put("height", element.getHeight());
		node.put("visible", element.visible);
		Object userObject = element.userObject;
		node.put("expanding", "expanding".equals(userObject) || Boolean.TRUE.equals(userObject));
		return node;
	}
}
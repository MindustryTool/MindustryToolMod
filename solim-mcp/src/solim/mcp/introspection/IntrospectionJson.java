package solim.mcp.introspection;

import arc.util.Nullable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.TextNode;

/** JSON rendering helpers for debug values. */
public final class IntrospectionJson {
	private static final ObjectMapper MAPPER = new ObjectMapper();
	private static final JsonNodeFactory NODES = JsonNodeFactory.instance;

	private IntrospectionJson() {}

	/**
	 * Renders an arbitrary value to a JSON node. Scalars are converted directly; anything else falls
	 * back to {@code className.toString()} so cyclic or heavy objects never recurse.
	 */
	public static JsonNode renderValue(@Nullable Object value) {
		if (value == null) return NODES.nullNode();
		if (value instanceof Number) return MAPPER.convertValue(value, JsonNode.class);
		if (value instanceof Boolean) return NODES.booleanNode((Boolean) value);
		if (value instanceof CharSequence) return NODES.textNode(value.toString());
		if (value instanceof Character) return NODES.textNode(String.valueOf(value));
		if (value instanceof Enum) return NODES.textNode(((Enum<?>) value).name());
		return NODES.textNode(value.getClass().getSimpleName() + ": " + value);
	}

	public static JsonNode text(String value) {
		return TextNode.valueOf(value == null ? "" : value);
	}
}
package solim.mcp.rpc;

import arc.util.Log;
import arc.util.Nullable;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import solim.mcp.tools.McpTool;
import solim.mcp.tools.ToolRegistry;

/**
 * Minimal MCP (JSON-RPC 2.0) server handler.
 *
 * <p>Implements just the subset needed for UI debugging: {@code initialize}, {@code ping}, {@code
 * notifications/initialized}, {@code tools/list}, {@code tools/call}, plus the custom {@code
 * solim/subscribe} and {@code solim/unsubscribe} methods that power sampling-based change
 * notifications.
 */
public final class JsonRpcHandler {
	public static final String PROTOCOL_VERSION = "2024-11-05";
	public static final String SERVER_NAME = "solim-mcp-debug";

	private static final int ERROR_PARSE = -32700;
	private static final int ERROR_INVALID_REQUEST = -32600;
	private static final int ERROR_METHOD_NOT_FOUND = -32601;
	private static final int ERROR_INVALID_PARAMS = -32602;
	private static final int ERROR_INTERNAL = -32603;
	private static final int ERROR_TOOL = -32000;

	private final ObjectMapper mapper = new ObjectMapper();
	private final ToolRegistry tools;
	private final Set<SubscriptionSink> subscribers = ConcurrentHashMap.newKeySet();

	public JsonRpcHandler(ToolRegistry tools) {
		this.tools = tools;
	}

	public Set<SubscriptionSink> subscribers() {
		return subscribers;
	}

	public boolean subscribe(SubscriptionSink sink) {
		return subscribers.add(sink);
	}

	public boolean unsubscribe(SubscriptionSink sink) {
		return subscribers.remove(sink);
	}

	/**
	 * Handles a single inbound JSON-RPC message.
	 *
	 * @return the response message to send back, or {@code null} for notifications
	 */
	public @Nullable String handle(String message, SubscriptionSink sink) {
		JsonNode request;
		try {
			request = mapper.readTree(message);
		} catch (Exception e) {
			return error(null, ERROR_PARSE, "Parse error: " + e.getMessage());
		}
		if (request == null || !request.isObject()) {
			return error(null, ERROR_INVALID_REQUEST, "Invalid request");
		}
		String method = request.path("method").asText("");
		JsonNode id = request.has("id") ? request.get("id") : null;
		JsonNode params = request.path("params");
		boolean isNotification = id == null || id.isNull();

		switch (method) {
			case "initialize":
				return ok(id, initialize());
			case "notifications/initialized":
				return null;
			case "ping":
				return isNotification ? null : ok(id, empty());
			case "tools/list":
				return isNotification ? null : toolsList(id);
			case "tools/call":
				return isNotification ? null : toolsCall(id, params);
			case "solim/subscribe":
				return subscribeAction(id, params, sink);
			case "solim/unsubscribe":
				return unsubscribeAction(id, sink);
			default:
				return isNotification ? null : error(id, ERROR_METHOD_NOT_FOUND, "Method not found: " + method);
		}
	}

	private ObjectNode initialize() {
		ObjectNode result = mapper.createObjectNode();
		result.put("protocolVersion", PROTOCOL_VERSION);
		ObjectNode capabilities = result.putObject("capabilities");
		capabilities.putObject("tools").put("listChanged", false);
		ObjectNode info = result.putObject("serverInfo");
		info.put("name", SERVER_NAME);
		info.put("version", "1.0");
		return result;
	}

	private @Nullable String toolsList(JsonNode id) {
		return ok(id, nodeOf(listTools()));
	}

	private ObjectNode nodeOf(JsonNode value) {
		ObjectNode result = mapper.createObjectNode();
		result.set("tools", value);
		return result;
	}

	private JsonNode listTools() {
		return tools.list();
	}

	private @Nullable String toolsCall(JsonNode id, JsonNode params) {
		String name = params.path("name").asText("");
		McpTool tool = tools.get(name);
		if (tool == null) {
			return error(id, ERROR_METHOD_NOT_FOUND, "Unknown tool: " + name);
		}
		JsonNode args = params.path("arguments");
		try {
			ObjectNode toolResult = tool.execute(args.isObject() ? (ObjectNode) args : mapper.createObjectNode());
			ObjectNode result = mapper.createObjectNode();
			ArrayNode content = result.putArray("content");
			content.addObject().put("type", "text").put("text", toolResult.toString());
			return ok(id, result);
		} catch (McpTool.MCPException e) {
			return error(id, ERROR_TOOL, e.getMessage());
		} catch (Exception e) {
			Log.err("solim-mcp] tool '" + name + "' failed", e);
			return error(id, ERROR_INTERNAL, "Tool execution failed: " + e.getMessage());
		}
	}

	private @Nullable String subscribeAction(JsonNode id, JsonNode params, SubscriptionSink sink) {
		if (sink == null) {
			return error(id, ERROR_INVALID_PARAMS, "Subscription requires a connection");
		}
		subscribe(sink);
		ObjectNode result = empty();
		return isNotificationLike(id) ? null : ok(id, result.put("subscribed", true));
	}

	private @Nullable String unsubscribeAction(JsonNode id, SubscriptionSink sink) {
		if (sink != null) unsubscribe(sink);
		ObjectNode result = empty();
		return isNotificationLike(id) ? null : ok(id, result.put("subscribed", false));
	}

	private boolean isNotificationLike(JsonNode id) {
		return id == null || id.isNull();
	}

	private ObjectNode empty() {
		return mapper.createObjectNode();
	}

	private String ok(JsonNode id, ObjectNode result) {
		ObjectNode response = mapper.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", id != null ? id : mapper.nullNode());
		response.set("result", result);
		return response.toString();
	}

	private String error(JsonNode id, int code, String message) {
		ObjectNode response = mapper.createObjectNode();
		response.put("jsonrpc", "2.0");
		response.set("id", id != null ? id : mapper.nullNode());
		ObjectNode err = response.putObject("error");
		err.put("code", code);
		err.put("message", message);
		return response.toString();
	}
}
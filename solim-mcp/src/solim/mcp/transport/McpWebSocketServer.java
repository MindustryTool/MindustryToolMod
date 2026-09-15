package solim.mcp.transport;

import arc.util.Log;
import arc.util.Nullable;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import solim.mcp.McpConfig;
import solim.mcp.rpc.JsonRpcHandler;
import solim.mcp.rpc.SubscriptionSink;

/**
 * WebSocket transport for the MCP debug server.
 *
 * <p>Connections must present the configured token, either as {@code ?token=...} in the resource
 * descriptor or as an {@code X-Mcp-Token} header. Unauthorized connections are closed immediately
 * and never receive or send data.
 */
public final class McpWebSocketServer extends WebSocketServer {
	private static final int CLOSE_UNAUTHORIZED = 4003;

	private final McpConfig config;
	private final JsonRpcHandler handler;
	private final Map<WebSocket, SubscriptionSink> sinks = new ConcurrentHashMap<>();

	public McpWebSocketServer(McpConfig config, JsonRpcHandler handler) {
		super(new InetSocketAddress(config.host, config.port));
		this.config = config;
		this.handler = handler;
		setReuseAddr(true);
		setDaemon(true);
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake) {
		if (!authorized(handshake)) {
			Log.warn("[solim-mcp] rejecting unauthorized connection from {0}", conn.getRemoteSocketAddress());
			conn.close(CLOSE_UNAUTHORIZED, "unauthorized");
			return;
		}
		SubscriptionSink sink = json -> conn.send(json);
		sinks.put(conn, sink);
	}

	@Override
	public void onMessage(WebSocket conn, String message) {
		SubscriptionSink sink = sinks.get(conn);
		if (sink == null) return;
		String response = handler.handle(message, sink);
		if (response != null) {
			conn.send(response);
		}
	}

	@Override
	public void onClose(WebSocket conn, int code, String reason, boolean remote) {
		SubscriptionSink sink = sinks.remove(conn);
		if (sink != null) {
			handler.unsubscribe(sink);
		}
	}

	@Override
	public void onError(WebSocket conn, Exception ex) {
		Log.warn("[solim-mcp] websocket error: {0}", ex != null ? ex.getMessage() : "unknown");
	}

	@Override
	public void onStart() {
		Log.info("[solim-mcp] WebSocket server listening on {0}:{1} ({2})",
			config.host, config.port, config.token.isEmpty() ? "no auth required" : "token: " + mask(config.token));
	}

	private boolean authorized(ClientHandshake handshake) {
		if (config.token.isEmpty()) {
			return true;
		}
		String query = handshake.getResourceDescriptor();
		String token = queryToken(query);
		if (token == null && handshake.hasFieldValue("X-Mcp-Token")) {
			token = handshake.getFieldValue("X-Mcp-Token");
		}
		return config.token.equals(token);
	}

	private static @Nullable String queryToken(String resourceDescriptor) {
		int q = resourceDescriptor.indexOf('?');
		if (q < 0) return null;
		String[] pairs = resourceDescriptor.substring(q + 1).split("&");
		for (String pair : pairs) {
			int eq = pair.indexOf('=');
			if (eq > 0 && "token".equals(pair.substring(0, eq))) {
				return pair.substring(eq + 1);
			}
		}
		return null;
	}

	private static String mask(String token) {
		return token == null ? "" : token.substring(0, Math.min(4, token.length())) + "...";
	}
}
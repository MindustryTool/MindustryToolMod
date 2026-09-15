package solim.mcp;

/**
 * Configuration for the Solim MCP debug server.
 *
 * <p>Read from system properties; enabled by default:
 *
 * <ul>
 *   <li>{@code solim.mcp.enabled} - "true" enables the server (default "true")
 *   <li>{@code solim.mcp.host} - bind host (default "127.0.0.1")
 *   <li>{@code solim.mcp.port} - WebSocket bind port (default 8754)
 *   <li>{@code solim.mcp.httpPort} - optional HTTP fallback port; 0 disables (default 0)
 *   <li>{@code solim.mcp.token} - connection token; if unset or empty, no auth is required
 *   <li>{@code solim.mcp.pollMs} - subscription sampling interval in ms (default 500)
 * </ul>
 */
public final class McpConfig {
	public final boolean enabled;
	public final String host;
	public final int port;
	public final int httpPort;
	public final String token;
	public final long pollMs;

	/**
	 * Test/embedding constructor. A {@code port} or {@code httpPort} of {@code 0} binds an ephemeral
	 * port; {@code pollMs} must be positive. Value validation is performed by {@link
	 * #fromProperties()}.
	 */
	public McpConfig(boolean enabled, String host, int port, int httpPort, String token, long pollMs) {
		this.enabled = enabled;
		this.host = host;
		this.port = port;
		this.httpPort = httpPort;
		this.token = token != null ? token : "";
		this.pollMs = pollMs > 0 ? pollMs : 500L;
	}

	public static McpConfig fromProperties() {
		boolean enabled = Boolean.parseBoolean(System.getProperty("solim.mcp.enabled", "true"));
		String host = System.getProperty("solim.mcp.host", "127.0.0.1");
		int port = parsePort(System.getProperty("solim.mcp.port", "8754"));
		int httpPort = parsePort(System.getProperty("solim.mcp.httpPort", "0"));
		long pollMs = parseLong(System.getProperty("solim.mcp.pollMs", "500"));
		String token = System.getProperty("solim.mcp.token", "");
		return new McpConfig(enabled, host, port, httpPort, token, pollMs);
	}

	private static long parseLong(String raw) {
		try {
			long value = Long.parseLong(raw);
			return value > 0 ? value : 500L;
		} catch (NumberFormatException e) {
			return 500L;
		}
	}

	private static int parsePort(String raw) {
		try {
			int port = Integer.parseInt(raw);
			if (port < 0 || port > 65535) {
				throw new IllegalArgumentException("port out of range: " + port);
			}
			return port;
		} catch (NumberFormatException e) {
			throw new IllegalArgumentException("invalid port: " + raw, e);
		}
	}

	/** Whether the server is enabled and should bind a socket. */
	public boolean isEnabled() {
		return enabled;
	}

	/**
	 * The connection token, or empty string if no authentication is required.
	 */
	public String token() {
		return token;
	}
}
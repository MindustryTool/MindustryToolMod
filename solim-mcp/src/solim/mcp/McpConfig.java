package solim.mcp;

public final class McpConfig {
    public final boolean enabled;
    public final String host;
    public final int port;
    public final int httpPort;
    public final String token;
    public final long pollMs;

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

    public boolean isEnabled() {
        return enabled;
    }

    public String token() {
        return token;
    }
}

package solim.mcp;

import arc.util.Log;
import arc.util.Nullable;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import solim.mcp.introspection.SnapshotRoot;
import solim.mcp.rpc.JsonRpcHandler;
import solim.mcp.tools.ToolRegistry;
import solim.mcp.transport.HttpFallbackServer;
import solim.mcp.transport.McpWebSocketServer;
import solim.mcp.transport.SubscriptionPoller;

public final class SolimMcpServer {
    private final McpConfig config;
    private final SnapshotRoot snapshotRoot;
    private final ToolRegistry tools;
    private final JsonRpcHandler handler;
    private final McpWebSocketServer webSocket;
    private final @Nullable HttpFallbackServer httpFallback;
    private final ScheduledExecutorService scheduler;
    private volatile boolean running = false;

    private SolimMcpServer(McpConfig config, SnapshotRoot snapshotRoot) {
        this.config = config;
        this.snapshotRoot = snapshotRoot;
        this.tools = new ToolRegistry(snapshotRoot);
        this.handler = new JsonRpcHandler(tools);
        this.webSocket = new McpWebSocketServer(config, handler);
        this.httpFallback = config.httpPort > 0 ? new HttpFallbackServer(config, handler) : null;
        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "solim-mcp-poller");
            t.setDaemon(true);
            return t;
        });
    }

    public static @Nullable SolimMcpServer start(McpConfig config) {
        return start(config, SnapshotRoot.sceneRoot());
    }

    public static @Nullable SolimMcpServer start(McpConfig config, SnapshotRoot snapshotRoot) {
        if (!config.isEnabled()) {
            Log.info("[solim-mcp] disabled; no server started (set solim.mcp.enabled=true to opt in)");
            return null;
        }
        SolimMcpServer server = new SolimMcpServer(config, snapshotRoot);
        server.init();
        return server;
    }

    private void init() {
        try {
            webSocket.start();
            if (httpFallback != null) {
                httpFallback.start();
            }
            SubscriptionPoller poller = new SubscriptionPoller(snapshotRoot, handler.subscribers());
            scheduler.scheduleWithFixedDelay(poller, 0, config.pollMs, TimeUnit.MILLISECONDS);
            running = true;
        } catch (Exception e) {
            Log.err("solim-mcp] failed to start server", e);
            stop();
        }
    }

    public boolean isRunning() {
        return running;
    }

    public int webSocketPort() {
        return webSocket.getPort();
    }

    public int httpPort() {
        return httpFallback != null ? config.httpPort : 0;
    }

    public String token() {
        return config.token;
    }

    public void stop() {
        running = false;
        try {
            webSocket.stop(200);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        if (httpFallback != null) {
            httpFallback.close();
        }
        handler.subscribers().clear();
        scheduler.shutdownNow();
    }
}

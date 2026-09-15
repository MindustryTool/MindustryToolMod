package solim.mcp.transport;

import arc.util.Log;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import solim.mcp.McpConfig;
import solim.mcp.rpc.JsonRpcHandler;
import java.net.InetAddress;

/**
 * Minimal HTTP/1.1 fallback for one-off MCP queries, implemented with plain sockets so the module
 * stays portable. POST {@code /mcp} with a JSON-RPC body returns a JSON-RPC response. Requires the
 * configured token via {@code Authorization: Bearer <token>} or {@code X-Mcp-Token} header.
 */
public final class HttpFallbackServer implements AutoCloseable {
	private final McpConfig config;
	private final JsonRpcHandler handler;
	private final AtomicBoolean running = new AtomicBoolean(false);
	private final ExecutorService executor = Executors.newCachedThreadPool(r -> {
		Thread t = new Thread(r, "solim-mcp-http");
		t.setDaemon(true);
		return t;
	});
	private ServerSocket serverSocket;

	public HttpFallbackServer(McpConfig config, JsonRpcHandler handler) {
		this.config = config;
		this.handler = handler;
	}

	public void start() throws IOException {
		serverSocket = new ServerSocket(config.httpPort, 16, InetAddress.getByName(config.host));
		serverSocket.setReuseAddress(true);
		running.set(true);
		Thread acceptor = new Thread(this::acceptLoop, "solim-mcp-http-acceptor");
		acceptor.setDaemon(true);
		acceptor.start();
		Log.info("[solim-mcp] HTTP fallback listening on {0}:{1}", config.host, config.httpPort);
	}

	private void acceptLoop() {
		while (running.get()) {
			try {
				Socket socket = serverSocket.accept();
				executor.execute(() -> handle(socket));
			} catch (IOException e) {
				if (running.get()) {
					Log.warn("[solim-mcp] HTTP accept failed: {0}", e.getMessage());
				}
			}
		}
	}

	private void handle(Socket socket) {
		try (Socket s = socket;
			InputStream in = s.getInputStream();
			BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8));
			OutputStream out = s.getOutputStream()) {

			String requestLine = reader.readLine();
			if (requestLine == null) return;
			String[] parts = requestLine.split(" ");
			if (parts.length < 3 || !"POST".equals(parts[0])) {
				writeResponse(out, 405, "{\"error\":\"method not allowed\"}");
				return;
			}

			String token = null;
			int contentLength = 0;
			String line;
			while ((line = reader.readLine()) != null && !line.isEmpty()) {
				int colon = line.indexOf(':');
				if (colon < 0) continue;
				String name = line.substring(0, colon).trim().toLowerCase();
				String value = line.substring(colon + 1).trim();
				if ("content-length".equals(name)) {
					contentLength = Integer.parseInt(value);
				} else if ("authorization".equals(name)) {
					if (value.regionMatches(true, 0, "bearer ", 0, 7)) {
						token = value.substring(7);
					}
				} else if ("x-mcp-token".equals(name)) {
					token = value;
				}
			}

			if (contentLength <= 0) {
				writeResponse(out, 400, "{\"error\":\"empty request\"}");
				return;
			}

			char[] body = new char[contentLength];
			int read = 0;
			while (read < contentLength) {
				int r = reader.read(body, read, contentLength - read);
				if (r < 0) break;
				read += r;
			}

			if (!config.token.isEmpty() && !config.token.equals(token)) {
				writeResponse(out, 401, "{\"error\":\"unauthorized\"}");
				return;
			}

			String response = handler.handle(new String(body, 0, read), null);
			writeResponse(out, 200, response != null ? response : "{}");
		} catch (Exception e) {
			Log.warn("[solim-mcp] HTTP request failed: {0}", e.getMessage());
		}
	}

	private void writeResponse(OutputStream out, int status, String body) throws IOException {
		String reason = status == 200 ? "OK" : status == 401 ? "Unauthorized" : status == 405 ? "Method Not Allowed" : "Bad Request";
		String headers = "HTTP/1.1 " + status + " " + reason + "\r\n"
			+ "Content-Type: application/json\r\n"
			+ "Content-Length: " + body.length() + "\r\n"
			+ "Connection: close\r\n\r\n";
		out.write((headers + body).getBytes(StandardCharsets.UTF_8));
		out.flush();
	}

	@Override
	public void close() {
		running.set(false);
		if (serverSocket != null) {
			try {
				serverSocket.close();
			} catch (IOException ignored) {
			}
		}
		executor.shutdownNow();
	}
}

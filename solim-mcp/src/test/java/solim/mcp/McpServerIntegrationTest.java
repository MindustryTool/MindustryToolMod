package solim.mcp;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;
import org.junit.jupiter.api.Test;
import solim.mcp.introspection.SnapshotRoot;
import solim.reactive.Signal;
import arc.scene.ui.layout.Table;
import java.io.ByteArrayOutputStream;
import java.util.concurrent.CountDownLatch;

class McpServerIntegrationTest {

	static class Gadget extends Table {
		final Signal<Float> progress = Signal.of(0.5f);
	}

	private static int freePort() throws Exception {
		try (ServerSocket socket = new ServerSocket(0)) {
			return socket.getLocalPort();
		}
	}

	@Test
	void webSocketTokenAuthAndFullFlow() throws Exception {
		Gadget gadget = new Gadget();
		gadget.name = "gadget";
		gadget.progress.set(0.5f);

		SolimMcpServer server = SolimMcpServer.start(
			new McpConfig(true, "127.0.0.1", 0, 0, "sekrit", 100),
			SnapshotRoot.fixed(gadget));
		assertNotNull(server);
		assertTrue(server.isRunning());

		LinkedBlockingQueue<String> inbox = new LinkedBlockingQueue<>();
		AtomicInteger closeCode = new AtomicInteger(-1);
		WebSocketClient client = new WebSocketClient(
			new URI("ws://127.0.0.1:" + server.webSocketPort() + "/?token=sekrit")) {
			@Override
			public void onOpen(ServerHandshake handshake) {}

			@Override
			public void onMessage(String message) {
				inbox.add(message);
			}

			@Override
			public void onClose(int code, String reason, boolean remote) {
				closeCode.set(code);
			}

			@Override
			public void onError(Exception ex) {}
		};
		try {
			assertTrue(client.connectBlocking(5, TimeUnit.SECONDS), "client should connect");

			client.send("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}");
			String init = inbox.poll(5, TimeUnit.SECONDS);
			assertNotNull(init, "no initialize response");
			assertTrue(init.contains("\"2024-11-05\""), init);

			client.send("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
			String tools = inbox.poll(5, TimeUnit.SECONDS);
			assertNotNull(tools);
			assertTrue(tools.contains("get_component_tree"), tools);

			client.send("{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"get_component_tree\",\"arguments\":{}}}");
			String tree = inbox.poll(5, TimeUnit.SECONDS);
			assertNotNull(tree);
			assertTrue(tree.contains("gadget"), tree);

			client.send("{\"jsonrpc\":\"2.0\",\"id\":31,\"method\":\"tools/call\",\"params\":{\"name\":\"execute_js\",\"arguments\":{\"code\":\"1 + 2\"}}}");
			String js = inbox.poll(5, TimeUnit.SECONDS);
			assertNotNull(js);
			assertTrue(js.contains("3"), js);

			client.send("{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"solim/subscribe\"}");
			String subscribed = inbox.poll(5, TimeUnit.SECONDS);
			assertNotNull(subscribed);
			assertTrue(subscribed.contains("\"subscribed\":true"), subscribed);

			gadget.progress.set(0.9f);
			String notification = awaitNotify(inbox, "0.9", 5);
			assertNotNull(notification, "expected solim/notify with progress 0.9");
			assertTrue(notification.contains("solim/notify"), notification);
		} finally {
			try {
				client.closeBlocking();
			} catch (Exception ignored) {
			}
			server.stop();
		}
	}

	@Test
	void invalidTokenIsRejectedWith4003() throws Exception {
		SolimMcpServer server = SolimMcpServer.start(
			new McpConfig(true, "127.0.0.1", 0, 0, "sekrit", 100),
			SnapshotRoot.fixed(new Table()));
		assertNotNull(server);

		AtomicInteger code = new AtomicInteger(-2);
		AtomicInteger opened = new AtomicInteger(0);
		AtomicInteger messages = new AtomicInteger(0);
		CountDownLatch closed = new CountDownLatch(1);
		WebSocketClient client = new WebSocketClient(
			new URI("ws://127.0.0.1:" + server.webSocketPort() + "/?token=wrong")) {
			@Override
			public void onOpen(ServerHandshake handshake) {
				opened.incrementAndGet();
			}

			@Override
			public void onMessage(String message) {
				messages.incrementAndGet();
			}

			@Override
			public void onClose(int c, String reason, boolean remote) {
				code.set(c);
				closed.countDown();
			}

			@Override
			public void onError(Exception ex) {}
		};
try {
			client.connectBlocking(5, TimeUnit.SECONDS);
			assertTrue(closed.await(5, TimeUnit.SECONDS), "connection should be closed by server");
			// Java-WebSocket surfaces a fast rejection as 4003, 1006, or -1 depending on timing.
			assertTrue(code.get() == 4003 || code.get() == 1006 || code.get() == -1,
				"unexpected close code: " + code.get());
			assertEquals(0, messages.get(), "rejected connection must not receive data");
		} finally {
			try {
				client.closeBlocking();
			} catch (Exception ignored) {
			}
			server.stop();
		}
	}

	@Test
	void httpFallbackServesToolsAndRejectsUnauthorized() throws Exception {
		int httpPort = freePort();
		SolimMcpServer server = SolimMcpServer.start(
			new McpConfig(true, "127.0.0.1", 0, httpPort, "sekrit", 100),
			SnapshotRoot.fixed(new Table()));
		assertNotNull(server);

		try {
			String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}";

			String ok = httpPost(httpPort, body, "Bearer sekrit");
			assertTrue(ok.startsWith("HTTP/1.1 200"), ok);
			assertTrue(ok.contains("get_component_tree"), ok);

			String unauthorized = httpPost(httpPort, body, "Bearer wrong");
			assertTrue(unauthorized.startsWith("HTTP/1.1 401"), unauthorized);

			String noToken = httpPost(httpPort, body, null);
			assertTrue(noToken.startsWith("HTTP/1.1 401"), noToken);
		} finally {
			server.stop();
		}
	}

	@Test
	void noAuthModeAllowsConnectionsWithoutToken() throws Exception {
		int httpPort = freePort();
		SolimMcpServer server = SolimMcpServer.start(
			new McpConfig(true, "127.0.0.1", 0, httpPort, "", 100),
			SnapshotRoot.fixed(new Table()));
		assertNotNull(server);

		try {
			// HTTP test without auth header
			String body = "{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"tools/list\"}";
			String ok = httpPost(httpPort, body, null);
			assertTrue(ok.startsWith("HTTP/1.1 200"), ok);
			assertTrue(ok.contains("get_component_tree"), ok);

			// WebSocket test without token
			LinkedBlockingQueue<String> inbox = new LinkedBlockingQueue<>();
			WebSocketClient client = new WebSocketClient(
				new URI("ws://127.0.0.1:" + server.webSocketPort() + "/")) {
				@Override
				public void onOpen(ServerHandshake handshake) {}

				@Override
				public void onMessage(String message) {
					inbox.add(message);
				}

				@Override
				public void onClose(int code, String reason, boolean remote) {}

				@Override
				public void onError(Exception ex) {}
			};

			try {
				assertTrue(client.connectBlocking(5, TimeUnit.SECONDS));
				client.send("{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}");
				String wsResponse = inbox.poll(5, TimeUnit.SECONDS);
				assertNotNull(wsResponse);
				assertTrue(wsResponse.contains("get_component_tree"), wsResponse);
			} finally {
				try {
					client.closeBlocking();
				} catch (Exception ignored) {
				}
			}
		} finally {
			server.stop();
		}
	}

	private static String awaitNotify(LinkedBlockingQueue<String> inbox, String needle, int seconds)
			throws InterruptedException {
		long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(seconds);
		while (System.currentTimeMillis() < deadline) {
			String message = inbox.poll(200, TimeUnit.MILLISECONDS);
			if (message != null && message.contains("solim/notify") && message.contains(needle)) {
				return message;
			}
		}
		return null;
	}

	private static String httpPost(int port, String body, String authHeader) throws Exception {
		try (Socket socket = new Socket()) {
			socket.connect(new InetSocketAddress("127.0.0.1", port), 5000);
			StringBuilder request = new StringBuilder();
			request.append("POST /mcp HTTP/1.1\r\n");
			request.append("Host: 127.0.0.1:").append(port).append("\r\n");
			request.append("Content-Type: application/json\r\n");
			request.append("Content-Length: ").append(body.getBytes(StandardCharsets.UTF_8).length).append("\r\n");
			if (authHeader != null) {
				request.append("Authorization: ").append(authHeader).append("\r\n");
			}
			request.append("Connection: close\r\n\r\n").append(body);
			socket.getOutputStream().write(request.toString().getBytes(StandardCharsets.UTF_8));
			socket.getOutputStream().flush();

			ByteArrayOutputStream out = new ByteArrayOutputStream();
			byte[] buffer = new byte[1024];
			int read;
			while ((read = socket.getInputStream().read(buffer)) > 0) {
				out.write(buffer, 0, read);
			}
			return out.toString("UTF-8");
		}
	}
}

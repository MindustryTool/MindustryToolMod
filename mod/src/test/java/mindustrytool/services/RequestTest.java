package mindustrytool.services;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import mindustrytool.services.auth.AuthProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class RequestTest extends SolimEnv {

	private static HttpServer server;
	private static int serverPort;

	@BeforeAll
	static void startServer() throws IOException {
		server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
		server.createContext("/test-get", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] response = "hello world".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "text/plain");
				exchange.getResponseHeaders().set("X-Custom-Header", "test-val");
				exchange.sendResponseHeaders(200, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});
		server.createContext("/test-post", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] reqBytes = Request.readAllBytes(exchange.getRequestBody());
				String body = new String(reqBytes, StandardCharsets.UTF_8);
				String auth = exchange.getRequestHeaders().getFirst("Authorization");
				String echo = "echo:" + body + "|auth:" + auth;
				byte[] response = echo.getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});
		server.createContext("/test-lines", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] response = "line1\nline2\nline3".getBytes(StandardCharsets.UTF_8);
				exchange.sendResponseHeaders(200, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});
		server.createContext("/test-error", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] response = "{\"error\":\"not found\"}".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				exchange.sendResponseHeaders(404, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});
		server.createContext("/test-500-text", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] response = "Internal Server Error".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(500, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});
		server.createContext("/test-502-html", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				byte[] response = "<html><body>502 Bad Gateway</body></html>".getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "text/html");
				exchange.sendResponseHeaders(502, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});
		server.createContext("/test-empty-error", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				exchange.sendResponseHeaders(400, -1);
			}
		});
		server.createContext("/test-query", new HttpHandler() {
			@Override
			public void handle(HttpExchange exchange) throws IOException {
				String query = exchange.getRequestURI().getQuery();
				byte[] response = (query != null ? query : "").getBytes(StandardCharsets.UTF_8);
				exchange.getResponseHeaders().set("Content-Type", "text/plain");
				exchange.sendResponseHeaders(200, response.length);
				try (OutputStream os = exchange.getResponseBody()) {
					os.write(response);
				}
			}
		});
		server.setExecutor(null);
		server.start();
		serverPort = server.getAddress().getPort();
	}

	@AfterAll
	static void stopServer() {
		if (server != null) {
			server.stop(0);
		}
	}

	@Test
	void testUrlResolution() {
		assertEquals("https://api.com/v1/users", Request.RequestBuilder.resolveUrl("https://api.com/v1", "users"));
		assertEquals("https://api.com/v1/users", Request.RequestBuilder.resolveUrl("https://api.com/v1/", "/users"));
		assertEquals("https://api.com/v1/users", Request.RequestBuilder.resolveUrl("https://api.com/v1/", "users"));
		assertEquals("https://api.com/v1/users", Request.RequestBuilder.resolveUrl("https://api.com/v1", "/users"));
		assertEquals("https://api.com/v1?page=1", Request.RequestBuilder.resolveUrl("https://api.com/v1", "?page=1"));
		assertEquals(
				"https://other.com/data",
				Request.RequestBuilder.resolveUrl("https://api.com/v1", "https://other.com/data"));
		assertEquals("users", Request.RequestBuilder.resolveUrl(null, "users"));
	}

	@Test
	void testGetRequest() throws Exception {
		Request client = Request.builder()
				.baseUrl("http://127.0.0.1:" + serverPort)
				.timeout(Duration.ofSeconds(5))
				.build();

		Request.Response<String> res = client.get("/test-get").sendAsync().get();
		assertEquals(200, res.statusCode());
		assertEquals("hello world", res.body());
		assertEquals("test-val", res.header("X-Custom-Header"));
	}

	@Test
	void testGetByteArray() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<byte[]> res = client.get("/test-get")
				.sendAsync(Request.BodyHandlers.ofByteArray())
				.get();
		assertEquals(200, res.statusCode());
		assertArrayEquals("hello world".getBytes(StandardCharsets.UTF_8), res.body());
	}

	@Test
	void testGetLines() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<Stream<String>> res = client.get("/test-lines")
				.sendAsync(Request.BodyHandlers.ofLines())
				.get();
		assertEquals(200, res.statusCode());
		assertEquals("line1,line2,line3", res.body().collect(Collectors.joining(",")));
	}

	@Test
	void testPostJsonWithAuth() throws Exception {
		AtomicInteger refreshCalls = new AtomicInteger();
		AuthProvider authProvider = new AuthProvider() {
			@Override
			public CompletableFuture<Void> refreshIfNeeded() {
				refreshCalls.incrementAndGet();
				return CompletableFuture.completedFuture(null);
			}

			@Override
			public String getAccessToken() {
				return "secret-token";
			}
		};

		Request client = Request.builder()
				.baseUrl("http://127.0.0.1:" + serverPort)
				.authProvider(authProvider)
				.build();

		Request.Response<String> res =
				client.post("/test-post").json("{\"foo\":\"bar\"}").sendAsync().get();

		assertEquals(200, res.statusCode());
		assertEquals("echo:{\"foo\":\"bar\"}|auth:Bearer secret-token", res.body());
		assertEquals(1, refreshCalls.get());
	}

	@Test
	void testPostWithoutAuth() throws Exception {
		AuthProvider authProvider = new AuthProvider() {
			@Override
			public CompletableFuture<Void> refreshIfNeeded() {
				return CompletableFuture.completedFuture(null);
			}

			@Override
			public String getAccessToken() {
				return "secret-token";
			}
		};

		Request client = Request.builder()
				.baseUrl("http://127.0.0.1:" + serverPort)
				.authProvider(authProvider)
				.build();

		Request.Response<String> res = client.post("/test-post")
				.withoutAuth()
				.body("raw-data")
				.sendAsync()
				.get();

		assertEquals(200, res.statusCode());
		assertEquals("echo:raw-data|auth:null", res.body());
	}

	@Test
	void testErrorResponse() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		ExecutionException ex =
				assertThrows(ExecutionException.class, () -> client.get("/test-error").sendAsync().get());
		assertTrue(ex.getCause() instanceof HttpException);
		HttpException httpEx = (HttpException) ex.getCause();
		assertEquals(404, httpEx.statusCode());
		assertNotNull(httpEx.errorBody());
		assertEquals("not found", httpEx.errorBody().get("error").asText());
		assertEquals("{\"error\":\"not found\"}", httpEx.rawBody());
	}

	@Test
	void testErrorResponseTextFallback() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		ExecutionException ex =
				assertThrows(ExecutionException.class, () -> client.get("/test-500-text").sendAsync().get());
		assertTrue(ex.getCause() instanceof HttpException);
		HttpException httpEx = (HttpException) ex.getCause();
		assertEquals(500, httpEx.statusCode());
		assertNotNull(httpEx.errorBody());
		assertTrue(httpEx.errorBody().isTextual());
		assertEquals("Internal Server Error", httpEx.errorBody().asText());
		assertEquals("Internal Server Error", httpEx.rawBody());
	}

	@Test
	void testErrorResponseHtmlFallback() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		ExecutionException ex =
				assertThrows(ExecutionException.class, () -> client.get("/test-502-html").sendAsync().get());
		assertTrue(ex.getCause() instanceof HttpException);
		HttpException httpEx = (HttpException) ex.getCause();
		assertEquals(502, httpEx.statusCode());
		assertNotNull(httpEx.errorBody());
		assertTrue(httpEx.errorBody().isTextual());
		assertEquals("<html><body>502 Bad Gateway</body></html>", httpEx.errorBody().asText());
		assertEquals("<html><body>502 Bad Gateway</body></html>", httpEx.rawBody());
	}

	@Test
	void testEmptyErrorResponse() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		ExecutionException ex =
				assertThrows(ExecutionException.class, () -> client.get("/test-empty-error").sendAsync().get());
		assertTrue(ex.getCause() instanceof HttpException);
		HttpException httpEx = (HttpException) ex.getCause();
		assertEquals(400, httpEx.statusCode());
		assertNull(httpEx.errorBody());
		assertTrue(httpEx.rawBody() == null || httpEx.rawBody().isEmpty());
	}

	@Test
	void testMultipartBodyBuilder() {
		byte[] body = Request.buildMultipartBody(
				"test-boundary", "file-content".getBytes(StandardCharsets.UTF_8), "file.txt", "abc123hash");
		String text = new String(body, StandardCharsets.UTF_8);
		assertTrue(text.contains("--test-boundary"));
		assertTrue(text.contains("name=\"hash\""));
		assertTrue(text.contains("abc123hash"));
		assertTrue(text.contains("name=\"file\"; filename=\"file.txt\""));
		assertTrue(text.contains("file-content"));
	}

	@Test
	void testSingleQueryStringParam() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<String> res = client.get("/test-query").query("page", 1).sendAsync().get();
		assertEquals(200, res.statusCode());
		assertEquals("page=1", res.body());
	}

	@Test
	void testMultipleQueryParams() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<String> res = client.get("/test-query")
				.query("page", 1)
				.query("size", 20)
				.sendAsync()
				.get();
		assertEquals(200, res.statusCode());
		assertEquals("page=1&size=20", res.body());
	}

	@Test
	void testRepeatedQueryKey() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<String> res = client.get("/test-query")
				.query("tags", "a")
				.query("tags", "b")
				.sendAsync()
				.get();
		assertEquals(200, res.statusCode());
		assertEquals("tags=a&tags=b", res.body());
	}

	@Test
	void testListQueryParam() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<String> res = client.get("/test-query")
				.query("tags", Arrays.asList("a", "b"))
				.sendAsync()
				.get();
		assertEquals(200, res.statusCode());
		assertEquals("tags=a&tags=b", res.body());
	}

	@Test
	void testMapQueryReplaces() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<String> res = client.get("/test-query")
				.query("tags", "a")
				.query(Collections.singletonMap("tags", "b"))
				.sendAsync()
				.get();
		assertEquals(200, res.statusCode());
		assertEquals("tags=b", res.body());
	}

	@Test
	void testNullQueryKeySkipped() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<String> res = client.get("/test-query")
				.query(null, "value")
				.sendAsync()
				.get();
		assertEquals(200, res.statusCode());
		assertEquals("", res.body());
	}

	@Test
	void testNullStringValueSkipped() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<String> res = client.get("/test-query")
				.query("sort", (String) null)
				.sendAsync()
				.get();
		assertEquals(200, res.statusCode());
		assertEquals("", res.body());
	}

	@Test
	void testEmptyStringValueSkipped() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<String> res = client.get("/test-query")
				.query("sort", "")
				.sendAsync()
				.get();
		assertEquals(200, res.statusCode());
		assertEquals("", res.body());
	}

	@Test
	void testQueryUrlEncoding() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<String> res = client.get("/test-query")
				.query("query", "hello world")
				.sendAsync()
				.get();
		assertEquals(200, res.statusCode());
		assertTrue(res.body().contains("query=hello+world") || res.body().contains("query=hello%20world"));
	}

	@Test
	void testQueryAppendedToExistingQueryString() throws Exception {
		Request client =
				Request.builder().baseUrl("http://127.0.0.1:" + serverPort).build();

		Request.Response<String> res = client.get("/test-query?page=0")
				.query("sort", "newest")
				.sendAsync()
				.get();
		assertEquals(200, res.statusCode());
		assertEquals("page=0&sort=newest", res.body());
	}

	@Test
	void testBuildMultipartFormData() {
		String boundary = "testboundary123";
		byte[] fileContent = "dummy png data".getBytes(StandardCharsets.UTF_8);
		byte[] body = Request.buildMultipartFormData(boundary, "image", "sample.png", "image/png", fileContent);

		String bodyString = new String(body, StandardCharsets.UTF_8);
		assertTrue(bodyString.contains("--" + boundary));
		assertTrue(bodyString.contains("name=\"image\"; filename=\"sample.png\""));
		assertTrue(bodyString.contains("Content-Type: image/png"));
		assertTrue(bodyString.contains("dummy png data"));
		assertTrue(bodyString.endsWith("--" + boundary + "--\r\n"));
	}
}

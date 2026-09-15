package solim.mcp.rpc;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.ui.layout.Table;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.mcp.introspection.SnapshotRoot;
import solim.mcp.tools.ToolRegistry;
import java.util.concurrent.atomic.AtomicBoolean;

class JsonRpcHandlerTest {

	private final ObjectMapper mapper = new ObjectMapper();
	private JsonRpcHandler handler;
	private SubscriptionSink sink;

	@BeforeEach
	void setUp() {
		Table root = new Table();
		root.name = "root";
		handler = new JsonRpcHandler(new ToolRegistry(SnapshotRoot.fixed(root)));
		sink = json -> {};
	}

	@Test
	void initializeAdvertisesProtocol() throws Exception {
		String response = handler.handle("{\"jsonrpc\":\"2.0\",\"id\":1,\"method\":\"initialize\"}", sink);
		JsonNode node = mapper.readTree(response);
		assertEquals(1, node.path("id").asInt());
		assertEquals("2024-11-05", node.path("result").path("protocolVersion").asText());
		assertEquals("solim-mcp-debug", node.path("result").path("serverInfo").path("name").asText());
	}

	@Test
	void initializedNotificationProducedNoResponse() {
		assertNull(handler.handle("{\"jsonrpc\":\"2.0\",\"method\":\"notifications/initialized\"}", sink));
	}

	@Test
	void pingReturnsEmptyResult() throws Exception {
		JsonNode node = mapper.readTree(handler.handle("{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"ping\"}", sink));
		assertEquals(9, node.path("id").asInt());
		assertNotNull(node.path("result"));
	}

	@Test
	void toolsListContainsAllTools() throws Exception {
		JsonNode node = mapper.readTree(handler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":2,\"method\":\"tools/list\"}", sink));
		JsonNode tools = node.path("result").path("tools");
		boolean foundTree = false;
		boolean foundJs = false;
		boolean foundClick = false;
		boolean foundFind = false;
		boolean foundStop = false;
		boolean foundScreenshot = false;
		boolean foundPerf = false;
		for (JsonNode tool : tools) {
			String name = tool.path("name").asText();
			if ("get_component_tree".equals(name)) foundTree = true;
			if ("execute_js".equals(name)) foundJs = true;
			if ("click_element".equals(name)) foundClick = true;
			if ("find_elements".equals(name)) foundFind = true;
			if ("stop".equals(name)) foundStop = true;
			if ("take_screenshot".equals(name)) foundScreenshot = true;
			if ("get_ui_perf_metrics".equals(name)) foundPerf = true;
		}
		assertTrue(foundTree);
		assertTrue(foundJs);
		assertTrue(foundClick);
		assertTrue(foundFind);
		assertTrue(foundStop);
		assertTrue(foundScreenshot);
		assertTrue(foundPerf);
	}

	@Test
	void toolsCallComponentTree() throws Exception {
		String response = handler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":3,\"method\":\"tools/call\",\"params\":{\"name\":\"get_component_tree\",\"arguments\":{}}}", sink);
		JsonNode node = mapper.readTree(response);
		assertEquals(3, node.path("id").asInt());
		String text = node.path("result").path("content").path(0).path("text").asText();
		assertTrue(text.contains("\"root\""), text);
	}

	@Test
	void toolsCallStop() throws Exception {
		AtomicBoolean called = new AtomicBoolean(false);
		Table root = new Table();
		JsonRpcHandler customHandler = new JsonRpcHandler(
			new ToolRegistry(SnapshotRoot.fixed(root), () -> called.set(true)));

		String response = customHandler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":30,\"method\":\"tools/call\",\"params\":{\"name\":\"stop\",\"arguments\":{}}}", sink);
		JsonNode node = mapper.readTree(response);
		assertEquals(30, node.path("id").asInt());
		String text = node.path("result").path("content").path(0).path("text").asText();
		assertTrue(text.contains("\"stopped\":true"), text);
		assertTrue(called.get());

		// Test alias stop_mindustry
		called.set(false);
		String aliasResp = customHandler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":31,\"method\":\"tools/call\",\"params\":{\"name\":\"stop_mindustry\",\"arguments\":{}}}", sink);
		JsonNode aliasNode = mapper.readTree(aliasResp);
		assertEquals(31, aliasNode.path("id").asInt());
		assertTrue(called.get());
	}

	@Test
	void toolsCallUnknownToolIsError() throws Exception {
		JsonNode node = mapper.readTree(handler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":4,\"method\":\"tools/call\",\"params\":{\"name\":\"nope\"}}", sink));
		assertEquals(-32601, node.path("error").path("code").asInt());
	}

	@Test
	void toolsCallToolErrorIsReported() throws Exception {
		JsonNode node = mapper.readTree(handler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":5,\"method\":\"tools/call\",\"params\":{\"name\":\"get_layout\",\"arguments\":{}}}", sink));
		assertEquals(-32000, node.path("error").path("code").asInt());
		assertTrue(node.path("error").path("message").asText().contains("target"));
	}

	@Test
	void unknownMethodIsError() throws Exception {
		JsonNode node = mapper.readTree(handler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":6,\"method\":\"wat\"}", sink));
		assertEquals(-32601, node.path("error").path("code").asInt());
	}

	@Test
	void parseError() throws Exception {
		JsonNode node = mapper.readTree(handler.handle("{not json", sink));
		assertEquals(-32700, node.path("error").path("code").asInt());
	}

	@Test
	void subscribeRequiresConnection() throws Exception {
		JsonNode node = mapper.readTree(handler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"solim/subscribe\"}", (SubscriptionSink) null));
		assertEquals(-32602, node.path("error").path("code").asInt());
	}

	@Test
	void subscribeAndUnsubscribe() throws Exception {
		JsonNode ok = mapper.readTree(handler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":8,\"method\":\"solim/subscribe\"}", sink));
		assertTrue(ok.path("result").path("subscribed").asBoolean());
		assertTrue(handler.subscribers().contains(sink));

		JsonNode unsub = mapper.readTree(handler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":9,\"method\":\"solim/unsubscribe\"}", sink));
		assertFalse(unsub.path("result").path("subscribed").asBoolean());
		assertFalse(handler.subscribers().contains(sink));
	}
}

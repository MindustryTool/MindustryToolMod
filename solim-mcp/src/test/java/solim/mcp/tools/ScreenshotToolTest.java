package solim.mcp.tools;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.ui.layout.Table;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.junit.jupiter.api.Test;
import solim.mcp.introspection.SnapshotRoot;
import solim.mcp.rpc.JsonRpcHandler;

class ScreenshotToolTest {

	private final ScreenshotTool tool = new ScreenshotTool();
	private final ObjectMapper mapper = new ObjectMapper();

	@Test
	void schemaDescribesOptionalArgs() {
		ObjectNode schema = tool.inputSchema();
		assertEquals("object", schema.path("type").asText());
		JsonNode props = schema.path("properties");
		assertTrue(props.has("maxWidth"), "schema must document maxWidth");
		assertTrue(props.has("maxBytes"), "schema must document maxBytes");
		assertTrue(props.has("x"), "schema must document x");
		assertTrue(props.has("y"), "schema must document y");
		assertTrue(props.has("width"), "schema must document width");
		assertTrue(props.has("height"), "schema must document height");
		assertTrue(schema.path("required").isMissingNode(), "all args must be optional");
	}

	@Test
	void toolName() {
		assertEquals("take_screenshot", tool.name());
	}

	@Test
	void partialRegionRejected() {
		ObjectNode args = JsonNodeFactory.instance.objectNode();
		args.put("x", 10);
		args.put("y", 20);
		McpTool.MCPException e = assertThrows(McpTool.MCPException.class, () -> tool.execute(args));
		assertTrue(e.getMessage().contains("together"), e.getMessage());
	}

	@Test
	void nonPositiveRegionRejected() {
		ObjectNode args = JsonNodeFactory.instance.objectNode();
		args.put("x", 0);
		args.put("y", 0);
		args.put("width", 0);
		args.put("height", 100);
		assertThrows(McpTool.MCPException.class, () -> tool.execute(args));
	}

	@Test
	void nonIntegerRegionRejected() {
		ObjectNode args = JsonNodeFactory.instance.objectNode();
		args.put("x", 0);
		args.put("y", 0);
		args.put("width", 100);
		args.put("height", "tall");
		assertThrows(McpTool.MCPException.class, () -> tool.execute(args));
	}

	@Test
	void headlessCaptureFailsWithRunningGameMessage() {
		McpTool.MCPException e = assertThrows(McpTool.MCPException.class,
			() -> tool.execute(JsonNodeFactory.instance.objectNode()));
		assertTrue(e.getMessage().contains("running game"), e.getMessage());
	}

	@Test
	void clampRegionInside() {
		assertArrayEquals(new int[]{10, 20, 100, 50}, ScreenshotTool.clampRegion(10, 20, 100, 50, 800, 600));
	}

	@Test
	void clampRegionPartial() {
		assertArrayEquals(new int[]{0, 0, 50, 50}, ScreenshotTool.clampRegion(-10, -10, 60, 60, 800, 600));
		assertArrayEquals(new int[]{700, 500, 100, 100}, ScreenshotTool.clampRegion(700, 500, 200, 200, 800, 600));
	}

	@Test
	void clampRegionOutsideReturnsNull() {
		assertNull(ScreenshotTool.clampRegion(900, 700, 100, 100, 800, 600));
		assertNull(ScreenshotTool.clampRegion(0, 0, 0, 50, 800, 600));
	}

	@Test
	void shrinkDimsPreservesAspectAndFloor() {
		int[] dims = ScreenshotTool.shrinkDims(1280, 720);
		assertEquals(960, dims[0]);
		assertEquals(540, dims[1]);
		int[] floored = ScreenshotTool.shrinkDims(400, 300);
		assertEquals(ScreenshotTool.MIN_WIDTH, floored[0]);
	}

	@Test
	void fitWidthPreservesAspect() {
		int[] dims = ScreenshotTool.fitWidth(1920, 1080, 1280);
		assertEquals(1280, dims[0]);
		assertEquals(720, dims[1]);
	}

	@Test
	void registryListsScreenshot() {
		ToolRegistry registry = new ToolRegistry(SnapshotRoot.fixed(new Table()));
		assertNotNull(registry.get("take_screenshot"));
		boolean found = false;
		for (JsonNode def : registry.list()) {
			if ("take_screenshot".equals(def.path("name").asText())) {
				found = true;
			}
		}
		assertTrue(found, "tools/list must advertise take_screenshot");
	}

	@Test
	void toolsCallHeadlessReturnsError() throws Exception {
		Table root = new Table();
		root.name = "root";
		JsonRpcHandler handler = new JsonRpcHandler(new ToolRegistry(SnapshotRoot.fixed(root)));
		String response = handler.handle(
			"{\"jsonrpc\":\"2.0\",\"id\":7,\"method\":\"tools/call\","
				+ "\"params\":{\"name\":\"take_screenshot\",\"arguments\":{}}}",
			json -> {});
		JsonNode node = mapper.readTree(response);
		assertEquals(7, node.path("id").asInt());
		assertTrue(node.path("error").path("message").asText().contains("running game"),
			"headless screenshot call must fail cleanly: " + response);
	}
}

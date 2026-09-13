package solim.mcp.tools;

import arc.Core;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.ui.Button;
import arc.scene.ui.Label;
import arc.util.Nullable;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import solim.mcp.introspection.SnapshotRoot;
import solim.mcp.introspection.UiSnapshot;

/**
 * Dispatches a programmatic click event to a target UI element on the main UI thread.
 */
public final class ClickElementTool implements McpTool {
	private final SnapshotRoot snapshotRoot;

	public ClickElementTool(SnapshotRoot snapshotRoot) {
		this.snapshotRoot = snapshotRoot;
	}

	@Override
	public String name() {
		return "click_element";
	}

	@Override
	public String description() {
		return "Finds a UI element by name, class, or label text and triggers a click event on the main game thread.";
	}

	@Override
	public ObjectNode inputSchema() {
		ObjectNode schema = JsonNodeFactory.instance.objectNode();
		schema.put("type", "object");
		ObjectNode props = schema.putObject("properties");
		props.putObject("target")
			.put("type", "string")
			.put("description", "Element name, class name, or label text to click.");
		schema.putArray("required").add("target");
		return schema;
	}

	@Override
	public ObjectNode execute(ObjectNode args) throws MCPException {
		if (!args.hasNonNull("target")) {
			throw new MCPException("Missing required argument: 'target'");
		}
		String target = args.get("target").asText();

		Element root = snapshotRoot.get();
		Element element = UiSnapshot.lookup(root, target);
		if (element == null) {
			element = findClickable(root, target);
		}
		if (element == null) {
			throw new MCPException("No element matches '" + target + "'");
		}

		final Element targetElement = element;
		if (Core.app != null) {
			CompletableFuture<Void> future = new CompletableFuture<>();
			Core.app.post(() -> {
				try {
					targetElement.fireClick();
					future.complete(null);
				} catch (Throwable t) {
					future.completeExceptionally(t);
				}
			});
			try {
				future.get(5, TimeUnit.SECONDS);
			} catch (TimeoutException e) {
				throw new MCPException("Click timed out after 5 seconds");
			} catch (Exception e) {
				throw new MCPException("Click failed: " + e.getMessage());
			}
		} else {
			targetElement.fireClick();
		}

		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("clicked", true);
		out.put("target", target);
		out.put("elementClass", targetElement.getClass().getName());
		return out;
	}

	private static @Nullable Element findClickable(@Nullable Element root, String target) {
		if (Core.app != null) {
			CompletableFuture<Element> future = new CompletableFuture<>();
			Core.app.post(() -> {
				try {
					Element rootEl = root != null ? root : (Core.scene != null ? Core.scene.root : null);
					future.complete(searchClickable(rootEl, target, 0, new IdentityHashMap<>()));
				} catch (Throwable t) {
					future.completeExceptionally(t);
				}
			});
			try {
				return future.get(5, TimeUnit.SECONDS);
			} catch (Exception e) {
				return null;
			}
		} else {
			Element rootEl = root != null ? root : (Core.scene != null ? Core.scene.root : null);
			return searchClickable(rootEl, target, 0, new IdentityHashMap<>());
		}
	}

	private static @Nullable Element searchClickable(@Nullable Element element, String target, int depth, Map<Element, Boolean> visited) {
		if (element == null || visited.put(element, Boolean.TRUE) != null || depth > 32) return null;

		if (element instanceof Label) {
			String text = ((Label) element).getText().toString();
			if (target.equalsIgnoreCase(text.trim())) {
				if (element.parent instanceof Button) {
					return element.parent;
				}
				return element;
			}
		}

		if (element instanceof Group) {
			for (Element child : ((Group) element).getChildren()) {
				Element found = searchClickable(child, target, depth + 1, visited);
				if (found != null) return found;
			}
		}
		return null;
	}
}

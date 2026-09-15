package solim.mcp.tools;

import arc.Core;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Stops and exits the running Mindustry instance.
 */
public final class StopTool implements McpTool {
	private final Runnable exitAction;

	public StopTool() {
		this(StopTool::defaultExit);
	}

	public StopTool(Runnable exitAction) {
		this.exitAction = exitAction;
	}

	@Override
	public String name() {
		return "stop";
	}

	@Override
	public String description() {
		return "Stops and exits the running Mindustry instance.";
	}

	@Override
	public ObjectNode inputSchema() {
		ObjectNode schema = JsonNodeFactory.instance.objectNode();
		schema.put("type", "object");
		schema.putObject("properties");
		return schema;
	}

	@Override
	public ObjectNode execute(ObjectNode args) throws MCPException {
		if (exitAction != null) {
			exitAction.run();
		}

		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("stopped", true);
		out.put("message", "Stopping Mindustry...");
		return out;
	}

	private static void defaultExit() {
		Thread thread = new Thread(() -> {
			try {
				Thread.sleep(100);
			} catch (InterruptedException ignored) {
			}
			if (Core.app != null) {
				Core.app.exit();
				try {
					Thread.sleep(2000);
				} catch (InterruptedException ignored) {
				}
				System.exit(0);
			}
		}, "mindustry-stop-thread");
		thread.setDaemon(true);
		thread.start();
	}
}

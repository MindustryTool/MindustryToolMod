package solim.mcp.tools;

import arc.Core;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import mindustry.Vars;

/**
 * Executes arbitrary JavaScript code via Mindustry's Rhino script engine.
 */
public final class ExecuteJsTool implements McpTool {

	@Override
	public String name() {
		return "execute_js";
	}

	@Override
	public String description() {
		return "Executes JavaScript code in the live Mindustry Rhino environment and returns the evaluated result.";
	}

	@Override
	public ObjectNode inputSchema() {
		ObjectNode schema = JsonNodeFactory.instance.objectNode();
		schema.put("type", "object");
		ObjectNode props = schema.putObject("properties");
		props.putObject("code")
			.put("type", "string")
			.put("description", "JavaScript code to evaluate in the Rhino runtime.");
		schema.putArray("required").add("code");
		return schema;
	}

	@Override
	public ObjectNode execute(ObjectNode args) throws MCPException {
		if (!args.hasNonNull("code")) {
			throw new MCPException("Missing required argument: 'code'");
		}
		String code = args.get("code").asText();

		String result;
		boolean success = true;

		try {
			result = evaluate(code);
		} catch (MCPException e) {
			throw e;
		} catch (Throwable t) {
			result = "Execution failed: " + (t.getMessage() != null ? t.getMessage() : t.getClass().getSimpleName());
			success = false;
		}

		ObjectNode out = JsonNodeFactory.instance.objectNode();
		out.put("result", result != null ? result : "null");
		out.put("success", success);
		return out;
	}

	private String evaluate(String code) throws Exception {
		// If Mindustry mods and scripts are available, use Mindustry's Scripts environment
		if (Vars.mods != null && Vars.mods.getScripts() != null) {
			if (Core.app != null) {
				CompletableFuture<String> future = new CompletableFuture<>();
				Core.app.post(() -> {
					boolean entered = false;
					try {
						if (rhino.Context.getCurrentContext() == null) {
							rhino.Context.enter();
							entered = true;
						}
						future.complete(Vars.mods.getScripts().runConsole(code));
					} catch (Throwable t) {
						future.completeExceptionally(t);
					} finally {
						if (entered) {
							rhino.Context.exit();
						}
					}
				});
				try {
					return future.get(5, TimeUnit.SECONDS);
				} catch (TimeoutException e) {
					throw new MCPException("Script execution timed out after 5 seconds");
				}
			} else {
				return Vars.mods.getScripts().runConsole(code);
			}
		}

		// Fallback: direct Rhino context execution (e.g. during headless testing)
		try {
			rhino.Context context = rhino.Context.enter();
			try {
				rhino.Scriptable scope = context.initSafeStandardObjects();
				Object evaluated = context.evaluateString(scope, code, "console.js", 1);
				if (evaluated instanceof rhino.NativeJavaObject) {
					evaluated = ((rhino.NativeJavaObject) evaluated).unwrap();
				}
				if (evaluated instanceof rhino.Undefined) {
					return "undefined";
				}
				return String.valueOf(evaluated);
			} finally {
				rhino.Context.exit();
			}
		} catch (Throwable t) {
			return "Rhino error: " + t.getMessage();
		}
	}
}

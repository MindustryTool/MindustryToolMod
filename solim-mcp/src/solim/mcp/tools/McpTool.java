package solim.mcp.tools;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A read-only MCP tool backed by the reflection-based introspectors.
 *
 * <p>All tools accept and return jackson JSON nodes; errors are signaled by {@link
 * MCPException}.
 */
public interface McpTool {
	/** Stable tool name used by the MCP client, e.g. {@code get_component_tree}. */
	String name();

	/** Human-readable description for toolkit discovery. */
	String description();

	/** JSON schema of the tool arguments. */
	ObjectNode inputSchema();

	/** Executes the tool and returns a JSON result node. */
	ObjectNode execute(ObjectNode args) throws MCPException;

	/** Raised for user-facing tool errors (bad input, missing element, etc.). */
	class MCPException extends Exception {
		public MCPException(String message) {
			super(message);
		}
	}
}
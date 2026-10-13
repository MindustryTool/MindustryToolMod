package solim.mcp.tools;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * A read-only MCP tool backed by the reflection-based introspectors.
 *
 * <p>
 * All tools accept and return jackson JSON nodes; errors are signaled by {@link
 * MCPException}.
 */
public interface McpTool {

    String name();

    String description();

    ObjectNode inputSchema();

    ObjectNode execute(ObjectNode args) throws MCPException;

    class MCPException extends Exception {
        public MCPException(String message) {
            super(message);
        }
    }
}

package solim.mcp.tools;

import com.fasterxml.jackson.databind.node.ObjectNode;

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

package solim.mcp.rpc;

/** Receives outbound JSON-RPC messages (notifications) from the server. */
public interface SubscriptionSink {
	/** Sends a JSON-RPC message to the connected client. */
	void send(String json);
}
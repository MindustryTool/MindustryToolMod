package solim.mcp.rpc;

public interface SubscriptionSink {

    void send(String json);
}

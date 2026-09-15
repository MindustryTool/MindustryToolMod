package solim.mcp.transport;

import arc.util.Log;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import solim.mcp.introspection.SnapshotRoot;
import solim.mcp.introspection.UiSnapshot;
import solim.mcp.rpc.SubscriptionSink;

/**
 * Samples live UI state on a fixed interval and pushes {@code solim/notify} notifications to
 * subscribers when discovered signal values change or the tree shape changes. Sampling instead of
 * per-signal subscription keeps the server decoupled and cheap.
 */
public final class SubscriptionPoller implements Runnable {
	private final SnapshotRoot root;
	private final Set<SubscriptionSink> subscribers;
	private final ObjectMapper mapper = new ObjectMapper();
	/** Last delivered signal snapshot signature per subscriber. */
	private final Map<SubscriptionSink, String> lastBySink = new ConcurrentHashMap<>();

	public SubscriptionPoller(SnapshotRoot root, Set<SubscriptionSink> subscribers) {
		this.root = root;
		this.subscribers = subscribers;
	}

	@Override
	public void run() {
		if (subscribers.isEmpty()) return;
		UiSnapshot snapshot;
		try {
			snapshot = UiSnapshot.capture(root.get());
		} catch (RuntimeException e) {
			Log.warn("[solim-mcp] poll failed: {0}", e.getMessage());
			return;
		}
		String signature = snapshot.signature() + "|" + snapshot.signalsJson().toString();
		for (SubscriptionSink sink : subscribers) {
			String previous = lastBySink.get(sink);
			if (signature.equals(previous)) continue;
			lastBySink.put(sink, signature);
			send(sink, snapshot);
		}
	}

	private void send(SubscriptionSink sink, UiSnapshot snapshot) {
		ObjectNode notification = mapper.createObjectNode();
		notification.put("jsonrpc", "2.0");
		notification.put("method", "solim/notify");
		ObjectNode params = notification.putObject("params");
		params.put("treeChanged", true);
		params.set("signals", snapshot.signalsJson());
		sink.send(notification.toString());
	}
}
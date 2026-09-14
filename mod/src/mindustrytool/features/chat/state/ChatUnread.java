package mindustrytool.features.chat.state;

import arc.util.Nullable;
import java.util.HashMap;
import java.util.Map;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

public final class ChatUnread {

    private final Signal<Map<String, Integer>> unreads = Signal.of(new HashMap<>());
    private final Map<String, Readable<Integer>> channelComputeds = new HashMap<>();
    private final Computed<Integer> total;

    public ChatUnread() {
        this.total = new Computed<>(() -> {
            Map<String, Integer> map = unreads.get();
            int sum = 0;
            for (int count : map.values()) {
                sum += count;
            }
            return sum;
        });
    }

    public Readable<Integer> total() {
        return total;
    }

    public int currentTotal() {
        return total.peek();
    }

    public Readable<Integer> forChannel(@Nullable String channelId) {
        if (channelId == null) {
            return Readable.of(0);
        }
        Readable<Integer> existing = channelComputeds.get(channelId);
        if (existing == null) {
            existing = unreads.map(map -> map.getOrDefault(channelId, 0));
            channelComputeds.put(channelId, existing);
        }
        return existing;
    }

    public int get(@Nullable String channelId) {
        return channelId != null ? unreads.peek().getOrDefault(channelId, 0) : 0;
    }

    public void increment(@Nullable String channelId) {
        if (channelId == null) return;
        Map<String, Integer> next = new HashMap<>(unreads.peek());
        next.put(channelId, next.getOrDefault(channelId, 0) + 1);
        unreads.set(next);
    }

    public void clear(@Nullable String channelId) {
        if (channelId == null) return;
        Map<String, Integer> next = new HashMap<>(unreads.peek());
        if (next.containsKey(channelId) && next.get(channelId) == 0) {
            return;
        }
        next.put(channelId, 0);
        unreads.set(next);
    }

    public void clearAll() {
        Map<String, Integer> current = unreads.peek();
        boolean hasNonZero = false;
        Map<String, Integer> next = new HashMap<>();
        for (Map.Entry<String, Integer> entry : current.entrySet()) {
            if (entry.getValue() != 0) {
                hasNonZero = true;
            }
            next.put(entry.getKey(), 0);
        }
        if (hasNonZero) {
            unreads.set(next);
        }
    }
}

package mindustrytool.features.chat.state;

import arc.Core;
import arc.util.Nullable;
import java.util.HashMap;
import java.util.Map;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

public final class ChatUnread {

    public static final String SETTING_PREFIX = "mindustrytool.chat.lastread.";

    private final Signal<Map<String, Integer>> unreads = Signal.of(new HashMap<>());
    private final Signal<Map<String, String>> latestMessageIds = Signal.of(new HashMap<>());
    private final Signal<Map<String, String>> readMessageIds = Signal.of(new HashMap<>());
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

    public static int compareMessageIds(@Nullable String id1, @Nullable String id2) {
        return id1 == null && id2 == null ? 0
                : id1 == null ? -1
                : id2 == null ? 1
                : id1.compareTo(id2);
    }

    public static boolean isNewer(@Nullable String latestId, @Nullable String readId) {
        if (latestId == null || latestId.isEmpty() || latestId.startsWith("temp_")) {
            return false;
        }
        return readId == null || readId.isEmpty() ? true : latestId.compareTo(readId) > 0;
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

    public @Nullable String getLatestMessageId(@Nullable String channelId) {
        return channelId != null ? latestMessageIds.peek().get(channelId) : null;
    }

    public @Nullable String getReadMessageId(@Nullable String channelId) {
        if (channelId == null) return null;
        String inMemory = readMessageIds.peek().get(channelId);
        if (inMemory != null) {
            return inMemory;
        }
        String fromSettings = Core.settings != null ? Core.settings.getString(SETTING_PREFIX + channelId, null) : null;
        if (fromSettings != null) {
            Map<String, String> next = new HashMap<>(readMessageIds.peek());
            next.put(channelId, fromSettings);
            readMessageIds.set(next);
        }
        return fromSettings;
    }

    public void setLatestMessage(@Nullable String channelId, @Nullable String messageId) {
        if (channelId == null || messageId == null || messageId.isEmpty() || messageId.startsWith("temp_")) {
            return;
        }

        String currentLatest = latestMessageIds.peek().get(channelId);
        if (currentLatest != null && messageId.compareTo(currentLatest) <= 0) {
            return;
        }

        Map<String, String> nextLatest = new HashMap<>(latestMessageIds.peek());
        nextLatest.put(channelId, messageId);
        latestMessageIds.set(nextLatest);

        String readId = getReadMessageId(channelId);
        if (isNewer(messageId, readId)) {
            Map<String, Integer> nextUnreads = new HashMap<>(unreads.peek());
            int current = nextUnreads.getOrDefault(channelId, 0);
            nextUnreads.put(channelId, Math.max(1, current));
            unreads.set(nextUnreads);
        }
    }

    public void markAsRead(@Nullable String channelId) {
        if (channelId == null) return;
        String latest = latestMessageIds.peek().get(channelId);
        markAsRead(channelId, latest);
    }

    public void markAsRead(@Nullable String channelId, @Nullable String messageId) {
        if (channelId == null) return;

        String idToPersist = messageId != null ? messageId : latestMessageIds.peek().get(channelId);
        if (idToPersist != null && !idToPersist.startsWith("temp_")) {
            String currentRead = getReadMessageId(channelId);
            if (currentRead == null || idToPersist.compareTo(currentRead) > 0) {
                if (Core.settings != null) {
                    Core.settings.put(SETTING_PREFIX + channelId, idToPersist);
                }
                Map<String, String> nextRead = new HashMap<>(readMessageIds.peek());
                nextRead.put(channelId, idToPersist);
                readMessageIds.set(nextRead);
            }
        }

        Map<String, Integer> nextUnreads = new HashMap<>(unreads.peek());
        if (nextUnreads.containsKey(channelId) && nextUnreads.get(channelId) == 0) {
            return;
        }
        nextUnreads.put(channelId, 0);
        unreads.set(nextUnreads);
    }

    public void increment(@Nullable String channelId) {
        if (channelId == null) return;
        Map<String, Integer> next = new HashMap<>(unreads.peek());
        next.put(channelId, next.getOrDefault(channelId, 0) + 1);
        unreads.set(next);
    }

    public void clear(@Nullable String channelId) {
        markAsRead(channelId);
    }

    public void clearAll() {
        for (String channelId : latestMessageIds.peek().keySet()) {
            markAsRead(channelId);
        }
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

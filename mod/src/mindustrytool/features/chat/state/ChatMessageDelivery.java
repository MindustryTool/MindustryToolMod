package mindustrytool.features.chat.state;

import arc.util.Nullable;
import java.util.HashMap;
import java.util.Map;
import mindustrytool.features.chat.MessageStatus;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class ChatMessageDelivery {

    private final Signal<Map<String, MessageStatus>> statuses = Signal.of(new HashMap<>());
    private final Map<String, Readable<MessageStatus>> statusComputeds = new HashMap<>();

    public Readable<MessageStatus> status(@Nullable String messageId) {
        if (messageId == null) {
            return Readable.of(null);
        }
        Readable<MessageStatus> existing = statusComputeds.get(messageId);
        if (existing == null) {
            existing = statuses.map(map -> map.get(messageId));
            statusComputeds.put(messageId, existing);
        }
        return existing;
    }

    public Readable<Boolean> isPending(@Nullable String messageId) {
        return status(messageId).map(s -> s == MessageStatus.PENDING);
    }

    public Readable<Boolean> isFailed(@Nullable String messageId) {
        return status(messageId).map(s -> s == MessageStatus.FAILED);
    }

    public boolean isPendingDirect(@Nullable String messageId) {
        return messageId != null && statuses.peek().get(messageId) == MessageStatus.PENDING;
    }

    public boolean isFailedDirect(@Nullable String messageId) {
        return messageId != null && statuses.peek().get(messageId) == MessageStatus.FAILED;
    }

    public void markPending(@Nullable String messageId) {
        if (messageId == null) return;
        Map<String, MessageStatus> next = new HashMap<>(statuses.peek());
        next.put(messageId, MessageStatus.PENDING);
        statuses.set(next);
    }

    public void markFailed(@Nullable String messageId) {
        if (messageId == null) return;
        Map<String, MessageStatus> next = new HashMap<>(statuses.peek());
        next.put(messageId, MessageStatus.FAILED);
        statuses.set(next);
    }

    public void clear(@Nullable String messageId) {
        if (messageId == null) return;
        Map<String, MessageStatus> next = new HashMap<>(statuses.peek());
        if (next.remove(messageId) != null) {
            statuses.set(next);
        }
    }

    public void clearAll() {
        if (!statuses.peek().isEmpty()) {
            statuses.set(new HashMap<>());
        }
    }
}

package mindustrytool.features.chat.state;

import arc.util.Nullable;
import mindustrytool.features.chat.MessageStatus;
import solim.reactive.MapSignal;
import solim.reactive.Readable;

public final class ChatMessageDelivery {

    private final MapSignal<String, MessageStatus> delivery = MapSignal.of();

    public Readable<MessageStatus> status(@Nullable String messageId) {
        return messageId == null ? Readable.of(null) : delivery.readable(messageId);
    }

    public Readable<Boolean> isPending(@Nullable String messageId) {
        return status(messageId).map(s -> s == MessageStatus.PENDING);
    }

    public Readable<Boolean> isFailed(@Nullable String messageId) {
        return status(messageId).map(s -> s == MessageStatus.FAILED);
    }

    public boolean isPendingDirect(@Nullable String messageId) {
        return messageId != null && delivery.peek().get(messageId) == MessageStatus.PENDING;
    }

    public boolean isFailedDirect(@Nullable String messageId) {
        return messageId != null && delivery.peek().get(messageId) == MessageStatus.FAILED;
    }

    public void markPending(@Nullable String messageId) {
        if (messageId == null) return;
        delivery.put(messageId, MessageStatus.PENDING);
    }

    public void markFailed(@Nullable String messageId) {
        if (messageId == null) return;
        delivery.put(messageId, MessageStatus.FAILED);
    }

    public void clear(@Nullable String messageId) {
        if (messageId == null) return;
        delivery.remove(messageId);
    }

    public void clearAll() {
        delivery.clear();
    }
}

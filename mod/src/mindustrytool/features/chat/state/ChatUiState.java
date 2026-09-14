package mindustrytool.features.chat.state;

import arc.util.Nullable;
import java.util.Objects;
import mindustrytool.models.response.ChatMessage;
import solim.signal.Readable;
import solim.signal.Signal;

public final class ChatUiState {

    private final Signal<ChatMessage> replyTarget = Signal.of(null);
    private final Signal<String> expandedMessageId = Signal.of(null);
    private final Signal<String> translatingMessageId = Signal.of(null);

    public Readable<ChatMessage> replyTarget() {
        return replyTarget;
    }

    public @Nullable ChatMessage currentReplyTarget() {
        return replyTarget.peek();
    }

    public void setReplyTarget(@Nullable ChatMessage target) {
        replyTarget.set(target);
    }

    public Readable<String> expandedMessageId() {
        return expandedMessageId;
    }

    public @Nullable String currentExpandedMessageId() {
        return expandedMessageId.peek();
    }

    public void setExpandedMessageId(@Nullable String messageId) {
        expandedMessageId.set(messageId);
    }

    public void toggleExpanded(@Nullable String messageId) {
        if (Objects.equals(expandedMessageId.peek(), messageId)) {
            expandedMessageId.set(null);
        } else {
            expandedMessageId.set(messageId);
        }
    }

    public Readable<String> translatingMessageId() {
        return translatingMessageId;
    }

    public @Nullable String currentTranslatingMessageId() {
        return translatingMessageId.peek();
    }

    public void setTranslatingMessageId(@Nullable String messageId) {
        translatingMessageId.set(messageId);
    }

    public void clearActiveSelection() {
        replyTarget.set(null);
        expandedMessageId.set(null);
    }
}

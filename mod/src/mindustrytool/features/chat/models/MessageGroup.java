package mindustrytool.features.chat.models;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class MessageGroup {

    private final @Nullable String authorId;
    private final @Nullable String createdAt;
    private final List<ParsedChatMessage> messages;

    public MessageGroup(
            @Nullable String authorId,
            @Nullable String createdAt,
            List<ParsedChatMessage> messages) {
        this.authorId = authorId;
        this.createdAt = createdAt;
        this.messages = Collections.unmodifiableList(new ArrayList<>(messages));
    }

    public @Nullable String getAuthorId() {
        return authorId;
    }

    public @Nullable String getCreatedAt() {
        return createdAt;
    }

    public List<ParsedChatMessage> getMessages() {
        return messages;
    }

    public int getMessageCount() {
        return messages.size();
    }

    public ParsedChatMessage getMessage(int index) {
        return messages.get(index);
    }

    public String getFirstMessageId() {
        return messages.isEmpty() ? "" : messages.get(0).getId();
    }

    public String getLastMessageId() {
        return messages.isEmpty() ? "" : messages.get(messages.size() - 1).getId();
    }

    public String getKey() {
        return getFirstMessageId() + "_" + messages.size() + "_" + getLastMessageId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        MessageGroup that = (MessageGroup) o;
        return Objects.equals(getKey(), that.getKey())
                && Objects.equals(authorId, that.authorId)
                && Objects.equals(createdAt, that.createdAt);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getKey(), authorId, createdAt);
    }
}

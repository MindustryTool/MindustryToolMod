package mindustrytool.features.chat.models;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Composite domain model grouping consecutive chat messages from the same author.
 *
 * <p>A group renders as a single {@code VirtualList} item: one shared 48px avatar on the left
 * with all messages stacked in the right-hand column.
 */
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

    /**
     * Creation time of the first message in the group, used for the group header timestamp.
     */
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

    /**
     * Stable reconciliation key for {@code VirtualList}.
     *
     * <p>Includes the message count and last message ID so appending a message to the active
     * group changes the key and triggers a structural update.
     */
    public String getKey() {
        return getFirstMessageId() + "_" + messages.size() + "_" + getLastMessageId();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
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

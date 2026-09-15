package mindustrytool.features.chat;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import arc.util.Nullable;
import mindustrytool.features.chat.models.MessageGroup;
import mindustrytool.features.chat.models.ParsedChatMessage;
import mindustrytool.models.response.ChatMessage;

/**
 * Groups consecutive chat messages from the same author into composite {@link MessageGroup}
 * items for rendering.
 */
public final class ChatMessageGrouper {

    private ChatMessageGrouper() {
    }

    public static List<MessageGroup> groupRaw(List<ChatMessage> rawMessages) {
        if (rawMessages == null || rawMessages.isEmpty()) {
            return Collections.emptyList();
        }
        List<ParsedChatMessage> parsed = new ArrayList<>(rawMessages.size());
        for (ChatMessage m : rawMessages) {
            parsed.add(ChatMessageParser.parse(m));
        }
        return group(parsed);
    }

    public static List<MessageGroup> group(List<ParsedChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return Collections.emptyList();
        }
        List<MessageGroup> result = new ArrayList<>();
        List<ParsedChatMessage> current = new ArrayList<>();
        @Nullable String currentAuthor = null;

        for (ParsedChatMessage cur : messages) {
            String author = cur.getCreatedBy();
            if (!current.isEmpty() && !Objects.equals(author, currentAuthor)) {
                result.add(toGroup(currentAuthor, current));
                current = new ArrayList<>();
            }
            currentAuthor = author;
            current.add(cur);
        }
        if (!current.isEmpty()) {
            result.add(toGroup(currentAuthor, current));
        }
        return result;
    }

    private static MessageGroup toGroup(@Nullable String authorId, List<ParsedChatMessage> messages) {
        String createdAt = messages.isEmpty() ? null : messages.get(0).getCreatedAt();
        return new MessageGroup(authorId, createdAt, messages);
    }
}

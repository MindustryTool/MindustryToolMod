package mindustrytool.features.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.Collections;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mindustrytool.features.chat.models.MessageGroup;
import mindustrytool.features.chat.models.ParsedChatMessage;
import mindustrytool.features.chat.models.ParsedChatMessage.TextMessage;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.test.MindustryTestEnv;

class ChatCacheBoundsTest extends MindustryTestEnv {

    @BeforeEach
    void resetCaches() {
        ChatMessageParser.clearCache();
        ChatMessageHeightCalculator.clearCache();
    }

    @AfterEach
    void clearCaches() {
        ChatMessageParser.clearCache();
        ChatMessageHeightCalculator.clearCache();
    }

    @Test
    void parserCacheStaysAtItsBoundAfterOverInsert() {
        for (int i = 0; i < ChatMessageParser.MAX_CACHE_ENTRIES + 8; i++) {
            ChatMessageParser.parse(message(i));
        }

        assertEquals(ChatMessageParser.MAX_CACHE_ENTRIES, ChatMessageParser.cacheSize());
    }

    @Test
    void parserCacheEvictsTheLeastRecentlyUsedEntry() {
        ParsedChatMessage evicted = ChatMessageParser.parse(message(1));
        ParsedChatMessage touched = ChatMessageParser.parse(message(0));
        for (int i = 2; i < ChatMessageParser.MAX_CACHE_ENTRIES; i++) {
            ChatMessageParser.parse(message(i));
        }
        ChatMessageParser.parse(message(0));

        for (int i = ChatMessageParser.MAX_CACHE_ENTRIES; i < ChatMessageParser.MAX_CACHE_ENTRIES + 4; i++) {
            ChatMessageParser.parse(message(i));
        }

        assertSame(touched, ChatMessageParser.parse(message(0)), "Recently used entries must survive eviction");
        assertNotSame(evicted, ChatMessageParser.parse(message(1)), "Least recently used entries must be evicted");
    }

    @Test
    void heightCacheStaysAtItsBoundAfterOverInsert() {
        for (int i = 0; i < ChatMessageHeightCalculator.MAX_CACHE_ENTRIES + 8; i++) {
            ChatMessageHeightCalculator.calculateHeight(group(i), 400f);
        }

        assertEquals(ChatMessageHeightCalculator.MAX_CACHE_ENTRIES, ChatMessageHeightCalculator.cacheSize());
    }

    private static ChatMessage message(int id) {
        ChatMessage message = new ChatMessage();
        message.setId("message-" + id);
        message.setContent("content " + id);
        return message;
    }

    private static MessageGroup group(int id) {
        ChatMessage raw = message(id);
        return new MessageGroup("author", "2026-01-01T00:00:00Z",
                Collections.singletonList(new TextMessage(raw, raw.getContent(), false)));
    }
}

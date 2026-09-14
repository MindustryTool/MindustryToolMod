package mindustrytool.features.chat.state;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import mindustrytool.models.response.ChatMessage;
import org.junit.jupiter.api.Test;
import solim.signal.Signal;

class ChatMessagesTest {

    private static ChatMessage msg(String id, String channelId, String content) {
        ChatMessage m = new ChatMessage();
        m.setId(id);
        m.setChannelId(channelId);
        m.setContent(content);
        return m;
    }

    @Test
    void testInitialState() {
        Signal<String> activeId = Signal.of(null);
        ChatMessages messages = new ChatMessages(activeId);

        assertTrue(messages.active().get().isEmpty());
        assertFalse(messages.isLoadingOlder());
        assertFalse(messages.activeFullyLoaded().get());
    }

    @Test
    void testAppendAndDeduplication() {
        Signal<String> activeId = Signal.of("ch1");
        ChatMessages messages = new ChatMessages(activeId);

        ChatMessage m1 = msg("1", "ch1", "Hello");
        ChatMessage m2 = msg("2", "ch1", "World");

        assertTrue(messages.append(m1));
        assertTrue(messages.append(m2));
        assertFalse(messages.append(m1), "Duplicate append should return false");

        List<ChatMessage> list = messages.active().get();
        assertEquals(2, list.size());
        assertEquals("1", list.get(0).getId());
        assertEquals("2", list.get(1).getId());
    }

    @Test
    void testPrependMessages() {
        Signal<String> activeId = Signal.of("ch1");
        ChatMessages messages = new ChatMessages(activeId);

        messages.append(msg("3", "ch1", "Third"));
        messages.append(msg("4", "ch1", "Fourth"));

        int added = messages.prepend("ch1", Arrays.asList(
                msg("1", "ch1", "First"),
                msg("2", "ch1", "Second"),
                msg("3", "ch1", "Duplicate third")));

        assertEquals(2, added);
        List<ChatMessage> list = messages.active().get();
        assertEquals(4, list.size());
        assertEquals("1", list.get(0).getId());
        assertEquals("2", list.get(1).getId());
        assertEquals("3", list.get(2).getId());
        assertEquals("4", list.get(3).getId());
    }

    @Test
    void testConfirmReplacesTempMessage() {
        Signal<String> activeId = Signal.of("ch1");
        ChatMessages messages = new ChatMessages(activeId);

        messages.append(msg("temp_123", "ch1", "Sending..."));

        ChatMessage real = msg("server_999", "ch1", "Confirmed!");
        boolean confirmed = messages.confirm("temp_123", real);

        assertTrue(confirmed);
        List<ChatMessage> list = messages.active().get();
        assertEquals(1, list.size());
        assertEquals("server_999", list.get(0).getId());
        assertEquals("Confirmed!", list.get(0).getContent());
    }

    @Test
    void testConfirmHandlesSseRaceCondition() {
        Signal<String> activeId = Signal.of("ch1");
        ChatMessages messages = new ChatMessages(activeId);

        messages.append(msg("temp_123", "ch1", "Sending..."));
        // SSE delivers real message first before HTTP POST returns
        messages.append(msg("server_999", "ch1", "Live delivery"));

        ChatMessage real = msg("server_999", "ch1", "Live delivery");
        boolean confirmed = messages.confirm("temp_123", real);

        assertTrue(confirmed);
        List<ChatMessage> list = messages.active().get();
        assertEquals(1, list.size());
        assertEquals("server_999", list.get(0).getId());
    }

    @Test
    void testRemoveMessage() {
        Signal<String> activeId = Signal.of("ch1");
        ChatMessages messages = new ChatMessages(activeId);

        messages.append(msg("1", "ch1", "First"));
        messages.append(msg("2", "ch1", "Second"));

        assertTrue(messages.remove("ch1", "1"));
        assertFalse(messages.remove("ch1", "non_existent"));
        assertEquals(1, messages.active().get().size());
        assertEquals("2", messages.active().get().get(0).getId());
    }

    @Test
    void testSwitchActiveChannelChangesActiveMessages() {
        Signal<String> activeId = Signal.of("ch1");
        ChatMessages messages = new ChatMessages(activeId);

        messages.append(msg("1", "ch1", "In ch1"));
        messages.append(msg("2", "ch2", "In ch2"));

        assertEquals(1, messages.active().get().size());
        assertEquals("1", messages.active().get().get(0).getId());

        activeId.set("ch2");
        assertEquals(1, messages.active().get().size());
        assertEquals("2", messages.active().get().get(0).getId());

        activeId.set(null);
        assertTrue(messages.active().get().isEmpty());
    }
}

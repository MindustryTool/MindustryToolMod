package mindustrytool.features.chat.state;

import static org.junit.jupiter.api.Assertions.*;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import java.util.Arrays;
import java.util.List;
import mindustrytool.models.response.ChatMessage;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.test.SolimEnv;

class ChatMessagesTest extends SolimEnv {

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

    @Test
    void testInitialLoadingAndErrorLifecycle() {
        Signal<String> activeId = Signal.of("ch1");
        ChatMessages messages = new ChatMessages(activeId);

        assertFalse(messages.isActiveLoadingInitial());
        assertFalse(messages.activeLoadingInitial().get());
        assertNull(messages.currentActiveError());
        assertNull(messages.activeError().get());

        messages.setLoadingInitial("ch1", true);
        assertTrue(messages.isActiveLoadingInitial());
        assertTrue(messages.activeLoadingInitial().get());

        messages.setLoadingInitial("ch1", false);
        messages.setError("ch1", "Connection timed out");
        assertFalse(messages.isActiveLoadingInitial());
        assertEquals("Connection timed out", messages.currentActiveError());
        assertEquals("Connection timed out", messages.activeError().get());

        // Channel switch reflects respective channel's state
        activeId.set("ch2");
        assertFalse(messages.isActiveLoadingInitial());
        assertNull(messages.currentActiveError());

        // Replace clears error
        activeId.set("ch1");
        assertEquals("Connection timed out", messages.currentActiveError());
        messages.replace("ch1", Arrays.asList(msg("10", "ch1", "Hi")));
        assertNull(messages.currentActiveError());
        assertEquals(1, messages.active().get().size());
    }

    @Test
    void testForChannelRetainsReactivityAfterComponentRebuild() {
        Signal<String> activeId = Signal.of("ch1");
        ChatMessages chatMessages = new ChatMessages(activeId);

        // Component 1 binds to forChannel("ch1") and is then disposed
        BaseComponent comp1 = new BaseComponent() {
            @Override
            protected Element build() {
                Readable<List<ChatMessage>> binding = chatMessages.forChannel("ch1");
                assertTrue(binding.get().isEmpty());
                return new Table();
            }
        };
        comp1.element();
        comp1.dispose();

        // Component 2 mounts later and binds to forChannel("ch1")
        BaseComponent comp2 = new BaseComponent() {
            @Override
            protected Element build() {
                Readable<List<ChatMessage>> binding = chatMessages.forChannel("ch1");
                assertTrue(binding.get().isEmpty());

                // New message arrives
                chatMessages.append(msg("m1", "ch1", "Hello"));

                // Component 2's binding must reactively update
                assertEquals(1, binding.get().size());
                assertEquals("m1", binding.get().get(0).getId());
                return new Table();
            }
        };
        comp2.element();
        comp2.dispose();
    }
}


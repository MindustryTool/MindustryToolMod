package mindustrytool.features.chat;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockSettings;
import arc.scene.Element;
import java.util.Collections;
import mindustrytool.models.response.ChannelDto;
import mindustrytool.test.MindustryTestEnv;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Signal;

class ChatChannelListViewTest extends MindustryTestEnv {

    private ChatStore store;

    @BeforeEach
    void setUp() {
        Core.settings = new MockSettings();
        Signal<String> activeSignal = Signal.of("ch1");
        Signal<Boolean> channelsCol = Signal.of(false);
        Signal<Boolean> usersCol = Signal.of(false);
        store = new ChatStore(activeSignal, channelsCol, usersCol);
    }

    @Test
    void testChannelItemBuildsAndBindsMuteToggle() {
        ChannelDto ch = new ChannelDto();
        ch.setId("ch1");
        ch.setName("general");
        store.channels().replace(Collections.singletonList(ch));

        ChatChannelListView view = new ChatChannelListView(store);
        Element element = view.element();
        assertNotNull(element);

        assertFalse(store.notifications().isCurrentlyMuted("ch1"));

        store.notifications().toggleMuted("ch1");
        assertTrue(store.notifications().isCurrentlyMuted("ch1"));

        flushEffects();
        view.dispose();
    }

    @Test
    void testUnreadDotSuppressedWhenMuted() {
        ChannelDto ch = new ChannelDto();
        ch.setId("ch2");
        ch.setName("spam");
        store.channels().replace(Collections.singletonList(ch));

        store.unread().increment("ch2");
        assertEquals(1, store.unread().get("ch2"));
        assertEquals(1, store.unread().total().get());

        // Mute ch2
        store.notifications().setMuted("ch2", true);
        assertEquals(0, store.unread().total().get());

        ChatChannelListView view = new ChatChannelListView(store);
        Element element = view.element();
        assertNotNull(element);

        flushEffects();
        view.dispose();
    }
}

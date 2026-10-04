package mindustrytool.features.chat.state;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockSettings;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.core.BaseComponent;
import solim.reactive.Readable;
import solim.test.SolimEnv;

class ChatUnreadTest extends SolimEnv {

    @BeforeEach
    void setUp() {
        Core.settings = new MockSettings();
    }

    @Test
    void testInitialState() {
        ChatUnread unread = new ChatUnread();
        assertEquals(0, unread.currentTotal());
        assertEquals(0, unread.total().get());
        assertEquals(0, unread.get("ch1"));
        assertEquals(0, unread.forChannel("ch1").get());
    }

    @Test
    void testUuidV7Comparison() {
        // Earlier timestamp UUIDv7 vs later timestamp UUIDv7
        String earlierId = "01944800-0000-7000-8000-000000000001";
        String laterId = "01944900-0000-7000-8000-000000000001";

        assertTrue(ChatUnread.isNewer(laterId, earlierId));
        assertFalse(ChatUnread.isNewer(earlierId, laterId));
        assertFalse(ChatUnread.isNewer(earlierId, earlierId));

        // Temporary optimistic ID should never be considered newer
        assertFalse(ChatUnread.isNewer("temp_1234", earlierId));
        assertFalse(ChatUnread.isNewer(null, earlierId));

        // Fresh launch / no read ID: any real latest ID is considered newer
        assertTrue(ChatUnread.isNewer(earlierId, null));
        assertTrue(ChatUnread.isNewer(earlierId, ""));

        // compareMessageIds
        assertTrue(ChatUnread.compareMessageIds(laterId, earlierId) > 0);
        assertTrue(ChatUnread.compareMessageIds(earlierId, laterId) < 0);
        assertEquals(0, ChatUnread.compareMessageIds(earlierId, earlierId));
        assertEquals(0, ChatUnread.compareMessageIds(null, null));
    }

    @Test
    void testFreshLaunchWithoutStoredReadId() {
        ChatUnread unread = new ChatUnread();
        String msgId = "01944800-0000-7000-8000-000000000001";

        unread.setLatestMessage("ch1", msgId);

        assertEquals(1, unread.get("ch1"));
        assertEquals(1, unread.forChannel("ch1").get());
        assertEquals(1, unread.total().get());
    }

    @Test
    void testIncomingNewerMessageUpdatesUnread() {
        ChatUnread unread = new ChatUnread();
        String msg1 = "01944800-0000-7000-8000-000000000001";
        String msg2 = "01944900-0000-7000-8000-000000000002";

        unread.setLatestMessage("ch1", msg1);
        unread.markAsRead("ch1", msg1);

        assertEquals(0, unread.get("ch1"));
        assertEquals(msg1, Core.settings.getString(ChatUnread.SETTING_PREFIX + "ch1"));

        // When newer message arrives
        unread.setLatestMessage("ch1", msg2);
        assertEquals(1, unread.get("ch1"));
        assertEquals(1, unread.total().get());
    }

    @Test
    void testOlderMessageDoesNotMarkUnread() {
        ChatUnread unread = new ChatUnread();
        String msg1 = "01944800-0000-7000-8000-000000000001";
        String msg2 = "01944900-0000-7000-8000-000000000002";

        // User already read up to msg2
        Core.settings.put(ChatUnread.SETTING_PREFIX + "ch1", msg2);

        // Older message msg1 is received
        unread.setLatestMessage("ch1", msg1);
        assertEquals(0, unread.get("ch1"));
        assertEquals(0, unread.total().get());
    }

    @Test
    void testMarkAsReadWithoutExplicitIdUsesLatest() {
        ChatUnread unread = new ChatUnread();
        String msg1 = "01944800-0000-7000-8000-000000000001";

        unread.setLatestMessage("ch1", msg1);
        assertEquals(1, unread.get("ch1"));

        unread.markAsRead("ch1");
        assertEquals(0, unread.get("ch1"));
        assertEquals(msg1, Core.settings.getString(ChatUnread.SETTING_PREFIX + "ch1"));
    }

    @Test
    void testIncrementAndTotal() {
        ChatUnread unread = new ChatUnread();
        unread.increment("ch1");
        unread.increment("ch1");
        unread.increment("ch2");

        assertEquals(2, unread.get("ch1"));
        assertEquals(2, unread.forChannel("ch1").get());
        assertEquals(1, unread.get("ch2"));
        assertEquals(1, unread.forChannel("ch2").get());
        assertEquals(3, unread.total().get());
        assertEquals(3, unread.currentTotal());
    }

    @Test
    void testClearChannel() {
        ChatUnread unread = new ChatUnread();
        unread.increment("ch1");
        unread.increment("ch2");
        assertEquals(2, unread.total().get());

        unread.clear("ch1");
        assertEquals(0, unread.get("ch1"));
        assertEquals(0, unread.forChannel("ch1").get());
        assertEquals(1, unread.get("ch2"));
        assertEquals(1, unread.total().get());
    }

    @Test
    void testClearAll() {
        ChatUnread unread = new ChatUnread();
        unread.increment("ch1");
        unread.increment("ch2");
        unread.increment("ch3");
        assertEquals(3, unread.total().get());

        unread.clearAll();
        assertEquals(0, unread.total().get());
        assertEquals(0, unread.get("ch1"));
        assertEquals(0, unread.get("ch2"));
        assertEquals(0, unread.get("ch3"));
    }

    @Test
    void testForChannelRetainsReactivityAfterComponentRebuild() {
        ChatUnread unread = new ChatUnread();

        // Simulate component 1 building and obtaining a reactive binding
        BaseComponent comp1 = new BaseComponent() {
            @Override
            protected Element build() {
                Readable<Integer> binding = unread.forChannel("ch1");
                assertEquals(0, binding.get());
                return new Table();
            }
        };
        comp1.element();
        // Component 1 unmounts/disposes
        comp1.dispose();

        // Simulate component 2 building for the same channel
        BaseComponent comp2 = new BaseComponent() {
            @Override
            protected Element build() {
                Readable<Integer> binding = unread.forChannel("ch1");
                assertEquals(0, binding.get());

                // An incoming message arrives
                unread.increment("ch1");

                // Reactive binding must reflect the incremented count
                assertEquals(1, binding.get());
                return new Table();
            }
        };
        comp2.element();
        comp2.dispose();
    }
}


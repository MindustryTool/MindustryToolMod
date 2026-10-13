package mindustrytool.features.chat.state;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockSettings;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.core.Component;
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

        String earlierId = "01944800-0000-7000-8000-000000000001";
        String laterId = "01944900-0000-7000-8000-000000000001";

        assertTrue(ChatUnread.isNewer(laterId, earlierId));
        assertFalse(ChatUnread.isNewer(earlierId, laterId));
        assertFalse(ChatUnread.isNewer(earlierId, earlierId));

        assertFalse(ChatUnread.isNewer("temp_1234", earlierId));
        assertFalse(ChatUnread.isNewer(null, earlierId));

        assertTrue(ChatUnread.isNewer(earlierId, null));
        assertTrue(ChatUnread.isNewer(earlierId, ""));

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

        unread.setLatestMessage("ch1", msg2);
        assertEquals(1, unread.get("ch1"));
        assertEquals(1, unread.total().get());
    }

    @Test
    void testOlderMessageDoesNotMarkUnread() {
        ChatUnread unread = new ChatUnread();
        String msg1 = "01944800-0000-7000-8000-000000000001";
        String msg2 = "01944900-0000-7000-8000-000000000002";

        Core.settings.put(ChatUnread.SETTING_PREFIX + "ch1", msg2);

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

        Component comp1 = new Component() {
            @Override
            protected Element build() {
                Readable<Integer> binding = unread.forChannel("ch1");
                assertEquals(0, binding.get());
                return new Table();
            }
        };
        comp1.element();

        comp1.dispose();

        Component comp2 = new Component() {
            @Override
            protected Element build() {
                Readable<Integer> binding = unread.forChannel("ch1");
                assertEquals(0, binding.get());

                unread.increment("ch1");

                assertEquals(1, binding.get());
                return new Table();
            }
        };
        comp2.element();
        comp2.dispose();
    }

    @Test
    void testMutedChannelExcludedFromTotal() {
        ChatNotifications notifications = new ChatNotifications();
        ChatUnread unread = new ChatUnread(notifications.mutedChannelIds());

        unread.increment("ch1");
        unread.increment("ch2");

        assertEquals(2, unread.total().get());

        notifications.setMuted("ch1", true);
        assertEquals(1, unread.total().get());
        assertEquals(1, unread.get("ch1"));

        notifications.setMuted("ch2", true);
        assertEquals(0, unread.total().get());

        notifications.setMuted("ch1", false);
        assertEquals(1, unread.total().get());
    }
}

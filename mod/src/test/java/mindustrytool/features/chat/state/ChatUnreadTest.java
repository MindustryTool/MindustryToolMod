package mindustrytool.features.chat.state;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class ChatUnreadTest {

    @Test
    void testInitialState() {
        ChatUnread unread = new ChatUnread();
        assertEquals(0, unread.currentTotal());
        assertEquals(0, unread.total().get());
        assertEquals(0, unread.get("ch1"));
        assertEquals(0, unread.forChannel("ch1").get());
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
}

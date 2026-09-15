package mindustrytool.features.chat.state;

import static org.junit.jupiter.api.Assertions.*;

import mindustrytool.features.chat.MessageStatus;
import org.junit.jupiter.api.Test;

class ChatMessageDeliveryTest {

    @Test
    void testInitialState() {
        ChatMessageDelivery delivery = new ChatMessageDelivery();
        assertNull(delivery.status("msg1").get());
        assertFalse(delivery.isPending("msg1").get());
        assertFalse(delivery.isFailed("msg1").get());
        assertFalse(delivery.isPendingDirect("msg1"));
        assertFalse(delivery.isFailedDirect("msg1"));
    }

    @Test
    void testMarkPending() {
        ChatMessageDelivery delivery = new ChatMessageDelivery();
        delivery.markPending("temp_1");

        assertEquals(MessageStatus.PENDING, delivery.status("temp_1").get());
        assertTrue(delivery.isPending("temp_1").get());
        assertFalse(delivery.isFailed("temp_1").get());
        assertTrue(delivery.isPendingDirect("temp_1"));

        // Other message remains unaffected
        assertNull(delivery.status("temp_2").get());
        assertFalse(delivery.isPending("temp_2").get());
    }

    @Test
    void testMarkFailed() {
        ChatMessageDelivery delivery = new ChatMessageDelivery();
        delivery.markPending("temp_1");
        delivery.markFailed("temp_1");

        assertEquals(MessageStatus.FAILED, delivery.status("temp_1").get());
        assertFalse(delivery.isPending("temp_1").get());
        assertTrue(delivery.isFailed("temp_1").get());
        assertTrue(delivery.isFailedDirect("temp_1"));
    }

    @Test
    void testClear() {
        ChatMessageDelivery delivery = new ChatMessageDelivery();
        delivery.markPending("temp_1");
        delivery.clear("temp_1");

        assertNull(delivery.status("temp_1").get());
        assertFalse(delivery.isPending("temp_1").get());
        assertFalse(delivery.isFailed("temp_1").get());
    }

    @Test
    void testClearAll() {
        ChatMessageDelivery delivery = new ChatMessageDelivery();
        delivery.markPending("temp_1");
        delivery.markFailed("temp_2");

        delivery.clearAll();
        assertNull(delivery.status("temp_1").get());
        assertNull(delivery.status("temp_2").get());
    }
}

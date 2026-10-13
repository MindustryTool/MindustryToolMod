package mindustrytool.features.chat.state;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.mock.MockSettings;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import solim.reactive.Effect;
import solim.test.SolimEnv;

class ChatNotificationsTest extends SolimEnv {

    @BeforeEach
    void setUp() {
        Core.settings = new MockSettings();
    }

    @Test
    void testInitialEmptyState() {
        ChatNotifications notifications = new ChatNotifications();
        assertFalse(notifications.isCurrentlyMuted("ch1"));
        assertFalse(notifications.isMuted("ch1").get());
        assertTrue(notifications.mutedChannelIds().get().isEmpty());
    }

    @Test
    void testToggleMuted() {
        ChatNotifications notifications = new ChatNotifications();

        notifications.toggleMuted("ch1");
        assertTrue(notifications.isCurrentlyMuted("ch1"));
        assertTrue(notifications.isMuted("ch1").get());
        assertEquals("ch1", Core.settings.getString(ChatNotifications.SETTING_KEY));

        notifications.toggleMuted("ch1");
        assertFalse(notifications.isCurrentlyMuted("ch1"));
        assertFalse(notifications.isMuted("ch1").get());
        assertEquals("", Core.settings.getString(ChatNotifications.SETTING_KEY));
    }

    @Test
    void testMultipleChannelsAndPersistenceReload() {
        ChatNotifications notifications = new ChatNotifications();
        notifications.setMuted("ch1", true);
        notifications.setMuted("ch2", true);

        assertTrue(notifications.isCurrentlyMuted("ch1"));
        assertTrue(notifications.isCurrentlyMuted("ch2"));
        assertFalse(notifications.isCurrentlyMuted("ch3"));

        ChatNotifications reloaded = new ChatNotifications();
        assertTrue(reloaded.isCurrentlyMuted("ch1"));
        assertTrue(reloaded.isCurrentlyMuted("ch2"));
        assertFalse(reloaded.isCurrentlyMuted("ch3"));
    }

    @Test
    void testReactiveSignalNotification() {
        ChatNotifications notifications = new ChatNotifications();
        AtomicInteger runs = new AtomicInteger(0);

        Effect effect = Effect.of(() -> {
            notifications.isMuted("ch1").get();
            runs.incrementAndGet();
        });
        flushEffects();
        assertEquals(1, runs.get());

        notifications.setMuted("ch1", true);
        flushEffects();
        assertEquals(2, runs.get());

        notifications.setMuted("ch1", true);
        flushEffects();
        assertEquals(2, runs.get());

        notifications.setMuted("ch1", false);
        flushEffects();
        assertEquals(3, runs.get());

        effect.dispose();
        notifications.setMuted("ch1", true);
        flushEffects();
        assertEquals(3, runs.get());
    }

    @Test
    void testNullSafe() {
        ChatNotifications notifications = new ChatNotifications();
        assertFalse(notifications.isMuted(null).get());
        assertFalse(notifications.isCurrentlyMuted(null));
        assertDoesNotThrow(() -> notifications.toggleMuted(null));
        assertDoesNotThrow(() -> notifications.setMuted(null, true));
    }
}

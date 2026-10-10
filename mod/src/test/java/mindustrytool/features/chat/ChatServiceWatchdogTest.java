package mindustrytool.features.chat;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import mindustrytool.models.response.ChatMessage;
import mindustrytool.models.response.UserData;
import mindustrytool.test.MindustryTestEnv;
import solim.reactive.QueryCache;
import solim.reactive.Signal;

class ChatServiceWatchdogTest extends MindustryTestEnv {

    private Signal<String> activeChannelSignal;
    private ChatStore store;
    private ChatService service;

    @BeforeEach
    void setUp() {
        QueryCache.getInstance().clear();
        activeChannelSignal = Signal.of("ch1");
        store = new ChatStore(activeChannelSignal, Signal.of(false), Signal.of(false));
        service = new ChatService(store, () -> true);
    }

    @AfterEach
    void tearDown() {
        service.stop();
        service.setRunningForTest(false);
        service.dispose();
        store.dispose();
        QueryCache.getInstance().clear();
        // Service and store own effects outside any component scope, so they
        // are caller-managed: dispose above unsubscribes them, flush drains
        // anything already queued so the env teardown sees an empty dispatcher.
        flushEffects();
    }

    @Test
    void testHandleStreamLineHeartbeatUpdatesLastEventTimeAndConnects() {
        store.session().setConnected(false);
        assertEquals(0L, service.getLastEventTime());

        long before = System.currentTimeMillis();
        service.handleStreamLine(":heartbeat");
        long after = System.currentTimeMillis();

        assertTrue(service.getLastEventTime() >= before && service.getLastEventTime() <= after);
        assertTrue(store.session().connected().get());
    }

    @Test
    void testHandleStreamLineCommentPingConnects() {
        store.session().setConnected(false);

        service.handleStreamLine(":ping");

        assertTrue(store.session().connected().get());
        assertTrue(service.getLastEventTime() > 0);
    }

    @Test
    void testHandleStreamLineEventHeartbeatConnects() {
        store.session().setConnected(false);

        service.handleStreamLine("event: heartbeat");
        service.handleStreamLine("data: ping");
        service.handleStreamLine("");

        assertTrue(store.session().connected().get());
    }

    @Test
    void testHandleStreamLineConnectedDataConnects() {
        store.session().setConnected(false);

        service.handleStreamLine("data: \"Connected\"");
        service.handleStreamLine("");

        assertTrue(store.session().connected().get());
    }

    @Test
    void testHandleStreamLineIncomingMessagesUpdatesStateAndConnects() {
        store.session().setConnected(false);

        service.handleStreamLine("data: {\"type\":\"message\",\"data\":{\"id\":\"msg_1\",\"channelId\":\"ch1\",\"content\":\"Hello world\"}}");
        service.handleStreamLine("");

        assertTrue(store.session().connected().get());
        assertEquals(1, store.messages().active().get().size());
        assertEquals("msg_1", store.messages().active().get().get(0).getId());
        assertEquals("Hello world", store.messages().active().get().get(0).getContent());
    }

    @Test
    void testCheckWatchdogCancelsStalledStreamWhenExceedingTimeout() {
        service.setRunningForTest(true);
        CompletableFuture<Void> mockReq = new CompletableFuture<>();
        service.setStreamRequestForTest(mockReq);
        store.session().setConnected(true);

        long expiredTime = System.currentTimeMillis() - ChatService.WATCHDOG_TIMEOUT_MS - 5000L;
        service.setLastEventTime(expiredTime);

        service.checkWatchdog();

        assertTrue(mockReq.isCancelled(), "Stalled stream request must be cancelled");
        assertNull(service.getStreamRequestForTest(), "streamRequest reference should be cleared");
        assertFalse(store.session().connected().get(), "Connection status should be set to false");

        service.setRunningForTest(false);
    }

    @Test
    void testCheckWatchdogDoesNotCancelStreamWhenFresh() {
        service.setRunningForTest(true);
        CompletableFuture<Void> mockReq = new CompletableFuture<>();
        service.setStreamRequestForTest(mockReq);
        store.session().setConnected(true);

        long freshTime = System.currentTimeMillis() - 10_000L;
        service.setLastEventTime(freshTime);

        service.checkWatchdog();

        assertFalse(mockReq.isCancelled(), "Active fresh stream must not be cancelled");
        assertSame(mockReq, service.getStreamRequestForTest());
        assertTrue(store.session().connected().get());

        service.setRunningForTest(false);
    }

    @Test
    void testCheckWatchdogNoOpWhenNotRunningOrNoStream() {
        service.setRunningForTest(false);
        service.setLastEventTime(100L);
        service.checkWatchdog();
        assertNull(service.getStreamRequestForTest());

        service.setRunningForTest(true);
        service.setStreamRequestForTest(null);
        service.checkWatchdog();
        assertNull(service.getStreamRequestForTest());

        service.setRunningForTest(false);
    }

    @Test
    void testSyncActiveChannelSilentlyDoesNotSetLoadingInitialIfCached() {
        ChatMessage existing = new ChatMessage();
        existing.setId("cached_1");
        existing.setChannelId("ch1");
        existing.setContent("Existing message");
        store.messages().append(existing);
        store.messages().setLoadingInitial("ch1", false);

        assertFalse(store.messages().isActiveLoadingInitial());

        service.syncActiveChannelSilently("ch1");

        // Cancel the incidental real-network request immediately: the assertions
        // above only cover synchronous flag updates, and a late Request-Worker
        // completion would otherwise clear flags or touch solim state during a
        // later test's teardown window (flaky SolimEnv failures).
        service.stop();

        assertFalse(store.messages().isActiveLoadingInitial(),
                "Silent sync should not trigger full loading screen when messages are cached");
    }

    @Test
    void testSyncActiveChannelSilentlySetsLoadingInitialIfEmptyCache() {
        store.messages().replace("ch1", Collections.emptyList());
        store.messages().setLoadingInitial("ch1", false);

        assertFalse(store.messages().isActiveLoadingInitial());

        service.syncActiveChannelSilently("ch1");

        // Same prompt cancellation as above: keep the late async completion
        // from racing these synchronous assertions or later tests.
        service.stop();

        assertTrue(store.messages().isActiveLoadingInitial(),
                "Silent sync should set loadingInitial = true when cache is completely empty");
    }

    @Test
    void testCheckConnectionAndReconnectCancelsStalledStream() {
        service.setRunningForTest(true);
        CompletableFuture<Void> mockReq = new CompletableFuture<>();
        service.setStreamRequestForTest(mockReq);
        store.session().setConnected(true);

        long expiredTime = System.currentTimeMillis() - ChatService.WATCHDOG_TIMEOUT_MS - 2000L;
        service.setLastEventTime(expiredTime);

        service.checkConnectionAndReconnect();

        // Promptly cancel the reconnect's incidental real-network requests
        // (stream + catch-up sync) for the same reason as above.
        service.stop();

        assertTrue(mockReq.isCancelled(), "Stalled stream should be cancelled by reconnect check");
        assertFalse(store.session().connected().get());

        service.setRunningForTest(false);
    }

    @Test
    void testFetchMissingUsersUsesStoreWithoutNetwork() {
        UserData knownUser = new UserData();
        knownUser.setId("author_123");
        knownUser.setName("CachedUser");
        store.users().put(knownUser);

        ChatMessage msg = new ChatMessage();
        msg.setId("msg_1");
        msg.setCreatedBy("author_123");
        msg.setContent("Test message");

        // Author already in store: no batch fetch issued, user retained
        service.fetchMissingUsers(Collections.singletonList(msg));

        UserData found = store.users().getDirect("author_123");
        assertNotNull(found);
        assertEquals("CachedUser", found.getName());
    }

    @Test
    void testIncomingMessageWhenFeedNotVisibleIncrementsUnread() {
        // Feed is not visible for ch1 (e.g., viewing channel list or collapsed)
        ChatService unreadTrackingService = new ChatService(store, channelId -> false);
        try {
            String json = "{\"type\":\"message\",\"data\":{\"id\":\"01944800-0000-7000-8000-000000000001\",\"channelId\":\"ch1\",\"content\":\"hello\",\"createdBy\":\"u1\"}}";
            unreadTrackingService.handleStreamLine("data: " + json);
            unreadTrackingService.handleStreamLine("");

            // Unread should increment so indicator dot can display
            assertEquals(1, store.unread().get("ch1"));
            assertEquals(1, store.unread().forChannel("ch1").get());
        } finally {
            unreadTrackingService.stop();
            unreadTrackingService.dispose();
        }
    }

    @Test
    void testIncomingMessageWhenFeedIsVisibleMarksAsRead() {
        // Feed is visible for ch1
        ChatService readService = new ChatService(store, channelId -> "ch1".equals(channelId));
        try {
            String json = "{\"type\":\"message\",\"data\":{\"id\":\"01944800-0000-7000-8000-000000000002\",\"channelId\":\"ch1\",\"content\":\"hello2\",\"createdBy\":\"u1\"}}";
            readService.handleStreamLine("data: " + json);
            readService.handleStreamLine("");

            // Unread should be 0 since message feed was visible
            assertEquals(0, store.unread().get("ch1"));
            assertEquals(0, store.unread().forChannel("ch1").get());
        } finally {
            readService.stop();
            readService.dispose();
        }
    }

    @Test
    void testHandleStreamLineDeletionRemovesMessageAndConnects() {
        ChatMessage msg1 = new ChatMessage();
        msg1.setId("msg_1");
        msg1.setChannelId("ch1");
        msg1.setContent("Message 1");
        store.messages().append(msg1);

        ChatMessage msg2 = new ChatMessage();
        msg2.setId("msg_2");
        msg2.setChannelId("ch1");
        msg2.setContent("Message 2");
        store.messages().append(msg2);

        assertEquals(2, store.messages().active().get().size());
        store.session().setConnected(false);

        service.handleStreamLine("data: {\"type\":\"delete\",\"data\":{\"id\":\"msg_1\",\"channelId\":\"ch1\"}}");
        service.handleStreamLine("");

        assertTrue(store.session().connected().get());
        assertEquals(1, store.messages().active().get().size());
        assertEquals("msg_2", store.messages().active().get().get(0).getId());
    }

    @Test
    void testHandleStreamLineDeletionClearsReplyTargetIfTargeted() {
        ChatMessage msg = new ChatMessage();
        msg.setId("msg_reply_target");
        msg.setChannelId("ch1");
        msg.setContent("To reply");
        store.messages().append(msg);
        store.ui().setReplyTarget(msg);

        assertEquals(msg, store.ui().currentReplyTarget());

        service.handleStreamLine("data: {\"type\":\"delete\",\"data\":{\"id\":\"msg_reply_target\",\"channelId\":\"ch1\"}}");
        service.handleStreamLine("");

        assertNull(store.ui().currentReplyTarget());
    }

    @Test
    void testHandleStreamLineDeletionDoesNotClearDifferentReplyTarget() {
        ChatMessage msg1 = new ChatMessage();
        msg1.setId("msg_1");
        msg1.setChannelId("ch1");
        msg1.setContent("To delete");
        store.messages().append(msg1);

        ChatMessage msg2 = new ChatMessage();
        msg2.setId("msg_2");
        msg2.setChannelId("ch1");
        msg2.setContent("To reply");
        store.messages().append(msg2);
        store.ui().setReplyTarget(msg2);

        service.handleStreamLine("data: {\"type\":\"delete\",\"data\":{\"id\":\"msg_1\",\"channelId\":\"ch1\"}}");
        service.handleStreamLine("");

        assertEquals(msg2, store.ui().currentReplyTarget());
    }

    @Test
    void testHandleStreamLineDeletionUpdatesLastMessageId() {
        ChatMessage msg = new ChatMessage();
        msg.setId("msg_to_delete");
        msg.setChannelId("ch1");
        msg.setContent("Will be deleted");
        store.messages().append(msg);

        service.handleStreamLine("data: {\"type\":\"delete\",\"data\":{\"id\":\"msg_to_delete\",\"channelId\":\"ch1\",\"lastMessageId\":\"msg_prev\"}}");
        service.handleStreamLine("");

        assertEquals("msg_prev", store.unread().getLatestMessageId("ch1"));
    }
}


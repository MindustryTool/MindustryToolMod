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

        service.handleStreamLine("data: {\"id\":\"msg_1\",\"channelId\":\"ch1\",\"content\":\"Hello world\"}");
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

        assertFalse(store.messages().isActiveLoadingInitial(),
                "Silent sync should not trigger full loading screen when messages are cached");
    }

    @Test
    void testSyncActiveChannelSilentlySetsLoadingInitialIfEmptyCache() {
        store.messages().replace("ch1", Collections.emptyList());
        store.messages().setLoadingInitial("ch1", false);

        assertFalse(store.messages().isActiveLoadingInitial());

        service.syncActiveChannelSilently("ch1");

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
}

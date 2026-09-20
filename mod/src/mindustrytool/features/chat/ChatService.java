package mindustrytool.features.chat;

import arc.Core;
import arc.util.Log;
import arc.util.Nullable;
import arc.util.Threads;
import arc.util.Timer;
import arc.util.Timer.Task;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;
import mindustrytool.models.response.ChannelDto;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.models.response.UserData;
import mindustrytool.services.MindustryTool;
import mindustrytool.utils.JsonUtils;
import solim.reactive.Effect;

public class ChatService {

    public static final int PAGE_SIZE = 50;
    public static final long WATCHDOG_TIMEOUT_MS = 45_000L;
    public static final float WATCHDOG_INTERVAL_SECONDS = 5f;

    private final ChatStore store;
    private final Supplier<Boolean> windowOpenSupplier;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final String chatId = UUID.randomUUID().toString();

    private @Nullable CompletableFuture<Void> streamRequest;
    private StringBuilder dataBuffer = new StringBuilder();
    private String currentEvent = "data";

    private volatile long lastEventTime = 0L;
    private @Nullable Task watchdogTask;
    private final AtomicBoolean reconnecting = new AtomicBoolean(false);

    public ChatService(ChatStore store, Supplier<Boolean> windowOpenSupplier) {
        this.store = store;
        this.windowOpenSupplier = windowOpenSupplier;

        store.channels().activeId().subscribe(channelId -> {
            if (channelId != null && !channelId.isEmpty()) {
                loadMessages(channelId);
            }
        });

        Effect.of(() -> {
            List<ChannelDto> channels = store.channels().channelsQuery().data().get();
            if (channels != null) {
                for (ChannelDto c : channels) {
                    if (c.getId() != null && c.getLastMessageId() != null) {
                        store.unread().setLatestMessage(c.getId(), c.getLastMessageId());
                    }
                }
            }
        });
    }

    public synchronized void start() {
        if (running.get()) {
            return;
        }
        running.set(true);
        refreshChannels();
        connectStream();
        if (watchdogTask == null) {
            watchdogTask = Timer.schedule(this::checkWatchdog, WATCHDOG_INTERVAL_SECONDS, WATCHDOG_INTERVAL_SECONDS);
        }
    }

    public synchronized void stop() {
        running.set(false);
        if (watchdogTask != null) {
            watchdogTask.cancel();
            watchdogTask = null;
        }
        if (streamRequest != null) {
            try {
                streamRequest.cancel(true);
            } catch (Exception ignored) {
            }
            streamRequest = null;
        }
        Core.app.post(() -> store.session().setConnected(false));
    }

    private static String extractError(Throwable throwable) {
        Throwable cause = throwable.getCause() != null ? throwable.getCause() : throwable;
        String msg = cause.getMessage();
        return (msg != null && !msg.trim().isEmpty()) ? msg.trim() : cause.getClass().getSimpleName();
    }

    public void refreshChannels() {
        store.channels().channelsQuery().refetch();
    }

    public void refresh(@Nullable String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            refreshChannels();
            checkConnectionAndReconnect();
            return;
        }

        loadMessages(channelId);
        store.members().query().refetch();
        checkConnectionAndReconnect();
    }

    public void syncActiveChannelSilently(@Nullable String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            return;
        }

        List<ChatMessage> existing = store.messages().currentActive();
        boolean hasExisting = existing != null && !existing.isEmpty();
        if (!hasExisting) {
            store.messages().setLoadingInitial(channelId, true);
        }

        MindustryTool.getChatMessages(channelId, null).thenAccept(messages -> {
            Core.app.post(() -> {
                store.messages().setLoadingInitial(channelId, false);
                if (messages != null) {
                    Collections.reverse(messages);
                }
                store.messages().replace(channelId, messages);
                store.messages().setFullyLoaded(channelId, messages == null || messages.size() < PAGE_SIZE);
                if (messages != null && !messages.isEmpty()) {
                    ChatMessage newest = messages.get(messages.size() - 1);
                    if (newest.getId() != null) {
                        store.unread().setLatestMessage(channelId, newest.getId());
                        boolean open = windowOpenSupplier.get();
                        boolean isActive = Objects.equals(store.channels().currentActiveId(), channelId);
                        if (open && isActive) {
                            store.unread().markAsRead(channelId, newest.getId());
                        }
                    }
                }
                fetchMissingUsers(messages);
            });
        }).exceptionally(e -> {
            Core.app.post(() -> {
                store.messages().setLoadingInitial(channelId, false);
                if (!hasExisting) {
                    store.messages().setError(channelId, extractError(e));
                }
            });
            Log.err("Failed to silently sync chat messages for " + channelId, e);
            return null;
        });
    }

    public void catchUpSync() {
        String activeId = store.channels().currentActiveId();
        if (activeId != null && !activeId.isEmpty()) {
            syncActiveChannelSilently(activeId);
        }
        MindustryTool.getChatChannels().thenAccept(channels -> {
            Core.app.post(() -> {
                store.channels().replace(channels);
                if (channels != null) {
                    for (ChannelDto c : channels) {
                        if (c.getId() != null && c.getLastMessageId() != null) {
                            store.unread().setLatestMessage(c.getId(), c.getLastMessageId());
                        }
                    }
                }
            });
        }).exceptionally(e -> {
            Log.err("Failed to catch up chat channels metadata", e);
            return null;
        });
    }

    public void checkConnectionAndReconnect() {
        if (!running.get()) {
            return;
        }
        long now = System.currentTimeMillis();
        boolean stalled = streamRequest != null && lastEventTime > 0 && (now - lastEventTime > WATCHDOG_TIMEOUT_MS);
        boolean disconnected = streamRequest == null || !Boolean.TRUE.equals(store.session().connected().peek());
        if (stalled || disconnected) {
            if (streamRequest != null) {
                try {
                    streamRequest.cancel(true);
                } catch (Exception ignored) {
                }
                streamRequest = null;
            }
            Core.app.post(() -> store.session().setConnected(false));
            connectStream();
            catchUpSync();
        }
    }

    public void loadMessages(String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            return;
        }

        store.messages().setLoadingInitial(channelId, true);
        store.messages().setError(channelId, null);

        MindustryTool.getChatMessages(channelId, null).thenAccept(messages -> {
            Core.app.post(() -> {
                store.messages().setLoadingInitial(channelId, false);
                if (messages != null) {
                    Collections.reverse(messages);
                }
                store.messages().replace(channelId, messages);
                store.messages().setFullyLoaded(channelId, messages == null || messages.size() < PAGE_SIZE);
                if (messages != null && !messages.isEmpty()) {
                    ChatMessage newest = messages.get(messages.size() - 1);
                    if (newest.getId() != null) {
                        store.unread().setLatestMessage(channelId, newest.getId());
                        boolean open = windowOpenSupplier.get();
                        boolean isActive = Objects.equals(store.channels().currentActiveId(), channelId);
                        if (open && isActive) {
                            store.unread().markAsRead(channelId, newest.getId());
                        }
                    }
                }
                fetchMissingUsers(messages);
            });
        }).exceptionally(e -> {
            Core.app.post(() -> {
                store.messages().setLoadingInitial(channelId, false);
                store.messages().setError(channelId, extractError(e));
            });
            Log.err("Failed to fetch chat messages for " + channelId, e);
            return null;
        });
    }

    public void fetchOlderMessages(String channelId) {
        if (channelId == null || channelId.isEmpty()) {
            return;
        }
        if (store.messages().isLoadingOlder()) {
            return;
        }
        if (store.messages().isFullyLoaded(channelId)) {
            return;
        }

        List<ChatMessage> currentMsgs = store.messages().currentActive();
        if (currentMsgs == null || currentMsgs.isEmpty()) {
            return;
        }

        String oldestId = currentMsgs.get(0).getId();
        store.messages().setLoadingOlder(true);

        MindustryTool.getChatMessages(channelId, oldestId).thenAccept(older -> {
            Core.app.post(() -> {
                store.messages().setLoadingOlder(false);
                if (older == null || older.isEmpty()) {
                    store.messages().setFullyLoaded(channelId, true);
                } else {
                    Collections.reverse(older);
                    int added = store.messages().prepend(channelId, older);
                    if (older.size() < PAGE_SIZE || added == 0) {
                        store.messages().setFullyLoaded(channelId, true);
                    }
                    fetchMissingUsers(older);
                }
            });
        }).exceptionally(e -> {
            Core.app.post(() -> store.messages().setLoadingOlder(false));
            Log.err("Failed to fetch older chat messages for " + channelId, e);
            return null;
        });
    }

    public CompletableFuture<ChatMessage> sendMessage(String content, @Nullable String replyTo) {
        String activeId = store.channels().currentActiveId();
        if (activeId == null || activeId.isEmpty()) {
            CompletableFuture<ChatMessage> failed = new CompletableFuture<>();
            failed.completeExceptionally(new IllegalStateException("No active channel"));
            return failed;
        }

        return MindustryTool.sendChatMessage("text", activeId, content, replyTo)
                .thenApply(msg -> {
                    Core.app.post(() -> {
                        boolean added = store.messages().append(msg);
                        if (added && msg.getId() != null) {
                            store.unread().setLatestMessage(msg.getChannelId(), msg.getId());
                            store.unread().markAsRead(msg.getChannelId(), msg.getId());
                        }
                        store.ui().setReplyTarget(null);
                    });
                    return msg;
                });
    }

    synchronized void connectStream() {
        if (!running.get()) {
            return;
        }

        lastEventTime = System.currentTimeMillis();
        CompletableFuture<Void> req = MindustryTool.chatStream(chatId, this::handleStreamLine);
        streamRequest = req;
        req.whenComplete((ignored, error) -> {
            if (streamRequest == req) {
                Core.app.post(() -> store.session().setConnected(false));
                scheduleReconnect();
            }
        });
    }

    void checkWatchdog() {
        if (!running.get() || streamRequest == null) {
            return;
        }
        long now = System.currentTimeMillis();
        long elapsed = now - lastEventTime;
        if (lastEventTime > 0 && elapsed > WATCHDOG_TIMEOUT_MS) {
            Log.warn("Chat SSE stream stalled (no data for " + elapsed + "ms). Reconnecting...");
            Core.app.post(() -> store.session().setConnected(false));
            if (streamRequest != null) {
                try {
                    streamRequest.cancel(true);
                } catch (Exception ignored) {
                }
                streamRequest = null;
            }
            scheduleReconnect();
        }
    }

    void handleStreamLine(String line) {
        if (line == null) {
            return;
        }

        lastEventTime = System.currentTimeMillis();

        if (line.isEmpty()) {
            dispatchCurrentEvent();
            return;
        }

        if (line.startsWith(":")) {
            Core.app.post(() -> store.session().setConnected(true));
            return;
        }

        if (line.startsWith("event:")) {
            currentEvent = line.substring("event:".length()).trim();
            return;
        }

        if (line.startsWith("data:")) {
            if (dataBuffer.length() > 0) {
                dataBuffer.append('\n');
            }
            dataBuffer.append(line.substring("data:".length()).trim());
        }
    }

    private void dispatchCurrentEvent() {
        String data = dataBuffer.toString().trim();
        String event = currentEvent;
        dataBuffer.setLength(0);
        currentEvent = "data";

        if (data.isEmpty()) {
            return;
        }

        if ("heartbeat".equalsIgnoreCase(event) || "\"Connected\"".equals(data) || "Connected".equals(data)) {
            Core.app.post(() -> store.session().setConnected(true));
            return;
        }

        try {
            if (data.startsWith("[")) {
                List<ChatMessage> list = JsonUtils.fromJsonArray(ChatMessage.class, data);
                if (list != null) {
                    Core.app.post(() -> {
                        store.session().setConnected(true);
                        boolean open = windowOpenSupplier.get();
                        for (ChatMessage msg : list) {
                            boolean added = store.messages().append(msg);
                            if (added) {
                                boolean isActive = Objects.equals(store.channels().currentActiveId(),
                                        msg.getChannelId());
                                if (msg.getId() != null) {
                                    store.unread().setLatestMessage(msg.getChannelId(), msg.getId());
                                }
                                if (open && isActive) {
                                    store.unread().markAsRead(msg.getChannelId(), msg.getId());
                                } else {
                                    store.unread().increment(msg.getChannelId());
                                }
                            }
                        }
                        fetchMissingUsers(list);
                    });
                }
            } else if (data.startsWith("{")) {
                ChatMessage msg = JsonUtils.fromJson(ChatMessage.class, data);
                if (msg != null && msg.getId() != null) {
                    Core.app.post(() -> {
                        store.session().setConnected(true);
                        boolean added = store.messages().append(msg);
                        if (added) {
                            boolean open = windowOpenSupplier.get();
                            boolean isActive = Objects.equals(store.channels().currentActiveId(), msg.getChannelId());
                            store.unread().setLatestMessage(msg.getChannelId(), msg.getId());
                            if (open && isActive) {
                                store.unread().markAsRead(msg.getChannelId(), msg.getId());
                            } else {
                                store.unread().increment(msg.getChannelId());
                            }
                        }
                        fetchMissingUsers(Collections.singletonList(msg));
                    });
                }
            }
        } catch (Exception e) {
            Log.err("Error processing chat stream event", e);
        }
    }

    public void fetchMissingUsers(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty())
            return;
        Map<String, UserData> cached = store.users().currentAll();
        List<String> missing = new ArrayList<>();
        for (ChatMessage msg : messages) {
            String authorId = msg.getCreatedBy();
            if (authorId != null && !authorId.isEmpty() && !cached.containsKey(authorId)) {
                if (!missing.contains(authorId)) {
                    missing.add(authorId);
                }
            }
        }
        if (!missing.isEmpty()) {
            // Network-only by contract: user profiles are fetched directly per
            // batch with no long-term QueryCache retention. The store itself
            // dedupes repeat authors within the session.
            MindustryTool.getUserBatch(missing).thenAccept(userDataList -> {
                if (userDataList != null) {
                    Core.app.post(() -> store.users().putAll(userDataList));
                }
            }).exceptionally(e -> {
                Log.err("Failed to fetch user batch", e);
                return null;
            });
        }
    }

    void scheduleReconnect() {
        if (!running.get() || !reconnecting.compareAndSet(false, true)) {
            return;
        }
        Threads.daemon("ChatReconnectThread", () -> {
            try {
                Thread.sleep(5000L);
            } catch (InterruptedException ignored) {
            } finally {
                reconnecting.set(false);
            }
            if (running.get()) {
                Core.app.post(() -> {
                    if (running.get()) {
                        connectStream();
                        catchUpSync();
                    }
                });
            }
        });
    }

    long getLastEventTime() {
        return lastEventTime;
    }

    void setLastEventTime(long time) {
        this.lastEventTime = time;
    }

    @Nullable Task getWatchdogTask() {
        return watchdogTask;
    }

    boolean isStreamActive() {
        return streamRequest != null && !streamRequest.isDone();
    }

    void setRunningForTest(boolean r) {
        this.running.set(r);
    }

    void setStreamRequestForTest(@Nullable CompletableFuture<Void> req) {
        this.streamRequest = req;
    }

    @Nullable CompletableFuture<Void> getStreamRequestForTest() {
        return this.streamRequest;
    }
}

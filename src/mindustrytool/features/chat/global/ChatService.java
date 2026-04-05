package mindustrytool.features.chat.global;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.function.Supplier;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.struct.Seq;
import arc.util.Log;
import arc.util.Timer;
import mindustrytool.features.chat.global.dto.ChannelDto;
import mindustrytool.features.chat.global.dto.ChatMessage;
import mindustrytool.features.chat.global.dto.ChatUser;
import mindustrytool.features.chat.global.events.ChatMessageReceive;
import mindustrytool.features.chat.global.events.ChatStateChange;

public class ChatService {
    private static ChatService instance;

    private final ChatStore store;
    private final ChatApiClient apiClient;
    private final ChatStreamClient streamClient;
    private final ChatStateManager stateManager;

    private ChatService() {
        store = ChatStore.getInstance();
        apiClient = new ChatApiClient();
        streamClient = new ChatStreamClient(this::handleIncomingMessages, this::broadcastConnectionStatus);
        stateManager = new ChatStateManager(apiClient, streamClient, this::refreshCurrentChannelUsers);
    }

    public synchronized static ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }

        return instance;
    }

    public boolean isConnected() {
        return streamClient.isConnected();
    }

    public void init() {
        Timer.schedule(this::connectStream, 0, 60);
        stateManager.init();
        fetchChannelsAndCurrentMessages();
    }

    public void fetchChannels() {
        getChannels(this::applyChannels, e -> Log.err("Failed to fetch channels", e));
    }

    public void fetchChannelsAndCurrentMessages() {
        apiClient.getChannels()
                .thenCompose(channels -> runOnApp(() -> applyChannels(channels)))
                .thenAccept(channelId -> {
                    if (channelId != null) {
                        fetchMessages(channelId, null);
                        fetchChatUsers(channelId);
                    }
                })
                .exceptionally(e -> {
                    Log.err("Failed to fetch channels", unwrap(e));
                    return null;
                });
    }

    public void connectStream() {
        streamClient.connect();
    }

    public void disconnectStream() {
        streamClient.disconnect();
        Log.info("Chat stream disconnected.");
    }

    public void getChannels(Cons<ChannelDto[]> onSuccess, Cons<Throwable> onError) {
        deliver(apiClient::getChannels, onSuccess, onError);
    }

    public CompletableFuture<ChatMessage> sendMessage(String channelId, String content, String replyTo,
            ContentType type) {
        return apiClient.sendMessage(channelId, content, replyTo, type);
    }

    public void getChatUsers(String channelId, Cons<ChatUser[]> onSuccess, Cons<Throwable> onError) {
        deliver(() -> apiClient.getChatUsers(channelId), onSuccess, onError);
    }

    public void getChatUserCount(String channelId, Cons<Integer> onSuccess, Cons<Throwable> onError) {
        deliver(() -> apiClient.getChatUserCount(channelId), onSuccess, onError);
    }

    public void fetchChatUsers(String channelId) {
        getChatUsers(channelId, users -> store.setUsers(channelId, users),
                e -> Log.err("Failed to fetch chat users for channel " + channelId, e));
    }

    public void fetchMessages(String channelId, String cursor, Cons<ChatMessage[]> onSuccess, Cons<Throwable> onError) {
        deliver(() -> apiClient.getMessages(channelId, cursor), onSuccess, onError);
    }

    public void fetchMessages(String channelId, String cursor) {
        if (!store.compareAndSetLoadingMessages(false, true)) {
            return;
        }

        fetchMessages(channelId, cursor,
                messages -> {
                    store.setLoadingMessages(false);
                    if (messages.length == 0) {
                        if (cursor == null) {
                            store.prependMessages(channelId, new Seq<>());
                        }
                        store.setFullyLoaded(channelId);
                        return;
                    }

                    store.prependMessages(channelId, new Seq<>(messages));
                },
                e -> {
                    store.setLoadingMessages(false);
                    Log.err("Failed to fetch messages for channel " + channelId, e);
                });
    }

    public void updateState(String state) {
        stateManager.updateState(state);
    }

    private void handleIncomingMessages(Seq<ChatMessage> messages) {
        Events.fire(new ChatMessageReceive(messages));
    }

    private void broadcastConnectionStatus(Boolean connected) {
        Events.fire(new ChatStateChange(connected));
    }

    private void refreshCurrentChannelUsers() {
        String channelId = store.getCurrentChannelId();
        if (channelId != null) {
            fetchChatUsers(channelId);
        }
    }

    private String applyChannels(ChannelDto[] channels) {
        Seq<ChannelDto> channelSeq = new Seq<>(channels);
        store.setChannels(channelSeq);

        if (channels.length == 0) {
            return null;
        }

        String currentId = store.getCurrentChannelId();
        if (currentId != null && channelSeq.contains(channel -> channel.id.equals(currentId))) {
            return currentId;
        }

        String nextChannelId = channels[0].id;
        store.setCurrentChannelId(nextChannelId);
        return nextChannelId;
    }

    private <T> void deliver(Supplier<CompletableFuture<T>> futureSupplier, Cons<T> onSuccess,
            Cons<Throwable> onError) {
        CompletableFuture<T> future;

        try {
            future = futureSupplier.get();
        } catch (Throwable throwable) {
            if (onError != null) {
                Throwable error = unwrap(throwable);
                Core.app.post(() -> onError.get(error));
            }
            return;
        }

        future.thenAccept(result -> Core.app.post(() -> onSuccess.get(result)))
                .exceptionally(e -> {
                    if (onError != null) {
                        Throwable error = unwrap(e);
                        Core.app.post(() -> onError.get(error));
                    }
                    return null;
                });
    }

    private <T> CompletableFuture<T> runOnApp(Supplier<T> supplier) {
        CompletableFuture<T> future = new CompletableFuture<>();
        Core.app.post(() -> {
            try {
                future.complete(supplier.get());
            } catch (Exception e) {
                future.completeExceptionally(e);
            }
        });
        return future;
    }

    private Throwable unwrap(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }
}

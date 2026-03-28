package mindustrytool.features.chat.global;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import arc.Core;
import arc.Events;
import arc.func.Cons;
import arc.util.Log;
import arc.util.Timer;
import arc.util.serialization.Jval;
import mindustry.io.JsonIO;
import arc.util.serialization.Json;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.auth.AuthHttp;
import mindustrytool.features.auth.AuthService;
import mindustrytool.features.chat.global.dto.ChatMessage;
import mindustrytool.features.chat.global.dto.ChatMessageReceive;
import mindustrytool.features.chat.global.dto.ChatStateChange;
import mindustrytool.features.chat.global.dto.ChatUser;
import mindustrytool.features.chat.global.dto.ChannelDto;
import arc.struct.Seq;

public class ChatService {
    private static ChatService instance;

    private volatile Thread streamThread;

    private AtomicBoolean isStreaming = new AtomicBoolean(false);
    private AtomicBoolean isConnected = new AtomicBoolean(false);

    public ChatService() {
        Timer.schedule(this::connectStream, 0, 60);
    }

    public static ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }

        return instance;
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public void init() {
        fetchChannelsAndCurrentMessages();
    }

    public void fetchChannelsAndCurrentMessages() {
        getChannels(chans -> {
            ChatStore store = ChatStore.getInstance();
            store.setChannels(new Seq<>(chans));
            if (chans.length > 0) {
                String currentId = store.getCurrentChannelId();
                final String currentIdFinal = currentId;
                if (currentId == null || !new Seq<>(chans).contains(c -> c.id.equals(currentIdFinal))) {
                    store.setCurrentChannelId(chans[0].id);
                    currentId = chans[0].id;
                }
                fetchMessages(currentId, null);
                fetchChatUsers(currentId);
            }
        }, e -> Log.err("Failed to fetch channels", e));
    }

    public synchronized void connectStream() {
        if (isStreaming.get()) {
            return;
        }

        isStreaming.set(true);

        streamThread = new Thread(() -> {
            while (isStreaming.get()) {
                HttpURLConnection conn = null;
                try {
                    AuthService.getInstance().refreshTokenIfNeeded().get();

                    Log.info("Connecting to chat stream");

                    String urlStr = Config.API_v4_URL + "chats/stream";

                    URL url = new URL(urlStr);
                    conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("GET");
                    conn.setRequestProperty("Accept", "text/event-stream");
                    conn.setConnectTimeout(10000);
                    conn.setReadTimeout(0); // Infinite read timeout for SSE

                    if (AuthService.getInstance().getAccessToken() != null) {
                        var bearer = "Bearer " + AuthService.getInstance().getAccessToken();
                        conn.setRequestProperty("Authorization", bearer);
                    }

                    int status = conn.getResponseCode();
                    if (status != 200) {
                        Log.err("Chat stream failed: " + status);
                        broadcastConnectionStatus(false);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException e) {
                            break;
                        }
                        continue;
                    }

                    broadcastConnectionStatus(true);

                    Log.info("Chat stream connected");

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;

                    while (isStreaming.get() && (line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if (!data.isEmpty()) {
                                try {
                                    // Parse array of messages
                                    Jval json = Jval.read(data);

                                    if (json.isString() && json.asString().equals("Connected")) {
                                        continue;
                                    }

                                    if (json.isArray()) {
                                        @SuppressWarnings("unchecked")
                                        Seq<ChatMessage> messages = JsonIO.json.fromJson(Seq.class, ChatMessage.class,
                                                data);
                                        Core.app.post(() -> Events.fire(new ChatMessageReceive(messages)));
                                    } else {
                                        ChatMessage msg = JsonIO.json.fromJson(ChatMessage.class, data);
                                        Core.app.post(() -> Events.fire(new ChatMessageReceive(Seq.with(msg))));
                                    }
                                } catch (Exception e) {
                                    Log.err("Failed to parse chat message", e);
                                }
                            }
                        }
                    }

                } catch (Exception e) {
                    if (isStreaming.get()) {
                        Log.err("Chat stream error", e);
                        broadcastConnectionStatus(false);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            break;
                        }
                    }
                } finally {
                    broadcastConnectionStatus(false);
                    if (conn != null)
                        conn.disconnect();
                }
            }
        }, "ChatStreamThread");
        streamThread.setDaemon(true);
        streamThread.start();
    }

    private void broadcastConnectionStatus(boolean connected) {
        if (isConnected.get() == connected) {
            return;
        }

        isConnected.set(connected);

        Events.fire(new ChatStateChange(connected));
    }

    public synchronized void disconnectStream() {
        isStreaming.set(false);

        broadcastConnectionStatus(false);

        try {
            if (streamThread != null) {
                streamThread.interrupt();
                streamThread = null;
            }
        } catch (Exception e) {
            Log.err("Failed to disconnect chat stream", e);
        }

        Log.info("Chat stream disconnected.");
    }

    public void getChannels(Cons<ChannelDto[]> onSuccess, Cons<Throwable> onError) {
        AuthHttp.get(Config.API_v4_URL + "chats/channels")
                .error(onError)
                .submit(res -> {
                    try {
                        Json json = new Json();
                        ChannelDto[] channels = json.fromJson(ChannelDto[].class, res.getResultAsString());
                        if (channels == null) {
                            Core.app.post(() -> onSuccess.get(new ChannelDto[0]));
                        } else {
                            Core.app.post(() -> onSuccess.get(channels));
                        }
                    } catch (Exception e) {
                        Core.app.post(() -> onError.get(e));
                    }
                });
    }

    public CompletableFuture<ChatMessage> sendMessage(String channelId, String content, String replyTo) {
        CompletableFuture<ChatMessage> future = new CompletableFuture<>();
        if (channelId == null) {
            future.completeExceptionally(new IllegalArgumentException("Channel ID cannot be null"));
            return future;
        }
        try {
            Jval json = Jval.newObject();
            json.put("content", content);
            json.put("channelId", channelId);
            if (replyTo != null && !replyTo.isEmpty()) {
                json.put("replyTo", replyTo);
            }

            AuthHttp.post(Config.API_v4_URL + "chats/text", json.toString())
                    .header("Content-Type", "application/json")
                    .error(future::completeExceptionally)
                    .submit(res -> future.complete(Utils.fromJson(ChatMessage.class, res.getResultAsString())));

            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
    }

    public CompletableFuture<ChatMessage> sendSchematic(String channelId, String content, String replyTo) {
        CompletableFuture<ChatMessage> future = new CompletableFuture<>();
        if (channelId == null) {
            future.completeExceptionally(new IllegalArgumentException("Channel ID cannot be null"));
            return future;
        }
        try {
            Jval json = Jval.newObject();
            json.put("content", content);
            json.put("channelId", channelId);
            if (replyTo != null && !replyTo.isEmpty()) {
                json.put("replyTo", replyTo);
            }

            AuthHttp.post(Config.API_v4_URL + "chats/msch", json.toString())
                    .header("Content-Type", "application/json")
                    .error(future::completeExceptionally)
                    .submit(res -> future.complete(Utils.fromJson(ChatMessage.class, res.getResultAsString())));

            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
    }

    public void getChatUsers(String channelId, Cons<ChatUser[]> onSuccess, Cons<Throwable> onError) {
        if (channelId == null) {
            onError.get(new IllegalArgumentException("Channel ID cannot be null"));
            return;
        }
        AuthHttp.get(Config.API_v4_URL + "chats/users?channelId=" + channelId)
                .error(onError)
                .submit(res -> {
                    try {
                        Json json = new Json();
                        ChatUser[] users = json.fromJson(ChatUser[].class, res.getResultAsString());

                        if (users == null) {
                            Core.app.post(() -> onSuccess.get(new ChatUser[0]));
                        } else {
                            Core.app.post(() -> onSuccess.get(users));
                        }
                    } catch (Exception e) {
                        Core.app.post(() -> onError.get(e));
                    }
                });
    }

    public void fetchChatUsers(String channelId) {
        getChatUsers(channelId,
                users -> ChatStore.getInstance().setUsers(channelId, users),
                e -> Log.err("Failed to fetch chat users for channel " + channelId, e));
    }

    public void fetchMessages(String channelId, String cursor, Cons<ChatMessage[]> onSuccess, Cons<Throwable> onError) {
        if (channelId == null) {
            onError.get(new IllegalArgumentException("Channel ID cannot be null"));
            return;
        }

        String url = Config.API_v4_URL + "chats?channelId=" + channelId;
        if (cursor != null && !cursor.isEmpty()) {
            url += "&cursor=" + cursor;
        }

        AuthHttp.get(url)
                .error(onError)
                .submit(res -> {
                    try {
                        Json json = new Json();
                        ChatMessage[] msgs = json.fromJson(ChatMessage[].class, res.getResultAsString());

                        if (msgs == null) {
                            Core.app.post(() -> onSuccess.get(new ChatMessage[0]));
                        } else {
                            Core.app.post(() -> onSuccess.get(msgs));
                        }
                    } catch (Exception e) {
                        Core.app.post(() -> onError.get(e));
                    }
                });
    }

    public void fetchMessages(String channelId, String cursor) {
        ChatStore store = ChatStore.getInstance();
        store.setLoadingMessages(true);
        fetchMessages(channelId, cursor,
                msgs -> {
                    store.setLoadingMessages(false);
                    if (msgs.length == 0) {
                        if (cursor == null) {
                            store.setMessages(channelId, new Seq<>());
                        }
                        store.setFullyLoaded(channelId);
                    } else {
                        if (cursor == null) {
                            store.setMessages(channelId, new Seq<>(msgs).reverse());
                        } else {
                            store.prependMessages(channelId, new Seq<>(msgs));
                        }
                    }
                },
                e -> {
                    store.setLoadingMessages(false);
                    Log.err("Failed to fetch messages for channel " + channelId, e);
                });
    }
}

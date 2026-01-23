package mindustrytool.features.chat.global;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;

import arc.Core;
import arc.func.Cons;
import arc.util.Log;
import arc.util.serialization.Jval;
import arc.util.serialization.Json;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.auth.AuthHttp;
import mindustrytool.features.auth.AuthService;
import mindustrytool.features.chat.global.dto.ChatMessage;
import mindustrytool.features.chat.global.dto.ChatUser;

public class ChatService {
    private static ChatService instance;

    private volatile Thread streamThread;
    private volatile Cons<Boolean> connectionListener;

    private AtomicBoolean isStreaming = new AtomicBoolean(false);
    private AtomicBoolean isConnected = new AtomicBoolean(false);
    private Cons<ChatMessage[]> messageListener;

    public static ChatService getInstance() {
        if (instance == null) {
            instance = new ChatService();
        }

        return instance;
    }

    public void setConnectionListener(Cons<Boolean> listener) {
        this.connectionListener = listener;
        // Initial state
        Core.app.post(() -> listener.get(isConnected.get()));
    }

    public boolean isConnected() {
        return isConnected.get();
    }

    public void setListener(Cons<ChatMessage[]> listener) {
        this.messageListener = listener;
    }

    public void connectStream() {
        if (isStreaming.get()) {
            return;
        }

        isStreaming.set(true);

        streamThread = new Thread(() -> {
            while (isStreaming.get()) {
                HttpURLConnection conn = null;
                try {
                    AuthService.getInstance().refreshTokenIfNeeded().get();

                    Log.info("Connecting to chat stream...");

                    URL url = new URL(Config.API_v4_URL + "chats/stream");
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

                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    String line;

                    while (isStreaming.get() && (line = reader.readLine()) != null) {
                        if (line.startsWith("data:")) {
                            String data = line.substring(5).trim();
                            if (!data.isEmpty()) {
                                try {
                                    // Parse array of messages
                                    Jval json = Jval.read(data);
                                    if (json.isArray()) {
                                        Json jsonParser = new Json();
                                        // Jval to Object mapping is manual in Arc usually or use Json
                                        // Let's manually map for safety or use Json if compatible
                                        ChatMessage[] messages = jsonParser.fromJson(ChatMessage[].class, data);

                                        if (messageListener != null && messages != null) {
                                            Core.app.post(() -> messageListener.get(messages));
                                        }
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
        if (connectionListener != null) {
            Core.app.post(() -> connectionListener.get(connected));
        }
    }

    public synchronized void disconnectStream() {
        isStreaming.set(false);

        broadcastConnectionStatus(false);

        messageListener = null;

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

    public CompletableFuture<ChatMessage> sendMessage(String content) {
        CompletableFuture<ChatMessage> future = new CompletableFuture<>();
        try {
            Jval json = Jval.newObject();
            json.put("content", content);

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

    public CompletableFuture<ChatMessage> sendSchematic(String content) {
        CompletableFuture<ChatMessage> future = new CompletableFuture<>();
        try {
            Jval json = Jval.newObject();
            json.put("content", content);

            AuthHttp.post(Config.API_v4_URL + "chats/schematic", json.toString())
                    .header("Content-Type", "application/json")
                    .error(future::completeExceptionally)
                    .submit(res -> future.complete(Utils.fromJson(ChatMessage.class, res.getResultAsString())));

            return future;
        } catch (Exception e) {
            future.completeExceptionally(e);
            return future;
        }
    }

    public void getChatUsers(Cons<ChatUser[]> onSuccess, Cons<Throwable> onError) {
        AuthHttp.get(Config.API_v4_URL + "chats/users")
                .error(onError)
                .submit(res -> {
                    try {
                        Json json = new Json();
                        ChatUser[] users = json.fromJson(ChatUser[].class, res.getResultAsString());

                        if (users == null) {
                            onSuccess.get(new ChatUser[0]);
                        } else {
                            Core.app.post(() -> onSuccess.get(users));
                        }
                    } catch (Exception e) {
                        Core.app.post(() -> onError.get(e));
                    }
                });
    }
}

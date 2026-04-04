package mindustrytool.features.chat.global;

import java.util.concurrent.CompletableFuture;

import arc.util.serialization.Jval;
import arc.util.serialization.Json;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.auth.AuthHttp;
import mindustrytool.features.chat.global.dto.ChannelDto;
import mindustrytool.features.chat.global.dto.ChatMessage;
import mindustrytool.features.chat.global.dto.ChatUser;

public class ChatApiClient {
    private static final String CHANNELS_ENDPOINT = "chats/channels";
    private static final String USERS_ENDPOINT = "chats/users";
    private static final String USERS_COUNT_ENDPOINT = "chats/users/count";
    private static final String MESSAGES_ENDPOINT = "chats";
    private static final String STATE_ENDPOINT = "chats/users/state";
    private static final String CONTENT_TYPE_HEADER = "Content-Type";
    private static final String APPLICATION_JSON = "application/json";

    public CompletableFuture<ChannelDto[]> getChannels() {
        return submit(AuthHttp.get(url(CHANNELS_ENDPOINT)), result -> parseJson(ChannelDto[].class, result));
    }

    public CompletableFuture<ChatMessage> sendMessage(String channelId, String content, String replyTo, ContentType type) {
        validateChannelId(channelId);

        Jval payload = Jval.newObject();
        payload.put("content", content);
        payload.put("channelId", channelId);

        if (replyTo != null && !replyTo.isEmpty()) {
            payload.put("replyTo", replyTo);
        }

        return submit(
                AuthHttp.post(url(type.getEndpoint()), payload.toString())
                        .header(CONTENT_TYPE_HEADER, APPLICATION_JSON),
                result -> Utils.fromJson(ChatMessage.class, result));
    }

    public CompletableFuture<ChatUser[]> getChatUsers(String channelId) {
        validateChannelId(channelId);
        return submit(AuthHttp.get(url(USERS_ENDPOINT) + "?channelId=" + channelId),
                result -> parseJson(ChatUser[].class, result));
    }

    public CompletableFuture<Integer> getChatUserCount(String channelId) {
        validateChannelId(channelId);
        return submit(AuthHttp.get(url(USERS_COUNT_ENDPOINT) + "?channelId=" + channelId), Integer::parseInt);
    }

    public CompletableFuture<ChatMessage[]> getMessages(String channelId, String cursor) {
        validateChannelId(channelId);

        String requestUrl = url(MESSAGES_ENDPOINT) + "?channelId=" + channelId;
        if (cursor != null && !cursor.isEmpty()) {
            requestUrl += "&cursor=" + cursor;
        }

        return submit(AuthHttp.get(requestUrl), result -> parseJson(ChatMessage[].class, result));
    }

    public CompletableFuture<Void> updateState(String state) {
        Jval payload = Jval.newObject();
        payload.put("state", state);

        return submit(AuthHttp.put(url(STATE_ENDPOINT))
                .content(payload.toString())
                .header(CONTENT_TYPE_HEADER, APPLICATION_JSON), result -> null);
    }

    private void validateChannelId(String channelId) {
        if (channelId == null) {
            throw new IllegalArgumentException("Channel ID cannot be null");
        }
    }

    private String url(String endpoint) {
        return Config.API_v4_URL + endpoint;
    }

    private <T> CompletableFuture<T> submit(AuthHttp.AuthRequest request, ResponseParser<T> parser) {
        CompletableFuture<T> future = new CompletableFuture<>();

        request.error(future::completeExceptionally)
                .submit(response -> {
                    try {
                        T value = parser.parse(response.getResultAsString());
                        future.complete(value);
                    } catch (Exception e) {
                        future.completeExceptionally(e);
                    }
                });

        return future;
    }

    private <T> T parseJson(Class<T> type, String json) {
        Json parser = new Json();
        T value = parser.fromJson(type, json);
        return value != null ? value : createEmptyValue(type);
    }

    @SuppressWarnings("unchecked")
    private <T> T createEmptyValue(Class<T> type) {
        if (type == ChannelDto[].class) {
            return (T) new ChannelDto[0];
        }
        if (type == ChatUser[].class) {
            return (T) new ChatUser[0];
        }
        if (type == ChatMessage[].class) {
            return (T) new ChatMessage[0];
        }
        return null;
    }

    @FunctionalInterface
    private interface ResponseParser<T> {
        T parse(String result) throws Exception;
    }
}

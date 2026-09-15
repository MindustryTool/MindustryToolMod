package mindustrytool.services;

import arc.Core;
import arc.util.Nullable;
import arc.util.serialization.Jval;
import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow;
import java.util.concurrent.SubmissionPublisher;
import mindustrytool.Config;
import mindustrytool.models.request.CrashReportRequest;
import mindustrytool.models.request.LogoutRequest;
import mindustrytool.models.request.RefreshTokenRequest;
import mindustrytool.models.request.SendChatMessageRequest;
import mindustrytool.models.request.UpdateChatStateRequest;
import mindustrytool.models.request.UserBatchRequest;
import mindustrytool.models.response.AuthTokenResponse;
import mindustrytool.models.response.ChannelDto;
import mindustrytool.models.response.ChatMessage;
import mindustrytool.models.response.ChatUser;
import mindustrytool.models.response.LoginUriResponse;
import mindustrytool.models.response.MapData;
import mindustrytool.models.response.MapDetailData;
import mindustrytool.models.response.ModData;
import mindustrytool.models.response.PlayerConnectProvider;
import mindustrytool.models.response.PlayerConnectRoom;
import mindustrytool.models.response.SchematicData;
import mindustrytool.models.response.SchematicDetailData;
import mindustrytool.models.response.ServerData;
import mindustrytool.models.response.TagCategory;
import mindustrytool.models.response.UserData;
import mindustrytool.models.response.UserSession;
import mindustrytool.services.Request.BodyHandlers;
import mindustrytool.services.auth.MindustryAuthProvider;
import mindustrytool.utils.JsonUtils;

public final class MindustryTool {

    private static final Duration LONG_TIMEOUT = Duration.ofSeconds(60);

    private static final Request api = Request.builder()
            .baseUrl(Config.API_URL)
            .timeout(Duration.ofSeconds(10))
            .authProvider(MindustryAuthProvider.getInstance())
            .header("mid", getMid())
            .build();

    private static final Request publicApi = Request.builder()
            .baseUrl(Config.API_URL)
            .timeout(Duration.ofSeconds(10))
            .header("mid", getMid())
            .build();

    private MindustryTool() {
    }

    // ─── Ping ──────────────────────────────────────────────────────

    public static CompletableFuture<Void> ping(String client) {
        return publicApi.get("/ping?client=" + client).sendAsync().thenAccept(r -> {
        });
    }

    // ─── Maps ──────────────────────────────────────────────────────

    public static CompletableFuture<byte[]> downloadMap(String itemId) {
        return publicApi
                .get("/maps/" + itemId + "/data")
                .timeout(LONG_TIMEOUT)
                .sendAsync(BodyHandlers.ofByteArray())
                .thenApply(r -> r.body());
    }

    public static CompletableFuture<MapDetailData> findMap(String itemId) {
        return publicApi
                .get("/maps/" + itemId)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJson(MapDetailData.class, r.body()));
    }

    public static CompletableFuture<List<MapData>> searchMaps(
            int page, int size, String sort, String name, List<String> tags,
            @Nullable String author, @Nullable String verification) {
        return publicApi
                .get("/maps")
                .query("page", page)
                .query("size", Math.min(size, 100))
                .query("sort", sort)
                .query("name", name)
                .query("tags", tags)
                .query("author", author)
                .query("verification", verification)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJsonArray(MapData.class, r.body()));
    }

    // ─── Schematics ────────────────────────────────────────────────

    public static CompletableFuture<byte[]> downloadSchematic(String itemId) {
        return publicApi
                .get("/schematics/" + itemId + "/data")
                .timeout(LONG_TIMEOUT)
                .sendAsync(BodyHandlers.ofByteArray())
                .thenApply(r -> r.body());
    }

    public static CompletableFuture<SchematicDetailData> findSchematic(String itemId) {
        return publicApi
                .get("/schematics/" + itemId)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJson(SchematicDetailData.class, r.body()));
    }

    public static CompletableFuture<List<SchematicData>> searchSchematics(
            int page, int size, String sort, String name, List<String> tags,
            List<String> blocks, @Nullable String author, @Nullable String verification) {
        return publicApi
                .get("/schematics")
                .query("page", page)
                .query("size", Math.min(size, 100))
                .query("sort", sort)
                .query("name", name)
                .query("tags", tags)
                .query("blocks", blocks)
                .query("author", author)
                .query("verification", verification)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJsonArray(SchematicData.class, r.body()));
    }

    // ─── Tags ──────────────────────────────────────────────────────

    public static CompletableFuture<List<TagCategory>> getTags(String group) {
        return publicApi
                .get("/tags?group=" + group)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJsonArray(TagCategory.class, r.body()));
    }

    // ─── Users ─────────────────────────────────────────────────────

    public static CompletableFuture<List<UserData>> getUserBatch(List<String> ids) {
        String json = JsonUtils.toJson(new UserBatchRequest(ids));
        return publicApi
                .post("/users/batches")
                .json(json)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJsonArray(UserData.class, r.body()));
    }

    // ─── Planets ───────────────────────────────────────────────────

    public static CompletableFuture<List<ModData>> getPlanets() {
        return publicApi.get("/planets").sendAsync().thenApply(r -> JsonUtils.fromJsonArray(ModData.class, r.body()));
    }

    // ─── Servers ───────────────────────────────────────────────────

    public static CompletableFuture<List<ServerData>> getServers(int page, int size) {
        return publicApi
                .get("/servers?page=" + page + "&size=" + size)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJsonArray(ServerData.class, r.body()));
    }

    // ─── Player Connect ────────────────────────────────────────────

    public static CompletableFuture<List<PlayerConnectRoom>> searchRooms(String query) {
        return publicApi
                .get("/player-connect/rooms?q=" + query)
                .timeout(LONG_TIMEOUT)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJsonArray(PlayerConnectRoom.class, r.body()));
    }

    public static CompletableFuture<List<PlayerConnectProvider>> getProviders() {
        return publicApi
                .get("/player-connect/providers")
                .timeout(LONG_TIMEOUT)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJsonArray(PlayerConnectProvider.class, r.body()));
    }

    // ─── Chat ──────────────────────────────────────────────────────

    public static CompletableFuture<List<ChannelDto>> getChatChannels() {
        return api.get("/chats/channels")
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJsonArray(ChannelDto.class, r.body()));
    }

    public static CompletableFuture<ChatMessage> sendChatMessage(
            String endpoint, String channelId, String content, String replyTo) {
        String normalizedReplyTo = (replyTo != null && !replyTo.isEmpty()) ? replyTo : null;
        SendChatMessageRequest payload = new SendChatMessageRequest(content, channelId, normalizedReplyTo);
        return api.post("/chats/" + endpoint)
                .json(JsonUtils.toJson(payload))
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJson(ChatMessage.class, r.body()));
    }

    public static CompletableFuture<List<ChatUser>> getChatUsers(String channelId) {
        return api.get("/chats/users?channelId=" + channelId)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJsonArray(ChatUser.class, r.body()));
    }

    public static CompletableFuture<Integer> getChatUserCount(String channelId) {
        return api.get("/chats/users/count?channelId=" + channelId)
                .sendAsync()
                .thenApply(r -> Integer.parseInt(r.body().trim()));
    }

    public static CompletableFuture<List<ChatMessage>> getChatMessages(String channelId, String cursor) {
        String u = "/chats?channelId=" + channelId;
        if (cursor != null && !cursor.isEmpty()) {
            u += "&cursor=" + cursor;
        }
        return api.get(u).sendAsync().thenApply(r -> JsonUtils.fromJsonArray(ChatMessage.class, r.body()));
    }

    public static CompletableFuture<Void> updateChatState(String state) {
        String json = JsonUtils.toJson(new UpdateChatStateRequest(state));
        return api.put("/chats/users/state").json(json).sendAsync().thenAccept(r -> {
        });
    }

    public static CompletableFuture<Flow.Publisher<String>> chatStream(String chatId) {
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
        api.get("/chats/stream")
                .header("Accept", "text/event-stream")
                .header("x-chat-id", chatId)
                .timeout(Duration.ofMillis(0))
                .sendAsync(BodyHandlers.ofLines())
                .thenAccept(response -> {
                    response.body().forEach(publisher::submit);
                    publisher.close();
                })
                .exceptionally(e -> {
                    publisher.close();
                    return null;
                });
        return CompletableFuture.completedFuture(publisher);
    }

    // ─── Player Connect ────────────────────────────────────────────

    public static CompletableFuture<Flow.Publisher<String>> playerConnectStream() {
        SubmissionPublisher<String> publisher = new SubmissionPublisher<>();
        publicApi.get("/player-connect/sse")
                .header("Accept", "text/event-stream")
                .timeout(Duration.ofMillis(0))
                .sendAsync(BodyHandlers.ofLines())
                .thenAccept(response -> {
                    response.body().forEach(publisher::submit);
                    publisher.close();
                })
                .exceptionally(e -> {
                    publisher.close();
                    return null;
                });
        return CompletableFuture.completedFuture(publisher);
    }

    public static CompletableFuture<List<PlayerConnectRoom>> getPlayerConnectRooms(@Nullable String query) {
        var req = publicApi.get("/player-connect/rooms");
        if (query != null && !query.trim().isEmpty()) {
            req.query("q", query);
        }
        return req.sendAsync().thenApply(r -> JsonUtils.fromJsonArray(PlayerConnectRoom.class, r.body()));
    }

    public static CompletableFuture<List<PlayerConnectProvider>> getPlayerConnectProviders() {
        return publicApi.get("/player-connect/providers")
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJsonArray(PlayerConnectProvider.class, r.body()));
    }

    // ─── Auth ──────────────────────────────────────────────────────

    public static CompletableFuture<UserSession> getSession() {
        return api.get("/auth/session").sendAsync().thenApply(r -> {
            String body = r.body();
            if (body == null || body.isEmpty() || body.equals("null")) {
                return null;
            }
            return JsonUtils.fromJson(UserSession.class, body);
        });
    }

    public static CompletableFuture<LoginUriResponse> getLoginUri() {
        return publicApi
                .get("/auth/app/login-uri")
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJson(LoginUriResponse.class, r.body()));
    }

    public static CompletableFuture<AuthTokenResponse> pollLoginToken(String loginId) {
        return publicApi
                .get("/auth/app/login-token?loginId=" + loginId)
                .timeout(LONG_TIMEOUT)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJson(AuthTokenResponse.class, r.body()));
    }

    public static CompletableFuture<Void> logout(String accessToken, String refreshToken) {
        String json = JsonUtils.toJson(new LogoutRequest(accessToken, refreshToken));
        // Use withoutAuth and manually attach token to avoid refresh recursion
        if (accessToken != null && !accessToken.isEmpty()) {
            return publicApi
                    .post("/auth/app/logout")
                    .withoutAuth()
                    .header("Authorization", "Bearer " + accessToken)
                    .json(json)
                    .sendAsync()
                    .thenAccept(r -> {
                    });
        } else {
            return publicApi
                    .post("/auth/app/logout")
                    .withoutAuth()
                    .json(json)
                    .sendAsync()
                    .thenAccept(r -> {
                    });
        }
    }

    public static CompletableFuture<AuthTokenResponse> refreshToken(String refreshToken) {
        String json = JsonUtils.toJson(new RefreshTokenRequest(refreshToken));
        return publicApi
                .post("/auth/app/refresh")
                .withoutAuth()
                .json(json)
                .sendAsync()
                .thenApply(r -> JsonUtils.fromJson(AuthTokenResponse.class, r.body()));
    }

    // ─── Crash Report ──────────────────────────────────────────────

    public static CompletableFuture<Void> submitCrashReport(String crashData) {
        String json = JsonUtils.toJson(new CrashReportRequest(crashData));
        return publicApi.post("/crashes").json(json).sendAsync().thenAccept(r -> {
        });
    }

    // ─── Images ────────────────────────────────────────────────────

    public static CompletableFuture<byte[]> downloadSchematicImage(String itemId, boolean preview) {
        String u = "/schematics/" + itemId + "/image.png";
        if (preview)
            u += "?variant=preview";
        return publicApi
                .get(u)
                .timeout(LONG_TIMEOUT)
                .sendAsync(BodyHandlers.ofByteArray())
                .thenApply(r -> r.body());
    }

    public static CompletableFuture<byte[]> downloadMapImage(String itemId, boolean preview) {
        String u = "/maps/" + itemId + "/image.png";
        if (preview)
            u += "?variant=preview";
        return publicApi
                .get(u)
                .timeout(LONG_TIMEOUT)
                .sendAsync(BodyHandlers.ofByteArray())
                .thenApply(r -> r.body());
    }

    // ─── Translation ────────────────────────────────────────────────
    public static CompletableFuture<String> translate(String content, String targetLanguage) {
        Jval body = Jval.newObject();
        body.put("content", content);
        body.put("target", targetLanguage);
        return api.post("/translations/translate")
                .json(body.toString())
                .sendAsync()
                .thenApply(Request.Response::body);
    }

    private static synchronized String getMid() {
        String midKey = "mod-mid-key";
        String mid = Core.settings.getString(midKey);
        if (mid == null || mid.isEmpty()) {
            mid = "mod:" + UUID.randomUUID().toString();
            Core.settings.put(midKey, mid);
        }

        return mid;
    }
}

package mindustrytool.features.chat.state;

import arc.util.Nullable;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import mindustrytool.models.response.ChatUser;
import mindustrytool.services.MindustryTool;
import solim.reactive.Query;
import solim.reactive.QueryKey;
import solim.reactive.Readable;

public final class ChatMembers {

    private final Query<List<ChatUser>> query;

    public ChatMembers(Readable<String> activeChannelId) {
        this.query = Query.<List<ChatUser>>builder()
                .key(QueryKey.of("chat", "members"))
                .fetch(() -> {
                    String channelId = activeChannelId.get();
                    if (channelId == null || channelId.isEmpty()) {
                        return CompletableFuture.completedFuture(Collections.emptyList());
                    }
                    return MindustryTool.getChatUsers(channelId);
                })
                .staleTime(Duration.ofSeconds(30))
                .build();
    }

    public Query<List<ChatUser>> query() {
        return query;
    }

    public Readable<List<ChatUser>> active() {
        return query.data().map(list -> list != null ? list : Collections.emptyList());
    }

    public List<ChatUser> currentActive() {
        List<ChatUser> list = query.data().peek();
        return list != null ? list : Collections.emptyList();
    }

    public Readable<Boolean> activeLoading() {
        return query.loading();
    }

    public boolean isActiveLoading() {
        return Boolean.TRUE.equals(query.loading().peek());
    }

    public Readable<String> activeError() {
        return query.error().map(err -> err != null ? (err.getMessage() != null ? err.getMessage() : err.toString()) : null);
    }

    public @Nullable String currentActiveError() {
        Throwable err = query.error().peek();
        return err != null ? (err.getMessage() != null ? err.getMessage() : err.toString()) : null;
    }

    public void dispose() {
        query.dispose();
    }
}

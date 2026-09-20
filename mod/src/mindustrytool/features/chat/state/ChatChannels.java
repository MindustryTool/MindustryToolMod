package mindustrytool.features.chat.state;

import arc.util.Nullable;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import mindustrytool.models.response.ChannelDto;
import mindustrytool.services.MindustryTool;
import solim.reactive.Computed;
import solim.reactive.Effect;
import solim.reactive.Query;
import solim.reactive.QueryKey;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class ChatChannels {

    private final Query<List<ChannelDto>> channelsQuery = Query.of(
            QueryKey.of("chat", "channels"),
            MindustryTool::getChatChannels
    ).staleTime(Duration.ofSeconds(30));
    private final Signal<String> activeChannelId;

    private final Computed<ChannelDto> active;

    public ChatChannels(Signal<String> activeChannelId) {
        this.activeChannelId = activeChannelId;
        this.active = new Computed<>(() -> {
            String id = activeChannelId.get();
            if (id == null) {
                return null;
            }
            List<ChannelDto> list = channelsQuery.data().get();
            if (list == null) {
                return null;
            }
            for (ChannelDto c : list) {
                if (Objects.equals(c.getId(), id)) {
                    return c;
                }
            }
            return null;
        });

        Effect.of(() -> {
            List<ChannelDto> list = channelsQuery.data().get();
            if (list != null && !list.isEmpty()) {
                String current = activeChannelId.peek();
                boolean exists = false;
                if (current != null) {
                    for (ChannelDto c : list) {
                        if (Objects.equals(c.getId(), current)) {
                            exists = true;
                            break;
                        }
                    }
                }
                if (!exists) {
                    activeChannelId.set(list.get(0).getId());
                }
            } else if (list != null && list.isEmpty()) {
                activeChannelId.set(null);
            }
        });
    }

    public Query<List<ChannelDto>> channelsQuery() {
        return channelsQuery;
    }

    public Readable<List<ChannelDto>> all() {
        return channelsQuery.data().map(list -> list != null ? list : Collections.emptyList());
    }

    public Readable<Boolean> loading() {
        return channelsQuery.loading();
    }

    public boolean isLoading() {
        return Boolean.TRUE.equals(channelsQuery.loading().peek());
    }

    public Readable<String> error() {
        return channelsQuery.error().map(e -> e != null ? (e.getMessage() != null ? e.getMessage() : e.toString()) : null);
    }

    public @Nullable String currentError() {
        Throwable t = channelsQuery.error().peek();
        return t != null ? (t.getMessage() != null ? t.getMessage() : t.toString()) : null;
    }

    public Signal<String> activeId() {
        return activeChannelId;
    }

    public @Nullable String currentActiveId() {
        return activeChannelId.peek();
    }

    public Readable<ChannelDto> active() {
        return active;
    }

    public @Nullable ChannelDto currentActive() {
        return active.peek();
    }

    public void replace(@Nullable List<ChannelDto> newChannels) {
        List<ChannelDto> normalized = newChannels != null ? new ArrayList<>(newChannels) : Collections.emptyList();
        channelsQuery.mutate(Collections.unmodifiableList(normalized));

        if (!normalized.isEmpty()) {
            String current = activeChannelId.peek();
            boolean exists = false;
            if (current != null) {
                for (ChannelDto c : normalized) {
                    if (Objects.equals(c.getId(), current)) {
                        exists = true;
                        break;
                    }
                }
            }
            if (!exists) {
                activeChannelId.set(normalized.get(0).getId());
            }
        } else {
            activeChannelId.set(null);
        }
    }

    public void select(@Nullable String channelId) {
        if (!Objects.equals(activeChannelId.peek(), channelId)) {
            activeChannelId.set(channelId);
        }
    }
}

package mindustrytool.features.chat.state;

import arc.util.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import mindustrytool.models.response.ChannelDto;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class ChatChannels {

    private final Signal<List<ChannelDto>> channels = Signal.of(Collections.emptyList());
    private final Signal<String> activeChannelId;
    private final Signal<Boolean> loading = Signal.of(false);
    private final Signal<String> error = Signal.of(null);

    private final Computed<ChannelDto> active;

    public ChatChannels(Signal<String> activeChannelId) {
        this.activeChannelId = activeChannelId;
        this.active = new Computed<>(() -> {
            String id = activeChannelId.get();
            if (id == null) {
                return null;
            }
            for (ChannelDto c : channels.get()) {
                if (Objects.equals(c.getId(), id)) {
                    return c;
                }
            }
            return null;
        });
    }

    public Readable<List<ChannelDto>> all() {
        return channels;
    }

    public Readable<Boolean> loading() {
        return loading;
    }

    public boolean isLoading() {
        return Boolean.TRUE.equals(loading.peek());
    }

    public void setLoading(boolean isLoading) {
        loading.set(isLoading);
    }

    public Readable<String> error() {
        return error;
    }

    public @Nullable String currentError() {
        return error.peek();
    }

    public void setError(@Nullable String errorMessage) {
        error.set(errorMessage);
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
        channels.set(Collections.unmodifiableList(normalized));

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

package mindustrytool.features.chat.state;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mindustrytool.models.response.ChatUser;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class ChatMembers {

    private final Signal<Map<String, List<ChatUser>>> members = Signal.of(new HashMap<>());
    private final Signal<Map<String, Boolean>> loading = Signal.of(new HashMap<>());
    private final Signal<Map<String, String>> errors = Signal.of(new HashMap<>());
    private final Map<String, Readable<List<ChatUser>>> channelComputeds = new HashMap<>();

    private final Computed<List<ChatUser>> active;
    private final Computed<Boolean> activeLoading;
    private final Computed<String> activeError;

    public ChatMembers(Readable<String> activeChannelId) {
        this.active = new Computed<>(() -> {
            String id = activeChannelId.get();
            if (id == null) {
                return Collections.emptyList();
            }
            List<ChatUser> list = members.get().get(id);
            return list != null ? list : Collections.emptyList();
        });

        this.activeLoading = new Computed<>(() -> {
            String id = activeChannelId.get();
            return id != null && Boolean.TRUE.equals(loading.get().get(id));
        });

        this.activeError = new Computed<>(() -> {
            String id = activeChannelId.get();
            return id != null ? errors.get().get(id) : null;
        });
    }

    public Readable<List<ChatUser>> active() {
        return active;
    }

    public List<ChatUser> currentActive() {
        return active.peek();
    }

    public Readable<Boolean> activeLoading() {
        return activeLoading;
    }

    public boolean isActiveLoading() {
        return Boolean.TRUE.equals(activeLoading.peek());
    }

    public Readable<String> activeError() {
        return activeError;
    }

    public @Nullable String currentActiveError() {
        return activeError.peek();
    }

    public void setLoading(@Nullable String channelId, boolean isLoading) {
        if (channelId == null) {
            return;
        }
        Map<String, Boolean> next = new HashMap<>(loading.peek());
        next.put(channelId, isLoading);
        loading.set(next);
    }

    public boolean isLoading(@Nullable String channelId) {
        return channelId != null && Boolean.TRUE.equals(loading.peek().get(channelId));
    }

    public void setError(@Nullable String channelId, @Nullable String errorMessage) {
        if (channelId == null) {
            return;
        }
        Map<String, String> next = new HashMap<>(errors.peek());
        if (errorMessage != null) {
            next.put(channelId, errorMessage);
        } else {
            next.remove(channelId);
        }
        errors.set(next);
    }

    public @Nullable String getError(@Nullable String channelId) {
        return channelId != null ? errors.peek().get(channelId) : null;
    }

    public Readable<List<ChatUser>> forChannel(@Nullable String channelId) {
        if (channelId == null) {
            return Readable.of(Collections.emptyList());
        }
        Readable<List<ChatUser>> existing = channelComputeds.get(channelId);
        if (existing == null) {
            existing = members.map(map -> {
                List<ChatUser> list = map.get(channelId);
                return list != null ? list : Collections.emptyList();
            });
            channelComputeds.put(channelId, existing);
        }
        return existing;
    }

    public void replace(@Nullable String channelId, @Nullable List<ChatUser> userList) {
        if (channelId == null) return;
        List<ChatUser> list = userList != null ? new ArrayList<>(userList) : Collections.emptyList();
        Map<String, List<ChatUser>> next = new HashMap<>(members.peek());
        next.put(channelId, Collections.unmodifiableList(list));
        members.set(next);
        setError(channelId, null);
    }
}

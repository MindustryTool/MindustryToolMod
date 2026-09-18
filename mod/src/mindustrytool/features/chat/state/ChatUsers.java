package mindustrytool.features.chat.state;

import arc.util.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mindustrytool.models.response.UserData;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class ChatUsers {

    private final Signal<Map<String, UserData>> cache = Signal.of(new HashMap<>());
    private final Map<String, Readable<UserData>> userComputeds = new HashMap<>();

    public Readable<Map<String, UserData>> all() {
        return cache;
    }

    public Map<String, UserData> currentAll() {
        return cache.peek();
    }

    public Readable<UserData> get(@Nullable String userId) {
        if (userId == null || userId.isEmpty()) {
            return Readable.of(null);
        }
        Readable<UserData> existing = userComputeds.get(userId);
        if (existing == null) {
            existing = cache.map(map -> map.get(userId));
            userComputeds.put(userId, existing);
        }
        return existing;
    }

    public @Nullable UserData getDirect(@Nullable String userId) {
        return userId != null ? cache.peek().get(userId) : null;
    }

    public boolean contains(@Nullable String userId) {
        return userId != null && cache.peek().containsKey(userId);
    }

    public void put(@Nullable UserData user) {
        if (user == null || user.getId() == null) return;
        Map<String, UserData> next = new HashMap<>(cache.peek());
        next.put(user.getId(), user);
        cache.set(next);
    }

    public void putAll(@Nullable List<UserData> users) {
        if (users == null || users.isEmpty()) return;
        Map<String, UserData> next = new HashMap<>(cache.peek());
        for (UserData u : users) {
            if (u != null && u.getId() != null) {
                next.put(u.getId(), u);
            }
        }
        cache.set(next);
    }
}

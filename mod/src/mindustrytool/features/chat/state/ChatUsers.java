package mindustrytool.features.chat.state;

import arc.util.Nullable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mindustrytool.models.response.UserData;
import solim.reactive.MapSignal;
import solim.reactive.Readable;

public final class ChatUsers {

    private final MapSignal<String, UserData> users = MapSignal.of();

    public Readable<Map<String, UserData>> all() {
        return users;
    }

    public Map<String, UserData> currentAll() {
        return users.peek();
    }

    public Readable<UserData> get(@Nullable String userId) {
        return userId == null || userId.isEmpty() ? Readable.of(null) : users.readable(userId);
    }

    public @Nullable UserData getDirect(@Nullable String userId) {
        return userId != null ? users.peek().get(userId) : null;
    }

    public boolean contains(@Nullable String userId) {
        return userId != null && users.peek().containsKey(userId);
    }

    public void put(@Nullable UserData user) {
        if (user == null || user.getId() == null) return;
        users.put(user.getId(), user);
    }

    public void putAll(@Nullable List<UserData> userList) {
        if (userList == null || userList.isEmpty()) return;
        Map<String, UserData> batch = new HashMap<>();
        for (UserData u : userList) {
            if (u != null && u.getId() != null) {
                batch.put(u.getId(), u);
            }
        }
        users.putAll(batch);
    }
}

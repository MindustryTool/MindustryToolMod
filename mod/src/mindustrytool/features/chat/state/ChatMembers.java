package mindustrytool.features.chat.state;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import mindustrytool.models.response.ChatUser;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

public final class ChatMembers {

    private final Signal<Map<String, List<ChatUser>>> members = Signal.of(new HashMap<>());
    private final Map<String, Readable<List<ChatUser>>> channelComputeds = new HashMap<>();
    private final Computed<List<ChatUser>> active;

    public ChatMembers(Readable<String> activeChannelId) {
        this.active = new Computed<>(() -> {
            String id = activeChannelId.get();
            if (id == null) {
                return Collections.emptyList();
            }
            List<ChatUser> list = members.get().get(id);
            return list != null ? list : Collections.emptyList();
        });
    }

    public Readable<List<ChatUser>> active() {
        return active;
    }

    public List<ChatUser> currentActive() {
        return active.peek();
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
    }
}

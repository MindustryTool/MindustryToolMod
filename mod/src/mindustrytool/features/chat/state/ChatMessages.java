package mindustrytool.features.chat.state;

import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import mindustrytool.models.response.ChatMessage;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

public final class ChatMessages {

    private final Signal<Map<String, List<ChatMessage>>> messages = Signal.of(new HashMap<>());
    private final Signal<Map<String, Boolean>> fullyLoaded = Signal.of(new HashMap<>());
    private final Signal<Boolean> loadingOlder = Signal.of(false);
    private final Signal<Map<String, Boolean>> loadingInitial = Signal.of(new HashMap<>());
    private final Signal<Map<String, String>> errors = Signal.of(new HashMap<>());

    private final Map<String, Readable<List<ChatMessage>>> channelComputeds = new HashMap<>();

    private final Computed<List<ChatMessage>> active;
    private final Computed<Boolean> activeFullyLoaded;
    private final Computed<Boolean> activeLoadingInitial;
    private final Computed<String> activeError;

    public ChatMessages(Readable<String> activeChannelId) {
        this.active = new Computed<>(() -> {
            String id = activeChannelId.get();
            if (id == null) {
                return Collections.emptyList();
            }
            List<ChatMessage> list = messages.get().get(id);
            return list != null ? list : Collections.emptyList();
        });

        this.activeFullyLoaded = new Computed<>(() -> {
            String id = activeChannelId.get();
            return id != null && Boolean.TRUE.equals(fullyLoaded.get().get(id));
        });

        this.activeLoadingInitial = new Computed<>(() -> {
            String id = activeChannelId.get();
            return id != null && Boolean.TRUE.equals(loadingInitial.get().get(id));
        });

        this.activeError = new Computed<>(() -> {
            String id = activeChannelId.get();
            return id != null ? errors.get().get(id) : null;
        });
    }

    public Readable<List<ChatMessage>> active() {
        return active;
    }

    public List<ChatMessage> currentActive() {
        return active.peek();
    }

    public Readable<Boolean> activeFullyLoaded() {
        return activeFullyLoaded;
    }

    public Readable<Boolean> activeLoadingInitial() {
        return activeLoadingInitial;
    }

    public boolean isActiveLoadingInitial() {
        return Boolean.TRUE.equals(activeLoadingInitial.peek());
    }

    public Readable<String> activeError() {
        return activeError;
    }

    public @Nullable String currentActiveError() {
        return activeError.peek();
    }

    public void setLoadingInitial(@Nullable String channelId, boolean loading) {
        if (channelId == null) {
            return;
        }
        Map<String, Boolean> next = new HashMap<>(loadingInitial.peek());
        next.put(channelId, loading);
        loadingInitial.set(next);
    }

    public boolean isLoadingInitial(@Nullable String channelId) {
        return channelId != null && Boolean.TRUE.equals(loadingInitial.peek().get(channelId));
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

    public Readable<Boolean> loadingOlder() {
        return loadingOlder;
    }

    public boolean isLoadingOlder() {
        return Boolean.TRUE.equals(loadingOlder.peek());
    }

    public void setLoadingOlder(boolean loading) {
        loadingOlder.set(loading);
    }

    public Readable<List<ChatMessage>> forChannel(@Nullable String channelId) {
        if (channelId == null) {
            return Readable.of(Collections.emptyList());
        }
        Readable<List<ChatMessage>> existing = channelComputeds.get(channelId);
        if (existing == null) {
            existing = messages.map(map -> {
                List<ChatMessage> list = map.get(channelId);
                return list != null ? list : Collections.emptyList();
            });
            channelComputeds.put(channelId, existing);
        }
        return existing;
    }

    public Readable<Boolean> fullyLoaded(@Nullable String channelId) {
        return fullyLoaded.map(map -> channelId != null && Boolean.TRUE.equals(map.get(channelId)));
    }

    public boolean isFullyLoaded(@Nullable String channelId) {
        return channelId != null && Boolean.TRUE.equals(fullyLoaded.peek().get(channelId));
    }

    public void setFullyLoaded(@Nullable String channelId, boolean isFullyLoaded) {
        if (channelId == null)
            return;
        Map<String, Boolean> next = new HashMap<>(fullyLoaded.peek());
        next.put(channelId, isFullyLoaded);
        fullyLoaded.set(next);
    }

    public void replace(@Nullable String channelId, @Nullable List<ChatMessage> newMessages) {
        if (channelId == null)
            return;
        List<ChatMessage> list = newMessages != null ? new ArrayList<>(newMessages) : Collections.emptyList();
        Map<String, List<ChatMessage>> next = copyState();
        next.put(channelId, Collections.unmodifiableList(list));
        messages.set(next);
        setError(channelId, null);
    }

    public int prepend(@Nullable String channelId, @Nullable List<ChatMessage> oldMessages) {
        if (channelId == null || oldMessages == null || oldMessages.isEmpty()) {
            return 0;
        }
        List<ChatMessage> existing = copyChannel(channelId);
        List<ChatMessage> merged = new ArrayList<>();
        for (ChatMessage m : oldMessages) {
            if (!containsMessage(existing, m.getId())) {
                merged.add(m);
            }
        }
        if (merged.isEmpty()) {
            return 0;
        }
        int added = merged.size();
        merged.addAll(existing);
        setChannel(channelId, merged);
        return added;
    }

    public boolean append(@Nullable ChatMessage message) {
        if (message == null || message.getChannelId() == null) {
            return false;
        }
        String chId = message.getChannelId();
        List<ChatMessage> existing = copyChannel(chId);
        if (containsMessage(existing, message.getId())) {
            return false;
        }
        existing.add(message);
        setChannel(chId, existing);
        return true;
    }

    public boolean confirm(@Nullable String tempId, @Nullable ChatMessage realMsg) {
        if (tempId == null || realMsg == null || realMsg.getChannelId() == null) {
            return false;
        }
        String chId = realMsg.getChannelId();
        List<ChatMessage> list = copyChannel(chId);

        // If real message already arrived (e.g. via live stream), simply remove tempId
        if (containsMessage(list, realMsg.getId())) {
            boolean removed = removeMessage(list, tempId);
            if (removed) {
                setChannel(chId, list);
            }
            return removed;
        }

        // Replace tempId with realMsg in place
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).getId(), tempId)) {
                list.set(i, realMsg);
                setChannel(chId, list);
                return true;
            }
        }
        return false;
    }

    public boolean remove(@Nullable String channelId, @Nullable String messageId) {
        if (channelId == null || messageId == null) {
            return false;
        }
        List<ChatMessage> list = copyChannel(channelId);
        if (removeMessage(list, messageId)) {
            setChannel(channelId, list);
            return true;
        }
        return false;
    }

    public boolean containsGlobally(@Nullable String messageId) {
        if (messageId == null) {
            return false;
        }
        for (List<ChatMessage> list : messages.peek().values()) {
            if (containsMessage(list, messageId)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, List<ChatMessage>> copyState() {
        return new HashMap<>(messages.peek());
    }

    private List<ChatMessage> copyChannel(String channelId) {
        List<ChatMessage> list = messages.peek().get(channelId);
        return list != null ? new ArrayList<>(list) : new ArrayList<>();
    }

    private void setChannel(String channelId, List<ChatMessage> list) {
        Map<String, List<ChatMessage>> next = copyState();
        next.put(channelId, Collections.unmodifiableList(list));
        messages.set(next);
    }

    private boolean containsMessage(@Nullable List<ChatMessage> list, @Nullable String id) {
        if (list == null || id == null)
            return false;
        for (ChatMessage m : list) {
            if (Objects.equals(m.getId(), id)) {
                return true;
            }
        }
        return false;
    }

    private boolean removeMessage(List<ChatMessage> list, @Nullable String id) {
        if (id == null)
            return false;
        for (int i = 0; i < list.size(); i++) {
            if (Objects.equals(list.get(i).getId(), id)) {
                list.remove(i);
                return true;
            }
        }
        return false;
    }
}

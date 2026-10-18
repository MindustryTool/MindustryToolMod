package mindustrytool.features.chat.state;

import arc.Core;
import arc.util.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class ChatNotifications {

    public static final String SETTING_KEY = "mindustrytool.chat.muted-channels";

    private final Signal<Set<String>> mutedChannelIds;

    public ChatNotifications() {
        this(loadFromSettings());
    }

    ChatNotifications(Set<String> initialMuted) {
        this.mutedChannelIds = Signal.of(Collections.unmodifiableSet(new HashSet<>(initialMuted)));
    }

    private static Set<String> loadFromSettings() {
        Set<String> set = new HashSet<>();
        if (Core.settings != null) {
            String raw = Core.settings.getString(SETTING_KEY, "");
            if (raw != null && !raw.trim().isEmpty()) {
                String[] parts = raw.split(",");
                for (String part : parts) {
                    String trimmed = part.trim();
                    if (!trimmed.isEmpty()) {
                        set.add(trimmed);
                    }
                }
            }
        }
        return set;
    }

    private void persistToSettings(Set<String> set) {
        if (Core.settings != null) {
            StringBuilder sb = new StringBuilder();
            boolean first = true;
            for (String id : set) {
                if (!first) {
                    sb.append(",");
                }
                sb.append(id);
                first = false;
            }
            Core.settings.put(SETTING_KEY, sb.toString());
        }
    }

    public Signal<Set<String>> mutedChannelIds() {
        return mutedChannelIds;
    }

    public Readable<Boolean> isMuted(@Nullable String channelId) {
        if (channelId == null) {
            return Readable.of(false);
        }
        return mutedChannelIds.map(set -> set != null && set.contains(channelId));
    }

    public boolean isCurrentlyMuted(@Nullable String channelId) {
        if (channelId == null) {
            return false;
        }
        Set<String> current = mutedChannelIds.peek();
        return current != null && current.contains(channelId);
    }

    public void toggleMuted(@Nullable String channelId) {
        if (channelId == null) {
            return;
        }
        setMuted(channelId, !isCurrentlyMuted(channelId));
    }

    public void setMuted(@Nullable String channelId, boolean muted) {
        if (channelId == null) {
            return;
        }
        Set<String> current = mutedChannelIds.peek();
        boolean alreadyMuted = current != null && current.contains(channelId);
        if (alreadyMuted == muted) {
            return;
        }

        Set<String> next = new HashSet<>(current != null ? current : Collections.emptySet());
        if (muted) {
            next.add(channelId);
        } else {
            next.remove(channelId);
        }

        persistToSettings(next);
        mutedChannelIds.set(Collections.unmodifiableSet(next));
    }
}

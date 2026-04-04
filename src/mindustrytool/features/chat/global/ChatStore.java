package mindustrytool.features.chat.global;

import arc.Core;
import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustrytool.features.chat.global.dto.ChannelDto;
import mindustrytool.features.chat.global.dto.ChatMessage;
import mindustrytool.features.chat.global.dto.ChatUser;
import mindustrytool.features.chat.global.dto.LastReadMessageStore;
import mindustrytool.features.chat.global.events.MessagesUpdateEvent;
import mindustrytool.features.chat.global.events.UsersUpdateEvent;
import mindustrytool.utils.State;

import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatStore {
    private static ChatStore instance;

    public static final String CURRENT_CHANNEL_ID_KEY = "mindustrytool.chat.current-channel-id";
    public static final String LAST_READ_MESSAGES_KEY = "mindustrytool.chat.last-read-messages";

    private final ObjectMap<String, Seq<ChatMessage>> messagesByChannel = new ObjectMap<>();
    private final ObjectSet<String> fullyLoadedChannels = new ObjectSet<>();
    private final ObjectMap<String, Integer> unreadByChannel = new ObjectMap<>();
    private final ObjectMap<String, Seq<ChatUser>> usersByChannel = new ObjectMap<>();
    private final Seq<ChannelDto> channels = new Seq<>();

    private final AtomicBoolean isLoadingMessages = new AtomicBoolean(false);
    private final LastReadMessageStore lastReadMessageStore;

    public final State<String> currentChannel = new State<>(Core.settings.getString(CURRENT_CHANNEL_ID_KEY, null));
    public final State<Seq<ChannelDto>> channelsState = new State<>(new Seq<>());
    public final State<Integer> unreadCountState = new State<>(0);
    public final State<Boolean> loadingMessagesState = new State<>(false);

    public ChatStore() {
        LastReadMessageStore stored = Core.settings.getJson(LAST_READ_MESSAGES_KEY, LastReadMessageStore.class,
                LastReadMessageStore::new);
        lastReadMessageStore = stored != null ? stored : new LastReadMessageStore();
    }

    public static ChatStore getInstance() {
        if (instance == null) {
            instance = new ChatStore();
        }
        return instance;
    }

    public void clearMessages() {
        messagesByChannel.clear();
        fullyLoadedChannels.clear();
        usersByChannel.clear();

    }

    public String getCurrentChannelId() {
        return currentChannel.get();
    }

    public void setCurrentChannelId(String channelId) {
        if (currentChannel.get() != null && currentChannel.get().equals(channelId)) {
            return;
        }

        Core.settings.put(CURRENT_CHANNEL_ID_KEY, channelId);
        int currentUnread = unreadCountState.get() - unreadByChannel.get(channelId, 0);
        if (currentUnread < 0)
            currentUnread = 0;
        unreadByChannel.put(channelId, 0);

        ChannelDto channel = channels.find(c -> c.id.equals(channelId));
        if (channel != null && channel.lastMessageId != null) {
            setLastReadMessageId(channelId, channel.lastMessageId);
        }

        currentChannel.set(channelId);
        unreadCountState.set(currentUnread);
    }

    public Seq<ChannelDto> getChannels() {
        return channels.sort(new Comparator<ChannelDto>() {
            @Override
            public int compare(ChannelDto o1, ChannelDto o2) {
                if (o1.lastMessageId == null || o2.lastMessageId == null) {
                    return 0;
                }
                return -o1.lastMessageId.compareTo(o2.lastMessageId);
            }
        });
    }

    public void setChannels(Seq<ChannelDto> newChannels) {
        channels.clear();
        channels.addAll(newChannels);
        if (currentChannel.get() != null) {
            ChannelDto channel = channels.find(c -> c.id.equals(currentChannel.get()));
            if (channel != null && channel.lastMessageId != null) {
                setLastReadMessageId(currentChannel.get(), channel.lastMessageId);
            }
        }
        channelsState.set(this.channels);
    }

    public Seq<ChatMessage> getMessages(String channelId) {
        return messagesByChannel.get(channelId, new Seq<>());
    }

    public void addMessages(String channelId, Seq<ChatMessage> messages) {
        Seq<ChatMessage> seq = messagesByChannel.get(channelId);

        if (seq == null) {
            seq = new Seq<>();
            messagesByChannel.put(channelId, seq);
        }

        for (ChatMessage msg : messages) {
            if (!seq.contains(m -> m.id.equals(msg.id))) {
                seq.add(msg);
            }
        }

        Events.fire(new MessagesUpdateEvent(channelId, false));
    }

    public void prependMessages(String channelId, Seq<ChatMessage> messages) {
        Seq<ChatMessage> seq = messagesByChannel.get(channelId);
        boolean isInitial = seq == null || seq.isEmpty();

        if (seq == null) {
            seq = new Seq<>();
            messagesByChannel.put(channelId, seq);
        }

        messages.reverse().addAll(seq);
        messagesByChannel.put(channelId, messages);

        if (isInitial) {
            fullyLoadedChannels.remove(channelId);
        }

        Events.fire(new MessagesUpdateEvent(channelId, !isInitial));
    }

    public boolean isFullyLoaded(String channelId) {
        return fullyLoadedChannels.contains(channelId);
    }

    public void setFullyLoaded(String channelId) {
        fullyLoadedChannels.add(channelId);
    }

    public int getUnreadCount() {
        return unreadCountState.get();
    }

    public int getUnreadByChannel(String channelId) {
        return unreadByChannel.get(channelId, 0);
    }

    public void addUnread(String channelId, int count) {
        unreadByChannel.put(channelId, unreadByChannel.get(channelId, 0) + count);
        unreadCountState.set(unreadCountState.get() + count);
    }

    public void resetUnreadCount() {
        unreadByChannel.clear();
        unreadCountState.set(0);
    }

    public String getLastReadMessageId(String channelId) {
        return lastReadMessageStore.lastReadMessageIds.get(channelId);
    }

    public void setLastReadMessageId(String channelId, String messageId) {
        if (messageId == null)
            return;
        if (messageId.equals(lastReadMessageStore.lastReadMessageIds.get(channelId)))
            return;
        lastReadMessageStore.lastReadMessageIds.put(channelId, messageId);

        Core.settings.putJson(LAST_READ_MESSAGES_KEY, LastReadMessageStore.class, lastReadMessageStore);

        unreadCountState.set(unreadCountState.get());
    }

    public Seq<ChatUser> getUsers(String channelId) {
        return usersByChannel.get(channelId, new Seq<>());
    }

    public void setUsers(String channelId, ChatUser[] users) {
        Arrays.sort(users, (u1, u2) -> {
            int l1 = u1.getHighestRole().map(ChatUser.SimpleRole::getLevel).orElse(-1);
            int l2 = u2.getHighestRole().map(ChatUser.SimpleRole::getLevel).orElse(-1);

            if (l1 == l2) {
                return Integer.compare(getStatePriority(u2.getState()), getStatePriority(u1.getState()));
            }

            return Integer.compare(l2, l1);
        });
        usersByChannel.put(channelId, new Seq<>(users));
        Events.fire(new UsersUpdateEvent(channelId));
    }

    public int getStatePriority(String state) {
        if (state.equalsIgnoreCase(ChatStateManager.MENU_STATE)) {
            return 0;
        }

        return 1;
    }

    public boolean isLoadingMessages() {
        return isLoadingMessages.get();
    }

    public void setLoadingMessages(boolean loading) {
        this.isLoadingMessages.set(loading);
        loadingMessagesState.set(loading);
    }

    public boolean compareAndSetLoadingMessages(boolean expect, boolean update) {
        boolean success = this.isLoadingMessages.compareAndSet(expect, update);
        if (success) {
            loadingMessagesState.set(update);
        }
        return success;
    }
}

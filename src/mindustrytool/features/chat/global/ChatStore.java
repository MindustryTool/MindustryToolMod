package mindustrytool.features.chat.global;

import arc.Core;
import arc.Events;
import arc.struct.ObjectMap;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import mindustrytool.features.chat.global.dto.ChannelDto;
import mindustrytool.features.chat.global.dto.ChatMessage;
import mindustrytool.features.chat.global.dto.ChatUser;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicBoolean;

public class ChatStore {
    private static ChatStore instance;

    public static final String CURRENT_CHANNEL_ID_KEY = "mindustrytool-current-channel-id";

    private final ObjectMap<String, Seq<ChatMessage>> messagesByChannel = new ObjectMap<>();
    private final ObjectSet<String> fullyLoadedChannels = new ObjectSet<>();
    private final ObjectMap<String, Integer> unreadByChannel = new ObjectMap<>();
    private final ObjectMap<String, Seq<ChatUser>> usersByChannel = new ObjectMap<>();
    private final Seq<ChannelDto> channels = new Seq<>();

    private String currentChannelId;
    private int unreadCount = 0;
    private final AtomicBoolean isLoadingMessages = new AtomicBoolean(false);

    public ChatStore() {
        currentChannelId = Core.settings.getString(CURRENT_CHANNEL_ID_KEY, null);
    }

    public static ChatStore getInstance() {
        if (instance == null) {
            instance = new ChatStore();
        }
        return instance;
    }

    public void clear() {
        messagesByChannel.clear();
        fullyLoadedChannels.clear();
        unreadByChannel.clear();
        usersByChannel.clear();
        channels.clear();
        unreadCount = 0;
        isLoadingMessages.set(false);
        Events.fire(new StoreUpdateEvent());
    }

    public void clearMessages() {
        messagesByChannel.clear();
        fullyLoadedChannels.clear();
        Events.fire(new StoreUpdateEvent());
    }

    public String getCurrentChannelId() {
        return currentChannelId;
    }

    public void setCurrentChannelId(String currentChannelId) {
        if (this.currentChannelId != null && this.currentChannelId.equals(currentChannelId)) {
            return;
        }

        this.currentChannelId = currentChannelId;
        Core.settings.put(CURRENT_CHANNEL_ID_KEY, currentChannelId);
        unreadCount -= unreadByChannel.get(currentChannelId, 0);
        if (unreadCount < 0)
            unreadCount = 0;
        unreadByChannel.put(currentChannelId, 0);
        Events.fire(new CurrentChannelChangeEvent(currentChannelId));
        Events.fire(new UnreadUpdateEvent());
    }

    public Seq<ChannelDto> getChannels() {
        return channels;
    }

    public void setChannels(Seq<ChannelDto> newChannels) {
        channels.clear();
        channels.addAll(newChannels);
        Events.fire(new ChannelsUpdateEvent());
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
        return unreadCount;
    }

    public int getUnreadByChannel(String channelId) {
        return unreadByChannel.get(channelId, 0);
    }

    public void addUnread(String channelId, int count) {
        unreadByChannel.put(channelId, unreadByChannel.get(channelId, 0) + count);
        unreadCount += count;
        Events.fire(new UnreadUpdateEvent());
    }

    public void resetUnreadCount() {
        unreadCount = 0;
        unreadByChannel.clear();
        Events.fire(new UnreadUpdateEvent());
    }

    public Seq<ChatUser> getUsers(String channelId) {
        return usersByChannel.get(channelId, new Seq<>());
    }

    public void setUsers(String channelId, ChatUser[] users) {
        Arrays.sort(users, (u1, u2) -> {
            int l1 = u1.getHighestRole().map(ChatUser.SimpleRole::getLevel).orElse(-1);
            int l2 = u2.getHighestRole().map(ChatUser.SimpleRole::getLevel).orElse(-1);
            return Integer.compare(l2, l1);
        });
        usersByChannel.put(channelId, new Seq<>(users));
        Events.fire(new UsersUpdateEvent(channelId));
    }

    public boolean isLoadingMessages() {
        return isLoadingMessages.get();
    }

    public void setLoadingMessages(boolean loading) {
        this.isLoadingMessages.set(loading);
        Events.fire(new LoadingMessagesEvent(loading));
    }

    public boolean compareAndSetLoadingMessages(boolean expect, boolean update) {
        boolean success = this.isLoadingMessages.compareAndSet(expect, update);
        if (success) {
            Events.fire(new LoadingMessagesEvent(update));
        }
        return success;
    }

    public static class StoreUpdateEvent {
    }

    public static class CurrentChannelChangeEvent {
        public final String channelId;

        public CurrentChannelChangeEvent(String channelId) {
            this.channelId = channelId;
        }
    }

    public static class ChannelsUpdateEvent {
    }

    public static class MessagesUpdateEvent {
        public final String channelId;
        public final boolean isPrepend;

        public MessagesUpdateEvent(String channelId, boolean isPrepend) {
            this.channelId = channelId;
            this.isPrepend = isPrepend;
        }
    }

    public static class UsersUpdateEvent {
        public final String channelId;

        public UsersUpdateEvent(String channelId) {
            this.channelId = channelId;
        }
    }

    public static class UnreadUpdateEvent {
    }

    public static class LoadingMessagesEvent {
        public final boolean isLoading;

        public LoadingMessagesEvent(boolean isLoading) {
            this.isLoading = isLoading;
        }
    }
}

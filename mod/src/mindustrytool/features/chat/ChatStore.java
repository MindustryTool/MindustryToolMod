package mindustrytool.features.chat;

import arc.util.Nullable;
import mindustrytool.features.chat.state.ChatChannels;
import mindustrytool.features.chat.state.ChatMembers;
import mindustrytool.features.chat.state.ChatMessageDelivery;
import mindustrytool.features.chat.state.ChatMessages;
import mindustrytool.features.chat.state.ChatSession;
import mindustrytool.features.chat.state.ChatTranslations;
import mindustrytool.features.chat.state.ChatUiState;
import mindustrytool.features.chat.state.ChatUnread;
import mindustrytool.features.chat.state.ChatUsers;
import solim.reactive.Signal;

public final class ChatStore {

    private final ChatSession session = new ChatSession();
    private final ChatUsers users = new ChatUsers();
    private final ChatUnread unread = new ChatUnread();
    private final ChatTranslations translations = new ChatTranslations();
    private final ChatMessageDelivery delivery = new ChatMessageDelivery();
    private final ChatUiState ui;

    private final ChatMessages messages;
    private final ChatMembers members;
    private final ChatChannels channels;

    public ChatStore(ChatFeature feature) {
        Signal<String> activeChannelSignal = feature.activeChannelConfig.signal();
        this.channels = new ChatChannels(activeChannelSignal);
        this.messages = new ChatMessages(channels.activeId());
        this.members = new ChatMembers(channels.activeId());
        // Panel collapse signals are backed directly by ConfigValue — single source of truth
        this.ui = new ChatUiState(
                feature.channelsCollapsedConfig.signal(),
                feature.usersCollapsedConfig.signal());
    }

    /** Package-private constructor for unit tests. */
    ChatStore(Signal<String> activeChannelSignal, Signal<Boolean> channelsCollapsed, Signal<Boolean> usersCollapsed) {
        this.channels = new ChatChannels(activeChannelSignal);
        this.messages = new ChatMessages(channels.activeId());
        this.members = new ChatMembers(channels.activeId());
        this.ui = new ChatUiState(channelsCollapsed, usersCollapsed);
    }

    public ChatSession session() {
        return session;
    }

    public ChatChannels channels() {
        return channels;
    }

    public ChatMessages messages() {
        return messages;
    }

    public ChatMembers members() {
        return members;
    }

    public ChatUsers users() {
        return users;
    }

    public ChatUnread unread() {
        return unread;
    }

    public ChatTranslations translations() {
        return translations;
    }

    public ChatMessageDelivery delivery() {
        return delivery;
    }

    public ChatUiState ui() {
        return ui;
    }

    public void selectChannel(@Nullable String channelId) {
        channels.select(channelId);
        ui.clearActiveSelection();
        if (channelId != null) {
            unread.clear(channelId);
        }
    }
}

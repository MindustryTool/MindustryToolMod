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

public final class ChatStore {

    private final ChatSession session = new ChatSession();
    private final ChatChannels channels = new ChatChannels();
    private final ChatMessages messages = new ChatMessages(channels.activeId());
    private final ChatMembers members = new ChatMembers(channels.activeId());
    private final ChatUsers users = new ChatUsers();
    private final ChatUnread unread = new ChatUnread();
    private final ChatTranslations translations = new ChatTranslations();
    private final ChatMessageDelivery delivery = new ChatMessageDelivery();
    private final ChatUiState ui = new ChatUiState();

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

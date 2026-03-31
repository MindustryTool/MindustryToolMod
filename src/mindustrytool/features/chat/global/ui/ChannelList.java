package mindustrytool.features.chat.global.ui;

import arc.graphics.Color;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.TextButton;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.ui.Styles;
import mindustrytool.features.chat.global.ChatConfig;
import mindustrytool.features.chat.global.ChatService;
import mindustrytool.features.chat.global.ChatStore;
import mindustrytool.features.chat.global.dto.ChannelDto;

public class ChannelList extends Table {
    private final Table channelListTable;
    private final ScrollPane scrollPane;
    private Runnable onChannelSelect;

    public ChannelList() {
        channelListTable = new Table();
        channelListTable.top().left();

        scrollPane = new ScrollPane(channelListTable, Styles.noBarPane);
        scrollPane.setScrollingDisabled(true, false);

        add(scrollPane).grow();

        ChatStore store = ChatStore.getInstance();
        store.channelsState.subscribe((n, o) -> rebuild());
        store.currentChannel.subscribe((n, o) -> rebuild());
        store.unreadCountState.subscribe((n, o) -> rebuild());
    }

    public void onChannelSelect(Runnable r) {
        this.onChannelSelect = r;
    }

    public void rebuild() {
        channelListTable.clear();
        channelListTable.top().left();

        ChatStore store = ChatStore.getInstance();
        Seq<ChannelDto> channels = store.getChannels();
        String currentChannelId = store.getCurrentChannelId();
        float scale = ChatConfig.scale();

        for (ChannelDto channel : channels) {
            boolean isSelected = channel.id.equals(currentChannelId);
            int unread = store.getUnreadByChannel(channel.id);
            String unreadString = (unread > 0 ? " (" + (unread > 99 ? "99+" : unread) + ")" : "");

            boolean hasNewUnreadMessage = unread == 0 && channel.lastMessageId != null
                    && !channel.lastMessageId.equals(store.getLastReadMessageId(channel.id));

            TextButton btn = new TextButton("# " + channel.name + unreadString,
                    isSelected ? Styles.togglet : Styles.cleart);

            btn.getLabel().setAlignment(Align.left);
            btn.getLabel().setFontScale(scale);
            btn.getLabel().setEllipsis(true);
            if (btn.getLabelCell() != null) {
                btn.getLabelCell().growX().minWidth(0).left();
            }

            if (hasNewUnreadMessage) {
                Label indicatorLabel = new Label("[white]\u25CF[white]");
                indicatorLabel.setFontScale(scale);
                indicatorLabel.setAlignment(Align.right);
                btn.add(indicatorLabel).right().padLeft(4 * scale);
            }

            if (isSelected) {
                btn.setChecked(true);
            }

            if (isSelected) {
                btn.getLabel().setColor(Color.white);
            } else {
                btn.getLabel().setColor(Color.lightGray);
            }

            btn.clicked(() -> {
                if (!isSelected) {
                    store.setCurrentChannelId(channel.id);
                    ChatService.getInstance().fetchMessages(channel.id, null);
                    ChatService.getInstance().fetchChatUsers(channel.id);
                }
                if (channel.lastMessageId != null) {
                    store.setLastReadMessageId(channel.id, channel.lastMessageId);
                }
                if (onChannelSelect != null) {
                    onChannelSelect.run();
                }
            });

            channelListTable.add(btn).growX().minWidth(0).height(40 * scale).pad(2 * scale).row();
        }
    }
}

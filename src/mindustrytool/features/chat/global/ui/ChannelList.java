package mindustrytool.features.chat.global.ui;

import arc.Events;
import arc.graphics.Color;
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

        Events.on(ChatStore.ChannelsUpdateEvent.class, e -> rebuild());
        Events.on(ChatStore.CurrentChannelChangeEvent.class, e -> rebuild());
        Events.on(ChatStore.UnreadUpdateEvent.class, e -> rebuild());
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
            TextButton btn = new TextButton("# " + channel.name, isSelected ? Styles.togglet : Styles.cleart);
            btn.getLabel().setAlignment(Align.left);
            btn.getLabel().setFontScale(scale);
            btn.getLabel().setEllipsis(true);
            if (btn.getLabelCell() != null) btn.getLabelCell().minWidth(0);

            if (isSelected) {
                btn.setChecked(true);
            }

            int unread = store.getUnreadByChannel(channel.id);
            if (unread > 0 && !isSelected) {
                btn.getLabel().setColor(Color.white);
                btn.setText("# " + channel.name + " (" + (unread > 99 ? "99+" : unread) + ")");
            } else {
                btn.getLabel().setColor(Color.lightGray);
            }

            btn.clicked(() -> {
                if (!isSelected) {
                    store.setCurrentChannelId(channel.id);
                    ChatService.getInstance().fetchMessages(channel.id, null);
                    ChatService.getInstance().fetchChatUsers(channel.id);
                }
                if (onChannelSelect != null) {
                    onChannelSelect.run();
                }
            });

            channelListTable.add(btn).growX().minWidth(0).height(40 * scale).pad(2 * scale).row();
        }
    }
}

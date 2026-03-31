package mindustrytool.features.chat.global.ui;

import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import arc.util.Log;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.chat.global.ChatConfig;
import mindustrytool.features.chat.global.ChatService;
import mindustrytool.features.chat.global.ChatStore;
import mindustrytool.features.chat.global.dto.ChatUser;
import mindustrytool.ui.NetworkImage;
import arc.Events;
import mindustrytool.features.chat.global.events.UsersUpdateEvent;

public class UserList extends Table {
    private final Table userListTable;
    private final ScrollPane scrollPane;
    private final Label countLabel;

    public UserList() {
        top().left();

        countLabel = new Label("");
        countLabel.setColor(Color.gray);
        add(countLabel).padBottom(8).padLeft(8).left().row();

        userListTable = new Table();
        userListTable.top().left();

        scrollPane = new ScrollPane(userListTable, Styles.noBarPane);
        scrollPane.setScrollingDisabled(true, false);

        add(scrollPane).grow();

        Events.on(UsersUpdateEvent.class, e -> {
            if (e != null && e.channelId.equals(ChatStore.getInstance().getCurrentChannelId())) {
                rebuild();
            }
        });

        ChatStore store = ChatStore.getInstance();
        store.currentChannel.subscribe((e, o) -> {
            rebuild();
            updateUserCount();
        });

        updateUserCount();
    }

    private void updateUserCount() {
        String currentChannelId = ChatStore.getInstance().getCurrentChannelId();
        if (currentChannelId != null) {
            ChatService.getInstance().getChatUserCount(currentChannelId, count -> {
                countLabel.setText(String.valueOf(count));
            }, e -> {
                countLabel.setText("?");
                Log.err(e);
            });
        }
    }

    public void rebuild() {
        userListTable.clear();
        userListTable.top().left();

        String currentChannelId = ChatStore.getInstance().getCurrentChannelId();
        if (currentChannelId == null) {
            return;
        }

        float scale = ChatConfig.scale();
        countLabel.setFontScale(scale * 0.8f);

        Seq<ChatUser> users = ChatStore.getInstance().getUsers(currentChannelId);

        for (ChatUser user : users) {
            Table card = new Table();

            if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                card.add(new NetworkImage(user.getImageUrl())).size(48 * scale).padRight(8 * scale);
            } else {
                card.add(new Image(Icon.players)).size(48 * scale).padRight(8 * scale);
            }

            card.table(info -> {
                info.left().top();
                Label l = info.add(user.getName() + "[white]")
                        .minWidth(0)
                        .ellipsis(true)
                        .style(Styles.defaultLabel)
                        .color(Color.white)
                        .growX()
                        .left().get();
                l.setFontScale(scale);
                info.row();

                user.getHighestRole().ifPresent(role -> {
                    Label l2 = info.add(role.getId()).minWidth(0).ellipsis(true).style(Styles.defaultLabel)
                            .color(Color.valueOf(role.getColor()))
                            .labelAlign(Align.left)
                            .left()
                            .growX()
                            .get();
                    l2.setFontScale(scale);
                    info.row();
                });
                String state = user.getState();
                if (state != null && !state.equals("menu")) {
                    info.add("[gray]" + state + "[white]").growX().ellipsis(true).fontScale(0.8f);
                    info.row();
                }
            }).growX().left();

            userListTable.add(card)
                    .growX()
                    .minWidth(0)
                    .padBottom(8 * scale)
                    .padLeft(8 * scale)
                    .padRight(8 * scale)
                    .row();
        }
        userListTable.pack();
    }
}

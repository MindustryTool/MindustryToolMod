package mindustrytool.features.chat.global.ui;

import arc.Events;
import arc.graphics.Color;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.ScrollPane;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.chat.global.ChatConfig;
import mindustrytool.features.chat.global.ChatStore;
import mindustrytool.features.chat.global.events.*;
import mindustrytool.features.chat.global.dto.ChatUser;
import mindustrytool.ui.NetworkImage;

public class UserList extends Table {
    private final Table userListTable;
    private final ScrollPane scrollPane;

    public UserList() {
        userListTable = new Table();
        userListTable.top().left();

        scrollPane = new ScrollPane(userListTable, Styles.noBarPane);
        scrollPane.setScrollingDisabled(true, false);

        add(scrollPane).grow();

        Events.on(UsersUpdateEvent.class, e -> {
            if (e.channelId.equals(ChatStore.getInstance().getCurrentChannelId())) {
                rebuild();
            }
        });
        Events.on(CurrentChannelChangeEvent.class, e -> rebuild());
    }

    public void rebuild() {
        userListTable.clear();
        userListTable.top().left();

        String currentChannelId = ChatStore.getInstance().getCurrentChannelId();
        if (currentChannelId == null) return;

        Seq<ChatUser> users = ChatStore.getInstance().getUsers(currentChannelId);
        float scale = ChatConfig.scale();

        for (ChatUser user : users) {
            Table card = new Table();

            if (user.getImageUrl() != null && !user.getImageUrl().isEmpty()) {
                card.add(new NetworkImage(user.getImageUrl())).size(40 * scale).padRight(8 * scale);
            } else {
                card.add(new Image(Icon.players)).size(40 * scale).padRight(8 * scale);
            }

            card.table(info -> {
                info.left();
                Label l = info.add(user.getName() + "[white]").minWidth(0).ellipsis(true).style(Styles.defaultLabel)
                        .color(Color.white)
                        .left().get();
                l.setFontScale(scale);
                info.row();

                user.getHighestRole().ifPresent(role -> {
                    Label l2 = info.add(role.getId()).minWidth(0).ellipsis(true).style(Styles.defaultLabel)
                            .color(Color.valueOf(role.getColor()))
                            .labelAlign(Align.left)
                            .left()
                            .get();
                    l2.setFontScale(scale);
                });
            }).growX().left();

            userListTable.add(card).growX().minWidth(0).padBottom(8 * scale).padLeft(8 * scale).padRight(8 * scale).row();
        }
        userListTable.pack();
    }
}

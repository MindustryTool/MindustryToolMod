package mindustrytool.features.chat;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import mindustrytool.models.response.ChatUser;
import solim.core.BaseComponent;
import solim.signal.Readable;

public class ChatUserListView extends BaseComponent {

    private final ChatStore store;

    public ChatUserListView(ChatStore store) {
        this.store = store;
    }

    @Override
    protected Element build() {
        Readable<Boolean> hasUsers = store.members().active()
                .map(list -> list != null && !list.isEmpty());

        return column().grow().top().left().gap(unit(1)).children(() -> {
            scroll().grow().left().children(() -> {
                column().growX().top().left().gap(unit(1)).children(() -> {
                    dynamic(hasUsers, available -> {
                        if (Boolean.TRUE.equals(available)) {
                            return forEach(store.members().active(), ChatUser::getName,
                                    UserItem::new);
                        } else {
                            return column()
                                    .padding(unit(2))
                                    .top().left()
                                    .children(() -> {
                                        text(Core.bundle.get("feature.chat.ui.empty-members",
                                                "No members online."))
                                                        .color(Color.gray)
                                                        .fontScale(0.9f)
                                                        .left();
                                    });
                        }
                    });
                });
            });
        })
                .element();
    }

    private static class UserItem extends BaseComponent {
        private final ChatUser user;

        public UserItem(ChatUser user) {
            this.user = user;
        }

        @Override
        protected Element build() {
            String name = user.getName() != null ? user.getName() : "Anonymous";
            Color roleColor = Color.white;
            if (user.getHighestRole()
                    .isPresent()) {
                try {
                    String colorHex = user.getHighestRole()
                            .get()
                            .getColor();
                    if (colorHex != null && !colorHex.isEmpty()) {
                        roleColor = Color.valueOf(colorHex);
                    }
                } catch (Exception ignored) {
                }
            }

            final Color finalRoleColor = roleColor;

            return card().growX().top().left()
                    .children(() -> {
                        row().growX().top().left().padding(unit(1)).gap(unit(1)).children(() -> {
                            new ChatAvatar(name, user.getImageUrl(), name, unit(10));

                            text(name).color(finalRoleColor)
                                    .growX()
                                    .ellipsis()
                                    .left();
                        });
                    })
                    .element();
        }
    }
}

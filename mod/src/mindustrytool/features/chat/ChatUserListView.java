package mindustrytool.features.chat;

import static solim.UI.*;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustrytool.components.Loader;
import mindustrytool.components.WebStyles;
import mindustrytool.models.response.ChatUser;
import solim.core.Component;
import solim.reactive.Readable;

public class ChatUserListView extends Component {

    private final ChatStore store;
    private final @Nullable ChatService service;

    public ChatUserListView(ChatStore store) {
        this(store, null);
    }

    public ChatUserListView(ChatStore store, @Nullable ChatService service) {
        this.store = store;
        this.service = service;
    }

    @Override
    protected Element build() {
        Readable<Boolean> hasChannel = store.channels().activeId().map(id -> id != null && !id.isEmpty());

        return column().name("user-list").props(grow(), padding(unit(2)), top(), left(), gap(unit(1))).children(() -> {
            when(hasChannel)
                    .elseDo(() -> {
                    dynamic(store.channels().loading(), chanLoading -> {
                        if (Boolean.TRUE.equals(chanLoading)) {
                            Loader.centered();
                        } else {
                            dynamic(store.channels().error(), chanErr -> {
                                if (chanErr != null && !chanErr.trim().isEmpty()) {
                                    column().props(grow(), padding(unit(2)), center(), gap(unit(2))).children(() -> {
                                        icon(Icon.warning).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))).color(Color.scarlet);
                                        text(Core.bundle.get("feature.chat.ui.channels-failed", "Channels failed to load."))
                                                .color(Color.scarlet)
                                                .fontScale(0.95f)
                                                .wrap()
                                                .center();
                                        text(chanErr).color(Color.gray).fontScale(0.8f).wrap().center();
                                        button(Core.bundle.get("feature.chat.ui.retry-channels", "Retry Channels"), () -> {
                                            if (service != null) {
                                                service.refreshChannels();
                                            }
                                        })
                                                .style(WebStyles.secondary())
                                                .props(height(unit(9)), prefHeight(unit(9)))
                                                .children(() -> {
                                                    icon(Icon.refresh).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                                                    text(Core.bundle.get("feature.chat.ui.retry-channels",
                                                            "Retry Channels"));
                                                });
                                    });
                                } else {
                                    column().props(padding(unit(2))).children(() -> {
                                        text(Core.bundle.get("feature.chat.ui.empty-channels", "No channels available."))
                                                .color(Color.gray)
                                                .fontScale(0.9f);
                                    });
                                }
                            }).props(grow());
                        }
                    }).props(grow());
                 })
                .thenDo(() -> query(store.members().query())
                        .props(grow())
                        .loading(Loader::centered)
                        .error(err -> column().props(grow(), padding(unit(2)), center(), gap(unit(2))).children(() -> {
                            icon(Icon.warning).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))).color(Color.scarlet);
                            text(Core.bundle.get("feature.chat.ui.error.members", "Failed to load members."))
                                    .color(Color.scarlet)
                                    .fontScale(0.95f)
                                    .wrap()
                                    .center();
                            text(err != null ? err.getMessage() : "")
                                    .color(Color.gray)
                                    .fontScale(0.8f)
                                    .wrap()
                                    .center();
                            button(Core.bundle.get("feature.chat.ui.retry", "Retry"),
                                    () -> store.members().query().refetch())
                                            .style(WebStyles.secondary())
                                            .props(height(unit(9)), prefHeight(unit(9)))
                                            .children(() -> {
                                                icon(Icon.refresh).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                                                text(Core.bundle.get("feature.chat.ui.retry", "Retry"));
                                            });
                        }))
                        .data(users -> {
                            if (users != null && !users.isEmpty()) {
                                return scroll().props(grow(), left()).children(() -> {
                                    column().props(growX(), top(), left(), gap(unit(1))).children(() -> {
                                        forEach(store.members().active()).key(ChatUser::getName)
                                                .items(UserItem::new);
                                    });
                                });
                            }

                            return column()
                                    .props(padding(unit(2)), top(), left())
                                    .children(() -> {
                                        text(Core.bundle.get("feature.chat.ui.empty-members",
                                                "No members online."))
                                                        .color(Color.gray)
                                                        .fontScale(0.9f)
                                                        .left();
                                    });
                        }))
                    .props(grow());
        }).element();
    }

    private static class UserItem extends Component {
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

            return card().props(growX(), top(), left())
                    .children(() -> {
                        row().props(growX(), padding(unit(1)), top(), left(), gap(unit(1))).children(() -> {
                            new ChatAvatar(name, user.getImageUrl(), name, unit(10));
                            column().children(() -> {
                                text(name).color(finalRoleColor)
                                        .growX()
                                        .ellipsis()
                                        .left();

                                if (user.getState() != null && !user.getState().isEmpty()) {
                                    text(user.getState()).color(Color.lightGray).fontScale(0.8f);
                                }
                            });
                        });
                    })
                    .element();
        }
    }
}

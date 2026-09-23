package mindustrytool.features.chat;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustrytool.components.Loader;
import mindustrytool.components.WebStyles;
import mindustrytool.models.response.ChatUser;
import solim.core.BaseComponent;
import solim.reactive.Readable;

public class ChatUserListView extends BaseComponent {

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

        return column().name("user-list").grow().top().left().gap(unit(1)).padding(unit(2)).children(() -> {
            when(hasChannel)
                    .elseDo(() -> {
                    dynamic(store.channels().loading(), chanLoading -> {
                        if (Boolean.TRUE.equals(chanLoading)) {
                            Loader.centered();
                        } else {
                            dynamic(store.channels().error(), chanErr -> {
                                if (chanErr != null && !chanErr.trim().isEmpty()) {
                                    column().grow().center().gap(unit(2)).padding(unit(2)).children(() -> {
                                        icon(Icon.warning).size(unit(6)).color(Color.scarlet);
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
                                                .height(unit(9))
                                                .children(() -> {
                                                    icon(Icon.refresh).size(unit(4));
                                                    text(Core.bundle.get("feature.chat.ui.retry-channels",
                                                            "Retry Channels"));
                                                });
                                    });
                                } else {
                                    column().padding(unit(2)).children(() -> {
                                        text(Core.bundle.get("feature.chat.ui.empty-channels", "No channels available."))
                                                .color(Color.gray)
                                                .fontScale(0.9f);
                                    });
                                }
                            }).grow();
                        }
                    }).grow();
                 })
                .thenDo(() -> query(store.members().query())
                        .grow()
                        .loading(Loader::centered)
                        .error(err -> column().grow().center().gap(unit(2)).padding(unit(2)).children(() -> {
                            icon(Icon.warning).size(unit(6)).color(Color.scarlet);
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
                                            .height(unit(9))
                                            .children(() -> {
                                                icon(Icon.refresh).size(unit(4));
                                                text(Core.bundle.get("feature.chat.ui.retry", "Retry"));
                                            });
                        }))
                        .data(users -> {
                            if (users != null && !users.isEmpty()) {
                                return scroll().grow().left().children(() -> {
                                    column().growX().top().left().gap(unit(1)).children(() -> {
                                        forEach(store.members().active()).key(ChatUser::getName)
                                                .children(UserItem::new);
                                    });
                                });
                            }

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
                        }))
                    .grow();
        }).element();
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


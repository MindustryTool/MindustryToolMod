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
import solim.reactive.Computed;
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
        Readable<Boolean> hasUsers = store.members().active()
                .map(list -> list != null && !list.isEmpty());
        Readable<Boolean> showRefreshErrorBanner = new Computed<>(() -> {
            String err = store.members().activeError().get();
            Boolean has = hasUsers.get();
            return err != null && !err.trim().isEmpty() && Boolean.TRUE.equals(has);
        });

        return column().grow().top().left().gap(unit(1)).padding(unit(2)).children(() -> {
            dynamic(hasChannel, channelSelected -> {
                if (!Boolean.TRUE.equals(channelSelected)) {
                    return dynamic(store.channels().loading(), chanLoading -> {
                        if (Boolean.TRUE.equals(chanLoading)) {
                            return Loader.centered();
                        }

                        return dynamic(store.channels().error(), chanErr -> {
                            if (chanErr != null && !chanErr.trim().isEmpty()) {
                                return column().grow().center().gap(unit(2)).padding(unit(2)).children(() -> {
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
                                                text(Core.bundle.get("feature.chat.ui.retry-channels", "Retry Channels"));
                                            });
                                });
                            }

                            return column().padding(unit(2)).children(() -> {
                                text(Core.bundle.get("feature.chat.ui.empty-channels", "No channels available."))
                                        .color(Color.gray)
                                        .fontScale(0.9f);
                            });
                        }).grow();
                    }).grow();
                }

                return column().grow().top().left().gap(unit(1)).children(() -> {
                    dynamic(showRefreshErrorBanner, show -> {
                        if (Boolean.TRUE.equals(show)) {
                            return card().growX().padding(unit(1.5f)).children(() -> {
                                row().growX().gap(unit(1)).center().children(() -> {
                                    icon(Icon.warning).size(unit(4)).color(Color.scarlet);
                                    text(Core.bundle.get("feature.chat.ui.error.members", "Failed to load members."))
                                            .color(Color.scarlet)
                                            .fontScale(0.85f)
                                            .growX()
                                            .left();
                                    button(Core.bundle.get("feature.chat.ui.retry", "Retry"), () -> {
                                        String activeId = store.channels().currentActiveId();
                                        if (activeId != null && service != null) {
                                            service.loadUsers(activeId);
                                        }
                                    })
                                            .style(WebStyles.secondary())
                                            .height(unit(8))
                                            .children(() -> {
                                                icon(Icon.refresh).size(unit(4));
                                                text(Core.bundle.get("feature.chat.ui.retry", "Retry"));
                                            });
                                });
                            });
                        }
                        return null;
                    });

                    dynamic(hasUsers, available -> {
                        if (Boolean.TRUE.equals(available)) {
                            return scroll().grow().left().children(() -> {
                                column().growX().top().left().gap(unit(1)).children(() -> {
                                    forEach(store.members().active(), ChatUser::getName, UserItem::new);
                                });
                            });
                        }

                        return dynamic(store.members().activeLoading(), isLoading -> {
                            if (Boolean.TRUE.equals(isLoading)) {
                                return Loader.centered();
                            }

                            return dynamic(store.members().activeError(), err -> {
                                if (err != null && !err.trim().isEmpty()) {
                                    return column().grow().center().gap(unit(2)).padding(unit(2)).children(() -> {
                                        icon(Icon.warning).size(unit(6)).color(Color.scarlet);
                                        text(Core.bundle.get("feature.chat.ui.error.members", "Failed to load members."))
                                                .color(Color.scarlet)
                                                .fontScale(0.95f)
                                                .wrap()
                                                .center();
                                        text(err).color(Color.gray).fontScale(0.8f).wrap().center();
                                        button(Core.bundle.get("feature.chat.ui.retry", "Retry"), () -> {
                                            String activeId = store.channels().currentActiveId();
                                            if (activeId != null && service != null) {
                                                service.loadUsers(activeId);
                                            }
                                        })
                                                .style(WebStyles.secondary())
                                                .height(unit(9))
                                                .children(() -> {
                                                    icon(Icon.refresh).size(unit(4));
                                                    text(Core.bundle.get("feature.chat.ui.retry", "Retry"));
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
                            }).grow();
                        }).grow();
                    }).grow();
                }).grow();
            }).grow();
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

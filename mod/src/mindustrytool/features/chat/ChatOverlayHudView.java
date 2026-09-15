package mindustrytool.features.chat;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import mindustrytool.components.FileIcon;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Direction;
import solim.overlay.Hud;
import solim.signal.Readable;
import solim.signal.Signal;
import solim.ui.Units;

public class ChatOverlayHudView extends BaseComponent {

    private final ChatFeature feature;
    private final ChatStore store;
    private final ChatService service;
    private final Signal<Integer> mobileTab = Signal.of(1); // 0: Channels, 1: Messages, 2: Members

    private Hud hud;

    public ChatOverlayHudView(ChatFeature feature) {
        this.feature = feature;
        this.store = feature.getStore();
        this.service = feature.getService();
    }

    @Override
    protected Element build() {
        Readable<Boolean> isCollapsed = feature.collapsedConfig.signal();

        hud = hud(() -> {
            dynamic(isCollapsed, collapsed -> {
                if (Boolean.TRUE.equals(collapsed)) {
                    return buildCollapsedBadge();
                } else {
                    return buildExpandedWindow();
                }
            });
        });

        hud.opacity(feature.opacityConfig.signal());
        hud.position(feature.xSignal, feature.ySignal);
        hud.toFrontOnTouch();

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });

        effect(() -> {
            isCollapsed.get();
        });

        Core.app.post(() -> {
            if (hud != null) {
                keepInScreen();
                hud.root().toFront();
            }
        });

        return hud.element();
    }

    private Component buildCollapsedBadge() {
        Readable<Boolean> hasUnread = store.unread().total().map(count -> count != null && count > 0);

        return card()
                .rounded(10, new Color(0f, 0f, 0f, 0.6f))
                .children(() -> {
                    button(() -> {
                        feature.collapsedConfig.set(false);
                        String activeId = store.channels().currentActiveId();
                        if (activeId != null) {
                            store.unread().markAsRead(activeId);
                        }
                    })
                            .size(unit(12))
                            .draggable(hud, feature.xSignal, feature.ySignal)
                            .children(() -> {
                                stack()
                                        .grow()
                                        .center()
                                        .layer(() -> icon(FileIcon.of("message-circle.png")).size(unit(8)).center()
                                                .grow().color(Color.white))
                                        .layer(() -> row().top().right().grow().visible(hasUnread).children(() -> {
                                            image(circle())
                                                    .cellPadding(unit(1))
                                                    .size(unit(3))
                                                    .color(Color.scarlet);
                                        }));
                            });
                });
    }

    private Component buildExpandedWindow() {
        Readable<Float> winWidth = feature.widthRatioConfig.signal().map(r -> {
            float sw = Units.width().get();
            float maxW = sw * 0.95f;
            float ratio = r != null ? r : 0.6f;
            float target = sw * Math.max(0.3f, Math.min(0.95f, ratio));
            float minW = Math.min(320f, maxW);
            return Math.max(minW, Math.min(maxW, target));
        });
        Readable<Float> winHeight = feature.heightRatioConfig.signal().map(r -> {
            float sh = Units.height().get();
            float maxH = sh * 0.95f;
            float ratio = r != null ? r : 0.6f;
            float target = sh * Math.max(0.3f, Math.min(0.95f, ratio));
            float minH = Math.min(240f, maxH);
            return Math.max(minH, Math.min(maxH, target));
        });

        Readable<Boolean> isConnected = store.session().connected();
        Readable<Boolean> isDesktop = Units.width().map(w -> w != null && w >= 1200f);
        Readable<String> channelTitle = store.channels().active().map(c -> {
            if (c != null && c.getName() != null && !c.getName().trim().isEmpty()) {
                return "# " + c.getName().trim();
            }
            return "";
        });

        Readable<Boolean> channelsCol = store.ui().channelsCollapsed();
        Readable<Drawable> channelsIcon = channelsCol.map(c -> Boolean.TRUE.equals(c)
                ? FileIcon.of("panel-left-open.png", Icon.rightOpen)
                : FileIcon.of("panel-left.png", Icon.leftOpen));
        Readable<String> channelsTooltip = channelsCol.map(c -> Boolean.TRUE.equals(c)
                ? Core.bundle.get("feature.chat.ui.expand-channels", "Show Channels")
                : Core.bundle.get("feature.chat.ui.collapse-channels", "Hide Channels"));

        Readable<Boolean> usersCol = store.ui().usersCollapsed();
        Readable<Drawable> usersIcon = usersCol.map(c -> Boolean.TRUE.equals(c)
                ? FileIcon.of("panel-right-open.png", Icon.leftOpen)
                : FileIcon.of("panel-right.png", Icon.rightOpen));
        Readable<String> usersTooltip = usersCol.map(c -> Boolean.TRUE.equals(c)
                ? Core.bundle.get("feature.chat.ui.expand-users", "Show Members")
                : Core.bundle.get("feature.chat.ui.collapse-users", "Hide Members"));

        // Shared floating message-action popup (zero-footprint overlay driver).
        ChatActionPopup.install(store);

        return card(Styles.black8)
                .width(winWidth)
                .height(winHeight)
                .maxWidth(Units.dvw(95f))
                .maxHeight(Units.dvh(95f))
                .children(() -> {
                    column().grow().children(() -> {
                        // Window Action Bar (draggable bar wrapping title & action buttons)
                        row().growX()
                                .background(colored(new Color(0.45f, 0.35f, 0.9f, 0.3f), Tex.whiteui))
                                .padding(unit(1), unit(2), unit(1), unit(2))
                                .gap(unit(2))
                                .draggable(hud, feature.xSignal, feature.ySignal)
                                .center()
                                .children(() -> {
                                    image(circle())
                                            .size(unit(3))
                                            .color(isConnected.map(c -> c ? Pal.heal : Color.scarlet));

                                    text(channelTitle)
                                            .color(Pal.accent)
                                            .fontScale(0.95f)
                                            .left();

                                    spacer();

                                    button(store.ui()::toggleChannelsCollapsed)
                                            .style(WebStyles.ghost())
                                            .size(unit(10), unit(10))
                                            .visible(isDesktop)
                                            .tooltip(channelsTooltip)
                                            .children(() -> icon(channelsIcon).size(unit(5)));

                                    button(store.ui()::toggleUsersCollapsed)
                                            .style(WebStyles.ghost())
                                            .size(unit(10), unit(10))
                                            .visible(isDesktop)
                                            .tooltip(usersTooltip)
                                            .children(() -> icon(usersIcon).size(unit(5)));

                                    button(() -> {
                                        var dialog = feature.getSettingDialog();
                                        if (dialog != null) {
                                            dialog.show();
                                        }
                                    })
                                            .style(WebStyles.ghost())
                                            .size(unit(10), unit(10))
                                            .tooltip(Core.bundle.get("feature.chat.ui.settings", "Settings"))
                                            .children(() -> icon(Icon.settings).size(unit(5)));

                                    button(() -> feature.collapsedConfig.set(true))
                                            .style(WebStyles.ghost())
                                            .size(unit(10), unit(10))
                                            .tooltip(Core.bundle.get("feature.chat.ui.collapse", "Collapse"))
                                            .children(() -> icon(Icon.cancel).size(unit(5)));
                                });

                        divider();

                        dynamic(Units.width(), width -> {
                            if (width < 1200) {
                                return buildMobileBody();
                            } else {
                                return buildDesktopBody();
                            }
                        })
                                .grow();
                    });
                });
    }

    private Component buildDesktopBody() {
        return row().grow().children(() -> {
            // Channel List
            dynamic(store.ui().channelsCollapsed(), collapsed -> {
                if (Boolean.TRUE.equals(collapsed)) {
                    return null;
                }
                return row().growY().children(() -> {
                    row().width(unit(80)).growY().children(() -> {
                        new ChatChannelListView(store);
                    });
                    divider(Direction.Y);
                });
            }).growY();

            // Message Area & Input
            column().grow().children(() -> {
                new ChatMessageListView(store, service);
                divider();
                new ChatInputView(store, service);
            });

            // User List
            dynamic(store.ui().usersCollapsed(), collapsed -> {
                if (Boolean.TRUE.equals(collapsed)) {
                    return null;
                }
                return row().growY().children(() -> {
                    divider(Direction.Y);
                    row().width(unit(80)).growY().children(() -> {
                        new ChatUserListView(store);
                    });
                });
            }).growY();
        });
    }

    private Component buildMobileBody() {
        return tabs(mobileTab)
                .grow()
                .tab(Core.bundle.get("feature.chat.ui.channels", "Channels"), () -> {
                    new ChatChannelListView(store);
                })
                .tab(Core.bundle.get("feature.chat.ui.messages", "Messages"), () -> {
                    column().grow().gap(unit(1)).children(() -> {
                        new ChatMessageListView(store, service);
                        divider();
                        new ChatInputView(store, service);
                    });
                })
                .tab(Core.bundle.get("feature.chat.ui.members", "Members"), () -> {
                    new ChatUserListView(store);
                });
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.root().invalidateHierarchy();
            hud.pack();
            hud.keepInScreen();
        }
    }
}

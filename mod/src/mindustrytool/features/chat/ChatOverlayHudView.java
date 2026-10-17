package mindustrytool.features.chat;

import static solim.UI.*;
import static solim.modifier.Values.background;
import static solim.modifier.Values.draggable;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.growY;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.position;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.rounded;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;
import static solim.modifier.Values.visible;
import static solim.modifier.Values.width;

import arc.Core;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import java.util.Objects;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import mindustrytool.components.FileIcon;
import solim.core.Component;
import solim.layout.Direction;
import solim.overlay.Hud;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.core.Units;

public class ChatOverlayHudView extends Component {

    private final ChatFeature feature;
    private final ChatStore store;
    private final ChatService service;

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
            when(isCollapsed)
                    .thenDo(this::buildCollapsedBadge)
                    .elseDo(this::buildExpandedWindow);
        });

        hud.opacity(feature.opacityConfig.signal());
        hud.props(position(feature.xSignal, feature.ySignal));
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
                .props(rounded(10, WebStyles.Colors.SECONDARY_BG))
                .children(() -> {
                    button(() -> {
                        feature.collapsedConfig.set(false);
                        String activeId = store.channels().currentActiveId();
                        if (activeId != null) {
                            store.unread().markAsRead(activeId);
                        }
                    })
                            .props(size(unit(12)), prefWidth(unit(12)), prefHeight(unit(12)), draggable(hud, feature.xSignal, feature.ySignal))
                            .children(() -> {
                                stack()
                                        .grow()
                                        .center()
                                        .layer(() -> row().props(padding(unit(2)))
                                                .children(() -> icon(FileIcon.of("message-circle.png"))
                                                        .props(size(unit(8)), prefWidth(unit(8)), prefHeight(unit(8)))
                                                        .center()
                                                        .props(grow())
                                                        .color(Color.white)))
                                        .layer(() -> row().props(top(), right(), grow(), visible(hasUnread)).children(() -> {
                                            image(circle())
                                                    .margin(unit(1))
                                                    .props(size(unit(3)), prefWidth(unit(3)), prefHeight(unit(3)))
                                                    .color(Color.scarlet);
                                        }));
                            });
                });
    }

    private Component buildExpandedWindow() {
        Readable<Float> winWidth = feature.widthRatioConfig.signal().map(r -> {
            float sw = Units.width();
            float maxW = sw * 0.90f;
            float ratio = r != null ? r : 0.6f;
            float target = sw * Math.max(0.3f, Math.min(0.95f, ratio));
            float minW = Math.min(320f, maxW);
            return Math.max(minW, Math.min(maxW, target));
        });
        Readable<Float> winHeight = feature.heightRatioConfig.signal().map(r -> {
            float sh = Units.height();
            float maxH = sh * 0.90f;
            float ratio = r != null ? r : 0.6f;
            float target = sh * Math.max(0.3f, Math.min(0.95f, ratio));
            float minH = Math.min(240f, maxH);
            return Math.max(minH, Math.min(maxH, target));
        });

        Readable<Boolean> isConnected = store.session().connected();
        Readable<Boolean> isDesktop = Units.dvw.map(w -> w >= 1200f);
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

        ChatActionPopup.install(store);

        return card(Styles.black9)
                .children(() -> {
                    column().props(grow()).children(() -> {

                        row().props(growX(), background(colored(new Color(0.45f, 0.35f, 0.9f, 0.3f), Tex.whiteui)), padding(unit(1), unit(2), unit(1), unit(2)), gap(unit(2)), draggable(hud, feature.xSignal, feature.ySignal), center())
                                .children(() -> {
                                    image(circle())
                                            .props(size(unit(3)), prefWidth(unit(3)), prefHeight(unit(3)))
                                            .color(isConnected.map(c -> c ? Pal.heal : Color.scarlet));

                                    text(channelTitle)
                                            .color(Pal.accent)
                                            .fontScale(0.95f)
                                            .left();

                                    spacer();

                                    button(store.ui()::toggleChannelsCollapsed)
                                            .style(WebStyles.ghost())
                                            .props(size(unit(10)), prefWidth(unit(10)), prefHeight(unit(10)))
                                            .visible(isDesktop)
                                            .props(tooltip(channelsTooltip))
                                            .children(() -> icon(channelsIcon).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));

                                    button(store.ui()::toggleUsersCollapsed)
                                            .style(WebStyles.ghost())
                                            .props(size(unit(10)), prefWidth(unit(10)), prefHeight(unit(10)))
                                            .visible(isDesktop)
                                            .props(tooltip(usersTooltip))
                                            .children(() -> icon(usersIcon).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6))));

                                    button(() -> service.refresh(store.channels().activeId().peek()))
                                            .style(WebStyles.ghost())
                                            .props(size(unit(10)), prefWidth(unit(10)), prefHeight(unit(10)))
                                            .children(() -> icon(Icon.refresh).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));

                                    button(() -> {
                                        Prov<SolimDialog> dialog = feature.getSettingDialog();
                                        if (dialog != null) {
                                            dialog.get().show();
                                        }
                                    })
                                            .style(WebStyles.ghost())
                                            .props(size(unit(10)), prefWidth(unit(10)), prefHeight(unit(10)), tooltip(Core.bundle.get("feature.chat.ui.settings", "Settings")))
                                            .children(() -> icon(Icon.settings).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));

                                    button(() -> feature.collapsedConfig.set(true))
                                            .style(WebStyles.ghost())
                                            .props(size(unit(10)), prefWidth(unit(10)), prefHeight(unit(10)), tooltip(Core.bundle.get("feature.chat.ui.collapse", "Collapse")))
                                            .children(() -> icon(Icon.cancel).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))));
                                });

                        divider();

                        dynamic(Units.dvw, width -> {
                            if (width < 1200) {
                                buildMobileBody();
                            } else {
                                buildDesktopBody();
                            }
                        })
                                .props(width(winWidth), prefWidth(winWidth), height(winHeight), prefHeight(winHeight), grow());
                    });
                });
    }

    private Component buildDesktopBody() {
        return row().props(grow()).children(() -> {

            when(store.ui().channelsCollapsed())
                    .elseDo(() -> row().props(growY()).children(() -> {
                        row().props(width(unit(80)), prefWidth(unit(80)), growY()).children(() -> {
                            new ChatChannelListView(store, service);
                        });
                        divider(Direction.Y);
                    }))
                    .props(growY());

            column().props(grow()).children(() -> {
                new ChatMessageListView(store, service);
                divider();
                new ChatInputView(store, service);
            });

            when(store.ui().usersCollapsed())
                    .elseDo(() -> row().props(growY()).children(() -> {
                        divider(Direction.Y);
                        row().props(width(unit(80)), prefWidth(unit(80)), growY()).children(() -> {
                            new ChatUserListView(store, service);
                        });
                    }))
                    .props(growY());
        });
    }

    private Component buildMobileBody() {
        return tabs(store.ui().mobileTab())
                .props(grow())
                .tab(Core.bundle.get("feature.chat.ui.channels", "Channels"), () -> {
                    new ChatChannelListView(store, service);
                })
                .tab(Core.bundle.get("feature.chat.ui.messages", "Messages"), () -> {
                    column().props(grow(), gap(unit(1))).children(() -> {
                        new ChatMessageListView(store, service);
                        divider();
                        new ChatInputView(store, service);
                    });
                })
                .tab(Core.bundle.get("feature.chat.ui.members", "Members"), () -> {
                    new ChatUserListView(store, service);
                });
    }

    public boolean isFeedVisible(@Nullable String channelId) {
        if (Boolean.TRUE.equals(feature.collapsedConfig.get())) {
            return false;
        }
        if (!Objects.equals(store.channels().currentActiveId(), channelId)) {
            return false;
        }

        if (Units.width() < 1200) {
            return store.ui().mobileTab().peek() == 1;
        }
        return true;
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.root().invalidateHierarchy();
            hud.pack();
            hud.keepInScreen();
        }
    }
}

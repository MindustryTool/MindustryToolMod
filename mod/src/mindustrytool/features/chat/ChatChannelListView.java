package mindustrytool.features.chat;

import static solim.UI.*;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;
import static solim.modifier.Values.visible;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import java.util.Objects;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustrytool.components.FileIcon;
import mindustrytool.components.Loader;
import mindustrytool.components.WebStyles;
import mindustrytool.models.response.ChannelDto;
import solim.core.Component;
import solim.graphics.RoundedDrawable;
import solim.reactive.Computed;
import solim.reactive.Readable;

public class ChatChannelListView extends Component {
    private final ChatStore store;
    private final @Nullable ChatService service;

    public ChatChannelListView(ChatStore store) {
        this(store, null);
    }

    public ChatChannelListView(ChatStore store, @Nullable ChatService service) {
        this.store = store;
        this.service = service;
    }

    @Override
    protected Element build() {
        return column().name("channel-list").props(grow(), padding(unit(2)), gap(unit(1))).children(() -> {
            query(store.channels().channelsQuery())
                    .props(grow())
                    .loading(Loader::centered)
                    .error(err -> column().props(grow(), padding(unit(2)), center(), gap(unit(2))).children(() -> {
                        icon(Icon.warning).props(size(unit(6)), prefWidth(unit(6)), prefHeight(unit(6)))
                                .color(Color.scarlet);
                        text(Core.bundle.get("feature.chat.ui.error.channels", "Failed to load channels."))
                                .color(Color.scarlet)
                                .fontScale(0.95f)
                                .wrap()
                                .center();
                        text(err != null ? err.getMessage() : "")
                                .color(Color.gray)
                                .fontScale(0.8f)
                                .wrap()
                                .center();
                        button(Core.bundle.get("feature.chat.ui.retry", "Retry"), () -> {
                            if (service != null) {
                                service.refreshChannels();
                            } else {
                                store.channels().channelsQuery().refetch();
                            }
                        })
                                .style(WebStyles.secondary())
                                .props(height(unit(9)), prefHeight(unit(9)))
                                .children(() -> {
                                    icon(Icon.refresh).props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)));
                                    text(Core.bundle.get("feature.chat.ui.retry", "Retry"));
                                });
                    }))
                    .data(channelList -> scroll().props(grow()).children(() -> {
                        column().props(growX(), gap(unit(1))).children(() -> {
                            if (channelList != null && !channelList.isEmpty()) {
                                forEach(store.channels().all())
                                        .key(ChannelDto::getId)
                                        .props(growX())
                                        .items(channel -> new ChannelItem(channel, store));
                            } else {
                                column().props(padding(unit(2))).children(() -> {
                                    text(Core.bundle.get("feature.chat.ui.empty-channels", "No channels available."))
                                            .color(Color.gray)
                                            .fontScale(0.9f);
                                });
                            }
                        });
                    })).props(grow());
        }).element();
    }

    private static class ChannelItem extends Component {
        private static final Color SELECTED_BG = new Color(0.45f, 0.35f, 0.9f, 0.8f);
        private static @Nullable TextButtonStyle selectedStyle;
        private static @Nullable TextButtonStyle defaultStyle;

        private static TextButtonStyle getSelectedStyle() {
            if (selectedStyle == null) {
                selectedStyle = new TextButtonStyle() {
                    {
                        down = new RoundedDrawable(unit(2), SELECTED_BG);
                        up = new RoundedDrawable(unit(2), SELECTED_BG);
                        over = new RoundedDrawable(unit(2), SELECTED_BG);
                        font = Fonts.def;
                        fontColor = Color.white;
                        disabledFontColor = Color.gray;
                    }
                };
            }
            return selectedStyle;
        }

        private static TextButtonStyle getDefaultStyle() {
            if (defaultStyle == null) {
                TextButtonStyle base = Styles.cleart != null ? Styles.cleart : new TextButtonStyle();
                defaultStyle = new TextButtonStyle(base) {
                    {
                        down = new RoundedDrawable(unit(2), Color.gray);
                        over = new RoundedDrawable(unit(2), Color.gray);
                    }
                };
            }
            return defaultStyle;
        }

        private final ChannelDto channel;
        private final ChatStore store;

        public ChannelItem(ChannelDto channel, ChatStore store) {
            this.channel = channel;
            this.store = store;
        }

        @Override
        protected Element build() {
            Computed<Boolean> isSelected = new Computed<>(
                    () -> Objects.equals(store.channels().activeId().get(), channel.getId()));

            Readable<Boolean> hasUnread = store.unread().forChannel(channel.getId())
                    .map(count -> count != null && count > 0);

            Readable<Boolean> isMuted = store.notifications().isMuted(channel.getId());

            Computed<Boolean> showUnreadDot = new Computed<>(
                    () -> Boolean.TRUE.equals(hasUnread.get()) && !Boolean.TRUE.equals(isMuted.get()));

            Computed<Boolean> showUnselectedMuted = new Computed<>(
                    () -> !Boolean.TRUE.equals(isSelected.get()) && Boolean.TRUE.equals(isMuted.get()));

            Readable<Drawable> bellIcon = isMuted.map(m -> Boolean.TRUE.equals(m)
                    ? FileIcon.of("bell-off.png")
                    : FileIcon.of("bell.png"));

            Readable<String> bellTooltip = isMuted.map(m -> Boolean.TRUE.equals(m)
                    ? Core.bundle.get("feature.chat.channel.notifications.unmute", "Unmute notifications")
                    : Core.bundle.get("feature.chat.channel.notifications.mute", "Mute notifications"));

            Computed<ButtonStyle> style = isSelected.map(s -> s ? getSelectedStyle() : getDefaultStyle());

            return row()
                    .props(growX(), height(unit(12)), prefHeight(unit(12)), padding(unit(1)), left())
                    .children(() -> {
                        button(() -> store.selectChannel(channel.getId()))
                                .style(style)
                                .left()
                                .props(grow(), paddingX(unit(1)))
                                .children(() -> {
                                    text("# " + channel.getName()).left();

                                    spacer();

                                    button(() -> store.notifications().toggleMuted(channel.getId()))
                                            .stopClickPropagation(true)
                                            .style(WebStyles.ghost())
                                            .props(size(unit(8)), prefWidth(unit(8)), prefHeight(unit(8)),
                                                    tooltip(bellTooltip))
                                            .visible(isSelected)
                                            .children(() -> icon(bellIcon)
                                                    .props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)))
                                                    .color(Color.white));

                                    image(FileIcon.of("bell-off.png"))
                                            .props(size(unit(4)), prefWidth(unit(4)), prefHeight(unit(4)))
                                            .color(Color.lightGray)
                                            .props(tooltip(Core.bundle.get("feature.chat.channel.notifications.muted",
                                                    "Muted")), visible(showUnselectedMuted));

                                    image(circle())
                                            .props(size(unit(3)), prefWidth(unit(3)), prefHeight(unit(3)))
                                            .color(Color.white)
                                            .props(visible(showUnreadDot));
                                });
                    })
                    .element();
        }
    }
}

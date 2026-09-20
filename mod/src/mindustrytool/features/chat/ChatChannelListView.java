package mindustrytool.features.chat;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.Button.ButtonStyle;
import arc.scene.ui.TextButton.TextButtonStyle;
import java.util.Objects;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustrytool.components.Loader;
import mindustrytool.components.WebStyles;
import mindustrytool.models.response.ChannelDto;
import solim.core.BaseComponent;
import solim.graphics.RoundedDrawable;
import solim.reactive.Computed;
import solim.reactive.Readable;

public class ChatChannelListView extends BaseComponent {

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
        return column().name("channel-list").grow().gap(unit(1)).padding(unit(2)).children(() -> {
            query(store.channels().channelsQuery())
                    .grow()
                    .loading(Loader::centered)
                    .error(err -> column().grow().center().gap(unit(2)).padding(unit(2)).children(() -> {
                        icon(Icon.warning).size(unit(6)).color(Color.scarlet);
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
                                .height(unit(9))
                                .children(() -> {
                                    icon(Icon.refresh).size(unit(4));
                                    text(Core.bundle.get("feature.chat.ui.retry", "Retry"));
                                });
                    }))
                    .data(channelList -> scroll().grow().children(() -> {
                        column().growX().gap(unit(1)).children(() -> {
                            if (channelList != null && !channelList.isEmpty()) {
                                forEach(store.channels().all(), ChannelDto::getId,
                                        channel -> new ChannelItem(channel, store))
                                                .growX();
                            } else {
                                column().padding(unit(2)).children(() -> {
                                    text(Core.bundle.get("feature.chat.ui.empty-channels", "No channels available."))
                                            .color(Color.gray)
                                            .fontScale(0.9f);
                                });
                            }
                        });
                    })).grow();
        }).element();
    }

    private static class ChannelItem extends BaseComponent {
        private static final Color SELECTED_BG = new Color(0.45f, 0.35f, 0.9f, 0.8f);
        private static final TextButtonStyle selectedStyle = new TextButtonStyle() {
            {
                down = new RoundedDrawable(unit(2), SELECTED_BG);
                up = new RoundedDrawable(unit(2), SELECTED_BG);
                over = new RoundedDrawable(unit(2), SELECTED_BG);
                font = Fonts.def;
                fontColor = Color.white;
                disabledFontColor = Color.gray;
            }
        };

        private static final TextButtonStyle defaultStyle = new TextButtonStyle(Styles.cleart) {
            {
                down = new RoundedDrawable(unit(2), Color.gray);
                over = new RoundedDrawable(unit(2), Color.gray);
            }
        };

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

            Computed<ButtonStyle> style = isSelected.map(s -> s ? selectedStyle : defaultStyle);

            return row()
                    .growX()
                    .height(unit(12))
                    .padding(unit(1))
                    .left()
                    .children(() -> {
                        button(() -> store.selectChannel(channel.getId()))
                                .style(style)
                                .left()
                                .grow()
                                .paddingX(unit(1))
                                .children(() -> {
                                    text("# " + channel.getName()).left();

                                    spacer();

                                    image(circle())
                                            .size(unit(3))
                                            .color(Color.white)
                                            .visible(hasUnread);
                                });
                    })
                    .element();
        }
    }
}

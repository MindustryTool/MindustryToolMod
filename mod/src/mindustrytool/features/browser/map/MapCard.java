package mindustrytool.features.browser.map;

import static solim.UI.*;
import static solim.modifier.Values.background;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.onClick;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;
import static solim.modifier.Values.width;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.components.FileIcon;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserLayout;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.models.response.MapData;
import solim.core.Component;
import solim.reactive.Readable;
import mindustrytool.components.WebStyles;

public class MapCard extends Component {

    public static final float CARD_SIZE = BrowserLayout.CARD_SIZE;

    private final MapData map;
    private final @Nullable Readable<Float> previewHeight;
    private final Runnable onClick;
    private final Runnable onDownload;
    private final Runnable onDetails;
    private final Runnable onPlay;

    public MapCard(MapData map, Runnable onClick, Runnable onDownload, Runnable onDetails, Runnable onPlay) {
        this(map, null, onClick, onDownload, onDetails, onPlay);
    }

    public MapCard(
            MapData map,
            @Nullable Readable<Float> previewHeight,
            Runnable onClick,
            Runnable onDownload,
            Runnable onDetails,
            Runnable onPlay) {
        this.map = map;
        this.previewHeight = previewHeight;
        this.onClick = onClick;
        this.onDownload = onDownload;
        this.onDetails = onDetails;
        this.onPlay = onPlay;
    }

    @Override
    protected Element build() {
        String imageUrl = BrowserImages.mapPreviewUrl(map.getItemId());
        String title = map.getName() != null ? map.getName()
                : Core.bundle.get("browser.map.unnamed");
        String likesText = BrowserStatsBadge.formatCount(BrowserImages.count(map.getLikes()));
        String commentsText = BrowserStatsBadge.formatCount(BrowserImages.count(map.getComments()));
        String downloadsText = BrowserStatsBadge.formatCount(BrowserImages.count(map.getDownloads()));

        Readable<Float> size = previewHeight != null ? previewHeight : Readable.of(CARD_SIZE);

        return column()
                .name("MapCard-" + map.getItemId())
                .props(width(size), prefWidth(size), gap(unit(2)))
                .children(() -> {
                    card()
                            .name("MapCard-preview-" + map.getItemId())
                            .props(background(Color.black), size(size), prefWidth(size), prefHeight(size), onClick(onClick))
                            .children(() -> {
                                stack().grow()
                                        .layer(() -> networkImage()
                                                .rounded(8)
                                                .scaling(Scaling.fit)
                                                .props(grow())
                                                .url(imageUrl)
                                                .placeholder(Icon.terrain)
                                                .fallback(Icon.terrain))
                                        .layer(() -> column().props(grow()).children(() -> {
                                            spacer();
                                            row().props(growX(), background(Styles.black8), padding(unit(1.5f), unit(2f), unit(1.5f), unit(2f)), center())
                                                    .children(() -> {
                                                        text(title)
                                                                .style(Styles.defaultLabel)
                                                                .color(Color.white)
                                                                .ellipsis(true)
                                                                .center()
                                                                .growX();
                                                    });
                                        }));
                            });

                    row().props(width(size), prefWidth(size), gap(unit(1.5f))).children(() -> {
                        statButton(
                                likesText,
                                FileIcon.of("heart.png", Icon.upOpenSmall),
                                Color.white,
                                onDetails,
                                Core.bundle.get("browser.map.details"));
                        statButton(
                                commentsText,
                                FileIcon.of("message-circle.png"),
                                Color.white,
                                onDetails,
                                Core.bundle.get("browser.map.details"));
                        statButton(
                                downloadsText,
                                Icon.downloadSmall,
                                map.getDownloads() != null && map.getDownloads() > 0 ? Color.sky : Color.white,
                                onDownload,
                                Core.bundle.get("browser.map.download"));

                        button(onPlay)
                                .style(WebStyles.actionCardText())
                                .props(growX(), height(unit(9)), prefHeight(unit(9)), tooltip(Core.bundle.get("browser.map.play")))
                                .children(() -> icon(Icon.play).props(size(unit(4.5f)), prefWidth(unit(4.5f)), prefHeight(unit(4.5f))).color(Color.white));
                    });
                }).element();
    }

    private void statButton(String count, Drawable iconDrawable, Color textColor, Runnable action, String tooltip) {
        button(action)
                .style(WebStyles.actionCardText())
                .props(growX(), height(unit(9)), prefHeight(unit(9)), gap(unit(1)), tooltip(tooltip))
                .children(() -> {
                    icon(iconDrawable).props(size(unit(4.5f)), prefWidth(unit(4.5f)), prefHeight(unit(4.5f))).color(Color.white).marginRight(unit(1));
                    text(count).style(Styles.defaultLabel).fontScale(0.85f).color(textColor);
                });
    }
}

package mindustrytool.features.browser.map;

import static solim.UI.*;

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
import solim.core.BaseComponent;
import solim.reactive.Readable;
import mindustrytool.components.WebStyles;

/**
 * Image-first card layout showing a dedicated preview card with a centered
 * title strip and a compact row of interactive stat action buttons.
 */
public class MapCard extends BaseComponent {

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

        Readable<Float> size = previewHeight != null ? previewHeight : Readable.of(CARD_SIZE);

        return column()
                .name("MapCard-" + map.getItemId())
                .width(size)
                .gap(unit(2))
                .children(() -> {
                    card(WebStyles.previewCardBackground())
                            .name("MapCard-preview-" + map.getItemId())
                            .size(size)
                            .onClick(onClick)
                            .children(() -> {
                                stack().grow().children(() -> {
                                    networkImage(imageUrl)
                                            .placeholder(Icon.terrain)
                                            .fallback(Icon.terrain)
                                            .grow()
                                            .rounded(8)
                                            .scaling(Scaling.fit);
                                }).children(() -> {
                                    column().grow().children(() -> {
                                        spacer();
                                        row().growX()
                                                .background(Styles.black8)
                                                .padding(unit(1.5f), unit(2f), unit(1.5f), unit(2f))
                                                .center()
                                                .children(() -> {
                                                    text(title)
                                                            .style(Styles.defaultLabel)
                                                            .color(Color.white)
                                                            .ellipsis(true)
                                                            .center()
                                                            .growX();
                                                });
                                    });
                                });
                            });

                    row().width(size).gap(unit(1.5f)).children(() -> {
                        statButton(
                                BrowserStatsBadge.formatCount(BrowserImages.count(map.getLikes())),
                                FileIcon.of("heart.png", Icon.upOpenSmall),
                                Color.white,
                                onDetails,
                                Core.bundle.get("browser.map.details"));
                        statButton(
                                BrowserStatsBadge.formatCount(BrowserImages.count(map.getComments())),
                                FileIcon.of("message-circle.png"),
                                Color.white,
                                onDetails,
                                Core.bundle.get("browser.map.details"));
                        statButton(
                                BrowserStatsBadge.formatCount(BrowserImages.count(map.getDownloads())),
                                Icon.downloadSmall,
                                map.getDownloads() != null && map.getDownloads() > 0 ? Color.sky : Color.white,
                                onDownload,
                                Core.bundle.get("browser.map.download"));

                        button(onPlay)
                                .style(WebStyles.cardActionText())
                                .growX()
                                .height(unit(9))
                                .tooltip(Core.bundle.get("browser.map.play"))
                                .children(() -> icon(Icon.play).size(unit(4.5f)).color(Color.white));
                    });
                }).element();
    }

    private void statButton(String count, Drawable iconDrawable, Color textColor, Runnable action, String tooltip) {
        button(action)
                .style(WebStyles.cardActionText())
                .growX()
                .height(unit(9))
                .gap(unit(1))
                .tooltip(tooltip)
                .children(() -> {
                    icon(iconDrawable).size(unit(4.5f)).color(Color.white).marginRight(unit(1));
                    text(count).style(Styles.defaultLabel).fontScale(0.85f).color(textColor);
                });
    }
}

package mindustrytool.features.browser.map;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.features.browser.common.WebStyles;
import mindustrytool.models.response.MapData;
import solim.core.BaseComponent;

/**
 * Hero image-first card showing a large map preview with the title overlaid
 * on a translucent bottom bar and a single row of compact interactive action
 * buttons with stat counts.
 */
public class MapCard extends BaseComponent {

    private final MapData map;
    private final Runnable onClick;
    private final Runnable onDownload;
    private final Runnable onDetails;
    private final Runnable onPlay;

    public MapCard(MapData map, Runnable onClick, Runnable onDownload, Runnable onDetails, Runnable onPlay) {
        this.map = map;
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

        return card(Styles.black8)
                .name("MapCard-" + map.getItemId())
                .growX()
                .rounded(6)
                .border(1f, Color.darkGray)
                .onClick(onClick)
                .children(() -> {
                    column().growX().padding(unit(2)).gap(unit(2)).children(() -> {
                        stack().growX().children(() -> {
                            networkImage(imageUrl)
                                    .placeholder(Icon.terrain)
                                    .fallback(Icon.terrain)
                                    .growX()
                                    .height(unit(38))
                                    .rounded(4)
                                    .scaling(Scaling.fit);
                        }).children(() -> {
                            column().grow().children(() -> {
                                spacer();
                                row().growX().background(Styles.black8).padding(unit(1)).children(() -> {
                                    text(title)
                                            .style(Styles.defaultLabel)
                                            .color(Color.white)
                                            .ellipsis(true)
                                            .left()
                                            .growX();
                                });
                            });
                        });

                        row().growX().gap(unit(1)).children(() -> {
                            statButton(
                                    BrowserStatsBadge.formatCount(BrowserImages.count(map.getLikes())),
                                    Icon.upOpenSmall, Color.scarlet, onDetails,
                                    Core.bundle.get("browser.map.details"));
                            statButton(
                                    BrowserStatsBadge.formatCount(BrowserImages.count(map.getComments())),
                                    Icon.chatSmall, Color.lightGray, onDetails,
                                    Core.bundle.get("browser.map.details"));
                            statButton(
                                    BrowserStatsBadge.formatCount(BrowserImages.count(map.getDownloads())),
                                    Icon.downloadSmall, Color.sky, onDownload,
                                    Core.bundle.get("browser.map.download"));

                            button(onPlay).style(WebStyles.outlineText()).growX().height(unit(10))
                                    .tooltip(Core.bundle.get("browser.map.play"))
                                    .children(() -> icon(Icon.play).size(unit(5)));
                        });
                    });
                }).element();
    }

    private void statButton(String count, Drawable icon, Color tint, Runnable action, String tooltip) {
        button(action)
                .style(WebStyles.outlineText())
                .growX()
                .height(unit(10))
                .tooltip(tooltip)
                .children(() -> {
                    icon(icon).size(unit(5)).color(tint);
                    text(count).style(Styles.defaultLabel).fontScale(0.9f);
                });
    }
}

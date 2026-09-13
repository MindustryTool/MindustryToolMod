package mindustrytool.features.browser.map;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Scaling;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.features.browser.common.WebStyles;
import mindustrytool.models.response.MapData;
import solim.core.BaseComponent;

/**
 * Card showing a map preview thumbnail, title, stats, and actions for
 * download and details.
 */
public class MapCard extends BaseComponent {

    private final MapData map;
    private final Runnable onClick;
    private final Runnable onDownload;
    private final Runnable onDetails;

    public MapCard(MapData map, Runnable onClick, Runnable onDownload, Runnable onDetails) {
        this.map = map;
        this.onClick = onClick;
        this.onDownload = onDownload;
        this.onDetails = onDetails;
    }

    @Override
    protected Element build() {
        String imageUrl = BrowserImages.mapPreviewUrl(map.getItemId());

        return card(Styles.black8)
                .name("MapCard-" + map.getItemId())
                .growX()
                .rounded(6)
                .border(1f, Color.darkGray)
                .onClick(onClick)
                .children(() -> {
                    column().growX().padding(unit(2)).gap(unit(1)).children(() -> {
                        networkImage(imageUrl)
                                .growX()
                                .height(unit(30))
                                .rounded(4)
                                .scaling(Scaling.fit);

                        text(map.getName() != null ? map.getName()
                                : Core.bundle.get("browser.map.unnamed"))
                                .style(Styles.defaultLabel)
                                .color(Color.white)
                                .ellipsis(true)
                                .left()
                                .growX();

                        new BrowserStatsBadge(
                                BrowserImages.count(map.getLikes()),
                                BrowserImages.count(map.getComments()),
                                BrowserImages.count(map.getDownloads()));

                        row().growX().gap(unit(1)).children(() -> {
                            button(onDownload).style(WebStyles.outlineText()).size(unit(10))
                                    .tooltip(Core.bundle.get("browser.map.download"))
                                    .children(() -> icon(Icon.download).size(unit(5)));

                            spacer();

                            button(onDetails).style(WebStyles.outlineText()).size(unit(10))
                                    .tooltip(Core.bundle.get("browser.map.details"))
                                    .children(() -> icon(Icon.infoCircle).size(unit(5)));
                        });
                    });
                }).element();
    }
}

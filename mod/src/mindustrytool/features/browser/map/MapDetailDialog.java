package mindustrytool.features.browser.map;

import static solim.UI.*;
import static solim.modifier.Values.color;
import static solim.modifier.Values.fillParent;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.origin;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.style;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Align;
import arc.util.Scaling;
import arc.util.Nullable;
import java.time.Duration;
import java.util.Collections;
import mindustry.gen.Icon;
import mindustrytool.Config;
import mindustrytool.components.WebStyles;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserLayout;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.models.response.MapDetailData;
import mindustrytool.services.MindustryTool;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Query;
import solim.reactive.QueryKey;

public class MapDetailDialog extends SolimDialog {

    public MapDetailDialog(MapDetailData detail, String itemId) {
        super(detail.getName() != null ? detail.getName() : Core.bundle.get("browser.map.unnamed"));

        addCloseButton();
        closeOnBack();
        props(fillParent(true));
        children(() -> new DetailContent(detail, itemId));
        actionButton(Core.bundle.get("browser.detail.open-online"), Icon.link,
                () -> Core.app.openURI(Config.WEB_URL + "/maps/" + itemId));
    }

    private static class DetailContent extends Component {
        private final MapDetailData detail;
        private final String itemId;
        private final @Nullable String authorId;
        private final @Nullable Query<String> authorQuery;

        DetailContent(MapDetailData detail, String itemId) {
            this.detail = detail;
            this.itemId = itemId;
            this.authorId = detail.getCreatedBy();
            this.authorQuery = authorId != null && !authorId.isEmpty()
                    ? Query.<String>builder()
                            .key(QueryKey.of("user", authorId))
                            .fetch(() -> MindustryTool.getUserBatch(Collections.singletonList(authorId))
                                    .thenApply(users -> users != null && !users.isEmpty() && users.get(0) != null
                                            && users.get(0).getName() != null
                                                    ? users.get(0).getName()
                                                    : authorId))
                            .staleTime(Duration.ofMinutes(10))
                            .build()
                    : null;
        }

        @Override
        protected Element build() {
            return column().props(grow(), padding(unit(2)), gap(unit(2))).children(() -> {
                when(isPortrait())
                        .thenDo(this::portraitLayout)
                        .elseDo(this::landscapeLayout)
                        .props(grow());
            }).element();
        }

        private Component portraitLayout() {
            return column().props(grow(), top(), left(), gap(unit(4))).children(() -> {
                previewImagePortrait();
                scroll().props(grow()).children(() -> details());
            });
        }

        private Component landscapeLayout() {
            return row().props(grow(), center(), top(), gap(unit(4))).children(() -> {
                previewImageLandscape();
                scroll().props(grow()).children(() -> details());
            });
        }

        private void previewImagePortrait() {
            row().props(grow()).children(() -> {
                networkImage(BrowserImages.mapImageUrl(itemId))
                        .props(
                                origin(Align.top | Align.left),
                                growX(),
                                solim.modifier.Values.placeholder(Icon.terrain),
                                solim.modifier.Values.fallback(Icon.terrain),
                                solim.modifier.Values.imageRounded(8),
                                solim.modifier.Values.scaling(Scaling.fit)
                        );
            });
        }

        private void previewImageLandscape() {
            row().props(grow()).children(() -> {
                networkImage(BrowserImages.mapImageUrl(itemId))
                        .props(
                                origin(Align.top),
                                growX(),
                                solim.modifier.Values.placeholder(Icon.terrain),
                                solim.modifier.Values.fallback(Icon.terrain),
                                solim.modifier.Values.scaling(Scaling.fit)
                        );
            });
        }

        private void details() {
            column().props(growX(), gap(unit(2))).children(() -> {
                card(WebStyles.previewCardBackground()).props(padding(unit(4)), growX(), gap(unit(2))).children(() -> {
                    row().props(growX(), gap(unit(2))).children(() -> {
                        text(Core.bundle.get("browser.detail.author")).props(color(Color.lightGray), solim.modifier.Values.fontScale(1.2f));
                        if (authorQuery != null) {
                            query(authorQuery)
                                    .props(growX())
                                    .loading(() -> text(authorId != null ? authorId : "")
                                            .props(color(Color.white), solim.modifier.Values.fontScale(1.2f)))
                                    .error(err -> text(authorId != null ? authorId : "")
                                            .props(color(Color.white), solim.modifier.Values.fontScale(1.2f)))
                                    .data(name -> text(name).props(color(Color.white), solim.modifier.Values.fontScale(1.2f)));
                        } else {
                            text(authorId != null ? authorId : "").props(color(Color.white), solim.modifier.Values.fontScale(1.2f));
                        }
                    });

                    row().props(growX(), gap(unit(2))).children(() -> {
                        text(Core.bundle.get("browser.detail.dimensions")).props(color(Color.lightGray));
                        text(detail.getWidth() + "x" + detail.getHeight()).props(color(Color.white));
                    });
                });

                card(WebStyles.previewCardBackground()).props(padding(unit(4)), growX(), gap(unit(2))).children(() -> {
                    new BrowserStatsBadge(
                            BrowserImages.count(detail.getLikes()),
                            BrowserImages.count(detail.getComments()),
                            BrowserImages.count(detail.getDownloads()));
                });

                BrowserLayout.renderTags(detail.getTags());

                if (detail.getDescription() != null && !detail.getDescription().isEmpty()) {
                    card(WebStyles.previewCardBackground()).props(padding(unit(4)), growX(), gap(unit(2))).children(() -> {
                        text(detail.getDescription()).props(color(Color.lightGray), solim.modifier.Values.wrap(true), solim.modifier.Values.left(), growX());
                    });
                }

                divider();

                row().props(growX(), gap(unit(2))).children(() -> {
                    button(Core.bundle.get("browser.map.download"),
                            () -> MapActions.downloadAndImport(itemId))
                            .props(style(WebStyles.primary()), growX(), height(unit(11)), prefHeight(unit(11)));

                    button(Core.bundle.get("browser.map.play"),
                            () -> MapActions.playMap(itemId))
                            .props(style(WebStyles.primary()), growX(), height(unit(11)), prefHeight(unit(11)));
                });
            });
        }
    }
}

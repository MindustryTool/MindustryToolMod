package mindustrytool.features.browser.map;

import static solim.UI.*;

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
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Query;
import solim.reactive.QueryKey;

/**
 * Detail dialog showing a full map preview, author, dimensions, stats, tags,
 * description, and download/play shortcuts. Stacks vertically in portrait and
 * side-by-side in landscape.
 */
public class MapDetailDialog extends SolimDialog {

    public MapDetailDialog(MapDetailData detail, String itemId) {
        super(detail.getName() != null ? detail.getName() : Core.bundle.get("browser.map.unnamed"));

        addCloseButton();
        closeOnBack();
        fillParent(true);
        children(() -> new DetailContent(detail, itemId));
        actionButton(Core.bundle.get("browser.detail.open-online"), Icon.link,
                () -> Core.app.openURI(Config.WEB_URL + "/maps/" + itemId));
    }

    private static class DetailContent extends BaseComponent {
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
            return column().grow().padding(unit(2)).gap(unit(2)).children(() -> {
                when(isPortrait())
                        .thenDo(this::portraitLayout)
                        .elseDo(this::landscapeLayout)
                        .grow();
            }).element();
        }

        private Component portraitLayout() {
            return column().grow().top().left().gap(unit(4)).children(() -> {
                previewImagePortrait();
                scroll().grow().children(() -> details());
            });
        }

        private Component landscapeLayout() {
            return row().grow().center().top().gap(unit(4)).children(() -> {
                previewImageLandscape();
                scroll().grow().children(() -> details());
            });
        }

        private void previewImagePortrait() {
            row().grow().children(() -> {
                networkImage(BrowserImages.mapImageUrl(itemId))
                        .placeholder(Icon.terrain)
                        .origin(Align.top | Align.left)
                        .fallback(Icon.terrain)
                        .growX()
                        .rounded(8)
                        .scaling(Scaling.fit);
            });
        }

        private void previewImageLandscape() {
            row().grow().children(() -> {
                networkImage(BrowserImages.mapImageUrl(itemId))
                        .placeholder(Icon.terrain)
                        .origin(Align.top)
                        .fallback(Icon.terrain)
                        .growX()
                        .scaling(Scaling.fit);
            });
        }

        private void details() {
            column().growX().gap(unit(2)).children(() -> {
                card(WebStyles.previewCardBackground()).padding(unit(4)).gap(unit(2)).growX().children(() -> {
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("browser.detail.author")).color(Color.lightGray).fontScale(1.2f);
                        if (authorQuery != null) {
                            query(authorQuery)
                                    .growX()
                                    .loading(() -> text(authorId != null ? authorId : "").color(Color.white)
                                            .fontScale(1.2f))
                                    .error(err -> text(authorId != null ? authorId : "").color(Color.white)
                                            .fontScale(1.2f))
                                    .data(name -> text(name).color(Color.white).fontScale(1.2f));
                        } else {
                            text(authorId != null ? authorId : "").color(Color.white).fontScale(1.2f);
                        }
                    });

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("browser.detail.dimensions")).color(Color.lightGray);
                        text(detail.getWidth() + "x" + detail.getHeight()).color(Color.white);
                    });
                });

                card(WebStyles.previewCardBackground()).padding(unit(4)).gap(unit(2)).growX().children(() -> {
                    new BrowserStatsBadge(
                            BrowserImages.count(detail.getLikes()),
                            BrowserImages.count(detail.getComments()),
                            BrowserImages.count(detail.getDownloads()));
                });

                BrowserLayout.renderTags(detail.getTags());

                if (detail.getDescription() != null && !detail.getDescription().isEmpty()) {
                    card(WebStyles.previewCardBackground()).padding(unit(4)).gap(unit(2)).growX().children(() -> {
                        text(detail.getDescription()).color(Color.lightGray).wrap(true).left().growX();
                    });
                }

                divider();

                row().growX().gap(unit(2)).children(() -> {
                    button(Core.bundle.get("browser.map.download"),
                            () -> MapActions.downloadAndImport(itemId))
                                    .style(WebStyles.primary())
                                    .growX()
                                    .height(unit(11));

                    button(Core.bundle.get("browser.map.play"),
                            () -> MapActions.playMap(itemId))
                                    .style(WebStyles.primary())
                                    .growX()
                                    .height(unit(11));
                });
            });
        }
    }
}

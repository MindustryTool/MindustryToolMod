package mindustrytool.features.browser.map;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Align;
import arc.util.Scaling;
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
import solim.reactive.Signal;

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
        private final Signal<String> authorName;

        DetailContent(MapDetailData detail, String itemId) {
            this.detail = detail;
            this.itemId = itemId;
            this.authorName = Signal.of(detail.getCreatedBy() != null ? detail.getCreatedBy() : "");
            resolveAuthorName(detail.getCreatedBy());
        }

        @Override
        protected Element build() {
            return column().grow().padding(unit(2)).gap(unit(2)).children(() -> {
                dynamic(isPortrait(), portrait -> {
                    if (Boolean.TRUE.equals(portrait)) {
                        return portraitLayout();
                    }
                    return landscapeLayout();
                }).grow();
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
            row().maxHeight(dvw(50)).grow().children(() -> {
                networkImage(BrowserImages.mapImageUrl(itemId))
                        .placeholder(Icon.terrain)
                        .origin(Align.top | Align.left)
                        .fallback(Icon.terrain)
                        .grow()
                        .scaling(Scaling.fit);
            });
        }

        private void previewImageLandscape() {
            row().maxWidth(dvw(50)).grow().children(() -> {
                networkImage(BrowserImages.mapImageUrl(itemId))
                        .placeholder(Icon.terrain)
                        .origin(Align.top)
                        .fallback(Icon.terrain)
                        .grow()
                        .scaling(Scaling.fit);
            });
        }

        private void details() {
            column().growX().gap(unit(2)).children(() -> {
                card(WebStyles.previewCardBackground()).padding(unit(4)).gap(unit(2)).growX().children(() -> {
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("browser.detail.author")).color(Color.lightGray).fontScale(1.2f);
                        text(authorName).color(Color.white).fontScale(1.2f);
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

        private void resolveAuthorName(String createdBy) {
            if (createdBy == null || createdBy.isEmpty()) {
                return;
            }
            MindustryTool.getUserBatch(Collections.singletonList(createdBy))
                    .whenComplete((users, throwable) -> {
                        if (throwable != null || users == null || users.isEmpty()) {
                            return;
                        }
                        Core.app.post(() -> {
                            if (!isDisposed() && users.get(0) != null && users.get(0).getName() != null) {
                                authorName.set(users.get(0).getName());
                            }
                        });
                    });
        }
    }
}

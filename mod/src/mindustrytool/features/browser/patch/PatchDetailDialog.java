package mindustrytool.features.browser.patch;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import java.time.Duration;
import java.util.Collections;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.Config;
import mindustrytool.components.WebStyles;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserLayout;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.models.response.ContentPatchDetailData;
import mindustrytool.services.MindustryTool;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Query;
import solim.reactive.QueryKey;

/**
 * Detail dialog showing metadata, author, stats, tags, description,
 * and a scrollable mono-font viewer for the raw HJSON patch content.
 */
public class PatchDetailDialog extends SolimDialog {

    public PatchDetailDialog(ContentPatchDetailData detail, String itemId) {
        super(detail.getName() != null && !detail.getName().trim().isEmpty()
                ? detail.getName().trim()
                : Core.bundle.get("browser.patch.unnamed"));

        addCloseButton();
        closeOnBack();
        fillParent(true);
        children(() -> new DetailContent(detail, itemId));
        actionButton(Core.bundle.get("browser.detail.open-online"), Icon.link,
                () -> Core.app.openURI(Config.WEB_URL + "/content-patches/" + itemId));
    }

    private static class DetailContent extends BaseComponent {
        private final ContentPatchDetailData detail;
        private final @Nullable String authorId;
        private final @Nullable Query<String> authorQuery;

        DetailContent(ContentPatchDetailData detail, String itemId) {
            this.detail = detail;
            this.authorId = detail.getCreatedBy();
            this.authorQuery = authorId != null && !authorId.isEmpty()
                    ? Query.<String>builder()
                            .key(QueryKey.of("user", authorId))
                            .fetch(() -> MindustryTool.getUserBatch(Collections.singletonList(authorId))
                                    .thenApply(users -> users != null && !users.isEmpty() && users.get(0) != null && users.get(0).getName() != null
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
            return scroll().grow().children(() -> column().grow().top().left().gap(unit(4)).children(() -> {
                details();
                codeSection();
                actionButtons();
            }));
        }

        private Component landscapeLayout() {
            return scroll().grow().children(() -> row().grow().top().gap(unit(4)).children(() -> {
                column().growX().top().gap(unit(3)).children(() -> {
                    details();
                    actionButtons();
                });
                column().grow().top().children(this::codeSection);
            }));
        }

        private void details() {
            column().growX().gap(unit(2)).children(() -> {
                // Author and Type row
                card(WebStyles.previewCardBackground()).padding(unit(4)).gap(unit(2)).growX().children(() -> {
                    row().growX().gap(unit(1)).children(() -> {
                        text(Core.bundle.get("browser.detail.author")).color(Color.lightGray).fontScale(1.1f);
                        if (authorQuery != null) {
                            query(authorQuery)
                                    .growX()
                                    .loading(() -> text(authorId != null ? authorId : "").color(Color.white).fontScale(1.1f))
                                    .error(err -> text(authorId != null ? authorId : "").color(Color.white).fontScale(1.1f))
                                    .data(name -> text(name).color(Color.white).fontScale(1.1f));
                        } else {
                            text(authorId != null ? authorId : "").color(Color.white).fontScale(1.1f);
                        }
                    });

                    boolean isPack = "DATAPACK".equalsIgnoreCase(detail.getType());
                    String typeLabel = isPack
                            ? Core.bundle.get("browser.patch.type.datapack")
                            : Core.bundle.get("browser.patch.type.data");
                    Color typeColor = isPack ? Color.royal : Color.coral;

                    row().growX().gap(unit(1)).children(() -> {
                        text(Core.bundle.get("browser.detail.type", "Type:")).color(Color.lightGray);
                        text(typeLabel).color(typeColor);
                    });
                });

                // Stats badge
                card(WebStyles.previewCardBackground()).padding(unit(4)).gap(unit(2)).growX().children(() -> {
                    new BrowserStatsBadge(
                            BrowserImages.count(detail.getLikes()),
                            BrowserImages.count(detail.getComments()),
                            BrowserImages.count(detail.getDownloads()));
                });

                // Tags
                if (detail.getTags() != null && !detail.getTags().isEmpty()) {
                    BrowserLayout.renderTags(detail.getTags());
                }

                // Description
                if (detail.getDescription() != null && !detail.getDescription().trim().isEmpty()) {
                    card(WebStyles.previewCardBackground()).padding(unit(4)).gap(unit(2)).growX().children(() -> {
                        text(detail.getDescription().trim()).color(Color.lightGray).wrap(true).left().growX();
                    });
                }
            });
        }

        private void codeSection() {
            String rawData = detail.getData() != null ? detail.getData() : "";
            String formattedData = HjsonHighlighter.format(rawData);
            String highlightedCode = HjsonHighlighter.highlight(formattedData);
            String lineNumbers = HjsonHighlighter.buildLineNumbers(formattedData);

            card(WebStyles.previewCardBackground()).grow().padding(unit(3)).gap(unit(2)).children(() -> {
                text(Core.bundle.get("browser.patch.data-section")).color(Color.white).growX().left();
                scroll().grow().maxHeight(unit(60)).scrollX(true).scrollY(true).children(() -> {
                    row().top().left().gap(unit(2)).children(() -> {
                        text(lineNumbers)
                                .style(Styles.monoLabel)
                                .color(Color.gray)
                                .wrap(false)
                                .right();

                        text(highlightedCode)
                                .style(Styles.monoLabel)
                                .wrap(false)
                                .left();
                    });
                });
            });
        }

        private void actionButtons() {
            String patchData = detail.getData() != null ? detail.getData() : "";
            String patchName = detail.getName() != null ? detail.getName() : "patch";

            row().growX().gap(unit(2)).children(() -> {
                button(Core.bundle.get("browser.patch.copy"),
                        () -> PatchActions.copyTextToClipboard(patchData))
                                .style(WebStyles.primary())
                                .growX()
                                .height(unit(11));

                button(Core.bundle.get("browser.patch.save"),
                        () -> PatchActions.saveTextToFile(patchData, patchName))
                                .style(WebStyles.primary())
                                .growX()
                                .height(unit(11));
            });
        }
    }
}

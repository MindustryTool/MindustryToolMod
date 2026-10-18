package mindustrytool.features.browser.patch;

import static solim.UI.*;
import static solim.modifier.Values.color;
import static solim.modifier.Values.fillParent;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.growY;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.style;
import static solim.modifier.Values.width;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import java.time.Duration;
import java.util.Collections;
import mindustry.gen.Icon;
import mindustry.ui.Fonts;
import mindustry.ui.Styles;
import mindustrytool.Config;
import mindustrytool.components.WebStyles;
import mindustrytool.features.browser.common.BrowserImages;
import mindustrytool.features.browser.common.BrowserLayout;
import mindustrytool.features.browser.common.BrowserStatsBadge;
import mindustrytool.models.response.ContentPatchDetailData;
import mindustrytool.services.MindustryTool;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Query;
import solim.reactive.QueryKey;

public class PatchDetailDialog extends SolimDialog {

    public PatchDetailDialog(ContentPatchDetailData detail, String itemId) {
        super(detail.getName() != null && !detail.getName().trim().isEmpty()
                ? detail.getName().trim()
                : Core.bundle.get("browser.patch.unnamed"));

        addCloseButton();
        closeOnBack();
        props(fillParent(true));
        children(() -> new DetailContent(detail, itemId));
        actionButton(Core.bundle.get("browser.detail.open-online"), Icon.link,
                () -> Core.app.openURI(Config.WEB_URL + "/content-patches/" + itemId));
    }

    private static class DetailContent extends Component {
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
            return scroll().props(grow()).children(() -> column().props(grow(), top(), left(), gap(unit(4))).children(() -> {
                details();
                codeSection();
                actionButtons();
            }));
        }

        private Component landscapeLayout() {
            return row().props(grow(), top(), gap(unit(4))).children(() -> {
                scroll().props(growY(), growX()).children(() -> column().props(growX(), top(), gap(unit(3))).children(() -> {
                    details();
                    actionButtons();
                }));
                column().props(grow(), top()).children(this::codeSection);
            });
        }

        private void details() {
            column().props(growX(), gap(unit(2))).children(() -> {
                card(WebStyles.previewCardBackground()).props(padding(unit(4)), growX(), gap(unit(2))).children(() -> {
                    row().props(growX(), gap(unit(1))).children(() -> {
                        text(Core.bundle.get("browser.detail.author")).props(color(Color.lightGray), solim.modifier.Values.fontScale(1.1f));
                        if (authorQuery != null) {
                            query(authorQuery)
                                    .props(growX())
                                    .loading(() -> text(authorId != null ? authorId : "")
                                            .props(color(Color.white), solim.modifier.Values.fontScale(1.1f)))
                                    .error(err -> text(authorId != null ? authorId : "")
                                            .props(color(Color.white), solim.modifier.Values.fontScale(1.1f)))
                                    .data(name -> text(name).props(color(Color.white), solim.modifier.Values.fontScale(1.1f)));
                        } else {
                            text(authorId != null ? authorId : "").props(color(Color.white), solim.modifier.Values.fontScale(1.1f));
                        }
                    });

                    boolean isPack = "DATAPACK".equalsIgnoreCase(detail.getType());
                    String typeLabel = isPack
                            ? Core.bundle.get("browser.patch.type.datapack")
                            : Core.bundle.get("browser.patch.type.data");
                    Color typeColor = isPack ? Color.royal : Color.coral;

                    row().props(growX(), gap(unit(1))).children(() -> {
                        text(Core.bundle.get("browser.detail.type")).props(color(Color.lightGray));
                        text(typeLabel).props(color(typeColor));
                    });
                });

                card(WebStyles.previewCardBackground()).props(padding(unit(4)), growX(), gap(unit(2))).children(() -> {
                    new BrowserStatsBadge(
                            BrowserImages.count(detail.getLikes()),
                            BrowserImages.count(detail.getComments()),
                            BrowserImages.count(detail.getDownloads()));
                });

                if (detail.getTags() != null && !detail.getTags().isEmpty()) {
                    BrowserLayout.renderTags(detail.getTags());
                }

                if (detail.getDescription() != null && !detail.getDescription().trim().isEmpty()) {
                    card(WebStyles.previewCardBackground()).props(padding(unit(4)), growX(), gap(unit(2))).children(() -> {
                        text(detail.getDescription().trim()).props(color(Color.lightGray), solim.modifier.Values.wrap(true), solim.modifier.Values.left(), growX());
                    });
                }
            });
        }

        private void codeSection() {
            if (Fonts.monospace != null && Fonts.monospace.getData() != null) {
                Fonts.monospace.getData().markupEnabled = true;
            }

            String rawData = detail.getData() != null ? detail.getData() : "";
            String formattedData = HjsonHighlighter.format(rawData);
            String highlightedCode = HjsonHighlighter.highlight(formattedData);
            String lineNumbers = HjsonHighlighter.buildLineNumbers(formattedData);

            card(WebStyles.previewCardBackground()).props(grow(), padding(unit(3)), gap(unit(2))).children(() -> {
                text(Core.bundle.get("browser.patch.data-section")).props(color(Color.white), growX(), solim.modifier.Values.left());
                scroll().props(grow(), solim.modifier.Values.scrollX(true), solim.modifier.Values.scrollY(true)).children(() -> {
                    row().props(top(), left()).children(() -> {
                        text(lineNumbers)
                                .props(style(Styles.monoLabel), color(Color.gray), solim.modifier.Values.wrap(false), solim.modifier.Values.right());

                        column().props(width(unit(4)), prefWidth(unit(4)));

                        text(highlightedCode)
                                .props(style(Styles.monoLabel), solim.modifier.Values.wrap(false), solim.modifier.Values.left());
                    });
                });
            });
        }

        private void actionButtons() {
            String patchData = detail.getData() != null ? detail.getData() : "";
            String patchName = detail.getName() != null ? detail.getName() : "patch";

            row().props(growX(), gap(unit(2))).children(() -> {
                button(Core.bundle.get("browser.patch.copy"),
                        () -> PatchActions.copyTextToClipboard(patchData))
                        .props(style(WebStyles.primary()), growX(), height(unit(11)), prefHeight(unit(11)));

                button(Core.bundle.get("browser.patch.save"),
                        () -> PatchActions.saveTextToFile(patchData, patchName))
                        .props(style(WebStyles.primary()), growX(), height(unit(11)), prefHeight(unit(11)));
            });
        }
    }
}

package mindustrytool.features.browser.schematic;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import mindustry.Vars;
import mindustry.ui.Styles;
import mindustrytool.Config;
import mindustrytool.components.Loader;
import mindustrytool.components.WebStyles;
import mindustrytool.features.browser.common.BrowserFilterDialog;
import mindustrytool.features.browser.common.BrowserFooter;
import mindustrytool.features.browser.common.BrowserLayout;
import mindustrytool.features.browser.common.BrowserSearchHeader;
import mindustrytool.features.browser.common.BrowserState;
import mindustrytool.models.response.SchematicData;
import mindustrytool.services.MindustryTool;
import solim.core.BaseComponent;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import java.util.Collections;

/**
 * Main schematic browser dialog with reactive column reflow and a keyed
 * reactive grid of schematic cards.
 */
public class SchematicBrowserDialog extends SolimDialog {

    private final BrowserState<SchematicData> state;
    private final BrowserFilterDialog filterDialog;

    public SchematicBrowserDialog() {
        super(Core.bundle.get("browser.schematic.title"));

        state = new BrowserState<>(SchematicBrowserDialog::fetchSchematics);
        filterDialog = new BrowserFilterDialog(state, "schematics", true, false);

        closeOnBack();
        fillParent(true);
        children(() -> new BrowserContent(state, filterDialog, this::hide));
        cont().background(Styles.black);
        shown(() -> state.start());
        hidden(() -> state.stop());
    }

    @Override
    protected void onDispose() {
        state.dispose();
    }

    private static CompletableFuture<List<SchematicData>> fetchSchematics(BrowserState<SchematicData> state) {
        Seq<String> blocks = state.selectedBlocks().get();
        List<String> blockList = blocks != null ? blocks.list() : Collections.emptyList();
        return MindustryTool.searchSchematics(
                state.page().peek() != null ? state.page().peek() : 0,
                state.getPageSize(),
                state.sort().peek(),
                state.searchQuery().peek(),
                state.selectedTags().peek().list(),
                blockList,
                null,
                state.verification().peek());
    }

    private static class BrowserContent extends BaseComponent {
        private final BrowserState<SchematicData> state;
        private final BrowserFilterDialog filterDialog;
        private final Runnable onClose;
        private final Computed<Float> viewportWidth = dvw(100f);
        private final Computed<Float> viewportHeight = dvh(100f);
        private final Computed<Integer> columnCount = new Computed<>(() -> {
            Float width = viewportWidth.get();
            return BrowserLayout.calculateColumns(width != null ? width : 800f);
        });
        private final Computed<Float> cardsWidth = new Computed<>(() -> {
            Float width = viewportWidth.get();
            return BrowserLayout.calculateCardsWidth(width != null ? width : 800f);
        });
        private final Computed<Float> contentWidth = new Computed<>(() -> {
            Float width = viewportWidth.get();
            return BrowserLayout.calculateContentWidth(width != null ? width : 800f);
        });
        private final Computed<Float> cardSize = cardsWidth.map(w -> Math.min(BrowserLayout.CARD_SIZE, w));
        private final Computed<Integer> calculatedPageSize = new Computed<>(() -> {
            Float width = viewportWidth.get();
            Float height = viewportHeight.get();
            return BrowserLayout.calculatePageSize(
                    width != null ? width : 800f,
                    height != null ? height : 600f);
        });

        BrowserContent(BrowserState<SchematicData> state, BrowserFilterDialog filterDialog, Runnable onClose) {
            this.state = state;
            this.filterDialog = filterDialog;
            this.onClose = onClose;

            effect(() -> {
                Integer size = calculatedPageSize.get();
                if (size != null) {
                    state.setPageSize(size);
                }
            });
        }

        @Override
        protected Element build() {
            return column().grow().center().paddingX(BrowserLayout.HORIZONTAL_PADDING).paddingY(unit(2))
                    .children(() -> {
                        column().width(contentWidth).growY().gap(unit(2)).children(() -> {
                            new BrowserSearchHeader(state, () -> filterDialog.show());

                            query(state.query())
                                    .loading(Loader::centered)
                                    .error(err -> row().grow().gap(unit(1)).children(() -> {
                                        Throwable cause = err != null && err.getCause() != null ? err.getCause() : err;
                                        String msg = cause != null && cause.getMessage() != null ? cause.getMessage()
                                                : (cause != null ? cause.toString() : "");
                                        text(msg).color(Color.scarlet).wrap(true).growX();
                                        button(Core.bundle.get("browser.retry"), () -> state.refresh())
                                                .style(WebStyles.outlineText())
                                                .height(unit(10));
                                    }))
                                    .data((list, fetching) -> fetching ? Loader.centered()
                                            : scroll().style(Styles.noBarPane).grow()
                                                    .paddingLeft(BrowserLayout.SCROLLBAR_GUTTER).children(() -> {
                                                        reactiveGrid(
                                                                columnCount,
                                                                state.items(),
                                                                SchematicData::getItemId,
                                                                item -> new SchematicCard(
                                                                        item,
                                                                        cardSize,
                                                                        () -> onCardClick(item),
                                                                        () -> SchematicActions
                                                                                .copyToClipboard(item.getItemId()),
                                                                        () -> SchematicActions
                                                                                .saveToLocal(item.getItemId()),
                                                                        () -> showDetails(item)))
                                                                                .empty(() -> {
                                                                                    text(Core.bundle
                                                                                            .get("browser.empty"))
                                                                                                    .color(Color.gray)
                                                                                                    .padding(unit(4));
                                                                                })
                                                                                .gap(BrowserLayout.CARD_GAP);
                                                    }))
                                    .grow();

                            new BrowserFooter(state, Config.UPLOAD_SCHEMATIC_URL, onClose);
                        });
                    }).element();
        }

        private void onCardClick(SchematicData item) {
            if (Vars.state.isMenu()) {
                showDetails(item);
                return;
            }
            if (!SchematicActions.canPlaceInGame()) {
                Vars.ui.showInfo(Core.bundle.get("schematic.disabled"));
                return;
            }
            SchematicActions.placeInGame(item.getItemId());
        }

        private void showDetails(SchematicData item) {
            MindustryTool.findSchematic(item.getItemId()).whenComplete((detail, throwable) -> {
                Core.app.post(() -> {
                    if (throwable == null && detail != null) {
                        new SchematicDetailDialog(detail, item.getItemId()).show();
                    } else {
                        Vars.ui.showErrorMessage(Core.bundle.get("browser.error.load-details"));
                    }
                });
            });
        }
    }
}

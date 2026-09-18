package mindustrytool.features.browser.map;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
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
import mindustrytool.models.response.MapData;
import mindustrytool.services.MindustryTool;
import solim.core.BaseComponent;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Readable;

/**
 * Main map browser dialog with reactive column reflow and a keyed reactive grid
 * of map cards.
 */
public class MapBrowserDialog extends SolimDialog {

    private final BrowserState<MapData> state;
    private final BrowserFilterDialog filterDialog;

    public MapBrowserDialog() {
        super(Core.bundle.get("browser.map.title"));

        state = new BrowserState<>(MapBrowserDialog::fetchMaps);
        filterDialog = new BrowserFilterDialog(state, "maps", false, true);

        closeOnBack();
        fillParent(true);
        children(() -> new BrowserContent(state, filterDialog, this::hide));
        cont().background(Styles.black);
        shown(() -> state.start());
        hidden(() -> state.stop());
    }

    private static CompletableFuture<List<MapData>> fetchMaps(BrowserState<MapData> state) {
        return MindustryTool.searchMaps(
                state.page().peek() != null ? state.page().peek() : 0,
                state.getPageSize(),
                state.sort().peek(),
                state.query().peek(),
                state.selectedTags().peek().list(),
                null,
                state.verification().peek());
    }

    private static class BrowserContent extends BaseComponent {
        private final BrowserState<MapData> state;
        private final BrowserFilterDialog filterDialog;
        private final Runnable onClose;
        private final Computed<Float> viewportWidth = dvw(100f);
        private final Computed<Float> viewportHeight = dvh(100f);
        private final Computed<Integer> columnCount = new Computed<>(() -> {
            Float width = viewportWidth.get();
            return BrowserLayout.calculateColumns(width != null ? width : 800f);
        });
        private final Computed<Float> contentWidth = new Computed<>(() -> {
            Float width = viewportWidth.get();
            return BrowserLayout.calculateContentWidth(width != null ? width : 800f);
        });
        private final Computed<Float> cardSize = contentWidth.map(w -> Math.min(BrowserLayout.CARD_SIZE, w));
        private final Computed<Integer> calculatedPageSize = new Computed<>(() -> {
            Float width = viewportWidth.get();
            Float height = viewportHeight.get();
            return BrowserLayout.calculatePageSize(
                    width != null ? width : 800f,
                    height != null ? height : 600f);
        });

        BrowserContent(BrowserState<MapData> state, BrowserFilterDialog filterDialog, Runnable onClose) {
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
            Readable<Boolean> hasError = state.error().map(e -> e != null && !e.trim().isEmpty());

            return column().grow().center().paddingX(BrowserLayout.HORIZONTAL_PADDING).paddingY(unit(2)).children(() -> {
                column().width(contentWidth).growY().gap(unit(2)).children(() -> {
                    new BrowserSearchHeader(state, () -> filterDialog.show());

                    dynamic(state.loading(), loading -> {
                        if (Boolean.TRUE.equals(loading)) {
                            return Loader.centered();
                        }

                        return dynamic(hasError, errorOccurred -> {
                            if (Boolean.TRUE.equals(errorOccurred)) {
                                return row().grow().gap(unit(1)).children(() -> {
                                    text(state.error().map(e -> e != null ? e : ""))
                                            .color(Color.scarlet)
                                            .wrap(true)
                                            .growX();
                                    button(Core.bundle.get("browser.retry"), () -> state.refresh())
                                            .style(WebStyles.outlineText())
                                            .height(unit(10));
                                });
                            }

                            return scroll().grow().children(() -> {
                                reactiveGrid(
                                        columnCount,
                                        state.items(),
                                        MapData::getItemId,
                                        item -> new MapCard(
                                                item,
                                                cardSize,
                                                () -> showDetails(item),
                                                () -> MapActions.downloadAndImport(item.getItemId()),
                                                () -> showDetails(item),
                                                () -> MapActions.playMap(item.getItemId())))
                                                        .empty(() -> {
                                                            text(Core.bundle.get("browser.empty")).color(Color.gray)
                                                                    .padding(unit(4));
                                                        })
                                                        .gap(BrowserLayout.CARD_GAP);
                            });
                        }).grow();
                    }).grow();

                    new BrowserFooter(state, Config.UPLOAD_MAP_URL, onClose);
                });
            }).element();
        }

        private void showDetails(MapData item) {
            MindustryTool.findMap(item.getItemId()).whenComplete((detail, throwable) -> {
                Core.app.post(() -> {
                    if (throwable == null && detail != null) {
                        new MapDetailDialog(detail, item.getItemId()).show();
                    } else {
                        Vars.ui.showErrorMessage(Core.bundle.get("browser.error.load-details"));
                    }
                });
            });
        }
    }
}

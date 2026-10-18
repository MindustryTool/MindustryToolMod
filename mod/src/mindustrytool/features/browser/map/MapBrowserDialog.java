package mindustrytool.features.browser.map;

import static solim.UI.*;
import static solim.modifier.Values.center;
import static solim.modifier.Values.color;
import static solim.modifier.Values.fillParent;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growY;
import static solim.modifier.Values.height;
import static solim.modifier.Values.paddingLeft;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.paddingY;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.style;
import static solim.modifier.Values.width;

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
import mindustrytool.models.response.MapData;
import mindustrytool.services.MindustryTool;
import solim.core.Units;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Signal;

public class MapBrowserDialog extends SolimDialog {

    private final BrowserState<MapData> state;
    private final BrowserFilterDialog filterDialog;

    public MapBrowserDialog() {
        super(Core.bundle.get("browser.map.title"));

        state = new BrowserState<>(MapBrowserDialog::fetchMaps);
        filterDialog = new BrowserFilterDialog(state, "maps", false, true);

        closeOnBack();
        props(fillParent(true));
        children(() -> new BrowserContent(state, filterDialog, this::hide));
        cont().background(Styles.black);
        shown(() -> state.start());
        hidden(() -> state.stop());
    }

    @Override
    protected void onDispose() {
        state.dispose();
    }

    private static CompletableFuture<List<MapData>> fetchMaps(BrowserState<MapData> state) {
        return MindustryTool.searchMaps(
                state.page().peek() != null ? state.page().peek() : 0,
                state.getPageSize(),
                state.sort().peek(),
                state.searchQuery().peek(),
                state.selectedTags().peek().list(),
                null,
                state.verification().peek());
    }

    private static class BrowserContent extends Component {
        private final BrowserState<MapData> state;
        private final BrowserFilterDialog filterDialog;
        private final Runnable onClose;

        BrowserContent(BrowserState<MapData> state, BrowserFilterDialog filterDialog, Runnable onClose) {
            this.state = state;
            this.filterDialog = filterDialog;
            this.onClose = onClose;
        }

        @Override
        protected Element build() {

            Signal<Float> viewportWidth = Units.dvw;
            Signal<Float> viewportHeight = Units.dvh;
            Computed<Integer> columnCount = new Computed<>(() -> {
                Float width = viewportWidth.get();
                return BrowserLayout.calculateColumns(width != null ? width : 800f);
            });
            Computed<Float> cardsWidth = new Computed<>(() -> {
                Float width = viewportWidth.get();
                return BrowserLayout.calculateCardsWidth(width != null ? width : 800f);
            });
            Computed<Float> contentWidth = new Computed<>(() -> {
                Float width = viewportWidth.get();
                return BrowserLayout.calculateContentWidth(width != null ? width : 800f);
            });
            Computed<Float> cardSize = cardsWidth.map(w -> Math.min(BrowserLayout.CARD_SIZE, w));
            Computed<Integer> calculatedPageSize = new Computed<>(() -> {
                Float width = viewportWidth.get();
                Float height = viewportHeight.get();
                return BrowserLayout.calculatePageSize(
                        width != null ? width : 800f,
                        height != null ? height : 600f);
            });

            Signal<Integer> visibleCount = Signal.of(BrowserLayout.RENDER_CHUNK_SIZE);
            Computed<Seq<MapData>> visibleItems = new Computed<>(() -> {
                Seq<MapData> all = state.items().get();
                Integer limit = visibleCount.get();
                return BrowserState.firstItems(all, limit != null ? limit : 0);
            });

            effect(() -> {
                Integer size = calculatedPageSize.get();
                if (size != null) {
                    state.setPageSize(size);
                }
            });

            effect(() -> {
                Seq<MapData> all = state.items().get();
                int total = all != null ? all.size : 0;
                Integer page = state.page().get();
                visibleCount.set(Math.min(BrowserLayout.RENDER_CHUNK_SIZE, total));
                expandVisible(visibleCount, total, page != null ? page : 0);
            });

            return column().props(grow(), paddingX(BrowserLayout.HORIZONTAL_PADDING), paddingY(unit(2)), center())
                    .children(() -> {
                        column().props(width(contentWidth), prefWidth(contentWidth), growY(), gap(unit(2))).children(() -> {
                            new BrowserSearchHeader(state, () -> filterDialog.show());

                            query(state.query())
                                    .props(grow())
                                    .loading(Loader::centered)
                                    .error(err -> row().props(grow(), gap(unit(1))).children(() -> {
                                        Throwable cause = err != null && err.getCause() != null ? err.getCause() : err;
                                        String msg = cause != null && cause.getMessage() != null ? cause.getMessage()
                                                : (cause != null ? cause.toString() : "");
                                         text(msg).props(color(Color.scarlet), solim.modifier.Values.wrap(true), solim.modifier.Values.growX());
                                        button(Core.bundle.get("browser.retry"), () -> state.refresh())
                                                .props(style(WebStyles.outlineText()), height(unit(10)), prefHeight(unit(10)));
                                    }))
                                    .data((list, fecthing) -> fecthing ? Loader.centered()
                                            : scroll().props(style(Styles.noBarPane), grow(), paddingLeft(BrowserLayout.SCROLLBAR_GUTTER)).children(() -> {
                                                        reactiveGrid(visibleItems).columns(columnCount)
                                                                .key(MapData::getItemId)
                                                                .props(gap(BrowserLayout.CARD_GAP))
                                                                .empty(() -> {
                                                                    text(Core.bundle
                                                                            .get("browser.empty"))
                                                                            .props(color(Color.gray), solim.modifier.Values.margin(unit(4)));
                                                                })
                                                                .items(item -> new MapCard(
                                                                        item,
                                                                        cardSize,
                                                                        () -> showDetails(item),
                                                                        () -> MapActions
                                                                                .downloadAndImport(item.getItemId()),
                                                                        () -> showDetails(item),
                                                                        () -> MapActions.playMap(item.getItemId())));
                                                    }))
                                    .props(grow());

                            new BrowserFooter(state, Config.UPLOAD_MAP_URL, onClose);
                        });
                    }).element();
        }

        private void expandVisible(Signal<Integer> visibleCount, int total, int page) {
            Integer current = visibleCount.peek();
            if (current == null || current >= total) {
                return;
            }
            Core.app.post(() -> {
                if (isDisposed()) {
                    return;
                }
                Integer currentPage = state.page().peek();
                if (currentPage == null || currentPage != page) {
                    return;
                }
                Integer shown = visibleCount.peek();
                int next = Math.min(total, (shown != null ? shown : 0) + BrowserLayout.RENDER_CHUNK_SIZE);
                visibleCount.set(next);
                expandVisible(visibleCount, total, page);
            });
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

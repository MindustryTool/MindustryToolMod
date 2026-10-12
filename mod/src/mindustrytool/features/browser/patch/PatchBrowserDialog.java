package mindustrytool.features.browser.patch;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import java.util.Collections;
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
import mindustrytool.models.response.ContentPatchData;
import mindustrytool.services.MindustryTool;
import solim.core.Component;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Signal;

/**
 * Main content patch browser dialog with reactive column reflow and a keyed
 * reactive grid of content patch cards.
 */
public class PatchBrowserDialog extends SolimDialog {

    private final BrowserState<ContentPatchData> state;
    private final BrowserFilterDialog filterDialog;

    public PatchBrowserDialog() {
        super(Core.bundle.get("browser.patch.title"));

        state = new BrowserState<>(PatchBrowserDialog::fetchPatches);
        filterDialog = new BrowserFilterDialog(state, "content-patches", false, false);

        closeOnBack();
        fillParent(true);
        children(() -> new BrowserContent(this, state, filterDialog, this::hide));
        cont().background(Styles.black);
        shown(() -> state.start());
        hidden(() -> state.stop());
    }

    @Override
    protected void onDispose() {
        state.dispose();
    }

    private static CompletableFuture<List<ContentPatchData>> fetchPatches(BrowserState<ContentPatchData> state) {
        return MindustryTool.searchContentPatches(
                state.page().peek() != null ? state.page().peek() : 0,
                state.getPageSize(),
                state.sort().peek(),
                state.searchQuery().peek(),
                state.selectedTags().peek() != null ? state.selectedTags().peek().list() : Collections.emptyList(),
                null,
                state.verification().peek());
    }

    private static class BrowserContent extends Component {
        private final BrowserState<ContentPatchData> state;
        private final BrowserFilterDialog filterDialog;
        private final Runnable onClose;

        BrowserContent(PatchBrowserDialog dialog, BrowserState<ContentPatchData> state,
                BrowserFilterDialog filterDialog, Runnable onClose) {
            this.state = state;
            this.filterDialog = filterDialog;
            this.onClose = onClose;
        }

        @Override
        protected Element build() {
            // Declared here rather than in field initialisers: inside build() this component is
            // the active ownership scope, so the derived values live and die with the view.
            Computed<Float> viewportWidth = dvw(100f);
            Computed<Float> viewportHeight = dvh(100f);

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
            Computed<Seq<ContentPatchData>> visibleItems = new Computed<>(() -> {
                Seq<ContentPatchData> all = state.items().get();
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
                Seq<ContentPatchData> all = state.items().get();
                int total = all != null ? all.size : 0;
                Integer page = state.page().get();
                visibleCount.set(Math.min(BrowserLayout.RENDER_CHUNK_SIZE, total));
                expandVisible(visibleCount, total, page != null ? page : 0);
            });

            return column().grow().center().paddingX(BrowserLayout.HORIZONTAL_PADDING).paddingY(unit(2))
                    .children(() -> {
                        column().width(contentWidth).growY().gap(unit(2)).children(() -> {
                            new BrowserSearchHeader(state, () -> filterDialog.show());
                            body(columnCount, cardSize, visibleItems);
                            new BrowserFooter(state, Config.UPLOAD_PATCH_URL, onClose);
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

        private void body(Computed<Integer> columnCount, Computed<Float> cardSize,
                Computed<Seq<ContentPatchData>> visibleItems) {
            query(state.query())
                    .grow()
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
                    .data((list, fetching) -> fetching ? Loader.centered() : items(columnCount, cardSize, visibleItems))
                    .grow();
        }

        private Component items(Computed<Integer> columnCount, Computed<Float> cardSize,
                Computed<Seq<ContentPatchData>> visibleItems) {
            return scroll().style(Styles.noBarPane).grow().paddingLeft(BrowserLayout.SCROLLBAR_GUTTER)
                    .children(() -> {
                        reactiveGrid(visibleItems)
                                .columns(columnCount)
                                .key(ContentPatchData::getItemId)
                                .empty(() -> text(Core.bundle.get("browser.empty")).color(Color.gray)
                                        .padding(unit(4)))
                                .gap(BrowserLayout.CARD_GAP)
                                .children(item -> new PatchCard(item, cardSize,
                                        () -> showDetails(item),
                                        () -> PatchActions.copyToClipboard(item.getItemId()),
                                        () -> PatchActions.saveToFile(item.getItemId(), item.getName()),
                                        () -> showDetails(item)));
                    });
        }

        private void showDetails(ContentPatchData item) {
            MindustryTool.findContentPatch(item.getItemId()).whenComplete((detail, throwable) -> {
                Core.app.post(() -> {
                    if (throwable == null && detail != null) {
                        new PatchDetailDialog(detail, item.getItemId()).show();
                    } else {
                        Vars.ui.showErrorMessage(Core.bundle.get("browser.error.load-details"));
                    }
                });
            });
        }
    }
}

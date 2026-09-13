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
import mindustrytool.features.browser.common.BrowserFilterDialog;
import mindustrytool.features.browser.common.BrowserFooter;
import mindustrytool.features.browser.common.BrowserSearchHeader;
import mindustrytool.features.browser.common.BrowserState;
import mindustrytool.features.browser.common.WebStyles;
import mindustrytool.models.response.MapData;
import mindustrytool.services.MindustryTool;
import solim.core.BaseComponent;
import solim.overlay.SolimDialog;
import solim.signal.Computed;
import solim.signal.Readable;

/**
 * Main map browser dialog with reactive column reflow and a keyed reactive
 * grid of map cards.
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

    private static CompletableFuture<List<MapData>> fetchMaps(
            int page, int size, String sort, String query, List<String> tags) {
        return MindustryTool.searchMaps(page, size, sort, query, tags);
    }

    private static class BrowserContent extends BaseComponent {
        private final BrowserState<MapData> state;
        private final BrowserFilterDialog filterDialog;
        private final Runnable onClose;
        private final Computed<Float> viewportWidth = dvw(100f);
        private final Readable<Boolean> portrait = isPortrait();
        private final Computed<Integer> columnCount = new Computed<>(() -> {
            Float width = viewportWidth.get();
            float w = width != null ? width : 800f;
            if (Boolean.TRUE.equals(portrait.get())) {
                return w < 500f ? 1 : 2;
            }
            if (w < 650f) {
                return 2;
            }
            if (w < 1100f) {
                return 3;
            }
            return Math.max(3, Math.min(6, (int) (w / 300f)));
        });

        BrowserContent(BrowserState<MapData> state, BrowserFilterDialog filterDialog, Runnable onClose) {
            this.state = state;
            this.filterDialog = filterDialog;
            this.onClose = onClose;
        }

        @Override
        protected Element build() {
            return column().grow().padding(unit(2)).gap(unit(2)).children(() -> {
                new BrowserSearchHeader(state, () -> filterDialog.show());

                dynamic(state.loading(), loading -> {
                    if (!Boolean.TRUE.equals(loading)) {
                        return null;
                    }
                    return row().growX().children(() -> {
                        text(Core.bundle.get("browser.loading")).color(Color.lightGray);
                    });
                });

                dynamic(state.error(), message -> {
                    if (message == null || message.isEmpty()) {
                        return null;
                    }
                    return row().growX().gap(unit(1)).children(() -> {
                        text(message)
                                .color(Color.scarlet)
                                .wrap(true)
                                .growX();
                        button(Core.bundle.get("browser.retry"), () -> state.refresh())
                                .style(WebStyles.outlineText())
                                .height(unit(10));
                    });
                });

                scroll().grow().children(() -> {
                    reactiveGrid(
                            columnCount,
                            state.items(),
                            MapData::getItemId,
                            item -> new MapCard(
                                    item,
                                    () -> showDetails(item),
                                    () -> MapActions.downloadAndImport(item.getItemId()),
                                    () -> showDetails(item),
                                    () -> MapActions.playMap(item.getItemId())))
                            .empty(() -> {
                                text(Core.bundle.get("browser.empty")).color(Color.gray).padding(unit(4));
                            })
                            .gap(unit(2));
                });

                new BrowserFooter(state, Config.UPLOAD_MAP_URL, onClose);
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

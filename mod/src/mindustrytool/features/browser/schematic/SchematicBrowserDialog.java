package mindustrytool.features.browser.schematic;

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
import mindustrytool.features.browser.common.BrowserSearchHeader;
import mindustrytool.features.browser.common.BrowserState;
import mindustrytool.models.response.SchematicData;
import mindustrytool.services.MindustryTool;
import solim.core.BaseComponent;
import solim.overlay.SolimDialog;
import solim.signal.Computed;
import solim.signal.Readable;

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

    private static CompletableFuture<List<SchematicData>> fetchSchematics(
            int page, int size, String sort, String query, List<String> tags) {
        return MindustryTool.searchSchematics(page, size, sort, query, tags);
    }

    private static class BrowserContent extends BaseComponent {
        private final BrowserState<SchematicData> state;
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

        BrowserContent(BrowserState<SchematicData> state, BrowserFilterDialog filterDialog, Runnable onClose) {
            this.state = state;
            this.filterDialog = filterDialog;
            this.onClose = onClose;
        }

        @Override
        protected Element build() {
            Readable<Boolean> hasError = state.error().map(e -> e != null && !e.trim().isEmpty());

            return column().grow().padding(unit(2)).gap(unit(2)).children(() -> {
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
                                    SchematicData::getItemId,
                                    (item, ctx) -> new SchematicCard(
                                            item,
                                            ctx.itemWidth(),
                                            () -> onCardClick(item),
                                            () -> SchematicActions.copyToClipboard(item.getItemId()),
                                            () -> SchematicActions.saveToLocal(item.getItemId()),
                                            () -> showDetails(item)))
                                                    .empty(() -> {
                                                        text(Core.bundle.get("browser.empty")).color(Color.gray)
                                                                .padding(unit(4));
                                                    })
                                                    .gap(unit(4));
                        });
                    }).grow();
                }).grow();

                new BrowserFooter(state, Config.UPLOAD_SCHEMATIC_URL, onClose);
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

package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import mindustry.game.EventType.ResizeEvent;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.Hud;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Floating frameless draggable HUD rendering the configured schematic grid with
 * a dedicated page tab column, subtle empty slot tiles, and screen clamping.
 */
public class QuickSchematicGridHudView extends BaseComponent {

    public static final class SlotModel {
        public final int page;
        public final int row;
        public final int col;
        public final @Nullable QuickSchematicEntry entry;

        public SlotModel(int page, int row, int col, @Nullable QuickSchematicEntry entry) {
            this.page = page;
            this.row = row;
            this.col = col;
            this.entry = entry;
        }

        public String key() {
            return page + ":" + row + ":" + col + ":" + (entry != null && entry.id != null ? entry.id : "empty");
        }
    }

    private final QuickSchematicGridFeature feature;
    private @Nullable Hud hud;

    public QuickSchematicGridHudView(QuickSchematicGridFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Float> buttonSize = feature.buttonSizeConfig.signal();

        hud = hud(() -> buildFullLayout(feature, null, true));
        hud.position(feature.xSignal, feature.ySignal);

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });

        effect(() -> {
            buttonSize.get();
            feature.rowsConfig.signal().get();
            feature.colsConfig.signal().get();
            feature.pageCountConfig.signal().get();
            feature.activePage.get();
            Core.app.post(this::keepInScreen);
        });

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    public static Component buildFullLayout(
            QuickSchematicGridFeature feature,
            @Nullable Runnable onBeforeActivate,
            boolean includeDragHandle) {
        Readable<Float> buttonSize = feature.buttonSizeConfig.signal();
        Readable<Float> dragIconSize = buttonSize.map(s -> (s != null ? s : 48f) * 0.45f);
        Readable<Float> gap = feature.buttonGapConfig.signal();

        Readable<List<Integer>> pagesList = Signal.computed(() -> {
            Integer count = feature.pageCountConfig.signal().get();
            feature.pageIcons().get();
            int total = Math.max(1, Math.min(QuickSchematicGridFeature.MAX_PAGES, count != null ? count : 1));
            List<Integer> list = new ArrayList<>(total);
            for (int i = 0; i < total; i++) {
                list.add(i);
            }
            return list;
        });

        return row().gap(gap).top().children(() -> {
            // Column 1: Tabs & Drag Handle
            column().gap(gap).top().children(() -> {
                if (includeDragHandle) {
                    dynamic(feature.hideDragHandleConfig.signal(), hide -> {
                        if (!Boolean.TRUE.equals(hide)) {
                            return button()
                                    .style(Styles.clearNonei)
                                    .background(Styles.black6)
                                    .size(buttonSize)
                                    .children(() -> icon(Icon.move).size(dragIconSize))
                                    .draggable(feature.xSignal, feature.ySignal);
                        }
                        return null;
                    });
                }

                reactiveGrid(
                        Signal.of(1),
                        pagesList,
                        pageIndex -> pageIndex + ":" + (feature.getPageIcon(pageIndex) != null ? feature.getPageIcon(pageIndex) : ""),
                        pageIndex -> pageTabButton(feature, pageIndex, buttonSize))
                        .gap(gap);
            });

            // Column 2: Schematic Grid for Active Page
            buildGrid(feature, onBeforeActivate);
        });
    }

    public static Component buildGrid(QuickSchematicGridFeature feature, @Nullable Runnable onBeforeActivate) {
        Readable<Float> gap = feature.buttonGapConfig.signal();

        Readable<List<SlotModel>> slots = Signal.computed(() -> {
            int p = feature.getActivePage();
            int rows = Math.max(1, Math.min(QuickSchematicGridFeature.MAX_ROWS, feature.rowsConfig.signal().get()));
            int cols = Math.max(1, Math.min(QuickSchematicGridFeature.MAX_COLS, feature.colsConfig.signal().get()));
            List<QuickSchematicEntry> allEntries = feature.entries().get();

            List<SlotModel> list = new ArrayList<>(rows * cols);
            for (int r = 0; r < rows; r++) {
                for (int c = 0; c < cols; c++) {
                    QuickSchematicEntry found = null;
                    if (allEntries != null) {
                        for (QuickSchematicEntry entry : allEntries) {
                            if (entry != null && entry.page == p && entry.row == r && entry.col == c) {
                                found = entry;
                                break;
                            }
                        }
                    }
                    list.add(new SlotModel(p, r, c, found));
                }
            }
            return list;
        });

        return reactiveGrid(
                feature.colsConfig.signal(),
                slots,
                SlotModel::key,
                slot -> slotComponent(feature, slot, onBeforeActivate))
                .gap(gap);
    }

    static Component pageTabButton(
            QuickSchematicGridFeature feature,
            int pageIndex,
            Readable<Float> buttonSize) {
        String icon = feature.getPageIcon(pageIndex);
        String label = icon != null && !icon.trim().isEmpty() ? icon : String.valueOf(pageIndex + 1);
        String tooltip = Core.bundle.format("feature.quick-schematic-grid.settings.page-tab", pageIndex + 1);

        return button(label, () -> feature.setActivePage(pageIndex))
                .style(WebStyles.filterChipText())
                .size(buttonSize)
                .tooltip(tooltip)
                .checked(feature.activePage.map(p -> p != null && p.intValue() == pageIndex));
    }

    static Component slotComponent(
            QuickSchematicGridFeature feature,
            SlotModel slot,
            @Nullable Runnable onBeforeActivate) {
        if (slot.entry == null) {
            return emptySlot(feature);
        }
        return schematicButton(feature, slot.entry, onBeforeActivate);
    }

    static Component emptySlot(QuickSchematicGridFeature feature) {
        Readable<Float> buttonSize = feature.buttonSizeConfig.signal();
        return button()
                .style(Styles.clearNonei)
                .background(Styles.black3)
                .size(buttonSize);
    }

    static Component schematicButton(
            QuickSchematicGridFeature feature,
            @Nullable QuickSchematicEntry entry,
            @Nullable Runnable onBeforeActivate) {
        if (entry == null) {
            return emptySlot(feature);
        }

        Readable<Float> buttonSize = feature.buttonSizeConfig.signal();
        Schematic schematic = feature.resolveSchematic(entry);

        if (schematic == null) {
            String missingLabel = entry.displayName() != null && !entry.displayName().trim().isEmpty()
                    ? entry.displayName()
                    : entry.schematicName;

            return button(() -> {
                if (onBeforeActivate != null) {
                    onBeforeActivate.run();
                }
                feature.useEntry(entry);
            })
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.format("feature.quick-schematic-grid.tooltip.missing",
                            missingLabel != null ? missingLabel : ""))
                    .children(() -> icon(Icon.warning).size(buttonSize.map(s -> (s != null ? s : 48f) * 0.5f))
                            .color(Color.scarlet));
        }

        String tooltip = Core.bundle.format("feature.quick-schematic-grid.tooltip.use", entry.displayName());
        Runnable activate = () -> {
            if (onBeforeActivate != null) {
                onBeforeActivate.run();
            }
            feature.useSchematic(schematic);
        };

        if (entry.hasCustomIcon()) {
            String glyph = entry.customIcon;

            return button(activate)
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(tooltip)
                    .children(() -> text(glyph != null ? glyph : "")
                            .fontScale(buttonSize.map(s -> (s != null ? s : 48f) / 32f))
                            .center());
        }

        return button(activate)
                .size(buttonSize)
                .background(Styles.black9)
                .tooltip(tooltip)
                .children(() -> new BoundedSchematicImage(schematic, buttonSize, 48f));
    }

    public @Nullable Hud getHud() {
        return hud;
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}

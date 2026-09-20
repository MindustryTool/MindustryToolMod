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
    //TODO: Right click to open edit UI
    //TODO: Hover over button show a shadow of schematic
    //TODO: Icon not render properly (it display schematic preview instead)
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
            feature.pagePositionConfig.signal().get();
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
        Readable<Float> buttonOpacity = feature.buttonOpacityConfig.signal();

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

        Readable<Integer> tabColumns = Signal.computed(() -> {
            String pos = feature.pagePositionConfig.signal().get();
            boolean horizontal = QuickSchematicGridFeature.PAGE_TOP.equals(pos)
                    || QuickSchematicGridFeature.PAGE_BOTTOM.equals(pos);
            if (!horizontal) {
                return 1;
            }
            List<Integer> pages = pagesList.get();
            int total = pages != null ? pages.size() : 1;
            return Math.max(1, total);
        });

        return dynamic(feature.pagePositionConfig.signal(), pos -> {
            String position = QuickSchematicGridFeature.PAGE_RIGHT.equals(pos)
                    || QuickSchematicGridFeature.PAGE_TOP.equals(pos)
                    || QuickSchematicGridFeature.PAGE_BOTTOM.equals(pos)
                            ? pos
                            : QuickSchematicGridFeature.PAGE_LEFT;
            boolean horizontal = QuickSchematicGridFeature.PAGE_TOP.equals(position)
                    || QuickSchematicGridFeature.PAGE_BOTTOM.equals(position);

            if (horizontal) {
                boolean tabsOnTop = QuickSchematicGridFeature.PAGE_TOP.equals(position);
                return column().gap(gap).top().children(() -> {
                    if (tabsOnTop) {
                        buildTabsRow(feature, buttonSize, dragIconSize, gap, buttonOpacity, pagesList, tabColumns,
                                includeDragHandle);
                    }
                    buildGrid(feature, onBeforeActivate);
                    if (!tabsOnTop) {
                        buildTabsRow(feature, buttonSize, dragIconSize, gap, buttonOpacity, pagesList, tabColumns,
                                includeDragHandle);
                    }
                });
            }

            boolean tabsOnLeft = QuickSchematicGridFeature.PAGE_LEFT.equals(position);
            return row().gap(gap).top().children(() -> {
                if (tabsOnLeft) {
                    buildTabsColumn(feature, buttonSize, dragIconSize, gap, buttonOpacity, pagesList,
                            includeDragHandle);
                }
                buildGrid(feature, onBeforeActivate);
                if (!tabsOnLeft) {
                    buildTabsColumn(feature, buttonSize, dragIconSize, gap, buttonOpacity, pagesList,
                            includeDragHandle);
                }
            });
        });
    }

    static Component buildTabsColumn(
            QuickSchematicGridFeature feature,
            Readable<Float> buttonSize,
            Readable<Float> dragIconSize,
            Readable<Float> gap,
            Readable<Float> buttonOpacity,
            Readable<List<Integer>> pagesList,
            boolean includeDragHandle) {
        return column().gap(gap).top().children(() -> {
            if (includeDragHandle) {
                buildDragHandle(feature, buttonSize, dragIconSize, buttonOpacity);
            }
            reactiveGrid(
                    Signal.of(1),
                    pagesList,
                    pageIndex -> pageIndex + ":" + (feature.getPageIcon(pageIndex) != null ? feature.getPageIcon(pageIndex) : ""),
                    pageIndex -> pageTabButton(feature, pageIndex, buttonSize, buttonOpacity))
                    .gap(gap);
        });
    }

    static Component buildTabsRow(
            QuickSchematicGridFeature feature,
            Readable<Float> buttonSize,
            Readable<Float> dragIconSize,
            Readable<Float> gap,
            Readable<Float> buttonOpacity,
            Readable<List<Integer>> pagesList,
            Readable<Integer> tabColumns,
            boolean includeDragHandle) {
        return row().gap(gap).top().left().children(() -> {
            if (includeDragHandle) {
                buildDragHandle(feature, buttonSize, dragIconSize, buttonOpacity);
            }
            reactiveGrid(
                    tabColumns,
                    pagesList,
                    pageIndex -> pageIndex + ":" + (feature.getPageIcon(pageIndex) != null ? feature.getPageIcon(pageIndex) : ""),
                    pageIndex -> pageTabButton(feature, pageIndex, buttonSize, buttonOpacity))
                    .gap(gap);
        });
    }

    static Component buildDragHandle(
            QuickSchematicGridFeature feature,
            Readable<Float> buttonSize,
            Readable<Float> dragIconSize,
            Readable<Float> buttonOpacity) {
        return dynamic(feature.hideDragHandleConfig.signal(), hide -> {
            if (!Boolean.TRUE.equals(hide)) {
                return button()
                        .style(Styles.clearNonei)
                        .background(Styles.black6)
                        .size(buttonSize)
                        .opacity(buttonOpacity)
                        .children(() -> icon(Icon.move).size(dragIconSize))
                        .draggable(feature.xSignal, feature.ySignal);
            }
            return null;
        });
    }

    public static Component buildGrid(QuickSchematicGridFeature feature, @Nullable Runnable onBeforeActivate) {
        Readable<Float> gap = feature.buttonGapConfig.signal();
        Readable<Float> buttonOpacity = feature.buttonOpacityConfig.signal();

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
                slot -> slotComponent(feature, slot, onBeforeActivate, buttonOpacity))
                .gap(gap);
    }

    static Component pageTabButton(
            QuickSchematicGridFeature feature,
            int pageIndex,
            Readable<Float> buttonSize,
            Readable<Float> buttonOpacity) {
        String icon = feature.getPageIcon(pageIndex);
        String label = icon != null && !icon.trim().isEmpty() ? icon : String.valueOf(pageIndex + 1);
        String tooltip = Core.bundle.format("feature.quick-schematic-grid.settings.page-tab", pageIndex + 1);

        return button(label, () -> feature.setActivePage(pageIndex))
                .style(WebStyles.filterChipText())
                .size(buttonSize)
                .opacity(buttonOpacity)
                .tooltip(tooltip)
                .checked(feature.activePage.map(p -> p != null && p.intValue() == pageIndex));
    }

    static Component pageTabButton(
            QuickSchematicGridFeature feature,
            int pageIndex,
            Readable<Float> buttonSize) {
        return pageTabButton(feature, pageIndex, buttonSize, feature.buttonOpacityConfig.signal());
    }

    static Component slotComponent(
            QuickSchematicGridFeature feature,
            SlotModel slot,
            @Nullable Runnable onBeforeActivate,
            Readable<Float> buttonOpacity) {
        if (slot.entry == null) {
            return emptySlot(feature, buttonOpacity);
        }
        return schematicButton(feature, slot.entry, onBeforeActivate, buttonOpacity);
    }

    static Component slotComponent(
            QuickSchematicGridFeature feature,
            SlotModel slot,
            @Nullable Runnable onBeforeActivate) {
        return slotComponent(feature, slot, onBeforeActivate, feature.buttonOpacityConfig.signal());
    }

    static Component emptySlot(QuickSchematicGridFeature feature, Readable<Float> buttonOpacity) {
        Readable<Float> buttonSize = feature.buttonSizeConfig.signal();
        return button()
                .style(Styles.clearNonei)
                .background(Styles.black3)
                .size(buttonSize)
                .opacity(buttonOpacity);
    }

    static Component emptySlot(QuickSchematicGridFeature feature) {
        return emptySlot(feature, feature.buttonOpacityConfig.signal());
    }

    static Component schematicButton(
            QuickSchematicGridFeature feature,
            @Nullable QuickSchematicEntry entry,
            @Nullable Runnable onBeforeActivate,
            Readable<Float> buttonOpacity) {
        if (entry == null) {
            return emptySlot(feature, buttonOpacity);
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
                    .opacity(buttonOpacity)
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
                    .opacity(buttonOpacity)
                    .tooltip(tooltip)
                    .children(() -> text(glyph != null ? glyph : "")
                            .fontScale(buttonSize.map(s -> (s != null ? s : 48f) / 32f))
                            .center());
        }

        return button(activate)
                .size(buttonSize)
                .background(Styles.black9)
                .opacity(buttonOpacity)
                .tooltip(tooltip)
                .children(() -> new BoundedSchematicImage(schematic, buttonSize, 48f));
    }

    static Component schematicButton(
            QuickSchematicGridFeature feature,
            @Nullable QuickSchematicEntry entry,
            @Nullable Runnable onBeforeActivate) {
        return schematicButton(feature, entry, onBeforeActivate, feature.buttonOpacityConfig.signal());
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

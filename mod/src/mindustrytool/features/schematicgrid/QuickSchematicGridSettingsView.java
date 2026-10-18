package mindustrytool.features.schematicgrid;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.minWidth;
import static solim.modifier.Values.paddingX;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.enabled;
import static solim.modifier.Values.checked;
import static solim.modifier.Values.color;
import static solim.modifier.Values.scrollX;
import static solim.modifier.Values.scrollY;
import static solim.modifier.Values.tooltip;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import java.util.ArrayList;
import java.util.List;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import solim.core.Component;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class QuickSchematicGridSettingsView extends Component {
    private final QuickSchematicGridFeature feature;

    public QuickSchematicGridSettingsView(QuickSchematicGridFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            layoutCard();
            pagesCard();
            gridEditorCard();
            actionsCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "HUD OVERLAY");
    }

    private Component layoutCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("team-resources.settings.section.layout", "Layout"),
                () -> {

                    row().props(growX(), gap(unit(2)), center()).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.display-mode"))
                                .props(color(Color.white), solim.modifier.Values.left());

                        spacer();
                        row().props(gap(unit(1.5f))).children(() -> {
                            button(Core.bundle.get("feature.quick-schematic-grid.settings.display-mode.hud"),
                                    () -> feature.displayModeConfig.set(QuickSchematicGridFeature.DISPLAY_HUD))
                                    .props(style(WebStyles.filterChipText()), checked(feature.displayModeConfig.signal()
                                            .map(QuickSchematicGridFeature.DISPLAY_HUD::equals)), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2.5f)));

                            button(Core.bundle.get("feature.quick-schematic-grid.settings.display-mode.popup"),
                                    () -> feature.displayModeConfig.set(QuickSchematicGridFeature.DISPLAY_POPUP))
                                    .props(style(WebStyles.filterChipText()), checked(feature.displayModeConfig.signal()
                                            .map(QuickSchematicGridFeature.DISPLAY_POPUP::equals)), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2.5f)));
                        });
                    });

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    SettingSlider.of(
                            Core.bundle.get("feature.quick-schematic-grid.settings.rows"),
                            feature.rowsConfig.signal(),
                            QuickSchematicGridFeature.MIN_ROWS,
                            QuickSchematicGridFeature.MAX_ROWS,
                            1,
                            feature.rowsConfig.signal().map(String::valueOf));

                    SettingSlider.of(
                            Core.bundle.get("feature.quick-schematic-grid.settings.cols"),
                            feature.colsConfig.signal(),
                            QuickSchematicGridFeature.MIN_COLS,
                            QuickSchematicGridFeature.MAX_COLS,
                            1,
                            feature.colsConfig.signal().map(String::valueOf));

                    SettingSlider.of(
                            Core.bundle.get("feature.quick-schematic-grid.settings.button-size"),
                            feature.buttonSizeConfig.signal(),
                            32f,
                            72f,
                            4f,
                            feature.buttonSizeConfig.signal()
                                    .map(v -> String.valueOf(Math.round(v != null ? v : 48f))));

                    SettingSlider.of(
                            Core.bundle.get("feature.quick-schematic-grid.settings.button-gap"),
                            feature.buttonGapConfig.signal(),
                            QuickSchematicGridFeature.MIN_GAP,
                            QuickSchematicGridFeature.MAX_GAP,
                            1f,
                            feature.buttonGapConfig.signal().map(v -> String.valueOf(Math.round(v != null ? v : 4f))));

                    SettingSlider.of(
                            Core.bundle.get("feature.quick-schematic-grid.settings.button-opacity"),
                            feature.buttonOpacityConfig.signal(),
                            QuickSchematicGridFeature.MIN_BUTTON_OPACITY,
                            QuickSchematicGridFeature.MAX_BUTTON_OPACITY,
                            0.05f,
                            feature.buttonOpacityConfig.signal()
                                    .map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    column().props(growX(), gap(unit(1.5f)), left()).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.page-position"))
                                .props(color(Color.white), solim.modifier.Values.left());

                        row().props(gap(unit(1.5f))).children(() -> {
                            button(Core.bundle.get("feature.quick-schematic-grid.settings.page-position.left"),
                                    () -> feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_LEFT))
                                    .props(style(WebStyles.filterChipText()), checked(feature.pagePositionConfig.signal()
                                            .map(QuickSchematicGridFeature.PAGE_LEFT::equals)), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2f)));

                            button(Core.bundle.get("feature.quick-schematic-grid.settings.page-position.right"),
                                    () -> feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_RIGHT))
                                    .props(style(WebStyles.filterChipText()), checked(feature.pagePositionConfig.signal()
                                            .map(QuickSchematicGridFeature.PAGE_RIGHT::equals)), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2f)));

                            button(Core.bundle.get("feature.quick-schematic-grid.settings.page-position.top"),
                                    () -> feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_TOP))
                                    .props(style(WebStyles.filterChipText()), checked(feature.pagePositionConfig.signal()
                                            .map(QuickSchematicGridFeature.PAGE_TOP::equals)), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2f)));

                            button(Core.bundle.get("feature.quick-schematic-grid.settings.page-position.bottom"),
                                    () -> feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_BOTTOM))
                                    .props(style(WebStyles.filterChipText()), checked(feature.pagePositionConfig.signal()
                                            .map(QuickSchematicGridFeature.PAGE_BOTTOM::equals)), height(unit(8)), prefHeight(unit(8)), paddingX(unit(2f)));
                        });
                    });

                    divider().props(color(WebStyles.Colors.SECTION_BORDER));

                    checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle"),
                            feature.hideDragHandleConfig.signal()).props(growX());
                });
    }

    private Component pagesCard() {
        return SettingCard.section(Icon.list, Core.bundle.get("feature.quick-schematic-grid.settings.pages"), () -> {
            row().props(growX(), gap(unit(2)), center()).children(() -> {
                text(Core.bundle.get("feature.quick-schematic-grid.settings.pages")).props(color(Color.white), solim.modifier.Values.left());

                spacer();

                button(Core.bundle.get("feature.quick-schematic-grid.settings.add-page"), feature::addPage)
                        .props(style(WebStyles.secondary()), enabled(feature.pageCountConfig.signal()
                                .map(count -> count != null && count < QuickSchematicGridFeature.MAX_PAGES)), height(unit(8.5f)), prefHeight(unit(8.5f)));

                button(Core.bundle.get("feature.quick-schematic-grid.settings.delete-page"),
                        this::confirmDeletePage)
                        .props(style(WebStyles.ghost()), enabled(feature.pageCountConfig.signal()
                                .map(count -> count != null && count > QuickSchematicGridFeature.MIN_PAGES)), height(unit(8.5f)), prefHeight(unit(8.5f)));
            });

            Readable<List<Integer>> pagesList = Signal.computed(() -> {
                Integer count = feature.pageCountConfig.signal().get();
                feature.pageIcons().get();
                int total = Math.max(1,
                        Math.min(QuickSchematicGridFeature.MAX_PAGES, count != null ? count : 1));
                List<Integer> list = new ArrayList<>(total);
                for (int i = 0; i < total; i++) {
                    list.add(i);
                }
                return list;
            });

            dynamic(pagesList, pages -> wrap().props(growX(), gap(unit(1))).children(() -> {
                if (pages != null) {
                    for (int pageIndex : pages) {
                        String icon = feature.getPageIcon(pageIndex);
                        String label = icon != null && !icon.trim().isEmpty() ? icon
                                : String.valueOf(pageIndex + 1);
                        String tooltip = Core.bundle.format("feature.quick-schematic-grid.settings.page-tab",
                                pageIndex + 1);

                        button(label, () -> feature.setActivePage(pageIndex))
                                .props(style(WebStyles.filterChipText()), checked(feature.activePage.map(p -> p != null && p.intValue() == pageIndex)), tooltip(tooltip), height(unit(8.5f)), prefHeight(unit(8.5f)), minWidth(unit(14f)));
                    }
                }
            }));

            divider().props(color(WebStyles.Colors.SECTION_BORDER));

            row().props(growX(), gap(unit(2)), center()).children(() -> {
                text(Core.bundle.get("feature.quick-schematic-grid.settings.page-icon"))
                        .props(color(WebStyles.Colors.GHOST_FG), solim.modifier.Values.left());

                Readable<String> activeIcon = Signal.computed(() -> {
                    int p = feature.activePage.get();
                    feature.pageIcons().get();
                    return feature.getPageIcon(p);
                });

                dynamic(activeIcon, icon -> {
                    if (icon == null || icon.trim().isEmpty()) {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.page-icon.none"))
                                .props(color(Color.gray));
                    } else {
                        text(icon).props(solim.modifier.Values.fontScale(1.3f));
                    }
                });

                spacer();

                button(Core.bundle.get("feature.quick-schematic-grid.settings.page-icon.pick"),
                        this::pickPageIcon)
                        .props(style(WebStyles.secondary()), height(unit(8.5f)), prefHeight(unit(8.5f)));

                dynamic(activeIcon, icon -> {
                    if (icon != null && !icon.trim().isEmpty()) {
                        button(Core.bundle.get("feature.quick-schematic-grid.settings.page-icon.clear"),
                                this::clearPageIcon)
                                .props(style(WebStyles.ghost()), height(unit(8.5f)), prefHeight(unit(8.5f)));
                    }
                });
            });
        });
    }

    private Component gridEditorCard() {
        return SettingCard.section(Icon.grid, Core.bundle.get("feature.quick-schematic-grid.settings.entries"), () -> {
            buildSettingsGrid();
        });
    }

    private void buildSettingsGrid() {
        Readable<Float> gap = feature.buttonGapConfig.signal();

        Readable<List<QuickSchematicGridHudView.SlotModel>> slots = Signal.computed(() -> {
            int p = feature.getActivePage();
            int rows = Math.max(1, Math.min(QuickSchematicGridFeature.MAX_ROWS, feature.rowsConfig.signal().get()));
            int cols = Math.max(1, Math.min(QuickSchematicGridFeature.MAX_COLS, feature.colsConfig.signal().get()));
            List<QuickSchematicEntry> allEntries = feature.entries().get();

            List<QuickSchematicGridHudView.SlotModel> list = new ArrayList<>(rows * cols);
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
                    list.add(new QuickSchematicGridHudView.SlotModel(p, r, c, found));
                }
            }
            return list;
        });

        scroll().props(growX(), center(), scrollX(true), scrollY(true)).children(() -> {
            reactiveGrid(slots).columns(feature.colsConfig.signal())
                    .key(QuickSchematicGridHudView.SlotModel::key)
                    .props(growX(), gap(gap))
                    .items(this::settingsSlotComponent);
        });
    }

    private Component settingsSlotComponent(QuickSchematicGridHudView.SlotModel slot) {
        Readable<Float> buttonSize = feature.buttonSizeConfig.signal();
        if (slot.entry == null) {
            return button(() -> openSlotPicker(slot.page, slot.row, slot.col))
                    .props(style(WebStyles.secondary()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.quick-schematic-grid.settings.add")))
                    .children(() -> icon(Icon.add).props(size(buttonSize.map(s -> (s != null ? s : 48f) * 0.4f)), prefWidth(buttonSize.map(s -> (s != null ? s : 48f) * 0.4f)), prefHeight(buttonSize.map(s -> (s != null ? s : 48f) * 0.4f)), color(Color.lightGray)));
        }

        Schematic schematic = feature.resolveSchematic(slot.entry);
        String tooltip = slot.entry.displayName();

        if (slot.entry.hasCustomIcon()) {
            String glyph = slot.entry.customIcon;
            return button(() -> openSlotDialog(slot.entry.id))
                    .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(tooltip))
                    .children(() -> text(glyph != null ? glyph : "")
                            .props(
                                    solim.modifier.Values.fontScale(buttonSize.map(s -> (s != null ? s : 48f) / 32f)),
                                    solim.modifier.Values.center()
                            ));
        }

        if (schematic == null) {
            return button(() -> openSlotDialog(slot.entry.id))
                    .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.format("feature.quick-schematic-grid.tooltip.missing", tooltip)))
                    .children(() -> icon(Icon.warning).props(size(buttonSize.map(s -> (s != null ? s : 48f) * 0.5f)), prefWidth(buttonSize.map(s -> (s != null ? s : 48f) * 0.5f)), prefHeight(buttonSize.map(s -> (s != null ? s : 48f) * 0.5f)), color(Color.scarlet)));
        }

        return button(() -> openSlotDialog(slot.entry.id))
                .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(tooltip))
                .children(() -> new BoundedSchematicImage(schematic, buttonSize, 48f));
    }

    private void openSlotPicker(int page, int row, int col) {
        new SchematicPickerDialog(schematic -> {
            String fileName = schematic.file != null ? schematic.file.name() : null;
            feature.addSchematic(page, row, col, schematic.name(), fileName);
        }).show();
    }

    private void openSlotDialog(String entryId) {
        new QuickSchematicGridSlotDialog(feature, entryId).show();
    }

    private void confirmDeletePage() {
        int pageIndex = feature.getActivePage();
        Vars.ui.showConfirm(
                Core.bundle.get("feature.quick-schematic-grid.delete-page.title"),
                Core.bundle.format("feature.quick-schematic-grid.delete-page.message", pageIndex + 1),
                () -> feature.deletePage(pageIndex));
    }

    private void pickPageIcon() {
        int pageIndex = feature.getActivePage();
        new SchematicIconPickerDialog(icon -> feature.setPageIcon(pageIndex, icon)).show();
    }

    private void clearPageIcon() {
        feature.clearPageIcon(feature.getActivePage());
    }

    private Component actionsCard() {
        return SettingCard.card().children(() -> {
            button(Core.bundle.get("feature.quick-schematic-grid.settings.reset-position"),
                    feature::resetPosition)
                    .props(style(WebStyles.outlineText()), height(unit(11)), prefHeight(unit(11)), growX());
        });
    }
}
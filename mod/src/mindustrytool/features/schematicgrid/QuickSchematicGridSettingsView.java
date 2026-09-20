package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import java.util.ArrayList;
import java.util.List;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class QuickSchematicGridSettingsView extends BaseComponent {
    // TODO: Allow to drag and drop schematic
    private final QuickSchematicGridFeature feature;

    public QuickSchematicGridSettingsView(QuickSchematicGridFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().grow().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
                    // Display Mode (HUD vs Popup)
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.display-mode")).left();

                        spacer();
                        row().gap(unit(2)).children(() -> {
                            button(Core.bundle.get("feature.quick-schematic-grid.settings.display-mode.hud"),
                                    () -> feature.displayModeConfig.set(QuickSchematicGridFeature.DISPLAY_HUD))
                                            .style(Styles.togglet)
                                            .checked(feature.displayModeConfig.signal()
                                                    .map(QuickSchematicGridFeature.DISPLAY_HUD::equals))
                                            .height(unit(8.5f));

                            button(Core.bundle.get("feature.quick-schematic-grid.settings.display-mode.popup"),
                                    () -> feature.displayModeConfig.set(QuickSchematicGridFeature.DISPLAY_POPUP))
                                            .style(Styles.togglet)
                                            .checked(feature.displayModeConfig.signal()
                                                    .map(QuickSchematicGridFeature.DISPLAY_POPUP::equals))
                                            .height(unit(8.5f));
                        });
                    });

                    divider();

                    // Rows Slider (1-7)
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.rows")).left();

                        spacer();
                        slider(feature.rowsConfig.signal(),
                                QuickSchematicGridFeature.MIN_ROWS,
                                QuickSchematicGridFeature.MAX_ROWS, 1);

                        row().width(unit(14)).children(() -> {
                            text(feature.rowsConfig.signal().map(String::valueOf));
                        });
                    });

                    // Columns Slider (1-7)
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.cols")).left();

                        spacer();
                        slider(feature.colsConfig.signal(),
                                QuickSchematicGridFeature.MIN_COLS,
                                QuickSchematicGridFeature.MAX_COLS, 1);

                        row().width(unit(14)).children(() -> {
                            text(feature.colsConfig.signal().map(String::valueOf));
                        });
                    });

                    // Button Size Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.button-size")).left();

                        spacer();
                        slider(feature.buttonSizeConfig.signal(), 32f, 72f, 4f);

                        row().width(unit(14)).children(() -> {
                            text(feature.buttonSizeConfig.signal()
                                    .map(v -> String.valueOf(Math.round(v != null ? v : 48f))));
                        });
                    });

                    // Button Gap Slider
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.button-gap")).left();

                        spacer();
                        slider(feature.buttonGapConfig.signal(),
                                QuickSchematicGridFeature.MIN_GAP,
                                QuickSchematicGridFeature.MAX_GAP, 1f);

                        row().width(unit(14)).children(() -> {
                            text(feature.buttonGapConfig.signal()
                                    .map(v -> String.valueOf(Math.round(v != null ? v : 4f))));
                        });
                    });

                    // Page Position (Left, Right, Top, Bottom)
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.page-position")).left();

                        spacer();
                        row().gap(unit(2)).children(() -> {
                            button(Core.bundle.get("feature.quick-schematic-grid.settings.page-position.left"),
                                    () -> feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_LEFT))
                                            .style(Styles.togglet)
                                            .checked(feature.pagePositionConfig.signal()
                                                    .map(QuickSchematicGridFeature.PAGE_LEFT::equals))
                                            .height(unit(8.5f));

                            button(Core.bundle.get("feature.quick-schematic-grid.settings.page-position.right"),
                                    () -> feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_RIGHT))
                                            .style(Styles.togglet)
                                            .checked(feature.pagePositionConfig.signal()
                                                    .map(QuickSchematicGridFeature.PAGE_RIGHT::equals))
                                            .height(unit(8.5f));

                            button(Core.bundle.get("feature.quick-schematic-grid.settings.page-position.top"),
                                    () -> feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_TOP))
                                            .style(Styles.togglet)
                                            .checked(feature.pagePositionConfig.signal()
                                                    .map(QuickSchematicGridFeature.PAGE_TOP::equals))
                                            .height(unit(8.5f));

                            button(Core.bundle.get("feature.quick-schematic-grid.settings.page-position.bottom"),
                                    () -> feature.pagePositionConfig.set(QuickSchematicGridFeature.PAGE_BOTTOM))
                                            .style(Styles.togglet)
                                            .checked(feature.pagePositionConfig.signal()
                                                    .map(QuickSchematicGridFeature.PAGE_BOTTOM::equals))
                                            .height(unit(8.5f));
                        });
                    });

                    // Button Opacity Slider (20% to 100%)
                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.button-opacity")).left();

                        spacer();
                        slider(feature.buttonOpacityConfig.signal(),
                                QuickSchematicGridFeature.MIN_BUTTON_OPACITY,
                                QuickSchematicGridFeature.MAX_BUTTON_OPACITY, 0.05f);

                        row().width(unit(14)).children(() -> {
                            text(feature.buttonOpacityConfig.signal()
                                    .map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100)));
                        });
                    });

                    // Hide Drag Handle Checkbox
                    checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle"),
                            feature.hideDragHandleConfig.signal()).growX();

                    divider();

                    // Page Management Bar
                    row().growX().gap(unit(2)).center().children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.pages")).left().color(Color.white);

                        spacer();

                        button(Core.bundle.get("feature.quick-schematic-grid.settings.add-page"), feature::addPage)
                                .style(WebStyles.secondary())
                                .enabled(feature.pageCountConfig.signal()
                                        .map(count -> count != null && count < QuickSchematicGridFeature.MAX_PAGES))
                                .height(unit(8.5f));

                        button(Core.bundle.get("feature.quick-schematic-grid.settings.delete-page"),
                                this::confirmDeletePage)
                                        .style(WebStyles.ghost())
                                        .enabled(feature.pageCountConfig.signal()
                                                .map(count -> count != null
                                                        && count > QuickSchematicGridFeature.MIN_PAGES))
                                        .height(unit(8.5f));
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

                    dynamic(pagesList, pages -> wrap().growX().gap(unit(1)).children(() -> {
                        if (pages != null) {
                            for (int pageIndex : pages) {
                                String icon = feature.getPageIcon(pageIndex);
                                String label = icon != null && !icon.trim().isEmpty() ? icon
                                        : String.valueOf(pageIndex + 1);
                                String tooltip = Core.bundle.format("feature.quick-schematic-grid.settings.page-tab",
                                        pageIndex + 1);

                                button(label, () -> feature.setActivePage(pageIndex))
                                        .style(WebStyles.filterChipText())
                                        .checked(feature.activePage.map(p -> p != null && p.intValue() == pageIndex))
                                        .tooltip(tooltip)
                                        .height(unit(8.5f))
                                        .minWidth(unit(14f));
                            }
                        }
                    }));

                    // Active Page Icon Configuration Row
                    row().growX().gap(unit(2)).center().children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.page-icon"))
                                .left()
                                .color(WebStyles.Colors.GHOST_FG);

                        Readable<String> activeIcon = Signal.computed(() -> {
                            int p = feature.activePage.get();
                            feature.pageIcons().get();
                            return feature.getPageIcon(p);
                        });

                        dynamic(activeIcon, icon -> {
                            if (icon == null || icon.trim().isEmpty()) {
                                return text(Core.bundle.get("feature.quick-schematic-grid.settings.page-icon.none"))
                                        .color(Color.gray);
                            }
                            return text(icon).fontScale(1.3f);
                        });

                        spacer();

                        button(Core.bundle.get("feature.quick-schematic-grid.settings.page-icon.pick"),
                                this::pickPageIcon)
                                        .style(WebStyles.secondary())
                                        .height(unit(8.5f));

                        dynamic(activeIcon, icon -> {
                            if (icon != null && !icon.trim().isEmpty()) {
                                return button(Core.bundle.get("feature.quick-schematic-grid.settings.page-icon.clear"),
                                        this::clearPageIcon)
                                                .style(WebStyles.ghost())
                                                .height(unit(8.5f));
                            }
                            return null;
                        });
                    });

                    divider();

                    // Interactive 2D Grid Editor
                    text(Core.bundle.get("feature.quick-schematic-grid.settings.entries")).left().growX()
                            .color(Color.white);

                    buildSettingsGrid();

                    divider();

                    // Reset Position
                    button(Core.bundle.get("feature.quick-schematic-grid.settings.reset-position"),
                            feature::resetPosition)
                                    .style(Styles.defaultb).growX();
                });
            });
        }).element();
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

        scroll().scrollX(true).scrollY(true).center().children(() -> {
            reactiveGrid(
                    feature.colsConfig.signal(),
                    slots,
                    QuickSchematicGridHudView.SlotModel::key,
                    this::settingsSlotComponent)
                            .gap(gap);
        });
    }

    private Component settingsSlotComponent(QuickSchematicGridHudView.SlotModel slot) {
        Readable<Float> buttonSize = feature.buttonSizeConfig.signal();
        if (slot.entry == null) {
            return button(() -> openSlotPicker(slot.page, slot.row, slot.col))
                    .style(WebStyles.secondary())
                    .size(buttonSize)
                    .tooltip(Core.bundle.get("feature.quick-schematic-grid.settings.add"))
                    .children(() -> icon(Icon.add).size(buttonSize.map(s -> (s != null ? s : 48f) * 0.4f))
                            .color(Color.lightGray));
        }

        Schematic schematic = feature.resolveSchematic(slot.entry);
        String tooltip = slot.entry.displayName();

        if (slot.entry.hasCustomIcon()) {
            String glyph = slot.entry.customIcon;
            return button(() -> openSlotDialog(slot.entry.id))
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(tooltip)
                    .children(() -> text(glyph != null ? glyph : "")
                            .fontScale(buttonSize.map(s -> (s != null ? s : 48f) / 32f))
                            .center());
        }

        if (schematic == null) {
            return button(() -> openSlotDialog(slot.entry.id))
                    .style(WebStyles.ghost())
                    .size(buttonSize)
                    .tooltip(Core.bundle.format("feature.quick-schematic-grid.tooltip.missing", tooltip))
                    .children(() -> icon(Icon.warning).size(buttonSize.map(s -> (s != null ? s : 48f) * 0.5f))
                            .color(Color.scarlet));
        }

        return button(() -> openSlotDialog(slot.entry.id))
                .style(WebStyles.ghost())
                .size(buttonSize)
                .tooltip(tooltip)
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
}

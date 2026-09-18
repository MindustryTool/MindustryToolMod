package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import java.util.List;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class QuickSchematicGridSettingsView extends BaseComponent {

    private final QuickSchematicGridFeature feature;

    public QuickSchematicGridSettingsView(QuickSchematicGridFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().grow().center().children(() -> {
                column().growX().gap(unit(2)).children(() -> {
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

                    row().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.settings.button-size")).left();

                        spacer();
                        slider(feature.buttonSizeConfig.signal(), 32f, 72f, 4f);

                        row().width(unit(14)).children(() -> {
                            text(feature.buttonSizeConfig.signal()
                                    .map(v -> String.valueOf(Math.round(v != null ? v : 48f))));
                        });
                    });

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

                    checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle"),
                            feature.hideDragHandleConfig.signal()).growX();

                    divider();

                    text(Core.bundle.get("feature.quick-schematic-grid.settings.entries")).left().growX()
                            .color(Color.white);

                    dynamic(feature.entries(), entries -> {
                        if (entries == null || entries.isEmpty()) {
                            return text(Core.bundle.get("feature.quick-schematic-grid.settings.empty"))
                                    .color(Color.gray)
                                    .padding(unit(4))
                                    .center();
                        }
                        return reactiveGrid(
                                Signal.of(1),
                                feature.entries(),
                                entry -> entry != null && entry.id != null ? entry.id : "",
                                entry -> new EntryRow(feature, entry))
                                .growX()
                                .gap(unit(2));
                    }).growX();

                    button(Core.bundle.get("feature.quick-schematic-grid.settings.add"), this::openPicker)
                            .style(WebStyles.secondary())
                            .growX();

                    divider();

                    button(Core.bundle.get("feature.quick-schematic-grid.settings.reset-position"),
                            feature::resetPosition)
                            .style(Styles.defaultb).growX();
                });
            });
        }).element();
    }

    private void openPicker() {
        new SchematicPickerDialog(schematic -> {
            String fileName = schematic.file != null ? schematic.file.name() : null;
            feature.addSchematic(schematic.name(), fileName);
        }).show();
    }

    private static final class EntryRow extends BaseComponent {

        private final QuickSchematicGridFeature feature;
        private final QuickSchematicEntry entry;

        EntryRow(QuickSchematicGridFeature feature, QuickSchematicEntry entry) {
            this.feature = feature;
            this.entry = entry;
        }

        @Override
        protected Element build() {
            String id = entry.id;
            Schematic schematic = feature.resolveSchematic(entry);
            String title = schematic != null && schematic.name() != null
                    ? schematic.name()
                    : entry.displayName();
            String subtitle = schematic != null
                    ? schematic.width + " x " + schematic.height
                    : Core.bundle.get("feature.quick-schematic-grid.warning.missing");
            Readable<Color> titleColor = Signal.of(schematic != null ? Color.white : Color.scarlet);

            return row().growX().gap(unit(1)).center().children(() -> {
                column().growX().gap(unit(0.5f)).children(() -> {
                    text(title).growX().left().ellipsis(true).color(titleColor);
                    text(subtitle).growX().left().color(Color.lightGray).fontScale(0.85f);
                });

                button(() -> feature.moveEarlier(id))
                        .style(WebStyles.ghost())
                        .size(unit(11))
                        .tooltip(Core.bundle.get("feature.quick-schematic-grid.button.move-left"))
                        .children(() -> text("◄").color(canMoveEarlier() ? Color.white : Color.darkGray));

                button(() -> feature.moveLater(id))
                        .style(WebStyles.ghost())
                        .size(unit(11))
                        .tooltip(Core.bundle.get("feature.quick-schematic-grid.button.move-right"))
                        .children(() -> text("►").color(canMoveLater() ? Color.white : Color.darkGray));

                button(() -> feature.removeEntry(id))
                        .style(WebStyles.ghost())
                        .size(unit(11))
                        .tooltip(Core.bundle.get("feature.quick-schematic-grid.button.remove"))
                        .children(() -> icon(Icon.cancel).size(unit(6)).color(Color.scarlet));
            }).element();
        }

        private boolean canMoveEarlier() {
            List<QuickSchematicEntry> current = feature.getEntries();
            return QuickSchematicGridFeature.canMoveEarlier(current, entry.id);
        }

        private boolean canMoveLater() {
            List<QuickSchematicEntry> current = feature.getEntries();
            return QuickSchematicGridFeature.canMoveLater(current, entry.id);
        }
    }
}

package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class QuickSchematicGridSlotDialog extends SolimDialog {

    private final QuickSchematicGridFeature feature;
    private final String entryId;

    private final Signal<String> labelText;
    private final Signal<String> schematicName;
    private final Signal<String> schematicFile;
    private final Signal<String> iconText;

    public QuickSchematicGridSlotDialog(QuickSchematicGridFeature feature, String entryId) {
        super(Core.bundle.get("feature.quick-schematic-grid.edit.title"));
        this.feature = feature;
        this.entryId = entryId;

        QuickSchematicEntry entry = feature.getEntry(entryId);

        labelText = Signal.of(entry != null && entry.customLabel != null ? entry.customLabel : "");
        schematicName = Signal.of(entry != null && entry.schematicName != null ? entry.schematicName : "");
        schematicFile = Signal.of(entry != null ? entry.schematicFile : null);
        iconText = Signal.of(entry != null ? entry.customIcon : null);

        Readable<Schematic> resolved = Signal.computed(
                () -> feature.resolveSchematic(schematicFile.get(), schematicName.get()));

        name("quickSchematicGridSlotDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> {
            Effect.of(labelText, value -> persist());
            Effect.of(iconText, value -> persist());

            column().props(grow(), padding(unit(3)), center(), gap(unit(2.5f))).children(() -> {
                text(Core.bundle.get("feature.quick-schematic-grid.edit.schematic"))
                        .props(color(WebStyles.Colors.GHOST_FG), growX(), left());

                card(WebStyles.previewCardBackground())
                        .props(growX(), height(unit(42f)), prefHeight(unit(42f)), onClick(this::repickSchematic))
                        .children(() -> {
                            dynamic(resolved, schematic -> {
                                if (schematic == null) {
                                    row().props(grow(), center()).children(() -> {
                                        icon(Icon.warning).props(size(unit(10)), prefWidth(unit(10)), prefHeight(unit(10)), color(Color.scarlet));
                                    });
                                } else {
                                    row().props(grow()).children(() -> {
                                        arc(new SchematicImage(schematic));
                                    });
                                }
                            }).props(grow());
                        });

                text(Core.bundle.get("feature.quick-schematic-grid.edit.label"))
                        .props(color(WebStyles.Colors.GHOST_FG), growX(), left());

                row().props(growX(), gap(unit(1.5f)),
                        padding(unit(1.5f)), rounded(unit(2), WebStyles.Colors.SECONDARY_BG), border(1.5f, WebStyles.Colors.BORDER_INPUT),
                        center())
                        .children(() -> {
                            textField(labelText)
                                    .props(style(WebStyles.clearInput()), growX(), height(unit(8)), prefHeight(unit(8)),
                                            placeholder(Core.bundle.get("feature.quick-schematic-grid.edit.label.hint")));
                        });

                text(Core.bundle.get("feature.quick-schematic-grid.edit.icon"))
                        .props(color(WebStyles.Colors.GHOST_FG), growX(), left());

                row().props(growX(), gap(unit(2)), center()).children(() -> {
                    dynamic(iconText, icon -> {
                        if (icon == null || icon.trim().isEmpty()) {
                            text(Core.bundle.get("feature.quick-schematic-grid.edit.icon.none"))
                                    .props(color(Color.gray), growX(), left());
                        } else {
                            text(icon).props(fontScale(1.5f), growX(), left());
                        }
                    }).props(growX());

                    spacer();

                    button(Core.bundle.get("feature.quick-schematic-grid.edit.icon.pick"), this::pickIcon)
                            .props(style(WebStyles.secondary()), height(unit(9)), prefHeight(unit(9)));

                    button(Core.bundle.get("feature.quick-schematic-grid.edit.icon.clear"), this::clearIcon)
                            .props(style(WebStyles.ghost()), height(unit(9)), prefHeight(unit(9)));
                });

                divider();

                button(this::clearSlot)
                        .props(style(WebStyles.ghost()), growX(), height(unit(9)), prefHeight(unit(9)))
                        .children(() -> {
                            icon(Icon.cancel).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)), color(Color.scarlet));
                            text(Core.bundle.get("feature.quick-schematic-grid.edit.clear-slot")).props(color(Color.scarlet));
                        });
            });
        });
    }

    private void clearSlot() {
        feature.removeEntry(entryId);
        hide();
    }

    private void repickSchematic() {
        new SchematicPickerDialog(schematic -> {
            schematicName.set(schematic.name() != null ? schematic.name() : "");
            schematicFile.set(schematic.file != null ? schematic.file.name() : null);
            persist();
        }).show();
    }

    private void pickIcon() {
        new SchematicIconPickerDialog(icon -> {
            iconText.set(icon);
            persist();
        }).show();
    }

    private void clearIcon() {
        iconText.set(null);
        persist();
    }

    private void persist() {
        String label = labelText.peek();
        String cleanLabel = label != null && !label.trim().isEmpty() ? label.trim() : null;
        String icon = iconText.peek();
        String cleanIcon = icon != null && !icon.trim().isEmpty() ? icon : null;
        String name = schematicName.peek();
        String file = schematicFile.peek();
        feature.updateEntry(entryId, entry -> {
            entry.customLabel = cleanLabel;
            entry.customIcon = cleanIcon;
            entry.schematicName = name != null ? name : "";
            entry.schematicFile = file;
        });
    }
}
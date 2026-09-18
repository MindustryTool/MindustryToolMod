package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import mindustrytool.components.WebStyles;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Per-slot edit dialog with live thumbnail (tap re-picks the schematic), custom
 * label field, and custom icon pick/clear actions. All edits persist
 * immediately to the feature.
 */
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

        labelText.subscribe(value -> persist());
        iconText.subscribe(value -> persist());

        Readable<Schematic> resolved = Signal.computed(
                () -> feature.resolveSchematic(schematicFile.get(), schematicName.get()));

        name("quickSchematicGridSlotDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(500f);

        children(() -> {
            column().grow().gap(unit(2.5f)).padding(unit(3)).center().children(() -> {
                text(Core.bundle.get("feature.quick-schematic-grid.edit.schematic"))
                        .growX()
                        .left()
                        .color(WebStyles.Colors.GHOST_FG);

                card(WebStyles.previewCardBackground())
                        .growX()
                        .height(unit(42f))
                        .onClick(this::repickSchematic)
                        .children(() -> {
                            dynamic(resolved, schematic -> {
                                if (schematic == null) {
                                    return row().grow().center().children(() -> {
                                        icon(Icon.warning).size(unit(10)).color(Color.scarlet);
                                    });
                                }
                                return row().grow().children(() -> {
                                    arc(new SchematicImage(schematic));
                                });
                            }).grow();
                        });

                text(Core.bundle.get("feature.quick-schematic-grid.edit.label"))
                        .growX()
                        .left()
                        .color(WebStyles.Colors.GHOST_FG);

                row().growX().gap(unit(1.5f))
                        .padding(unit(1.5f))
                        .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                        .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                        .center()
                        .children(() -> {
                            textField(labelText)
                                    .growX()
                                    .height(unit(8))
                                    .style(WebStyles.clearInput())
                                    .placeholder(Core.bundle.get("feature.quick-schematic-grid.edit.label.hint"));
                        });

                text(Core.bundle.get("feature.quick-schematic-grid.edit.icon"))
                        .growX()
                        .left()
                        .color(WebStyles.Colors.GHOST_FG);

                row().growX().gap(unit(2)).center().children(() -> {
                    dynamic(iconText, icon -> {
                        if (icon == null || icon.trim().isEmpty()) {
                            return text(Core.bundle.get("feature.quick-schematic-grid.edit.icon.none"))
                                    .growX()
                                    .left()
                                    .color(Color.gray);
                        }
                        return text(icon).fontScale(1.5f).growX().left();
                    }).growX();

                    spacer();

                    button(Core.bundle.get("feature.quick-schematic-grid.edit.icon.pick"), this::pickIcon)
                            .style(WebStyles.secondary())
                            .height(unit(9));

                    button(Core.bundle.get("feature.quick-schematic-grid.edit.icon.clear"), this::clearIcon)
                            .style(WebStyles.ghost())
                            .height(unit(9));
                });
            });
        });
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

package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.util.Scaling;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import mindustry.Vars;
import mindustry.game.Schematic;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.overlay.SolimDialog;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Solim picker for local schematics with search, tag filtering, and preview cards.
 */
public class SchematicPickerDialog extends SolimDialog {

    private final Signal<String> searchQuery = Signal.of("");
    private final Signal<String> selectedTag = Signal.of((String) null);

    public SchematicPickerDialog(Consumer<Schematic> onSelect) {
        super(Core.bundle.get("feature.quick-schematic-grid.picker.title"));

        name("schematicPickerDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(1000f);

        Seq<Schematic> all = snapshotSchematics();
        Seq<String> tags = collectTags(all);

        Readable<Seq<Schematic>> filtered = Signal.computed(() -> filter(all, searchQuery.get(), selectedTag.get()));

        Computed<Float> viewportWidth = dvw(100f);
        Readable<Boolean> portrait = isPortrait();
        Computed<Integer> columnCount = new Computed<>(() -> {
            Float width = viewportWidth.get();
            float w = width != null ? width : 800f;
            if (Boolean.TRUE.equals(portrait.get())) {
                return w < 500f ? 2 : 3;
            }
            if (w < 650f) {
                return 2;
            }
            if (w < 1100f) {
                return 3;
            }
            return Math.max(3, Math.min(5, (int) (w / 260f)));
        });

        children(() -> {
            column().grow().gap(unit(2.5f)).padding(unit(3)).center().children(() -> {
                row().growX().gap(unit(1.5f))
                        .padding(unit(1.5f))
                        .rounded(unit(2), WebStyles.Colors.SECONDARY_BG)
                        .border(1.5f, WebStyles.Colors.BORDER_INPUT)
                        .center()
                        .children(() -> {
                            icon(Icon.zoom).size(unit(4.5f)).color(WebStyles.Colors.GHOST_FG);
                            textField(searchQuery)
                                    .growX()
                                    .height(unit(8))
                                    .style(WebStyles.clearInput())
                                    .placeholder(Core.bundle.get("feature.quick-schematic-grid.picker.search"));
                        });

                scroll().growX().children(() -> {
                    wrap().left().gap(unit(1.5f)).children(() -> {
                        tagChip(null, tags);
                        for (String tag : tags) {
                            tagChip(tag, tags);
                        }
                    });
                });

                scroll().grow().children(() -> {
                    reactiveGrid(
                            columnCount,
                            filtered,
                            SchematicPickerDialog::keyOf,
                            schematic -> new SchematicPickerCard(schematic, () -> {
                                if (onSelect != null) {
                                    onSelect.accept(schematic);
                                }
                                hide();
                            }))
                            .empty(() -> {
                                text(Core.bundle.get("feature.quick-schematic-grid.picker.empty"))
                                        .color(Color.gray)
                                        .padding(unit(4));
                            })
                            .gap(unit(3));
                });
            });
        });
    }

    private Component tagChip(@Nullable String tag, Seq<String> tags) {
        String label = tag == null
                ? Core.bundle.get("feature.quick-schematic-grid.picker.tag.all")
                : tag;
        return button()
                .style(WebStyles.filterChip())
                .checked(selectedTag.map(sel -> tag == null ? sel == null : tag.equals(sel)))
                .onClick(() -> selectedTag.set(tag))
                .padding(unit(1.5f))
                .tooltip(label)
                .children(() -> text(label));
    }

    private static Seq<Schematic> snapshotSchematics() {
        try {
            Seq<Schematic> all = Vars.schematics.all();
            return all != null ? new Seq<>(all) : new Seq<>();
        } catch (Exception ignored) {
            return new Seq<>();
        }
    }

    private static Seq<String> collectTags(Seq<Schematic> all) {
        Seq<String> tags = new Seq<>();
        if (all == null) {
            return tags;
        }
        for (Schematic schematic : all) {
            if (schematic == null || schematic.labels == null) {
                continue;
            }
            for (String label : schematic.labels) {
                if (label != null && !label.trim().isEmpty() && !tags.contains(label)) {
                    tags.add(label);
                }
            }
        }
        tags.sort();
        return tags;
    }

    static Seq<Schematic> filter(@Nullable Seq<Schematic> all, @Nullable String query, @Nullable String tag) {
        Seq<Schematic> result = new Seq<>();
        if (all == null) {
            return result;
        }
        String lower = query != null ? query.trim().toLowerCase() : "";
        boolean hasQuery = !lower.isEmpty();
        boolean hasTag = tag != null && !tag.trim().isEmpty();
        for (Schematic schematic : all) {
            if (schematic == null) {
                continue;
            }
            if (hasTag && (schematic.labels == null || !schematic.labels.contains(tag))) {
                continue;
            }
            if (hasQuery) {
                String name = schematic.name() != null ? schematic.name().toLowerCase() : "";
                String desc = schematic.description() != null ? schematic.description().toLowerCase() : "";
                if (!name.contains(lower) && !desc.contains(lower)) {
                    continue;
                }
            }
            result.add(schematic);
        }
        return result;
    }

    static String keyOf(Schematic schematic) {
        if (schematic.file != null && schematic.file.name() != null) {
            return schematic.file.name();
        }
        String name = schematic.name() != null ? schematic.name() : "schematic";
        return name + "@" + schematic.width + "x" + schematic.height;
    }

    static List<String> collectTagList(List<String> labels) {
        List<String> tags = new ArrayList<>();
        if (labels == null) {
            return tags;
        }
        for (String label : labels) {
            if (label != null && !label.trim().isEmpty() && !tags.contains(label)) {
                tags.add(label);
            }
        }
        return tags;
    }

    /**
     * Schematic card showing preview thumbnail, title, dimensions, and selection callback.
     */
    public static class SchematicPickerCard extends BaseComponent {

        private final Schematic schematic;
        private final Runnable onClick;

        public SchematicPickerCard(Schematic schematic, Runnable onClick) {
            this.schematic = schematic;
            this.onClick = onClick;
        }

        @Override
        protected Element build() {
            String title = schematic.name() != null && !schematic.name().trim().isEmpty()
                    ? schematic.name()
                    : Core.bundle.get("browser.schematic.unnamed");
            String dimensions = schematic.width + " x " + schematic.height;

            return column()
                    .growX()
                    .gap(unit(1.5f))
                    .children(() -> {
                        card(WebStyles.previewCardBackground())
                                .growX()
                                .height(unit(42f))
                                .onClick(onClick)
                                .tooltip(title)
                                .children(() -> {
                                    // FillParent makes the image track the fixed-height card bounds so
                                    // Scaling.fit centers the preview instead of anchoring top-left
                                    // at its native preferred size.
                                    SchematicImage image = new SchematicImage(schematic);
                                    image.setScaling(Scaling.fit);
                                    image.setFillParent(true);
                                    arc(image);
                                });

                        text(title)
                                .growX()
                                .left()
                                .ellipsis(true)
                                .color(Color.white);

                        text(dimensions)
                                .growX()
                                .left()
                                .color(Color.lightGray)
                                .fontScale(0.85f);
                    }).element();
        }
    }
}

package mindustrytool.features.schematicgrid;

import static solim.UI.*;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.style;
import static solim.modifier.Values.color;
import static solim.modifier.Values.tooltip;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.TextureRegionDrawable;
import arc.struct.Seq;
import arc.util.Nullable;
import arc.func.Cons;
import mindustry.Vars;
import mindustry.ctype.Content;
import mindustry.ctype.ContentType;
import mindustry.ctype.UnlockableContent;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.emoji.EmojiIcons;
import mindustrytool.features.emoji.EmojiIcons.Entry;
import solim.modifier.Values;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class SchematicIconPickerDialog extends SolimDialog {

    private final Signal<String> searchQuery = Signal.of("");
    private final Cons<String> onSelect;

    public SchematicIconPickerDialog(Cons<String> onSelect) {
        super(Core.bundle.get("feature.quick-schematic-grid.icon.title"));
        this.onSelect = onSelect;

        name("schematicIconPickerDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(1000f);

        Seq<Entry> glyphs = EmojiIcons.entries();
        Seq<UnlockableContent> contents = collectContentIcons();

        Readable<Seq<Entry>> filteredGlyphs = searchQuery.map(query -> {
            if (query == null || query.trim().isEmpty()) {
                return glyphs;
            }
            String lower = query.trim().toLowerCase();
            return glyphs.select(entry -> entry.name.toLowerCase().contains(lower));
        });

        Readable<Seq<UnlockableContent>> filteredContents = searchQuery.map(query -> {
            if (query == null || query.trim().isEmpty()) {
                return contents;
            }
            String lower = query.trim().toLowerCase();
            return contents.select(content -> {
                String name = content.localizedName != null ? content.localizedName.toLowerCase() : "";
                return name.contains(lower);
            });
        });

        children(() -> {
            column().props(grow(), padding(unit(3)), center(), gap(unit(2.5f))).children(() -> {
                row().props(growX(), padding(unit(1.5f)), Values.rounded(unit(2), WebStyles.Colors.SECONDARY_BG), Values.border(1.5f, WebStyles.Colors.BORDER_INPUT), gap(unit(1.5f)), center())
                        .children(() -> {
                            icon(Icon.zoom).props(size(unit(4.5f)), prefWidth(unit(4.5f)), prefHeight(unit(4.5f)), color(WebStyles.Colors.GHOST_FG));
                            textField(searchQuery)
                                    .props(style(WebStyles.clearInput()), growX(), height(unit(8)), prefHeight(unit(8)),
                                            Values.placeholder(Core.bundle.get("feature.quick-schematic-grid.icon.search")));
                        });

                scroll().props(grow()).children(() -> {
                    column().props(growX(), gap(unit(2))).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.icon.glyphs"))
                                .props(color(Color.white), growX(), solim.modifier.Values.left());

                        dynamic(filteredGlyphs, items -> {
                            if (items == null || items.isEmpty()) {
                                row().props(growX(), padding(unit(2)), center()).children(() -> {
                                    text(Core.bundle.get("feature.quick-schematic-grid.icon.empty"))
                                            .props(color(Color.gray));
                                });
                            } else {
                                wrap().props(left(), gap(unit(1.5f))).children(() -> {
                                    for (Entry entry : items) {
                                        glyphChip(entry);
                                    }
                                });
                            }
                        });

                        text(Core.bundle.get("feature.quick-schematic-grid.icon.content"))
                                .props(color(Color.white), growX(), solim.modifier.Values.left());

                        dynamic(filteredContents, items -> {
                            if (items == null || items.isEmpty()) {
                                row().props(growX(), padding(unit(2)), center()).children(() -> {
                                    text(Core.bundle.get("feature.quick-schematic-grid.icon.empty"))
                                            .props(color(Color.gray));
                                });
                            } else {
                                wrap().props(left(), gap(unit(1.5f))).children(() -> {
                                    for (UnlockableContent content : items) {
                                        contentChip(content);
                                    }
                                });
                            }
                        });
                    });
                });
            });
        });
    }

    private void glyphChip(Entry entry) {
        button()
                .props(style(WebStyles.actionChip()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), padding(unit(1)), tooltip(entry.name))
                .onClick(() -> select(String.valueOf(entry.glyph)))
                .children(() -> text(String.valueOf(entry.glyph)).props(solim.modifier.Values.fontScale(1.2f)));
    }

    private void contentChip(UnlockableContent content) {
        String emoji = content.emoji() + "";
        button()
                .props(style(WebStyles.actionChip()), size(unit(11)), prefWidth(unit(11)), prefHeight(unit(11)), padding(unit(1)), tooltip(content.localizedName))
                .onClick(() -> select(emoji))
                .children(() -> image(new TextureRegionDrawable(content.uiIcon)).props(size(unit(6.5f)), prefWidth(unit(6.5f)), prefHeight(unit(6.5f))));
    }

    private void select(@Nullable String emoji) {
        if (emoji == null || emoji.trim().isEmpty()) {
            return;
        }
        if (onSelect != null) {
            onSelect.get(emoji);
        }
        hide();
    }

    static Seq<UnlockableContent> collectContentIcons() {
        Seq<UnlockableContent> result = new Seq<>();
        Seq<String> seen = new Seq<>();
        try {
            for (ContentType type : Vars.defaultContentIcons) {
                for (Content content : Vars.content.getBy(type)) {
                    if (!(content instanceof UnlockableContent)) {
                        continue;
                    }
                    UnlockableContent unlockable = (UnlockableContent) content;
                    if (unlockable.isHidden() || !unlockable.unlockedNow() || !unlockable.hasEmoji()) {
                        continue;
                    }
                    String emoji = unlockable.emoji() + "";
                    if (seen.contains(emoji)) {
                        continue;
                    }
                    seen.add(emoji);
                    result.add(unlockable);
                }
            }
        } catch (Exception ignored) {
        }
        return result;
    }
}
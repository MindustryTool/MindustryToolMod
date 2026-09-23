package mindustrytool.features.schematicgrid;

import static solim.UI.*;

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
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Vanilla-style icon picker with font glyph and unlocked content sections.
 * Selection returns the single resolved emoji string for slot storage.
 */
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
                                    .placeholder(Core.bundle.get("feature.quick-schematic-grid.icon.search"));
                        });

                scroll().grow().children(() -> {
                    column().growX().gap(unit(2)).children(() -> {
                        text(Core.bundle.get("feature.quick-schematic-grid.icon.glyphs"))
                                .growX()
                                .left()
                                .color(Color.white);

                        dynamic(filteredGlyphs, items -> {
                            if (items == null || items.isEmpty()) {
                                row().growX().padding(unit(2)).center().children(() -> {
                                    text(Core.bundle.get("feature.quick-schematic-grid.icon.empty"))
                                            .color(Color.gray);
                                });
                            } else {
                                wrap().left().gap(unit(1.5f)).children(() -> {
                                    for (Entry entry : items) {
                                        glyphChip(entry);
                                    }
                                });
                            }
                        });

                        text(Core.bundle.get("feature.quick-schematic-grid.icon.content"))
                                .growX()
                                .left()
                                .color(Color.white);

                        dynamic(filteredContents, items -> {
                            if (items == null || items.isEmpty()) {
                                row().growX().padding(unit(2)).center().children(() -> {
                                    text(Core.bundle.get("feature.quick-schematic-grid.icon.empty"))
                                            .color(Color.gray);
                                });
                            } else {
                                wrap().left().gap(unit(1.5f)).children(() -> {
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
                .style(WebStyles.filterChip())
                .onClick(() -> select(String.valueOf(entry.glyph)))
                .size(unit(11))
                .padding(unit(1))
                .tooltip(entry.name)
                .children(() -> text(String.valueOf(entry.glyph)).fontScale(1.2f));
    }

    private void contentChip(UnlockableContent content) {
        String emoji = content.emoji() + "";
        button()
                .style(WebStyles.filterChip())
                .onClick(() -> select(emoji))
                .size(unit(11))
                .padding(unit(1))
                .tooltip(content.localizedName)
                .children(() -> image(new TextureRegionDrawable(content.uiIcon)).size(unit(6.5f)));
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

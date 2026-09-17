package mindustrytool.features.emoji;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustrytool.components.WebStyles;
import mindustrytool.features.emoji.EmojiIcons.Entry;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Searchable browser for {@link Iconc} font glyphs. Typing filters entries by
 * field name; clicking a chip copies its glyph character to the clipboard.
 */
public class EmojiDialog extends SolimDialog {

    private final Signal<String> searchQuery = Signal.of("");

    public EmojiDialog() {
        super(Core.bundle.get("feature.emoji.title"));

        name("emojiDialog");
        addCloseButton();
        closeOnBack();
        maxWidth(1000f);

        Seq<Entry> allEntries = EmojiIcons.entries();
        Readable<Seq<Entry>> filteredEntries = searchQuery.map(query -> {
            if (query == null || query.trim().isEmpty()) {
                return allEntries;
            }
            String lower = query.trim().toLowerCase();
            return allEntries.select(entry -> entry.name.toLowerCase().contains(lower));
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
                                    .placeholder(Core.bundle.get("feature.emoji.search"));
                        });

                scroll().grow().children(() -> {
                    dynamic(filteredEntries, entries -> entries == null || entries.isEmpty()
                            ? row().growX().padding(unit(4)).center().children(() -> {
                                text(Core.bundle.get("feature.emoji.empty")).color(Color.gray);
                            })
                            : wrap().left().gap(unit(1.5f)).children(() -> {
                                for (Entry entry : entries) {
                                    button()
                                            .style(WebStyles.filterChip())
                                            .onClick(() -> copyGlyph(entry))
                                            .padding(unit(1.5f))
                                            .tooltip(entry.name)
                                            .children(() -> {
                                                row().gap(unit(1.5f)).center().children(() -> {
                                                    text(String.valueOf(entry.glyph));
                                                    text(entry.name);
                                                });
                                            });
                                }
                            }))
                                    .grow();
                });
            });
        });
    }

    private void copyGlyph(Entry entry) {
        Core.app.setClipboardText(String.valueOf(entry.glyph));
        Vars.ui.showInfoFade(Core.bundle.format("feature.emoji.copied", entry.name));
    }
}

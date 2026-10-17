package mindustrytool.features.emoji;

import static solim.UI.*;
import static solim.modifier.Values.grow;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.height;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;

import arc.Core;
import arc.graphics.Color;
import arc.struct.Seq;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.emoji.EmojiIcons.Entry;
import solim.modifier.Values;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

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
            column().props(grow(), padding(unit(3)), center(), gap(unit(2.5f))).children(() -> {
                row().props(growX(), padding(unit(1.5f)), Values.rounded(unit(2), WebStyles.Colors.SECONDARY_BG), Values.border(1.5f, WebStyles.Colors.BORDER_INPUT), gap(unit(1.5f)), center())
                        .children(() -> {
                            icon(Icon.zoom).props(size(unit(4.5f)), prefWidth(unit(4.5f)), prefHeight(unit(4.5f))).color(WebStyles.Colors.GHOST_FG);
                            textField(searchQuery)
                                    .props(growX(), height(unit(8)), prefHeight(unit(8)))
                                    .style(WebStyles.clearInput())
                                    .placeholder(Core.bundle.get("feature.emoji.search"));
                        });

                scroll().props(grow()).children(() -> {
                    dynamic(filteredEntries, entries -> {
                        if (entries == null || entries.isEmpty()) {
                            row().props(growX(), padding(unit(4)), center()).children(() -> {
                                text(Core.bundle.get("feature.emoji.empty")).color(Color.gray);
                            });
                        } else {
                            wrap().props(left(), gap(unit(1.5f))).children(() -> {
                                for (Entry entry : entries) {
                                    button()
                                            .style(WebStyles.actionChip())
                                            .onClick(() -> copyGlyph(entry))
                                            .props(padding(unit(1.5f)), tooltip(entry.name))
                                            .children(() -> {
                                                row().props(gap(unit(1.5f)), center()).children(() -> {
                                                    text(String.valueOf(entry.glyph));
                                                    text(entry.name);
                                                });
                                            });
                                }
                            });
                        }
                    }).props(grow());
                });
            });
        });
    }

    private void copyGlyph(Entry entry) {
        Core.app.setClipboardText(String.valueOf(entry.glyph));
        Vars.ui.showInfoFade(Core.bundle.format("feature.emoji.copied", entry.name));
    }
}
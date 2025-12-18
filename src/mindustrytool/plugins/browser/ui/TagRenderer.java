package mindustrytool.plugins.browser.ui;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Align;
import mindustry.ui.Styles;
import mindustrytool.plugins.browser.NetworkImage;
import mindustrytool.plugins.browser.TagData;

public class TagRenderer {

    public static void sortTags(Seq<TagData> tags) {
        if (tags == null)
            return;
        tags.sort((t1, t2) -> {
            String[] priority = { "#394ba0", "#d54799", "#ef4444", "#faa31b" };
            int p1 = 100, p2 = 100;

            if (t1.color() != null) {
                for (int i = 0; i < priority.length; i++) {
                    if (t1.color().equalsIgnoreCase(priority[i])) {
                        p1 = i;
                        break;
                    }
                }
            }
            if (t2.color() != null) {
                for (int i = 0; i < priority.length; i++) {
                    if (t2.color().equalsIgnoreCase(priority[i])) {
                        p2 = i;
                        break;
                    }
                }
            }
            return Integer.compare(p1, p2);
        });
    }

    public static void render(Table table, TagData tag, float scale, Runnable onClick) {
        if (tag == null || tag.name() == null)
            return;

        String tagName = tag.name();
        String colorHex = tag.color();
        String iconUrl = tag.icon();

        Color tagColor = mindustry.graphics.Pal.accent;
        if (colorHex != null && !colorHex.isEmpty()) {
            try {
                tagColor = Color.valueOf(colorHex);
            } catch (Throwable ignored) {
            }
        }
        final Color finalColor = tagColor;

        table.button(b -> {
            b.left();
            if (iconUrl != null && !iconUrl.isEmpty()) {
                try {
                    b.add(new NetworkImage(iconUrl)).size(20 * scale).padRight(4).align(Align.center);
                } catch (Throwable ignored) {
                }
            }

            // Category Display
            if (tag.categoryId() != null && !tag.categoryId().isEmpty()) {
                String cat = mindustrytool.plugins.browser.TagService.getCategoryName(tag.categoryId());
                // Use same color for category as requested
                if (cat != null && !cat.isEmpty()) {
                    b.add(cat).color(finalColor).fontScale(0.9f * scale).padRight(4);
                }
            }

            b.add(tagName).color(finalColor).fontScale(0.9f * scale).align(Align.center);
        }, Styles.flatBordert, onClick)
                .height(36 * scale).pad(2).padRight(4);
    }
}

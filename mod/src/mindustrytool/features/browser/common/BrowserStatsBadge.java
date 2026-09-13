package mindustrytool.features.browser.common;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.Element;
import mindustry.gen.Icon;
import mindustry.ui.Styles;
import solim.core.BaseComponent;

/**
 * Compact badge row showing formatted likes, comments, and downloads counts
 * with vanilla stat icons.
 */
public class BrowserStatsBadge extends BaseComponent {

    private final long likes;
    private final long comments;
    private final long downloads;

    public BrowserStatsBadge(long likes, long comments, long downloads) {
        this.likes = likes;
        this.comments = comments;
        this.downloads = downloads;
    }

    @Override
    protected Element build() {
        return row().gap(unit(1)).children(() -> {
            icon(Icon.upOpenSmall).size(unit(4)).color(Color.scarlet);
            text(formatCount(likes))
                    .style(Styles.defaultLabel)
                    .fontScale(0.85f);

            icon(Icon.chatSmall).size(unit(4)).color(Color.lightGray);
            text(formatCount(comments))
                    .style(Styles.defaultLabel)
                    .fontScale(0.85f);

            icon(Icon.downloadSmall).size(unit(4)).color(Color.sky);
            text(formatCount(downloads))
                    .style(Styles.defaultLabel)
                    .fontScale(0.85f);
        }).element();
    }

    public static String formatCount(long count) {
        if (count >= 1_000_000) {
            return String.format("%.1fM", count / 1_000_000.0);
        }
        if (count >= 1_000) {
            return String.format("%.1fK", count / 1_000.0);
        }
        return String.valueOf(count);
    }
}

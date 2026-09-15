package mindustrytool.features.browser.common;

import static solim.UI.*;

import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
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
        return row().gap(unit(4)).children(() -> {
            row().gap(unit(2)).children(() -> {
                icon(FileIcon.of("heart.png")).size(unit(4));
                text(formatCount(likes));
            });

            row().gap(unit(2)).children(() -> {
                icon(FileIcon.of("message-circle.png")).size(unit(4));
                text(formatCount(comments));
            });

            row().gap(unit(2)).children(() -> {
                icon(Icon.downloadSmall).size(unit(4));
                text(formatCount(downloads));
            });
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

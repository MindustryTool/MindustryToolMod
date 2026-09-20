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
        return row().growX().gap(unit(4)).children(() -> {
            row().gap(unit(2)).center().children(() -> {
                icon(FileIcon.of("heart.png")).size(unit(5));
                text(formatCount(likes));
            });

            row().gap(unit(2)).center().children(() -> {
                icon(FileIcon.of("message-circle.png")).size(unit(5));
                text(formatCount(comments));
            });

            row().gap(unit(2)).center().children(() -> {
                icon(Icon.downloadSmall).size(unit(5));
                text(formatCount(downloads));
            });
        }).element();
    }

    public static String formatCount(long count) {
        if (count >= 1_000_000 || count <= -1_000_000) {
            return oneDecimal(count / 1_000_000.0) + "M";
        }
        if (count >= 1_000 || count <= -1_000) {
            return oneDecimal(count / 1_000.0) + "K";
        }
        return String.valueOf(count);
    }

    /**
     * Uses a locale-invariant decimal point for compact UI counts. Negative
     * half values follow Java Math.round semantics, which is irrelevant for
     * normal non-negative browser statistics but keeps this helper deterministic.
     */
    private static String oneDecimal(double value) {
        long rounded = Math.round(value * 10.0);
        long whole = rounded / 10;
        long fraction = Math.abs(rounded % 10);
        return whole + "." + fraction;
    }
}

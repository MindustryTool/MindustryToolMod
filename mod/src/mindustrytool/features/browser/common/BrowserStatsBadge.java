package mindustrytool.features.browser.common;

import static solim.UI.*;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.size;

import arc.scene.Element;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import solim.core.Component;

public class BrowserStatsBadge extends Component {

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
        return row().props(growX(), gap(unit(4))).children(() -> {
            row().props(gap(unit(2)), center()).children(() -> {
                icon(FileIcon.of("heart.png")).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)));
                text(formatCount(likes));
            });

            row().props(gap(unit(2)), center()).children(() -> {
                icon(FileIcon.of("message-circle.png")).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)));
                text(formatCount(comments));
            });

            row().props(gap(unit(2)), center()).children(() -> {
                icon(Icon.downloadSmall).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5)));
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

    private static String oneDecimal(double value) {
        long rounded = Math.round(value * 10.0);
        long whole = rounded / 10;
        long fraction = Math.abs(rounded % 10);
        return whole + "." + fraction;
    }
}

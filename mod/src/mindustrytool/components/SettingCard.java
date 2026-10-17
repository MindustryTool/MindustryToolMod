package mindustrytool.components;

import static solim.UI.*;
import static solim.modifier.Values.border;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.padding;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.rounded;
import static solim.modifier.Values.size;

import arc.scene.style.Drawable;
import arc.util.Nullable;
import mindustry.graphics.Pal;
import mindustrytool.features.Feature;
import solim.core.Component;
import solim.layout.Card;
import solim.reactive.Readable;

public final class SettingCard {

    private SettingCard() {
    }

    public static Card card() {
        return solim.UI.card()
                .props(growX(), padding(unit(2.5f)), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1f, WebStyles.Colors.SECTION_BORDER), gap(unit(2)));
    }

    public static Component section(@Nullable Drawable icon, String title, Runnable content) {
        return card().children(() -> {
            row().props(growX(), gap(unit(2)), center()).children(() -> {
                if (icon != null) {
                    icon(icon).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))).color(Pal.accent);
                }
                text(title)
                        .color(Pal.accent)
                        .growX()
                        .left();
            });
            content.run();
        });
    }

    public static Component section(String title, Runnable content) {
        return section(null, title, content);
    }

    public static Component header(Feature feature, String badgeText) {
        return header(
                feature.getMetadata() != null ? feature.getMetadata().getIcon() : null,
                feature.getName(),
                badgeText,
                feature.getDescription());
    }

    public static Component header(@Nullable Drawable icon, String name, String badgeText,
            @Nullable String description) {
        return card().children(() -> {
            row().props(growX(), gap(unit(2)), center()).children(() -> {
                if (icon != null) {
                    icon(icon).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))).color(Pal.accent);
                }
                text(name)
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge(badgeText).color(WebStyles.Colors.PRIMARY);
            });

            if (description != null && !description.trim().isEmpty()) {
                text(description)
                        .color(WebStyles.Colors.GHOST_FG)
                        .left()
                        .wrap()
                        .growX();
            }
        });
    }

    public static Component header(@Nullable Drawable icon, String name, Readable<String> badgeText,
            @Nullable String description) {
        return card().children(() -> {
            row().props(growX(), gap(unit(2)), center()).children(() -> {
                if (icon != null) {
                    icon(icon).props(size(unit(5)), prefWidth(unit(5)), prefHeight(unit(5))).color(Pal.accent);
                }
                text(name)
                        .color(Pal.accent)
                        .growX()
                        .left();
                badge(badgeText).color(WebStyles.Colors.PRIMARY);
            });

            if (description != null && !description.trim().isEmpty()) {
                text(description)
                        .color(WebStyles.Colors.GHOST_FG)
                        .left()
                        .wrap()
                        .growX();
            }
        });
    }
}

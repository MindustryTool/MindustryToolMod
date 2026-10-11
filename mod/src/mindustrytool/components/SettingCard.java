package mindustrytool.components;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.util.Nullable;
import mindustry.graphics.Pal;
import mindustrytool.features.Feature;
import solim.core.Component;
import solim.layout.Card;
import solim.reactive.Readable;

/**
 * Standardized settings card builder following the unified modern settings layout specification.
 * Each section card uses padding 2.5 units, gap 2 units, rounded corners, and section border.
 */
public final class SettingCard {

    private SettingCard() {
    }

    /**
     * Creates a base section card with unified styling (padding 2.5 unit, gap 2 unit, rounded, border).
     */
    public static Card card() {
        return solim.UI.card()
                .growX()
                .padding(unit(2.5f))
                .gap(unit(2))
                .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                .border(1f, WebStyles.Colors.SECTION_BORDER);
    }

    /**
     * Creates a standardized section card containing a header row ([Pal.accent] icon + title)
     * followed by child elements.
     */
    public static Component section(@Nullable Drawable icon, String title, Runnable content) {
        return card().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                if (icon != null) {
                    icon(icon).size(unit(5)).color(Pal.accent);
                }
                text(title)
                        .color(Pal.accent)
                        .growX()
                        .left();
            });
            content.run();
        });
    }

    /**
     * Creates a standardized section card containing a header row without icon.
     */
    public static Component section(String title, Runnable content) {
        return section(null, title, content);
    }

    /**
     * Creates a standardized header card for a feature containing:
     * - Row 1: [Pal.accent] icon + feature name, right-aligned category badge
     * - Row 2: Muted feature description
     */
    public static Component header(Feature feature, String badgeText) {
        return header(
                feature.getMetadata() != null ? feature.getMetadata().getIcon() : null,
                feature.getName(),
                badgeText,
                feature.getDescription()
        );
    }

    /**
     * Creates a standardized header card with custom values.
     */
    public static Component header(@Nullable Drawable icon, String name, String badgeText, @Nullable String description) {
        return card().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                if (icon != null) {
                    icon(icon).size(unit(5)).color(Pal.accent);
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

    /**
     * Creates a standardized header card with a reactive category badge.
     */
    public static Component header(@Nullable Drawable icon, String name, Readable<String> badgeText, @Nullable String description) {
        return card().children(() -> {
            row().growX().gap(unit(2)).center().children(() -> {
                if (icon != null) {
                    icon(icon).size(unit(5)).color(Pal.accent);
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

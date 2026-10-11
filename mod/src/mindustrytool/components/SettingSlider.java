package mindustrytool.components;

import static solim.UI.*;

import arc.graphics.Color;
import arc.util.Nullable;
import solim.core.Component;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Standardized settings slider following the unified modern settings layout:
 * - Header row placed strictly ABOVE the slider track (title left, value right).
 * - Formatted value readout rendered without background.
 * - Full-width slider track (.growX()).
 */
public final class SettingSlider {

    private SettingSlider() {
    }

    /**
     * Creates a float setting slider with header and value readout above the track.
     */
    public static Component of(
            String title,
            @Nullable String description,
            Signal<Float> signal,
            float min,
            float max,
            float step,
            Readable<String> formattedValue) {
        return column().growX().gap(unit(1)).left().children(() -> {
            row().growX().center().children(() -> {
                text(title)
                        .color(Color.white)
                        .left();
                spacer();
                text(formattedValue)
                        .color(WebStyles.Colors.GHOST_FG)
                        .right();
            });

            if (description != null && !description.trim().isEmpty()) {
                text(description)
                        .color(WebStyles.Colors.GHOST_FG)
                        .left()
                        .wrap()
                        .growX();
            }

            slider(signal, min, max, step).growX();
        });
    }

    /**
     * Creates a float setting slider without description.
     */
    public static Component of(
            String title,
            Signal<Float> signal,
            float min,
            float max,
            float step,
            Readable<String> formattedValue) {
        return of(title, null, signal, min, max, step, formattedValue);
    }

    /**
     * Creates an integer setting slider with header and value readout above the track.
     */
    public static Component of(
            String title,
            @Nullable String description,
            Signal<Integer> signal,
            int min,
            int max,
            int step,
            Readable<String> formattedValue) {
        return column().growX().gap(unit(1)).left().children(() -> {
            row().growX().center().children(() -> {
                text(title)
                        .color(Color.white)
                        .left();
                spacer();
                text(formattedValue)
                        .color(WebStyles.Colors.GHOST_FG)
                        .right();
            });

            if (description != null && !description.trim().isEmpty()) {
                text(description)
                        .color(WebStyles.Colors.GHOST_FG)
                        .left()
                        .wrap()
                        .growX();
            }

            slider(signal, min, max, step).growX();
        });
    }

    /**
     * Creates an integer setting slider without description.
     */
    public static Component of(
            String title,
            Signal<Integer> signal,
            int min,
            int max,
            int step,
            Readable<String> formattedValue) {
        return of(title, null, signal, min, max, step, formattedValue);
    }
}

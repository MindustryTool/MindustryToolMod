package mindustrytool.components;

import static solim.UI.*;

import arc.graphics.Color;
import arc.util.Nullable;
import solim.core.Component;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class SettingSlider {

    private SettingSlider() {
    }

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

    public static Component of(
            String title,
            Signal<Float> signal,
            float min,
            float max,
            float step,
            Readable<String> formattedValue) {
        return of(title, null, signal, min, max, step, formattedValue);
    }

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

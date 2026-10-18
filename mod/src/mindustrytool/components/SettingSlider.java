package mindustrytool.components;

import static solim.UI.*;
import static solim.modifier.Values.color;
import static solim.modifier.Values.growX;
import static solim.modifier.Values.left;
import static solim.modifier.Values.right;
import static solim.modifier.Values.wrap;

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
        return column().props(growX(), gap(unit(1)), left()).children(() -> {
            row().props(growX(), center()).children(() -> {
                text(title).props(color(Color.white), left());
                spacer();
                text(formattedValue).props(color(WebStyles.Colors.GHOST_FG), right());
            });

            if (description != null && !description.trim().isEmpty()) {
                text(description).props(color(WebStyles.Colors.GHOST_FG), left(), wrap(), growX());
            }

            slider(signal, min, max, step).props(growX());
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
        return column().props(growX(), gap(unit(1)), left()).children(() -> {
            row().props(growX(), center()).children(() -> {
                text(title).props(color(Color.white), left());
                spacer();
                text(formattedValue).props(color(WebStyles.Colors.GHOST_FG), right());
            });

            if (description != null && !description.trim().isEmpty()) {
                text(description).props(color(WebStyles.Colors.GHOST_FG), left(), wrap(), growX());
            }

            slider(signal, min, max, step).props(growX());
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

package mindustrytool.components;

import static solim.UI.*;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.Image;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Scaling;
import arc.util.Time;
import mindustry.graphics.Pal;
import solim.core.BaseComponent;
import solim.core.Component;

/**
 * Animated circular loading spinner utilizing {@code loader-circle.png}.
 * Continuously spins around its center point at ~360 deg/sec.
 */
public class Loader extends BaseComponent {
    private final float size;
    private final @Nullable Color color;
    private final float speed;

    public Loader() {
        this(unit(10), Pal.accent);
    }

    public Loader(float size) {
        this(size, Pal.accent);
    }

    public Loader(float size, @Nullable Color color) {
        this.size = size;
        this.color = color;
        this.speed = 6f;
    }

    public static Component centered() {
        return row().grow().center().children(() -> new Loader());
    }

    public static Component centered(float size) {
        return row().grow().center().children(() -> new Loader(size));
    }

    public static Component centered(float size, @Nullable Color color) {
        return row().grow().center().children(() -> new Loader(size, color));
    }

    @Override
    protected Element build() {
        Image img = new Image(FileIcon.of("loader-circle.png")) {
            @Override
            public float getPrefWidth() {
                return size;
            }

            @Override
            public float getPrefHeight() {
                return size;
            }
        };

        img.setSize(size, size);
        img.setScaling(Scaling.fit);
        if (color != null) {
            img.setColor(color);
        }

        img.update(() -> {
            img.setOrigin(Align.center);
            img.rotation -= speed * Time.delta;
        });

        return img;
    }
}

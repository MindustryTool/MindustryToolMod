package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.scene.Element;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.game.Schematic;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import solim.core.BaseComponent;
import solim.core.SolimToken;
import solim.modifier.PendingCellConfig;
import solim.reactive.Readable;

/**
 * Vanilla schematic preview with a fixed preferred size. Arc Table sizes cells
 * from actor preferred size, so reporting the bound as the pref constrains the
 * preview on every layout pass with no dependence on attach timing. (A previous
 * effect-based cell-pinning approach silently never applied: effects run during
 * lazy build before the image has a parent.) Centering travels on the element
 * as a pending cell config, applied by the framework at attach time.
 */
final class BoundedSchematicImage extends BaseComponent {

    private final Schematic schematic;
    private final @Nullable Readable<Float> size;
    private final float fallback;
    private final int align;

    private @Nullable FixedPreview image;

    BoundedSchematicImage(Schematic schematic, @Nullable Readable<Float> size, float fallback) {
        this(schematic, size, fallback, Align.center);
    }

    BoundedSchematicImage(Schematic schematic, @Nullable Readable<Float> size, float fallback, int align) {
        this.schematic = schematic;
        this.size = size;
        this.fallback = fallback;
        this.align = align;
    }

    @Override
    protected Element build() {
        image = new FixedPreview(schematic);
        PendingCellConfig cell = new PendingCellConfig();
        cell.align = align;
        SolimToken.bind(image, this, cell);
        arc(image);
        effect(() -> {
            if (size != null) {
                size.get();
            }
            if (image != null) {
                image.invalidateHierarchy();
            }
        });
        return image;
    }

    private float bound() {
        Float value = size != null ? size.peek() : null;
        return value != null ? value : fallback;
    }

    private final class FixedPreview extends SchematicImage {

        FixedPreview(Schematic schematic) {
            super(schematic);
            setScaling(Scaling.fit);
            setAlign(Align.center);
        }

        @Override
        public float getPrefWidth() {
            return bound();
        }

        @Override
        public float getPrefHeight() {
            return bound();
        }
    }
}

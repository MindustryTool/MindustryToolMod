package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.scene.Element;
import arc.scene.ui.layout.Scl;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.game.Schematic;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import solim.core.SolimElement;
import solim.reactive.Readable;

final class BoundedSchematicImage extends SolimElement {

    private final Schematic schematic;
    private final @Nullable Readable<Float> size;
    private final float fallback;
    private final int cellAlign;

    private FixedPreview image;

    BoundedSchematicImage(Schematic schematic, @Nullable Readable<Float> size, float fallback) {
        this(schematic, size, fallback, Align.center);
    }

    BoundedSchematicImage(Schematic schematic, @Nullable Readable<Float> size, float fallback, int cellAlign) {
        this.schematic = schematic;
        this.size = size;
        this.fallback = fallback;
        this.cellAlign = cellAlign;
        props(align(cellAlign));
    }

    @Override
    protected Element createElement() {
        image = new FixedPreview(schematic);
        effect(() -> {
            if (size != null) {
                size.get();
            }
            image.invalidate();
        });
        return image;
    }

    private float bound() {
        Float value = size != null ? size.peek() : null;
        return Scl.scl(value != null ? value : fallback);
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

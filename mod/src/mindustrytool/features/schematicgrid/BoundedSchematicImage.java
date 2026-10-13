package mindustrytool.features.schematicgrid;

import static solim.UI.*;

import arc.scene.Element;
import arc.util.Align;
import arc.util.Nullable;
import arc.util.Scaling;
import mindustry.game.Schematic;
import mindustry.ui.dialogs.SchematicsDialog.SchematicImage;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.PendingCellConfig;
import solim.reactive.Readable;

final class BoundedSchematicImage extends Component {

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
                image.invalidate();
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

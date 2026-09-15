package solim.layout;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.util.Nullable;
import arc.util.Scaling;
import solim.core.Component;
import solim.display.SolimImage;
import solim.modifier.ElementConfig;
import solim.signal.Readable;
import solim.modifier.PendingCellConfig;

/**
 * Divider line supporting horizontal (X) and vertical (Y) directions. Uses an
 * Image instead of a Table for lightweight rendering.
 */
public final class Divider implements Component, CellConfig<Divider>, ElementConfig<Divider> {
    private final SolimImage image;
    private final Direction direction;

    public Divider() {
        this(Direction.X);
    }

    public Divider(Direction direction) {
        this.direction = direction != null ? direction : Direction.X;
        Drawable white = (Core.atlas != null && Core.atlas.has("whiteui"))
                ? Core.atlas.drawable("whiteui")
                : null;

        this.image = new SolimImage(white);
        this.image.element().name = "solim-divider";
        this.image.color(Color.darkGray);
        // Stretch (not fit): the source is a square pixel, and fit would shrink it
        // into a centered dot instead of a line filling the cell.
        this.image.scaling(Scaling.stretch);

        if (this.direction == Direction.Y) {
            growY();
            width(1.5f);
            minWidth(1.5f);
            cellPaddingBottom(1);
        } else {
            growX();
            height(1.5f);
            minHeight(1.5f);
            cellPaddingRight(1);
        }
    }

    public Direction direction() {
        return direction;
    }

    public SolimImage solimImage() {
        return image;
    }

    public Image image() {
        return image.image();
    }

    public Divider color(Color color) {
        image.color(color);
        return this;
    }

    public Divider color(Readable<Color> color) {
        image.color(color);
        return this;
    }

    @Override
    public Divider width(float width) {
        image.width(width);
        return this;
    }

    @Override
    public Divider width(@Nullable Readable<Float> width) {
        image.width(width);
        return this;
    }

    @Override
    public Divider height(float height) {
        image.height(height);
        return this;
    }

    @Override
    public Divider height(@Nullable Readable<Float> height) {
        image.height(height);
        return this;
    }

    @Override
    public Element element() {
        return image.element();
    }

    @Override
    public PendingCellConfig cellConfig() {
        return image.cellConfig();
    }

    @Override
    public Divider name(String name) {
        image.name(name);
        return this;
    }

    @Override
    public void dispose() {
        image.dispose();
    }
}

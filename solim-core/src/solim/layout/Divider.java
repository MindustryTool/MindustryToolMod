package solim.layout;

import solim.modifier.CellConfig;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.util.Nullable;
import arc.util.Scaling;
import solim.core.Component;
import solim.display.SolimImage;
import solim.modifier.ElementConfig;
import solim.reactive.Readable;
import solim.modifier.PendingCellConfig;

public final class Divider extends Component implements CellConfig<Divider>, ElementConfig<Divider> {
    private final SolimImage image;
    private final Direction direction;

    public Divider() {
        this(Direction.X);
    }

    public Divider(Direction direction) {
        this(whitePixel(), direction);
    }

    private Divider(@Nullable Drawable white, Direction direction) {
        this(new SolimImage(white), direction);
    }

    private Divider(SolimImage image, Direction direction) {
        super(image.element());
        this.image = image;
        this.direction = direction != null ? direction : Direction.X;
        own(image);
        this.image.element().name = "solim-divider";
        this.image.color(Color.darkGray);

        this.image.scaling(Scaling.stretch);

        if (this.direction == Direction.Y) {
            growY();
            width(1.5f);
            minWidth(1.5f);
            marginBottom(1);
        } else {
            growX();
            height(1.5f);
            minHeight(1.5f);
            marginRight(1);
        }
    }

    private static @Nullable Drawable whitePixel() {
        return Core.atlas != null && Core.atlas.has("whiteui") ? Core.atlas.drawable("whiteui") : null;
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
    public PendingCellConfig cellConfig() {
        return image.cellConfig();
    }

    @Override
    public Divider name(String name) {
        image.name(name);
        return this;
    }

    @Override
    public Divider self() {
        return this;
    }
}

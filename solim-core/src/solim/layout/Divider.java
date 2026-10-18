package solim.layout;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.util.Nullable;
import arc.util.Scaling;
import solim.core.Component;
import solim.core.OwnershipContext;
import solim.display.SolimImage;
import solim.modifier.Configurable;
import solim.modifier.ElementCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.Values;

public final class Divider extends Component
        implements Configurable<Divider>, ElementCapability {
    private final SolimImage image;
    private final Direction direction;
    private @Nullable Props<Divider> pending;

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
        OwnershipContext.push(this);
        try {
            OwnershipContext.register(image);
        } finally {
            OwnershipContext.pop();
        }
        this.image.element().name = "solim-divider";
        this.image.props(Values.color(Color.darkGray), Values.scaling(Scaling.stretch));

        OwnershipContext.push(this);
        try {
            if (this.direction == Direction.Y) {
                props(Values.growY(), Values.width(1.5f), Values.minWidth(1.5f),
                        Values.marginBottom(1f));
            } else {
                props(Values.growX(), Values.height(1.5f), Values.minHeight(1.5f),
                        Values.marginRight(1f));
            }
        } finally {
            OwnershipContext.pop();
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

    @Override
    public PendingCellConfig cellConfig() {
        return image.cellConfig();
    }

    @Override
    public @Nullable Props<Divider> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Divider> props) {
        this.pending = props;
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

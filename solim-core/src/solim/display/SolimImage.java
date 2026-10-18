package solim.display;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Scaling;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ElementCapability;
import solim.modifier.IconCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;

public final class SolimImage extends Component
        implements Configurable<SolimImage>, ElementCapability, CellCapability, AlignmentCapability, IconCapability {

    private final Image element;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<SolimImage> pending;
    private Scaling scaling = Scaling.fit;

    public SolimImage() {
        this((Drawable) null);
    }

    public SolimImage(Drawable d) {
        this(d, Scaling.fit);
    }

    public SolimImage(Drawable d, Scaling scaling) {
        this(new Image(d, scaling), scaling);
    }

    private SolimImage(Image element, Scaling scaling) {
        super(element);
        this.element = element;
        this.scaling = scaling;
        SolimToken.bind(element, this, constraints);
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public SolimImage name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public SolimImage self() {
        return this;
    }

    public static SolimImage of(Drawable d) {
        return new SolimImage(d);
    }

    public static SolimImage of(Signal<Drawable> s) {
        SolimImage img = new SolimImage();
        img.drawable(s);
        return img;
    }

    public SolimImage drawable(Drawable d) {
        element.setDrawable(d);
        return this;
    }

    public SolimImage drawable(Readable<Drawable> d) {
        if (d != null) {
            withinOwnership(() -> Effect.of(() -> element.setDrawable(d.get())));
        }
        return this;
    }

    public SolimImage icon(@Nullable Drawable d) {
        return drawable(d);
    }

    public SolimImage icon(@Nullable Readable<Drawable> d) {
        return drawable(d);
    }

    public void setScaling(Scaling scaling) {
        this.scaling = scaling;
        element.setScaling(scaling);
    }

    public Scaling getScaling() {
        return scaling;
    }

    public Image image() {
        return element;
    }

    @Override
    public @Nullable Table table() {
        return null;
    }

    @Override
    public @Nullable Props<SolimImage> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<SolimImage> props) {
        this.pending = props;
    }
}

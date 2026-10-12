package solim.display;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Scaling;
import solim.core.Component;
import solim.core.SolimToken;
import solim.core.SpacingAware;
import solim.modifier.CellConfig;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;

/** Display widget for drawable content. */
public final class SolimImage extends Component implements SpacingAware, CellConfig<SolimImage>, ElementConfig<SolimImage> {

    private final Image element;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private Scaling scaling = Scaling.fit;

    private float padTop;
    private float padLeft;
    private float padBottom;
    private float padRight;

    private float marginTop;
    private float marginLeft;
    private float marginBottom;
    private float marginRight;

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
            own(Effect.of(() -> element.setDrawable(d.get())));
        }
        return this;
    }

    public SolimImage scaling(Scaling scaling) {
        this.scaling = scaling;
        element.setScaling(scaling);
        return this;
    }

    public Scaling getScaling() {
        return scaling;
    }

    public SolimImage color(Color color) {
        if (color != null) {
            element.setColor(color);
        }
        return this;
    }

    public SolimImage color(Readable<Color> color) {
        if (color != null) {
            own(Effect.of(() -> {
                Color c = color.get();
                if (c != null) {
                    element.setColor(c);
                }
            }));
        }
        return this;
    }

    public SolimImage padding(float p) {
        this.padTop = this.padLeft = this.padBottom = this.padRight = p;
        applySpacing();
        return this;
    }

    public SolimImage padding(float top, float left, float bottom, float right) {
        this.padTop = top;
        this.padLeft = left;
        this.padBottom = bottom;
        this.padRight = right;
        applySpacing();
        return this;
    }

    public SolimImage paddingTop(float top) {
        this.padTop = top;
        applySpacing();
        return this;
    }

    public SolimImage paddingBottom(float bottom) {
        this.padBottom = bottom;
        applySpacing();
        return this;
    }

    public SolimImage paddingLeft(float left) {
        this.padLeft = left;
        applySpacing();
        return this;
    }

    public SolimImage paddingRight(float right) {
        this.padRight = right;
        applySpacing();
        return this;
    }

    public SolimImage paddingX(float x) {
        this.padLeft = this.padRight = x;
        applySpacing();
        return this;
    }

    public SolimImage paddingY(float y) {
        this.padTop = this.padBottom = y;
        applySpacing();
        return this;
    }

    public SolimImage margin(float m) {
        this.marginTop = this.marginLeft = this.marginBottom = this.marginRight = m;
        applySpacing();
        return this;
    }

    public SolimImage margin(float top, float left, float bottom, float right) {
        this.marginTop = top;
        this.marginLeft = left;
        this.marginBottom = bottom;
        this.marginRight = right;
        applySpacing();
        return this;
    }

    public SolimImage marginTop(float top) {
        this.marginTop = top;
        applySpacing();
        return this;
    }

    public SolimImage marginBottom(float bottom) {
        this.marginBottom = bottom;
        applySpacing();
        return this;
    }

    public SolimImage marginLeft(float left) {
        this.marginLeft = left;
        applySpacing();
        return this;
    }

    public SolimImage marginRight(float right) {
        this.marginRight = right;
        applySpacing();
        return this;
    }

    public SolimImage marginX(float x) {
        this.marginLeft = this.marginRight = x;
        applySpacing();
        return this;
    }

    public SolimImage marginY(float y) {
        this.marginTop = this.marginBottom = y;
        applySpacing();
        return this;
    }

    @Override
    public void applySpacing() {
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null) {
                cell.pad(padTop + marginTop, padLeft + marginLeft, padBottom + marginBottom, padRight + marginRight);
            }
        }
    }

    public Image image() {
        return element;
    }

    public SolimImage align(int align) {
        constraints.align = align;
        constraints.applyAlignToParentCell(element);
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.align(align);
        }
        return this;
    }

    public SolimImage top() {
        constraints.alignTop();
        constraints.applyAlignToParentCell(element);
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.top();
        }
        return this;
    }

    public SolimImage bottom() {
        constraints.alignBottom();
        constraints.applyAlignToParentCell(element);
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.bottom();
        }
        return this;
    }

    public SolimImage left() {
        constraints.alignLeft();
        constraints.applyAlignToParentCell(element);
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.left();
        }
        return this;
    }

    public SolimImage right() {
        constraints.alignRight();
        constraints.applyAlignToParentCell(element);
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.right();
        }
        return this;
    }

    public SolimImage center() {
        constraints.alignCenter();
        constraints.applyAlignToParentCell(element);
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.center();
        }
        return this;
    }
}

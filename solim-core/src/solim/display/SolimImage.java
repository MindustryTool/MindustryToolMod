package solim.display;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import solim.core.LeafComponent;
import solim.core.SpacingAware;
import solim.runtime.ComponentContext;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;

/** Display widget for drawable content. */
public final class SolimImage extends LeafComponent<Image, SolimImage> implements SpacingAware {

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
        super(new Image(d, scaling));
        this.scaling = scaling;
    }

    public static SolimImage of(Drawable d) {
        return new SolimImage(d);
    }

    public static SolimImage of(Signal<Drawable> s) {
        SolimImage img = new SolimImage();
        Effect.of(() -> img.element.setDrawable(s.get()));
        return img;
    }

    public SolimImage drawable(Drawable d) {
        element.setDrawable(d);
        return this;
    }

    public SolimImage drawable(Readable<Drawable> d) {
        if (d != null) {
            Effect.of(() -> element.setDrawable(d.get()));
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
            Effect.of(() -> {
                Color c = color.get();
                if (c != null) {
                    element.setColor(c);
                }
            });
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
        return element();
    }

    public SolimImage top() {
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.top();
        }
        return this;
    }

    public SolimImage left() {
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.left();
        }
        return this;
    }

    public SolimImage center() {
        if (element.parent instanceof Table) {
            Cell<?> cell = ((Table) element.parent).getCell(element);
            if (cell != null)
                cell.center();
        }
        return this;
    }
}

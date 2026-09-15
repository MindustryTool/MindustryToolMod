package solim.display;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Scaling;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SpacingAware;
import solim.layout.CellConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.ElementConfig;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;

/** Display widget for drawable content. */
public final class SolimImage implements Component, CellConfig<SolimImage>, ElementConfig<SolimImage>, SpacingAware {

    private final Image image;
    private final List<Disposable> bindings = new ArrayList<>();
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
        this.image = new Image(d, scaling);
        this.image.userObject = this;
        this.image.name = "solim-image-image";
    }

    public static SolimImage of(Drawable d) {
        return new SolimImage(d);
    }

    public static SolimImage of(Signal<Drawable> s) {
        SolimImage img = new SolimImage();
        Effect e = Effect.of(() -> {
            img.image.setDrawable(s.get());
        });
        img.bindings.add(e);
        ComponentContext.register(e);
        return img;
    }

    public SolimImage drawable(Drawable d) {
        image.setDrawable(d);
        return this;
    }

    public SolimImage drawable(Readable<Drawable> d) {
        if (d != null) {
            Effect e = Effect.of(() -> image.setDrawable(d.get()));
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public SolimImage scaling(Scaling scaling) {
        this.scaling = scaling;
        image.setScaling(scaling);
        return this;
    }

    public Scaling getScaling() {
        return scaling;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    public SolimImage color(Color color) {
        if (color != null) {
            image.setColor(color);
        }
        return this;
    }

    public SolimImage color(Readable<Color> color) {
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                if (c != null) {
                    image.setColor(c);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
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

    @Override
    public SolimImage cellPaddingX(float x) {
        this.marginLeft = this.marginRight = x;
        applySpacing();
        return this;
    }

    @Override
    public SolimImage cellPaddingY(float y) {
        this.marginTop = this.marginBottom = y;
        applySpacing();
        return this;
    }

    public void applySpacing() {
        if (image.parent instanceof Table) {
            Cell<?> cell = ((Table) image.parent).getCell(image);
            if (cell != null) {
                cell.pad(padTop + marginTop, padLeft + marginLeft, padBottom + marginBottom, padRight + marginRight);
            }
        }
    }

    public Image image() {
        applySpacing();
        return image;
    }

    @Override
    public Element element() {
        applySpacing();
        return image;
    }

    public SolimImage top() {
        if (image.parent instanceof Table) {
            Cell<?> cell = ((Table) image.parent).getCell(image);
            if (cell != null)
                cell.top();
        }
        return this;
    }

    public SolimImage left() {
        if (image.parent instanceof Table) {
            Cell<?> cell = ((Table) image.parent).getCell(image);
            if (cell != null)
                cell.left();
        }
        return this;
    }

    public SolimImage center() {
        if (image.parent instanceof Table) {
            Cell<?> cell = ((Table) image.parent).getCell(image);
            if (cell != null)
                cell.center();
        }
        return this;
    }

    @Override
    public void dispose() {
        for (Disposable d : bindings) {
            d.dispose();
        }
        bindings.clear();
    }

    @Override
    public SolimImage self() {
        return this;
    }
}

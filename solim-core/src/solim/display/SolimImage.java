package solim.display;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import arc.util.Scaling;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SpacingAware;
import solim.layout.LayoutModifiers;
import solim.layout.SizeConstraints;
import solim.modifier.ElementModifiers;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;

/** Display widget for drawable content. */
public final class SolimImage implements Component, LayoutModifiers<SolimImage>, SpacingAware {

	private final Image image;
	private final List<Disposable> bindings = new ArrayList<>();
	private final SizeConstraints constraints = new SizeConstraints();
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
	public SizeConstraints sizeConstraints() {
		return constraints;
	}

	@Override
	public SolimImage width(float width) {
		constraints.prefWidth = Readable.of(width);
		ElementModifiers.width(image, width);
		constraints.applySizeToParentCell(image);
		return this;
	}

	@Override
	public SolimImage width(@Nullable Readable<Float> width) {
		constraints.prefWidth = width;
		if (width != null) {
			Effect e = Effect.of(() -> {
				Float w = width.get();
				if (w != null) {
					ElementModifiers.width(image, w);
					constraints.applySizeToParentCell(image);
				}
			});
			bindings.add(e);
			ComponentContext.register(e);
		}
		return this;
	}

	@Override
	public SolimImage height(float height) {
		constraints.prefHeight = Readable.of(height);
		ElementModifiers.height(image, height);
		constraints.applySizeToParentCell(image);
		return this;
	}

	@Override
	public SolimImage height(@Nullable Readable<Float> height) {
		constraints.prefHeight = height;
		if (height != null) {
			Effect e = Effect.of(() -> {
				Float h = height.get();
				if (h != null) {
					ElementModifiers.height(image, h);
					constraints.applySizeToParentCell(image);
				}
			});
			bindings.add(e);
			ComponentContext.register(e);
		}
		return this;
	}

	public SolimImage size(float width, float height) {
		width(width);
		height(height);
		return this;
	}

	public SolimImage size(float size) {
		return size(size, size);
	}

	public SolimImage size(@Nullable Readable<Float> size) {
		if (size != null) {
			width(size);
			height(size);
		}
		return this;
	}

	public SolimImage size(@Nullable Readable<Float> width, @Nullable Readable<Float> height) {
		if (width != null) width(width);
		if (height != null) height(height);
		return this;
	}

	public SolimImage growX() {
		image.userObject = "expanding";
		if (image.parent instanceof Table) {
			Cell<?> cell = ((Table) image.parent).getCell(image);
			if (cell != null) {
				cell.growX();
				((Table) image.parent).invalidateHierarchy();
			}
		}
		return this;
	}

	public SolimImage growY() {
		image.userObject = "expanding";
		if (image.parent instanceof Table) {
			Cell<?> cell = ((Table) image.parent).getCell(image);
			if (cell != null) {
				cell.growY();
				((Table) image.parent).invalidateHierarchy();
			}
		}
		return this;
	}

	public SolimImage grow() {
		return growX().growY();
	}

	public SolimImage x(float x) {
		ElementModifiers.x(image, x);
		return this;
	}

	public SolimImage y(float y) {
		ElementModifiers.y(image, y);
		return this;
	}

	public SolimImage position(float x, float y) {
		ElementModifiers.position(image, x, y);
		return this;
	}

	public SolimImage visible(boolean visible) {
		ElementModifiers.visible(image, visible);
		return this;
	}

	public SolimImage visible(@Nullable Readable<Boolean> signal) {
		ElementModifiers.visible(image, signal);
		return this;
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
	public SolimImage marginX(float x) {
		this.marginLeft = this.marginRight = x;
		applySpacing();
		return this;
	}

	@Override
	public SolimImage marginY(float y) {
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

	@Override
	public SolimImage name(String name) {
		ElementModifiers.name(image, name);
		return this;
	}

	@Override
	public void dispose() {
		for (Disposable d : bindings) {
			d.dispose();
		}
		bindings.clear();
	}
}

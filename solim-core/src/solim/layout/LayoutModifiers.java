package solim.layout;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import solim.signal.Readable;

/**
 * Category B — Parent-layout modifiers (and Category C — container modifiers).
 *
 * <p>Mixin interface implemented by Solim layout containers ({@link Row}, {@link Column},
 * {@link Card}, {@link Grid}, {@link Scroll}, etc.). Each method configures how this component
 * behaves inside its <em>parent</em> layout cell, or how the container manages its children.
 *
 * <p>Modifier categories:
 * <ul>
 *   <li><b>Category B — Parent-cell configuration</b>: {@code growX/growY/grow}, {@code margin},
 *       {@code align} — these are applied to the {@link arc.scene.ui.layout.Cell} that the
 *       parent layout allocates for this element.</li>
 *   <li><b>Category A — Element size</b>: {@code width/height/size/minWidth/maxWidth} —
 *       these go into {@link SizeConstraints} and are applied to the cell as preferred/min/max
 *       size constraints. They do NOT directly mutate the element unlike
 *       {@link solim.modifier.ElementModifiers} static methods.</li>
 *   <li><b>Category C — Container defaults</b>: {@code padding/gap/opacity} — stored on the
 *       component's root element and affect how children are laid out.</li>
 * </ul>
 *
 * <p>Contrast with {@link solim.modifier.ElementModifiers}, which is a Category A static
 * utility that mutates an Arc element's own properties directly.
 *
 * <p>Fluent ordering convention:
 * <ol>
 *   <li>Container/self configuration (before {@code children()})</li>
 *   <li>{@code children()} — declare child components</li>
 *   <li>Parent-layout modifiers — {@code grow()}, {@code margin()}, etc. (after
 *       {@code children()})</li>
 * </ol>
 *
 * @param <SELF> the concrete component type, enabling fluent chaining
 */
public interface LayoutModifiers<SELF extends LayoutModifiers<SELF>> {

	/** Returns the {@link SizeConstraints} owned by this component's root element. */
	SizeConstraints sizeConstraints();

	// ---------- preferred size ----------

	/** Sets the preferred width to a static value. */
	default SELF width(float v) {
		sizeConstraints().prefWidth = Readable.of(v);
		if (this instanceof solim.core.Component) {
			arc.scene.Element el = ((solim.core.Component) this).element();
			if (el != null) {
				el.setWidth(Math.max(0f, v));
				sizeConstraints().applySizeToParentCell(el);
			}
		}
		return self();
	}

	/** Sets the preferred width to a reactive value that updates automatically. */
	default SELF width(Readable<Float> v) {
		sizeConstraints().prefWidth = v;
		if (this instanceof solim.core.Component && v != null) {
			arc.scene.Element el = ((solim.core.Component) this).element();
			if (el != null) {
				solim.signal.Effect e = solim.signal.Effect.of(() -> {
					Float val = v.get();
					if (val != null) {
						el.setWidth(Math.max(0f, val));
						sizeConstraints().applySizeToParentCell(el);
					}
				});
				solim.runtime.ComponentContext.register(e);
			}
		}
		return self();
	}

	/** Sets the preferred height to a static value. */
	default SELF height(float v) {
		sizeConstraints().prefHeight = Readable.of(v);
		if (this instanceof solim.core.Component) {
			arc.scene.Element el = ((solim.core.Component) this).element();
			if (el != null) {
				el.setHeight(Math.max(0f, v));
				sizeConstraints().applySizeToParentCell(el);
			}
		}
		return self();
	}

	/** Sets the preferred height to a reactive value that updates automatically. */
	default SELF height(Readable<Float> v) {
		sizeConstraints().prefHeight = v;
		if (this instanceof solim.core.Component && v != null) {
			arc.scene.Element el = ((solim.core.Component) this).element();
			if (el != null) {
				solim.signal.Effect e = solim.signal.Effect.of(() -> {
					Float val = v.get();
					if (val != null) {
						el.setHeight(Math.max(0f, val));
						sizeConstraints().applySizeToParentCell(el);
					}
				});
				solim.runtime.ComponentContext.register(e);
			}
		}
		return self();
	}

	/** Sets both preferred width and height to the same static value. */
	default SELF size(float s) {
		return width(s).height(s);
	}

	/** Sets preferred width and height to separate static values. */
	default SELF size(float w, float h) {
		return width(w).height(h);
	}

	/** Sets both preferred width and height to the same reactive value. */
	default SELF size(Readable<Float> s) {
		return width(s).height(s);
	}

	/** Sets preferred width and height to separate reactive values. */
	default SELF size(Readable<Float> w, Readable<Float> h) {
		return width(w).height(h);
	}

	// ---------- minimum size ----------

	/** Sets the minimum width to a static value. */
	default SELF minWidth(float v) {
		sizeConstraints().minWidth = Readable.of(v);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applySizeToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	/** Sets the minimum width to a reactive value. */
	default SELF minWidth(Readable<Float> v) {
		sizeConstraints().minWidth = v;
		if (this instanceof solim.core.Component && v != null) {
			arc.scene.Element el = ((solim.core.Component) this).element();
			solim.signal.Effect e = solim.signal.Effect.of(() -> {
				sizeConstraints().applySizeToParentCell(el);
			});
			solim.runtime.ComponentContext.register(e);
		}
		return self();
	}

	/** Sets the minimum height to a static value. */
	default SELF minHeight(float v) {
		sizeConstraints().minHeight = Readable.of(v);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applySizeToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	/** Sets the minimum height to a reactive value. */
	default SELF minHeight(Readable<Float> v) {
		sizeConstraints().minHeight = v;
		if (this instanceof solim.core.Component && v != null) {
			arc.scene.Element el = ((solim.core.Component) this).element();
			solim.signal.Effect e = solim.signal.Effect.of(() -> {
				sizeConstraints().applySizeToParentCell(el);
			});
			solim.runtime.ComponentContext.register(e);
		}
		return self();
	}

	// ---------- maximum size ----------

	/** Sets the maximum width to a static value. */
	default SELF maxWidth(float v) {
		sizeConstraints().maxWidth = Readable.of(v);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applySizeToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	/** Sets the maximum width to a reactive value. */
	default SELF maxWidth(Readable<Float> v) {
		sizeConstraints().maxWidth = v;
		if (this instanceof solim.core.Component && v != null) {
			arc.scene.Element el = ((solim.core.Component) this).element();
			solim.signal.Effect e = solim.signal.Effect.of(() -> {
				sizeConstraints().applySizeToParentCell(el);
			});
			solim.runtime.ComponentContext.register(e);
		}
		return self();
	}

	/** Sets the maximum height to a static value. */
	default SELF maxHeight(float v) {
		sizeConstraints().maxHeight = Readable.of(v);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applySizeToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	/** Sets the maximum height to a reactive value. */
	default SELF maxHeight(Readable<Float> v) {
		sizeConstraints().maxHeight = v;
		if (this instanceof solim.core.Component && v != null) {
			arc.scene.Element el = ((solim.core.Component) this).element();
			solim.signal.Effect e = solim.signal.Effect.of(() -> {
				sizeConstraints().applySizeToParentCell(el);
			});
			solim.runtime.ComponentContext.register(e);
		}
		return self();
	}

	// ---------- grow (independent of width/height) ----------

	/**
	 * Marks this component as wanting to grow on the X axis in its parent cell.
	 * Independent from {@code width()} — both can coexist (CSS flex-basis style).
	 */
	default SELF growX() {
		sizeConstraints().growX = true;
		if (this instanceof solim.core.Component) {
			arc.scene.Element el = ((solim.core.Component) this).element();
			if (el != null) {
				if (el.userObject == null) {
					el.userObject = this;
				}
				sizeConstraints().applyGrowToParentCell(el);
			}
		}
		return self();
	}

	/**
	 * Marks this component as wanting to grow on the Y axis in its parent cell.
	 * Independent from {@code height()} — both can coexist.
	 */
	default SELF growY() {
		sizeConstraints().growY = true;
		if (this instanceof solim.core.Component) {
			arc.scene.Element el = ((solim.core.Component) this).element();
			if (el != null) {
				if (el.userObject == null) {
					el.userObject = this;
				}
				sizeConstraints().applyGrowToParentCell(el);
			}
		}
		return self();
	}

	/** Marks this component as wanting to grow on both axes. */
	default SELF grow() {
		return growX().growY();
	}

	// ---------- opacity / alpha ----------

	default SELF opacity(float v) {
		if (this instanceof solim.core.Component) {
			solim.modifier.ElementModifiers.opacity(((solim.core.Component) this).element(), v);
		}
		return self();
	}

	default SELF opacity(Readable<Float> v) {
		if (this instanceof solim.core.Component) {
			solim.modifier.ElementModifiers.opacity(((solim.core.Component) this).element(), v);
		}
		return self();
	}

	default SELF alpha(float v) {
		return opacity(v);
	}

	default SELF alpha(Readable<Float> v) {
		return opacity(v);
	}

	// ---------- alignment ----------

	/**
	 * Centers this component within its parent cell.
	 */
	default SELF center() {
		sizeConstraints().alignCenter();
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyAlignToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	/**
	 * Aligns this component to the top of its parent cell.
	 */
	default SELF top() {
		sizeConstraints().alignTop();
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyAlignToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	/**
	 * Aligns this component to the bottom of its parent cell.
	 */
	default SELF bottom() {
		sizeConstraints().alignBottom();
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyAlignToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	/**
	 * Aligns this component to the left of its parent cell.
	 */
	default SELF left() {
		sizeConstraints().alignLeft();
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyAlignToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	/**
	 * Aligns this component to the right of its parent cell.
	 */
	default SELF right() {
		sizeConstraints().alignRight();
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyAlignToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	// ---------- margin (outer spacing via parent cell pad) ----------

	default SELF margin(float m) {
		return margin(m, m, m, m);
	}

	default SELF margin(float top, float left, float bottom, float right) {
		sizeConstraints().padTop = Readable.of(top);
		sizeConstraints().padLeft = Readable.of(left);
		sizeConstraints().padBottom = Readable.of(bottom);
		sizeConstraints().padRight = Readable.of(right);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF margin(Readable<Float> m) {
		return margin(m, m, m, m);
	}

	default SELF margin(Readable<Float> top, Readable<Float> left, Readable<Float> bottom, Readable<Float> right) {
		sizeConstraints().padTop = top;
		sizeConstraints().padLeft = left;
		sizeConstraints().padBottom = bottom;
		sizeConstraints().padRight = right;
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginTop(float top) {
		sizeConstraints().padTop = Readable.of(top);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginTop(Readable<Float> top) {
		sizeConstraints().padTop = top;
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginBottom(float bottom) {
		sizeConstraints().padBottom = Readable.of(bottom);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginBottom(Readable<Float> bottom) {
		sizeConstraints().padBottom = bottom;
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginLeft(float left) {
		sizeConstraints().padLeft = Readable.of(left);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginLeft(Readable<Float> left) {
		sizeConstraints().padLeft = left;
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginRight(float right) {
		sizeConstraints().padRight = Readable.of(right);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginRight(Readable<Float> right) {
		sizeConstraints().padRight = right;
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginX(float x) {
		sizeConstraints().padLeft = Readable.of(x);
		sizeConstraints().padRight = Readable.of(x);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginX(Readable<Float> x) {
		sizeConstraints().padLeft = x;
		sizeConstraints().padRight = x;
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginY(float y) {
		sizeConstraints().padTop = Readable.of(y);
		sizeConstraints().padBottom = Readable.of(y);
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	default SELF marginY(Readable<Float> y) {
		sizeConstraints().padTop = y;
		sizeConstraints().padBottom = y;
		if (this instanceof solim.core.Component) {
			sizeConstraints().applyMarginToParentCell(((solim.core.Component) this).element());
		}
		return self();
	}

	// ---------- rounded & border ----------

	default SELF rounded(int radius) {
		if (this instanceof solim.core.Component) {
			solim.modifier.ElementModifiers.rounded(((solim.core.Component) this).element(), radius);
		}
		return self();
	}

	default SELF rounded(int radius, Color color) {
		if (this instanceof solim.core.Component) {
			solim.modifier.ElementModifiers.rounded(((solim.core.Component) this).element(), radius, color);
		}
		return self();
	}

	default SELF rounded(int radius, Readable<Color> color) {
		if (this instanceof solim.core.Component) {
			solim.modifier.ElementModifiers.rounded(((solim.core.Component) this).element(), radius, color);
		}
		return self();
	}

	default SELF border(float stroke, Color color) {
		if (this instanceof solim.core.Component) {
			solim.modifier.ElementModifiers.border(((solim.core.Component) this).element(), stroke, color);
		}
		return self();
	}

	default SELF border(float stroke, Readable<Color> color) {
		if (this instanceof solim.core.Component) {
			solim.modifier.ElementModifiers.border(((solim.core.Component) this).element(), stroke, color);
		}
		return self();
	}

	// ---------- background ----------

	default SELF background(Drawable bg) {
		if (this instanceof solim.core.Component) {
			solim.modifier.ElementModifiers.background(((solim.core.Component) this).element(), bg);
		}
		return self();
	}

	default SELF background(Color color) {
		if (this instanceof solim.core.Component) {
			solim.modifier.ElementModifiers.background(((solim.core.Component) this).element(), color);
		}
		return self();
	}

	// ---------- internal ----------

	@SuppressWarnings("unchecked")
	private SELF self() {
		return (SELF) this;
	}
}

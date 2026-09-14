package solim.layout;

import solim.signal.Readable;
import arc.scene.Element;
import solim.core.Component;
import solim.modifier.PendingCellConfig;

/**
 * Parent-cell configuration mixin.
 *
 * <p>
 * Implemented by Solim layout containers ({@link Row}, {@link Column},
 * {@link Card}, {@link Grid}, {@link Scroll}, etc.). Each method configures
 * how this component behaves inside its <em>parent</em> layout cell.
 *
 * <p>
 * CellConfig provides ONLY parent-cell methods:
 * <ul>
 * <li><b>Grow</b>: {@code growX/growY/grow} — marks this component to grow
 * in its parent cell.</li>
 * <li><b>Min/Max Size</b>: {@code minWidth/minHeight/maxWidth/maxHeight} —
 * minimum and maximum size constraints applied to the parent cell.</li>
 * <li><b>Cell padding</b>: {@code cellPadding/cellPaddingTop/...} — outer
 * spacing between this element and the parent cell boundary (applied as
 * {@code Cell.pad()}).</li>
 * </ul>
 *
 * <p>
 * Element-targeted operations (width/height/size, opacity, rounded/border/background)
 * come from {@link solim.modifier.ElementConfig}.
 * Table-targeted operations (alignment, margin, padding, gap) come from
 * {@link solim.modifier.TableConfig}.
 *
 * @param <SELF> the concrete component type, enabling fluent chaining
 */
public interface CellConfig<SELF extends CellConfig<SELF>> {

    /**
     * Returns the {@link solim.modifier.PendingCellConfig} owned by this component's root element.
     */
    PendingCellConfig sizeConstraints();

    // ---------- minimum size ----------

    /** Sets the minimum width to a static value. */
    default SELF minWidth(float v) {
        sizeConstraints().minWidth = Readable.of(v);
        return self();
    }

    /** Sets the minimum width to a reactive value. */
    default SELF minWidth(Readable<Float> v) {
        sizeConstraints().minWidth = v;
        return self();
    }

    /** Sets the minimum height to a static value. */
    default SELF minHeight(float v) {
        sizeConstraints().minHeight = Readable.of(v);
        return self();
    }

    /** Sets the minimum height to a reactive value. */
    default SELF minHeight(Readable<Float> v) {
        sizeConstraints().minHeight = v;
        return self();
    }

    // ---------- maximum size ----------

    /** Sets the maximum width to a static value. */
    default SELF maxWidth(float v) {
        sizeConstraints().maxWidth = Readable.of(v);
        return self();
    }

    /** Sets the maximum width to a reactive value. */
    default SELF maxWidth(Readable<Float> v) {
        sizeConstraints().maxWidth = v;
        return self();
    }

    /** Sets the maximum height to a static value. */
    default SELF maxHeight(float v) {
        sizeConstraints().maxHeight = Readable.of(v);
        return self();
    }

    /** Sets the maximum height to a reactive value. */
    default SELF maxHeight(Readable<Float> v) {
        sizeConstraints().maxHeight = v;
        return self();
    }

    // ---------- grow ----------

    /**
     * Marks this component as wanting to grow on the X axis in its parent cell.
     * Independent from {@code width()} — both can coexist (CSS flex-basis style).
     */
    default SELF growX() {
        sizeConstraints().growX = true;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
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
        if (this instanceof Component) {
            Element el = ((Component) this).element();
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

    // ---------- cell padding (outer spacing via parent cell pad) ----------

    default SELF cellPadding(float p) {
        return cellPadding(p, p, p, p);
    }

    default SELF cellPadding(float top, float left, float bottom, float right) {
        sizeConstraints().padTop = Readable.of(top);
        sizeConstraints().padLeft = Readable.of(left);
        sizeConstraints().padBottom = Readable.of(bottom);
        sizeConstraints().padRight = Readable.of(right);
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPadding(Readable<Float> p) {
        return cellPadding(p, p, p, p);
    }

    default SELF cellPadding(Readable<Float> top, Readable<Float> left, Readable<Float> bottom, Readable<Float> right) {
        sizeConstraints().padTop = top;
        sizeConstraints().padLeft = left;
        sizeConstraints().padBottom = bottom;
        sizeConstraints().padRight = right;
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingTop(float top) {
        sizeConstraints().padTop = Readable.of(top);
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingTop(Readable<Float> top) {
        sizeConstraints().padTop = top;
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingBottom(float bottom) {
        sizeConstraints().padBottom = Readable.of(bottom);
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingBottom(Readable<Float> bottom) {
        sizeConstraints().padBottom = bottom;
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingLeft(float left) {
        sizeConstraints().padLeft = Readable.of(left);
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingLeft(Readable<Float> left) {
        sizeConstraints().padLeft = left;
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingRight(float right) {
        sizeConstraints().padRight = Readable.of(right);
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingRight(Readable<Float> right) {
        sizeConstraints().padRight = right;
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingX(float x) {
        sizeConstraints().padLeft = Readable.of(x);
        sizeConstraints().padRight = Readable.of(x);
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingX(Readable<Float> x) {
        sizeConstraints().padLeft = x;
        sizeConstraints().padRight = x;
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingY(float y) {
        sizeConstraints().padTop = Readable.of(y);
        sizeConstraints().padBottom = Readable.of(y);
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF cellPaddingY(Readable<Float> y) {
        sizeConstraints().padTop = y;
        sizeConstraints().padBottom = y;
        if (this instanceof Component) {
            sizeConstraints().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    // ---------- internal ----------

    @SuppressWarnings("unchecked")
    private SELF self() {
        return (SELF) this;
    }
}

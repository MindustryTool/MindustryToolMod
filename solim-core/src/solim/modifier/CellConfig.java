package solim.modifier;

import solim.reactive.Readable;
import solim.reactive.Effect;
import arc.scene.Element;
import solim.core.Component;
import solim.core.SolimToken;

/**
 * Parent-cell configuration mixin.
 *
 * <p>
 * Implemented by Solim layout containers ({@link Row}, {@link Column},
 * {@link Card}, {@link Grid}, {@link Scroll}, etc.). Each method configures how
 * this component behaves inside its <em>parent</em> layout cell.
 *
 * <p>
 * CellConfig provides ONLY parent-cell methods:
 * <ul>
 * <li><b>Grow</b>: {@code growX/growY/grow} — marks this component to grow in
 * its parent cell.</li>
 * <li><b>Min/Max Size</b>: {@code minWidth/minHeight/maxWidth/maxHeight} —
 * minimum and maximum size constraints applied to the parent cell.</li>
 * <li><b>Margin</b>: {@code margin/marginTop/...} — outer
 * spacing between this element and the parent cell boundary (applied as
 * {@code Cell.pad()}).</li>
 * </ul>
 *
 * <p>
 * Element-targeted operations (width/height/size, opacity,
 * rounded/border/background) come from {@link solim.modifier.ElementConfig}.
 * Table-targeted operations (alignment, margin, padding, gap) come from
 * {@link solim.modifier.TableConfig}.
 *
 * <p>
 * Every reactive sizing and margin method installs an {@code Effect} that calls
 * <b>only its own</b> {@code PendingCellConfig} applier, so each binding tracks
 * exactly one {@link Readable} instead of every constraint at once.
 *
 * @param <SELF> the concrete component type, enabling fluent chaining
 */
public interface CellConfig<SELF extends CellConfig<SELF>> {

    /**
     * Returns the {@link solim.modifier.PendingCellConfig} owned by this
     * component's root element.
     */
    PendingCellConfig cellConfig();

    // ---------- minimum size ----------

    default SELF minWidth(float v) {
        cellConfig().minWidth = Readable.of(v);
        if (this instanceof Component) {
            cellConfig().applyMinWidth(((Component) this).element());
        }
        return self();
    }

    default SELF minWidth(Readable<Float> v) {
        cellConfig().minWidth = v;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyMinWidth(el);
            if (el != null && v != null) {
                Effect e = Effect.of(() -> cellConfig().applyMinWidth(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    default SELF minHeight(float v) {
        cellConfig().minHeight = Readable.of(v);
        if (this instanceof Component) {
            cellConfig().applyMinHeight(((Component) this).element());
        }
        return self();
    }

    default SELF minHeight(Readable<Float> v) {
        cellConfig().minHeight = v;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyMinHeight(el);
            if (el != null && v != null) {
                Effect e = Effect.of(() -> cellConfig().applyMinHeight(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    // ---------- maximum size ----------

    default SELF maxWidth(float v) {
        cellConfig().maxWidth = Readable.of(v);
        if (this instanceof Component) {
            cellConfig().applyMaxWidth(((Component) this).element());
        }
        return self();
    }

    default SELF maxWidth(Readable<Float> v) {
        cellConfig().maxWidth = v;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyMaxWidth(el);
            if (el != null && v != null) {
                Effect e = Effect.of(() -> cellConfig().applyMaxWidth(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    default SELF maxHeight(float v) {
        cellConfig().maxHeight = Readable.of(v);
        if (this instanceof Component) {
            cellConfig().applyMaxHeight(((Component) this).element());
        }
        return self();
    }

    default SELF maxHeight(Readable<Float> v) {
        cellConfig().maxHeight = v;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyMaxHeight(el);
            if (el != null && v != null) {
                Effect e = Effect.of(() -> cellConfig().applyMaxHeight(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    // ---------- grow ----------

    /**
     * Marks this component as wanting to grow on the X axis in its parent cell.
     * Independent from {@code width()} — both can coexist (CSS flex-basis style).
     */
    default SELF growX() {
        cellConfig().growX = true;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            if (el != null) {
                SolimToken.bind(el, (Component) this, cellConfig());
                cellConfig().applyGrowToParentCell(el);
            }
        }
        return self();
    }

    /**
     * Marks this component as wanting to grow on the Y axis in its parent cell.
     * Independent from {@code height()} — both can coexist.
     */
    default SELF growY() {
        cellConfig().growY = true;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            if (el != null) {
                SolimToken.bind(el, (Component) this, cellConfig());
                cellConfig().applyGrowToParentCell(el);
            }
        }
        return self();
    }

    default SELF grow() {
        return growX().growY();
    }

    // ---------- margin (outer spacing via parent cell pad) ----------

    default SELF margin(float m) {
        return margin(m, m, m, m);
    }

    default SELF margin(float top, float left, float bottom, float right) {
        cellConfig().padTop = Readable.of(top);
        cellConfig().padLeft = Readable.of(left);
        cellConfig().padBottom = Readable.of(bottom);
        cellConfig().padRight = Readable.of(right);
        if (this instanceof Component) {
            cellConfig().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF margin(Readable<Float> m) {
        return margin(m, m, m, m);
    }

    default SELF margin(Readable<Float> top, Readable<Float> left, Readable<Float> bottom,
            Readable<Float> right) {
        cellConfig().padTop = top;
        cellConfig().padLeft = left;
        cellConfig().padBottom = bottom;
        cellConfig().padRight = right;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyMarginToParentCell(el);
            if (el != null) {
                Effect e = Effect.of(() -> cellConfig().applyMarginToParentCell(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    default SELF marginTop(float top) {
        cellConfig().padTop = Readable.of(top);
        if (this instanceof Component) {
            cellConfig().applyPadTop(((Component) this).element());
        }
        return self();
    }

    default SELF marginTop(Readable<Float> top) {
        cellConfig().padTop = top;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyPadTop(el);
            if (el != null && top != null) {
                Effect e = Effect.of(() -> cellConfig().applyPadTop(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    default SELF marginBottom(float bottom) {
        cellConfig().padBottom = Readable.of(bottom);
        if (this instanceof Component) {
            cellConfig().applyPadBottom(((Component) this).element());
        }
        return self();
    }

    default SELF marginBottom(Readable<Float> bottom) {
        cellConfig().padBottom = bottom;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyPadBottom(el);
            if (el != null && bottom != null) {
                Effect e = Effect.of(() -> cellConfig().applyPadBottom(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    default SELF marginLeft(float left) {
        cellConfig().padLeft = Readable.of(left);
        if (this instanceof Component) {
            cellConfig().applyPadLeft(((Component) this).element());
        }
        return self();
    }

    default SELF marginLeft(Readable<Float> left) {
        cellConfig().padLeft = left;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyPadLeft(el);
            if (el != null && left != null) {
                Effect e = Effect.of(() -> cellConfig().applyPadLeft(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    default SELF marginRight(float right) {
        cellConfig().padRight = Readable.of(right);
        if (this instanceof Component) {
            cellConfig().applyPadRight(((Component) this).element());
        }
        return self();
    }

    default SELF marginRight(Readable<Float> right) {
        cellConfig().padRight = right;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyPadRight(el);
            if (el != null && right != null) {
                Effect e = Effect.of(() -> cellConfig().applyPadRight(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    default SELF marginX(float x) {
        cellConfig().padLeft = Readable.of(x);
        cellConfig().padRight = Readable.of(x);
        if (this instanceof Component) {
            cellConfig().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF marginX(Readable<Float> x) {
        cellConfig().padLeft = x;
        cellConfig().padRight = x;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyMarginToParentCell(el);
            if (el != null && x != null) {
                Effect e = Effect.of(() -> cellConfig().applyMarginToParentCell(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    default SELF marginY(float y) {
        cellConfig().padTop = Readable.of(y);
        cellConfig().padBottom = Readable.of(y);
        if (this instanceof Component) {
            cellConfig().applyMarginToParentCell(((Component) this).element());
        }
        return self();
    }

    default SELF marginY(Readable<Float> y) {
        cellConfig().padTop = y;
        cellConfig().padBottom = y;
        if (this instanceof Component) {
            Element el = ((Component) this).element();
            cellConfig().applyMarginToParentCell(el);
            if (el != null && y != null) {
                Effect e = Effect.of(() -> cellConfig().applyMarginToParentCell(el));
                ModifierOwnership.own(this, e);
            }
        }
        return self();
    }

    // ---------- internal ----------

    SELF self();
}

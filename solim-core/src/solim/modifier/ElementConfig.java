package solim.modifier;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.graphics.RoundedDrawable;
import solim.layout.CellConfig;
import solim.overlay.Hud;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;

/**
 * Mixin interface for operations that directly mutate an Arc {@link Element}'s own properties.
 *
 * <p>
 * Implementing components provide {@link #element()} to supply their root Arc Element.
 * All methods are default methods that operate on the element returned by {@code element()}.
 *
 * <p>
 * Targets:
 * <ul>
 * <li>{@code width/height/size} — element's size</li>
 * <li>{@code x/y/position} — element's position in local coordinates</li>
 * <li>{@code visible} — element's visibility</li>
 * <li>{@code opacity/alpha} — element's transparency</li>
 * <li>{@code name} — element's debug name</li>
 * <li>{@code rounded/border/background} — element's visual styling</li>
 * <li>{@code draggable} — element drag-to-move integration</li>
 * </ul>
 *
 * @param <SELF> the concrete component type, enabling fluent chaining
 */
public interface ElementConfig<SELF extends ElementConfig<SELF>> {

    /** Returns the root Arc Element for this component. */
    Element element();

    // ---------- size ----------

    /** Sets the element's width and updates the parent cell if attached. */
    default SELF width(float width) {
        Element el = element();
        if (el == null) return self();
        float val = Math.max(0f, width);
        el.setWidth(val);
        if (el.parent instanceof Table) {
            Cell<?> cell = ((Table) el.parent).getCell(el);
            if (cell != null) {
                cell.width(val);
            }
        }
        if (this instanceof CellConfig) {
            ((CellConfig<?>) this).sizeConstraints().prefWidth = Readable.of(val);
        }
        el.invalidateHierarchy();
        return self();
    }

    /** Sets the element's width reactively. */
    default SELF width(@Nullable Readable<Float> width) {
        if (width == null) return self();
        if (this instanceof CellConfig) {
            ((CellConfig<?>) this).sizeConstraints().prefWidth = width;
        }
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Float w = width.get();
                if (w != null) width(w);
            });
            ComponentContext.register(e);
        }
        return self();
    }

    /** Sets the element's height and updates the parent cell if attached. */
    default SELF height(float height) {
        Element el = element();
        if (el == null) return self();
        float val = Math.max(0f, height);
        el.setHeight(val);
        if (el.parent instanceof Table) {
            Cell<?> cell = ((Table) el.parent).getCell(el);
            if (cell != null) {
                cell.height(val);
            }
        }
        if (this instanceof CellConfig) {
            ((CellConfig<?>) this).sizeConstraints().prefHeight = Readable.of(val);
        }
        el.invalidateHierarchy();
        return self();
    }

    /** Sets the element's height reactively. */
    default SELF height(@Nullable Readable<Float> height) {
        if (height == null) return self();
        if (this instanceof CellConfig) {
            ((CellConfig<?>) this).sizeConstraints().prefHeight = height;
        }
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Float h = height.get();
                if (h != null) height(h);
            });
            ComponentContext.register(e);
        }
        return self();
    }

    /** Sets both width and height to the given dimensions. */
    default SELF size(float width, float height) {
        width(width);
        height(height);
        return self();
    }

    /** Sets both width and height to the same value (square). */
    default SELF size(float size) {
        return size(size, size);
    }

    /** Sets both width and height to the same reactive value. */
    default SELF size(@Nullable Readable<Float> size) {
        width(size);
        height(size);
        return self();
    }

    /** Sets width and height to independent reactive values. */
    default SELF size(@Nullable Readable<Float> width, @Nullable Readable<Float> height) {
        width(width);
        height(height);
        return self();
    }

    // ---------- position ----------

    /** Sets the element's x coordinate. */
    default SELF x(float x) {
        Element el = element();
        if (el != null) el.x = x;
        return self();
    }

    /** Sets the element's x coordinate reactively. */
    default SELF x(@Nullable Readable<Float> x) {
        if (x == null) return self();
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Float v = x.get();
                if (v != null) x(v);
            });
            ComponentContext.register(e);
        }
        return self();
    }

    /** Sets the element's y coordinate. */
    default SELF y(float y) {
        Element el = element();
        if (el != null) el.y = y;
        return self();
    }

    /** Sets the element's y coordinate reactively. */
    default SELF y(@Nullable Readable<Float> y) {
        if (y == null) return self();
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Float v = y.get();
                if (v != null) y(v);
            });
            ComponentContext.register(e);
        }
        return self();
    }

    /** Sets the element's position (x, y). */
    default SELF position(float x, float y) {
        x(x);
        y(y);
        return self();
    }

    /** Sets the element's position (x, y) reactively. */
    default SELF position(@Nullable Readable<Float> x, @Nullable Readable<Float> y) {
        x(x);
        y(y);
        return self();
    }

    // ---------- visibility ----------

    /** Sets whether the element is visible. */
    default SELF visible(boolean visible) {
        Element el = element();
        if (el == null) return self();
        el.visible = visible;
        if (el.parent instanceof Table) {
            Table parentTable = (Table) el.parent;
            if (parentTable.userObject instanceof solim.layout.GapContainer) {
                ((solim.layout.GapContainer) parentTable.userObject).respace();
            }
        }
        return self();
    }

    /** Sets element visibility reactively. */
    default SELF visible(@Nullable Readable<Boolean> visible) {
        if (visible == null) return self();
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Boolean v = visible.get();
                visible(Boolean.TRUE.equals(v));
            });
            ComponentContext.register(e);
        }
        return self();
    }

    // ---------- opacity / alpha ----------

    /** Sets the element's opacity (alpha channel of its color). */
    default SELF opacity(float opacity) {
        Element el = element();
        if (el != null) {
            el.color.a = Math.max(0f, Math.min(1f, opacity));
        }
        return self();
    }

    /** Sets the element's opacity reactively. */
    default SELF opacity(@Nullable Readable<Float> opacity) {
        if (opacity == null) return self();
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Float v = opacity.get();
                if (v != null) opacity(v);
            });
            ComponentContext.register(e);
        }
        return self();
    }

    /** Alias for {@link #opacity(float)}. */
    default SELF alpha(float alpha) {
        return opacity(alpha);
    }

    /** Alias for {@link #opacity(Readable)}. */
    default SELF alpha(@Nullable Readable<Float> alpha) {
        return opacity(alpha);
    }

    // ---------- name ----------

    /** Sets the element's debug name. */
    default SELF name(String name) {
        Element el = element();
        if (el != null) el.name = name;
        return self();
    }

    // ---------- draggable ----------

    /** Makes this element draggable to move its parent HUD. */
    default SELF draggable() {
        Hud.makeDraggable(element(), null, null, null);
        return self();
    }

    /** Makes this element draggable with reactive coordinate reporting. */
    default SELF draggable(@Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
        Hud.makeDraggable(element(), null, xSignal, ySignal);
        return self();
    }

    /** Makes this element draggable targeting an explicit HUD. */
    default SELF draggable(@Nullable Hud hud) {
        Hud.makeDraggable(element(), hud, null, null);
        return self();
    }

    /** Makes this element draggable targeting an explicit HUD with reactive coordinate reporting. */
    default SELF draggable(@Nullable Hud hud, @Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
        Hud.makeDraggable(element(), hud, xSignal, ySignal);
        return self();
    }

    // ---------- internal ----------

    @SuppressWarnings("unchecked")
    private SELF self() {
        return (SELF) this;
    }
}

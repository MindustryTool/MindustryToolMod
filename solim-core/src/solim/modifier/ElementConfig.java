package solim.modifier;

import arc.func.Cons;
import arc.scene.Element;
import arc.scene.event.EventListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Label;
import arc.scene.ui.Tooltip;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.layout.CellConfig;
import solim.overlay.Hud;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;
import solim.layout.GapContainer;

/**
 * Mixin interface for operations that directly mutate an Arc {@link Element}'s
 * own properties.
 *
 * <p>
 * Implementing components provide {@link #element()} to supply their root Arc
 * Element. All methods are default methods that operate on the element returned
 * by {@code element()}.
 *
 * <p>
 * Targets:
 * <ul>
 * <li>{@code width/height/size} — element's size</li>
 * <li>{@code x/y/position} — element's position in local coordinates</li>
 * <li>{@code visible} — element's visibility</li>
 * <li>{@code opacity/alpha} — element's transparency</li>
 * <li>{@code name} — element's debug name</li>
 * <li>{@code onClick/stopClickPropagation} — element click handling</li>
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
        if (el == null)
            return self();
        float val = Math.max(0f, width);
        el.setWidth(val);
        if (el.parent instanceof Table) {
            Cell<?> cell = ((Table) el.parent).getCell(el);
            if (cell != null) {
                cell.width(val);
            }
        }
        if (this instanceof CellConfig) {
            ((CellConfig<?>) this).cellConfig().prefWidth = Readable.of(val);
        }
        el.invalidateHierarchy();
        return self();
    }

    /** Sets the element's width reactively. */
    default SELF width(@Nullable Readable<Float> width) {
        if (width == null)
            return self();
        if (this instanceof CellConfig) {
            ((CellConfig<?>) this).cellConfig().prefWidth = width;
        }
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Float w = width.get();
                if (w != null)
                    width(w);
            });
            ComponentContext.register(e);
        }
        return self();
    }

    /** Sets the element's height and updates the parent cell if attached. */
    default SELF height(float height) {
        Element el = element();
        if (el == null)
            return self();
        float val = Math.max(0f, height);
        el.setHeight(val);
        if (el.parent instanceof Table) {
            Cell<?> cell = ((Table) el.parent).getCell(el);
            if (cell != null) {
                cell.height(val);
            }
        }
        if (this instanceof CellConfig) {
            ((CellConfig<?>) this).cellConfig().prefHeight = Readable.of(val);
        }
        el.invalidateHierarchy();
        return self();
    }

    /** Sets the element's height reactively. */
    default SELF height(@Nullable Readable<Float> height) {
        if (height == null)
            return self();
        if (this instanceof CellConfig) {
            ((CellConfig<?>) this).cellConfig().prefHeight = height;
        }
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Float h = height.get();
                if (h != null)
                    height(h);
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
        if (el != null)
            el.x = x;
        return self();
    }

    /** Sets the element's x coordinate reactively. */
    default SELF x(@Nullable Readable<Float> x) {
        if (x == null)
            return self();
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Float v = x.get();
                if (v != null)
                    x(v);
            });
            ComponentContext.register(e);
        }
        return self();
    }

    /** Sets the element's y coordinate. */
    default SELF y(float y) {
        Element el = element();
        if (el != null)
            el.y = y;
        return self();
    }

    /** Sets the element's y coordinate reactively. */
    default SELF y(@Nullable Readable<Float> y) {
        if (y == null)
            return self();
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Float v = y.get();
                if (v != null)
                    y(v);
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
        if (el == null)
            return self();
        el.visible = visible;
        if (el.parent instanceof Table) {
            Table parentTable = (Table) el.parent;
            if (parentTable.userObject instanceof GapContainer) {
                ((GapContainer) parentTable.userObject).respace();
            }
        }
        return self();
    }

    /** Sets element visibility reactively. */
    default SELF visible(@Nullable Readable<Boolean> visible) {
        if (visible == null)
            return self();
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
        if (opacity == null)
            return self();
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Float v = opacity.get();
                if (v != null)
                    opacity(v);
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
        if (el != null)
            el.name = name;
        return self();
    }

    // ---------- click ----------

    /**
     * Makes this element clickable, replacing any previously registered handler.
     * The element becomes touchable automatically. Propagation is not stopped
     * unless {@link #stopClickPropagation(boolean)} is enabled.
     */
    default SELF onClick(Runnable action) {
        Element el = element();
        if (el == null)
            return self();
        ElementClickBinding binding = ElementClickBinding.find(el);
        if (binding == null) {
            binding = new ElementClickBinding();
            el.addListener(binding);
        }
        binding.handler = action;
        el.touchable = Touchable.enabled;
        return self();
    }

    /** Sets whether this element's click listener stops propagation. */
    default SELF stopClickPropagation(boolean stop) {
        Element el = element();
        if (el == null)
            return self();
        ElementClickBinding binding = ElementClickBinding.find(el);
        if (binding != null)
            binding.stop = stop;
        return self();
    }

    // ---------- tooltip ----------

    /** Removes all Tooltip listeners attached to the given element. */
    static void removeTooltips(@Nullable Element el) {
        if (el == null)
            return;
        for (int i = el.getListeners().size - 1; i >= 0; i--) {
            EventListener l = el.getListeners().get(i);
            if (l instanceof Tooltip) {
                el.removeListener(l);
            }
        }
    }

    /**
     * Attaches a static text tooltip to this element, replacing any previous tooltip.
     * Passing {@code null} or an empty string removes existing tooltips.
     */
    default SELF tooltip(@Nullable String tip) {
        if (tip == null || tip.isEmpty()) {
            removeTooltips(element());
            return self();
        }
        return tooltip(t -> t.add(tip));
    }

    /**
     * Attaches a reactive text tooltip to this element, replacing any previous tooltip.
     * The tooltip text updates whenever the signal changes, and its effect is owned
     * by the active component context. Passing {@code null} removes existing tooltips.
     */
    default SELF tooltip(@Nullable Readable<String> tip) {
        if (tip == null) {
            removeTooltips(element());
            return self();
        }
        return tooltip(t -> {
            Label label = new Label("");
            Effect e = Effect.of(() -> label.setText(tip.get() != null ? tip.get() : ""));
            ComponentContext.register(e);
            t.add(label);
        });
    }

    /**
     * Attaches a custom tooltip built with the given container builder, replacing any previous tooltip.
     * Passing {@code null} removes existing tooltips.
     */
    default SELF tooltip(@Nullable Cons<Table> tooltipBuilder) {
        Element el = element();
        if (el == null)
            return self();
        removeTooltips(el);
        if (tooltipBuilder != null) {
            try {
                el.addListener(new Tooltip(tooltipBuilder));
            } catch (Throwable ignored) {
            }
        }
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

    /**
     * Makes this element draggable targeting an explicit HUD with reactive
     * coordinate reporting.
     */
    default SELF draggable(@Nullable Hud hud, @Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
        Hud.makeDraggable(element(), hud, xSignal, ySignal);
        return self();
    }

    default SELF update(Cons<Element> fn) {
        element().update(() -> fn.get(element()));
        return self();
    }

    default SELF origin(int align) {
        element().setOrigin(align);
        return self();
    }

    default SELF rotation(float degree) {
        element().rotation = degree;
        return self();
    }

    // ---------- internal ----------

    SELF self();
}

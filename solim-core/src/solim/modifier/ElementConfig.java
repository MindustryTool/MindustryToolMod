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
import solim.core.Component;
import solim.overlay.Hud;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.layout.GapContainer;

public interface ElementConfig<SELF extends ElementConfig<SELF>> {

    Element element();

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
            ((Component) this).own(e);
        }
        return self();
    }

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
            ((Component) this).own(e);
        }
        return self();
    }

    default SELF size(float width, float height) {
        width(width);
        height(height);
        return self();
    }

    default SELF size(float size) {
        return size(size, size);
    }

    default SELF size(@Nullable Readable<Float> size) {
        width(size);
        height(size);
        return self();
    }

    default SELF size(@Nullable Readable<Float> width, @Nullable Readable<Float> height) {
        width(width);
        height(height);
        return self();
    }

    default SELF x(float x) {
        Element el = element();
        if (el != null)
            el.x = x;
        return self();
    }

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
            ((Component) this).own(e);
        }
        return self();
    }

    default SELF y(float y) {
        Element el = element();
        if (el != null)
            el.y = y;
        return self();
    }

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
            ((Component) this).own(e);
        }
        return self();
    }

    default SELF position(float x, float y) {
        x(x);
        y(y);
        return self();
    }

    default SELF position(@Nullable Readable<Float> x, @Nullable Readable<Float> y) {
        x(x);
        y(y);
        return self();
    }

    default SELF visible(boolean visible) {
        Element el = element();
        if (el == null)
            return self();
        el.visible = visible;
        if (el.parent instanceof Table) {
            GapContainer.respace((Table) el.parent);
        }
        return self();
    }

    default SELF visible(@Nullable Readable<Boolean> visible) {
        if (visible == null)
            return self();
        Element el = element();
        if (el != null) {
            Effect e = Effect.of(() -> {
                Boolean v = visible.get();
                visible(Boolean.TRUE.equals(v));
            });
            ((Component) this).own(e);
        }
        return self();
    }

    default SELF opacity(float opacity) {
        Element el = element();
        if (el != null) {
            el.color.a = Math.max(0f, Math.min(1f, opacity));
        }
        return self();
    }

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
            ((Component) this).own(e);
        }
        return self();
    }

    default SELF alpha(float alpha) {
        return opacity(alpha);
    }

    default SELF alpha(@Nullable Readable<Float> alpha) {
        return opacity(alpha);
    }

    default SELF name(String name) {
        Element el = element();
        if (el != null)
            el.name = name;
        return self();
    }

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

    default SELF stopClickPropagation(boolean stop) {
        Element el = element();
        if (el == null)
            return self();
        ElementClickBinding binding = ElementClickBinding.find(el);
        if (binding != null)
            binding.stop = stop;
        return self();
    }

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

    default SELF tooltip(@Nullable String tip) {
        if (tip == null || tip.isEmpty()) {
            removeTooltips(element());
            return self();
        }
        return tooltip(t -> t.add(tip));
    }

    default SELF tooltip(@Nullable Readable<String> tip) {
        if (tip == null) {
            removeTooltips(element());
            return self();
        }
        return tooltip(t -> {
            Label label = new Label("");
            Effect e = Effect.of(() -> {
                String text = tip.get();
                label.setText(text != null ? text : "");
            });
            ((Component) this).own(e);
            t.add(label);
        });
    }

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

    default SELF draggable() {
        Hud.makeDraggable(element(), null, null, null);
        return self();
    }

    default SELF draggable(@Nullable Signal<Float> xSignal, @Nullable Signal<Float> ySignal) {
        Hud.makeDraggable(element(), null, xSignal, ySignal);
        return self();
    }

    default SELF draggable(@Nullable Hud hud) {
        Hud.makeDraggable(element(), hud, null, null);
        return self();
    }

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

    SELF self();
}

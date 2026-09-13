package solim.display;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.layout.CellConfig;
import solim.layout.SizeConstraints;
import solim.modifier.ElementConfig;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;

/**
 * Compact pill-shaped badge component for notification counters and tag
 * indicators.
 */
public final class Badge implements Component, CellConfig<Badge> {

    private final Table table;
    private final SizeConstraints constraints = new SizeConstraints();
    private final Text label;
    private final List<Disposable> bindings = new ArrayList<>();
    private boolean hideOnZero = false;
    private @Nullable Effect visibilityEffect;
    private @Nullable Readable<Integer> countSignal;

    public Badge(String text) {
        this(Readable.of(text));
    }

    public Badge(Readable<String> text) {
        this.table = new Table();
        this.table.userObject = this;
        this.table.name = "solim-badge-table";
        this.table.center();

        Drawable bg = (Core.atlas != null && Core.atlas.has("whiteui"))
                ? Core.atlas.drawable("whiteui")
                : null;
        if (bg != null) {
            table.setBackground(bg);
            table.setColor(new Color(0.85f, 0.25f, 0.25f, 0.9f));
        }
        ElementConfig.padding(table, 2f, 6f, 2f, 6f);

        this.label = Text.of(text);
        table.add(label.label()).center();

        ComponentContext.register(this);
    }

    public static Badge of(String text) {
        return new Badge(text);
    }

    public static Badge of(Readable<String> text) {
        return new Badge(text);
    }

    public static Badge ofCount(int count) {
        return ofCount(Readable.of(count));
    }

    public static Badge ofCount(Readable<Integer> count) {
        Readable<String> text = count.map(c -> c != null ? String.valueOf(c) : "0");
        Badge badge = new Badge(text);
        badge.countSignal = count;
        badge.hideOnZero(true);
        return badge;
    }

    public Badge hideOnZero(boolean hide) {
        this.hideOnZero = hide;
        updateVisibilityBinding();
        return this;
    }

    public Badge hideOnZero() {
        return hideOnZero(true);
    }

    private void updateVisibilityBinding() {
        if (visibilityEffect != null) {
            visibilityEffect.dispose();
            bindings.remove(visibilityEffect);
            visibilityEffect = null;
        }
        if (countSignal != null) {
            visibilityEffect = Effect.of(() -> {
                Integer c = countSignal.get();
                if (hideOnZero) {
                    table.visible = (c != null && c > 0);
                } else {
                    table.visible = true;
                }
            });
            bindings.add(visibilityEffect);
            ComponentContext.register(visibilityEffect);
        }
    }

    public Badge color(Color color) {
        table.setColor(color);
        return this;
    }

    public Badge textColor(Color color) {
        label.color(color);
        return this;
    }

    public Text text() {
        return label;
    }

    public Table table() {
        return table;
    }

    @Override
    public Element element() {
        return table;
    }

    @Override
    public SizeConstraints sizeConstraints() {
        return constraints;
    }

    public Badge name(String name) {
        ElementConfig.name(table, name);
        return this;
    }

    public Badge rounded(int radius) {
        ElementConfig.rounded(table, radius);
        return this;
    }

    public Badge rounded(int radius, @Nullable Color color) {
        ElementConfig.rounded(table, radius, color);
        return this;
    }

    public Badge rounded(int radius, @Nullable Readable<Color> color) {
        ElementConfig.rounded(table, radius, color);
        return this;
    }

    public Badge border(float stroke, @Nullable Color color) {
        ElementConfig.border(table, stroke, color);
        return this;
    }

    public Badge border(float stroke, @Nullable Readable<Color> color) {
        ElementConfig.border(table, stroke, color);
        return this;
    }

    @Override
    public void dispose() {
        for (Disposable d : bindings) {
            d.dispose();
        }
        bindings.clear();
        label.dispose();
    }
}

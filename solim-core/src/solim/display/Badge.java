package solim.display;

import arc.Core;
import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.LeafComponent;
import solim.modifier.TableConfig;
import solim.runtime.ComponentContext;
import solim.reactive.Effect;
import solim.reactive.Readable;

/**
 * Compact pill-shaped badge component for notification counters and tag
 * indicators.
 */
public final class Badge extends LeafComponent<Table, Badge> implements TableConfig<Badge> {

    private final Text label;
    private boolean hideOnZero = false;
    private @Nullable Effect visibilityEffect;
    private @Nullable Readable<Integer> countSignal;

    public Badge(String text) {
        this(Readable.of(text));
    }

    public Badge(Readable<String> text) {
        super(new Table());
        this.element.name = "solim-badge-table";
        this.element.center();

        Drawable bg = (Core.atlas != null && Core.atlas.has("whiteui"))
                ? Core.atlas.drawable("whiteui")
                : null;
        if (bg != null) {
            element.setBackground(bg);
            element.setColor(new Color(0.85f, 0.25f, 0.25f, 0.9f));
        }
        element.margin(2f, 6f, 2f, 6f);

        this.label = Text.of(text);
        element.add(label.label()).center();
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
            visibilityEffect = null;
        }
        if (countSignal != null) {
            visibilityEffect = Effect.of(() -> {
                Integer c = countSignal.get();
                if (hideOnZero) {
                    element.visible = (c != null && c > 0);
                } else {
                    element.visible = true;
                }
            });
            own(visibilityEffect);
            ComponentContext.register(visibilityEffect);
        }
    }

    public Badge color(Color color) {
        element.setColor(color);
        return this;
    }

    public Badge textColor(Color color) {
        label.color(color);
        return this;
    }

    public Text text() {
        return label;
    }

    @Override
    public Table table() {
        return element;
    }

    @Override
    public void dispose() {
        if (isDisposed()) {
            return;
        }
        label.dispose();
        super.dispose();
    }
}

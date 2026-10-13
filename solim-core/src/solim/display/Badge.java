package solim.display;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.Disposable;
import solim.graphics.RoundedDrawable;
import solim.modifier.CellConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;
import solim.reactive.Effect;
import solim.reactive.Readable;

public final class Badge extends Component implements CellConfig<Badge>, TableConfig<Badge> {

    private final Table element;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final Text label;
    private boolean hideOnZero = false;
    private @Nullable Effect visibilityEffect;
    private @Nullable Readable<Integer> countSignal;

    public Badge(String text) {
        this(Readable.of(text));
    }

    public Badge(Readable<String> text) {
        this(new Table(), text);
    }

    private Badge(Table element, Readable<String> text) {
        super(element);
        this.element = element;
        this.element.name = "solim-badge-table";
        this.element.center();

        rounded(8, new Color(1f, 1f, 1f, 0.12f));
        border(1f, new Color(1f, 1f, 1f, 0.25f));
        element.margin(2f, 8f, 2f, 8f);

        this.label = Text.of(text);
        own(label);
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
        if (text instanceof Disposable) {
            badge.own((Disposable) text);
        }
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
            visibilityEffect = own(Effect.of(() -> {
                Integer c = countSignal.get();
                if (hideOnZero) {
                    element.visible = (c != null && c > 0);
                } else {
                    element.visible = true;
                }
            }));
        }
    }

    public Badge color(Color color) {
        element.setColor(color);
        if (element.getBackground() instanceof RoundedDrawable) {
            RoundedDrawable rd = (RoundedDrawable) element.getBackground();
            if (color != null) {
                rd.fillColor(new Color(color.r, color.g, color.b, 0.16f));
                rd.border(1f, new Color(color.r, color.g, color.b, 0.55f));
                label.color(color);
            }
        }
        return this;
    }

    public Badge textColor(Color color) {
        label.color(color);
        return this;
    }

    public Text text() {
        return label;
    }

    public Badge name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public Table table() {
        return element;
    }

    @Override
    public Badge self() {
        return this;
    }
}

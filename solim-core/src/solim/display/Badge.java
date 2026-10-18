package solim.display;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.graphics.RoundedDrawable;
import solim.modifier.RoundedHelper;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.modifier.TableCapability;
import solim.reactive.Effect;
import solim.reactive.Readable;

public final class Badge extends Component implements Configurable<Badge>, TableCapability, CellCapability, AlignmentCapability {

    private final Table element;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final Text label;
    private @Nullable Props<Badge> pending;
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
        SolimToken.bind(element, this, constraints);

        RoundedDrawable badgeBackground = RoundedHelper.getOrCreateRounded(element, 8);
        badgeBackground.radius(8);
        badgeBackground.fillColor(new Color(1f, 1f, 1f, 0.12f));
        badgeBackground.border(1f, new Color(1f, 1f, 1f, 0.25f));
        element.margin(2f, 8f, 2f, 8f);

        this.label = Text.of(text);
        OwnershipContext.push(this);
        try {
            OwnershipContext.register(label);
        } finally {
            OwnershipContext.pop();
        }
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
        Badge badge = new Badge(String.valueOf(count != null ? count.peek() : 0));
        badge.withinOwnership(() -> {
            Readable<String> text = count.map(c -> c != null ? String.valueOf(c) : "0");
            badge.label.setText(text);
            OwnershipContext.register((Disposable) text);
        });
        badge.countSignal = count;
        badge.setHideOnZero(true);
        return badge;
    }

    public void setHideOnZero(boolean hide) {
        this.hideOnZero = hide;
        updateVisibilityBinding();
    }

    private void updateVisibilityBinding() {
        if (visibilityEffect != null) {
            visibilityEffect.dispose();
            visibilityEffect = null;
        }
        if (countSignal != null) {
            OwnershipContext.push(this);
            try {
                visibilityEffect = Effect.of(() -> {
                    Integer c = countSignal.get();
                    if (hideOnZero) {
                        element.visible = (c != null && c > 0);
                    } else {
                        element.visible = true;
                    }
                });
            } finally {
                OwnershipContext.pop();
            }
        }
    }

    public void setBadgeColor(Color color) {
        element.setColor(color);
        if (element.getBackground() instanceof RoundedDrawable) {
            RoundedDrawable rd = (RoundedDrawable) element.getBackground();
            if (color != null) {
                rd.fillColor(new Color(color.r, color.g, color.b, 0.16f));
                rd.border(1f, new Color(color.r, color.g, color.b, 0.55f));
                label.element().setColor(color);
            }
        }
    }

    public void setTextColor(Color color) {
        label.element().setColor(color);
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
    public @Nullable Props<Badge> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Badge> props) {
        this.pending = props;
    }

    @Override
    public Badge self() {
        return this;
    }
}

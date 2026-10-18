package solim.display;

import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.graphics.RoundedDrawable;
import solim.modifier.RoundedHelper;
import solim.modifier.Configurable;
import solim.modifier.PendingCellConfig;
import solim.modifier.Prop;
import solim.modifier.Props;
import solim.modifier.TableCapability;
import solim.modifier.TextCapability;
import solim.reactive.Readable;

public final class Badge extends Component implements Configurable<Badge>, TableCapability, TextCapability {

    private final Table element;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Text label;
    private @Nullable Props<Badge> pending;
    private boolean hideOnZero = false;
    private @Nullable Readable<Integer> countSignal;

    public Badge(String text) {
        this(Readable.of(text));
    }

    public Badge(@Nullable Readable<String> text) {
        this(new Table(), text);
    }

    private Badge(Table element, @Nullable Readable<String> text) {
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

        props(Prop.elementSource(TextCapability.class, "text", text != null ? text : Readable.of(""),
                (host, value) -> setText(value)));
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
        Badge badge = new Badge("");
        badge.countSignal = count;
        badge.hideOnZero = true;
        badge.props(
                Prop.elementSource(TextCapability.class, "text",
                        count.map(c -> c != null ? String.valueOf(c) : "0"),
                        (element, v) -> badge.setText(v)),
                Prop.tableSource(TableCapability.class, "visibleOnCount", count,
                        (table, v) -> table.visible = !badge.hideOnZero || (v != null && v > 0)));
        return badge;
    }

    public void setHideOnZero(boolean hide) {
        this.hideOnZero = hide;
        if (countSignal != null) {
            applyCountVisibility(countSignal.peek());
        }
    }

    private void applyCountVisibility(@Nullable Integer count) {
        element.visible = !hideOnZero || (count != null && count > 0);
    }

    public void setText(@Nullable String value) {
        label().setText(value != null ? value : "");
    }

    public void setBadgeColor(Color color) {
        element.setColor(color);
        if (element.getBackground() instanceof RoundedDrawable) {
            RoundedDrawable rd = (RoundedDrawable) element.getBackground();
            if (color != null) {
                rd.fillColor(new Color(color.r, color.g, color.b, 0.16f));
                rd.border(1f, new Color(color.r, color.g, color.b, 0.55f));
                label().element().setColor(color);
            }
        }
    }

    public void setTextColor(Color color) {
        label().element().setColor(color);
    }

    public Text text() {
        return label();
    }

    private Text label() {
        Text existing = label;
        if (existing != null) {
            return existing;
        }
        Text created = Text.of("");
        element.add(created.label()).center();
        label = created;
        return created;
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

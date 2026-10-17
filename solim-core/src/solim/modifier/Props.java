package solim.modifier;

import arc.scene.Element;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import solim.core.DisposableOwner;
import solim.reactive.Effect;

/**
 * Recorded property list bound to a host. Building one is pure: no Arc type is
 * resolved and no {@link Effect} is created. Properties are drained inside an
 * ownership window so reactive ones auto-register with the host.
 */
public final class Props<T extends Configurable<T> & DisposableOwner> {

    private final T host;
    private final List<Prop> props;

    public Props(T host, Prop[] props) {
        this.host = host;
        this.props = new ArrayList<>(Arrays.asList(props));
    }

    /**
     * Appends further properties to a still-pending list, so consecutive {@code props(...)}
     * calls on a not-yet-realized host accumulate in call order instead of the later call
     * discarding the earlier one.
     */
    public void append(Prop[] more) {
        props.addAll(Arrays.asList(more));
    }

    public T host() {
        return host;
    }

    public int size() {
        return props.size();
    }

    public T children(@Nullable Runnable block) {
        applyAll();
        return host.children(block);
    }

    public T apply() {
        applyAll();
        return host;
    }

    public void applyAll() {
        for (int i = 0; i < props.size(); i++) {
            Prop prop = props.get(i);
            if (prop.isReactive()) {
                Effect.of(() -> applyOne(prop));
            } else {
                applyOne(prop);
            }
        }
        props.clear();
    }

    /**
     * Records the cell-kind properties into the host's pending cell configuration immediately,
     * without a window and without installing any binding. Cell records are pure data, so a
     * {@code props(...)} call on a not-yet-built host is observable through {@code cellConfig()}
     * right away, exactly like the old chained modifiers. Element and table properties, and
     * the live bindings of reactive cell properties, still drain inside an ownership window.
     */
    public void applyCellRecords() {
        PendingCellConfig config = host.cellConfig();
        if (config == null) {
            return;
        }
        for (int i = 0; i < props.size(); i++) {
            Prop prop = props.get(i);
            if (prop.kind() == Prop.Kind.CELL) {
                prop.writeTo(config);
            } else {
                applyAlignCellRecord(config, prop);
            }
        }
    }

    private void applyAlignCellRecord(PendingCellConfig config, Prop prop) {
        String label = prop.label();
        if ("align".equals(label)) {
            Object val = prop.resolve();
            if (val instanceof Integer) {
                config.align = (Integer) val;
            }
        } else if ("top".equals(label)) {
            config.alignTop();
        } else if ("bottom".equals(label)) {
            config.alignBottom();
        } else if ("left".equals(label)) {
            config.alignLeft();
        } else if ("right".equals(label)) {
            config.alignRight();
        } else if ("center".equals(label)) {
            config.alignCenter();
        }
    }

    private void applyOne(Prop prop) {
        Object target = targetFor(prop);
        prop.writeTo(target);
        if (prop.kind() == Prop.Kind.CELL && target instanceof PendingCellConfig) {
            applyToLiveParentCell((PendingCellConfig) target);
        }
    }

    /**
     * Mirrors the old mixin behavior of applying a cell constraint to the live parent cell
     * when the host element is already attached. Before attachment every helper below is a
     * no-op, and the recorded configuration reaches the cell through the attachment
     * configurator instead.
     */
    private void applyToLiveParentCell(PendingCellConfig config) {
        Element element = host.element();
        config.applySizeToParentCell(element);
        config.applyMarginToParentCell(element);
        config.applyGrowToParentCell(element);
        config.applyAlignToParentCell(element);
    }

    private @Nullable Object targetFor(Prop prop) {
        switch (prop.kind()) {
            case ELEMENT:
                return host.element();
            case TABLE:
                return host.table();
            case CELL:
            default:
                return host.cellConfig();
        }
    }
}

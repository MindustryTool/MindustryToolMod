package solim.modifier;

import arc.func.Cons;
import arc.scene.Element;
import arc.scene.ui.layout.Table;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import solim.core.DisposableOwner;
import solim.core.HostCapability;
import solim.reactive.Effect;

public final class Props<T extends Configurable<T> & DisposableOwner & ElementCapability> {

    private final T host;
    private final List<Prop<? super T, ?>> props;

    public Props(T host, Prop<? super T, ?>[] props) {
        this.host = host;
        this.props = new ArrayList<>(Arrays.asList(props));
    }

    public Props(T host, List<Prop<? super T, ?>> props) {
        this.host = host;
        this.props = new ArrayList<>(props);
    }

    public void append(Prop<? super T, ?>[] more) {
        props.addAll(Arrays.asList(more));
    }

    public void append(List<Prop<? super T, ?>> more) {
        props.addAll(more);
    }

    public T host() {
        return host;
    }

    public int size() {
        return props.size();
    }

    public T apply() {
        applyAll();
        return host;
    }

    public void applyAll() {
        for (int i = 0; i < props.size(); i++) {
            Prop<? super T, ?> prop = props.get(i);
            if (prop.isReactive()) {
                Effect.of(() -> applyOne(prop));
            } else {
                applyOne(prop);
            }
        }
        props.clear();
    }

    public void applyCellRecords() {
        Prop.HostView view = hostView();
        for (int i = 0; i < props.size(); i++) {
            Prop<?, ?> prop = props.get(i);
            if (!prop.requiredCapability().isInstance(host)) {
                throw new IllegalArgumentException(
                        "Property '" + prop.label() + "' requires " + prop.requiredCapability().getSimpleName()
                                + " but host " + host.getClass().getSimpleName() + " does not provide it");
            }
            if (prop.kind() == Prop.Kind.CELL) {
                prop.writeTo(view);
            }
        }
    }

    private void applyOne(Prop<?, ?> prop) {
        if (!prop.requiredCapability().isInstance(host)) {
            throw new IllegalArgumentException(
                    "Property '" + prop.label() + "' requires " + prop.requiredCapability().getSimpleName()
                            + " but host " + host.getClass().getSimpleName() + " does not provide it");
        }
        PendingCellConfig config = host.cellConfig();
        prop.writeTo(hostView());
        if (prop.kind() == Prop.Kind.CELL && config != null) {
            applyToLiveParentCell(config);
        }
    }

    private void applyToLiveParentCell(PendingCellConfig config) {
        Element element = host.element();
        config.applySizeToParentCell(element);
        config.applyMarginToParentCell(element);
        config.applyGrowToParentCell(element);
        config.applyAlignToParentCell(element);
    }

    private Prop.HostView hostView() {
        return new Prop.HostView() {
            @Override
            public void withElement(Cons<Element> action) {
                Element element = host.element();
                if (element != null) {
                    action.get(element);
                }
            }

            @Override
            public void withTable(Cons<Table> action) {
                if (host instanceof TableCapability) {
                    Table table = ((TableCapability) host).table();
                    if (table != null) {
                        action.get(table);
                    }
                }
            }

            @Override
            public void withCell(Cons<PendingCellConfig> action) {
                PendingCellConfig config = host.cellConfig();
                if (config != null) {
                    action.get(config);
                }
            }

            @Override
            public <H extends HostCapability> void withCapability(Class<H> capabilityClass, Cons<H> action) {
                if (capabilityClass.isInstance(host)) {
                    action.get(capabilityClass.cast(host));
                }
            }
        };
    }
}

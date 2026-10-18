package solim.core;

import arc.scene.Element;
import arc.util.Nullable;
import solim.modifier.Configurable;
import solim.modifier.ElementCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;

public abstract class SolimElement extends Component
        implements Configurable<SolimElement>, ElementCapability {

    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<SolimElement> pendingProps;

    protected SolimElement() {
    }

    protected abstract Element createElement();

    @Override
    protected Element build() {
        Element element = createElement();
        SolimToken.bind(element, this, constraints);
        return element;
    }

    @Override
    protected void onRealized(Element element) {
        applyPendingProps();
    }

    @Override
    public SolimElement self() {
        return this;
    }

    @Override
    public SolimElement name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public @Nullable Props<SolimElement> pendingProps() {
        return pendingProps;
    }

    @Override
    public void setPendingProps(@Nullable Props<SolimElement> props) {
        this.pendingProps = props;
    }
}

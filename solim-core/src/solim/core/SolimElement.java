package solim.core;

import arc.scene.Element;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.modifier.Configurable;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;

/**
 * Component that hosts an Arc {@link Element} of its own. A subclass returns that element from
 * {@link #createElement()} and declares its layout with {@code props(...)}; the element binding
 * and the cell configuration stay owned here, so no caller has to reach into the engine.
 *
 * <p>
 * Use it for a leaf element that Arc or Mindustry hands over, such as a drawing element or a
 * preview image. A component whose element is a container should extend the matching container
 * component instead.
 */
public abstract class SolimElement extends Component implements Configurable<SolimElement> {

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
    public SolimElement children(@Nullable Runnable block) {
        applyPendingProps();
        withinOwnership(() -> {
            if (block != null) {
                block.run();
            }
        });
        return this;
    }

    @Override
    public @Nullable Table table() {
        Element element = element();
        return element instanceof Table ? (Table) element : null;
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

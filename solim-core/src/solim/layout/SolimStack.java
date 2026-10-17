package solim.layout;


import arc.func.Func;
import arc.func.Prov;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Stack;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.ElementCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import java.util.List;
import solim.runtime.OwnershipScope;

public final class SolimStack extends Component
        implements Configurable<SolimStack>, ElementCapability, CellCapability, ContainerCapability<SolimStack>, AlignmentCapability {
    private final Stack stack;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<SolimStack> pending;

    public SolimStack() {
        this(new Stack());
    }

    private SolimStack(Stack stack) {
        super(stack);
        this.stack = stack;
        SolimToken.bind(stack, this, constraints);
        stack.name = "solim-stack-stack";
    }

    public Stack stack() {
        return stack;
    }

    public SolimStack center() {
        if (stack.parent instanceof Table) {
            Cell<?> cell = ((Table) stack.parent).getCell(stack);
            if (cell != null)
                cell.center();
        }
        return this;
    }

    public SolimStack add(Element child) {
        if (child != null) {
            stack.add(child);
        }
        return this;
    }

    public SolimStack add(Component child) {
        if (child != null) {
            stack.add(child.element());
        }
        return this;
    }

    public SolimStack layer(@Nullable Prov<Component> Prov) {
        if (isDisposed() || Prov == null) {
            return this;
        }
        attachLayer(OwnershipScope.build(Prov));
        return this;
    }

    public SolimStack layer(@Nullable Func<Element, Component> factory) {
        if (isDisposed() || factory == null) {
            return this;
        }
        attachLayer(OwnershipScope.build(() -> factory.get(stack)));
        return this;
    }

    private void attachLayer(@Nullable Component component) {
        if (component == null) {
            return;
        }
        try {
            Element child = component.element();
            if (child != null && !isDisposed()) {
                stack.add(child);
                adopt(component);
            } else {
                component.dispose();
            }
        } catch (RuntimeException e) {
            component.dispose();
            throw e;
        } catch (Error e) {
            component.dispose();
            throw e;
        }
    }

    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public @Nullable Table table() {
        return null;
    }

    
    @Override
    public SolimStack children(@Nullable Runnable block) {
        applyPendingProps();
        int before = children().size();
        OwnershipContext.push(this);
        try {
            if (block != null) {
                block.run();
            }
        } finally {
            OwnershipContext.pop();
        }
        List<Component> kids = children();
        for (int i = before; i < kids.size(); i++) {
            Element el = kids.get(i).element();
            if (el != null && el.parent != stack) {
                stack.add(el);
            }
        }
        return this;
    }

    @Override
    public @Nullable Props<SolimStack> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<SolimStack> props) {
        this.pending = props;
    }

    public SolimStack growX() {
        constraints.growX = true;
        constraints.applyGrowToParentCell(element());
        return this;
    }

    public SolimStack growY() {
        constraints.growY = true;
        constraints.applyGrowToParentCell(element());
        return this;
    }

    public SolimStack grow() {
        return growX().growY();
    }

    @Override
    public SolimStack name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public SolimStack self() {
        return this;
    }
}

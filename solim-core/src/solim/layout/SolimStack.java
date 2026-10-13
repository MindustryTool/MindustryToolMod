package solim.layout;

import solim.modifier.CellConfig;

import arc.scene.Element;
import arc.scene.ui.layout.Stack;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.ElementConfig;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import solim.modifier.PendingCellConfig;
import solim.runtime.AttachmentStack;
import solim.runtime.OwnershipScope;
import solim.runtime.ReactiveContext;
import arc.func.Func;
import arc.func.Prov;

public final class SolimStack extends Component implements CellConfig<SolimStack>, ElementConfig<SolimStack> {
    private final Stack stack;
    private final PendingCellConfig constraints = new PendingCellConfig();

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
        @Nullable
        Component component = ReactiveContext.untracked(() -> {
            OwnershipScope scope = OwnershipScope.open();
            try {
                Component created = AttachmentStack.isolate(Prov);
                if (created != null) {
                    scope.adoptInto(created);
                }
                return created;
            } finally {
                scope.close();
            }
        });
        attachLayer(component);
        return this;
    }

    public SolimStack layer(@Nullable Func<Element, Component> factory) {
        if (isDisposed() || factory == null) {
            return this;
        }
        @Nullable
        Component component = ReactiveContext.untracked(() -> {
            OwnershipScope scope = OwnershipScope.open();
            try {
                Component created = AttachmentStack.isolate(() -> factory.get(stack));
                if (created != null) {
                    scope.adoptInto(created);
                }
                return created;
            } finally {
                scope.close();
            }
        });
        attachLayer(component);
        return this;
    }

    /**
     * Mounts a layer and hands it to this component. A layer that mounts but turns
     * out to be
     * unusable is disposed here rather than adopted, so ownership only ever holds
     * what is live.
     */
    private void attachLayer(@Nullable Component component) {
        if (component == null) {
            return;
        }
        try {
            Element child = component.element();
            if (child != null && !isDisposed()) {
                stack.add(child);
                own(component);
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

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
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

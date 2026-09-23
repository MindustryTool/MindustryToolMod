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
import solim.runtime.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.runtime.ReactiveContext;
import java.util.ArrayList;
import java.util.List;
import arc.func.Func;
import arc.func.Prov;

/** Stack container: overlays children on top of each other. */
public final class SolimStack implements Component, CellConfig<SolimStack>, ElementConfig<SolimStack> {
    private final Stack stack = new Stack();
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final List<Component> ownedLayers = new ArrayList<>();
    private boolean disposed = false;

    public SolimStack() {
        SolimToken.bind(this.stack, this, constraints);
        this.stack.name = "solim-stack-stack";
        OwnershipContext.register(this);
    }

    public Stack stack() {
        return stack;
    }

    @Override
    public Element element() {
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
        if (disposed || Prov == null) {
            return this;
        }
        @Nullable Component component = ReactiveContext.untracked(() -> AttachmentStack.isolate(Prov));
        attachLayer(component);
        return this;
    }

    public SolimStack layer(@Nullable Func<Element, Component> factory) {
        if (disposed || factory == null) {
            return this;
        }
        @Nullable Component component = ReactiveContext
                .untracked(() -> AttachmentStack.isolate(() -> factory.get(stack)));
        attachLayer(component);
        return this;
    }

    private void attachLayer(@Nullable Component component) {
        if (component == null) {
            return;
        }
        try {
            Element child = component.element();
            if (child != null && !disposed) {
                stack.add(child);
                ownedLayers.add(component);
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
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (int i = ownedLayers.size() - 1; i >= 0; i--) {
            ownedLayers.get(i).dispose();
        }
        ownedLayers.clear();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public SolimStack self() {
        return this;
    }
}
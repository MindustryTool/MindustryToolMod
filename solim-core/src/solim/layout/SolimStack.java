package solim.layout;

import arc.scene.Element;
import arc.scene.ui.layout.Stack;
import solim.core.Component;
import solim.modifier.ElementConfig;

/** Stack container: overlays children on top of each other. */
public final class SolimStack implements Component, CellConfig<SolimStack>, ElementConfig<SolimStack> {
    private final Stack stack = new Stack();
    private final solim.modifier.PendingCellConfig constraints = new solim.modifier.PendingCellConfig();

    public SolimStack() {
        this.stack.name = "solim-stack-stack";
    }

    public Stack stack() {
        return stack;
    }

    @Override
    public Element element() {
        return stack;
    }

    public SolimStack center() {
        if (stack.parent instanceof arc.scene.ui.layout.Table) {
            arc.scene.ui.layout.Cell<?> cell = ((arc.scene.ui.layout.Table) stack.parent).getCell(stack);
            if (cell != null) cell.center();
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

    public SolimStack layer(Runnable r) {
        Row layerRow = solim.runtime.ParentStack.isolate(() -> {
            Row row = new Row();
            row.children(r);
            return row;
        });
        stack.add(layerRow.element());
        return this;
    }

    public SolimStack children(Runnable r) {
        if (r != null) {
            layer(r);
        }
        return this;
    }

    @Override
    public solim.modifier.PendingCellConfig sizeConstraints() {
        return constraints;
    }
}

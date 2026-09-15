package solim.layout;

import arc.scene.Element;
import arc.scene.ui.layout.Stack;
import solim.core.Component;
import solim.modifier.ElementConfig;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import solim.modifier.PendingCellConfig;
import solim.runtime.ParentStack;

/** Stack container: overlays children on top of each other. */
public final class SolimStack implements Component, CellConfig<SolimStack>, ElementConfig<SolimStack> {
    private final Stack stack = new Stack();
    private final PendingCellConfig constraints = new PendingCellConfig();

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

    public SolimStack layer(Runnable r) {
        Row layerRow = ParentStack.isolate(() -> {
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
    public PendingCellConfig cellConfig() {
        return constraints;
    }
}

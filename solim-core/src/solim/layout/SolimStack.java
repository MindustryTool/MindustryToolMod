package solim.layout;

import arc.scene.Element;
import arc.scene.ui.layout.Stack;
import solim.core.Component;
import solim.modifier.ElementConfig;

/** Stack container: overlays children on top of each other. */
public final class SolimStack implements Component, CellConfig<SolimStack> {
    private final Stack stack = new Stack();
    private final SizeConstraints constraints = new SizeConstraints();

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

    public SolimStack name(String name) {
        ElementConfig.name(stack, name);
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
    public SizeConstraints sizeConstraints() {
        return constraints;
    }
}

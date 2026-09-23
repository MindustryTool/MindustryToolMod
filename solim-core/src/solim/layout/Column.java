package solim.layout;
import solim.modifier.CellConfig;

import arc.func.Cons;
import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SolimToken;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;
import solim.runtime.AttachmentStack;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.modifier.PendingCellConfig;
import solim.runtime.OwnershipContext;

/** Column layout — vertical Table wrapper. */
public final class Column
        implements Component, CellConfig<Column>, ElementConfig<Column>, TableConfig<Column>, GapContainer {

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        if (SolimToken.isExpandingChild(child)) {
            cell.growY();
        }
        cell.row();
        GapContainer gc = GapContainer.find(table);
        if (gc != null) {
            GapContainer.spaceAttachedCell(table, cell, Direction.VERTICAL, gc.gap());
        }
        return cell;
    };

    private final Table table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private float gap = 0f;
    private final List<Disposable> bindings = new ArrayList<>();
    private boolean disposed = false;

    public Column() {
        this.table = new Table();
        SolimToken.bind(this.table, this, constraints);
        this.table.name = "solim-column-table";
        this.table.top().left();
        this.table.defaults().top().left();
        OwnershipContext.register(this);
    }

    public Table table() {
        return table;
    }

    @Override
    public Element element() {
        return table;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    public Column fillParent(boolean fillParent) {
        table.setFillParent(fillParent);
        return this;
    }

    public Column fillParent() {
        return fillParent(true);
    }

    public Column touchable(Touchable touchable) {
        table.touchable = touchable;
        return this;
    }

    public Column gap(float g) {
        this.gap = g;
        respace();
        return this;
    }

    public Column gap(@Nullable Readable<Float> gapSignal) {
        if (gapSignal != null) {
            Effect e = Effect.of(() -> {
                Float g = gapSignal.get();
                if (g != null) {
                    gap(g);
                }
            });
            bindings.add(e);
            OwnershipContext.register(e);
        }
        return this;
    }

    @Override
    public Direction direction() {
        return Direction.VERTICAL;
    }

    @Override
    public float gap() {
        return gap;
    }

    @Override
    public void respace() {
        GapContainer.applySpacing(table, Direction.VERTICAL, gap);
    }

    @Override
    public Column top() {
        TableConfig.super.top();
        table.defaults().top();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.top();
        }
        return this;
    }

    @Override
    public Column bottom() {
        TableConfig.super.bottom();
        table.defaults().bottom();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.bottom();
        }
        return this;
    }

    @Override
    public Column left() {
        TableConfig.super.left();
        table.defaults().left();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.left();
        }
        return this;
    }

    @Override
    public Column right() {
        TableConfig.super.right();
        table.defaults().right();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.right();
        }
        return this;
    }

    @Override
    public Column center() {
        TableConfig.super.center();
        cellConfig().alignCenter();
        cellConfig().applyAlignToParentCell(element());
        table.defaults().center();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.center();
        }
        return this;
    }

    public Column align(Align a) {
        switch (a) {
        case START:
        case STRETCH:
            return top();
        case CENTER:
            return center();
        case END:
            return bottom();
        default:
            break;
        }
        return this;
    }

    public Column children(@Nullable Runnable r) {
        AttachmentStack.push(table, ATTACHER);
        try {
            if (r != null) {
                r.run();
            }
        } finally {
            AttachmentStack.pop();
        }
        AttachmentStack.attachToParent(table);
        respace();
        return this;
    }
  
    public Column children(@Nullable Cons<Element> r) {
        AttachmentStack.push(table, ATTACHER);
        try {
            if (r != null) {
                r.get(element());
            }
        } finally {
            AttachmentStack.pop();
        }
        AttachmentStack.attachToParent(table);
        respace();
        return this;
    }

    public Cell<?> add(Element e) {
        Cell<?> cell = table.add(e);
        respace();
        return cell;
    }

    @Override
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (Disposable d : bindings) {
            d.dispose();
        }
        bindings.clear();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public Column self() {
        return this;
    }
}

package solim.layout;
import solim.modifier.CellConfig;

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
import solim.runtime.ParentStack;
import solim.reactive.Readable;
import solim.modifier.PendingCellConfig;
import solim.runtime.ComponentContext;
import solim.reactive.Effect;

/** Row layout — horizontal Table wrapper. */
public final class Row implements Component, CellConfig<Row>, ElementConfig<Row>, TableConfig<Row>, GapContainer {

    public static final ParentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        if (SolimToken.isExpandingChild(child)) {
            cell.growX();
        }
        GapContainer gc = GapContainer.find(table);
        if (gc != null) {
            GapContainer.spaceAttachedCell(table, cell, Direction.HORIZONTAL, gc.gap());
        }
        return cell;
    };

    private final Table table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private float gap = 0f;
    private final List<Disposable> bindings = new ArrayList<>();
    private boolean disposed = false;

    public Row() {
        this.table = new Table();
        SolimToken.bind(this.table, this, constraints);
        this.table.name = "solim-row-table";
        this.table.top().left();
        this.table.defaults().top().left();
        ComponentContext.register(this);
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

    public Row fillParent(boolean fillParent) {
        table.setFillParent(fillParent);
        return this;
    }

    public Row fillParent() {
        return fillParent(true);
    }

    public Row touchable(Touchable touchable) {
        table.touchable = touchable;
        return this;
    }

    public Row gap(float g) {
        this.gap = g;
        respace();
        return this;
    }

    public Row gap(@Nullable Readable<Float> gapSignal) {
        if (gapSignal != null) {
            Effect e = Effect.of(() -> {
                Float g = gapSignal.get();
                if (g != null) {
                    gap(g);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    @Override
    public Direction direction() {
        return Direction.HORIZONTAL;
    }

    @Override
    public float gap() {
        return gap;
    }

    @Override
    public void respace() {
        GapContainer.applySpacing(table, Direction.HORIZONTAL, gap);
    }

    @Override
    public Row top() {
        TableConfig.super.top();
        table.defaults().top();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.top();
        }
        return this;
    }

    @Override
    public Row bottom() {
        TableConfig.super.bottom();
        table.defaults().bottom();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.bottom();
        }
        return this;
    }

    @Override
    public Row left() {
        TableConfig.super.left();
        table.defaults().left();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.left();
        }
        return this;
    }

    @Override
    public Row right() {
        TableConfig.super.right();
        table.defaults().right();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.right();
        }
        return this;
    }

    @Override
    public Row center() {
        TableConfig.super.center();
        table.defaults().center();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.center();
        }
        return this;
    }

    public Row align(Align a) {
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

    public Row children(@Nullable Runnable r) {
        ParentStack.push(table, ATTACHER);
        try {
            if (r != null) {
                r.run();
            }
        } finally {
            ParentStack.pop();
        }
        ParentStack.attachToParent(table);
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
    public Row self() {
        return this;
    }
}

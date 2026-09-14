package solim.layout;

import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;
import solim.runtime.ParentStack;
import solim.signal.Readable;
import solim.ui.Ui;
import solim.modifier.PendingCellConfig;
import solim.runtime.ComponentContext;
import solim.signal.Effect;

/** Row layout — horizontal Table wrapper. */
public final class Row implements Component, CellConfig<Row>, ElementConfig<Row>, TableConfig<Row>, GapContainer {

    public static final ParentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        if (Ui.isExpanding(child)) {
            cell.growX();
        }
        if (table.userObject instanceof GapContainer) {
            GapContainer gc = (GapContainer) table.userObject;
            GapContainer.spaceAttachedCell(table, cell, Direction.HORIZONTAL, gc.gap());
        }
        return cell;
    };

    private final Table table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private float gap = 0f;

    public Row() {
        this.table = new Table();
        this.table.userObject = this;
        this.table.name = "solim-row-table";
        this.table.left();
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

    public Row justify(Justify j) {
        switch (j) {
        case START:
            return left();
        case CENTER:
            return center();
        case END:
            return right();
        case BETWEEN:
        case AROUND:
        case EVENLY:
        default:
            break;
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
}

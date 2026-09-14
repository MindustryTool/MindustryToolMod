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
import solim.signal.Effect;
import solim.signal.Readable;
import solim.ui.Ui;
import solim.modifier.PendingCellConfig;
import solim.runtime.ComponentContext;

/** Column layout — vertical Table wrapper. */
public final class Column implements Component, CellConfig<Column>, ElementConfig<Column>, TableConfig<Column>, GapContainer {

    public static final ParentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        if (Ui.isExpanding(child)) {
            cell.growY();
        }
        cell.row();
        if (table.userObject instanceof GapContainer) {
            GapContainer gc = (GapContainer) table.userObject;
            GapContainer.spaceAttachedCell(table, cell, Direction.VERTICAL, gc.gap());
        }
        return cell;
    };

    private final Table table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private float gap = 0f;

    public Column() {
        this.table = new Table();
        this.table.userObject = this;
        this.table.name = "solim-column-table";
        this.table.top().left();
    }

    public Table table() {
        return table;
    }

    @Override
    public Element element() {
        return table;
    }

    @Override
    public PendingCellConfig sizeConstraints() {
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
            ComponentContext.register(e);
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
        sizeConstraints().alignCenter();
        sizeConstraints().applyAlignToParentCell(element());
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

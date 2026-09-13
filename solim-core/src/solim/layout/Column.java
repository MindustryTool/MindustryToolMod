package solim.layout;

import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.modifier.ElementConfig;
import solim.runtime.ComponentContext;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.runtime.ParentStack;
import solim.ui.Ui;

/** Column layout — vertical Table wrapper. */
public final class Column implements Component, CellConfig<Column>, GapContainer {

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
    private final SizeConstraints constraints = new SizeConstraints();
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
    public SizeConstraints sizeConstraints() {
        return constraints;
    }

    public Column name(String name) {
        ElementConfig.name(table, name);
        return this;
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

    public Column background(@Nullable Drawable bg) {
        ElementConfig.background(table, bg);
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

    public Column padding(float p) {
        ElementConfig.padding(table, p);
        return this;
    }

    public Column padding(float top, float left, float bottom, float right) {
        ElementConfig.padding(table, top, left, bottom, right);
        return this;
    }

    public Column paddingTop(float top) {
        ElementConfig.paddingTop(table, top);
        return this;
    }

    public Column paddingBottom(float bottom) {
        ElementConfig.paddingBottom(table, bottom);
        return this;
    }

    public Column paddingLeft(float left) {
        ElementConfig.paddingLeft(table, left);
        return this;
    }

    public Column paddingRight(float right) {
        ElementConfig.paddingRight(table, right);
        return this;
    }

    public Column margin(float m) {
        ElementConfig.margin(table, m);
        return this;
    }

    public Column margin(float top, float left, float bottom, float right) {
        ElementConfig.margin(table, top, left, bottom, right);
        return this;
    }

    public Column marginTop(float top) {
        ElementConfig.marginTop(table, top);
        return this;
    }

    public Column marginBottom(float bottom) {
        ElementConfig.marginBottom(table, bottom);
        return this;
    }

    public Column marginLeft(float left) {
        ElementConfig.marginLeft(table, left);
        return this;
    }

    public Column marginRight(float right) {
        ElementConfig.marginRight(table, right);
        return this;
    }

    public Column paddingX(float x) {
        ElementConfig.paddingX(table, x);
        return this;
    }

    public Column paddingY(float y) {
        ElementConfig.paddingY(table, y);
        return this;
    }

    @Override
    public Column cellPaddingX(float x) {
        ElementConfig.marginX(table, x);
        return this;
    }

    @Override
    public Column cellPaddingY(float y) {
        ElementConfig.marginY(table, y);
        return this;
    }

    public Column x(float x) {
        ElementConfig.x(table, x);
        return this;
    }

    public Column y(float y) {
        ElementConfig.y(table, y);
        return this;
    }

    public Column position(float x, float y) {
        ElementConfig.position(table, x, y);
        return this;
    }

    public Column visible(boolean visible) {
        ElementConfig.visible(table, visible);
        return this;
    }

    public Column visible(@Nullable Readable<Boolean> visible) {
        ElementConfig.visible(table, visible);
        return this;
    }

    @Override
    public Column top() {
        ElementConfig.top(table);
        table.defaults().top();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.top();
        }
        return CellConfig.super.top();
    }

    @Override
    public Column bottom() {
        ElementConfig.bottom(table);
        table.defaults().bottom();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.bottom();
        }
        return CellConfig.super.bottom();
    }

    @Override
    public Column left() {
        ElementConfig.left(table);
        table.defaults().left();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.left();
        }
        return CellConfig.super.left();
    }

    @Override
    public Column right() {
        ElementConfig.right(table);
        table.defaults().right();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.right();
        }
        return CellConfig.super.right();
    }

    @Override
    public Column center() {
        ElementConfig.center(table);
        table.defaults().center();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.center();
        }
        return CellConfig.super.center();
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

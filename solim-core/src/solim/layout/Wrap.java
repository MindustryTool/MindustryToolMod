package solim.layout;

import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WrapTable;
import arc.util.Nullable;
import solim.core.Component;
import solim.modifier.ElementModifiers;
import solim.runtime.ParentStack;
import solim.ui.Ui;

/**
 * Wrap container: lays out children in a row that wraps when the row is full.
 * Children receive growX() by default so WrapTable receives the full available width.
 */
public final class Wrap implements Component, LayoutModifiers<Wrap>, GapContainer {

    public static final ParentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        if (Ui.isExpanding(child)) {
            cell.growX();
        }
        return cell;
    };

    private final WrapTable table = new WrapTable();
    private final SizeConstraints constraints = new SizeConstraints();
    private float gap = 4f;

    public Wrap() {
        this.table.name = "solim-wrap-table";
        this.table.userObject = this;
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
    public SizeConstraints sizeConstraints() {
        return constraints;
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
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.padRight(gap).padBottom(gap);
            }
        }
        table.defaults().padRight(gap).padBottom(gap);
    }

    public Wrap name(String name) {
        ElementModifiers.name(table, name);
        return this;
    }

    public Wrap gap(float g) {
        this.gap = g;
        respace();
        return this;
    }

    public Wrap background(@Nullable Drawable bg) {
        table.background(bg);
        return this;
    }

    public Wrap padding(float p) {
        ElementModifiers.padding(table, p);
        return this;
    }

    public Wrap padding(float top, float left, float bottom, float right) {
        ElementModifiers.padding(table, top, left, bottom, right);
        return this;
    }

    public Wrap left() {
        table.left();
        table.defaults().left();
        for (Cell<?> c : table.getCells()) {
            if (c != null) c.left();
        }
        return LayoutModifiers.super.left();
    }

    public Wrap right() {
        table.right();
        table.defaults().right();
        for (Cell<?> c : table.getCells()) {
            if (c != null) c.right();
        }
        return LayoutModifiers.super.right();
    }

    public Wrap center() {
        table.center();
        table.defaults().center();
        for (Cell<?> c : table.getCells()) {
            if (c != null) c.center();
        }
        return LayoutModifiers.super.center();
    }

    public Wrap children(@Nullable Runnable r) {
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

    public Wrap add(Element child) {
        table.add(child);
        respace();
        return this;
    }
}

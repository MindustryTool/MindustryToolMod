package solim.layout;
import solim.modifier.CellConfig;

import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.core.Ui;
import solim.modifier.PendingCellConfig;

/**
 * Clickable card container built on a single Arc Table with lazy click
 * handling borrowed from Arc Button semantics.
 */
public final class Card implements Component, CellConfig<Card>, ElementConfig<Card>, TableConfig<Card>, GapContainer {

    public static final ParentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        cell.top().left();
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

    private final Table table = new Table();
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final List<Disposable> bindings = new ArrayList<>();
    private boolean disposed = false;
    private float gap = 0f;

    public Card() {
        this.table.userObject = this;
        this.table.name = "solim-card-table";
        this.table.top().left();
        this.table.defaults().top().left();
        ComponentContext.register(this);
    }

    public Card(@Nullable Drawable background) {
        this();
        if (background != null) {
            background(background);
        }
    }

    public static Card of(@Nullable Runnable children) {
        return new Card();
    }

    public Table container() {
        return table;
    }

    @Override
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

    public Card color(@Nullable Color color) {
        if (color != null) {
            table.setColor(color);
        }
        return this;
    }

    public Card color(@Nullable Readable<Color> color) {
        if (color != null) {
            Effect e = Effect.of(() -> {
                Color c = color.get();
                if (c != null) {
                    table.setColor(c);
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public Card children(@Nullable Runnable r) {
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

    public Card gap(float g) {
        this.gap = g;
        respace();
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
    public Card self() {
        return this;
    }
}

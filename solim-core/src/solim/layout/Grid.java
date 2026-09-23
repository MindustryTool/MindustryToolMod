package solim.layout;
import solim.modifier.CellConfig;

import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SolimToken;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;
import solim.runtime.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.modifier.PendingCellConfig;

/** Simple grid with fixed or reactive column count and customizable gap. */
public final class Grid implements Component, CellConfig<Grid>, GapContainer, ElementConfig<Grid>, TableConfig<Grid> {

    private final Table table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private int columns = 1;
    private float gap = 4f;
    private int currentCell = 0;
    private final List<Disposable> bindings = new ArrayList<>();
    private boolean disposed = false;

    public Grid() {
        this.table = new Table();
        SolimToken.bind(this.table, this, constraints);
        this.table.name = "solim-grid-table";
        this.table.top().left();
        this.table.defaults().top().left();
        respace();
        OwnershipContext.register(this);
    }

    public Grid(int columns) {
        this();
        this.columns = Math.max(1, columns);
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

    public Grid columns(int c) {
        this.columns = Math.max(1, c);
        reflow();
        return this;
    }

    public Grid columns(@Nullable Readable<Integer> c) {
        if (c != null) {
            Effect e = Effect.of(() -> {
                Integer cols = c.get();
                if (cols != null) {
                    this.columns = Math.max(1, cols);
                    reflow();
                }
            });
            bindings.add(e);
            OwnershipContext.register(e);
        }
        return this;
    }

    public Grid gap(float g) {
        this.gap = g;
        respace();
        return this;
    }

    public Grid gap(@Nullable Readable<Float> gapSignal) {
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
        return Direction.HORIZONTAL;
    }

    @Override
    public float gap() {
        return gap;
    }

    @Override
    public void respace() {
        GapContainer.applyGridSpacing(table, columns, gap);
    }

    public Grid background(@Nullable Drawable bg) {
        table.background(bg);
        return this;
    }

    public Grid children(@Nullable Runnable r) {
        int[] count = new int[] { 0 };
        AttachmentStack.push(table, (tbl, child) -> {
            Cell<?> cell = tbl.add(child);
            cell.top().left();
            if (SolimToken.isExpandingChild(child)) {
                cell.growX().fillX();
            }
            cell.uniformX();
            if (++count[0] % Math.max(1, columns) == 0) {
                tbl.row();
            }
            respace();
            return cell;
        });
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

    private void reflow() {
        if (table.getChildren().size == 0)
            return;
        Seq<Element> children = new Seq<>(table.getChildren());
        table.clear();
        int col = 0;
        for (Element child : children) {
            Cell<?> cell = table.add(child);
            cell.top().left();
            if (SolimToken.isExpandingChild(child)) {
                cell.growX().fillX();
            }
            cell.uniformX();
            if (++col % Math.max(1, columns) == 0) {
                table.row();
            }
        }
        respace();
        table.invalidateHierarchy();
    }

    /** Add a child element to the grid; wraps to next row when columns exceeded. */
    public Grid add(Element child) {
        Cell<?> cell = table.add(child);
        cell.top().left();
        if (SolimToken.isExpandingChild(child)) {
            cell.growX().fillX();
        }
        cell.uniformX();
        currentCell++;
        if (currentCell >= columns) {
            table.row();
            currentCell = 0;
        }
        respace();
        return this;
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
    public Grid self() {
        return this;
    }
}

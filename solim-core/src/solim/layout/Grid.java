package solim.layout;
import solim.modifier.CellConfig;

import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.struct.Seq;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.modifier.PendingCellConfig;

/** Simple grid with fixed or reactive column count and customizable gap. */
public final class Grid extends Component implements CellConfig<Grid>, GapContainer, ElementConfig<Grid>, TableConfig<Grid> {

    private final Table table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private int columns = 1;
    private float gap = 4f;
    private int currentCell = 0;
    private boolean attached = false;

    public Grid() {
        this(new Table());
    }

    private Grid(Table table) {
        super(table);
        this.table = table;
        SolimToken.bind(this.table, this, constraints);
        this.table.name = "solim-grid-table";
        this.table.top().left();
        this.table.defaults().top().left();
        respace();
    }

    public Grid(int columns) {
        this();
        this.columns = Math.max(1, columns);
    }

    public Table table() {
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
        own(Effect.of(() -> {
                Integer cols = c.get();
                if (cols != null) {
                    this.columns = Math.max(1, cols);
                    reflow();
                }
            }));
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
        own(Effect.of(() -> {
                Float g = gapSignal.get();
                if (g != null) {
                    gap(g);
                }
            }));
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
        OwnershipContext.push(this);
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
            OwnershipContext.pop();
        }
        attachSelf();
        respace();
        return this;
    }

    private void attachSelf() {
        if (!attached) {
            attached = true;
            AttachmentStack.attachToParent(table);
        }
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
    public Grid align(int align) {
        TableConfig.super.align(align);
        constraints.align = align;
        constraints.applyAlignToParentCell(element());
        table.defaults().align(align);
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.align(align);
            }
        }
        return this;
    }

    @Override
    public Grid center() {
        TableConfig.super.center();
        constraints.alignCenter();
        constraints.applyAlignToParentCell(element());
        table.defaults().center();
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.center();
            }
        }
        return this;
    }

    @Override
    public Grid top() {
        TableConfig.super.top();
        constraints.alignTop();
        constraints.applyAlignToParentCell(element());
        table.defaults().top();
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.top();
            }
        }
        return this;
    }

    @Override
    public Grid bottom() {
        TableConfig.super.bottom();
        constraints.alignBottom();
        constraints.applyAlignToParentCell(element());
        table.defaults().bottom();
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.bottom();
            }
        }
        return this;
    }

    @Override
    public Grid left() {
        TableConfig.super.left();
        constraints.alignLeft();
        constraints.applyAlignToParentCell(element());
        table.defaults().left();
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.left();
            }
        }
        return this;
    }

    @Override
    public Grid right() {
        TableConfig.super.right();
        constraints.alignRight();
        constraints.applyAlignToParentCell(element());
        table.defaults().right();
        for (Cell<?> c : table.getCells()) {
            if (c != null) {
                c.right();
            }
        }
        return this;
    }

    @Override
    public Grid name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public Grid self() {
        return this;
    }
}

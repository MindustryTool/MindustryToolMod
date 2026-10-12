package solim.layout;
import solim.modifier.CellConfig;

import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WrapTable;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.ElementConfig;
import solim.modifier.TableConfig;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.modifier.PendingCellConfig;

/**
 * Wrap container: lays out children in a row that wraps when the row is full.
 * Children receive growX() by default so WrapTable receives the full available
 * width.
 */
public final class Wrap extends Component implements CellConfig<Wrap>, ElementConfig<Wrap>, TableConfig<Wrap>,
        GapContainer {

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        if (SolimToken.isExpandingChild(child)) {
            cell.growX();
        }
        return cell;
    };

    private final WrapTable table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private float gap = 4f;
    private boolean attached = false;

    public Wrap() {
        this(new WrapTable());
    }

    private Wrap(WrapTable table) {
        super(table);
        this.table = table;
        this.table.name = "solim-wrap-table";
        SolimToken.bind(this.table, this, constraints);
        this.table.top().left();
        this.table.defaults().top().left();
    }

    public Table table() {
        return table;
    }


    @Override
    public PendingCellConfig cellConfig() {
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

    @Override
    public Wrap gap(float g) {
        this.gap = g;
        respace();
        return this;
    }

    @Override
    public Wrap background(@Nullable Drawable bg) {
        table.background(bg);
        return this;
    }

    @Override
    public Wrap padding(float p) {
        table.margin(p);
        return this;
    }

    @Override
    public Wrap padding(float top, float left, float bottom, float right) {
        table.margin(top, left, bottom, right);
        return this;
    }

    @Override
    public Wrap align(int align) {
        TableConfig.super.align(align);
        constraints.align = align;
        constraints.applyAlignToParentCell(element());
        table.defaults().align(align);
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.align(align);
        }
        return this;
    }

    @Override
    public Wrap top() {
        TableConfig.super.top();
        constraints.alignTop();
        constraints.applyAlignToParentCell(element());
        table.defaults().top();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.top();
        }
        return this;
    }

    @Override
    public Wrap bottom() {
        TableConfig.super.bottom();
        constraints.alignBottom();
        constraints.applyAlignToParentCell(element());
        table.defaults().bottom();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.bottom();
        }
        return this;
    }

    @Override
    public Wrap left() {
        TableConfig.super.left();
        constraints.alignLeft();
        constraints.applyAlignToParentCell(element());
        table.defaults().left();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.left();
        }
        return this;
    }

    @Override
    public Wrap right() {
        TableConfig.super.right();
        constraints.alignRight();
        constraints.applyAlignToParentCell(element());
        table.defaults().right();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.right();
        }
        return this;
    }

    @Override
    public Wrap center() {
        TableConfig.super.center();
        constraints.alignCenter();
        constraints.applyAlignToParentCell(element());
        table.defaults().center();
        for (Cell<?> c : table.getCells()) {
            if (c != null)
                c.center();
        }
        return this;
    }

    public Wrap children(@Nullable Runnable r) {
        OwnershipContext.push(this);
        AttachmentStack.push(table, ATTACHER);
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

    public Wrap add(Element child) {
        table.add(child);
        respace();
        return this;
    }



    @Override
    public Wrap name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public Wrap self() {
        return this;
    }
}

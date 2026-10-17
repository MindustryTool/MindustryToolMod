package solim.layout;

import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.Props;
import solim.modifier.TableCapability;

import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.scene.ui.layout.WrapTable;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.modifier.PendingCellConfig;

public final class Wrap extends Component
        implements GapContainer, Configurable<Wrap>, TableCapability, CellCapability, ContainerCapability<Wrap>, AlignmentCapability {

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        if (SolimToken.isExpandingChild(child)) {
            cell.growX();
        }
        return cell;
    };

    private final WrapTable table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<Wrap> pending;
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
    public void setGap(float g) {
        this.gap = g;
        respace();
    }

    public Wrap background(@Nullable Drawable bg) {
        table.background(bg);
        return this;
    }

    public Wrap padding(float p) {
        table.margin(p);
        return this;
    }

    public Wrap padding(float top, float left, float bottom, float right) {
        table.margin(top, left, bottom, right);
        return this;
    }

    public Wrap children(@Nullable Runnable r) {
        applyPendingProps();
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
    public @Nullable Props<Wrap> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Wrap> props) {
        this.pending = props;
    }

    public Wrap self() {
        return this;
    }
}
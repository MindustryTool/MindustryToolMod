package solim.layout;

import solim.modifier.AlignmentCapability;
import solim.modifier.CellCapability;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.Props;
import solim.modifier.TableCapability;

import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.runtime.AttachmentStack;
import solim.modifier.PendingCellConfig;
import solim.core.OwnershipContext;

public final class Row extends Component
        implements GapContainer, Configurable<Row>, TableCapability, CellCapability, ContainerCapability<Row>, AlignmentCapability {

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
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
    private boolean attached = false;
    private @Nullable Props<Row> pending;

    public Row() {
        this(new Table());
    }

    private Row(Table table) {
        super(table);
        this.table = table;
        SolimToken.bind(this.table, this, constraints);
        this.table.name = "solim-row-table";
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

    @Override
    public Direction direction() {
        return Direction.HORIZONTAL;
    }

    @Override
    public float gap() {
        return gap;
    }

    @Override
    public void setGap(float g) {
        this.gap = g;
        respace();
    }

    @Override
    public void respace() {
        GapContainer.applySpacing(table, Direction.HORIZONTAL, gap);
    }

    public Row children(@Nullable Runnable r) {
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

    public Cell<?> add(Element e) {
        Cell<?> cell = table.add(e);
        respace();
        return cell;
    }

    @Override
    public Row name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public @Nullable Props<Row> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Row> props) {
        this.pending = props;
    }

    public Row self() {
        return this;
    }
}
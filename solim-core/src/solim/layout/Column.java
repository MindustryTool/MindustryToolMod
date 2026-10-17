package solim.layout;


import arc.scene.Element;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.Configurable;
import solim.modifier.Props;
import solim.runtime.AttachmentStack;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.modifier.PendingCellConfig;
import solim.core.OwnershipContext;

public final class Column extends Component
        implements GapContainer, Configurable<Column> {

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        if (SolimToken.isExpandingChild(child)) {
            cell.growY();
        }
        cell.row();
        GapContainer gc = GapContainer.find(table);
        if (gc != null) {
            GapContainer.spaceAttachedCell(table, cell, Direction.VERTICAL, gc.gap());
        }
        return cell;
    };

    private final Table table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<Column> pending;
    private float gap = 0f;
    private boolean attached = false;

    public Column() {
        this(new Table());
    }

    private Column(Table table) {
        super(table);
        this.table = table;
        SolimToken.bind(this.table, this, constraints);
        this.table.name = "solim-column-table";
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

    @Override
    public Direction direction() {
        return Direction.VERTICAL;
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
        GapContainer.applySpacing(table, Direction.VERTICAL, gap);
    }

    public Column children(@Nullable Runnable r) {
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
    public Column name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public @Nullable Props<Column> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Column> props) {
        this.pending = props;
    }

    public Column self() {
        return this;
    }
}
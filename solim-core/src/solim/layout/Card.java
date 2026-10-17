package solim.layout;

import solim.modifier.Configurable;
import solim.modifier.Props;

import arc.graphics.Color;
import arc.scene.style.Drawable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.modifier.PendingCellConfig;

public final class Card extends Component
        implements GapContainer, Configurable<Card> {

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        cell.top().left();
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
    private float gap = 0f;
    private boolean attached = false;
    private @Nullable Props<Card> pending;

    public Card() {
        this(new Table());
    }

    private Card(Table table) {
        super(table);
        this.table = table;
        SolimToken.bind(this.table, this, constraints);
        this.table.name = "solim-card-table";
        this.table.top().left();
        this.table.defaults().top().left();
    }

    public Card(@Nullable Drawable background) {
        this();
        if (background != null) {
            table.setBackground(background);
        }
    }

    public Table container() {
        return table;
    }

    @Override
    public Table table() {
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
            withinOwnership(() -> Effect.of(() -> {
                Color c = color.get();
                if (c != null) {
                    table.setColor(c);
                }
            }));
        }
        return this;
    };

    public Card children(@Nullable Runnable r) {
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

    @Override
    public Card name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public @Nullable Props<Card> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<Card> props) {
        this.pending = props;
    }

    public Card self() {
        return this;
    }
}
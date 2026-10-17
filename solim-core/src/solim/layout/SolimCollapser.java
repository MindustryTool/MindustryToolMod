package solim.layout;

import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.Configurable;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;

public final class SolimCollapser extends Component implements GapContainer, Configurable<SolimCollapser> {

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

    private final Table content;
    private final Collapser collapser;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private @Nullable Props<SolimCollapser> pending;
    private boolean animated = true;
    private boolean initialized = false;
    private float gap = 0f;

    public SolimCollapser() {
        this(true);
    }

    public SolimCollapser(boolean collapsed) {
        this(new Table(), collapsed);
    }

    private SolimCollapser(Table content, boolean collapsed) {
        super(content);
        this.content = content;
        SolimToken.bind(this.content, this, constraints);
        this.content.name = "solim-collapser-content";
        this.content.top().left();
        this.content.defaults().top().left();

        this.collapser = new Collapser(content, collapsed);
        this.collapser.name = "solim-collapser";
        this.collapser.setDuration(0.2f);
    }

    public Collapser collapser() {
        return collapser;
    }

    public Table content() {
        return content;
    }

    @Override
    public Table table() {
        return content;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    public SolimCollapser duration(float seconds) {
        collapser.setDuration(seconds);
        return this;
    }

    public SolimCollapser animated(boolean animated) {
        this.animated = animated;
        return this;
    }

    public boolean isCollapsed() {
        return collapser.isCollapsed();
    }

    public SolimCollapser collapsed(boolean collapsed) {
        collapser.setCollapsed(collapsed, initialized && animated);
        initialized = true;
        return this;
    }

    public SolimCollapser collapsed(@Nullable Readable<Boolean> collapsed) {
        if (collapsed != null) {
            withinOwnership(() -> Effect.of(() -> {
                Boolean col = collapsed.get();
                if (col != null) {
                    boolean anim = initialized && animated;
                    collapser.setCollapsed(col, anim);
                    initialized = true;
                }
            }));
        }
        return this;
    };

    public SolimCollapser expanded(boolean expanded) {
        return collapsed(!expanded);
    }

    public SolimCollapser expanded(@Nullable Readable<Boolean> expanded) {
        if (expanded != null) {
            withinOwnership(() -> Effect.of(() -> {
                Boolean exp = expanded.get();
                if (exp != null) {
                    boolean anim = initialized && animated;
                    collapser.setCollapsed(!exp, anim);
                    initialized = true;
                }
            }));
        }
        return this;
    };

    public SolimCollapser children(@Nullable Runnable r) {
        applyPendingProps();
        OwnershipContext.push(this);
        AttachmentStack.push(content, ATTACHER);
        try {
            if (r != null) {
                r.run();
            }
        } finally {
            AttachmentStack.pop();
            OwnershipContext.pop();
        }
        AttachmentStack.attachToParent(collapser);
        respace();
        return this;
    }

    @Override
    public void setGap(float g) {
        this.gap = g;
        respace();
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
        GapContainer.applySpacing(content, Direction.VERTICAL, gap);
    }

    @Override
    public @Nullable Props<SolimCollapser> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<SolimCollapser> props) {
        this.pending = props;
    }

    @Override
    public SolimCollapser name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public SolimCollapser self() {
        return this;
    }
}

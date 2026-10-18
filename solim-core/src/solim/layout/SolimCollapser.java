package solim.layout;

import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.SolimToken;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.reactive.Effect;
import solim.reactive.Signal;
import solim.reactive.TwoWayBinding;
import solim.core.DisposableAction;
import solim.core.OwnershipContext;
import solim.runtime.AttachmentStack;

public final class SolimCollapser extends Component
        implements GapContainer, Configurable<SolimCollapser>, ContainerCapability<SolimCollapser> {

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
    private float duration = 0.2f;
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
        this.collapser.setDuration(duration);
        this.collapsedState = Signal.of(collapsed);
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

    public void setDuration(float seconds) {
        this.duration = seconds;
        collapser.setDuration(seconds);
    }

    public float getDuration() {
        return duration;
    }

    public void setAnimated(boolean animated) {
        this.animated = animated;
    }

    private final Signal<Boolean> collapsedState;

    public boolean isCollapsed() {
        return collapser.isCollapsed();
    }

    public void setCollapsed(boolean collapsed) {
        collapser.setCollapsed(collapsed, initialized && animated);
        initialized = true;
        collapsedState.set(collapser.isCollapsed());
    }

    public void bindCollapsedSignal(Signal<Boolean> signal) {
        new TwoWayBinding<>(
                signal,
                this::isCollapsed,
                this::setCollapsed,
                onChange -> {
                    Effect.of(() -> {
                        collapsedState.get();
                        onChange.run();
                    });
                    return DisposableAction.empty();
                });
    }

    public void setExpanded(boolean expanded) {
        setCollapsed(!expanded);
    }

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

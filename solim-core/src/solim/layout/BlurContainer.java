package solim.layout;

import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.graphics.BlurHelper;
import solim.modifier.BlurCapability;
import solim.modifier.Configurable;
import solim.modifier.ContainerCapability;
import solim.modifier.PendingCellConfig;
import solim.modifier.Props;
import solim.runtime.AttachmentStack;

public final class BlurContainer extends Component
        implements GapContainer, Configurable<BlurContainer>, ContainerCapability<BlurContainer>, BlurCapability {

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

    private final BlurTable table;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private float gap = 0f;
    private boolean attached = false;
    private @Nullable Props<BlurContainer> pending;
    private float backdropBlurRadius = 0f;

    public BlurContainer() {
        this(new BlurTable());
    }

    public BlurContainer(float contentBlurRadius) {
        this();
        contentBlur(contentBlurRadius);
    }

    private BlurContainer(BlurTable table) {
        super(table);
        this.table = table;
        SolimToken.bind(this.table, this, constraints);
        this.table.name = "solim-blur-container";
        this.table.top().left();
        this.table.defaults().top().left();
    }

    @Override
    public BlurContainer children(@Nullable Runnable r) {
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
        this.gap = Math.max(0f, g);
        respace();
    }

    @Override
    public void respace() {
        GapContainer.applySpacing(table, Direction.VERTICAL, gap);
    }

    @Override
    public BlurContainer name(@Nullable String name) {
        setName(name);
        return this;
    }

    @Override
    public Table table() {
        return table;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    public @Nullable Props<BlurContainer> pendingProps() {
        return pending;
    }

    @Override
    public void setPendingProps(@Nullable Props<BlurContainer> props) {
        this.pending = props;
    }

    @Override
    public BlurContainer self() {
        return this;
    }

    @Override
    public void backdropBlur(float radius) {
        this.backdropBlurRadius = radius;
        BlurHelper.applyBackdropBlur(table, radius);
    }

    @Override
    public void contentBlur(float radius) {
        table.contentBlurRadius(radius);
    }

    @Override
    public float backdropBlur() {
        return backdropBlurRadius;
    }

    @Override
    public float contentBlur() {
        return table.contentBlurRadius();
    }
}

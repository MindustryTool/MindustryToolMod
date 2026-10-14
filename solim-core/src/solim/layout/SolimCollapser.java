package solim.layout;

import arc.graphics.g2d.Draw;
import arc.math.Interp;
import arc.scene.actions.TemporalAction;
import arc.scene.event.Touchable;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import solim.core.Component;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.modifier.CellConfig;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.runtime.AttachmentStack;

public final class SolimCollapser extends Component implements CellConfig<SolimCollapser>,
        ElementConfig<SolimCollapser>, TableConfig<SolimCollapser>, GapContainer {

    public static final AttachmentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        cell.top().left();
        GapContainer gc = GapContainer.find(table);
        Direction dir = gc != null ? gc.direction() : Direction.VERTICAL;
        if (SolimToken.isExpandingChild(child)) {
            if (dir == Direction.HORIZONTAL) {
                cell.growX();
            } else {
                cell.growY();
            }
        }
        if (dir == Direction.VERTICAL) {
            cell.row();
        }
        if (gc != null) {
            GapContainer.spaceAttachedCell(table, cell, dir, gc.gap());
        }
        return cell;
    };

    private final Table content;
    private final DirectionalCollapser collapser;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private Direction axis = Direction.VERTICAL;
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

        this.collapser = new DirectionalCollapser(content, collapsed);
        SolimToken.bind(this.collapser, this, constraints);
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

    public SolimCollapser direction(Direction direction) {
        this.axis = direction != null ? direction : Direction.VERTICAL;
        this.collapser.setDirection(this.axis);
        respace();
        return this;
    }

    public SolimCollapser duration(float seconds) {
        collapser.setDuration(seconds);
        return this;
    }

    public SolimCollapser duration(float seconds, @Nullable Interp interp) {
        collapser.setDuration(seconds, interp);
        return this;
    }

    public SolimCollapser interp(@Nullable Interp interp) {
        collapser.setInterpolation(interp);
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
            own(Effect.of(() -> {
                Boolean col = collapsed.get();
                if (col != null) {
                    boolean anim = initialized && animated;
                    collapser.setCollapsed(col, anim);
                    initialized = true;
                }
            }));
        }
        return this;
    }

    public SolimCollapser expanded(boolean expanded) {
        return collapsed(!expanded);
    }

    public SolimCollapser expanded(@Nullable Readable<Boolean> expanded) {
        if (expanded != null) {
            own(Effect.of(() -> {
                Boolean exp = expanded.get();
                if (exp != null) {
                    boolean anim = initialized && animated;
                    collapser.setCollapsed(!exp, anim);
                    initialized = true;
                }
            }));
        }
        return this;
    }

    public SolimCollapser children(@Nullable Runnable r) {
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

    public SolimCollapser gap(float g) {
        this.gap = g;
        respace();
        return this;
    }

    @Override
    public Direction direction() {
        return axis;
    }

    @Override
    public float gap() {
        return gap;
    }

    @Override
    public void respace() {
        GapContainer.applySpacing(content, axis, gap);
    }

    @Override
    public SolimCollapser align(int align) {
        TableConfig.super.align(align);
        constraints.align = align;
        constraints.applyAlignToParentCell(element());
        content.defaults().align(align);
        for (Cell<?> c : content.getCells()) {
            if (c != null) {
                c.align(align);
            }
        }
        return this;
    }

    @Override
    public SolimCollapser center() {
        TableConfig.super.center();
        constraints.alignCenter();
        constraints.applyAlignToParentCell(element());
        content.defaults().center();
        for (Cell<?> c : content.getCells()) {
            if (c != null) {
                c.center();
            }
        }
        return this;
    }

    @Override
    public SolimCollapser top() {
        TableConfig.super.top();
        constraints.alignTop();
        constraints.applyAlignToParentCell(element());
        content.defaults().top();
        for (Cell<?> c : content.getCells()) {
            if (c != null) {
                c.top();
            }
        }
        return this;
    }

    @Override
    public SolimCollapser bottom() {
        TableConfig.super.bottom();
        constraints.alignBottom();
        constraints.applyAlignToParentCell(element());
        content.defaults().bottom();
        for (Cell<?> c : content.getCells()) {
            if (c != null) {
                c.bottom();
            }
        }
        return this;
    }

    @Override
    public SolimCollapser left() {
        TableConfig.super.left();
        constraints.alignLeft();
        constraints.applyAlignToParentCell(element());
        content.defaults().left();
        for (Cell<?> c : content.getCells()) {
            if (c != null) {
                c.left();
            }
        }
        return this;
    }

    @Override
    public SolimCollapser right() {
        TableConfig.super.right();
        constraints.alignRight();
        constraints.applyAlignToParentCell(element());
        content.defaults().right();
        for (Cell<?> c : content.getCells()) {
            if (c != null) {
                c.right();
            }
        }
        return this;
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

    private static final class DirectionalCollapser extends Collapser {
        private final Table contentTable;
        private final AxisCollapseAction axisAction = new AxisCollapseAction();
        private Direction axis = Direction.VERTICAL;
        private boolean isCollapsedState;
        private boolean running;
        private float curWidth;
        private float curHeight;

        DirectionalCollapser(Table contentTable, boolean collapsed) {
            super(contentTable, collapsed);
            this.contentTable = contentTable;
            this.isCollapsedState = collapsed;
            setTransform(true);
            updateTouchableState();
            this.axisAction.setDuration(0.2f);
            this.axisAction.setInterpolation(Interp.linear);
        }

        void setDirection(Direction direction) {
            this.axis = direction != null ? direction : Direction.VERTICAL;
            if (!running) {
                if (this.axis == Direction.HORIZONTAL) {
                    curWidth = isCollapsedState ? 0f : contentTable.getPrefWidth();
                    curHeight = contentTable.getPrefHeight();
                } else {
                    curWidth = contentTable.getPrefWidth();
                    curHeight = isCollapsedState ? 0f : contentTable.getPrefHeight();
                }
            }
            invalidateHierarchy();
        }

        void setInterpolation(@Nullable Interp interp) {
            this.axisAction.setInterpolation(interp != null ? interp : Interp.linear);
        }

        @Override
        public Collapser setDuration(float seconds) {
            super.setDuration(seconds);
            this.axisAction.setDuration(seconds);
            return this;
        }

        Collapser setDuration(float seconds, @Nullable Interp interp) {
            setDuration(seconds);
            setInterpolation(interp);
            return this;
        }

        @Override
        public boolean isCollapsed() {
            return isCollapsedState;
        }

        @Override
        public void setCollapsed(boolean collapse, boolean animate) {
            this.isCollapsedState = collapse;
            updateTouchableState();
            removeAction(axisAction);
            if (animate) {
                this.running = true;
                if (axis == Direction.HORIZONTAL) {
                    curWidth = collapse ? contentTable.getPrefWidth() : 0f;
                    curHeight = contentTable.getPrefHeight();
                } else {
                    curWidth = contentTable.getPrefWidth();
                    curHeight = collapse ? contentTable.getPrefHeight() : 0f;
                }
                axisAction.restart();
                addAction(axisAction);
            } else {
                this.running = false;
                if (axis == Direction.HORIZONTAL) {
                    curWidth = collapse ? 0f : contentTable.getPrefWidth();
                    curHeight = contentTable.getPrefHeight();
                } else {
                    curWidth = contentTable.getPrefWidth();
                    curHeight = collapse ? 0f : contentTable.getPrefHeight();
                }
                invalidateHierarchy();
            }
        }

        @Override
        public void setCollapsed(boolean collapse) {
            setCollapsed(collapse, true);
        }

        private void updateTouchableState() {
            this.touchable = isCollapsedState ? Touchable.disabled : Touchable.enabled;
        }

        @Override
        public void draw() {
            if (curWidth > 1f && curHeight > 1f) {
                Draw.flush();
                if (clipBegin(x, y, curWidth, curHeight)) {
                    validate();
                    if (isTransform()) {
                        applyTransform(computeTransform());
                        drawChildren();
                        resetTransform();
                    } else {
                        drawChildren();
                    }
                    Draw.flush();
                    clipEnd();
                }
            }
        }

        @Override
        protected void drawChildren() {
            if (isCollapsedState && !running) {
                return;
            }
            super.drawChildren();
        }

        @Override
        public void layout() {
            contentTable.setBounds(0f, 0f, getWidth(), getHeight());
            if (!running) {
                if (axis == Direction.HORIZONTAL) {
                    curWidth = isCollapsedState ? 0f : contentTable.getPrefWidth();
                    curHeight = getHeight();
                } else {
                    curWidth = getWidth();
                    curHeight = isCollapsedState ? 0f : contentTable.getPrefHeight();
                }
            } else {
                if (axis == Direction.HORIZONTAL) {
                    curHeight = getHeight();
                } else {
                    curWidth = getWidth();
                }
            }
        }

        @Override
        public float getPrefWidth() {
            if (axis != Direction.HORIZONTAL) {
                return contentTable.getPrefWidth();
            }
            if (!running) {
                return isCollapsedState ? 0f : contentTable.getPrefWidth();
            }
            return curWidth;
        }

        @Override
        public float getPrefHeight() {
            if (axis != Direction.VERTICAL) {
                return contentTable.getPrefHeight();
            }
            if (!running) {
                return isCollapsedState ? 0f : contentTable.getPrefHeight();
            }
            return curHeight;
        }

        private final class AxisCollapseAction extends TemporalAction {
            @Override
            protected void begin() {
                running = true;
            }

            @Override
            protected void update(float percent) {
                float ratio = isCollapsedState ? (1f - percent) : percent;
                if (axis == Direction.HORIZONTAL) {
                    curWidth = ratio * contentTable.getPrefWidth();
                    curHeight = contentTable.getPrefHeight();
                } else {
                    curWidth = contentTable.getPrefWidth();
                    curHeight = ratio * contentTable.getPrefHeight();
                }
                invalidateHierarchy();
            }

            @Override
            protected void end() {
                running = false;
                invalidateHierarchy();
            }
        }
    }
}

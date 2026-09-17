package solim.layout;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Collapser;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.Ui;
import solim.modifier.CellConfig;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;

/**
 * Animated collapsible container wrapping an Arc {@link Collapser} around a content {@link Table}.
 * When collapsed, its preferred and minimum heights become zero, preventing empty whitespace allocation.
 */
public final class SolimCollapser implements Component, CellConfig<SolimCollapser>, ElementConfig<SolimCollapser>, TableConfig<SolimCollapser>, GapContainer {

    public static final ParentStack.Attacher ATTACHER = (table, child) -> {
        Cell<?> cell = table.add(child);
        cell.top().left();
        if (Ui.isExpanding(child)) {
            cell.growY();
        }
        cell.row();
        if (table.userObject instanceof GapContainer) {
            GapContainer gc = (GapContainer) table.userObject;
            GapContainer.spaceAttachedCell(table, cell, Direction.VERTICAL, gc.gap());
        }
        return cell;
    };

    private final Table content = new Table();
    private final Collapser collapser;
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final List<Disposable> bindings = new ArrayList<>();
    private boolean disposed = false;
    private boolean animated = true;
    private boolean initialized = false;
    private float gap = 0f;

    public SolimCollapser() {
        this(true);
    }

    public SolimCollapser(boolean collapsed) {
        this.content.userObject = this;
        this.content.name = "solim-collapser-content";
        this.content.top().left();
        this.content.defaults().top().left();

        this.collapser = new Collapser(content, collapsed);
        this.collapser.name = "solim-collapser";
        this.collapser.setDuration(0.2f);
        ComponentContext.register(this);
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
    public Element element() {
        return collapser;
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
            Effect e = Effect.of(() -> {
                Boolean col = collapsed.get();
                if (col != null) {
                    boolean anim = initialized && animated;
                    collapser.setCollapsed(col, anim);
                    initialized = true;
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public SolimCollapser expanded(boolean expanded) {
        return collapsed(!expanded);
    }

    public SolimCollapser expanded(@Nullable Readable<Boolean> expanded) {
        if (expanded != null) {
            Effect e = Effect.of(() -> {
                Boolean exp = expanded.get();
                if (exp != null) {
                    boolean anim = initialized && animated;
                    collapser.setCollapsed(!exp, anim);
                    initialized = true;
                }
            });
            bindings.add(e);
            ComponentContext.register(e);
        }
        return this;
    }

    public SolimCollapser children(@Nullable Runnable r) {
        ParentStack.push(content, ATTACHER);
        try {
            if (r != null) {
                r.run();
            }
        } finally {
            ParentStack.pop();
        }
        ParentStack.attachToParent(collapser);
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
    public void dispose() {
        if (disposed) {
            return;
        }
        disposed = true;
        for (Disposable d : bindings) {
            d.dispose();
        }
        bindings.clear();
    }

    @Override
    public boolean isDisposed() {
        return disposed;
    }

    @Override
    public SolimCollapser self() {
        return this;
    }
}

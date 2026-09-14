package solim.modifier;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Disposable;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.layout.CellConfig;
import solim.layout.GapContainer;
import arc.util.Align;

/**
 * Deferred configuration buffer for parent Cell settings.
 *
 * <p>
 * Holds pending values that will be applied to a parent Cell when the element
 * is attached to a parent Table via {@link ParentStack}.
 */
public final class PendingCellConfig {

    static {
        ParentStack.setCellConfigurator((cell, child) -> {
            PendingCellConfig config = find(child);
            if (config != null) {
                List<Disposable> effects = config.applyToCell(cell);
                for (Disposable effect : effects) {
                    ComponentContext.register(effect);
                }
            }
            Table t = cell.getTable();
            if (t != null && t.userObject instanceof GapContainer) {
                ((GapContainer) t.userObject).respace();
            }
        });
    }

    /** Preferred width. Null means "no constraint — use natural size". */
    public @Nullable Readable<Float> prefWidth;

    /** Preferred height. Null means "no constraint — use natural size". */
    public @Nullable Readable<Float> prefHeight;

    /** Minimum width. Applied as cell.minWidth(). */
    public @Nullable Readable<Float> minWidth;

    /** Minimum height. Applied as cell.minHeight(). */
    public @Nullable Readable<Float> minHeight;

    /** Maximum width. Applied as cell.maxWidth(). */
    public @Nullable Readable<Float> maxWidth;

    /** Maximum height. Applied as cell.maxHeight(). */
    public @Nullable Readable<Float> maxHeight;

    /** Whether the parent cell should grow this element on the X axis. */
    public boolean growX = false;

    /** Whether the parent cell should grow this element on the Y axis. */
    public boolean growY = false;

    /** Outer margins (applied as cell.pad()). Null means use container default. */
    public @Nullable Readable<Float> padTop;
    public @Nullable Readable<Float> padLeft;
    public @Nullable Readable<Float> padBottom;
    public @Nullable Readable<Float> padRight;

    /**
     * Alignment of this element within its parent cell. Null means use container
     * default.
     */
    public @Nullable Integer align;

    /**
     * Returns true if any explicit preferred or bounded width constraint is set.
     */
    public boolean hasExplicitWidth() {
        return prefWidth != null || minWidth != null || maxWidth != null;
    }

    /**
     * Returns true if any explicit preferred or bounded height constraint is set.
     */
    public boolean hasExplicitHeight() {
        return prefHeight != null || minHeight != null || maxHeight != null;
    }

    public void alignCenter() {
        this.align = Align.center;
    }

    public void alignTop() {
        int current = this.align != null ? this.align : 0;
        this.align = (current | Align.top) & ~Align.bottom;
    }

    public void alignBottom() {
        int current = this.align != null ? this.align : 0;
        this.align = (current | Align.bottom) & ~Align.top;
    }

    public void alignLeft() {
        int current = this.align != null ? this.align : 0;
        this.align = (current | Align.left) & ~Align.right;
    }

    public void alignRight() {
        int current = this.align != null ? this.align : 0;
        this.align = (current | Align.right) & ~Align.left;
    }

    public static @Nullable PendingCellConfig find(@Nullable Object target) {
        if (target == null)
            return null;
        if (target instanceof CellConfig) {
            return ((CellConfig<?>) target).cellConfig();
        }
        if (target instanceof PendingCellConfig) {
            return (PendingCellConfig) target;
        }
        if (target instanceof Element) {
            Element el = (Element) target;
            return find(el.userObject);
        }
        return null;
    }

    /**
     * Applies all constraints to the given cell. Static values are applied
     * immediately. Reactive values ({@code Signal}, {@code Computed}) install an
     * {@code Effect} that re-applies the constraint whenever the source changes.
     *
     * @return a list of installed {@link Effect} instances — the caller must own
     *         and dispose them when the component is disposed.
     */
    public List<Disposable> applyToCell(Cell<?> cell) {
        List<Disposable> effects = new ArrayList<>();

        effects.addAll(bind(cell, prefWidth, v -> applyPrefWidth(cell, v)));
        effects.addAll(bind(cell, prefHeight, v -> applyPrefHeight(cell, v)));
        effects.addAll(bind(cell, minWidth, v -> applyMinWidth(cell, v)));
        effects.addAll(bind(cell, minHeight, v -> applyMinHeight(cell, v)));
        effects.addAll(bind(cell, maxWidth, v -> applyMaxWidth(cell, v)));
        effects.addAll(bind(cell, maxHeight, v -> applyMaxHeight(cell, v)));

        effects.addAll(bind(cell, padTop, v -> {
            Table t = cell.getTable();
            if (t != null && t.userObject instanceof GapContainer) {
                ((GapContainer) t.userObject).respace();
            } else {
                cell.padTop(Math.max(0f, v));
            }
        }));
        effects.addAll(bind(cell, padLeft, v -> {
            Table t = cell.getTable();
            if (t != null && t.userObject instanceof GapContainer) {
                ((GapContainer) t.userObject).respace();
            } else {
                cell.padLeft(Math.max(0f, v));
            }
        }));
        effects.addAll(bind(cell, padBottom, v -> {
            Table t = cell.getTable();
            if (t != null && t.userObject instanceof GapContainer) {
                ((GapContainer) t.userObject).respace();
            } else {
                cell.padBottom(Math.max(0f, v));
            }
        }));
        effects.addAll(bind(cell, padRight, v -> {
            Table t = cell.getTable();
            if (t != null && t.userObject instanceof GapContainer) {
                ((GapContainer) t.userObject).respace();
            } else {
                cell.padRight(Math.max(0f, v));
            }
        }));

        if (growX) {
            cell.growX();
            if (minWidth == null) {
                cell.minWidth(0f);
            }
        }
        if (growY)
            cell.growY();

        if (align != null) {
            cell.align(align);
        }

        return effects;
    }

    /**
     * Immediately applies size constraints to the element's parent cell if the
     * element is already attached to a parent Table.
     */
    public void applySizeToParentCell(@Nullable Element element) {
        if (element != null && element.parent instanceof Table) {
            Table parentTable = (Table) element.parent;
            Cell<?> cell = parentTable.getCell(element);
            if (cell != null) {
                if (prefWidth != null && prefWidth.get() != null)
                    cell.width(Math.max(0f, prefWidth.get()));
                if (prefHeight != null && prefHeight.get() != null)
                    cell.height(Math.max(0f, prefHeight.get()));
                if (minWidth != null && minWidth.get() != null)
                    cell.minWidth(Math.max(0f, minWidth.get()));
                if (minHeight != null && minHeight.get() != null)
                    cell.minHeight(Math.max(0f, minHeight.get()));
                if (maxWidth != null && maxWidth.get() != null)
                    cell.maxWidth(Math.max(0f, maxWidth.get()));
                if (maxHeight != null && maxHeight.get() != null)
                    cell.maxHeight(Math.max(0f, maxHeight.get()));
                parentTable.invalidateHierarchy();
            }
        }
    }

    /**
     * Immediately applies margin constraints to the element's parent cell if the
     * element is already attached to a parent Table.
     */
    public void applyMarginToParentCell(@Nullable Element element) {
        if (element != null && element.parent instanceof Table) {
            Table parentTable = (Table) element.parent;
            if (parentTable.userObject instanceof GapContainer) {
                ((GapContainer) parentTable.userObject).respace();
                return;
            }
            Cell<?> cell = parentTable.getCell(element);
            if (cell != null) {
                if (padTop != null && padTop.get() != null)
                    cell.padTop(Math.max(0f, padTop.get()));
                if (padLeft != null && padLeft.get() != null)
                    cell.padLeft(Math.max(0f, padLeft.get()));
                if (padBottom != null && padBottom.get() != null)
                    cell.padBottom(Math.max(0f, padBottom.get()));
                if (padRight != null && padRight.get() != null)
                    cell.padRight(Math.max(0f, padRight.get()));
                parentTable.invalidate();
            }
        }
    }

    /**
     * Immediately applies grow constraints to the element's parent cell if the
     * element is already attached to a parent Table.
     */
    public void applyGrowToParentCell(@Nullable Element element) {
        if (element != null && element.parent instanceof Table) {
            Table parentTable = (Table) element.parent;
            Cell<?> cell = parentTable.getCell(element);
            if (cell != null) {
                if (growX)
                    cell.growX();
                if (growY)
                    cell.growY();
                parentTable.invalidate();
            }
        }
    }

    /**
     * Immediately applies alignment constraint to the element's parent cell if the
     * element is already attached to a parent Table.
     */
    public void applyAlignToParentCell(@Nullable Element element) {
        if (element != null && element.parent instanceof Table) {
            Table parentTable = (Table) element.parent;
            Cell<?> cell = parentTable.getCell(element);
            if (cell != null && align != null) {
                cell.align(align);
                parentTable.invalidate();
            }
        }
    }

    // ---------- private helpers ----------

    private static List<Disposable> bind(Cell<?> cell, @Nullable Readable<Float> source, FloatApplier applier) {
        List<Disposable> result = new ArrayList<>();
        if (source == null)
            return result;
        Effect e = Effect.of(() -> {
            Float v = source.get();
            if (v != null) {
                applier.apply(v);
                if (cell.get() != null) {
                    cell.get().invalidate();
                }
                if (cell.getTable() != null) {
                    cell.getTable().invalidateHierarchy();
                }
            }
        });
        result.add(e);
        return result;
    }

    private static void applyPrefWidth(Cell<?> cell, float v) {
        cell.width(Math.max(0f, v));
    }

    private static void applyPrefHeight(Cell<?> cell, float v) {
        cell.height(Math.max(0f, v));
    }

    private static void applyMinWidth(Cell<?> cell, float v) {
        cell.minWidth(Math.max(0f, v));
    }

    private static void applyMinHeight(Cell<?> cell, float v) {
        cell.minHeight(Math.max(0f, v));
    }

    private static void applyMaxWidth(Cell<?> cell, float v) {
        cell.maxWidth(Math.max(0f, v));
    }

    private static void applyMaxHeight(Cell<?> cell, float v) {
        cell.maxHeight(Math.max(0f, v));
    }

    @FunctionalInterface
    private interface FloatApplier {
        void apply(float value);
    }
}

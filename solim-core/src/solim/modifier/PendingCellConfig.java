package solim.modifier;

import solim.runtime.ReactiveContext;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import solim.core.Disposable;
import solim.core.OwnershipContext;
import solim.core.SolimToken;
import solim.runtime.AttachmentStack;
import solim.reactive.Effect;
import solim.reactive.Readable;
import solim.layout.GapContainer;
import arc.util.Align;

public final class PendingCellConfig {

    public static final AttachmentStack.CellConfigurator DEFAULT_CONFIGURATOR = (cell, child, comp) -> {
        PendingCellConfig config = comp != null ? find(comp) : null;

        if (config == null) {
            config = find(child);
        }

        if (config != null) {
            if (comp != null) {
                comp.openSelfWindow();
                try {
                    config.applyToCell(cell);
                } finally {
                    comp.closeSelfWindow();
                }
            } else {
                config.applyToCell(cell);
            }
        }

        GapContainer.respace(cell.getTable());
    };

    static {
        install();
    }

    public static void install() {
        AttachmentStack.setCellConfigurator(DEFAULT_CONFIGURATOR);
    }

    public @Nullable Readable<Float> prefWidth;

    public @Nullable Readable<Float> prefHeight;

    public @Nullable Readable<Float> minWidth;

    public @Nullable Readable<Float> minHeight;

    public @Nullable Readable<Float> maxWidth;

    public @Nullable Readable<Float> maxHeight;

    public boolean growX = false;

    public boolean growY = false;

    public @Nullable Readable<Float> padTop;
    public @Nullable Readable<Float> padLeft;
    public @Nullable Readable<Float> padBottom;
    public @Nullable Readable<Float> padRight;

    public @Nullable Integer align;

    private @Nullable List<Disposable> appliedBindings;

    public boolean hasExplicitWidth() {
        return prefWidth != null || minWidth != null || maxWidth != null;
    }

    public boolean hasExplicitHeight() {
        return prefHeight != null || minHeight != null || maxHeight != null;
    }

    public void align(int named) {
        if (named == 0) {
            this.align = null;
            return;
        }
        if (named == Align.center) {
            this.align = Align.center;
            return;
        }
        int current = this.align != null ? this.align : 0;
        int opposite = named == Align.top ? Align.bottom : named == Align.bottom ? Align.top
                : named == Align.left ? Align.right : Align.left;
        this.align = (current | named) & ~opposite;
    }

    public void alignCenter() {
        this.align = Align.center;
    }

    public void alignTop() {
        align(Align.top);
    }

    public void alignBottom() {
        align(Align.bottom);
    }

    public void alignLeft() {
        align(Align.left);
    }

    public void alignRight() {
        align(Align.right);
    }

    public static @Nullable PendingCellConfig find(@Nullable Object target) {
        if (target == null) {
            return null;
        }

        if (target instanceof Configurable) {
            return ((Configurable<?>) target).cellConfig();
        }

        if (target instanceof PendingCellConfig) {
            return (PendingCellConfig) target;
        }

        if (target instanceof Element) {
            Element el = (Element) target;
            SolimToken token = SolimToken.get(el);
            if (token != null) {
                if (token.cellConfig != null) {
                    return token.cellConfig;
                }
                if (token.component instanceof Configurable) {
                    return ((Configurable<?>) token.component).cellConfig();
                }
            }
            return null;
        }
        return null;
    }

    public List<Disposable> applyToCell(Cell<?> cell) {
        releaseApplied();
        List<Disposable> effects = new ArrayList<>();
        boolean windowed = OwnershipContext.size() > 0;
        effects.addAll(bind(cell, prefWidth, v -> applyPrefWidth(cell, v)));
        effects.addAll(bind(cell, prefHeight, v -> applyPrefHeight(cell, v)));
        effects.addAll(bind(cell, minWidth, v -> applyMinWidth(cell, v)));
        effects.addAll(bind(cell, minHeight, v -> applyMinHeight(cell, v)));
        effects.addAll(bind(cell, maxWidth, v -> applyMaxWidth(cell, v)));
        effects.addAll(bind(cell, maxHeight, v -> applyMaxHeight(cell, v)));

        effects.addAll(bind(cell, padTop, v -> {
            Table t = cell.getTable();
            if (GapContainer.isGapContainer(t)) {
                GapContainer.respace(t);
            } else {
                cell.padTop(Math.max(0f, v));
            }
        }));
        effects.addAll(bind(cell, padLeft, v -> {
            Table t = cell.getTable();
            if (GapContainer.isGapContainer(t)) {
                GapContainer.respace(t);
            } else {
                cell.padLeft(Math.max(0f, v));
            }
        }));
        effects.addAll(bind(cell, padBottom, v -> {
            Table t = cell.getTable();
            if (GapContainer.isGapContainer(t)) {
                GapContainer.respace(t);
            } else {
                cell.padBottom(Math.max(0f, v));
            }
        }));
        effects.addAll(bind(cell, padRight, v -> {
            Table t = cell.getTable();
            if (GapContainer.isGapContainer(t)) {
                GapContainer.respace(t);
            } else {
                cell.padRight(Math.max(0f, v));
            }
        }));
        if (!windowed) {
            adoptAll(effects);
        }

        if (growX) {
            cell.growX();
            Float floor = ReactiveContext.untracked(() -> minWidth != null ? minWidth.peek() : null);
            if (floor != null) {
                cell.minWidth(Math.max(0f, floor));
            } else if (minWidth == null) {
                cell.minWidth(0f);
            }
        }
        if (growY)
            cell.growY();

        if (align != null) {
            cell.align(align);
        }

        if (windowed) {
            appliedBindings = effects;
        }
        return effects;
    }

    /**
     * Takes ownership of bindings created outside a component window, so a direct
     * {@code applyToCell} call from application or test code still has a lifecycle.
     */
    private void adoptAll(List<Disposable> effects) {
        List<Disposable> owned = appliedBindings;
        if (owned == null) {
            owned = new ArrayList<>();
            appliedBindings = owned;
        }
        owned.addAll(effects);
    }

    /**
     * A reconciler re-attaches its items on every pass, so the bindings installed
     * for the previous cell must be released before the new ones are created.
     * Without this the item would accumulate one live binding per reflow.
     */
    private void releaseApplied() {
        List<Disposable> stale = appliedBindings;
        appliedBindings = null;
        if (stale == null) {
            return;
        }
        for (int i = stale.size() - 1; i >= 0; i--) {
            try {
                stale.get(i).dispose();
            } catch (Throwable ignored) {
            }
        }
    }

    public void applySizeToParentCell(@Nullable Element element) {
        applyPrefWidth(element);
        applyPrefHeight(element);
        applyMinWidth(element);
        applyMinHeight(element);
        applyMaxWidth(element);
        applyMaxHeight(element);
    }

    public void applyPrefWidth(@Nullable Element element) {
        applySize(element, prefWidth, Cell::width);
    }

    public void applyPrefHeight(@Nullable Element element) {
        applySize(element, prefHeight, Cell::height);
    }

    public void applyMinWidth(@Nullable Element element) {
        applySize(element, minWidth, Cell::minWidth);
    }

    public void applyMinHeight(@Nullable Element element) {
        applySize(element, minHeight, Cell::minHeight);
    }

    public void applyMaxWidth(@Nullable Element element) {
        applySize(element, maxWidth, Cell::maxWidth);
    }

    public void applyMaxHeight(@Nullable Element element) {
        applySize(element, maxHeight, Cell::maxHeight);
    }

    public void applyMarginToParentCell(@Nullable Element element) {
        applyPadTop(element);
        applyPadLeft(element);
        applyPadBottom(element);
        applyPadRight(element);
    }

    public void applyPadTop(@Nullable Element element) {
        applyMargin(element, padTop, Cell::padTop);
    }

    public void applyPadLeft(@Nullable Element element) {
        applyMargin(element, padLeft, Cell::padLeft);
    }

    public void applyPadBottom(@Nullable Element element) {
        applyMargin(element, padBottom, Cell::padBottom);
    }

    public void applyPadRight(@Nullable Element element) {
        applyMargin(element, padRight, Cell::padRight);
    }

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

    private static void applySize(@Nullable Element element, @Nullable Readable<Float> source,
            SizeApplier applier) {
        if (source == null) {
            return;
        }
        Cell<?> cell = parentCell(element);
        if (cell == null) {
            return;
        }
        Float value = source.get();
        if (value == null) {
            return;
        }
        applier.apply(cell, Math.max(0f, value));
        cell.getTable().invalidateHierarchy();
    }

    private static void applyMargin(@Nullable Element element, @Nullable Readable<Float> source,
            EdgeApplier applier) {
        if (source == null) {
            return;
        }
        Table parentTable = parentTable(element);
        if (parentTable == null) {
            return;
        }
        if (GapContainer.isGapContainer(parentTable)) {
            GapContainer.respace(parentTable);
            return;
        }
        Cell<?> cell = parentTable.getCell(element);
        if (cell == null) {
            return;
        }
        Float value = source.get();
        if (value == null) {
            return;
        }
        applier.apply(cell, Math.max(0f, value));
        parentTable.invalidate();
    }

    private static @Nullable Table parentTable(@Nullable Element element) {
        return element != null && element.parent instanceof Table ? (Table) element.parent : null;
    }

    private static @Nullable Cell<?> parentCell(@Nullable Element element) {
        Table parentTable = parentTable(element);
        return parentTable != null ? parentTable.getCell(element) : null;
    }

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

    @FunctionalInterface
    private interface SizeApplier {
        void apply(Cell<?> cell, float value);
    }

    @FunctionalInterface
    private interface EdgeApplier {
        void apply(Cell<?> cell, float value);
    }
}
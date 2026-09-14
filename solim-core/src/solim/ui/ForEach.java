package solim.ui;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import java.util.*;
import java.util.function.Function;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.signal.Effect;
import solim.signal.Readable;

import solim.layout.CellConfig;
import solim.modifier.PendingCellConfig;
import solim.runtime.StructuralReconciler;

/**
 * Keyed reactive list component that efficiently manages child components
 * without rebuilding unchanged items.
 */
public final class ForEach<T, K> extends BaseComponent implements CellConfig<ForEach<T, K>> {
    private final Table container = new Table();
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final Readable<? extends Iterable<T>> collection;
    private final Function<T, K> keyExtractor;
    private final Function<T, Component> itemFactory;
    private final StructuralReconciler<K, Component> reconciler = new StructuralReconciler<>();

    public ForEach(
            Readable<? extends Iterable<T>> collection,
            Function<T, K> keyExtractor,
            Function<T, Component> itemFactory) {
        this.collection = collection;
        this.keyExtractor = keyExtractor;
        this.itemFactory = itemFactory;
        this.container.userObject = this;
    }

    public static <T, K> ForEach<T, K> of(
            Readable<? extends Iterable<T>> collection,
            Function<T, K> keyExtractor,
            Function<T, Component> itemFactory) {
        return new ForEach<>(collection, keyExtractor, itemFactory);
    }

    public Table container() {
        return container;
    }

    @Override
    public PendingCellConfig cellConfig() {
        return constraints;
    }

    @Override
    protected Element build() {
        applyContainerAlign();
        Effect.of(this::reconcile);
        return container;
    }

    private void reconcile() {
        Map<K, Component> active = reconciler.reconcile(collection.get(), keyExtractor, itemFactory);

        container.clearChildren();
        for (Component comp : active.values()) {
            Element el = comp.element();
            Cell<?> cell = container.add(el);
            cell.minWidth(0f);
            PendingCellConfig sc = PendingCellConfig.find(comp);
            if (sc == null) {
                sc = PendingCellConfig.find(el);
            }
            if (sc != null) {
                sc.applyToCell(cell);
            } else if (Ui.isExpanding(el)) {
                cell.growX();
            }
            cell.row();
        }
        applyContainerAlign();
    }

    private void applyContainerAlign() {
        if (constraints.align != null) {
            container.align(constraints.align);
        } else {
            container.top();
        }
    }

    @Override
    protected void onDispose() {
        reconciler.dispose();
        container.clearChildren();
    }
}

package solim.reactive;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.*;
import arc.func.Func;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.SolimToken;

import solim.modifier.CellConfig;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;
import solim.runtime.StructuralReconciler;

/**
 * Keyed reactive list component that efficiently manages child components
 * without rebuilding unchanged items.
 */
public final class ForEach<T> extends BaseComponent
        implements CellConfig<ForEach<T>>, ElementConfig<ForEach<T>>, TableConfig<ForEach<T>> {
    private final Table container = new Table();
    private final PendingCellConfig constraints = new PendingCellConfig();
    private final Readable<? extends Iterable<T>> collection;
    private Func<T, ?> keyExtractor = v -> v;
    private @Nullable Func<T, Component> itemFactory;
    private final StructuralReconciler<Object, Component> reconciler = new StructuralReconciler<>();

    public ForEach(Readable<? extends Iterable<T>> collection) {
        this.collection = collection;
        SolimToken.bind(this.container, this, constraints);
        this.container.top().left();
        this.container.defaults().top().left();
    }

    public static <T> ForEach<T> of(Readable<? extends Iterable<T>> collection) {
        return new ForEach<>(collection);
    }

    public ForEach<T> key(@Nullable Func<T, ?> keyExtractor) {
        this.keyExtractor = keyExtractor != null ? keyExtractor : v -> v;
        return this;
    }

    public void children(@Nullable Func<T, Component> itemFactory) {
        this.itemFactory = itemFactory;
        if (itemFactory != null && isBuilt()) {
            reconcile();
        }
    }

    private Object extractKey(T item) {
        return keyExtractor.get(item);
    }

    public Table container() {
        return container;
    }

    @Override
    public Table table() {
        return container;
    }

    @Override
    public ForEach<T> name(@Nullable String name) {
        super.name(name);
        return this;
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
        Func<T, Component> factory = itemFactory;
        if (factory == null) {
            return;
        }
        Map<Object, Component> active = reconciler.reconcile(collection.get(), this::extractKey, factory);

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
            } else if (SolimToken.isExpandingChild(el)) {
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

    @Override
    public ForEach<T> self() {
        return this;
    }
}

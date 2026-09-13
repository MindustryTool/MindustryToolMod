package solim.layout;

import arc.Core;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Supplier;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.Disposable;
import solim.modifier.ElementModifiers;
import solim.runtime.ComponentContext;
import solim.runtime.ParentStack;
import solim.runtime.StructuralReconciler;
import solim.signal.Computed;
import solim.signal.Effect;
import solim.signal.Readable;
import solim.signal.Signal;
import solim.ui.Ui;
import solim.ui.Units;

/**
 * Keyed reactive grid that reflows existing component cells when column count changes and
 * structurally reconciles items when the item collection changes.
 */
public final class ReactiveGrid<T, K> extends BaseComponent implements LayoutModifiers<ReactiveGrid<T, K>>, GapContainer {
	private final Signal<Float> tableWidth = Signal.of(0f);
	private final Signal<Float> gapSignal = Signal.of(0f);
	private final Computed<Float> itemWidth;
	private final GridItemContext context;

	private final Table table = new Table() {
		@Override
		public void layout() {
			super.layout();
			checkWidth(getWidth());
		}

		@Override
		protected void sizeChanged() {
			super.sizeChanged();
			checkWidth(getWidth());
		}
	};

	private final SizeConstraints constraints = new SizeConstraints();
	private final Readable<Integer> columnCount;
	private final Readable<? extends Iterable<T>> items;
	private final Function<T, K> keyExtractor;
	private final BiFunction<T, GridItemContext, Component> itemFactory;
	private final StructuralReconciler<K, Component> reconciler = new StructuralReconciler<>();
	private final List<Disposable> itemBindings = new ArrayList<>();

	private Runnable emptyRunnable;
	private Supplier<Component> emptyViewSupplier;
	private Component currentEmptyComponent;
	private float gap = 0f;

	public ReactiveGrid(
			Readable<Integer> columnCount,
			Readable<? extends Iterable<T>> items,
			Function<T, K> keyExtractor,
			Function<T, Component> itemFactory) {
		this(columnCount, items, keyExtractor, (item, ctx) -> itemFactory.apply(item));
	}

	public ReactiveGrid(
			Readable<Integer> columnCount,
			Readable<? extends Iterable<T>> items,
			Function<T, K> keyExtractor,
			BiFunction<T, GridItemContext, Component> itemFactory) {
		this.table.name = "solim-reactive-grid-table";
		this.table.userObject = this;
		this.table.update(() -> checkWidth(this.table.getWidth()));
		this.columnCount = columnCount;
		this.items = items;
		this.keyExtractor = keyExtractor;
		this.itemFactory = itemFactory;

		this.itemWidth = new Computed<>(() -> {
			float tw = tableWidth.get();
			int cols = Math.max(1, columnCount.get() != null ? columnCount.get() : 1);
			float g = gapSignal.get();
			float horizontalMargin = table.getMarginLeft() + table.getMarginRight();
			float availableWidth = Math.max(0f, tw - horizontalMargin);
			float totalGaps = (cols - 1) * g;
			if (tw <= 0f) {
				float sw = Units.screenWidth();
				float fallbackW = sw > 0f ? sw - 32f : 300f;
				return Math.max(0f, (fallbackW - totalGaps) / cols);
			}
			return Math.max(0f, (availableWidth - totalGaps) / cols);
		});

		this.context = new GridItemContext() {
			@Override
			public Readable<Float> itemWidth() {
				return itemWidth;
			}

			@Override
			public Readable<Integer> columnCount() {
				return columnCount;
			}
		};

		growX();
	}

	public static <T, K> ReactiveGrid<T, K> of(
			Readable<Integer> columnCount,
			Readable<? extends Iterable<T>> items,
			Function<T, K> keyExtractor,
			Function<T, Component> itemFactory) {
		return new ReactiveGrid<>(columnCount, items, keyExtractor, itemFactory);
	}

	public static <T, K> ReactiveGrid<T, K> of(
			Readable<Integer> columnCount,
			Readable<? extends Iterable<T>> items,
			Function<T, K> keyExtractor,
			BiFunction<T, GridItemContext, Component> itemFactory) {
		return new ReactiveGrid<>(columnCount, items, keyExtractor, itemFactory);
	}

	public GridItemContext context() {
		return context;
	}

	public Readable<Float> itemWidth() {
		return itemWidth;
	}

	private void checkWidth(float w) {
		if (w > 0f && Math.abs(w - tableWidth.get()) > 0.5f) {
			tableWidth.set(w);
		}
	}

	public ReactiveGrid<T, K> empty(Runnable emptyRunnable) {
		this.emptyRunnable = emptyRunnable;
		return this;
	}

	public ReactiveGrid<T, K> emptyView(Supplier<Component> supplier) {
		this.emptyViewSupplier = supplier;
		return this;
	}

	public ReactiveGrid<T, K> gap(float gap) {
		this.gap = gap;
		this.gapSignal.set(gap);
		respace();
		return this;
	}

	@Override
	public Direction direction() {
		return Direction.HORIZONTAL;
	}

	@Override
	public float gap() {
		return gap;
	}

	@Override
	public void respace() {
		int cols = Math.max(1, columnCount.get() != null ? columnCount.get() : 1);
		GapContainer.applyGridSpacing(table, cols, gap);
	}

	public ReactiveGrid<T, K> gap(@Nullable Readable<Float> gapSignal) {
		if (gapSignal != null) {
			ComponentContext.register(Effect.of(() -> {
				Float g = gapSignal.get();
				if (g != null) {
					gap(g);
				}
			}));
		}
		return this;
	}


	public Table table() {
		return table;
	}

	@Override
	public SizeConstraints sizeConstraints() {
		return constraints;
	}

	@Override
	protected Element build() {
		table.top().left();

		Effect.of(() -> {
			Iterable<T> itemList = items.get();
			int cols = Math.max(1, columnCount.get() != null ? columnCount.get() : 1);
			if (table.getScene() != null && Core.app != null) {
				Core.app.post(() -> {
					updateItemsAndReflow(itemList, cols);
				});
			} else {
				updateItemsAndReflow(itemList, cols);
			}
		});

		return table;
	}

	private void updateItemsAndReflow(Iterable<T> itemList, int cols) {
		reconciler.reconcile(itemList, keyExtractor, item -> itemFactory.apply(item, context));
		reflow(cols);
	}

	private void reflow(int cols) {
		for (Disposable d : itemBindings) {
			d.dispose();
		}
		itemBindings.clear();

		table.clear();
		table.top().left();

		if (reconciler.isEmpty()) {
			if (emptyRunnable != null) {
				Table emptyTable = new Table();
				ParentStack.push(emptyTable);
				try {
					emptyRunnable.run();
				} finally {
					ParentStack.pop();
				}
				table.add(emptyTable).center().colspan(cols).growX();
			} else if (emptyViewSupplier != null) {
				if (currentEmptyComponent == null) {
					currentEmptyComponent = ParentStack.isolate(() -> {
						Component c = emptyViewSupplier.get();
						if (c != null) {
							c.element();
						}
						return c;
					});
				}
				table.add(currentEmptyComponent.element()).center().colspan(cols).growX();
			}
			return;
		}

		if (currentEmptyComponent != null) {
			currentEmptyComponent.dispose();
			currentEmptyComponent = null;
		}

		int col = 0;
		for (Component comp : reconciler.activeComponents().values()) {
			Element el = comp.element();
			Cell<?> cell = table.add(el).top().left();
			SizeConstraints sc = null;
			if (comp instanceof LayoutModifiers) {
				sc = ((LayoutModifiers<?>) comp).sizeConstraints();
			} else if (el.userObject instanceof LayoutModifiers) {
				sc = ((LayoutModifiers<?>) el.userObject).sizeConstraints();
			}
			if (sc != null) {
				List<Disposable> effects = sc.applyToCell(cell);
				itemBindings.addAll(effects);
				if (sc.growX || !sc.hasExplicitWidth()) {
					cell.growX().uniformX();
				}
			} else {
				cell.growX().uniformX();
			}
			if (++col % cols == 0) {
				table.row();
			}
		}
		while (col % cols != 0) {
			table.add().uniformX().growX();
			col++;
		}
		table.row();
		respace();
		table.invalidateHierarchy();
	}

	@Override
	protected void onDispose() {
		for (Disposable d : itemBindings) {
			d.dispose();
		}
		itemBindings.clear();

		reconciler.dispose();
		itemWidth.dispose();

		if (currentEmptyComponent != null) {
			currentEmptyComponent.dispose();
			currentEmptyComponent = null;
		}
		table.clear();
	}
}

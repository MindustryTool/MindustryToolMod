package solim.ui;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import java.util.Objects;
import java.util.function.Function;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.GapContainer;
import solim.layout.CellConfig;
import solim.layout.SizeConstraints;
import solim.runtime.ParentStack;
import solim.runtime.ReactiveContext;
import solim.signal.Effect;
import solim.signal.Readable;

/** Structural reactive component for switching dynamic subtrees based on a reactive value. */
public final class Dynamic<T> extends BaseComponent implements CellConfig<Dynamic<T>> {
	private static final Object SENTINEL = new Object();
	private final Table container = new Table();
	private final SizeConstraints constraints = new SizeConstraints();
	private final Readable<T> source;
	private final Function<T, Component> factory;
	private Component currentComponent;
	@SuppressWarnings("unchecked")
	private T lastValue = (T) SENTINEL;
	private final java.util.List<solim.core.Disposable> currentBindings = new java.util.ArrayList<>();

	public Dynamic(Readable<T> source, Function<T, Component> factory) {
		this.source = source;
		this.factory = factory;
		this.container.userObject = this;
	}

	public static <T> Dynamic<T> of(Readable<T> source, Function<T, Component> factory) {
		return new Dynamic<>(source, factory);
	}

	public Table container() {
		return container;
	}

	@Override
	public SizeConstraints sizeConstraints() {
		return constraints;
	}

	@Override
	protected Element build() {
		applyContainerAlign();
		Effect.of(() -> {
			T value = source.get();
			if (Objects.equals(value, lastValue)) {
				return;
			}
			lastValue = value;
			if (currentComponent != null) {
				currentComponent.dispose();
				currentComponent = null;
			}
			for (solim.core.Disposable d : currentBindings) {
				d.dispose();
			}
			currentBindings.clear();
			container.clearChildren();
			if (value != null && factory != null) {
				currentComponent = ReactiveContext.untracked(() ->
					ParentStack.isolate(() -> {
						Component c = factory.apply(value);
						if (c != null) {
							c.element();
						}
						return c;
					})
				);
				if (currentComponent != null) {
					Element el = currentComponent.element();
					Cell<?> cell = container.add(el);
					cell.minWidth(0f);
					SizeConstraints sc = SizeConstraints.find(currentComponent);
					if (sc == null) {
						sc = SizeConstraints.find(el);
					}
					if (sc != null) {
						currentBindings.addAll(sc.applyToCell(cell));
					} else if (Ui.isExpanding(el)) {
						cell.growX();
					}
				}
			}
			applyContainerAlign();
			updateParentCell();
			container.invalidateHierarchy();
		});
		return container;
	}

	private void applyContainerAlign() {
		if (constraints.align != null) {
			container.align(constraints.align);
		} else {
			container.top();
		}
	}

	private void updateParentCell() {
		Cell<?> parentCell = container.parent instanceof Table ? ((Table) container.parent).getCell(container) : null;
		if (currentComponent != null) {
			container.visible = true;
			if (parentCell != null) {
				parentCell.minWidth(Float.NEGATIVE_INFINITY).minHeight(Float.NEGATIVE_INFINITY);
				parentCell.maxWidth(Float.NEGATIVE_INFINITY).maxHeight(Float.NEGATIVE_INFINITY);
				constraints.applyToCell(parentCell);
			}
		} else {
			container.visible = false;
			if (parentCell != null) {
				parentCell.size(0f).pad(0f);
			}
		}
		if (container.parent instanceof Table) {
			GapContainer.respace((Table) container.parent);
		}
	}

	@Override
	protected void onDispose() {
		if (currentComponent != null) {
			currentComponent.dispose();
			currentComponent = null;
		}
		for (solim.core.Disposable d : currentBindings) {
			d.dispose();
		}
		currentBindings.clear();
		container.clearChildren();
	}
}

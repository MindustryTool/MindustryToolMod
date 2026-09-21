package solim.reactive;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import arc.func.Func2;
import arc.func.Func;
import arc.func.Prov;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.core.Disposable;
import solim.core.SolimToken;
import solim.display.Text;
import solim.input.Button;
import solim.layout.GapContainer;
import solim.modifier.CellConfig;
import solim.modifier.ElementConfig;
import solim.modifier.PendingCellConfig;
import solim.modifier.TableConfig;
import solim.runtime.ParentStack;
import solim.runtime.ReactiveContext;

/**
 * Declarative reactive component that binds to a {@link Query} and dynamically
 * renders loading, error, and data states with stale-while-revalidate awareness.
 *
 * @param <T> query data type
 */
public final class QueryView<T> extends BaseComponent
		implements CellConfig<QueryView<T>>, TableConfig<QueryView<T>>, ElementConfig<QueryView<T>> {

	private enum ViewState {
		NONE,
		LOADING,
		ERROR,
		DATA,
		EMPTY
	}

	private static final Object SENTINEL = new Object();

	private static Prov<Component> defaultLoadingFactory = () -> {
		Table table = new Table();
		table.center();
		String text = Core.bundle != null ? Core.bundle.get("loading", "Loading...") : "Loading...";
		Text label = new Text(text).color(Color.lightGray);
		table.add(label.element());
		return () -> table;
	};

	private static Func2<Throwable, Runnable, Component> defaultErrorFactory = (throwable, retry) -> {
		Table table = new Table();
		table.center();
		String msg = throwable != null
				? (throwable.getMessage() != null ? throwable.getMessage() : throwable.toString())
				: "An error occurred";
		Text errorText = new Text(msg).color(Color.scarlet);
		table.add(errorText.element()).padBottom(8f).row();

		String btnText = Core.bundle != null ? Core.bundle.get("button.retry", "Retry") : "Retry";
		Button btn = new Button(retry);
		btn.table().add(new Text(btnText).element());
		table.add(btn.element());
		return () -> table;
	};

	private final Query<T> query;
	private final Table container = new Table();
	private final PendingCellConfig constraints = new PendingCellConfig();

	private @Nullable Func<T, Component> dataFactory;
	private @Nullable Func2<T, Boolean, Component> dataWithFetchingFactory;
	private @Nullable Prov<Component> loadingFactory;
	private @Nullable Func<Throwable, Component> errorFactory;

	private Component currentComponent;
	private final List<Disposable> currentBindings = new ArrayList<>();

	private ViewState currentViewState = ViewState.NONE;
	@SuppressWarnings("unchecked")
	private T lastData = (T) SENTINEL;
	private @Nullable Throwable lastError = null;
	private @Nullable Boolean lastFetching = null;

	public QueryView(Query<T> query) {
		this.query = Objects.requireNonNull(query, "query cannot be null");
		SolimToken.bind(this.container, this, constraints);
		this.container.top().left();
		this.container.defaults().top().left();
	}

	public static <T> QueryView<T> of(Query<T> query) {
		return new QueryView<>(query);
	}

	public static void setDefaultLoadingFactory(Prov<Component> factory) {
		defaultLoadingFactory = Objects.requireNonNull(factory, "factory cannot be null");
	}

	public static void setDefaultErrorFactory(Func2<Throwable, Runnable, Component> factory) {
		defaultErrorFactory = Objects.requireNonNull(factory, "factory cannot be null");
	}

	private @Nullable Effect effect;
	private boolean built = false;

	public QueryView<T> data(Func<T, Component> dataFactory) {
		this.dataFactory = dataFactory;
		if (built && (currentViewState == ViewState.DATA || currentComponent == null)) {
			currentViewState = ViewState.NONE;
			if (effect != null) {
				effect.runPending();
			}
		}
		return this;
	}

	public QueryView<T> data(Func2<T, Boolean, Component> dataWithFetchingFactory) {
		this.dataWithFetchingFactory = dataWithFetchingFactory;
		if (built && (currentViewState == ViewState.DATA || currentComponent == null)) {
			currentViewState = ViewState.NONE;
			if (effect != null) {
				effect.runPending();
			}
		}
		return this;
	}

	public QueryView<T> loading(Prov<Component> loadingFactory) {
		this.loadingFactory = loadingFactory;
		if (built && currentViewState == ViewState.LOADING) {
			currentViewState = ViewState.NONE;
			if (effect != null) {
				effect.runPending();
			}
		}
		return this;
	}

	public QueryView<T> error(Func<Throwable, Component> errorFactory) {
		this.errorFactory = errorFactory;
		if (built && currentViewState == ViewState.ERROR) {
			currentViewState = ViewState.NONE;
			if (effect != null) {
				effect.runPending();
			}
		}
		return this;
	}

	public Table container() {
		return container;
	}

	@Override
	public Table table() {
		return container;
	}

	@Override
	public QueryView<T> name(@Nullable String name) {
		super.name(name);
		return this;
	}

	@Override
	public PendingCellConfig cellConfig() {
		return constraints;
	}

	@Override
	protected Element build() {
		built = true;
		applyContainerAlign();
		query.ensureFresh();
		effect = Effect.of(this::updateView);
		return container;
	}

	private void updateView() {
		if (isDisposed()) return;

		boolean isLoading = Boolean.TRUE.equals(query.loading().get());
		Throwable err = query.error().get();
		T data = query.data().get();
		boolean isFetching = Boolean.TRUE.equals(query.fetching().get());

		ViewState targetState;
		if (isLoading) {
			targetState = ViewState.LOADING;
		} else if (err != null && data == null) {
			targetState = ViewState.ERROR;
		} else if (data != null) {
			targetState = ViewState.DATA;
		} else {
			targetState = ViewState.EMPTY;
		}

		// Avoid unnecessary rebuilding if neither state nor data changed
		if (currentComponent != null && targetState == currentViewState) {
			if (targetState == ViewState.DATA) {
				boolean isFetchingRelevant = dataWithFetchingFactory != null;
				if (Objects.equals(data, lastData) && (!isFetchingRelevant || Objects.equals(isFetching, lastFetching))) {
					return;
				}
			} else if (targetState == ViewState.LOADING) {
				return;
			} else if (targetState == ViewState.ERROR && Objects.equals(err, lastError)) {
				return;
			}
		}

		currentViewState = targetState;
		lastData = data;
		lastError = err;
		lastFetching = isFetching;

		cleanupCurrent();

		currentComponent = ReactiveContext.untracked(() -> ParentStack.isolate(() -> {
			try {
				switch (targetState) {
					case LOADING:
						return loadingFactory != null ? loadingFactory.get() : defaultLoadingFactory.get();
					case ERROR:
						return errorFactory != null
								? errorFactory.get(err)
								: defaultErrorFactory.get(err, query::refetch);
					case DATA:
						if (dataWithFetchingFactory != null) {
							return dataWithFetchingFactory.get(data, isFetching);
						} else if (dataFactory != null) {
							return dataFactory.get(data);
						}
						return null;
					case EMPTY:
					default:
						return null;
				}
			} catch (Throwable t) {
				Log.err("[Solim] Error rendering QueryView", t);
				return defaultErrorFactory.get(t, query::refetch);
			}
		}));

		if (currentComponent != null) {
			Element el = currentComponent.element();
			Cell<?> cell = container.add(el);
			cell.minWidth(0f);
			PendingCellConfig sc = PendingCellConfig.find(currentComponent);
			if (sc == null) {
				sc = PendingCellConfig.find(el);
			}
			if (sc != null) {
				currentBindings.addAll(sc.applyToCell(cell));
			} else if (SolimToken.isExpandingChild(el)) {
				cell.growX();
			}
		}

		applyContainerAlign();
		updateParentCell();
		container.invalidateHierarchy();
	}

	private void cleanupCurrent() {
		if (currentComponent != null) {
			currentComponent.dispose();
			currentComponent = null;
		}
		for (Disposable d : currentBindings) {
			d.dispose();
		}
		currentBindings.clear();
		container.clearChildren();
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
		cleanupCurrent();
		if (effect != null) {
			effect.dispose();
			effect = null;
		}
	}

	@Override
	public QueryView<T> self() {
		return this;
	}
}

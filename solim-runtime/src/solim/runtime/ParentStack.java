package solim.runtime;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import arc.func.Cons2;
import arc.func.Boolf;
import arc.func.Prov;
import solim.core.Component;
import solim.core.SpacingAware;
import solim.performance.PerfSpan;

/**
 * Implicit parent stack for declarative UI construction with guaranteed cleanup. Supports
 * customizable cell attachment strategies per container.
 *
 * <p>The base class holds only structural state and declares empty profiling hooks. All static
 * entry points delegate to a singleton instance, allowing a profiling-enabled subclass to be
 * installed without the base class carrying any monitoring logic.
 */
public class ParentStack {

	@FunctionalInterface
	public interface Attacher {
		Cell<?> attach(Table parent, Element child);
	}

	public static class Entry {
		public final Table table;
		public final Attacher attacher;
		public final List<Component> pendingComponents = new ArrayList<>();

		public Entry(Table table, Attacher attacher) {
			this.table = table;
			this.attacher = attacher != null ? attacher : Table::add;
		}
	}

	@FunctionalInterface
	public interface CellConfigurator {
		void configure(Cell<?> cell, Element child, @Nullable Component component);
	}

	private static volatile ParentStack INSTANCE = new ParentStack();
	private static final ThreadLocal<Deque<List<Component>>> CAPTURED_LIST_POOL =
			ThreadLocal.withInitial(ArrayDeque::new);

	static List<Component> takeCaptureList() {
		Deque<List<Component>> pool = CAPTURED_LIST_POOL.get();
		List<Component> list = pool.pollFirst();
		return list != null ? list : new ArrayList<>();
	}

	static void releaseCaptureList(List<Component> list) {
		if (list != null) {
			list.clear();
			CAPTURED_LIST_POOL.get().push(list);
		}
	}

	protected final Deque<Entry> stack = new ArrayDeque<>();
	private @Nullable CellConfigurator cellConfigurator = null;

	protected ParentStack() {}

	/** Returns the active singleton instance. */
	public static ParentStack instance() {
		return INSTANCE;
	}

	/** Installs the active singleton instance, falling back to a fresh base instance for {@code null}. */
	public static void install(@Nullable ParentStack instance) {
		INSTANCE = instance != null ? instance : new ParentStack();
	}

	public static void setCellConfigurator(@Nullable CellConfigurator configurator) {
		INSTANCE.doSetCellConfigurator(configurator);
	}

	public static void setCellConfigurator(@Nullable Cons2<Cell<?>, Element> configurator) {
		INSTANCE.doSetCellConfigurator(configurator != null
				? (cell, child, comp) -> configurator.get(cell, child)
				: null);
	}

	public static void push(Table parent) {
		INSTANCE.doPush(parent, null);
	}

	public static void push(Table parent, Attacher attacher) {
		INSTANCE.doPush(parent, attacher);
	}

	public static Table pop() {
		return INSTANCE.doPop();
	}

	public static Table current() {
		return INSTANCE.doCurrent();
	}

	public static @Nullable Table find(Boolf<Table> Boolf) {
		return INSTANCE.doFind(Boolf);
	}

	public static void clear() {
		INSTANCE.doClear();
	}

	public static int size() {
		return INSTANCE.doSize();
	}

	/**
	 * Executes the given Prov in an isolated ParentStack context where no parent is on the stack.
	 */
	public static <T> T isolate(Prov<T> Prov) {
		return INSTANCE.doIsolate(Prov);
	}

	/**
	 * Executes the given runnable in an isolated ParentStack context where no parent is on the stack.
	 */
	public static void isolate(Runnable runnable) {
		INSTANCE.doIsolate(runnable);
	}

	/**
	 * Runs the given block in an isolated context and captures the components created inside it,
	 * in creation order, without attaching them to any live parent. Used by dynamic structural
	 * components to adopt void factories: the captured components become owned by the caller,
	 * which is responsible for mounting and disposing them.
	 *
	 * <p>If the block throws, all partially created components are disposed, ambient state is
	 * restored, and the exception propagates.
	 */
	public static List<Component> capture(Runnable runnable) {
		if (runnable == null) {
			return Collections.emptyList();
		}
		List<Component> captured = new ArrayList<>();
		INSTANCE.doCapture(runnable, captured);
		return captured;
	}

	/** Internal capture using a pre-allocated list to reduce allocation pressure. */
	static List<Component> capture(Runnable runnable, List<Component> captured) {
		INSTANCE.doCapture(runnable, captured);
		return captured;
	}

	/**
	 * Registers a component to be attached when its parent is popped, ensuring the component is fully
	 * constructed before element() is invoked.
	 */
	public static void registerPendingComponent(Component component, Table parent) {
		INSTANCE.doRegisterPendingComponent(component, parent);
	}

	/** Attaches all pending components registered for the given entry. */
	public static void attachPendingComponents(Entry entry) {
		INSTANCE.doAttachPendingComponents(entry);
	}

	/** Attaches all pending components registered for the given table. */
	public static void attachPendingComponents(Table parent) {
		INSTANCE.doAttachPendingComponents(parent);
	}

	/** Attach child to current parent if one exists; otherwise no-op. */
	public static void attachToParent(Element child) {
		INSTANCE.doAttachToParent(child);
	}

	public static Element add(Object child) {
		return INSTANCE.doAdd(child);
	}

	public static List<PerfSpan> snapshot(int max) {
		return INSTANCE.doSnapshot(max);
	}

	public static int totalRecorded() {
		return INSTANCE.doTotalRecorded();
	}

	public static void reset() {
		INSTANCE.doReset();
	}

	public static void setThreshold(float ms) {
		INSTANCE.doSetThreshold(ms);
	}

	public static int maxDepthObserved() {
		return INSTANCE.doMaxDepthObserved();
	}

	protected void doSetCellConfigurator(@Nullable CellConfigurator configurator) {
		this.cellConfigurator = configurator;
	}

	protected void doPush(Table parent, Attacher attacher) {
		SolimAssert.checkMainThread();
		if (parent != null) {
			stack.push(new Entry(parent, attacher));
		}
	}

	protected Table doPop() {
		SolimAssert.checkMainThread();
		if (!stack.isEmpty()) {
			Entry popped = stack.pop();
			doAttachPendingComponents(popped);
			return popped.table;
		}
		return null;
	}

	protected Table doCurrent() {
		return stack.isEmpty() ? null : stack.peek().table;
	}

	protected @Nullable Table doFind(Boolf<Table> Boolf) {
		if (Boolf == null) return null;
		for (Entry entry : stack) {
			if (Boolf.get(entry.table)) {
				return entry.table;
			}
		}
		return null;
	}

	protected void doClear() {
		stack.clear();
	}

	protected int doSize() {
		return stack.size();
	}

	protected <T> T doIsolate(Prov<T> Prov) {
		if (Prov == null) {
			return null;
		}
		Deque<Entry> saved = new ArrayDeque<>(stack);
		stack.clear();
		try {
			return Prov.get();
		} finally {
			stack.clear();
			stack.addAll(saved);
		}
	}

	protected void doIsolate(Runnable runnable) {
		if (runnable == null) {
			return;
		}
		Deque<Entry> saved = new ArrayDeque<>(stack);
		stack.clear();
		try {
			runnable.run();
		} finally {
			stack.clear();
			stack.addAll(saved);
		}
	}

	protected void doCapture(Runnable runnable, List<Component> captured) {
		SolimAssert.checkMainThread();
		if (runnable == null) {
			captured.clear();
			return;
		}
		// Save ambient stack; skip copy when already empty.
		Deque<Entry> saved = stack.isEmpty() ? null : new ArrayDeque<>(stack);
		stack.clear();
		captured.clear();
		// Capture through ComponentContext: components register there in creation order
		// (BaseComponent via registerChild, plain components like Row/Column via register),
		// while plain disposables (bindings) belong to their owning component and are ignored.
		ComponentContext.pushCapture(captured);
		// Runnable.run() cannot throw checked exceptions, so RuntimeException/Error is exhaustive.
		RuntimeException runtimeFailure = null;
		Error errorFailure = null;
		try {
			runnable.run();
		} catch (RuntimeException t) {
			runtimeFailure = t;
		} catch (Error t) {
			errorFailure = t;
		} finally {
			stack.clear();
			if (saved != null) {
				stack.addAll(saved);
			}
			ComponentContext.pop();
		}
		if (runtimeFailure != null || errorFailure != null) {
			for (Component c : captured) {
				try {
					c.dispose();
				} catch (Throwable ex) {
					Log.err("Error disposing partially captured component", ex);
				}
			}
		}
		if (runtimeFailure != null) {
			throw runtimeFailure;
		}
		if (errorFailure != null) {
			throw errorFailure;
		}
	}

	protected void doRegisterPendingComponent(Component component, Table parent) {
		if (component != null && parent != null) {
			for (Entry entry : stack) {
				if (entry.table == parent) {
					entry.pendingComponents.add(component);
					break;
				}
			}
		}
	}

	protected void doAttachPendingComponents(Entry entry) {
		if (entry == null || entry.pendingComponents.isEmpty()) {
			return;
		}
		List<Component> list = new ArrayList<>(entry.pendingComponents);
		entry.pendingComponents.clear();
		for (Component comp : list) {
			Element el = doIsolate(comp::element);
			if (el != null && el.parent == null) {
				doAttach(entry.table, el, entry.attacher, comp);
			}
			if (comp instanceof SpacingAware) {
				((SpacingAware) comp).applySpacing();
			}
		}
	}

	protected void doAttachPendingComponents(Table parent) {
		if (parent == null) {
			return;
		}
		for (Entry entry : stack) {
			if (entry.table == parent) {
				doAttachPendingComponents(entry);
				break;
			}
		}
	}

	protected void doAttachToParent(Element child) {
		if (child == null || stack.isEmpty()) {
			return;
		}
		Entry entry = stack.peek();
		doAttachPendingComponents(entry);
		doAttach(entry.table, child, entry.attacher, null);
	}

	protected Element doAdd(Object child) {
		Element e = ElementResolver.resolve(child);
		doAttachToParent(e);
		return e;
	}

	private void doAttach(Table parent, Element child, Attacher attacher, @Nullable Component comp) {
		if (parent != null && child != null) {
			if (child.parent != parent && !parent.getChildren().contains(child, true)) {
				Cell<?> cell;
				if (attacher != null) {
					cell = attacher.attach(parent, child);
				} else {
					cell = parent.add(child);
				}
				if (cell != null && cellConfigurator != null) {
					cellConfigurator.configure(cell, child, comp);
				}
			}
		}
	}

	protected List<PerfSpan> doSnapshot(int max) {
		return Collections.emptyList();
	}

	protected int doTotalRecorded() {
		return 0;
	}

	protected void doReset() {
	}

	protected void doSetThreshold(float ms) {
	}

	protected int doMaxDepthObserved() {
		return 0;
	}
}

package solim.runtime;

import arc.scene.Element;
import arc.scene.ui.layout.Cell;
import arc.scene.ui.layout.Table;
import arc.util.Nullable;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import arc.func.Cons2;
import arc.func.Boolf;
import arc.func.Prov;
import solim.core.AttachmentBridge;
import solim.core.Component;
import solim.core.DisposableOwner;
import solim.core.SolimAssert;
import solim.core.SpacingAware;
import solim.instrumentation.AttachSubtree;
import solim.instrumentation.Instrumentation;

public final class AttachmentStack {

    static {
        AttachmentBridge.install(component -> {
            Table parent = AttachmentStack.current();
            if (parent != null) {
                AttachmentStack.registerPendingComponent(component, parent);
            }
        });
    }

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

    private static final Deque<Entry> stack = new ArrayDeque<>();
    private static final ThreadLocal<Deque<List<Component>>> capturedListPool = ThreadLocal
            .withInitial(ArrayDeque::new);

    private static int suspendFloor = 0;

    private static @Nullable CellConfigurator cellConfigurator = null;

    private AttachmentStack() {
    }

    static List<Component> takeCaptureList() {
        Deque<List<Component>> pool = capturedListPool.get();
        List<Component> list = pool.pollFirst();
        return list != null ? list : new ArrayList<>();
    }

    static void releaseCaptureList(List<Component> list) {
        if (list != null) {
            list.clear();
            capturedListPool.get().push(list);
        }
    }

    public static void setCellConfigurator(@Nullable CellConfigurator configurator) {
        cellConfigurator = configurator;
    }

    public static void setCellConfigurator(@Nullable Cons2<Cell<?>, Element> configurator) {
        cellConfigurator = configurator != null
                ? (cell, child, comp) -> configurator.get(cell, child)
                : null;
    }

    public static void push(Table parent) {
        push(parent, null);
    }

    public static void push(Table parent, Attacher attacher) {
        SolimAssert.checkMainThread();
        if (parent == null) {
            return;
        }
        stack.push(new Entry(parent, attacher));
        if (Instrumentation.enabled()) {
            Instrumentation.emit(AttachSubtree.opened(parent.name, stack.size(), System.nanoTime()));
        }
    }

    public static @Nullable Table pop() {
        SolimAssert.checkMainThread();
        if (stack.isEmpty()) {
            return null;
        }
        Entry popped = stack.pop();
        attachPendingComponents(popped);
        if (Instrumentation.enabled()) {
            Instrumentation.emit(AttachSubtree.closed(popped.table.name, stack.size() + 1,
                    popped.table.getChildren().size, System.nanoTime()));
        }
        return popped.table;
    }

    public static @Nullable Table current() {
        return stack.size() > suspendFloor ? stack.peek().table : null;
    }

    public static @Nullable Table find(Boolf<Table> predicate) {
        if (predicate == null) {
            return null;
        }
        int visible = stack.size() - suspendFloor;
        int index = 0;
        for (Entry entry : stack) {
            if (index++ >= visible) {
                break;
            }
            if (predicate.get(entry.table)) {
                return entry.table;
            }
        }
        return null;
    }

    public static void clear() {
        stack.clear();
        suspendFloor = 0;
    }

    public static int size() {
        return stack.size();
    }

    public static <T> T isolate(Prov<T> supplier) {
        if (supplier == null) {
            return null;
        }
        SolimAssert.checkMainThread();
        int depth = stack.size();
        int floor = suspendFloor;
        suspendFloor = Math.max(floor, depth);
        try {
            return supplier.get();
        } finally {
            suspendFloor = floor;
            truncate(depth);
        }
    }

    public static void isolate(Runnable runnable) {
        if (runnable == null) {
            return;
        }
        SolimAssert.checkMainThread();
        int depth = stack.size();
        int floor = suspendFloor;
        suspendFloor = Math.max(floor, depth);
        try {
            runnable.run();
        } finally {
            suspendFloor = floor;
            truncate(depth);
        }
    }

    public static List<Component> capture(@Nullable Runnable runnable) {
        if (runnable == null) {
            return Collections.emptyList();
        }
        List<Component> captured = new ArrayList<>();
        capture(runnable, captured, null);
        return captured;
    }

    static void capture(Runnable runnable, List<Component> captured, @Nullable DisposableOwner sink) {
        SolimAssert.checkMainThread();
        captured.clear();
        if (runnable == null) {
            return;
        }

        OwnershipScope scope = OwnershipScope.open();

        RuntimeException runtimeFailure = null;
        Error errorFailure = null;
        try {
            isolate(runnable);
            scope.extractComponents(captured);
            scope.adoptInto(sink);
        } catch (RuntimeException t) {
            runtimeFailure = t;
        } catch (Error t) {
            errorFailure = t;
        } finally {
            scope.close();
        }
        if (runtimeFailure != null) {
            captured.clear();
            throw runtimeFailure;
        }
        if (errorFailure != null) {
            captured.clear();
            throw errorFailure;
        }
    }

    public static void registerPendingComponent(Component component, Table parent) {
        if (component != null && parent != null) {
            int visible = stack.size() - suspendFloor;
            int index = 0;
            for (Entry entry : stack) {
                if (index++ >= visible) {
                    break;
                }
                if (entry.table == parent) {
                    entry.pendingComponents.add(component);
                    break;
                }
            }
        }
    }

    public static void attachPendingComponents(Entry entry) {
        if (entry == null || entry.pendingComponents.isEmpty()) {
            return;
        }
        List<Component> list = new ArrayList<>(entry.pendingComponents);
        entry.pendingComponents.clear();
        for (Component comp : list) {
            Element el = isolate(comp::element);
            if (el != null && el.parent == null) {
                attach(entry.table, el, entry.attacher, comp);
            }
            if (comp instanceof SpacingAware) {
                ((SpacingAware) comp).applySpacing();
            }
        }
    }

    public static void attachPendingComponents(@Nullable Table parent) {
        if (parent == null) {
            return;
        }
        int visible = stack.size() - suspendFloor;
        int index = 0;
        for (Entry entry : stack) {
            if (index++ >= visible) {
                break;
            }
            if (entry.table == parent) {
                attachPendingComponents(entry);
                break;
            }
        }
    }

    public static void attachToParent(Element child) {
        if (child == null || stack.size() <= suspendFloor) {
            return;
        }
        Entry entry = stack.peek();
        attachPendingComponents(entry);
        attach(entry.table, child, entry.attacher, null);
    }

    public static Element add(Object child) {
        Element element = ElementResolver.resolve(child);
        attachToParent(element);
        return element;
    }

    private static void truncate(int depth) {
        while (stack.size() > depth) {
            stack.pop();
        }
    }

    private static void attach(Table parent, Element child, Attacher attacher, @Nullable Component comp) {
        if (parent == null || child == null) {
            return;
        }
        if (child.parent != parent && !parent.getChildren().contains(child, true)) {
            Cell<?> cell = attacher != null ? attacher.attach(parent, child) : parent.add(child);
            CellConfigurator configurator = cellConfigurator;
            if (cell != null && configurator != null) {
                configurator.configure(cell, child, comp);
            }
        }
    }
}
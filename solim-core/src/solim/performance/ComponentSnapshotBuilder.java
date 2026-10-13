package solim.performance;

import arc.scene.Element;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import solim.core.Component;
import solim.core.DebugReactiveNode;
import solim.core.Disposable;
import solim.core.SessionRoot;
import solim.runtime.DebugReactiveContext;
import solim.runtime.ReactiveContext;

/**
 * Assembles the component tree and reactive graph snapshot consumed by the
 * profile report.
 *
 * <p>
 * Each component is attributed the reactive nodes from two independent sources:
 * <ol>
 * <li>nodes created while that component was the active
 * {@code OwnershipContext} owner, recorded by
 * {@link DebugReactiveContext}, and</li>
 * <li>nodes reachable through the dependency graphs of the {@code Computed}s
 * and {@code Effect}s it
 * owns, which is what surfaces shared or external state driving the
 * component.</li>
 * </ol>
 *
 * <p>
 * Node types are discovered structurally rather than by concrete type: a
 * {@link DebugReactiveNode}
 * whose {@code debugLabel()} reports an effect or a computed is filed
 * accordingly. That keeps this
 * builder free of any dependency on the reactive package's concrete classes
 * while still separating
 * the three categories in the report.
 *
 * <p>
 * Layout bounds are read through {@link Component#peekElement()}, which never
 * triggers a build,
 * so capturing a profile does not mutate the tree being observed.
 */
public final class ComponentSnapshotBuilder {

    private static final int MAX_DEPTH = 64;

    private ComponentSnapshotBuilder() {
    }

    /**
     * Captures the live component hierarchy rooted at {@link SessionRoot#get()}.
     *
     * @return one snapshot per session-root child; empty when no components are
     *         live
     */
    public static List<ComponentNodeSnapshot> capture() {
        List<ComponentNodeSnapshot> roots = new ArrayList<>();
        Map<Component, Boolean> visited = new IdentityHashMap<>();
        int[] nextId = { 1 };
        for (Component child : SessionRoot.get().children()) {
            captureComponent(child, visited, nextId, 0, roots);
        }
        return roots;
    }

    private static void captureComponent(@Nullable Component component, Map<Component, Boolean> visited,
            int[] nextId, int depth, List<ComponentNodeSnapshot> out) {
        if (component == null || depth > MAX_DEPTH || visited.put(component, Boolean.TRUE) != null) {
            return;
        }

        List<ComponentNodeSnapshot> children = new ArrayList<>();
        for (Component child : component.children()) {
            captureComponent(child, visited, nextId, depth + 1, children);
        }

        Element element = component.peekElement();
        NodeReactive reactive = collectReactive(component);

        out.add(new ComponentNodeSnapshot(nextId[0]++,
                component.getClass().getSimpleName(),
                component.name(),
                element != null ? element.getClass().getSimpleName() : null,
                boundsOf(element),
                element != null && element.visible,
                component.isDisposed(),
                children,
                reactive.signals,
                reactive.computeds,
                reactive.effects,
                reactive.others));
    }

    private static float[] boundsOf(@Nullable Element element) {
        if (element == null) {
            return new float[] { 0f, 0f, 0f, 0f };
        }
        return new float[] { element.x, element.y, element.getWidth(), element.getHeight() };
    }

    private static final class NodeReactive {
        final List<SignalSnapshot> signals = new ArrayList<>();
        final List<ComputedSnapshot> computeds = new ArrayList<>();
        final List<EffectSnapshot> effects = new ArrayList<>();
        final List<String> others = new ArrayList<>();
    }

    private static NodeReactive collectReactive(Component component) {
        NodeReactive result = new NodeReactive();
        DebugReactiveContext debug = debugContext();

        // Ordered, identity-deduplicated attribution: nodes created directly in this
        // component's
        // build scope first, then everything transitively reachable through the
        // reactive nodes the
        // component owns. Ordering keeps report output stable between runs.
        List<Object> attributed = new ArrayList<>();
        Set<Object> seen = DebugReactiveContext.newIdentitySet();
        if (debug != null) {
            for (Object node : debug.createdNodes(component)) {
                addAttributed(attributed, seen, node);
            }
        }
        for (Disposable disposable : component.disposables()) {
            if (!(disposable instanceof DebugReactiveNode)) {
                result.others.add(disposable.getClass().getSimpleName());
                continue;
            }
            for (Object node : DebugReactiveContext.reachableSources(disposable)) {
                addAttributed(attributed, seen, node);
            }
            addAttributed(attributed, seen, disposable);
        }

        if (debug == null) {
            return result;
        }
        for (Object node : attributed) {
            SignalSnapshot signal = debug.signalSnapshot(node);
            if (signal != null) {
                result.signals.add(signal);
                continue;
            }
            ComputedSnapshot computed = debug.computedSnapshot(node);
            if (computed != null) {
                result.computeds.add(computed);
                continue;
            }
            EffectSnapshot effect = debug.effectSnapshot(node);
            if (effect != null) {
                result.effects.add(effect);
            }
        }
        return result;
    }

    private static void addAttributed(List<Object> attributed, Set<Object> seen, @Nullable Object node) {
        if (node != null && seen.add(node)) {
            attributed.add(node);
        }
    }

    private static @Nullable DebugReactiveContext debugContext() {
        ReactiveContext context = ReactiveContext.instance();
        return context instanceof DebugReactiveContext ? (DebugReactiveContext) context : null;
    }

    public static List<ComponentNodeSnapshot> empty() {
        return Collections.emptyList();
    }
}

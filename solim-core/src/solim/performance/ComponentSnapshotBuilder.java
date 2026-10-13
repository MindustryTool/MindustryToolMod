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

public final class ComponentSnapshotBuilder {

    private static final int MAX_DEPTH = 64;

    private ComponentSnapshotBuilder() {
    }

    public static List<ComponentNodeSnapshot> capture() {
        return capture(Perf.sink());
    }

    public static List<ComponentNodeSnapshot> capture(@Nullable ProfilerSink sink) {
        List<ComponentNodeSnapshot> roots = new ArrayList<>();
        Map<Component, Boolean> visited = new IdentityHashMap<>();
        int[] nextId = { 1 };
        for (Component child : SessionRoot.get().children()) {
            captureComponent(sink, child, visited, nextId, 0, roots);
        }
        return roots;
    }

    private static void captureComponent(@Nullable ProfilerSink sink, @Nullable Component component,
            Map<Component, Boolean> visited, int[] nextId, int depth,
            List<ComponentNodeSnapshot> out) {
        if (component == null || depth > MAX_DEPTH || visited.put(component, Boolean.TRUE) != null) {
            return;
        }

        List<ComponentNodeSnapshot> children = new ArrayList<>();
        for (Component child : component.children()) {
            captureComponent(sink, child, visited, nextId, depth + 1, children);
        }

        Element element = component.peekElement();
        NodeReactive reactive = collectReactive(sink, component);

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

    private static NodeReactive collectReactive(@Nullable ProfilerSink sink, Component component) {
        NodeReactive result = new NodeReactive();

        List<Object> attributed = new ArrayList<>();
        Set<Object> seen = ProfilerSink.newIdentitySet();
        if (sink != null) {
            for (Object node : sink.createdNodes(component)) {
                addAttributed(attributed, seen, node);
            }
        }
        for (Disposable disposable : component.disposables()) {
            if (!(disposable instanceof DebugReactiveNode)) {
                result.others.add(disposable.getClass().getSimpleName());
                continue;
            }
            for (Object node : ProfilerSink.reachableSources(disposable)) {
                addAttributed(attributed, seen, node);
            }
            addAttributed(attributed, seen, disposable);
        }

        if (sink == null) {
            return result;
        }
        for (Object node : attributed) {
            SignalSnapshot signal = sink.signalSnapshot(node);
            if (signal != null) {
                result.signals.add(signal);
                continue;
            }
            ComputedSnapshot computed = sink.computedSnapshot(node);
            if (computed != null) {
                result.computeds.add(computed);
                continue;
            }
            EffectSnapshot effect = sink.effectSnapshot(node);
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

    public static List<ComponentNodeSnapshot> empty() {
        return Collections.emptyList();
    }
}
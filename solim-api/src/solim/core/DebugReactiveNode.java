package solim.core;

import java.util.List;

/**
 * Read-only introspection seam implemented by the reactive primitives
 * ({@code Signal},
 * {@code Computed}, {@code Effect}) so the runtime module can inspect them
 * without depending on the
 * module that declares them.
 *
 * <p>
 * Normal application code never implements or calls this. It exists for the
 * profiling runtime in
 * {@code solim-runtime}, which must be able to walk dependency graphs and
 * render value summaries
 * while the debug services are installed.
 */
public interface DebugReactiveNode {

    String debugLabel();

    /**
     * Returns a truncated, human-readable summary of this node's current value.
     * Implementations
     * must guard against exceptions from {@code toString()} and must never return
     * the live object.
     */
    String debugValue();

    List<ReactiveSource> debugDependencies();

    List<ReactiveObserver> debugObservers();

    int debugListenerCount();

    boolean debugDirty();
}

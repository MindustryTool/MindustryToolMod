package solim.core;

/**
 * Reactive source in the dependency graph (Signal, Computed, MapSignal, key readables).
 * Observers attach and detach polymorphically instead of concrete-type discovery.
 *
 * <p>Internal by convention: application code should read sources via {@code get()}
 * inside {@code Computed} or {@code Effect} and never wire the graph by hand.
 */
public interface ReactiveSource {

    /**
     * Attaches an observer notified when this source changes.
     *
     * @param observer the computed or effect observing this source
     */
    void addObserver(ReactiveObserver observer);

    /**
     * Detaches a previously attached observer.
     *
     * @param observer the observer to remove
     */
    void removeObserver(ReactiveObserver observer);
}

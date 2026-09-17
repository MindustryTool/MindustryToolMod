package solim.layout;

import solim.reactive.Readable;

/**
 * Contextual metrics provided by {@link ReactiveGrid} to child item component factories.
 */
public interface GridItemContext {

    /**
     * Reactive usable content width of each column cell in the grid,
     * calculated from the backing grid width, column count, and padding/gap.
     */
    Readable<Float> itemWidth();

    /**
     * Reactive column count of the grid.
     */
    Readable<Integer> columnCount();
}

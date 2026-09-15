package solim.layout;

/**
 * Functional interface for computing the layout height of an item in a {@link VirtualList}
 * given the available container width.
 *
 * @param <T> the item type
 */
@FunctionalInterface
public interface ItemHeightProvider<T> {
    /**
     * Calculates the height for the given item.
     *
     * @param item           the item to measure
     * @param containerWidth the available width of the container
     * @return the layout height in pixels
     */
    float getHeight(T item, float containerWidth);
}

package solim.layout;

@FunctionalInterface
public interface ItemHeightProvider<T> {

    float getHeight(T item, float containerWidth);
}

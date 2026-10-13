package solim.layout;

import solim.reactive.Readable;

public interface GridItemContext {

    Readable<Float> itemWidth();

    Readable<Integer> columnCount();
}

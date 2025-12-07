package mindustrytool.domain.handler;

import arc.func.Cons;

/**
 * Base interface for content handlers (Map/Schematic).
 * @param <T> Content data type
 */
public interface ContentHandler<T> {
    void download(T content);
    void copy(T content);
    void downloadData(T content, Cons<String> callback);
}

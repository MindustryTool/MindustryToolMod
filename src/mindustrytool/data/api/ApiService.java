package mindustrytool.data.api;

import arc.func.Cons;

/**
 * Base interface for API operations.
 * @param <T> Detail data type
 */
public interface ApiService<T> {
    void findById(String id, Cons<T> callback);
    void downloadData(String id, Cons<byte[]> callback);
}

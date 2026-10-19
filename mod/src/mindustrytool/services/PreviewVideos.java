package mindustrytool.services;

import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import arc.util.Nullable;

public final class PreviewVideos {

    private static final int MAX_ENTRIES = 128;

    private static final Request probe = Request.builder()
            .timeout(Duration.ofSeconds(10))
            .build();

    private static final Request.BodyHandler<Void> discard = stream -> {
        stream.close();
        return null;
    };

    private static final Map<String, CompletableFuture<Boolean>> available = new LinkedHashMap<String, CompletableFuture<Boolean>>(
            16, 0.75f, true) {
        @Override
        protected boolean removeEldestEntry(Map.Entry<String, CompletableFuture<Boolean>> eldest) {
            return size() > MAX_ENTRIES;
        }
    };

    private PreviewVideos() {
    }

    public static CompletableFuture<Boolean> exists(@Nullable String url) {
        if (url == null || url.trim().isEmpty()) {
            return CompletableFuture.completedFuture(false);
        }
        synchronized (available) {
            CompletableFuture<Boolean> cached = available.get(url);
            if (cached != null) {
                return cached;
            }
            CompletableFuture<Boolean> result = probe.get(url)
                    .withoutAuth()
                    .noRetry()
                    .header("Range", "bytes=0-0")
                    .sendAsync(discard)
                    .handle((response, error) -> error == null);
            available.put(url, result);
            result.thenAccept(exists -> {
                if (!Boolean.TRUE.equals(exists)) {
                    synchronized (available) {
                        if (available.get(url) == result) {
                            available.remove(url);
                        }
                    }
                }
            });
            return result;
        }
    }
}

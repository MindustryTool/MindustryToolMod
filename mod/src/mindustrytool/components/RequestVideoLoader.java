package mindustrytool.components;

import java.time.Duration;

import arc.files.Fi;
import arc.func.Cons;
import mindustrytool.services.Request;
import solim.display.video.VideoCache;
import solim.display.video.VideoLoader;

public final class RequestVideoLoader implements VideoLoader {

    private final Request api = Request.builder()
            .timeout(Duration.ofSeconds(120))
            .build();

    @Override
    public void load(String url, Cons<Fi> onSuccess, Cons<Throwable> onError) {
        if (url == null || url.trim().isEmpty()) {
            if (onError != null) {
                onError.get(new IllegalArgumentException("Empty URL"));
            }
            return;
        }
        Fi cached = VideoCache.videoFile(url);
        if (VideoCache.isFreshVideo(cached)) {
            if (onSuccess != null) {
                onSuccess.get(cached);
            }
            return;
        }
        api.get(url)
                .withoutAuth()
                .sendAsync(Request.BodyHandlers.ofByteArray())
                .whenComplete((response, error) -> {
                    if (error != null) {
                        if (onError != null) {
                            onError.get(error);
                        }
                        return;
                    }
                    byte[] bytes = response != null ? response.body() : null;
                    if (bytes == null || bytes.length == 0) {
                        if (onError != null) {
                            onError.get(new IllegalStateException("Empty response"));
                        }
                        return;
                    }
                    VideoCache.writeVideo(url, bytes);
                    Fi file = VideoCache.videoFile(url);
                    if (file != null) {
                        if (onSuccess != null) {
                            onSuccess.get(file);
                        }
                    } else if (onError != null) {
                        onError.get(new IllegalStateException("Cache unavailable"));
                    }
                });
    }
}

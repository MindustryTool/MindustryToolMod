package solim.display;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.graphics.g2d.TextureRegion;
import arc.util.Http;
import arc.util.Log;
import mindustry.core.Version;

final class DefaultImageLoader implements ImageLoader {

    @Override
    public void load(String url, int radius, float targetW, float targetH,
            Cons<TextureRegion> onSuccess, Cons<Throwable> onError) {
        if (url == null || url.trim().isEmpty()) {
            if (onError != null)
                onError.get(new IllegalArgumentException("Empty URL"));
            return;
        }
        try {
            Http.get(url)
                    .timeout(30000)
                    .header("User-Agent", "MindustryTool/" + Version.buildString())
                    .error(onError != null ? onError : err -> Log.err("SolimImage network load failed", err))
                    .submit(response -> {
                        byte[] bytes = response.getResult();
                        if (bytes == null || bytes.length == 0) {
                            if (onError != null)
                                onError.get(new IllegalStateException("Empty response"));
                            return;
                        }
                        ImageCache.writeToDisk(url, bytes);
                        Pixmap pixmap;
                        try {
                            pixmap = ImageCache.decodePixmap(bytes, radius, targetW, targetH);
                        } catch (Throwable t) {
                            if (onError != null)
                                onError.get(t);
                            return;
                        }
                        if (Core.app != null) {
                            Core.app.post(() -> {
                                try {
                                    onSuccess.get(ImageCache.toTexture(pixmap));
                                } catch (Throwable t) {
                                    if (onError != null)
                                        onError.get(t);
                                }
                            });
                        } else {
                            pixmap.dispose();
                        }
                    });
        } catch (Throwable t) {
            if (onError != null)
                onError.get(t);
        }
    }
}

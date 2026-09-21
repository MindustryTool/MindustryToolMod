package solim.display;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Pixmap;
import arc.graphics.g2d.TextureRegion;
import arc.util.Http;
import arc.util.Log;
import mindustry.core.Version;

/**
 * Default {@link ImageLoader}: fetches bytes over HTTP, writes them to the
 * disk cache, and decodes them off-thread. Texture upload is posted to the
 * main thread. Disk-cache reads and corruption fallback are handled by
 * {@link NetworkImage} so they apply to every loader.
 */
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
					.error(onError != null ? onError : err -> Log.err("NetworkImage error", err))
					.submit(response -> {
						byte[] bytes = response.getResult();
						if (bytes == null || bytes.length == 0) {
							if (onError != null)
								onError.get(new IllegalStateException("Empty response"));
							return;
						}
						NetworkImage.writeToDisk(url, bytes);
						Pixmap pixmap;
						try {
							pixmap = NetworkImage.decodePixmap(bytes, radius, targetW, targetH);
						} catch (Throwable t) {
							if (onError != null)
								onError.get(t);
							return;
						}
						if (Core.app != null) {
							Core.app.post(() -> {
								try {
									onSuccess.get(NetworkImage.toTexture(pixmap));
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

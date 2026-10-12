package solim.display.video;

import arc.files.Fi;
import arc.func.Cons;
import arc.util.Http;
import arc.util.Log;
import mindustry.core.Version;

/**
 * Default {@link VideoLoader}: serves fresh {@code <hash>.mp4} entries from
 * the shared disk cache, otherwise fetches bytes over HTTP and persists them
 * before reporting the cached file. Disk-cache corruption fallback is handled
 * by {@link SolimVideo} so it applies to every loader.
 */
final class DefaultVideoLoader implements VideoLoader {

	@Override
	public void load(String url, Cons<Fi> onSuccess, Cons<Throwable> onError) {
		if (url == null || url.trim().isEmpty()) {
			if (onError != null)
				onError.get(new IllegalArgumentException("Empty URL"));
			return;
		}
		Fi cached = VideoCache.videoFile(url);
		if (VideoCache.isFresh(cached)) {
			if (onSuccess != null)
				onSuccess.get(cached);
			return;
		}
		try {
			Http.get(url)
					.timeout(120000)
					.header("User-Agent", "MindustryTool/" + Version.buildString())
					.error(onError != null ? onError : err -> Log.err("SolimVideo download error", err))
					.submit(response -> {
						byte[] bytes = response.getResult();
						if (bytes == null || bytes.length == 0) {
							if (onError != null)
								onError.get(new IllegalStateException("Empty response"));
							return;
						}
						VideoCache.writeVideo(url, bytes);
						Fi file = VideoCache.videoFile(url);
						if (file != null) {
							if (onSuccess != null)
								onSuccess.get(file);
						} else if (onError != null) {
							onError.get(new IllegalStateException("Cache unavailable"));
						}
					});
		} catch (Throwable t) {
			if (onError != null)
				onError.get(t);
		}
	}
}

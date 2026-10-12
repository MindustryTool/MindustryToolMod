package solim.display.video;

import arc.files.Fi;
import arc.func.Cons;

/**
 * Fetch strategy for {@link SolimVideo}. Mirrors {@link solim.display.ImageLoader}:
 * loaders download the remote file into the shared Solim disk cache and hand
 * back the cached file. Callbacks may arrive on any thread; SolimVideo
 * marshals them onto the main thread.
 */
@FunctionalInterface
public interface VideoLoader {

	void load(String url, Cons<Fi> onSuccess, Cons<Throwable> onError);
}

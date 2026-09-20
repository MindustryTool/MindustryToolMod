package solim.display;

import arc.func.Cons;
import arc.graphics.g2d.TextureRegion;

/**
 * Fetch strategy for {@link NetworkImage}. A single method carrying the full
 * load contract, so an implementation cannot silently drop the radius or
 * target-size parameters.
 */
@FunctionalInterface
public interface ImageLoader {

	void load(String url, int radius, float targetW, float targetH,
			Cons<TextureRegion> onSuccess, Cons<Throwable> onError);
}

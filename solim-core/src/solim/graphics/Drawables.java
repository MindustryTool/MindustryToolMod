package solim.graphics;

import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.util.Nullable;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Utility methods for manipulating and adapting Drawables in Solim.
 */
public final class Drawables {

	private static final Map<TextureRegionDrawable, TextureRegionDrawable> scalableCache = new ConcurrentHashMap<>();

	private Drawables() {}

	/**
	 * Converts font-glyph-based icons (such as Mindustry's {@code Icon.*}) or other non-scaling
	 * {@link TextureRegionDrawable} subclasses into standard, scalable {@link TextureRegionDrawable}s
	 * that properly obey width and height in {@link arc.scene.ui.Image} and {@link SolimImage}.
	 *
	 * <p>Results are cached per drawable instance to prevent allocations.
	 */
	public static @Nullable Drawable scalable(@Nullable Drawable drawable) {
		if (drawable == null) {
			return null;
		}
		if (drawable instanceof TextureRegionDrawable) {
			TextureRegionDrawable trd = (TextureRegionDrawable) drawable;
			if (trd.getClass() != TextureRegionDrawable.class && trd.getRegion() != null) {
				TextureRegionDrawable cached = scalableCache.get(trd);
				if (cached == null) {
					cached = new TextureRegionDrawable(trd.getRegion());
					cached.setMinWidth(trd.getMinWidth());
					cached.setMinHeight(trd.getMinHeight());
					scalableCache.put(trd, cached);
				}
				return cached;
			}
		}
		return drawable;
	}
}

package solim.graphics;

import static org.junit.jupiter.api.Assertions.*;

import arc.graphics.g2d.TextureRegion;
import arc.scene.style.BaseDrawable;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import org.junit.jupiter.api.Test;

class DrawablesTest {

	static class CustomGlyphDrawable extends TextureRegionDrawable {
		CustomGlyphDrawable(TextureRegion region) {
			super(region);
			setMinWidth(32f);
			setMinHeight(32f);
		}
	}

	@Test
	void testNullHandledSafely() {
		assertNull(Drawables.scalable(null));
	}

	@Test
	void testStandardTextureRegionDrawableNotWrapped() {
		TextureRegion region = new TextureRegion();
		TextureRegionDrawable standard = new TextureRegionDrawable(region);

		Drawable result = Drawables.scalable(standard);
		assertSame(standard, result);
	}

	@Test
	void testSubclassWrappedInStandardTextureRegionDrawable() {
		TextureRegion region = new TextureRegion();
		CustomGlyphDrawable custom = new CustomGlyphDrawable(region);

		Drawable result = Drawables.scalable(custom);
		assertNotNull(result);
		assertNotSame(custom, result);
		assertEquals(TextureRegionDrawable.class, result.getClass());

		TextureRegionDrawable trd = (TextureRegionDrawable) result;
		assertSame(region, trd.getRegion());
		assertEquals(32f, trd.getMinWidth());
		assertEquals(32f, trd.getMinHeight());
	}

	@Test
	void testScalableResultsAreCached() {
		TextureRegion region = new TextureRegion();
		CustomGlyphDrawable custom = new CustomGlyphDrawable(region);

		Drawable first = Drawables.scalable(custom);
		Drawable second = Drawables.scalable(custom);

		assertSame(first, second);
	}

	@Test
	void testNonTextureRegionDrawableReturnedAsIs() {
		BaseDrawable base = new BaseDrawable();
		assertSame(base, Drawables.scalable(base));
	}
}

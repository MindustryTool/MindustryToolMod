package solim;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import arc.graphics.g2d.TextureRegion;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import solim.display.SolimImage;
import solim.reactive.Signal;
import solim.runtime.ParentStack;
import solim.test.SolimEnv;

class IconTest extends SolimEnv {

	static class CustomIconDrawable extends TextureRegionDrawable {
		CustomIconDrawable(TextureRegion region) {
			super(region);
		}
	}

	@AfterEach
	void clear() {
		ParentStack.clear();
	}

	@Test
	void imageDoesNotWrapCustomDrawableAutomatically() {
		TextureRegion region = new TextureRegion();
		CustomIconDrawable custom = new CustomIconDrawable(region);

		SolimImage img = UI.image(custom);
		Image arcImg = (Image) img.element();

		assertSame(custom, arcImg.getDrawable());
	}

	@Test
	void iconWrapsCustomDrawableWithScalable() {
		TextureRegion region = new TextureRegion();
		CustomIconDrawable custom = new CustomIconDrawable(region);

		SolimImage img = UI.icon(custom);
		Image arcImg = (Image) img.element();

		assertNotNull(arcImg.getDrawable());
		assertNotSame(custom, arcImg.getDrawable());
		assertEquals(TextureRegionDrawable.class, arcImg.getDrawable().getClass());
		assertSame(region, ((TextureRegionDrawable) arcImg.getDrawable()).getRegion());
	}

	@Test
	void iconReactiveWrapsCustomDrawable() {
		TextureRegion region = new TextureRegion();
		CustomIconDrawable custom = new CustomIconDrawable(region);
		Signal<Drawable> sig = Signal.of(custom);

		SolimImage img = UI.icon(sig);
		Image arcImg = (Image) img.element();

		assertNotNull(arcImg.getDrawable());
		assertNotSame(custom, arcImg.getDrawable());
		assertEquals(TextureRegionDrawable.class, arcImg.getDrawable().getClass());
	}
}

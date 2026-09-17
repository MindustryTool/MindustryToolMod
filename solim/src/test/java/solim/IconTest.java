package solim;

import static org.junit.jupiter.api.Assertions.*;

import arc.Core;
import arc.graphics.g2d.TextureRegion;
import arc.mock.MockApplication;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.Image;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import solim.display.SolimImage;
import solim.runtime.ParentStack;
import solim.reactive.Signal;
import arc.mock.MockGraphics;

class IconTest {

	static class CustomIconDrawable extends TextureRegionDrawable {
		CustomIconDrawable(TextureRegion region) {
			super(region);
		}
	}

	@BeforeAll
	static void init() {
		if (Core.app == null) {
			Core.app = new MockApplication();
		}
		if (Core.graphics == null) {
			Core.graphics = new MockGraphics();
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

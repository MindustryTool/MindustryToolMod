package solim.graphics;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import arc.Core;
import arc.mock.MockApplication;
import arc.mock.MockGraphics;
import arc.scene.ui.Image;

class CircleDrawableTest {

	@Test
	void instanceIsSingletonWithMinimumDimensions() {
		assertSame(CircleDrawable.INSTANCE, CircleDrawable.INSTANCE);
		assertEquals(1f, CircleDrawable.INSTANCE.getMinWidth());
		assertEquals(1f, CircleDrawable.INSTANCE.getMinHeight());
	}

	@Test
	void testDefaultDimensionsAreZero() {
		CircleDrawable cd = CircleDrawable.INSTANCE;
		assertEquals(1f, cd.getMinWidth());
		assertEquals(1f, cd.getMinHeight());
		assertEquals(0f, cd.getLeftWidth());
		assertEquals(0f, cd.getRightWidth());
		assertEquals(0f, cd.getTopHeight());
		assertEquals(0f, cd.getBottomHeight());
	}

	@Test
	void testImageLayout() {
		if (Core.app == null) Core.app = new MockApplication();
		if (Core.graphics == null) Core.graphics = new MockGraphics();
		Image img = new Image(CircleDrawable.INSTANCE);
		img.setSize(12f, 12f);
		img.layout();
		assertEquals(12f, img.getImageWidth());
		assertEquals(12f, img.getImageHeight());
	}
}

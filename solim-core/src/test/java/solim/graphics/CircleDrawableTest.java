package solim.graphics;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.junit.jupiter.api.Test;

import arc.scene.ui.Image;
import solim.test.SolimEnv;

class CircleDrawableTest extends SolimEnv {

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
		Image img = new Image(CircleDrawable.INSTANCE);
		img.setSize(12f, 12f);
		img.layout();
		assertEquals(12f, img.getImageWidth());
		assertEquals(12f, img.getImageHeight());
	}
}

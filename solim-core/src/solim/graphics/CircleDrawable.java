package solim.graphics;

import arc.graphics.g2d.Fill;
import arc.scene.style.BaseDrawable;

/**
 * Procedural solid circle drawable using Arc's {@link Fill#circle}.
 */
public class CircleDrawable extends BaseDrawable {
	public static final CircleDrawable INSTANCE = new CircleDrawable();

	public CircleDrawable() {
		// BaseDrawable defaults minWidth/minHeight to 0. With Scaling.fit, a 0×0 source
		// produces a 0×0 image region, causing draw() to be called with zero dimensions
		// and Fill.circle to draw nothing. Setting 1×1 ensures Scaling.fit always
		// preserves the cell size (1:1 aspect ratio → fills the allocated area).
		setMinWidth(1f);
		setMinHeight(1f);
	}

	@Override
	public void draw(float x, float y, float width, float height) {
		float radius = Math.min(width, height) / 2f;
		Fill.circle(x + width / 2f, y + height / 2f, radius);
	}

	@Override
	public void draw(float x, float y, float originX, float originY, float width, float height, float scaleX, float scaleY, float rotation) {
		float radius = (Math.min(width, height) / 2f) * Math.min(scaleX, scaleY);
		Fill.circle(x + width / 2f, y + height / 2f, radius);
	}
}

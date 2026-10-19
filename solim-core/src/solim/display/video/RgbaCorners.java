package solim.display.video;

import java.nio.ByteBuffer;
import solim.graphics.RoundedGenerator;

/**
 * Applies the rounded-corner alpha mask that video surfaces need. Only the four corner
 * squares are touched, so the cost stays independent of the frame size.
 */
public final class RgbaCorners {

	private RgbaCorners() {
	}

	public static void apply(ByteBuffer rgba, int width, int height, int cornerRadius) {
		int radius = Math.min(Math.max(0, cornerRadius), Math.min(width, height) / 2);
		if (rgba == null || radius <= 0 || width <= 0 || height <= 0)
			return;
		applyCorner(rgba, width, 0, 0, radius, true, true);
		applyCorner(rgba, width, width - radius, 0, radius, false, true);
		applyCorner(rgba, width, 0, height - radius, radius, true, false);
		applyCorner(rgba, width, width - radius, height - radius, radius, false, false);
	}

	private static void applyCorner(ByteBuffer rgba, int width, int startX, int startY, int radius,
			boolean left, boolean top) {
		for (int y = 0; y < radius; y++) {
			for (int x = 0; x < radius; x++) {
				float dx = left ? radius - 0.5f - x : x + 0.5f;
				float dy = top ? radius - 0.5f - y : y + 0.5f;
				rgba.put(alphaIndex(width, startX + x, startY + y), alphaByte(dx, dy, radius));
			}
		}
	}

	private static int alphaIndex(int width, int x, int y) {
		return (y * width + x) * 4 + 3;
	}

	private static byte alphaByte(float dx, float dy, int radius) {
		float alpha = RoundedGenerator.computeAlpha(dx, dy, radius);
		if (alpha < 0f)
			alpha = 0f;
		if (alpha > 1f)
			alpha = 1f;
		return (byte) Math.round(alpha * 255f);
	}
}

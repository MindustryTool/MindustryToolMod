package solim.display.video;

import java.nio.ByteBuffer;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import solim.graphics.RoundedGenerator;

/**
 * Converts JCodec YUV420 planar frames to RGBA8888 bytes using integer
 * BT.601 math. Writes into a caller-owned direct buffer with absolute puts,
 * so conversion itself allocates nothing; callers are expected to reuse the
 * buffer across frames.
 */
public final class YuvToRgbaConverter {

	private YuvToRgbaConverter() {
	}

	/** Direct RGBA buffer sized for a frame. Reuse one per decode stream. */
	public static ByteBuffer allocate(int width, int height) {
		return ByteBuffer.allocateDirect(Math.max(1, width) * Math.max(1, height) * 4);
	}

	/**
	 * Converts {@code picture} into {@code out} as RGBA bytes. Honors the
	 * picture crop rect, so coded padding never reaches the screen.
	 * {@code cornerRadius} fades the corners (frame pixels, 0 disables).
	 * Leaves {@code out} positioned for reading (position 0, limit w*h*4).
	 */
	public static void convert(Picture picture, ByteBuffer out, int cornerRadius) {
		if (picture == null || out == null)
			return;
		Rect crop = picture.getCrop();
		int x0 = crop != null ? Math.max(0, crop.getX()) : 0;
		int y0 = crop != null ? Math.max(0, crop.getY()) : 0;
		int width = crop != null ? crop.getWidth() : picture.getWidth();
		int height = crop != null ? crop.getHeight() : picture.getHeight();
		width = Math.min(width, picture.getWidth() - x0);
		height = Math.min(height, picture.getHeight() - y0);
		if (width <= 0 || height <= 0 || out.capacity() < width * height * 4)
			return;

		byte[][] data = picture.getData();
		if (data == null || data.length < 3 || data[0] == null || data[1] == null || data[2] == null)
			return;
		byte[] yPlane = data[0];
		byte[] uPlane = data[1];
		byte[] vPlane = data[2];
		int yStride = Math.max(1, picture.getPlaneWidth(0));
		int cStride = Math.max(1, picture.getPlaneWidth(1));
		boolean fullRange = picture.getColor() == ColorSpace.YUV420J;

		int radius = Math.min(Math.max(0, cornerRadius), Math.min(width, height) / 2);

		int pos = 0;
		for (int y = 0; y < height; y++) {
			int yRow = (y0 + y) * yStride + x0;
			int cRow = ((y0 + y) >> 1) * cStride + (x0 >> 1);
			for (int x = 0; x < width; x++) {
				int Y = yPlane[yRow + x] & 0xFF;
				int U = uPlane[cRow + (x >> 1)] & 0xFF;
				int V = vPlane[cRow + (x >> 1)] & 0xFF;

				int r;
				int g;
				int b;
				if (fullRange) {
					int d = U - 128;
					int e = V - 128;
					r = Y + ((359 * e) >> 8);
					g = Y - ((88 * d + 183 * e) >> 8);
					b = Y + ((454 * d) >> 8);
				} else {
					int c = Y - 16;
					int d = U - 128;
					int e = V - 128;
					r = (298 * c + 409 * e + 128) >> 8;
					g = (298 * c - 100 * d - 208 * e + 128) >> 8;
					b = (298 * c + 516 * d + 128) >> 8;
				}

				int alpha = 255;
				if (radius > 0 && x < radius && y < radius) {
					alpha = cornerByte(radius - 0.5f - x, radius - 0.5f - y, radius);
				} else if (radius > 0 && x >= width - radius && y < radius) {
					alpha = cornerByte(x - (width - radius) + 0.5f, radius - 0.5f - y, radius);
				} else if (radius > 0 && x < radius && y >= height - radius) {
					alpha = cornerByte(radius - 0.5f - x, y - (height - radius) + 0.5f, radius);
				} else if (radius > 0 && x >= width - radius && y >= height - radius) {
					alpha = cornerByte(x - (width - radius) + 0.5f, y - (height - radius) + 0.5f, radius);
				}

				out.put(pos++, (byte) clip(r));
				out.put(pos++, (byte) clip(g));
				out.put(pos++, (byte) clip(b));
				out.put(pos++, (byte) alpha);
			}
		}
		out.position(0);
		out.limit(width * height * 4);
	}

	private static int cornerByte(float dx, float dy, int radius) {
		float alpha = RoundedGenerator.computeAlpha(dx, dy, radius);
		if (alpha < 0f)
			alpha = 0f;
		if (alpha > 1f)
			alpha = 1f;
		return Math.round(alpha * 255f);
	}

	private static int clip(int value) {
		return value < 0 ? 0 : value > 255 ? 255 : value;
	}
}

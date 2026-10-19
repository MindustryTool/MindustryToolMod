package solim.display.video;

import java.nio.ByteBuffer;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;

public final class YuvToRgbaConverter {

	private YuvToRgbaConverter() {
	}

	public static ByteBuffer allocateRgba(int width, int height) {
		return ByteBuffer.allocateDirect(Math.max(1, width) * Math.max(1, height) * 4);
	}

	public static void convert(Picture picture, ByteBuffer out) {
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

		int pos = 0;
		for (int y = 0; y < height; y++) {
			int yRow = (y0 + y) * yStride + x0;
			int cRow = ((y0 + y) >> 1) * cStride + (x0 >> 1);
			for (int x = 0; x < width; x++) {
				int Y = yPlane[yRow + x] + 128;
				int U = uPlane[cRow + (x >> 1)] + 128;
				int V = vPlane[cRow + (x >> 1)] + 128;

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

				out.put(pos++, (byte) clip(r));
				out.put(pos++, (byte) clip(g));
				out.put(pos++, (byte) clip(b));
				out.put(pos++, (byte) 255);
			}
		}
		out.position(0);
		out.limit(width * height * 4);
	}

	private static int clip(int value) {
		return value < 0 ? 0 : value > 255 ? 255 : value;
	}
}

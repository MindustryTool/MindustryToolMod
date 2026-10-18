package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.junit.jupiter.api.Test;

class YuvToRgbaConverterTest {

	private static Picture solidFrame(int width, int height, ColorSpace color, int y, int u, int v) {
		Picture picture = Picture.create(width, height, color);
		byte[][] data = picture.getData();
		fill(data[0], y);
		fill(data[1], u);
		fill(data[2], v);
		return picture;
	}

	private static void fill(byte[] plane, int value) {
		for (int i = 0; i < plane.length; i++) {
			plane[i] = (byte) value;
		}
	}

	private static int[] pixel(ByteBuffer out, int x, int y, int width) {
		int base = (y * width + x) * 4;
		return new int[] { out.get(base) & 0xFF, out.get(base + 1) & 0xFF, out.get(base + 2) & 0xFF,
				out.get(base + 3) & 0xFF };
	}

	@Test
	void limitedRangeBlackAndWhite() {
		Picture black = solidFrame(4, 4, ColorSpace.YUV420, 16, 128, 128);
		ByteBuffer out = YuvToRgbaConverter.allocate(4, 4);
		YuvToRgbaConverter.convert(black, out, 0);
		assertArrayEquals(new int[] { 0, 0, 0, 255 }, pixel(out, 0, 0, 4));
		assertArrayEquals(new int[] { 0, 0, 0, 255 }, pixel(out, 3, 3, 4));

		Picture white = solidFrame(4, 4, ColorSpace.YUV420, 235, 128, 128);
		YuvToRgbaConverter.convert(white, out, 0);
		assertArrayEquals(new int[] { 255, 255, 255, 255 }, pixel(out, 1, 2, 4));
	}

	@Test
	void limitedRangePrimaryRed() {
		Picture red = solidFrame(4, 4, ColorSpace.YUV420, 82, 90, 240);
		ByteBuffer out = YuvToRgbaConverter.allocate(4, 4);
		YuvToRgbaConverter.convert(red, out, 0);
		assertArrayEquals(new int[] { 255, 1, 0, 255 }, pixel(out, 0, 0, 4));
	}

	@Test
	void fullRangeMapsIdentityOnNeutralChroma() {
		Picture black = solidFrame(4, 4, ColorSpace.YUV420J, 0, 128, 128);
		ByteBuffer out = YuvToRgbaConverter.allocate(4, 4);
		YuvToRgbaConverter.convert(black, out, 0);
		assertArrayEquals(new int[] { 0, 0, 0, 255 }, pixel(out, 0, 0, 4));

		Picture white = solidFrame(4, 4, ColorSpace.YUV420J, 255, 128, 128);
		YuvToRgbaConverter.convert(white, out, 0);
		assertArrayEquals(new int[] { 255, 255, 255, 255 }, pixel(out, 0, 0, 4));
	}

	@Test
	void cropRectLimitsOutput() {
		Picture cropped = Picture.createCropped(16, 16, ColorSpace.YUV420, new Rect(4, 4, 8, 8));
		ByteBuffer out = YuvToRgbaConverter.allocate(16, 16);
		YuvToRgbaConverter.convert(cropped, out, 0);
		assertEquals(0, out.position());
		assertEquals(8 * 8 * 4, out.limit());
	}

	@Test
	void roundedCornersFadeAlphaOnlyAtCorners() {
		Picture white = solidFrame(8, 8, ColorSpace.YUV420, 235, 128, 128);
		ByteBuffer out = YuvToRgbaConverter.allocate(8, 8);
		YuvToRgbaConverter.convert(white, out, 4);
		int[] corner = pixel(out, 0, 0, 8);
		int[] center = pixel(out, 4, 4, 8);
		int[] edge = pixel(out, 4, 0, 8);
		assertEquals(255, corner[0]);
		assertTrue(corner[3] < 255, "corner alpha should fade, was " + corner[3]);
		assertEquals(255, center[3]);
		assertEquals(255, edge[3]);
	}

	@Test
	void undersizedBufferIsLeftAlone() {
		Picture frame = solidFrame(4, 4, ColorSpace.YUV420, 235, 128, 128);
		ByteBuffer out = ByteBuffer.allocateDirect(8);
		YuvToRgbaConverter.convert(frame, out, 0);
		assertEquals(8, out.remaining());
	}

	@Test
	void nullInputsAreIgnored() {
		ByteBuffer out = YuvToRgbaConverter.allocate(4, 4);
		assertDoesNotThrow(() -> YuvToRgbaConverter.convert(null, out, 0));
		Picture frame = solidFrame(4, 4, ColorSpace.YUV420, 16, 128, 128);
		assertDoesNotThrow(() -> YuvToRgbaConverter.convert(frame, null, 0));
	}
}

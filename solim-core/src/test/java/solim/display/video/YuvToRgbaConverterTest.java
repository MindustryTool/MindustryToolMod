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
		storeShifted(data[0], y);
		storeShifted(data[1], u);
		storeShifted(data[2], v);
		return picture;
	}

	private static void storeShifted(byte[] plane, int value) {
		byte sample = (byte) (value - 128);
		for (int i = 0; i < plane.length; i++) {
			plane[i] = sample;
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
		ByteBuffer out = YuvToRgbaConverter.allocateRgba(4, 4);
		YuvToRgbaConverter.convert(black, out);
		assertArrayEquals(new int[] { 0, 0, 0, 255 }, pixel(out, 0, 0, 4));
		assertArrayEquals(new int[] { 0, 0, 0, 255 }, pixel(out, 3, 3, 4));

		Picture white = solidFrame(4, 4, ColorSpace.YUV420, 235, 128, 128);
		YuvToRgbaConverter.convert(white, out);
		assertArrayEquals(new int[] { 255, 255, 255, 255 }, pixel(out, 1, 2, 4));
	}

	@Test
	void limitedRangePrimaryRed() {
		Picture red = solidFrame(4, 4, ColorSpace.YUV420, 82, 90, 240);
		ByteBuffer out = YuvToRgbaConverter.allocateRgba(4, 4);
		YuvToRgbaConverter.convert(red, out);
		assertArrayEquals(new int[] { 255, 1, 0, 255 }, pixel(out, 0, 0, 4));
	}

	@Test
	void neutralPlanesConvertToGray() {
		Picture neutral = solidFrame(4, 4, ColorSpace.YUV420, 128, 128, 128);
		ByteBuffer out = YuvToRgbaConverter.allocateRgba(4, 4);
		YuvToRgbaConverter.convert(neutral, out);
		assertArrayEquals(new int[] { 130, 130, 130, 255 }, pixel(out, 0, 0, 4),
				"a frame with no chroma must stay gray instead of turning into a saturated color mask");
	}

	@Test
	void fullRangeMapsIdentityOnNeutralChroma() {
		Picture black = solidFrame(4, 4, ColorSpace.YUV420J, 0, 128, 128);
		ByteBuffer out = YuvToRgbaConverter.allocateRgba(4, 4);
		YuvToRgbaConverter.convert(black, out);
		assertArrayEquals(new int[] { 0, 0, 0, 255 }, pixel(out, 0, 0, 4));

		Picture white = solidFrame(4, 4, ColorSpace.YUV420J, 255, 128, 128);
		YuvToRgbaConverter.convert(white, out);
		assertArrayEquals(new int[] { 255, 255, 255, 255 }, pixel(out, 0, 0, 4));
	}

	@Test
	void cropRectLimitsOutput() {
		Picture cropped = Picture.createCropped(16, 16, ColorSpace.YUV420, new Rect(4, 4, 8, 8));
		ByteBuffer out = YuvToRgbaConverter.allocateRgba(16, 16);
		YuvToRgbaConverter.convert(cropped, out);
		assertEquals(0, out.position());
		assertEquals(8 * 8 * 4, out.limit());
	}

	@Test
	void undersizedBufferIsLeftAlone() {
		Picture frame = solidFrame(4, 4, ColorSpace.YUV420, 235, 128, 128);
		ByteBuffer out = ByteBuffer.allocateDirect(8);
		YuvToRgbaConverter.convert(frame, out);
		assertEquals(8, out.remaining());
	}

	@Test
	void nullInputsAreIgnored() {
		ByteBuffer out = YuvToRgbaConverter.allocateRgba(4, 4);
		assertDoesNotThrow(() -> YuvToRgbaConverter.convert(null, out));
		Picture frame = solidFrame(4, 4, ColorSpace.YUV420, 16, 128, 128);
		assertDoesNotThrow(() -> YuvToRgbaConverter.convert(frame, null));
	}
}

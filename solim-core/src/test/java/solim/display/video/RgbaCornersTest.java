package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class RgbaCornersTest {

	private static ByteBuffer opaqueFrame(int width, int height) {
		ByteBuffer buffer = ByteBuffer.allocateDirect(width * height * 4);
		for (int i = 0; i < width * height; i++) {
			buffer.put(i * 4, (byte) 200);
			buffer.put(i * 4 + 1, (byte) 200);
			buffer.put(i * 4 + 2, (byte) 200);
			buffer.put(i * 4 + 3, (byte) 255);
		}
		return buffer;
	}

	private static int alpha(ByteBuffer buffer, int x, int y, int width) {
		return buffer.get((y * width + x) * 4 + 3) & 0xFF;
	}

	@Test
	void cornersFadeWhileEdgesAndCenterStayOpaque() {
		int size = 16;
		int radius = 6;
		ByteBuffer buffer = opaqueFrame(size, size);
		RgbaCorners.apply(buffer, size, size, radius);

		assertTrue(alpha(buffer, 0, 0, size) < 255);
		assertTrue(alpha(buffer, size - 1, 0, size) < 255);
		assertTrue(alpha(buffer, 0, size - 1, size) < 255);
		assertTrue(alpha(buffer, size - 1, size - 1, size) < 255);
		assertEquals(255, alpha(buffer, size / 2, size / 2, size));
		assertEquals(255, alpha(buffer, size / 2, 0, size));
		assertEquals(255, alpha(buffer, 0, size / 2, size));
	}

	@Test
	void zeroRadiusLeavesFrameUntouched() {
		int size = 8;
		ByteBuffer buffer = opaqueFrame(size, size);
		for (int i = 0; i < size * size; i++) {
			buffer.put(i * 4 + 3, (byte) 77);
		}
		RgbaCorners.apply(buffer, size, size, 0);
		for (int i = 0; i < size * size; i++) {
			assertEquals(77, buffer.get(i * 4 + 3) & 0xFF);
		}
	}

	@Test
	void radiusIsClampedToHalfTheFrame() {
		int size = 10;
		ByteBuffer buffer = opaqueFrame(size, size);
		assertDoesNotThrow(() -> RgbaCorners.apply(buffer, size, size, 500));
		assertTrue(alpha(buffer, 0, 0, size) < 100);
		assertEquals(255, alpha(buffer, size / 2, size / 2, size));
	}
}

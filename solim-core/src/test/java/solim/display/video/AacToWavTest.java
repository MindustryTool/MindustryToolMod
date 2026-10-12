package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;

import java.io.ByteArrayOutputStream;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;

class AacToWavTest {

	private static int readIntLE(byte[] buf, int offset) {
		return (buf[offset] & 0xFF) | ((buf[offset + 1] & 0xFF) << 8)
				| ((buf[offset + 2] & 0xFF) << 16) | ((buf[offset + 3] & 0xFF) << 24);
	}

	private static int readShortLE(byte[] buf, int offset) {
		return (buf[offset] & 0xFF) | ((buf[offset + 1] & 0xFF) << 8);
	}

	@Test
	void wavHeaderHasValidRiffStructure() throws Exception {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		AacToWav.writeWavHeader(out, 44100, 2, 1000);
		byte[] header = out.toByteArray();

		assertEquals(44, header.length);
		assertEquals("RIFF", new String(header, 0, 4, "US-ASCII"));
		assertEquals("WAVE", new String(header, 8, 4, "US-ASCII"));
		assertEquals("fmt ", new String(header, 12, 4, "US-ASCII"));
		assertEquals("data", new String(header, 36, 4, "US-ASCII"));

		assertEquals(16, readIntLE(header, 16));
		assertEquals(1, readShortLE(header, 20));
		assertEquals(2, readShortLE(header, 22));
		assertEquals(44100, readIntLE(header, 24));
		assertEquals(44100 * 2 * 2, readIntLE(header, 28));
		assertEquals(4, readShortLE(header, 32));
		assertEquals(16, readShortLE(header, 34));
		assertEquals(1000, readIntLE(header, 40));
		assertEquals(36 + 1000, readIntLE(header, 4));
	}

	@Test
	void adtsWrapProducesSyncWordAndLength() {
		byte[] asc = new byte[] { 0x12, 0x10 };
		byte[] raw = new byte[100];
		for (int i = 0; i < raw.length; i++) {
			raw[i] = (byte) i;
		}
		ByteBuffer frame = AacToWav.wrapAdts(asc, ByteBuffer.wrap(raw));

		assertEquals(107, frame.remaining());
		assertEquals((byte) 0xFF, frame.get(0));
		// jcodec writes the MPEG-2 ID bit; decoders ignore it and key off the profile bits.
		assertEquals((byte) 0xF9, frame.get(1));
		int length = ((frame.get(3) & 0x03) << 11) | ((frame.get(4) & 0xFF) << 3) | ((frame.get(5) & 0xFF) >> 5);
		assertEquals(107, length);
		for (int i = 0; i < raw.length; i++) {
			assertEquals(raw[i], frame.get(7 + i));
		}
	}
}

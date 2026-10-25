package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import arc.files.Fi;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import org.junit.jupiter.api.Test;

class AacToWavTest {

	private static File previewClip() {
		File dir = new File(System.getProperty("user.dir"));
		while (dir != null) {
			File candidate = new File(dir, "preview/quick-rotate.mp4");
			if (candidate.isFile())
				return candidate;
			dir = dir.getParentFile();
		}
		return null;
	}

	private static int readIntLE(byte[] buf, int offset) {
		return (buf[offset] & 0xFF) | ((buf[offset + 1] & 0xFF) << 8)
				| ((buf[offset + 2] & 0xFF) << 16) | ((buf[offset + 3] & 0xFF) << 24);
	}

	private static int readShortLE(byte[] buf, int offset) {
		return (buf[offset] & 0xFF) | ((buf[offset + 1] & 0xFF) << 8);
	}

	@Test
	void transcodeCommitsDecodedPcmToTheFinalFile() throws Exception {
		File clip = previewClip();
		assumeTrue(clip != null, "preview/quick-rotate.mp4 is not available");
		File dir = Files.createTempDirectory("solim-transcode").toFile();
		File wav = new File(dir, "audio.wav");

		AacToWav.AudioInfo info = AacToWav.decode(clip, wav);

		assertNotNull(info);
		assertTrue(info.sampleRate > 0);
		assertFalse(new File(dir, "audio.wav.part").exists());

		byte[] bytes = new Fi(wav).readBytes();
		assertTrue(bytes.length > VideoCache.WAV_HEADER_BYTES, "decoded PCM must survive the commit");
		long dataLen = readIntLE(bytes, 40) & 0xFFFFFFFFL;
		assertEquals(bytes.length - VideoCache.WAV_HEADER_BYTES, dataLen);
		assertEquals(36 + dataLen, readIntLE(bytes, 4) & 0xFFFFFFFFL);
		assertTrue(dataLen > (long) info.sampleRate * info.channels, "audio must cover more than half a second");
	}

	@Test
	void abandonedTranscodeLeavesNoPartFile() throws Exception {
		File clip = previewClip();
		assumeTrue(clip != null, "preview/quick-rotate.mp4 is not available");
		File dir = Files.createTempDirectory("solim-transcode-abandon").toFile();
		File blocked = new File(dir, "audio.wav");
		assumeTrue(blocked.mkdir() && new File(blocked, "occupied").createNewFile());

		assertNull(AacToWav.decode(clip, blocked));

		assertFalse(new File(dir, "audio.wav.part").exists());
		assertTrue(blocked.isDirectory());
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
		assertEquals((byte) 0xF9, frame.get(1));
		int length = ((frame.get(3) & 0x03) << 11) | ((frame.get(4) & 0xFF) << 3) | ((frame.get(5) & 0xFF) >> 5);
		assertEquals(107, length);
		for (int i = 0; i < raw.length; i++) {
			assertEquals(raw[i], frame.get(7 + i));
		}
	}

	@Test
	void aSampleThatAlreadyCarriesAHeaderIsNotWrappedAgain() {
		byte[] asc = new byte[] { 0x12, 0x10 };
		byte[] wrapped = new byte[107];
		wrapped[0] = (byte) 0xFF;
		wrapped[1] = (byte) 0xF9;
		wrapped[2] = 0x50;
		// The length field of the header has to name the whole frame.
		wrapped[3] = 0x40;
		wrapped[4] = (byte) (wrapped.length >> 3);
		wrapped[5] = (byte) ((wrapped.length & 7) << 5);
		for (int i = 7; i < wrapped.length; i++) {
			wrapped[i] = (byte) i;
		}

		ByteBuffer frame = AacToWav.wrapAdts(asc, ByteBuffer.wrap(wrapped));

		assertEquals(wrapped.length, frame.remaining());
		for (int i = 0; i < wrapped.length; i++) {
			assertEquals(wrapped[i], frame.get(i));
		}
	}

	@Test
	void aSampleThatOnlyLooksLikeAHeaderIsWrapped() {
		byte[] asc = new byte[] { 0x12, 0x10 };
		// Raw audio that starts with the sync word by chance. The length field of the
		// header does not match the sample, so the sample is not a header.
		byte[] raw = new byte[100];
		raw[0] = (byte) 0xFF;
		raw[1] = (byte) 0xF1;
		raw[3] = (byte) 0x7F;
		for (int i = 4; i < raw.length; i++) {
			raw[i] = (byte) i;
		}

		ByteBuffer frame = AacToWav.wrapAdts(asc, ByteBuffer.wrap(raw));

		assertEquals(107, frame.remaining());
		assertEquals((byte) 0xFF, frame.get(0));
		for (int i = 0; i < raw.length; i++) {
			assertEquals(raw[i], frame.get(7 + i));
		}
	}
}

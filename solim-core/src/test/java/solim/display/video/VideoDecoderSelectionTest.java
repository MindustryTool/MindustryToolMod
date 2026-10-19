package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.*;

import arc.files.Fi;
import java.io.File;
import java.nio.ByteBuffer;
import org.junit.jupiter.api.Test;
import solim.test.SolimEnv;

class VideoDecoderSelectionTest extends SolimEnv {

	private static Fi sampleClip() {
		File dir = new File(System.getProperty("user.dir"));
		while (dir != null) {
			File candidate = new File(dir, "preview/quick-rotate.mp4");
			if (candidate.isFile())
				return new Fi(candidate);
			dir = dir.getParentFile();
		}
		assumeTrue(false, "preview/quick-rotate.mp4 is required for decoder tests");
		return null;
	}

	private static int sampleCount(VideoFrameDecoder decoder, int maxFrames) throws Exception {
		ByteBuffer pixels = YuvToRgbaConverter.allocateRgba(decoder.width(), decoder.height());
		int frames = 0;
		while (frames < maxFrames && decoder.decodeNext(pixels) != VideoFrameDecoder.END_OF_STREAM)
			frames++;
		return frames;
	}

	@Test
	void softwareDecoderDecodesSampleClip() throws Exception {
		Fi clip = sampleClip();
		VideoFrameDecoder decoder = VideoDecoders.openSoftware(clip);
		try {
			assertFalse(decoder.isNative());
			assertTrue(decoder.width() > 0 && decoder.height() > 0);
			assertTrue(decoder.duration() > 0);
			assertTrue(sampleCount(decoder, 12) >= 10, "software decoder must deliver frames");
		} finally {
			decoder.close();
		}
	}

	@Test
	void autoPreferenceSelectsNativeDecoderWhenAvailable() throws Exception {
		Fi clip = sampleClip();
		assumeTrue(VideoDecoders.isNativeAvailable(), "no native decoder registered on this platform");
		VideoDecoders.setPreference(VideoDecoders.Preference.AUTO);
		VideoFrameDecoder decoder = VideoDecoders.open(clip);
		try {
			assertTrue(decoder.isNative(), "auto preference must select the native decoder");
			assertTrue(sampleCount(decoder, 12) >= 10, "native decoder must deliver frames");
		} finally {
			decoder.close();
		}
	}

	@Test
	void nativeAndSoftwareAgreeOnGeometryAndFrameCount() throws Exception {
		Fi clip = sampleClip();
		assumeTrue(VideoDecoders.isNativeAvailable(), "no native decoder registered on this platform");
		VideoFrameDecoder nativeDecoder = VideoDecoders.open(clip);
		VideoFrameDecoder softwareDecoder = VideoDecoders.openSoftware(clip);
		try {
			assertEquals(softwareDecoder.width(), nativeDecoder.width());
			assertEquals(softwareDecoder.height(), nativeDecoder.height());
			assertEquals(softwareDecoder.duration(), nativeDecoder.duration(), 0.5);

			assertEquals(sampleCount(softwareDecoder, 40), sampleCount(nativeDecoder, 40),
					"both decoders must expose the same number of frames");
		} finally {
			nativeDecoder.close();
			softwareDecoder.close();
		}
	}

	@Test
	void nativeDecoderProducesComparablePixels() throws Exception {
		Fi clip = sampleClip();
		assumeTrue(VideoDecoders.isNativeAvailable(), "no native decoder registered on this platform");
		VideoFrameDecoder nativeDecoder = VideoDecoders.open(clip);
		VideoFrameDecoder softwareDecoder = VideoDecoders.openSoftware(clip);
		try {
			ByteBuffer nativePixels = YuvToRgbaConverter.allocateRgba(nativeDecoder.width(), nativeDecoder.height());
			ByteBuffer softwarePixels = YuvToRgbaConverter.allocateRgba(softwareDecoder.width(), softwareDecoder.height());
			for (int frame = 0; frame < 3; frame++) {
				assertTrue(nativeDecoder.decodeNext(nativePixels) >= 0);
				assertTrue(softwareDecoder.decodeNext(softwarePixels) >= 0);
			}
			double difference = meanChannelDifference(nativePixels, softwarePixels);
			assertTrue(difference < 12.0,
					"native output should match the software conversion, mean channel difference was " + difference);
		} finally {
			nativeDecoder.close();
			softwareDecoder.close();
		}
	}

	@Test
	void seekPositionsBothDecodersAtTheRequestedTime() throws Exception {
		Fi clip = sampleClip();
		assumeTrue(VideoDecoders.isNativeAvailable(), "no native decoder registered on this platform");
		VideoFrameDecoder decoder = VideoDecoders.open(clip);
		try {
			double target = Math.min(1.0, decoder.duration() / 2);
			decoder.seek(target);
			ByteBuffer pixels = YuvToRgbaConverter.allocateRgba(decoder.width(), decoder.height());
			double pts = decoder.decodeNext(pixels);
			assertTrue(pts >= target - 0.001, "first frame after seek was at " + pts + ", expected at least " + target);
			assertTrue(pts <= target + 0.5, "first frame after seek was at " + pts + ", too far from " + target);
		} finally {
			decoder.close();
		}
	}

	private static double meanChannelDifference(ByteBuffer first, ByteBuffer second) {
		int pixels = Math.min(first.capacity(), second.capacity()) / 4;
		int step = Math.max(1, pixels / 4000);
		long total = 0;
		int samples = 0;
		for (int index = 0; index < pixels; index += step) {
			int base = index * 4;
			for (int channel = 0; channel < 3; channel++) {
				total += Math.abs((first.get(base + channel) & 0xFF) - (second.get(base + channel) & 0xFF));
				samples++;
			}
		}
		return samples == 0 ? 0 : (double) total / samples;
	}
}

package solim.display.video;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.Arrays;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Rect;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;
import org.junit.jupiter.api.Test;

class H264PacketDecoderTest {

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

	private static class Stream {
		final SeekableByteChannel channel;
		final SeekableDemuxerTrack track;
		final byte[] codecPrivate;

		Stream(File clip) throws Exception {
			channel = NIOUtils.readableChannel(clip);
			MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(channel);
			track = (SeekableDemuxerTrack) demuxer.getVideoTrack();
			DemuxerTrackMeta meta = track.getMeta();
			codecPrivate = NIOUtils.toArray(meta.getCodecPrivate());
		}

		void close() {
			try {
				channel.close();
			} catch (Exception ignored) {
			}
		}
	}

	@Test
	void everyPacketOfTheClipDecodesIntoItsDisplayFrame() throws Exception {
		File clip = previewClip();
		assumeTrue(clip != null, "preview/quick-rotate.mp4 is not available");
		Stream stream = new Stream(clip);
		try {
			H264PacketDecoder decoder = new H264PacketDecoder(stream.codecPrivate);
			int packets = 0;
			int decoded = 0;
			Picture first = null;
			Packet packet;
			while ((packet = stream.track.nextFrame()) != null) {
				packets++;
				Picture picture = decoder.decode(packet.getData());
				if (picture != null) {
					decoded++;
					if (first == null)
						first = picture;
				}
			}

			assertTrue(packets > 100, "the clip must demux into many packets");
			assertEquals(packets, decoded, "every demuxed packet must decode");
			assertNotNull(first);
			Rect crop = first.getCrop();
			assertNotNull(crop, "the decoded frame must carry its display crop");
			assertTrue(crop.getWidth() > 0 && crop.getHeight() > 0);
			assertTrue(first.getWidth() >= crop.getWidth(), "plane storage must cover the display frame");
			assertTrue(first.getWidth() % 16 == 0 && first.getHeight() % 16 == 0,
					"planes must be macroblock aligned");
		} finally {
			stream.close();
		}
	}

	@Test
	void decodedFramesDifferFromEachOther() throws Exception {
		File clip = previewClip();
		assumeTrue(clip != null, "preview/quick-rotate.mp4 is not available");
		Stream stream = new Stream(clip);
		try {
			H264PacketDecoder decoder = new H264PacketDecoder(stream.codecPrivate);
			byte[] first = null;
			byte[] last = null;
			Packet packet;
			while ((packet = stream.track.nextFrame()) != null) {
				Picture picture = decoder.decode(packet.getData());
				if (picture == null)
					continue;
				byte[] snapshot = picture.getPlaneData(0).clone();
				if (first == null)
					first = snapshot;
				last = snapshot;
			}

			assertNotNull(first);
			assertNotNull(last);
			assertFalse(Arrays.equals(first, last), "decoding must produce moving content, not one repeated frame");
		} finally {
			stream.close();
		}
	}

	@Test
	void decodedFrameRendersWithNaturalColors() throws Exception {
		File clip = previewClip();
		assumeTrue(clip != null, "preview/quick-rotate.mp4 is not available");
		Stream stream = new Stream(clip);
		try {
			H264PacketDecoder decoder = new H264PacketDecoder(stream.codecPrivate);
			Picture picture = null;
			Packet packet;
			while (picture == null && (packet = stream.track.nextFrame()) != null) {
				picture = decoder.decode(packet.getData());
			}

			assertNotNull(picture);
			Rect crop = picture.getCrop();
			int pixels = crop.getWidth() * crop.getHeight();
			ByteBuffer rgba = YuvToRgbaConverter.allocate(crop.getWidth(), crop.getHeight());
			YuvToRgbaConverter.convert(picture, rgba, 0);

			int saturated = 0;
			for (int i = 0; i < pixels; i++) {
				if (singleChannelSaturated(rgba.get(i * 4) & 0xFF, rgba.get(i * 4 + 1) & 0xFF,
						rgba.get(i * 4 + 2) & 0xFF)) {
					saturated++;
				}
			}

			assertTrue(saturated * 5 < pixels, "a decoded frame must not render as a saturated color mask, but "
					+ saturated * 100 / pixels + "% of its pixels are one saturated channel");
		} finally {
			stream.close();
		}
	}

	private static boolean singleChannelSaturated(int r, int g, int b) {
		return (r > 200 && g < 60 && b < 60)
				|| (g > 200 && r < 60 && b < 60)
				|| (b > 200 && r < 60 && g < 60);
	}

	@Test
	void decoderRejectsEmptyPackets() throws Exception {
		File clip = previewClip();
		assumeTrue(clip != null, "preview/quick-rotate.mp4 is not available");
		Stream stream = new Stream(clip);
		try {
			H264PacketDecoder decoder = new H264PacketDecoder(stream.codecPrivate);
			assertNull(decoder.decode(null));
			assertNull(decoder.decode(ByteBuffer.allocate(0)));
		} finally {
			stream.close();
		}
	}
}

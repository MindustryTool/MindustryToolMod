package solim.display.video;

import arc.util.Log;
import arc.util.Nullable;
import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.List;
import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.codecs.aac.AACUtils;
import org.jcodec.codecs.aac.ADTSParser;
import org.jcodec.common.AudioFormat;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

public final class AacToWav {

	private AacToWav() {
	}

	public static final class AudioInfo {
		public final int sampleRate;
		public final int channels;
		public final double durationSec;

		public AudioInfo(int sampleRate, int channels, double durationSec) {
			this.sampleRate = sampleRate;
			this.channels = channels;
			this.durationSec = durationSec;
		}
	}

	public static @Nullable AudioInfo decode(File mp4File, File wavOut) {
		if (mp4File == null || wavOut == null)
			return null;
		try (RandomAccessFile raf = new RandomAccessFile(mp4File, "r");
				SeekableByteChannel channel = new FileChannelWrapper(raf.getChannel());
				// The raw demuxer hands the samples over exactly as the file stores them. The
				// regular one wraps them in an ADTS header, and then the header would be
				// added a second time here.
				MP4Demuxer demuxer = MP4Demuxer.createRawMP4Demuxer(channel)) {
			DemuxerTrack audio = findAacTrack(demuxer);
			if (audio == null)
				return null;
			return transcode(audio, wavOut);
		} catch (Throwable t) {
			Log.debug("SolimVideo: AAC transcode failed: " + t.getMessage());
			return null;
		}
	}

	private static @Nullable DemuxerTrack findAacTrack(MP4Demuxer demuxer) {
		List<DemuxerTrack> tracks;
		try {
			tracks = demuxer.getAudioTracks();
		} catch (Throwable t) {
			return null;
		}
		if (tracks == null)
			return null;
		for (DemuxerTrack track : tracks) {
			if (track == null)
				continue;
			DemuxerTrackMeta meta;
			try {
				meta = track.getMeta();
			} catch (Throwable t) {
				continue;
			}
			if (meta != null && meta.getCodec() != Codec.AAC)
				continue;
			ByteBuffer codecPrivate = codecPrivate(track);
			if (codecPrivate == null)
				continue;
			try {
				new AACDecoder(codecPrivate.duplicate());
				return track;
			} catch (Throwable ignored) {
			}
		}
		return null;
	}

	private static @Nullable ByteBuffer codecPrivate(DemuxerTrack track) {
		try {
			SampleEntry[] entries = track instanceof AbstractMP4DemuxerTrack
					? ((AbstractMP4DemuxerTrack) track).getSampleEntries()
					: null;
			if (entries == null)
				return null;
			for (SampleEntry entry : entries) {
				if (entry instanceof AudioSampleEntry) {
					ByteBuffer priv = AACUtils.getCodecPrivate(entry);
					if (priv != null && priv.remaining() > 0)
						return priv;
				}
			}
			return null;
		} catch (Throwable t) {
			return null;
		}
	}

	private static @Nullable AudioInfo transcode(DemuxerTrack audio, File wavOut) throws Exception {
		ByteBuffer codecPrivate = codecPrivate(audio);
		if (codecPrivate == null)
			return null;
		byte[] asc = NIOUtils.toArray(codecPrivate);
		AACDecoder decoder = new AACDecoder(ByteBuffer.wrap(asc));

		File parent = wavOut.getParentFile();
		if (parent != null)
			parent.mkdirs();
		File tmp = new File(parent, wavOut.getName() + ".part");
		boolean committed = false;
		int sampleRate = 0;
		int channels = 0;
		long dataLen = 0;
		int peak = 0;
		long trim = 0;
		// The number of bytes at the front of the track that belong to the delay.
		try {
			try (OutputStream out = new BufferedOutputStream(new FileOutputStream(tmp))) {
				writeWavHeader(out, 44100, 2, 0);
				byte[] copy = new byte[8192];
				Packet packet;
				while ((packet = audio.nextFrame()) != null) {
					ByteBuffer raw = packet.getData();
					if (raw == null || raw.remaining() == 0)
						continue;
					AudioBuffer decoded = decoder.decodeFrame(wrapAdts(asc, raw), null);
					if (decoded == null || decoded.getFormat() == null)
						continue;
					if (sampleRate <= 0) {
						AudioFormat format = decoded.getFormat();
						if (format.getSampleSizeInBits() != 16)
							return null;
						sampleRate = format.getSampleRate();
						channels = format.getChannels();
						if (sampleRate <= 0 || channels <= 0)
							return null;
						// The audio track starts at the media time of its first sample, and
						// those first samples are the delay the encoder left behind. A player
						// that compensates it starts the sound at the picture, and one that
						// does not plays the whole sound behind the picture.
						double offset = packet.getPts() / (double) packet.getTimescale();
						trim = offset > 0 ? Math.round(offset * sampleRate) * 2 * channels : 0;
					}
					ByteBuffer pcm = decoded.getData();
					while (pcm.hasRemaining()) {
						int n = Math.min(copy.length, pcm.remaining());
						pcm.get(copy, 0, n);
						for (int index = 0; index + 1 < n; index += 2) {
							short value = (short) ((copy[index] & 0xff) | (copy[index + 1] << 8));
							peak = Math.max(peak, Math.abs(value));
						}
						if (trim > 0) {
							int drop = (int) Math.min(trim, n);
							trim -= drop;
							if (drop == n)
								continue;
							out.write(copy, drop, n - drop);
							dataLen += n - drop;
						} else {
							out.write(copy, 0, n);
							dataLen += n;
						}
					}
				}
			}
			if (dataLen == 0 || sampleRate <= 0 || channels <= 0)
				return null;
			if (peak == 0) {
				// The track decoded, but every sample is zero. Writing it out would give a
				// sound that plays in silence, which is worse than having no sound at all.
				return null;
			}
			rewriteWavHeader(tmp, sampleRate, channels, dataLen);
			if (wavOut.exists() && !wavOut.delete())
				return null;
			if (!tmp.renameTo(wavOut))
				return null;
			committed = true;
			DemuxerTrackMeta meta = audio.getMeta();
			return new AudioInfo(sampleRate, channels, meta != null ? meta.getTotalDuration() : 0);
		} finally {
			if (!committed)
				tmp.delete();
		}
	}

	/**
	 * Presents an audio sample the way the AAC decoder expects it.
	 *
	 * <p>The decoder reads an ADTS header and then the raw data. Samples that already carry
	 * such a header are handed over untouched: wrapping them a second time leaves the decoder
	 * with a header where it expects audio data, and it answers with silence.
	 */
	static ByteBuffer wrapAdts(byte[] asc, ByteBuffer raw) {
		if (hasAdtsHeader(raw)) {
			return raw.duplicate();
		}
		int frameSize = raw.remaining();
		ADTSParser.Header header = AACUtils.streamInfoToADTS(ByteBuffer.wrap(asc), true, 1, frameSize);
		ByteBuffer frame = ByteBuffer.allocate(7 + frameSize);
		frame.put(ADTSParser.write(header, frame));
		frame.put(raw.duplicate());
		frame.flip();
		return frame;
	}

	/**
	 * Whether the sample already is an ADTS frame.
	 *
	 * <p>The sync word alone is not enough: raw audio data starts with such a byte sequence
	 * by chance now and then. Taking the sample at face value then leaves the decoder with
	 * a frame that is cut in the wrong place, and because the decoder carries the leftover
	 * bits of a frame over into the next one, every frame after it comes out silent.
	 * The length field of the header settles it.
	 */
	private static boolean hasAdtsHeader(ByteBuffer data) {
		int size = data.remaining();
		if (size < 7)
			return false;
		int position = data.position();
		int first = data.get(position) & 0xFF;
		int second = data.get(position + 1) & 0xFF;
		if (first != 0xFF || (second & 0xF0) != 0xF0)
			return false;
		int length = ((data.get(position + 3) & 0x03) << 11) | ((data.get(position + 4) & 0xFF) << 3)
				| ((data.get(position + 5) & 0xFF) >> 5);
		return length == size;
	}

	static void writeWavHeader(OutputStream out, int sampleRate, int channels, long dataLen) throws Exception {
		writeAscii(out, "RIFF");
		writeIntLE(out, (int) Math.min(36 + dataLen, Integer.MAX_VALUE - 8));
		writeAscii(out, "WAVE");
		writeAscii(out, "fmt ");
		writeIntLE(out, 16);
		writeShortLE(out, 1);
		writeShortLE(out, channels);
		writeIntLE(out, sampleRate);
		writeIntLE(out, sampleRate * channels * 2);
		writeShortLE(out, channels * 2);
		writeShortLE(out, 16);
		writeAscii(out, "data");
		writeIntLE(out, (int) Math.min(dataLen, Integer.MAX_VALUE));
	}

	private static void rewriteWavHeader(File wav, int sampleRate, int channels, long dataLen) throws Exception {
		ByteArrayOutputStream head = new ByteArrayOutputStream(44);
		writeWavHeader(head, sampleRate, channels, dataLen);
		byte[] bytes = head.toByteArray();
		try (RandomAccessFile raf = new RandomAccessFile(wav, "rw")) {
			raf.seek(0);
			raf.write(bytes);
		}
	}

	private static void writeAscii(OutputStream out, String text) throws Exception {
		for (int i = 0; i < text.length(); i++) {
			out.write(text.charAt(i));
		}
	}

	private static void writeShortLE(OutputStream out, int value) throws Exception {
		out.write(value & 0xFF);
		out.write((value >> 8) & 0xFF);
	}

	private static void writeIntLE(OutputStream out, int value) throws Exception {
		out.write(value & 0xFF);
		out.write((value >> 8) & 0xFF);
		out.write((value >> 16) & 0xFF);
		out.write((value >> 24) & 0xFF);
	}
}

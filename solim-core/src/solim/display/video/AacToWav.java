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
				MP4Demuxer demuxer = MP4Demuxer.createMP4Demuxer(channel)) {
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
					}
					ByteBuffer pcm = decoded.getData();
					while (pcm.hasRemaining()) {
						int n = Math.min(copy.length, pcm.remaining());
						pcm.get(copy, 0, n);
						out.write(copy, 0, n);
						dataLen += n;
					}
				}
			}
			if (dataLen == 0 || sampleRate <= 0 || channels <= 0)
				return null;
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

	static ByteBuffer wrapAdts(byte[] asc, ByteBuffer raw) {
		int frameSize = raw.remaining();
		ADTSParser.Header header = AACUtils.streamInfoToADTS(ByteBuffer.wrap(asc), true, 1, frameSize);
		ByteBuffer frame = ByteBuffer.allocate(7 + frameSize);
		frame.put(ADTSParser.write(header, frame));
		frame.put(raw.duplicate());
		frame.flip();
		return frame;
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

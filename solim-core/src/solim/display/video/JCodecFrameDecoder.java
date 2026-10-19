package solim.display.video;

import arc.files.Fi;
import java.io.RandomAccessFile;import java.nio.ByteBuffer;
import org.jcodec.common.Codec;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.DemuxerTrackMeta;
import org.jcodec.common.SeekableDemuxerTrack;
import org.jcodec.common.VideoCodecMeta;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.NIOUtils;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.common.model.Picture;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

/**
 * Pure-Java H.264 decoding through JCodec. Portable to every platform but limited to
 * roughly six frames per second at 1440p, so it is the fallback rather than the default.
 */
final class JCodecFrameDecoder implements VideoFrameDecoder {

	private static final int MAX_DISCARDED_FRAMES = 8;

	private final Fi file;
	private SeekableByteChannel channel;
	private RandomAccessFile mediaFile;
	private MP4Demuxer demuxer;
	private SeekableDemuxerTrack track;
	private H264PacketDecoder packetDecoder;
	private int width;
	private int height;
	private int totalFrames;
	private double duration;
	private double frameRate = 30;
	private double discardUntil = -1;
	private int discarded = 0;

	private JCodecFrameDecoder(Fi file) {
		this.file = file;
	}

	static VideoFrameDecoder open(Fi file) throws Exception {
		JCodecFrameDecoder decoder = new JCodecFrameDecoder(file);
		try {
			decoder.openStream();
			return decoder;
		} catch (Throwable error) {
			decoder.close();
			throw error;
		}
	}

	@Override
	public String name() {
		return "JCodec";
	}

	@Override
	public boolean isNative() {
		return false;
	}

	@Override
	public int width() {
		return width;
	}

	@Override
	public int height() {
		return height;
	}

	@Override
	public double duration() {
		return duration;
	}

	@Override
	public double frameRate() {
		return frameRate;
	}

	@Override
	public double decodeNext(ByteBuffer rgba) throws Exception {
		while (true) {
			Packet packet = track.nextFrame();
			if (packet == null)
				return END_OF_STREAM;
			double pts = packet.getTimescale() > 0 ? (double) packet.getPts() / packet.getTimescale() : 0;
			if (discardUntil >= 0) {
				// The track restarts at the sync frame before the target. The drop is bounded so
				// that a stream of seeks cannot starve playback.
				if (pts < discardUntil && discarded < MAX_DISCARDED_FRAMES) {
					discarded++;
					continue;
				}
				discardUntil = -1;
				discarded = 0;
			}
			Picture picture = packetDecoder.decode(packet.getData());
			if (picture == null)
				continue;
			YuvToRgbaConverter.convert(picture, rgba);
			return pts;
		}
	}

	@Override
	public void seek(double seconds) {
		double target = Math.max(0, seconds);
		discardUntil = target;
		discarded = 0;
		long frameNo = totalFrames > 0
				? Math.min((long) (target * frameRate), totalFrames - 1)
				: (long) (target * frameRate);
		boolean positioned = false;
		try {
			positioned = track.gotoSyncFrame(Math.max(0, frameNo));
		} catch (Throwable ignored) {
		}
		if (!positioned)
			reopenAtStart();
	}

	@Override
	public void close() {
		if (demuxer != null) {
			try {
				demuxer.close();
			} catch (Throwable ignored) {
			}
			demuxer = null;
		}
		if (channel != null) {
			try {
				channel.close();
			} catch (Throwable ignored) {
			}
			channel = null;
		}
		if (mediaFile != null) {
			try {
				mediaFile.close();
			} catch (Throwable ignored) {
			}
			mediaFile = null;
		}
		track = null;
	}

	private void openStream() throws Exception {
		openChannel();
		demuxer = MP4Demuxer.createMP4Demuxer(channel);
		DemuxerTrack demuxerTrack = demuxer.getVideoTrack();
		if (!(demuxerTrack instanceof SeekableDemuxerTrack))
			throw new IllegalStateException("Video track is not seekable");
		track = (SeekableDemuxerTrack) demuxerTrack;
		DemuxerTrackMeta meta = demuxerTrack.getMeta();
		if (meta == null || meta.getCodec() != Codec.H264)
			throw new IllegalStateException("Only H.264 video is supported");
		VideoCodecMeta videoMeta = meta.getVideoCodecMeta();
		Size size = videoMeta != null ? videoMeta.getSize() : null;
		if (size == null || size.getWidth() <= 0 || size.getHeight() <= 0)
			throw new IllegalStateException("Unknown video dimensions");
		width = size.getWidth();
		height = size.getHeight();
		duration = Math.max(0, meta.getTotalDuration());
		totalFrames = Math.max(0, meta.getTotalFrames());
		frameRate = duration > 0 && totalFrames > 0 ? (double) totalFrames / duration : 30;
		ByteBuffer codecPrivate = meta.getCodecPrivate();
		if (codecPrivate == null || codecPrivate.remaining() == 0)
			throw new IllegalStateException("Missing AVC configuration");
		packetDecoder = new H264PacketDecoder(NIOUtils.toArray(codecPrivate));
	}

	private void openChannel() throws Exception {
		RandomAccessFile opened = new RandomAccessFile(file.file(), "r");
		try {
			channel = new FileChannelWrapper(opened.getChannel());
			mediaFile = opened;
		} catch (Throwable error) {
			try {
				opened.close();
			} catch (Throwable ignored) {
			}
			throw error;
		}
	}

	private void reopenAtStart() {
		close();
		try {
			openStream();
			try {
				track.gotoSyncFrame(0);
			} catch (Throwable ignored) {
			}
		} catch (Throwable ignored) {
		}
	}
}

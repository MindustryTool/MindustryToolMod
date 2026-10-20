package solim.display.video;

import java.io.File;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.util.List;
import org.jcodec.codecs.aac.AACDecoder;
import org.jcodec.codecs.aac.AACUtils;
import org.jcodec.codecs.aac.ADTSParser;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.AudioBuffer;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.boxes.AudioSampleEntry;
import org.jcodec.containers.mp4.boxes.SampleEntry;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

public class LeadProbe {

	public static void main(String[] args) throws Exception {
		File clip = new File(args[0]);
		try (RandomAccessFile raf = new RandomAccessFile(clip, "r");
				SeekableByteChannel channel = new FileChannelWrapper(raf.getChannel());
				MP4Demuxer demuxer = MP4Demuxer.createRawMP4Demuxer(channel)) {
			List<DemuxerTrack> tracks = demuxer.getAudioTracks();
			DemuxerTrack track = tracks.get(0);
			ByteBuffer priv = null;
			for (SampleEntry entry : ((org.jcodec.containers.mp4.demuxer.AbstractMP4DemuxerTrack) track)
					.getSampleEntries()) {
				if (entry instanceof AudioSampleEntry) {
					priv = AACUtils.getCodecPrivate((AudioSampleEntry) entry);
				}
			}
			byte[] asc = new byte[priv.remaining()];
			priv.duplicate().get(asc);
			AACDecoder decoder = new AACDecoder(ByteBuffer.wrap(asc));

			int index = 0;
			Packet packet;
			while ((packet = track.nextFrame()) != null && index < 12) {
				ByteBuffer raw = packet.getData();
				ByteBuffer frame = raw.duplicate();
				if (!isAdts(frame)) {
					int size = frame.remaining();
					ADTSParser.Header header = AACUtils.streamInfoToADTS(ByteBuffer.wrap(asc), true, 1, size);
					ByteBuffer wrapped = ByteBuffer.allocate(7 + size);
					wrapped.put(ADTSParser.write(header, wrapped));
					wrapped.put(frame.duplicate());
					wrapped.flip();
					frame = wrapped;
				}
				AudioBuffer decoded = decoder.decodeFrame(frame, null);
				ByteBuffer pcm = decoded.getData();
				int peak = 0;
				int count = 0;
				while (pcm.remaining() > 1) {
					short value = pcm.getShort();
					peak = Math.max(peak, Math.abs(value));
					count++;
				}
				System.out.println("frame " + index + " pts=" + packet.getPts() + " ("
						+ (packet.getPts() / (double) packet.getTimescale()) + "s) samples=" + count + " peak=" + peak);
				index++;
			}
		}
	}

	private static boolean isAdts(ByteBuffer data) {
		if (data.remaining() < 7)
			return false;
		int position = data.position();
		int first = data.get(position) & 0xFF;
		int second = data.get(position + 1) & 0xFF;
		if (first != 0xFF || (second & 0xF0) != 0xF0)
			return false;
		int length = ((data.get(position + 3) & 0x03) << 11) | ((data.get(position + 4) & 0xFF) << 3)
				| ((data.get(position + 5) & 0xFF) >> 5);
		return length == data.remaining();
	}
}

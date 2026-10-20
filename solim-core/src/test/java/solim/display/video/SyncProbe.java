package solim.display.video;

import java.io.File;
import java.io.RandomAccessFile;
import java.util.List;
import org.jcodec.common.DemuxerTrack;
import org.jcodec.common.io.FileChannelWrapper;
import org.jcodec.common.io.SeekableByteChannel;
import org.jcodec.common.model.Packet;
import org.jcodec.containers.mp4.demuxer.MP4Demuxer;

public class SyncProbe {

	public static void main(String[] args) throws Exception {
		File clip = new File(args[0]);
		try (RandomAccessFile raf = new RandomAccessFile(clip, "r");
				SeekableByteChannel channel = new FileChannelWrapper(raf.getChannel());
				MP4Demuxer demuxer = MP4Demuxer.createRawMP4Demuxer(channel)) {
			for (DemuxerTrack video : demuxer.getVideoTracks()) {
				Packet packet = video.nextFrame();
				int frames = 1;
				while (video.nextFrame() != null)
					frames++;
				System.out.println("video  frames=" + frames + " firstPts=" + packet.getPts()
						+ " lastDuration=" + packet.getDuration() + " timescale=" + packet.getTimescale());
			}
			for (DemuxerTrack audio : demuxer.getAudioTracks()) {
				Packet packet = audio.nextFrame();
				int samples = 1;
				long lastPts = packet.getPts();
				Packet next;
				while ((next = audio.nextFrame()) != null) {
					lastPts = next.getPts();
					samples++;
				}
				System.out.println("audio  samples=" + samples + " firstPts=" + packet.getPts()
						+ " lastPts=" + lastPts + " duration=" + packet.getDuration()
						+ " timescale=" + packet.getTimescale());
			}
		}
	}
}

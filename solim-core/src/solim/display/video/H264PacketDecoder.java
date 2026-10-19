package solim.display.video;

import arc.util.Nullable;
import java.nio.ByteBuffer;
import org.jcodec.codecs.h264.H264Decoder;
import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.codecs.h264.io.model.NALUnit;
import org.jcodec.codecs.h264.io.model.NALUnitType;
import org.jcodec.codecs.h264.io.model.SeqParameterSet;
import org.jcodec.common.model.ColorSpace;
import org.jcodec.common.model.Picture;

public final class H264PacketDecoder {

	private final H264Decoder decoder;
	private final byte[][] planes;

	public H264PacketDecoder(byte[] codecPrivate) {
		decoder = H264Decoder.createH264DecoderFromCodecPrivate(ByteBuffer.wrap(codecPrivate));
		planes = allocatePlanes(codecPrivate);
	}

	public @Nullable Picture decode(ByteBuffer packet) {
		if (packet == null || !packet.hasRemaining())
			return null;
		try {
			return decoder.decodeFrame(packet, planes);
		} catch (Throwable ignored) {
			return null;
		}
	}

	private static byte[][] allocatePlanes(byte[] codecPrivate) {
		int mbWidth = 0;
		int mbHeight = 0;
		ByteBuffer units = ByteBuffer.wrap(codecPrivate);
		ByteBuffer unit;
		while ((unit = H264Utils.nextNALUnit(units)) != null) {
			NALUnit nal = NALUnit.read(unit);
			if (nal.type != NALUnitType.SPS)
				continue;
			SeqParameterSet sps = H264Utils.readSPS(unit);
			mbWidth = Math.max(mbWidth, sps.picWidthInMbsMinus1 + 1);
			mbHeight = Math.max(mbHeight, SeqParameterSet.getPicHeightInMbs(sps));
		}
		return Picture.create(Math.max(1, mbWidth) << 4, Math.max(1, mbHeight) << 4, ColorSpace.YUV444).getData();
	}
}

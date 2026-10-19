package solim.display.video;

import java.nio.ByteBuffer;

/**
 * Produces RGBA8888 frames for {@link VideoPlayer}. Implementations own their file
 * access and codec state, and are always closed by their creator.
 */
public interface VideoFrameDecoder extends AutoCloseable {

	/** Returned by {@link #decodeNext(ByteBuffer)} when the stream has no more frames. */
	double END_OF_STREAM = -1d;

	String name();

	boolean isNative();

	int width();

	int height();

	double duration();

	double frameRate();

	/**
	 * Decodes the next frame into {@code rgba} (capacity at least {@code width * height * 4})
	 * and returns its presentation time in seconds, or {@link #END_OF_STREAM}.
	 */
	double decodeNext(ByteBuffer rgba) throws Exception;

	/** Positions the stream so the next decoded frame is at or after {@code seconds}. */
	void seek(double seconds) throws Exception;

	@Override
	void close();
}

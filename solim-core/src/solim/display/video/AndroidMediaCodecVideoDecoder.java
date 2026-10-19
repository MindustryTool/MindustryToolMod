package solim.display.video;

import arc.files.Fi;
import arc.util.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/**
 * Android decoder built on {@code MediaCodec} and {@code MediaExtractor}. It decodes H.264
 * into a {@code ByteBuffer} RGBA frame so it slots into the existing {@link VideoFrameDecoder}
 * contract; every Android-specific call is made reflectively so {@code solim-core} keeps
 * compiling against the Java 17 toolchain without an {@code android.jar}.
 */
final class AndroidMediaCodecVideoDecoder implements VideoFrameDecoder {

	private static final int COLOR_FORMAT_YUV420_PLANAR = 19;
	private static final int COLOR_FORMAT_YUV420_SEMI_PLANAR = 21;
	private static final int COLOR_FORMAT_YUV420_PACKED_SEMI_PLANAR = 39;
	private static final int BUFFER_FLAG_END_OF_STREAM = 4;
	private static final int INFO_OUTPUT_FORMAT_CHANGED = -2;
	private static final int INFO_OUTPUT_BUFFERS_CHANGED = -3;
	private static final int INFO_TRY_AGAIN_LATER = -1;
	private static final int SEEK_TO_PREVIOUS_SYNC = 0;

	private final Object extractor;
	private Object codec;
	private int videoTrack = -1;
	private int width;
	private int height;
	private int codedWidth;
	private int codedHeight;
	private int offsetX;
	private int offsetY;
	private int stride;
	private int sliceHeight;
	private int colorFormat = COLOR_FORMAT_YUV420_SEMI_PLANAR;
	private double duration;
	private double frameRate = 30;
	private boolean started;
	private boolean inputEOS;
	private boolean outputEOS;
	private long currentPtsUs;
	private final Nv12ToRgbaConverter converter = new Nv12ToRgbaConverter();

	/**
	 * Guards the codec. Releasing it while a dequeue is in flight leaves the decoder calling
	 * into a freed native object, which kills the process without a Java stack trace.
	 */
	private final Object streamLock = new Object();
	private byte[] rawFrame = new byte[0];
	private int[] pixels = new int[0];

	private AndroidMediaCodecVideoDecoder(Fi file) throws Exception {
		Class<?> extractorClass = Class.forName("android.media.MediaExtractor");
		extractor = extractorClass.getDeclaredConstructor().newInstance();
		extractorClass.getMethod("setDataSource", String.class).invoke(extractor, file.file().getAbsolutePath());

		// Pick the first video track.
		int trackCount = (int) extractorClass.getMethod("getTrackCount").invoke(extractor);
		Class<?> formatClass = Class.forName("android.media.MediaFormat");
		Object videoFormat = null;
		for (int index = 0; index < trackCount; index++) {
			Object candidate = extractorClass.getMethod("getTrackFormat", int.class).invoke(extractor, index);
			String mime = (String) formatClass.getMethod("getString", String.class).invoke(candidate, "mime");
			if (mime != null && mime.startsWith("video/")) {
				videoTrack = index;
				videoFormat = candidate;
				break;
			}
		}
		if (videoTrack < 0 || videoFormat == null)
			throw new IllegalStateException("No video track");

		extractorClass.getMethod("selectTrack", int.class).invoke(extractor, videoTrack);

		String mime = (String) formatClass.getMethod("getString", String.class).invoke(videoFormat, "mime");
		width = getInt(videoFormat, "width", 0);
		height = getInt(videoFormat, "height", 0);
		duration = getLong(videoFormat, "durationUs", 0L) / 1_000_000.0;
		frameRate = getFloat(videoFormat, "frame-rate", 30f);
		if (width <= 0 || height <= 0)
			throw new IllegalStateException("Unknown video dimensions");

		Class<?> codecClass = Class.forName("android.media.MediaCodec");
		// Require API 21+ buffer accessors.
		codecClass.getMethod("getInputBuffer", int.class);
		codecClass.getMethod("getOutputBuffer", int.class);
		codecClass.getMethod("configure", formatClass, Class.forName("android.view.Surface"),
				Class.forName("android.media.MediaCrypto"), int.class)
				.invoke(codec, videoFormat, null, null, 0);
		codecClass.getMethod("start").invoke(codec);
		started = true;

		// Initial output format may be available immediately.
		try {
			Object outFormat = codecClass.getMethod("getOutputFormat").invoke(codec);
			applyOutputFormat(outFormat);
		} catch (Throwable ignored) {
		}
	}

	static @Nullable VideoFrameDecoder tryOpen(Fi file) {
		AndroidMediaCodecVideoDecoder decoder = null;
		try {
			decoder = new AndroidMediaCodecVideoDecoder(file);
			// Prime dimensions/format by pulling the first output format if not already set.
			if (decoder.width <= 0) {
				decoder.close();
				return null;
			}
			return decoder;
		} catch (Throwable error) {
			if (decoder != null)
				decoder.close();
			return null;
		}
	}

	static boolean isSupported() {
		try {
			Class.forName("android.media.MediaCodec");
			Class.forName("android.media.MediaExtractor");
			// Require API 21+ buffer accessors.
			Class.forName("android.media.MediaCodec").getMethod("getInputBuffer", int.class);
			Class.forName("android.media.MediaCodec").getMethod("getOutputBuffer", int.class);
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	@Override
	public String name() {
		return "MediaCodec";
	}

	@Override
	public boolean isNative() {
		return true;
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
	public double decodeNext(ByteBuffer rgba) {
		synchronized (streamLock) {
			return decodeNextLocked(rgba);
		}
	}

	private double decodeNextLocked(ByteBuffer rgba) {
		Class<?> codecClass = null;
		try {
			codecClass = Class.forName("android.media.MediaCodec");
		} catch (Throwable ignored) {
			return END_OF_STREAM;
		}
		long timeoutUs = 10_000;
		int outputIndex = -1;
		while (true) {
			if (disposedGuard())
				return END_OF_STREAM;

			// Feed input.
			if (!inputEOS) {
				try {
					int inIndex = (int) codecClass.getMethod("dequeueInputBuffer", long.class).invoke(codec, timeoutUs);
					if (inIndex >= 0) {
						ByteBuffer inBuf = (ByteBuffer) codecClass.getMethod("getInputBuffer", int.class).invoke(codec, inIndex);
						if (inBuf != null) {
							inBuf.clear();
							int sampleSize = (int) Class.forName("android.media.MediaExtractor")
									.getMethod("readSampleData", ByteBuffer.class, int.class).invoke(extractor, inBuf, 0);
							if (sampleSize < 0) {
								codecClass.getMethod("queueInputBuffer", int.class, int.class, int.class, long.class, int.class)
										.invoke(codec, inIndex, 0, 0, 0L, BUFFER_FLAG_END_OF_STREAM);
								inputEOS = true;
							} else {
								long pts = (long) Class.forName("android.media.MediaExtractor")
										.getMethod("getSampleTime").invoke(extractor);
								codecClass.getMethod("queueInputBuffer", int.class, int.class, int.class, long.class, int.class)
										.invoke(codec, inIndex, 0, sampleSize, pts, 0);
								Class.forName("android.media.MediaExtractor").getMethod("advance").invoke(extractor);
							}
						}
					}
				} catch (Throwable ignored) {
				}
			}

			// Dequeue output.
			try {
				Class<?> infoClass = Class.forName("android.media.MediaCodec$BufferInfo");
				Object info = infoClass.getDeclaredConstructor().newInstance();
				outputIndex = (int) codecClass.getMethod("dequeueOutputBuffer", infoClass, long.class).invoke(codec, info, timeoutUs);
				if (outputIndex >= 0) {
					ByteBuffer outBuf = (ByteBuffer) codecClass.getMethod("getOutputBuffer", int.class).invoke(codec, outputIndex);
					int offset = (int) infoClass.getField("offset").get(info);
					int size = (int) infoClass.getField("size").get(info);
					long ptsUs = (long) infoClass.getField("presentationTimeUs").get(info);
					int flags = (int) infoClass.getField("flags").get(info);

					if (outBuf != null && size > 0) {
						ByteBuffer view = outBuf.duplicate();
						view.position(offset);
						view.limit(offset + size);
						ByteBuffer slice = view.slice();
						convertFrame(slice, rgba);
					}
					codecClass.getMethod("releaseOutputBuffer", int.class, boolean.class).invoke(codec, outputIndex, false);
					outputIndex = -1;

					currentPtsUs = ptsUs;
					if ((flags & BUFFER_FLAG_END_OF_STREAM) != 0)
						outputEOS = true;
					return ptsUs / 1_000_000.0;
				} else if (outputIndex == INFO_OUTPUT_FORMAT_CHANGED) {
					Object newFormat = codecClass.getMethod("getOutputFormat").invoke(codec);
					applyOutputFormat(newFormat);
				} else if (outputIndex == INFO_OUTPUT_BUFFERS_CHANGED) {
					// deprecated on API 21+; ignore
				}
			} catch (Throwable ignored) {
			}

			if (outputEOS && outputIndex < 0)
				return END_OF_STREAM;
			// Avoid busy-spinning.
			try {
				Thread.sleep(5);
			} catch (InterruptedException e) {
				Thread.currentThread().interrupt();
				return END_OF_STREAM;
			}
		}
	}

	private boolean disposedGuard() {
		return false;
	}

	@Override
	public void seek(double seconds) {
		synchronized (streamLock) {
			try {
				Class<?> codecClass = Class.forName("android.media.MediaCodec");
				codecClass.getMethod("flush").invoke(codec);
				Class<?> extractorClass = Class.forName("android.media.MediaExtractor");
				extractorClass.getMethod("seekTo", long.class, int.class).invoke(extractor, (long) (seconds * 1_000_000.0), SEEK_TO_PREVIOUS_SYNC);
				inputEOS = false;
				outputEOS = false;
			} catch (Throwable ignored) {
			}
		}
	}

	@Override
	public void close() {
		synchronized (streamLock) {
			try {
				if (codec != null) {
					Class<?> codecClass = Class.forName("android.media.MediaCodec");
					try {
						codecClass.getMethod("stop").invoke(codec);
					} catch (Throwable ignored) {
					}
					codecClass.getMethod("release").invoke(codec);
					codec = null;
				}
			} catch (Throwable ignored) {
			}
		}
		synchronized (streamLock) {
			try {
				if (extractor != null) {
					Class.forName("android.media.MediaExtractor").getMethod("release").invoke(extractor);
				}
			} catch (Throwable ignored) {
			}
			converter.close();
		}
	}

	private void applyOutputFormat(Object format) {
		try {
			Class<?> fc = Class.forName("android.media.MediaFormat");
			colorFormat = getInt(format, "color-format", COLOR_FORMAT_YUV420_SEMI_PLANAR);
			stride = getInt(format, "stride", width);
			if (stride <= 0)
				stride = width;
			sliceHeight = getInt(format, "slice-height", height);
			if (sliceHeight <= 0)
				sliceHeight = height;
			int cropLeft = getInt(format, "crop-left", 0);
			int cropTop = getInt(format, "crop-top", 0);
			int cropRight = getInt(format, "crop-right", width - 1);
			int cropBottom = getInt(format, "crop-bottom", height - 1);
			offsetX = cropLeft;
			offsetY = cropTop;
			width = cropRight - cropLeft + 1;
			height = cropBottom - cropTop + 1;
			if (width <= 0)
				width = codedWidth > 0 ? codedWidth : width;
			if (height <= 0)
				height = codedHeight > 0 ? codedHeight : height;
		} catch (Throwable ignored) {
		}
		if (codedWidth <= 0)
			codedWidth = width;
		if (codedHeight <= 0)
			codedHeight = height;
		int needed = width * height * 4;
		if (pixels.length < needed)
			pixels = new int[needed / 4];
		int frameBytes = Math.max(1, stride) * Math.max(1, sliceHeight) * 3 / 2;
		if (rawFrame.length < frameBytes)
			rawFrame = new byte[frameBytes];
	}

	private void convertFrame(ByteBuffer outBuf, ByteBuffer rgba) {
		int uvOffset;
		int ySize = stride * sliceHeight;
		if (colorFormat == COLOR_FORMAT_YUV420_PLANAR) {
			// I420: Y (ySize), U (cStride*cHeight), V (cStride*cHeight). Interleave to NV12.
			int cStride = stride / 2;
			int cHeight = sliceHeight / 2;
			int uOff = ySize;
			int vOff = ySize + cStride * cHeight;
			// copy Y
			outBuf.position(0);
			outBuf.get(rawFrame, 0, Math.min(ySize, outBuf.remaining()));
			// interleave U,V into rawFrame's UV region
			for (int row = 0; row < cHeight; row++) {
				for (int col = 0; col < cStride; col++) {
					rawFrame[ySize + row * stride + col * 2] = outBuf.get(uOff + row * cStride + col);
					rawFrame[ySize + row * stride + col * 2 + 1] = outBuf.get(vOff + row * cStride + col);
				}
			}
			uvOffset = ySize;
		} else if (colorFormat == COLOR_FORMAT_YUV420_PACKED_SEMI_PLANAR) {
			// NV21 (V,U): swap pairs in place into NV12.
			outBuf.position(0);
			outBuf.get(rawFrame, 0, Math.min(rawFrame.length, outBuf.remaining()));
			for (int i = ySize; i + 1 < rawFrame.length; i += 2) {
				byte tmp = rawFrame[i];
				rawFrame[i] = rawFrame[i + 1];
				rawFrame[i + 1] = tmp;
			}
			uvOffset = ySize;
		} else {
			// NV12 (and flexible treated as NV12).
			outBuf.position(0);
			outBuf.get(rawFrame, 0, Math.min(rawFrame.length, outBuf.remaining()));
			uvOffset = stride * sliceHeight;
		}
		converter.convert(rawFrame, stride, uvOffset, sliceHeight, offsetX, offsetY, width, height, false, pixels);
		rgba.clear();
		rgba.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(pixels, 0, width * height);
		rgba.position(0);
		rgba.limit(width * height * 4);
	}

	private static int getInt(Object format, String key, int fallback) {
		try {
			return (int) Class.forName("android.media.MediaFormat").getMethod("getInteger", String.class).invoke(format, key);
		} catch (Throwable ignored) {
			return fallback;
		}
	}

	private static long getLong(Object format, String key, long fallback) {
		try {
			return (long) Class.forName("android.media.MediaFormat").getMethod("getLong", String.class).invoke(format, key);
		} catch (Throwable ignored) {
			return fallback;
		}
	}

	private static float getFloat(Object format, String key, float fallback) {
		try {
			return (float) Class.forName("android.media.MediaFormat").getMethod("getFloat", String.class).invoke(format, key);
		} catch (Throwable ignored) {
			return fallback;
		}
	}
}

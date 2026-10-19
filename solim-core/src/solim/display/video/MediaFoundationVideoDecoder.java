package solim.display.video;

import arc.files.Fi;
import arc.util.Log;
import arc.util.Nullable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import solim.display.video.foreign.ForeignMemory;
import solim.display.video.foreign.ForeignRuntime;

/**
 * Hardware-independent Windows decoder built on Media Foundation's source reader. The
 * reader demultiplexes the MP4, decodes H.264 through the operating system's codec, and
 * hands back NV12 frames, which are converted to RGBA in parallel; that native decode
 * step is what makes 1440p reachable.
 */
final class MediaFoundationVideoDecoder implements VideoFrameDecoder {

	private static final int MF_VERSION = 0x00020070;
	private static final int MF_SOURCE_READER_FIRST_VIDEO_STREAM = 0xFFFFFFFC;
	private static final int MF_SOURCE_READER_ALL_STREAMS = 0xFFFFFFFE;
	private static final int MF_SOURCE_READER_MEDIASOURCE = 0xFFFFFFFF;
	private static final int MF_SOURCE_READERF_ENDOFSTREAM = 0x2;
	private static final int MF_SOURCE_READERF_CURRENTMEDIATYPECHANGED = 0x20;
	private static final int VT_I8 = 20;
	private static final int VT_UI8 = 21;
	private static final int COINIT_MULTITHREADED = 0;

	private static final int IUNKNOWN_RELEASE = 2;
	private static final int ATTRIBUTES_GET_UINT32 = 7;
	private static final int ATTRIBUTES_GET_BLOB = 15;
	private static final int ATTRIBUTES_GET_UINT64 = 8;
	private static final int ATTRIBUTES_SET_UINT32 = 21;
	private static final int ATTRIBUTES_SET_UINT64 = 22;
	private static final int ATTRIBUTES_SET_GUID = 24;
	private static final int SOURCE_READER_SET_STREAM_SELECTION = 4;
	private static final int SOURCE_READER_GET_NATIVE_MEDIA_TYPE = 5;
	private static final int SOURCE_READER_GET_CURRENT_MEDIA_TYPE = 6;
	private static final int SOURCE_READER_SET_CURRENT_MEDIA_TYPE = 7;
	private static final int SOURCE_READER_SET_CURRENT_POSITION = 8;
	private static final int SOURCE_READER_READ_SAMPLE = 9;
	private static final int SOURCE_READER_GET_PRESENTATION_ATTRIBUTE = 12;
	private static final int SAMPLE_CONVERT_TO_CONTIGUOUS_BUFFER = 41;
	private static final int BUFFER_LOCK = 3;
	private static final int BUFFER_UNLOCK = 4;

	private static final Object[] NO_ARGUMENTS = new Object[0];

	private static volatile Boolean supported;

	private final ForeignRuntime ffi;
	private final ForeignMemory actualStream;
	private final ForeignMemory streamFlags;
	private final ForeignMemory timestamp;
	private final ForeignMemory sampleOut;
	private final ForeignMemory bufferOut;
	private final ForeignMemory dataOut;
	private final ForeignMemory maxLength;
	private final ForeignMemory currentLength;
	private final ForeignMemory timeVariant;
	private final ForeignMemory timeFormat;
	private final long startup;
	private final long shutdown;
	private final long createMediaType;
	private final long createAttributes;
	private final long createSourceReader;
	private final long coInitializeEx;

	private @Nullable ForeignMemory reader;
	private @Nullable ForeignMemory attributes;
	private boolean started;
	private int width;
	private int height;
	private int codedWidth;
	private int codedHeight;
	private int offsetX;
	private int offsetY;
	private int stride;
	private double duration;
	private double frameRate = 30;
	private double discardUntil = -1;
	private byte[] rawFrame = new byte[0];
	private int[] pixels = new int[0];
	private final Nv12ToRgbaConverter converter = new Nv12ToRgbaConverter();

	private MediaFoundationVideoDecoder() {
		ffi = ForeignRuntime.get();
		actualStream = ffi.allocate(4);
		streamFlags = ffi.allocate(4);
		timestamp = ffi.allocate(8);
		sampleOut = ffi.allocate(8);
		bufferOut = ffi.allocate(8);
		dataOut = ffi.allocate(8);
		maxLength = ffi.allocate(4);
		currentLength = ffi.allocate(4);
		timeVariant = ffi.allocate(24);
		timeFormat = ffi.allocate(16);
		startup = ffi.symbol("MFStartup");
		shutdown = ffi.symbol("MFShutdown");
		createMediaType = ffi.symbol("MFCreateMediaType");
		createAttributes = ffi.symbol("MFCreateAttributes");
		createSourceReader = ffi.symbol("MFCreateSourceReaderFromURL");
		coInitializeEx = ffi.symbol("CoInitializeEx");
	}

	static boolean isSupported() {
		Boolean cached = supported;
		if (cached != null)
			return cached;
		boolean available = probe();
		supported = available;
		return available;
	}

	static @Nullable VideoFrameDecoder tryOpen(Fi file) {
		if (!isSupported())
			return null;
		MediaFoundationVideoDecoder decoder = new MediaFoundationVideoDecoder();
		try {
			decoder.open(file);
			return decoder;
		} catch (Throwable error) {
			Log.debug("SolimVideo: Media Foundation decode unavailable (" + error.getMessage()
					+ "), using software decoder");
			decoder.close();
			return null;
		}
	}

	private static boolean probe() {
		if (!VideoDecoders.isNativePlatform() || !ForeignRuntime.isAvailable())
			return false;
		try {
			ForeignRuntime ffi = ForeignRuntime.get();
			ffi.loadLibrary("mfplat");
			ffi.loadLibrary("mfreadwrite");
			ffi.loadLibrary("ole32");
			ffi.symbol("MFStartup");
			ffi.symbol("MFShutdown");
			ffi.symbol("MFCreateMediaType");
			ffi.symbol("MFCreateAttributes");
			ffi.symbol("MFCreateSourceReaderFromURL");
			ffi.symbol("CoInitializeEx");
			return true;
		} catch (Throwable ignored) {
			return false;
		}
	}

	@Override
	public String name() {
		return "Media Foundation";
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
		while (true) {
			check(call(reader(), SOURCE_READER_READ_SAMPLE, MF_SOURCE_READER_FIRST_VIDEO_STREAM, 0,
					actualStream, streamFlags, timestamp, sampleOut), "ReadSample");
			int flags = streamFlags.getInt(0);
			ForeignMemory sample = sampleOut.getPointer(0);
			if (sample.isNull()) {
				if ((flags & MF_SOURCE_READERF_ENDOFSTREAM) != 0)
					return END_OF_STREAM;
				continue;
			}
			try {
				if ((flags & MF_SOURCE_READERF_ENDOFSTREAM) != 0)
					return END_OF_STREAM;
				if ((flags & MF_SOURCE_READERF_CURRENTMEDIATYPECHANGED) != 0)
					refreshOutputFormat();
				double pts = timestamp.getLong(0) / 10_000_000.0;
				if (discardUntil >= 0) {
					if (pts + 1e-6 < discardUntil)
						continue;
					discardUntil = -1;
				}
				copyFrame(sample, rgba);
				return pts;
			} finally {
				release(sample);
			}
		}
	}

	@Override
	public void seek(double seconds) {
		double target = Math.max(0, seconds);
		timeVariant.clear();
		timeVariant.setShort(0, (short) VT_I8);
		timeVariant.setLong(8, (long) (target * 10_000_000L));
		check(call(reader(), SOURCE_READER_SET_CURRENT_POSITION, timeFormat, timeVariant), "SetCurrentPosition");
		discardUntil = target;
	}

	@Override
	public void close() {
		converter.close();
		if (reader != null) {
			release(reader);
			reader = null;
		}
		if (attributes != null) {
			release(attributes);
			attributes = null;
		}
		if (started) {
			started = false;
			try {
				ffi.invoke(shutdown);
			} catch (Throwable ignored) {
			}
		}
	}

	private void open(Fi file) {
		timeFormat.clear();
		initializeCom();
		check(ffi.invoke(startup, MF_VERSION, 0), "MFStartup");
		started = true;

		ForeignMemory attributesOut = ffi.allocate(8);
		check(ffi.invoke(createAttributes, attributesOut, 2), "MFCreateAttributes");
		attributes = attributesOut.getPointer(0);
		setUint32(attributes, Keys.MF_SOURCE_READER_ENABLE_VIDEO_PROCESSING, 1);

		ForeignMemory readerOut = ffi.allocate(8);
		check(ffi.invoke(createSourceReader, ffi.wideString(file.file().getAbsolutePath()),
				attributes, readerOut), "MFCreateSourceReaderFromURL");
		reader = readerOut.getPointer(0);

		setStreamSelection(MF_SOURCE_READER_ALL_STREAMS, false);
		setStreamSelection(MF_SOURCE_READER_FIRST_VIDEO_STREAM, true);

		ForeignMemory nativeType = getNativeMediaType();
		long codedSize = 0;
		try {
			if (nativeType != null) {
				codedSize = getUint64(nativeType, Keys.MF_MT_FRAME_SIZE);
				codedWidth = (int) (codedSize >>> 32);
				codedHeight = (int) (codedSize & 0xFFFFFFFFL);
				readAperture(nativeType);
				double nativeRate = readFrameRate(nativeType);
				if (nativeRate > 0)
					frameRate = nativeRate;
			}
		} finally {
			if (nativeType != null)
				release(nativeType);
		}

		ForeignMemory outputType = createMediaType();
		try {
			setGuid(outputType, Keys.MF_MT_MAJOR_TYPE, Keys.MFMediaType_Video);
			setGuid(outputType, Keys.MF_MT_SUBTYPE, Keys.MFVideoFormat_NV12);
			if (codedSize > 0)
				setUint64(outputType, Keys.MF_MT_FRAME_SIZE, codedSize);
			check(call(reader, SOURCE_READER_SET_CURRENT_MEDIA_TYPE,
					MF_SOURCE_READER_FIRST_VIDEO_STREAM, ffi.pointer(0), outputType), "SetCurrentMediaType");
		} finally {
			release(outputType);
		}

		readOutputFormat();
		if (codedWidth <= 0 || codedHeight <= 0) {
			codedWidth = (int) (codedSize >>> 32);
			codedHeight = (int) (codedSize & 0xFFFFFFFFL);
		}
		if (width <= 0 || height <= 0) {
			width = codedWidth > 0 ? codedWidth : width;
			height = codedHeight > 0 ? codedHeight : height;
			offsetX = 0;
			offsetY = 0;
		}
		duration = readDuration();
		rawFrame = new byte[Nv12ToRgbaConverter.frameSize(stride, codedHeight > 0 ? codedHeight : height)];
		pixels = new int[width * height];
	}

	private void refreshOutputFormat() {
		readOutputFormat();
		int required = Nv12ToRgbaConverter.frameSize(stride, codedHeight);
		if (rawFrame.length < required)
			rawFrame = new byte[required];
	}

	private void readOutputFormat() {
		ForeignMemory type = getCurrentMediaType();
		try {
			long frameSize = getUint64(type, Keys.MF_MT_FRAME_SIZE);
			int w = (int) (frameSize >>> 32);
			int h = (int) (frameSize & 0xFFFFFFFFL);
			if (w > 0 && h > 0 && codedWidth <= 0) {
				codedWidth = w;
				codedHeight = h;
			}
			int negotiatedStride = (int) getUint32(type, Keys.MF_MT_DEFAULT_STRIDE);
			if (negotiatedStride != 0)
				stride = negotiatedStride;
			else if (stride == 0 && w > 0)
				stride = w;
		} finally {
			release(type);
		}
	}

	private double readFrameRate(ForeignMemory type) {
		long rate = getUint64(type, Keys.MF_MT_FRAME_RATE);
		double numerator = rate >>> 32;
		double denominator = rate & 0xFFFFFFFFL;
		return numerator > 0 && denominator > 0 ? numerator / denominator : 0;
	}

	private void readAperture(ForeignMemory type) {
		ForeignMemory sizeOut = ffi.allocate(4);
		int hr = call(type, ATTRIBUTES_GET_BLOB, Keys.MF_MT_GEOMETRIC_APERTURE, ffi.pointer(0), 0, sizeOut);
		int blobSize = hr < 0 ? 0 : sizeOut.getInt(0);
		if (blobSize < 16) {
			if (width <= 0)
				width = codedWidth;
			if (height <= 0)
				height = codedHeight;
			offsetX = 0;
			offsetY = 0;
			return;
		}
		ForeignMemory buffer = ffi.allocate(blobSize);
		hr = call(type, ATTRIBUTES_GET_BLOB, Keys.MF_MT_GEOMETRIC_APERTURE, buffer, blobSize, sizeOut);
		if (hr < 0) {
			if (width <= 0)
				width = codedWidth;
			if (height <= 0)
				height = codedHeight;
			offsetX = 0;
			offsetY = 0;
			return;
		}
		offsetX = buffer.getShort(2);
		offsetY = buffer.getShort(6);
		int areaW = buffer.getInt(8);
		int areaH = buffer.getInt(12);
		if (width <= 0)
			width = areaW > 0 ? areaW : codedWidth;
		if (height <= 0)
			height = areaH > 0 ? areaH : codedHeight;
		if (width <= 0)
			width = codedWidth;
		if (height <= 0)
			height = codedHeight;
	}

	private double readDuration() {
		ForeignMemory variant = ffi.allocate(24);
		int result = call(reader(), SOURCE_READER_GET_PRESENTATION_ATTRIBUTE,
				MF_SOURCE_READER_MEDIASOURCE, Keys.MF_PD_DURATION, variant);
		if (result < 0 || variant.getShort(0) != VT_UI8)
			return 0;
		return variant.getLong(8) / 10_000_000.0;
	}

	private void copyFrame(ForeignMemory sample, ByteBuffer rgba) {
		int sourceStride = Math.abs(stride);
		check(call(sample, SAMPLE_CONVERT_TO_CONTIGUOUS_BUFFER, bufferOut), "ConvertToContiguousBuffer");
		ForeignMemory buffer = bufferOut.getPointer(0);
		try {
			check(call(buffer, BUFFER_LOCK, dataOut, maxLength, currentLength), "Lock");
			try {
				int length = currentLength.getInt(0);
				if (length > rawFrame.length)
					rawFrame = new byte[length];
				ffi.view(dataOut.getPointer(0).address(), length).readBytes(rawFrame, length);
			} finally {
				call(buffer, BUFFER_UNLOCK);
			}
		} finally {
			release(buffer);
		}
		converter.convert(rawFrame, sourceStride, sourceStride * codedHeight, codedHeight,
				offsetX, offsetY, width, height, stride < 0, pixels);
		rgba.clear();
		rgba.order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(pixels, 0, width * height);
		rgba.position(0);
		rgba.limit(width * height * 4);
	}

	private void setStreamSelection(int streamIndex, boolean selected) {
		check(call(reader(), SOURCE_READER_SET_STREAM_SELECTION, streamIndex, selected ? 1 : 0),
				"SetStreamSelection");
	}

	private ForeignMemory createMediaType() {
		ForeignMemory typeOut = ffi.allocate(8);
		check(ffi.invoke(createMediaType, typeOut), "MFCreateMediaType");
		return typeOut.getPointer(0);
	}

	private ForeignMemory getCurrentMediaType() {
		ForeignMemory typeOut = ffi.allocate(8);
		check(call(reader(), SOURCE_READER_GET_CURRENT_MEDIA_TYPE,
				MF_SOURCE_READER_FIRST_VIDEO_STREAM, typeOut), "GetCurrentMediaType");
		return typeOut.getPointer(0);
	}

	private @Nullable ForeignMemory getNativeMediaType() {
		ForeignMemory typeOut = ffi.allocate(8);
		int result = call(reader(), SOURCE_READER_GET_NATIVE_MEDIA_TYPE,
				MF_SOURCE_READER_FIRST_VIDEO_STREAM, 0, typeOut);
		return result < 0 ? null : typeOut.getPointer(0);
	}

	private void setUint64(ForeignMemory target, ForeignMemory key, long value) {
		check(call(target, ATTRIBUTES_SET_UINT64, key, value), "SetUINT64");
	}

	private void setUint32(ForeignMemory target, ForeignMemory key, int value) {
		check(call(target, ATTRIBUTES_SET_UINT32, key, value), "SetUINT32");
	}

	private long getUint32(ForeignMemory target, ForeignMemory key) {
		ForeignMemory value = ffi.allocate(4);
		int result = call(target, ATTRIBUTES_GET_UINT32, key, value);
		return result < 0 ? 0 : value.getInt(0) & 0xFFFFFFFFL;
	}

	private long getUint64(ForeignMemory target, ForeignMemory key) {
		ForeignMemory value = ffi.allocate(8);
		int result = call(target, ATTRIBUTES_GET_UINT64, key, value);
		return result < 0 ? 0 : value.getLong(0);
	}

	private void setGuid(ForeignMemory target, ForeignMemory key, ForeignMemory value) {
		check(call(target, ATTRIBUTES_SET_GUID, key, value), "SetGUID");
	}

	private int call(ForeignMemory object, int methodIndex, Object... arguments) {
		return ffi.vtableCall(object, methodIndex, arguments);
	}

	private static void release(@Nullable ForeignMemory object) {
		if (object != null)
			ForeignRuntime.get().vtableCall(object, IUNKNOWN_RELEASE, NO_ARGUMENTS);
	}

	private static void check(int result, String operation) {
		if (result < 0)
			throw new IllegalStateException(operation + " failed (0x" + Integer.toHexString(result) + ")");
	}

	private ForeignMemory reader() {
		ForeignMemory value = reader;
		if (value == null)
			throw new IllegalStateException("Decoder is closed");
		return value;
	}

	private void initializeCom() {
		try {
			// RPC_E_CHANGED_MODE only means the thread already joined an apartment, which
			// Media Foundation accepts as well.
			ffi.invoke(coInitializeEx, ffi.pointer(0), COINIT_MULTITHREADED);
		} catch (Throwable ignored) {
		}
	}

	/**
	 * Media Foundation attribute keys. Held in a separate holder so the decoder class can
	 * initialize without touching native memory, which keeps {@link #isSupported()} safe on
	 * runtimes without the Foreign Function &amp; Memory API.
	 */
	private static final class Keys {
		static final ForeignMemory MF_MT_MAJOR_TYPE = guid(0x48eba18eL, 0xf8c9, 0x4687,
				0xbf, 0x11, 0x0a, 0x74, 0xc9, 0xf9, 0x6a, 0x8f);
		static final ForeignMemory MF_MT_SUBTYPE = guid(0xf7e34c9aL, 0x42e8, 0x4714,
				0xb7, 0x4b, 0xcb, 0x29, 0xd7, 0x2c, 0x35, 0xe5);
		static final ForeignMemory MF_MT_FRAME_SIZE = guid(0x1652c33dL, 0xd6b2, 0x4012,
				0xb8, 0x34, 0x72, 0x03, 0x08, 0x49, 0xa3, 0x7d);
		static final ForeignMemory MF_MT_DEFAULT_STRIDE = guid(0x644b4e48L, 0x1e02, 0x4516,
				0xb0, 0xeb, 0xc0, 0x1c, 0xa9, 0xd4, 0x9a, 0xc6);
		static final ForeignMemory MF_MT_FRAME_RATE = guid(0xc459a2e8L, 0x3d2c, 0x4e44,
				0xb1, 0x32, 0xfe, 0xe5, 0x15, 0x6c, 0x7b, 0xb0);
		static final ForeignMemory MF_MT_GEOMETRIC_APERTURE = guid(0x66758743L, 0x7e5f, 0x400d,
				0x98, 0x0a, 0xaa, 0x85, 0x96, 0xc8, 0x56, 0x96);
		static final ForeignMemory MF_PD_DURATION = guid(0x6c990d33L, 0xbb8e, 0x477a,
				0x85, 0x98, 0x0d, 0x5d, 0x96, 0xfc, 0xd8, 0x8a);
		static final ForeignMemory MFMediaType_Video = guid(0x73646976L, 0x0000, 0x0010,
				0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71);
		static final ForeignMemory MFVideoFormat_NV12 = guid(0x3231564EL, 0x0000, 0x0010,
				0x80, 0x00, 0x00, 0xaa, 0x00, 0x38, 0x9b, 0x71);
		static final ForeignMemory MF_SOURCE_READER_ENABLE_VIDEO_PROCESSING = guid(0xfb394f3dL, 0xccf1, 0x42ee,
				0xbb, 0xb3, 0xf9, 0xb8, 0x45, 0xd5, 0x68, 0x1d);

		private Keys() {
		}

		private static ForeignMemory guid(long data1, int data2, int data3, int... data4) {
			ForeignMemory value = ForeignRuntime.get().allocate(16);
			value.setInt(0, (int) data1);
			value.setShort(4, (short) data2);
			value.setShort(6, (short) data3);
			for (int index = 0; index < 8; index++)
				value.setByte(8 + index, (byte) data4[index]);
			return value;
		}
	}
}

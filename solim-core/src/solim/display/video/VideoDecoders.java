package solim.display.video;

import arc.files.Fi;
import arc.util.Log;
import arc.util.Nullable;
import java.util.Locale;

/**
 * Opens the best available decoder for a video file. The native path is preferred and any
 * failure — unsupported platform, missing codec, unreadable stream — transparently falls
 * back to the portable JCodec implementation.
 */
public final class VideoDecoders {

	public enum Preference {
		AUTO, SOFTWARE
	}

	private static final String PREFERENCE_PROPERTY = "solim.video.decoder";

	private static final boolean WINDOWS =
			System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("windows");

	private static volatile Preference preference = readPreference();
	private static volatile boolean nativeDisabled;

	private VideoDecoders() {
	}

	/**
	 * Whether the running platform can host the native decoder at all. Checked without
	 * touching any JNA type, so an unsupported runtime never fails to load the native
	 * decoder class.
	 */
	static boolean isNativePlatform() {
		return WINDOWS;
	}

	public static void setPreference(Preference value) {
		preference = value != null ? value : Preference.AUTO;
	}

	public static Preference getPreference() {
		return preference;
	}

	public static boolean isNativeAvailable() {
		if (!isNativePlatform() || nativeDisabled)
			return false;
		try {
			return MediaFoundationVideoDecoder.isSupported();
		} catch (Throwable error) {
			disableNative(error);
			return false;
		}
	}

	public static VideoFrameDecoder open(Fi file) throws Exception {
		if (preference == Preference.AUTO) {
			VideoFrameDecoder nativeDecoder = openNative(file);
			if (nativeDecoder != null)
				return nativeDecoder;
		}
		return openSoftware(file);
	}

	static VideoFrameDecoder openSoftware(Fi file) throws Exception {
		return JCodecFrameDecoder.open(file);
	}

	private static @Nullable VideoFrameDecoder openNative(Fi file) {
		if (!isNativePlatform() || nativeDisabled)
			return null;
		try {
			return MediaFoundationVideoDecoder.tryOpen(file);
		} catch (Throwable error) {
			disableNative(error);
			return null;
		}
	}

	private static void disableNative(Throwable error) {
		nativeDisabled = true;
		Log.warn("SolimVideo: native decoder unavailable, using software decoding ("
				+ error.getClass().getSimpleName() + ": " + error.getMessage() + ")");
	}

	private static Preference readPreference() {
		String value = System.getProperty(PREFERENCE_PROPERTY);
		return value != null && "software".equalsIgnoreCase(value)
				? Preference.SOFTWARE
				: Preference.AUTO;
	}
}

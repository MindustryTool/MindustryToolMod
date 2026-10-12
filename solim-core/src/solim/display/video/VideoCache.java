package solim.display.video;

import arc.files.Fi;
import arc.util.Log;
import arc.util.Nullable;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import mindustry.Vars;

/**
 * Shared disk cache for video assets. Uses the exact same directory and
 * SHA-256 naming scheme as {@code NetworkImage} so cache maintenance applies
 * uniformly; video files carry a {@code .mp4} suffix and pre-decoded audio a
 * {@code .wav} suffix to avoid colliding with extensionless image entries.
 */
public final class VideoCache {

	private VideoCache() {
	}

	static final long CACHE_MAX_AGE = 30L * 24 * 60 * 60 * 1000;

	private static volatile boolean cleanupDone = false;

	/**
	 * Shared Solim disk cache directory:
	 * {@code Vars.dataDirectory/solim/cache/networkImage/}.
	 */
	public static @Nullable Fi cacheDir() {
		try {
			return Vars.dataDirectory.child("solim").child("cache").child("networkImage");
		} catch (Throwable t) {
			return null;
		}
	}

	/** SHA-256 URL hash as lowercase hex. Same scheme as NetworkImage. */
	public static String cacheName(String url) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			byte[] hash = md.digest(url.getBytes(StandardCharsets.UTF_8));
			StringBuilder sb = new StringBuilder(hash.length * 2);
			for (byte b : hash) {
				sb.append(Character.forDigit((b >> 4) & 0xF, 16));
				sb.append(Character.forDigit(b & 0xF, 16));
			}
			return sb.toString();
		} catch (Throwable t) {
			return Integer.toHexString(url.hashCode());
		}
	}

	/** Cached {@code <hash>.mp4} file for a URL, or null when caching is unavailable. */
	public static @Nullable Fi videoFile(String url) {
		Fi dir = cacheDir();
		return dir != null ? dir.child(videoName(url)) : null;
	}

	/** Cached {@code <hash>.wav} file for a URL, or null when caching is unavailable. */
	public static @Nullable Fi audioFile(String url) {
		Fi dir = cacheDir();
		return dir != null ? dir.child(audioName(url)) : null;
	}

	/** File name of the cached video, without the directory. Pure for tests. */
	static String videoName(String url) {
		return cacheName(url) + ".mp4";
	}

	/** File name of the cached audio, without the directory. Pure for tests. */
	static String audioName(String url) {
		return cacheName(url) + ".wav";
	}

	/** True when the file exists, is non-empty, and is younger than the max age. */
	public static boolean isFresh(@Nullable Fi file) {
		try {
			if (file == null || !file.exists() || file.isDirectory() || file.length() == 0)
				return false;
			return System.currentTimeMillis() - file.lastModified() <= CACHE_MAX_AGE;
		} catch (Throwable t) {
			return false;
		}
	}

	/** Persists downloaded video bytes as {@code <hash>.mp4}. Failures are logged, never thrown. */
	public static void writeVideo(String url, byte[] bytes) {
		try {
			Fi dir = cacheDir();
			if (dir == null || bytes == null || bytes.length == 0)
				return;
			dir.mkdirs();
			dir.child(cacheName(url) + ".mp4").writeBytes(bytes);
		} catch (Throwable t) {
			Log.debug("SolimVideo: disk write failed: " + t.getMessage());
		}
	}

	static void deleteQuietly(@Nullable Fi file) {
		try {
			if (file != null) {
				file.delete();
			}
		} catch (Throwable ignored) {
		}
	}

	/** One-shot sweep deleting stale entries (images and video alike) from the shared directory. */
	public static void scheduleCleanup() {
		if (cleanupDone)
			return;
		cleanupDone = true;
		try {
			Thread thread = new Thread(() -> {
				try {
					Fi dir = cacheDir();
					if (dir == null || !dir.exists())
						return;
					Fi[] files = dir.list();
					if (files == null)
						return;
					long now = System.currentTimeMillis();
					for (Fi file : files) {
						if (file != null && !file.isDirectory() && now - file.lastModified() > CACHE_MAX_AGE) {
							file.delete();
						}
					}
				} catch (Throwable ignored) {
				}
			}, "solim-video-cache-cleanup");
			thread.setDaemon(true);
			thread.start();
		} catch (Throwable ignored) {
		}
	}
}

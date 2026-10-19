package solim.display.video;

import arc.files.Fi;
import arc.util.Log;
import arc.util.Nullable;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import mindustry.Vars;

public final class VideoCache {

	private VideoCache() {
	}

	static final long CACHE_MAX_AGE = 30L * 24 * 60 * 60 * 1000;
	static final String AUDIO_CACHE_VERSION = "v2";
	static final int WAV_HEADER_BYTES = 44;
	static final long MIN_VIDEO_BYTES = 16;

	private static volatile boolean cleanupDone = false;

	public static @Nullable Fi cacheDir() {
		try {
			return Vars.dataDirectory.child("solim").child("cache").child("video");
		} catch (Throwable t) {
			return null;
		}
	}

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

	public static @Nullable Fi videoFile(String url) {
		Fi dir = cacheDir();
		return dir != null ? dir.child(videoName(url)) : null;
	}

	public static @Nullable Fi audioFile(String url) {
		Fi dir = cacheDir();
		return dir != null ? dir.child(audioName(url)) : null;
	}

	static String videoName(String url) {
		return cacheName(url) + ".mp4";
	}

	static String audioName(String url) {
		return cacheName(url) + "." + AUDIO_CACHE_VERSION + ".wav";
	}

	public static boolean isFreshVideo(@Nullable Fi file) {
		return isFresh(file, MIN_VIDEO_BYTES);
	}

	public static boolean isFreshAudio(@Nullable Fi file) {
		return isFresh(file, WAV_HEADER_BYTES + 1L) && hasWaveHeader(file);
	}

	/**
	 * Whether the cached file really is a wave file. A cache entry that was written by an
	 * older build, or that was cut short, would keep the audio silent forever, because a
	 * fresh cache is never rewritten.
	 */
	private static boolean hasWaveHeader(@Nullable Fi file) {
		if (file == null) {
			return false;
		}
		try (RandomAccessFile raf = new RandomAccessFile(file.file(), "r")) {
			byte[] head = new byte[12];
			raf.readFully(head);
			return head[0] == 'R' && head[1] == 'I' && head[2] == 'F' && head[3] == 'F'
					&& head[8] == 'W' && head[9] == 'A' && head[10] == 'V' && head[11] == 'E';
		} catch (Throwable t) {
			return false;
		}
	}

	private static boolean isFresh(@Nullable Fi file, long minimumBytes) {
		try {
			if (file == null || !file.exists() || file.isDirectory() || file.length() < minimumBytes)
				return false;
			return System.currentTimeMillis() - file.lastModified() <= CACHE_MAX_AGE;
		} catch (Throwable t) {
			return false;
		}
	}

	public static void writeVideo(String url, byte[] bytes) {
		try {
			Fi dir = cacheDir();
			if (dir == null || bytes == null || bytes.length == 0)
				return;
			dir.mkdirs();
			Fi part = dir.child(videoName(url) + ".part");
			part.writeBytes(bytes);
			Fi target = dir.child(videoName(url));
			target.delete();
			if (!part.file().renameTo(target.file())) {
				part.delete();
				Log.debug("SolimVideo: cache commit failed: " + target.name());
			}
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

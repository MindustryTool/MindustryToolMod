package mindustrytool.services.crash;

import arc.files.Fi;
import arc.struct.Seq;
import arc.util.Log;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.function.Supplier;

/**
 * Single Responsibility: locate the latest crash file on disk. Filters out crashes older than the
 * cutoff date (2026-01-25 UTC).
 */
public final class CrashLocator {

	private static final long CUTOFF_EPOCH =
			LocalDate.of(2026, 1, 25).atStartOfDay(ZoneOffset.UTC).toEpochSecond() * 1000;

	private final Supplier<Fi> crashesDirSupplier;

	public CrashLocator(Supplier<Fi> crashesDirSupplier) {
		this.crashesDirSupplier = crashesDirSupplier;
	}

	/** Finds the latest crash file, or null if none / filtered. */
	public Fi findLatest() {
		Fi dir;
		try {
			dir = crashesDirSupplier.get();
		} catch (Exception e) {
			Log.err("Failed to resolve crashes directory", e);
			return null;
		}

		if (dir == null || !dir.exists()) {
			return null;
		}

		Fi[] files = dir.list();
		if (files == null || files.length == 0) {
			return null;
		}

		Fi latest = Seq.with(files).max(CrashTimestampParser::parse);
		if (latest == null) return null;

		long time = CrashTimestampParser.parse(latest);
		if (time < CUTOFF_EPOCH) {
			return null;
		}

		return latest;
	}

	public static long cutoffEpoch() {
		return CUTOFF_EPOCH;
	}
}

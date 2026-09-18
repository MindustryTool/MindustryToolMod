package mindustrytool.utils;

import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.function.Supplier;

/**
 * Utility for safe, resilient timezone access.
 * Falls back to {@link ZoneOffset#UTC} when JRE timezone data ({@code tzdb.dat}) is missing or corrupted.
 */
public final class TimeZones {

	private TimeZones() {}

	/**
	 * Returns {@link ZoneId#systemDefault()}, falling back to {@link ZoneOffset#UTC} if
	 * the JRE fails to load timezone rules (e.g. missing {@code tzdb.dat} in stripped/portable environments).
	 *
	 * @return host system {@link ZoneId}, or {@link ZoneOffset#UTC} on any failure
	 */
	public static ZoneId systemDefaultOrUtc() {
		return resolveOrFallback(ZoneId::systemDefault, ZoneOffset.UTC);
	}

	static ZoneId resolveOrFallback(Supplier<ZoneId> zoneSupplier, ZoneId fallback) {
		try {
			ZoneId resolved = zoneSupplier.get();
			return resolved != null ? resolved : fallback;
		} catch (Throwable ignored) {
			return fallback;
		}
	}
}

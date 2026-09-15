package mindustrytool.services.crash;

import arc.files.Fi;
import arc.util.Log;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

/**
 * Pure parser for crash file timestamps. Single Responsibility: convert {@link Fi} filename to
 * epoch millis. Thread-safe — uses {@link DateTimeFormatter} instead of {@code SimpleDateFormat}.
 */
public final class CrashTimestampParser {

	private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM_dd_yyyy_HH_mm_ss", Locale.ROOT);

	private CrashTimestampParser() {}

	/**
	 * Parses timestamp from a crash file name. Supports {@code crash-report-MM_dd_yyyy_HH_mm_ss} and
	 * {@code crash_<epochMillis>}.
	 *
	 * @return epoch millis, or 0 if unparsable / null
	 */
	public static long parse(Fi file) {
		if (file == null) return 0;

		String filename = file.nameWithoutExtension();

		if (filename.startsWith("crash-report-")) {
			String time = filename.substring("crash-report-".length());
			try {
				LocalDateTime ldt = LocalDateTime.parse(time, FORMATTER);
				return ldt.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
			} catch (DateTimeParseException e) {
				Log.err("Failed to parse crash-report date: " + time, e);
				return 0;
			}
		}

		if (filename.startsWith("crash_")) {
			String time = filename.substring("crash_".length());
			try {
				return Long.parseLong(time);
			} catch (NumberFormatException e) {
				Log.err("Failed to parse crash epoch: " + time, e);
				return 0;
			}
		}

		return 0;
	}
}

package mindustrytool.services.crash;

import arc.files.Fi;
import arc.util.Log;
import arc.util.Nullable;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class CrashTimestampParser {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("MM_dd_yyyy_HH_mm_ss", Locale.ROOT);

    private CrashTimestampParser() {
    }

    public static long parse(@Nullable Fi file) {
        if (file == null)
            return 0;

        try {
            String filename = file.nameWithoutExtension();

            if (filename.startsWith("crash-report-")) {
                String time = filename.substring("crash-report-".length());
                try {
                    LocalDateTime ldt = LocalDateTime.parse(time, FORMATTER);
                    return ldt.toInstant(ZoneOffset.UTC).toEpochMilli();
                } catch (Throwable e) {
                    Log.err("Failed to parse crash-report date: " + time, e);
                    return 0;
                }
            }

            if (filename.startsWith("crash_")) {
                String time = filename.substring("crash_".length());
                try {
                    return Long.parseLong(time);
                } catch (Throwable e) {
                    Log.err("Failed to parse crash epoch: " + time, e);
                    return 0;
                }
            }

            return 0;
        } catch (Throwable e) {
            Log.err("Unexpected error parsing crash file: " + file, e);
            return 0;
        }
    }
}

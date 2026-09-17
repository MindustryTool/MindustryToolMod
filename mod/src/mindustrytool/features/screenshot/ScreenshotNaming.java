package mindustrytool.features.screenshot;

import arc.files.Fi;
import arc.util.Nullable;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Fixed filename utilities for the screenshot feature.
 * Always produces timestamped png names and resolves collisions
 * with an incremental numeric suffix. Contains no game state
 * so it stays unit testable.
 */
public final class ScreenshotNaming {

    public static final String PREFIX = "screenshot";

    public static final String TIMESTAMP_FORMAT = "yyyy-MM-dd_HH-mm-ss";

    private ScreenshotNaming() {
    }

    public static String formatTimestamp(long millis) {
        return new SimpleDateFormat(TIMESTAMP_FORMAT, Locale.US).format(new Date(millis));
    }

    /**
     * Resolves the fixed filename for the given capture time.
     */
    public static String resolveFilename(long timestampMillis) {
        return PREFIX + "-" + formatTimestamp(timestampMillis) + ".png";
    }

    /**
     * Returns a non-existing target file inside directory by appending
     * an incremental numeric suffix when a collision is detected.
     */
    public static Fi resolveCollision(@Nullable Fi directory, @Nullable String filename) {
        String safeName = filename != null && !filename.trim().isEmpty()
                ? filename.trim()
                : resolveFilename(System.currentTimeMillis());
        if (directory == null) {
            return new Fi(safeName);
        }

        Fi candidate = directory.child(safeName);
        if (!candidate.exists()) {
            return candidate;
        }

        String nameWithoutExt = safeName.toLowerCase(Locale.US).endsWith(".png")
                ? safeName.substring(0, safeName.length() - 4)
                : safeName;

        int index = 1;
        Fi next = directory.child(nameWithoutExt + "_" + index + ".png");
        while (next.exists()) {
            index++;
            next = directory.child(nameWithoutExt + "_" + index + ".png");
        }
        return next;
    }
}

package mindustrytool.plugins.browser;

import java.io.*;

import arc.struct.*;

import arc.util.serialization.Base64Coder;

import mindustry.game.Schematics;
import mindustry.game.Schematic;

/** Schematic reading utilities. */
public final class SchematicUtils {

    private static final ObjectMap<String, Schematic> cache = new ObjectMap<>();

    private SchematicUtils() {
    }

    public static Schematic readSchematic(String data) {
        if (!cache.containsKey(data))
            cache.put(data, readBase64(data));
        return cache.get(data);
    }

    public static Schematic readBase64(String schematic) {
        try {
            return Schematics.read(new ByteArrayInputStream(Base64Coder.decode(schematic.trim())));
        } catch (Exception e) {
            arc.util.Log.err("Error reading schematic", e);
            return null;
        }
    }

    /*
     * Native read is redundant as Vars.schematics.read handles it.
     * This utility solely wraps base64 decoding now.
     */
    public static Schematic read(InputStream input) throws IOException {
        return Schematics.read(input);
    }

    public static String parseRelativeTime(String isoDate) {
        if (isoDate == null)
            return "Unknown";
        try {
            java.time.Instant instant = java.time.Instant.parse(isoDate);
            long diff = java.time.Duration.between(instant, java.time.Instant.now()).toMillis();

            long seconds = diff / 1000;
            if (seconds < 60)
                return "Just now";
            long minutes = seconds / 60;
            if (minutes < 60)
                return minutes + "m ago";
            long hours = minutes / 60;
            if (hours < 24)
                return hours + "h ago";
            long days = hours / 24;
            if (days < 30)
                return days + "d ago";
            long months = days / 30;
            if (months < 12)
                return months + "mo ago";
            return (months / 12) + "y ago";
        } catch (Exception e) {
            return isoDate;
        }
    }
}

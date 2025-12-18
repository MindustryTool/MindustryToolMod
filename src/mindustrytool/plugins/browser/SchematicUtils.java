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
}

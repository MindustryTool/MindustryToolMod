package mindustrytool.plugins.browser;

import java.io.*;
import java.util.zip.InflaterInputStream;
import arc.math.geom.Point2;
import arc.struct.*;
import arc.util.io.Reads;
import arc.util.serialization.Base64Coder;
import mindustry.*;
import mindustry.content.Blocks;
import mindustry.game.Schematic;
import mindustry.game.Schematic.Stile;
import mindustry.io.*;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.legacy.LegacyBlock;
import mindustry.world.blocks.power.LightBlock;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.Unloader;

/** Schematic reading utilities. */
public final class SchematicUtils {
    private static final byte[] HEADER = { 'm', 's', 'c', 'h' };
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
            return Vars.schematics.read(new ByteArrayInputStream(Base64Coder.decode(schematic.trim())));
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
        return Vars.schematics.read(input);
    }
}

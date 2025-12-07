package mindustrytool.domain.service;

import java.io.*;
import java.util.zip.InflaterInputStream;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustrytool.data.cache.SchematicCache;

public class Utils {
    public static Schematic readSchematic(String data) {
        return SchematicCache.get(data, Utils::readBase64);
    }

    private static Schematic readBase64(String schematic) {
        try {
            byte[] bytes = arc.util.serialization.Base64Coder.decode(schematic);
            return Schematics.read(new DataInputStream(new InflaterInputStream(new ByteArrayInputStream(bytes))));
        } catch (Exception e) { arc.util.Log.err("Error reading schematic", e); return null; }
    }
}

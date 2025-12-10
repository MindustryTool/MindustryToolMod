package mindustrytool.service.schematic;

import java.io.*;
import java.util.zip.InflaterInputStream;
import arc.struct.*;
import arc.util.serialization.Base64Coder;
import mindustry.game.Schematic;
import mindustry.world.Block;
import mindustrytool.data.cache.SchematicCache;

/** Schematic reading utilities. */
public final class Utils {
    private static final byte[] HEADER = { 'm', 's', 'c', 'h' };

    private Utils() {}

    public static Schematic readSchematic(String data) {
        return SchematicCache.get(data, Utils::readBase64);
    }

    private static Schematic readBase64(String schematic) {
        try {
            return read(new ByteArrayInputStream(Base64Coder.decode(schematic.trim())));
        } catch (Exception e) { arc.util.Log.err("Error reading schematic", e); return null; }
    }

    private static Schematic read(InputStream input) throws IOException {
        for (byte b : HEADER) if (input.read() != b) throw new IOException("Not a schematic file");
        int ver = input.read();
        try (DataInputStream s = new DataInputStream(new InflaterInputStream(input))) {
            short w = s.readShort(), h = s.readShort();
            if (w > 1028 || h > 1028) throw new IOException("Schematic too large");
            StringMap map = SchematicStreamReader.readTags(s);
            String[] labels = SchematicStreamReader.parseLabels(map);
            IntMap<Block> blocks = SchematicStreamReader.readBlocks(s);
            Schematic out = new Schematic(SchematicStreamReader.readTiles(s, blocks, ver), map, w, h);
            if (labels != null) out.labels.addAll(labels);
            return out;
        }
    }
}

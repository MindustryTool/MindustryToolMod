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

    private SchematicUtils() {}

    public static Schematic readSchematic(String data) {
        if (!cache.containsKey(data)) cache.put(data, readBase64(data));
        return cache.get(data);
    }

    private static Schematic readBase64(String schematic) {
        try {
            return read(new ByteArrayInputStream(Base64Coder.decode(schematic.trim())));
        } catch (Exception e) { 
            arc.util.Log.err("Error reading schematic", e); 
            return null; 
        }
    }

    private static Schematic read(InputStream input) throws IOException {
        for (byte b : HEADER) if (input.read() != b) throw new IOException("Not a schematic file");
        int ver = input.read();
        try (DataInputStream s = new DataInputStream(new InflaterInputStream(input))) {
            short w = s.readShort(), h = s.readShort();
            if (w > 1028 || h > 1028) throw new IOException("Schematic too large");
            StringMap map = readTags(s);
            String[] labels = parseLabels(map);
            IntMap<Block> blocks = readBlocks(s);
            Schematic out = new Schematic(readTiles(s, blocks, ver), map, w, h);
            if (labels != null) out.labels.addAll(labels);
            return out;
        }
    }

    // Stream reading helpers
    private static StringMap readTags(DataInputStream s) throws IOException {
        StringMap map = new StringMap();
        for (int i = 0, n = s.readUnsignedByte(); i < n; i++) map.put(s.readUTF(), s.readUTF());
        return map;
    }

    private static String[] parseLabels(StringMap m) {
        try { 
            return JsonIO.read(String[].class, m.get("labels", "[]")); 
        } catch (Exception e) { 
            return null; 
        }
    }

    private static IntMap<Block> readBlocks(DataInputStream s) throws IOException {
        IntMap<Block> blocks = new IntMap<>();
        for (int i = 0, len = s.readByte(); i < len; i++) {
            String name = s.readUTF();
            Block b = Vars.content.getByName(mindustry.ctype.ContentType.block, SaveFileReader.fallback.get(name, name));
            blocks.put(i, b == null || b instanceof LegacyBlock ? Blocks.air : b);
        }
        return blocks;
    }

    private static Seq<Stile> readTiles(DataInputStream s, IntMap<Block> blks, int ver) throws IOException {
        int total = s.readInt();
        if (total > 16384) throw new IOException("Too many blocks");
        Seq<Stile> tiles = new Seq<>(total);
        for (int i = 0; i < total; i++) {
            Block b = blks.get(s.readByte()); 
            int pos = s.readInt();
            Object cfg = ver == 0 ? mapConfig(b, s.readInt(), pos) : TypeIO.readObject(new Reads(s));
            if (b != Blocks.air) tiles.add(new Stile(b, Point2.x(pos), Point2.y(pos), cfg, s.readByte()));
            else s.readByte();
        }
        return tiles;
    }

    private static Object mapConfig(Block b, int value, int position) {
        if (b instanceof Sorter || b instanceof Unloader || b instanceof ItemSource) return Vars.content.item(value);
        if (b instanceof LiquidSource) return Vars.content.liquid(value);
        if (b instanceof MassDriver || b instanceof ItemBridge) return Point2.unpack(value).sub(Point2.x(position), Point2.y(position));
        if (b instanceof LightBlock) return value;
        return null;
    }
}

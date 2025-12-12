package mindustrytool.service.schematic;
import java.io.*;
import arc.math.geom.Point2;
import arc.struct.*;
import arc.util.io.Reads;
import mindustry.*;
import mindustry.content.Blocks;
import mindustry.ctype.ContentType;
import mindustry.game.Schematic.Stile;
import mindustry.io.*;
import mindustry.world.*;
import mindustry.world.blocks.legacy.LegacyBlock;

/** Stream reading helpers for schematic parsing. */
final class SchematicStreamReader {
    private SchematicStreamReader() {}

    static StringMap readTags(DataInputStream s) throws IOException {
        StringMap map = new StringMap();
        for (int i = 0, n = s.readUnsignedByte(); i < n; i++) map.put(s.readUTF(), s.readUTF());
        return map;
    }

    static String[] parseLabels(StringMap m) {
        try { return JsonIO.read(String[].class, m.get("labels", "[]")); } catch (Exception e) { return null; }
    }

    static IntMap<Block> readBlocks(DataInputStream s) throws IOException {
        IntMap<Block> blocks = new IntMap<>();
        for (int i = 0, len = s.readByte(); i < len; i++) {
            String name = s.readUTF();
            Block b = Vars.content.getByName(ContentType.block, SaveFileReader.fallback.get(name, name));
            blocks.put(i, b == null || b instanceof LegacyBlock ? Blocks.air : b);
        }
        return blocks;
    }

    static Seq<Stile> readTiles(DataInputStream s, IntMap<Block> blks, int ver) throws IOException {
        int total = s.readInt();
        if (total > 16384) throw new IOException("Too many blocks");
        Seq<Stile> tiles = new Seq<>(total);
        for (int i = 0; i < total; i++) {
            Block b = blks.get(s.readByte()); int pos = s.readInt();
            Object cfg = ver == 0 ? BlockConfigMapper.map(b, s.readInt(), pos) : TypeIO.readObject(new Reads(s));
            if (b != Blocks.air) tiles.add(new Stile(b, Point2.x(pos), Point2.y(pos), cfg, s.readByte()));
            else s.readByte();
        }
        return tiles;
    }
}

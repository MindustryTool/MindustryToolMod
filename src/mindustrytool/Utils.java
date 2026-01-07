package mindustrytool;

import java.io.IOException;

import arc.math.geom.*;
import arc.struct.*;
import arc.util.io.*;
import arc.util.serialization.*;
import mindustry.*;
import mindustry.content.*;
import mindustry.ctype.*;
import mindustry.game.Schematic;
import mindustry.game.Schematic.*;
import mindustry.io.*;
import mindustry.world.*;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.legacy.*;
import mindustry.world.blocks.power.*;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.*;

import java.io.*;
import java.util.zip.*;

import static mindustry.Vars.*;

public class Utils {

    public static ObjectMap<String, Schematic> schematicData = new ObjectMap<>();
    private static final byte[] header = { 'm', 's', 'c', 'h' };

    public static synchronized Schematic readSchematic(String data) {
        return schematicData.get(data, () -> readBase64(data));
    }

    public static Schematic readBase64(String schematic) {
        try {
            return read(new ByteArrayInputStream(Base64Coder.decode(schematic.trim())));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static Schematic read(InputStream input) throws IOException {
        for (byte b : header) {
            if (input.read() != b) {
                throw new IOException("Not a schematic file (missing header).");
            }
        }

        int ver = input.read();

        try (DataInputStream stream = new DataInputStream(new InflaterInputStream(input))) {
            short width = stream.readShort(), height = stream.readShort();

            if (width > 1028 || height > 1028)
                throw new IOException("Invalid schematic: Too large (max possible size is 128x128)");

            StringMap map = new StringMap();
            int tags = stream.readUnsignedByte();
            for (int i = 0; i < tags; i++) {
                map.put(stream.readUTF(), stream.readUTF());
            }

            String[] labels = null;

            // try to read the categories, but skip if it fails
            try {
                labels = JsonIO.read(String[].class, map.get("labels", "[]"));
            } catch (Exception ignored) {
            }

            IntMap<Block> blocks = new IntMap<>();
            byte length = stream.readByte();
            for (int i = 0; i < length; i++) {
                String name = stream.readUTF();
                Block block = Vars.content.getByName(ContentType.block, SaveFileReader.fallback.get(name, name));
                blocks.put(i, block == null || block instanceof LegacyBlock ? Blocks.air : block);
            }

            int total = stream.readInt();

            if (total > 128 * 128)
                throw new IOException("Invalid schematic: Too many blocks.");

            Seq<Stile> tiles = new Seq<>(total);
            for (int i = 0; i < total; i++) {
                Block block = blocks.get(stream.readByte());
                int position = stream.readInt();
                Object config = ver == 0 ? mapConfig(block, stream.readInt(), position)
                        : TypeIO.readObject(Reads.get(stream));
                byte rotation = stream.readByte();
                if (block != Blocks.air) {
                    tiles.add(new Stile(block, Point2.x(position), Point2.y(position), config, rotation));
                }
            }

            Schematic out = new Schematic(tiles, map, width, height);
            if (labels != null)
                out.labels.addAll(labels);
            return out;
        }
    }

    private static Object mapConfig(Block block, int value, int position) {
        if (block instanceof Sorter || block instanceof Unloader || block instanceof ItemSource)
            return content.item(value);
        if (block instanceof LiquidSource)
            return content.liquid(value);
        if (block instanceof MassDriver || block instanceof ItemBridge)
            return Point2.unpack(value).sub(Point2.x(position), Point2.y(position));
        if (block instanceof LightBlock)
            return value;

        return null;
    }
}

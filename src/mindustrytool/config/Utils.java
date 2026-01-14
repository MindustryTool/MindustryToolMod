package mindustrytool.config; // Khai báo package chứa các class config/utility

import java.io.IOException; // Import IOException

import arc.math.geom.*; // Import geometry classes (Point2)
import arc.struct.*; // Import các struct classes (ObjectMap, IntMap, Seq, StringMap)
import arc.util.io.*; // Import IO utilities (Reads)
import arc.util.serialization.*; // Import serialization (Base64Coder)

import mindustry.*; // Import Mindustry main classes
import mindustry.content.*; // Import Blocks content
import mindustry.ctype.*; // Import ContentType
import mindustry.game.Schematic; // Import Schematic class
import mindustry.game.Schematic.*; // Import nested classes (Stile)
import mindustry.io.*; // Import IO classes (SaveFileReader, TypeIO, JsonIO)
import mindustry.world.*; // Import world classes (Block)
import mindustry.world.blocks.distribution.*; // Import distribution blocks
import mindustry.world.blocks.legacy.*; // Import legacy blocks
import mindustry.world.blocks.power.*; // Import power blocks
import mindustry.world.blocks.sandbox.*; // Import sandbox blocks
import mindustry.world.blocks.storage.*; // Import storage blocks

import java.io.*; // Import Java IO
import java.util.zip.*; // Import zip utilities (InflaterInputStream)

import static mindustry.Vars.*; // Static import Vars

/**
 * Utility class chứa các helper methods.
 * Chủ yếu để đọc schematic từ base64 string.
 */
public class Utils { // Class Utils

    // Cache schematic đã parse để tránh parse lại
    public static ObjectMap<String, Schematic> schematicData = new ObjectMap<>();
    // Header bytes của schematic file: "msch"
    private static final byte[] header = { 'm', 's', 'c', 'h' };

    /**
     * Đọc schematic từ base64 string.
     * Sử dụng cache để tránh parse lại.
     * @param data Base64 encoded schematic data
     * @return Schematic object
     */
    public static synchronized Schematic readSchematic(String data) {
        // Lấy từ cache hoặc parse mới
        return schematicData.get(data, () -> readBase64(data));
    }

    /**
     * Parse schematic từ base64 string.
     * @param schematic Base64 encoded string
     * @return Schematic object
     */
    public static Schematic readBase64(String schematic) {
        try {
            // Decode base64 và đọc từ byte stream
            return read(new ByteArrayInputStream(Base64Coder.decode(schematic.trim())));
        } catch (IOException e) {
            throw new RuntimeException(e); // Wrap exception
        }
    }

    /**
     * Đọc schematic từ InputStream.
     * Parse binary format của schematic.
     * @param input InputStream chứa schematic data
     * @return Schematic object
     * @throws IOException Nếu có lỗi đọc
     */
    public static Schematic read(InputStream input) throws IOException {
        // Kiểm tra header "msch"
        for (byte b : header) {
            if (input.read() != b) {
                throw new IOException("Not a schematic file (missing header).");
            }
        }

        // Đọc version
        int ver = input.read();

        // Decompress với Inflater và đọc data
        try (DataInputStream stream = new DataInputStream(new InflaterInputStream(input))) {
            // Đọc kích thước
            short width = stream.readShort(), height = stream.readShort();

            // Validate kích thước
            if (width > 1028 || height > 1028)
                throw new IOException("Invalid schematic: Too large (max possible size is 128x128)");

            // Đọc tags metadata
            StringMap map = new StringMap();
            int tags = stream.readUnsignedByte();
            for (int i = 0; i < tags; i++) {
                map.put(stream.readUTF(), stream.readUTF());
            }

            // Đọc labels (categories)
            String[] labels = null;

            // Try to read labels, skip nếu fail
            try {
                labels = JsonIO.read(String[].class, map.get("labels", "[]"));
            } catch (Exception ignored) {
                // Ignore parse errors
            }

            // Đọc block palette
            IntMap<Block> blocks = new IntMap<>();
            byte length = stream.readByte();
            for (int i = 0; i < length; i++) {
                String name = stream.readUTF();
                // Lookup block từ content, sử dụng fallback nếu có
                Block block = Vars.content.getByName(ContentType.block, SaveFileReader.fallback.get(name, name));
                // Nếu không tìm thấy hoặc là legacy block, dùng air
                blocks.put(i, block == null || block instanceof LegacyBlock ? Blocks.air : block);
            }

            // Đọc tổng số tiles
            int total = stream.readInt();

            // Validate số tiles
            if (total > 128 * 128)
                throw new IOException("Invalid schematic: Too many blocks.");

            // Đọc từng tile
            Seq<Stile> tiles = new Seq<>(total);
            for (int i = 0; i < total; i++) {
                Block block = blocks.get(stream.readByte()); // Block từ palette
                int position = stream.readInt(); // Position packed
                // Đọc config tùy theo version
                Object config = ver == 0 ? mapConfig(block, stream.readInt(), position)
                        : TypeIO.readObject(new Reads(stream));
                byte rotation = stream.readByte(); // Rotation
                // Chỉ thêm nếu không phải air
                if (block != Blocks.air) {
                    tiles.add(new Stile(block, Point2.x(position), Point2.y(position), config, rotation));
                }
            }

            // Tạo Schematic object
            Schematic out = new Schematic(tiles, map, width, height);
            // Add labels nếu có
            if (labels != null)
                out.labels.addAll(labels);
            return out; // Trả về schematic
        }
    }

    /**
     * Map config từ integer value cho các block types cũ (version 0).
     * @param block Block type
     * @param value Config value
     * @param position Position của block
     * @return Config object hoặc null
     */
    private static Object mapConfig(Block block, int value, int position) {
        // Sorter, Unloader, ItemSource: config là item
        if (block instanceof Sorter || block instanceof Unloader || block instanceof ItemSource)
            return content.item(value);
        // LiquidSource: config là liquid
        if (block instanceof LiquidSource)
            return content.liquid(value);
        // MassDriver, ItemBridge: config là relative position
        if (block instanceof MassDriver || block instanceof ItemBridge)
            return Point2.unpack(value).sub(Point2.x(position), Point2.y(position));
        // LightBlock: config là color value
        if (block instanceof LightBlock)
            return value;

        return null; // Không có config
    }
}

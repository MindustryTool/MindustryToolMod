package mindustrytool.features.smartdrill.support;

import arc.files.Fi;
import arc.struct.Seq;
import arc.struct.StringMap;
import arc.util.Nullable;
import java.util.HashMap;
import java.util.Map;
import mindustry.Vars;
import mindustry.core.ContentLoader;
import mindustry.game.Schematic;
import mindustry.game.Schematics;
import mindustry.world.Block;
import mindustrytool.features.smartdrill.plan.DrillPlanResult;
import mindustrytool.features.smartdrill.plan.DrillWorldView;
import mindustrytool.features.smartdrill.plan.PlannedBuild;
import mindustrytool.features.smartdrill.plan.TileInfo;

public final class PlanSchematics {

    public static final String OUTPUT_DIR = "build/smartdrill-preview";

    private static final Map<String, String> ORE_BLOCKS = new HashMap<>();
    private static final Map<String, Integer> ORE_COLORS = new HashMap<>();
    private static final Map<String, Block> BLOCKS = new HashMap<>();

    static {
        ORE_BLOCKS.put("copper", "copper-ore");
        ORE_BLOCKS.put("lead", "lead-ore");
        ORE_BLOCKS.put("titanium", "titanium-ore");
        ORE_BLOCKS.put("thorium", "thorium-ore");
        ORE_BLOCKS.put("coal", "coal-ore");
        ORE_BLOCKS.put("scrap", "scrap-ore");
        ORE_BLOCKS.put("sand", "sand-ore");

        ORE_COLORS.put("copper", 0xffd98d5a);
        ORE_COLORS.put("lead", 0xff8c8c8c);
        ORE_COLORS.put("titanium", 0xff8f7fd4);
        ORE_COLORS.put("thorium", 0xffd4574f);
        ORE_COLORS.put("coal", 0xff555a63);
        ORE_COLORS.put("scrap", 0xff8a8f98);
        ORE_COLORS.put("sand", 0xffd9c68a);
    }

    private static final Schematics SCHEMATICS = new Schematics();

    private PlanSchematics() {
    }

    public static void write(TestDrillWorld world, @Nullable DrillPlanResult plan, String name) {
        Fi directory = new Fi(OUTPUT_DIR);
        directory.mkdirs();

        ContentLoader previous = Vars.content;
        boolean installed = Vars.content == null;
        if (installed) {
            Vars.content = new ContentLoader();
            BLOCKS.clear();
        }

        try {
            Schematic floor = new Schematic(floorTiles(world), new StringMap(), world.width(),
                    world.height());
            floor.tags.put("name", name + " floor");
            write(directory, name + "-floor", floor, false);
            FloorImageRenderer.write(world, directory.child(name + "-floor.png"));

            Schematic planned = new Schematic(planTiles(plan), new StringMap(), world.width(),
                    world.height());
            planned.tags.put("name", name + " plan");
            write(directory, name + "-plan", planned, true);
        } finally {
            if (installed) {
                Vars.content = previous;
            }
        }
    }

    private static Seq<Schematic.Stile> floorTiles(DrillWorldView world) {
        Seq<Schematic.Stile> tiles = new Seq<>();
        for (int y = 0; y < world.height(); y++) {
            for (int x = 0; x < world.width(); x++) {
                TileInfo info = world.tile(x, y);
                if (info == null) {
                    continue;
                }
                if (info.floorOre != null) {
                    String ore = info.floorOre;
                    tiles.add(new Schematic.Stile(block(oreBlockName(ore), oreColor(ore)), x, y, null, (byte) 0));
                } else if (info.wallOre != null) {
                    String ore = info.wallOre;
                    tiles.add(new Schematic.Stile(block(ore + "-wall", oreColor(ore)), x, y, null, (byte) 0));
                }
            }
        }
        return tiles;
    }

    private static Seq<Schematic.Stile> planTiles(@Nullable DrillPlanResult plan) {
        Seq<Schematic.Stile> tiles = new Seq<>();
        if (plan == null) {
            return tiles;
        }
        for (int i = 0; i < plan.builds.size; i++) {
            PlannedBuild build = plan.builds.get(i);
            tiles.add(new Schematic.Stile(block(build.block, 0), build.x, build.y, build.config,
                    (byte) build.rotation));
        }
        return tiles;
    }

    private static String oreBlockName(String ore) {
        String mapped = ORE_BLOCKS.get(ore);
        return mapped != null ? mapped : ore + "-ore";
    }

    public static int oreColor(String ore) {
        Integer fixed = ORE_COLORS.get(ore);
        if (fixed != null) {
            return fixed;
        }
        int hash = ore.hashCode();
        int r = 64 + Math.floorMod(hash, 128);
        int g = 64 + Math.floorMod(hash >> 8, 128);
        int b = 64 + Math.floorMod(hash >> 16, 128);
        return 0xff000000 | (r << 16) | (g << 8) | b;
    }

    private static Block block(String name, int mapColor) {
        Block cached = BLOCKS.get(name);
        if (cached != null) {
            return cached;
        }
        Block created = new Block(name);
        if (mapColor != 0) {
            created.mapColor.set(mapColor);
        }
        BLOCKS.put(name, created);
        return created;
    }

    private static void write(Fi directory, String fileName, Schematic schematic, boolean render) {
        try {
            Schematics.write(schematic, directory.child(fileName + ".msch"));
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (!render) {
            return;
        }
        byte[] image = SchematicImageApi.render(SCHEMATICS.writeBase64(schematic));
        if (image != null) {
            directory.child(fileName + ".png").writeBytes(image);
        }
    }
}
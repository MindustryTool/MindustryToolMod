package mindustrytool.features.smartdrill.plan;

import arc.util.Nullable;
import mindustry.Vars;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.type.Item;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.production.BeamDrill;
import mindustry.world.blocks.production.Drill;

public final class MindustryDrillWorldView implements DrillWorldView {

    private final Team team;

    public MindustryDrillWorldView(Team team) {
        this.team = team;
    }

    @Override
    public int width() {
        return Vars.world.width();
    }

    @Override
    public int height() {
        return Vars.world.height();
    }

    @Override
    public @Nullable TileInfo tile(int x, int y) {
        Tile tile = Vars.world.tile(x, y);
        if (tile == null) {
            return null;
        }
        Item floorOre = tile.drop();
        Item wallOre = tile.wallDrop();
        return new TileInfo(floorOre != null ? floorOre.name : null,
                wallOre != null ? wallOre.name : null, tile.solid());
    }

    @Override
    public @Nullable BlockSpec block(@Nullable String name) {
        Block block = resolve(name);
        if (block == null) {
            return null;
        }
        int tier = 0;
        int range = 0;
        if (block instanceof Drill) {
            tier = ((Drill) block).tier;
        } else if (block instanceof BeamDrill) {
            tier = ((BeamDrill) block).tier;
            range = ((BeamDrill) block).range;
        }
        return new BlockSpec(block.name, block.size, block instanceof Drill, block instanceof BeamDrill, tier,
                range, block.placeablePlayer);
    }

    @Override
    public boolean canPlace(@Nullable String blockName, int x, int y, int rotation) {
        Block block = resolve(blockName);
        if (block == null) {
            return false;
        }
        return new BuildPlan(x, y, rotation, block).placeable(team);
    }

    private static @Nullable Block resolve(@Nullable String name) {
        if (name == null || Vars.content == null) {
            return null;
        }
        return Vars.content.block(name);
    }
}
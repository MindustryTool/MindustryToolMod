package mindustrytool.service.schematic;

import arc.math.geom.Point2;
import mindustry.world.Block;
import mindustry.world.blocks.distribution.*;
import mindustry.world.blocks.power.LightBlock;
import mindustry.world.blocks.sandbox.*;
import mindustry.world.blocks.storage.Unloader;
import static mindustry.Vars.content;

/** Maps legacy block configurations to current format. */
public final class BlockConfigMapper {
    private BlockConfigMapper() {}

    public static Object map(Block block, int value, int position) {
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

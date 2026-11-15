package mindustrytool.features.smartdrill.plan;

import arc.util.Nullable;

public final class BlockSpec {

    public final String name;
    public final int size;
    public final int sizeOffset;
    public final boolean drill;
    public final boolean beamDrill;
    public final int tier;
    public final int range;
    public final boolean placeablePlayer;

    public BlockSpec(String name, int size, boolean drill, boolean beamDrill, int tier, int range,
            boolean placeablePlayer) {
        this.name = name;
        this.size = Math.max(1, size);
        this.sizeOffset = -((this.size - 1) / 2);
        this.drill = drill;
        this.beamDrill = beamDrill;
        this.tier = tier;
        this.range = range;
        this.placeablePlayer = placeablePlayer;
    }

    public boolean isDrill() {
        return drill || beamDrill;
    }

    public int lanes() {
        return beamDrill ? size : 0;
    }

    public @Nullable BlockSpec withName(String newName) {
        return newName.equals(name) ? this
                : new BlockSpec(newName, size, drill, beamDrill, tier, range, placeablePlayer);
    }
}
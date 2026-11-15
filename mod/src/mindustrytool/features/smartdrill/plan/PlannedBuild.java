package mindustrytool.features.smartdrill.plan;

import arc.util.Nullable;

public final class PlannedBuild {

    public enum Role {
        DRILL,
        BEAM_DRILL,
        BRIDGE,
        DUCT,
        NODE,
        RIG
    }

    public final String block;
    public final int x;
    public final int y;
    public final int rotation;
    public final @Nullable Object config;
    public final Role role;

    public PlannedBuild(String block, int x, int y, int rotation, @Nullable Object config, Role role) {
        this.block = block;
        this.x = x;
        this.y = y;
        this.rotation = rotation;
        this.config = config;
        this.role = role;
    }

    public PlannedBuild withPosition(int newX, int newY) {
        return new PlannedBuild(block, newX, newY, rotation, config, role);
    }

    public boolean samePos(PlannedBuild other) {
        return other != null && x == other.x && y == other.y;
    }

    @Override
    public String toString() {
        return "PlannedBuild[" + block + " @ " + x + "," + y + " rot " + rotation + " " + role + "]";
    }
}
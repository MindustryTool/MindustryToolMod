package mindustrytool.features.smartdrill.plan;

import arc.struct.Seq;

public final class DrillPlanResult {

    public static final DrillPlanResult EMPTY = new DrillPlanResult(new Seq<PlannedBuild>(), 0, 0, 0, 0);

    public final Seq<PlannedBuild> builds;
    public final int drillCount;
    public final int coveredOre;
    public final int bridgeCount;
    public final int placedBlocks;

    public DrillPlanResult(Seq<PlannedBuild> builds, int drillCount, int coveredOre, int bridgeCount,
            int placedBlocks) {
        this.builds = builds;
        this.drillCount = drillCount;
        this.coveredOre = coveredOre;
        this.bridgeCount = bridgeCount;
        this.placedBlocks = placedBlocks;
    }

    public boolean isEmpty() {
        return builds.isEmpty();
    }

    public float efficiency() {
        return placedBlocks <= 0 ? 0f : (float) coveredOre / (float) placedBlocks;
    }

    public Seq<PlannedBuild> buildsOf(PlannedBuild.Role role) {
        return builds.select(build -> build.role == role);
    }
}
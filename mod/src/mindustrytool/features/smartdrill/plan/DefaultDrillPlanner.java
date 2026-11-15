package mindustrytool.features.smartdrill.plan;

import arc.util.Nullable;

public final class DefaultDrillPlanner implements DrillPlanner {

    @Override
    public DrillPlanResult plan(DrillPlanRequest request, DrillWorldView world) {
        DrillPlanResult result = planDrills(request, world);
        if (result.drillCount <= 0 || !hasFloorDrills(result)) {
            return result;
        }
        return BridgePlanner.attach(result, world, request);
    }

    private static DrillPlanResult planDrills(DrillPlanRequest request, DrillWorldView world) {
        if (request.schematic != null) {
            return SchematicPlanner.plan(request, world);
        }
        BlockSpec drill = world.block(request.drill);
        if (drill != null && drill.beamDrill) {
            return BeamDrillPlanner.plan(request, world);
        }
        return FloorDrillPlanner.plan(request, world);
    }

    private static boolean hasFloorDrills(DrillPlanResult result) {
        for (int i = 0; i < result.builds.size; i++) {
            if (result.builds.get(i).role == PlannedBuild.Role.DRILL) {
                return true;
            }
        }
        return false;
    }

    public static @Nullable DrillPlanRequest withFacing(DrillPlanRequest request, int facing) {
        return DrillPlanRequest.builder()
                .target(request.targetOre, request.targetKind)
                .rect(request.rect)
                .facing(facing)
                .drill(request.drill)
                .schematic(request.schematic)
                .maxFloodTiles(request.maxFloodTiles)
                .maxRectSize(request.maxRectSize)
                .build();
    }
}
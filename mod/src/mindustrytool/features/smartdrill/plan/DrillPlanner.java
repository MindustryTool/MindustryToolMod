package mindustrytool.features.smartdrill.plan;

public interface DrillPlanner {

    DrillPlanResult plan(DrillPlanRequest request, DrillWorldView world);
}
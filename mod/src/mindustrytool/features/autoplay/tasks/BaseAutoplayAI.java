package mindustrytool.features.autoplay.tasks;

import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.util.Nullable;
import mindustry.entities.units.AIController;
import mindustry.gen.Teamc;

/**
 * Base AI controller for autoplay tasks.
 * Tracks target positions for overhead indicator rendering.
 */
public abstract class BaseAutoplayAI extends AIController {

    public final Vec2 targetPos = new Vec2();
    public boolean hasTargetPos = false;

    public void setTarget(@Nullable Teamc target) {
        this.target = target;
    }

    public void clearTargetPos() {
        hasTargetPos = false;
    }

    @Override
    public void updateUnit() {
        if (unit == null) {
            return;
        }
        unit.updateBuilding = true;
        hasTargetPos = false;
        super.updateUnit();
    }

    @Override
    public void updateTargeting() {
        // Targeted by specific task logic
    }

    @Override
    public void moveTo(Position target, float circleLength, float smooth, boolean keepDistance, @Nullable Vec2 offset, boolean arrive) {
        if (unit == null || target == null) {
            return;
        }
        targetPos.set(target.getX(), target.getY());
        hasTargetPos = true;
        super.moveTo(target, circleLength, smooth, keepDistance, offset, arrive);
    }

    @Override
    public void moveTo(Position target, float circleLength, float smooth) {
        if (unit == null || target == null) {
            return;
        }
        targetPos.set(target.getX(), target.getY());
        hasTargetPos = true;
        super.moveTo(target, circleLength, smooth);
    }

    @Override
    public void circle(Position target, float circleLength) {
        if (unit == null || target == null) {
            return;
        }
        targetPos.set(target.getX(), target.getY());
        hasTargetPos = true;
        super.circle(target, circleLength);
    }

    @Override
    public void circle(Position target, float circleLength, float speed) {
        if (unit == null || target == null) {
            return;
        }
        targetPos.set(target.getX(), target.getY());
        hasTargetPos = true;
        super.circle(target, circleLength, speed);
    }
}

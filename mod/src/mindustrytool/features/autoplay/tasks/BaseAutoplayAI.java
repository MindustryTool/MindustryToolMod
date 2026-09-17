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

    public void setTarget(@Nullable Teamc target) {
        this.target = target;
    }

    @Override
    public void updateUnit() {
        if (unit == null) {
            return;
        }
        unit.updateBuilding = true;
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
        super.moveTo(target, circleLength, smooth, keepDistance, offset, arrive);
    }

    @Override
    public void moveTo(Position target, float circleLength, float smooth) {
        if (unit == null || target == null) {
            return;
        }
        targetPos.set(target.getX(), target.getY());
        super.moveTo(target, circleLength, smooth);
    }


    @Override
    public void circle(Position target, float circleLength) {
        if (unit == null || target == null) {
            return;
        }
        targetPos.set(target.getX(), target.getY());
        super.circle(target, circleLength);
    }

    @Override
    public void circle(Position target, float circleLength, float speed) {
        if (unit == null || target == null) {
            return;
        }
        targetPos.set(target.getX(), target.getY());
        super.circle(target, circleLength, speed);
    }
}

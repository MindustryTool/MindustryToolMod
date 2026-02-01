package mindustrytool.features.autoplay.tasks;

import arc.math.geom.Position;
import arc.math.geom.Vec2;
import arc.util.Nullable;
import mindustry.entities.units.AIController;
import mindustry.gen.Teamc;

public abstract class BaseAutoplayAI extends AIController {
    public final Vec2 targetPos = new Vec2();

    public void setTarget(Teamc target) {
        this.target = target;
    }

    @Override
    public void updateUnit() {
        if (unit == null) return;
        unit.updateBuilding = true;
        super.updateUnit();
    }

    @Override
    public void updateTargeting() {
        // Do nothing by default, tasks handle targeting in shouldRun or updateMovement
    }

    @Override
    public void moveTo(Position target, float circleLength, float smooth, boolean keepDistance, @Nullable Vec2 offset, boolean arrive) {
        if (target != null) {
            targetPos.set(target.getX(), target.getY());
        } else {
            targetPos.set(unit.x, unit.y);
        }
        super.moveTo(target, circleLength, smooth, keepDistance, offset, arrive);
    }

    // Override other variants just in case, though they usually call the one above
    @Override
    public void moveTo(Position pos, float offset, float cornerRadius) {
        if (pos != null) {
            targetPos.set(pos.getX(), pos.getY());
        }
        super.moveTo(pos, offset, cornerRadius);
    }
}

package mindustrytool.input;

import arc.Core;
import arc.math.Angles;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.Vars;
import mindustry.gen.Mechc;
import mindustry.gen.Payloadc;
import mindustry.gen.Unit;
import mindustry.input.Binding;
import mindustry.input.DesktopInput;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.freecamera.FreeCameraFeature;
import mindustrytool.features.joystick.JoystickFeature;

/**
 * Unified desktop input handler for MindustryTool.
 * Decouples camera snapping when FreeCameraFeature is active,
 * blends virtual joystick input with WASD when JoystickFeature is active,
 * and falls back to vanilla DesktopInput behavior when features are inactive.
 */
public class ModDesktopInput extends DesktopInput {

    @Override
    public void update() {
        boolean freeCam = FreeCameraFeature.isFreeCam();
        boolean origDetach = Core.settings.getBool("detach-camera", false);
        if (freeCam) {
            Core.settings.put("detach-camera", true);
        }
        try {
            super.update();
        } finally {
            if (freeCam && !origDetach) {
                Core.settings.put("detach-camera", origDetach);
            }
        }
    }

    @Override
    protected void updateMovement(Unit unit) {
        boolean omni = unit.type.omniMovement;

        float speed = unit.speed();
        float xa = Core.input.axis(Binding.moveX);
        float ya = Core.input.axis(Binding.moveY);
        boolean boosted = (unit instanceof Mechc && unit.isFlying());

        JoystickFeature jf = FeatureManager.getFeature(JoystickFeature.class);
        Vec2 joystick = (jf != null && jf.isEnabled()) ? jf.moveVector : Vec2.ZERO;

        if (!joystick.isZero()) {
            movement.set(xa, ya);
            if (movement.len() > 1f) {
                movement.nor();
            }
            movement.add(joystick).limit(1f);
            movement.scl(speed);
        } else if (Core.input.keyDown(Binding.mouseMove)) {
            movement.set(xa, ya).nor().scl(speed);
            float mousePull = 1f / 25f * speed;
            movement.add((Core.input.mouseWorldX() - Vars.player.x) * mousePull, (Core.input.mouseWorldY() - Vars.player.y) * mousePull)
                    .limit(speed);
        } else {
            movement.set(xa, ya).nor().scl(speed);
        }

        float mouseAngle = Angles.mouseAngle(unit.x, unit.y);
        boolean aimCursor = omni && Vars.player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted;

        if (aimCursor) {
            unit.lookAt(mouseAngle);
        } else {
            unit.lookAt(unit.prefRotation());
        }

        unit.movePref(movement);

        unit.aim(Core.input.mouseWorldX(), Core.input.mouseWorldY(), true);
        unit.controlWeapons(true, Vars.player.shooting && !boosted);

        Vars.player.boosting = Core.input.keyDown(Binding.boost);
        Vars.player.mouseX = unit.aimX();
        Vars.player.mouseY = unit.aimY();

        if (unit instanceof Payloadc) {
            if (Core.input.keyTap(Binding.pickupCargo)) {
                tryPickupPayload();
                lastPayloadKeyTapMillis = Time.millis();
            }

            if (Core.input.keyDown(Binding.pickupCargo)
                    && Time.timeSinceMillis(lastPayloadKeyHoldMillis) > 20
                    && Time.timeSinceMillis(lastPayloadKeyTapMillis) > 200) {
                tryPickupPayload();
                lastPayloadKeyHoldMillis = Time.millis();
            }

            if (Core.input.keyTap(Binding.dropCargo)) {
                tryDropPayload();
                lastPayloadKeyTapMillis = Time.millis();
            }

            if (Core.input.keyDown(Binding.dropCargo)
                    && Time.timeSinceMillis(lastPayloadKeyHoldMillis) > 20
                    && Time.timeSinceMillis(lastPayloadKeyTapMillis) > 200) {
                tryDropPayload();
                lastPayloadKeyHoldMillis = Time.millis();
            }
        }
    }
}

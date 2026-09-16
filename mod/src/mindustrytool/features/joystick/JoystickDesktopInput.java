package mindustrytool.features.joystick;

import arc.Core;
import arc.math.Angles;
import arc.math.geom.Vec2;
import arc.util.Time;
import mindustry.gen.Mechc;
import mindustry.gen.Payloadc;
import mindustry.gen.Unit;
import mindustry.input.Binding;
import mindustry.input.DesktopInput;

import static mindustry.Vars.player;

/**
 * Desktop input handler that supports joystick movement alongside keyboard and mouse.
 * While the joystick knob is held, its vector is combined with the WASD axes.
 * All other desktop behaviors stay identical to vanilla.
 */
public class JoystickDesktopInput extends DesktopInput {

    private final JoystickFeature feature;

    public JoystickDesktopInput(JoystickFeature feature) {
        this.feature = feature;
    }

    @Override
    protected void updateMovement(Unit unit) {
        boolean omni = unit.type.omniMovement;

        float speed = unit.speed();
        float xa = Core.input.axis(Binding.moveX);
        float ya = Core.input.axis(Binding.moveY);
        boolean boosted = (unit instanceof Mechc && unit.isFlying());
        Vec2 joystick = feature.moveVector;

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
            movement.add((Core.input.mouseWorldX() - player.x) * mousePull, (Core.input.mouseWorldY() - player.y) * mousePull)
                    .limit(speed);
        } else {
            movement.set(xa, ya).nor().scl(speed);
        }

        float mouseAngle = Angles.mouseAngle(unit.x, unit.y);
        boolean aimCursor = omni && player.shooting && unit.type.hasWeapons() && unit.type.faceTarget && !boosted;

        if (aimCursor) {
            unit.lookAt(mouseAngle);
        } else {
            unit.lookAt(unit.prefRotation());
        }

        unit.movePref(movement);

        unit.aim(Core.input.mouseWorldX(), Core.input.mouseWorldY(), true);
        unit.controlWeapons(true, player.shooting && !boosted);

        player.boosting = Core.input.keyDown(Binding.boost);
        player.mouseX = unit.aimX();
        player.mouseY = unit.aimY();

        //update payload input
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

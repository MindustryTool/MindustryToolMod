package mindustrytool.features.joystick;

import arc.Core;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.util.Tmp;
import arc.util.Time;
import mindustry.entities.EntityCollisions;
import arc.math.geom.Geometry;
import mindustry.entities.Predict;
import mindustry.entities.Units;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.world.blocks.ControlBlock;
import mindustry.gen.Healthc;
import mindustry.gen.Mechc;
import mindustry.gen.BlockUnitUnit;
import mindustry.gen.Payloadc;
import mindustry.gen.Unit;
import mindustry.input.MobileInput;
import mindustry.type.UnitType;
import mindustrytool.features.settings.ModSettings;

import static mindustry.Vars.*;

/**
 * Mobile input handler that drives unit movement purely from the joystick vector.
 * The unit no longer follows the camera center, so panning moves the camera freely.
 * Targeting, shooting, boosting and payload behaviors stay identical to vanilla.
 */
public class JoystickMobileInput extends MobileInput {

    /** World-space offset applied along the joystick direction so the unit always runs at full speed. */
    private static final float JOYSTICK_OFFSET = 80f;

    private final JoystickFeature feature;
    private final Vec2 lastPinchPan = new Vec2();
    private boolean pinchPanning;
    private boolean isPanning;
    private long lastPanTime;

    public JoystickMobileInput(JoystickFeature feature) {
        this.feature = feature;
    }

    public void cancelPanDelay() {
        isPanning = false;
        pinchPanning = false;
        lastPanTime = 0;
    }

    @Override
    public void update() {
        super.update();
        updateCamera();
    }

    private void updateCamera() {
        if (Boolean.TRUE.equals(ModSettings.freeCamera.get())) {
            return;
        }
        if (state == null || !state.isGame() || player == null || player.dead()) {
            return;
        }
        Unit unit = player.unit();
        if (unit == null || unit.dead) {
            return;
        }
        if (!isPanning && !pinchPanning && Time.timeSinceMillis(lastPanTime) > 500) {
            Core.camera.position.lerpDelta(unit, 0.08f);
        }
    }

    @Override
    public boolean pan(float x, float y, float deltaX, float deltaY) {
        isPanning = true;
        lastPanTime = Time.millis();
        return super.pan(x, y, deltaX, deltaY);
    }

    @Override
    public boolean panStop(float x, float y, int pointer, KeyCode button) {
        isPanning = false;
        lastPanTime = Time.millis();
        return super.panStop(x, y, pointer, button);
    }

    @Override
    public boolean zoom(float initialDistance, float distance) {
        if (feature.isKnobHeld()) {
            return false;
        }
        return super.zoom(initialDistance, distance);
    }

    @Override
    public boolean pinch(Vec2 initialPointer1, Vec2 initialPointer2, Vec2 pointer1, Vec2 pointer2) {
        if (feature.isKnobHeld()) {
            Vec2 panPointer = feature.activePointer == 0 ? pointer2 : pointer1;
            if (!pinchPanning) {
                pinchPanning = true;
                lastPinchPan.set(panPointer);
            } else {
                float dx = panPointer.x - lastPinchPan.x;
                float dy = panPointer.y - lastPinchPan.y;
                lastPinchPan.set(panPointer);
                super.pan(panPointer.x, panPointer.y, dx, dy);
            }
            isPanning = true;
            lastPanTime = Time.millis();
            return true;
        }
        return super.pinch(initialPointer1, initialPointer2, pointer1, pointer2);
    }

    @Override
    public void pinchStop() {
        pinchPanning = false;
        isPanning = false;
        lastPanTime = Time.millis();
        super.pinchStop();
    }

    @Override
    protected void updateMovement(Unit unit) {
        Rect rect = Tmp.r3;

        UnitType type = unit.type;
        if (type == null) return;

        boolean omni = unit.type.omniMovement;
        boolean allowHealing = type.canHeal;
        boolean validHealTarget = allowHealing && target instanceof Building && ((Building) target).isValid()
                && target.team() == unit.team && ((Building) target).damaged() && target.within(unit, type.range);
        boolean boosted = (unit instanceof Mechc && unit.isFlying());
        //reset target if:
        // - in the editor, or...
        // - it's both an invalid standard target and an invalid heal target
        if ((Units.invalidateTarget(target, unit, type.range) && !validHealTarget) || state.isEditor()) {
            target = null;
        }

        //decouple from camera: movement target comes from the joystick, not the camera center
        Vec2 vec = feature.moveVector;
        if (!vec.isZero() && !player.dead()) {
            targetPos.set(player).add(Tmp.v2.set(vec).nor().scl(JOYSTICK_OFFSET));
        } else {
            targetPos.set(player);
        }

        float attractDst = 15f;
        float speed = unit.speed();
        float range = unit.hasWeapons() ? unit.range() : 0f;
        float mouseAngle = unit.angleTo(unit.aimX(), unit.aimY());
        boolean aimCursor = omni && player.shooting && type.hasWeapons() && !boosted && type.faceTarget;

        if (aimCursor) {
            unit.lookAt(mouseAngle);
        } else {
            unit.lookAt(unit.prefRotation());
        }

        //validate payload, if it's a destroyed unit/building, remove it
        if (payloadTarget instanceof Healthc && !((Healthc) payloadTarget).isValid()) {
            payloadTarget = null;
        }

        if (payloadTarget != null && unit instanceof Payloadc) {
            Payloadc pay = (Payloadc) unit;
            targetPos.set(payloadTarget);
            attractDst = 0f;

            if (unit.within(payloadTarget, 3f * Time.delta)) {
                if (pay.hasPayload() && (payloadTarget instanceof Vec2
                        || (payloadTarget instanceof Building && ((Building) payloadTarget).team == player.team()
                                && ((Building) payloadTarget).acceptPayload((Building) payloadTarget, pay.payloads().peek())))) {
                    //vec -> dropping something
                    tryDropPayload();
                } else if (payloadTarget instanceof Building && ((Building) payloadTarget).team == unit.team) {
                    //building -> picking building up
                    Call.requestBuildPayload(player, (Building) payloadTarget);
                } else if (payloadTarget instanceof Unit && pay.canPickup((Unit) payloadTarget)) {
                    //unit -> picking unit up
                    Call.requestUnitPayload(player, (Unit) payloadTarget);
                }

                payloadTarget = null;
            }
        } else {
            payloadTarget = null;
        }

        movement.set(targetPos).sub(player).limit(speed);
        movement.setAngle(Mathf.slerp(movement.angle(), unit.vel.angle(), 0.05f));

        if (player.within(targetPos, attractDst)) {
            movement.setZero();
            unit.vel.approachDelta(Vec2.ZERO, unit.speed() * type.accel / 2f);
        }

        unit.hitbox(rect);
        rect.grow(4f);

        player.boosting = collisions.overlapsTile(rect, EntityCollisions::solid) || !unit.within(targetPos, 85f);

        unit.movePref(movement);

        //update shooting if not building + not mining
        if (!unit.activelyBuilding() && unit.mineTile == null && !state.isEditor()) {

            //autofire targeting
            if (manualShooting) {
                player.shooting = !boosted;
                unit.aim(player.mouseX = Core.input.mouseWorldX(), player.mouseY = Core.input.mouseWorldY(), true);
            } else if (target == null) {
                player.shooting = false;
                if (Core.settings.getBool("autotarget") && !isControlledBlockUnit()) {
                    if (unit.type.canAttack) {
                        target = Units.closestTarget(unit.team, unit.x, unit.y, range, u -> u.checkTarget(type.targetAir, type.targetGround),
                                u -> type.targetGround && type.targetBuildingsMobile);
                    }

                    if (allowHealing && target == null) {
                        target = Geometry.findClosest(unit.x, unit.y, indexer.getDamaged(player.team()));
                        if (target != null && !unit.within(target, range)) {
                            target = null;
                        }
                    }
                }

                //when not shooting, aim at mouse cursor
                unit.aim(Core.input.mouseWorldX(), Core.input.mouseWorldY(), true);
            } else {
                Vec2 intercept = player.unit().type.weapons.contains(w -> w.predictTarget) ? Predict.intercept(unit, target, type.weapons.first().bullet)
                        : Tmp.v1.set(target);

                player.mouseX = intercept.x;
                player.mouseY = intercept.y;
                player.shooting = !boosted;

                unit.aim(player.mouseX, player.mouseY, true);
            }
        }

        unit.controlWeapons(player.shooting && !boosted);
    }

    /** Mirrors the vanilla check: true when the player controls a block unit whose block must not auto-target. */
    private static boolean isControlledBlockUnit() {
        Unit unit = player.unit();
        if (unit instanceof BlockUnitUnit) {
            BlockUnitUnit blockUnit = (BlockUnitUnit) unit;
            return blockUnit.tile() instanceof ControlBlock && !((ControlBlock) blockUnit.tile()).shouldAutoTarget();
        }
        return false;
    }
}

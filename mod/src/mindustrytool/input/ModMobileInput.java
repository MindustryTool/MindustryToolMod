package mindustrytool.input;

import arc.Core;
import arc.input.GestureDetector;
import arc.input.GestureDetector.GestureListener;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Geometry;
import arc.math.geom.Rect;
import arc.math.geom.Vec2;
import arc.util.Log;
import arc.util.Time;
import arc.util.Tmp;
import mindustry.Vars;
import mindustry.entities.EntityCollisions;
import mindustry.entities.Predict;
import mindustry.entities.Units;
import mindustry.gen.BlockUnitUnit;
import mindustry.gen.Building;
import mindustry.gen.Call;
import mindustry.gen.Healthc;
import mindustry.gen.Mechc;
import mindustry.gen.Payloadc;
import mindustry.gen.Unit;
import mindustry.input.MobileInput;
import mindustry.input.PlaceMode;
import mindustry.type.UnitType;
import mindustry.world.blocks.ControlBlock;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.autoplay.AutoplayFeature;
import mindustrytool.features.freecamera.FreeCameraFeature;
import mindustrytool.features.joystick.JoystickFeature;

/**
 * Unified mobile input handler for MindustryTool.
 * Drives unit movement purely from virtual joystick vector when JoystickFeature is enabled,
 * falls back to camera-centered movement when disabled, suppresses camera lerp when
 * FreeCameraFeature is active, and intercepts pinch gestures during joystick holding.
 */
public class ModMobileInput extends MobileInput {

    private static final float JOYSTICK_OFFSET = 80f;

    final Vec2 lastPinchPan = new Vec2();
    boolean pinchPanning;
    boolean isPanning;
    long lastPanTime;

    public void cancelPanDelay() {
        isPanning = false;
        pinchPanning = false;
        lastPanTime = 0;
    }

    @Override
    public void add() {
        super.add();
        Core.input.removeProcessor(detector);
        Core.input.removeProcessor(this);
        detector = new GestureDetector(20, 0.5f, 0.3f, 0.15f, new ModGestureListener());
        Core.input.addProcessor(detector);
        Core.input.addProcessor(this);
    }

    @Override
    public void remove() {
        super.remove();
        pinchPanning = false;
        isPanning = false;
    }

    @Override
    public void update() {
        super.update();
        updateCamera();
    }

    void updateCamera() {
        JoystickFeature jf = FeatureManager.getFeature(JoystickFeature.class);
        if (jf == null || !jf.isEnabled() || FreeCameraFeature.isFreeCam()) {
            return;
        }
        if (Vars.state == null || !Vars.state.isGame() || Vars.player == null || Vars.player.dead()) {
            return;
        }
        Unit unit = Vars.player.unit();
        if (unit == null || unit.dead) {
            return;
        }
        if (!isPanning && !pinchPanning && !lineMode && !selecting && mode == PlaceMode.none
                && Time.timeSinceMillis(lastPanTime) > 500) {
            Core.camera.position.lerpDelta(unit, 0.08f);
        }
    }

    @Override
    public boolean tap(float x, float y, int count, KeyCode button) {
        try {
            return super.tap(x, y, count, button);
        } catch (Throwable t) {
            Log.err("Error processing mobile tap in ModMobileInput", t);
            return false;
        }
    }

    @Override
    public boolean longPress(float x, float y) {
        try {
            return super.longPress(x, y);
        } catch (Throwable t) {
            Log.err("Error processing mobile longPress in ModMobileInput", t);
            return false;
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
        JoystickFeature jf = FeatureManager.getFeature(JoystickFeature.class);
        if (jf != null && jf.isEnabled() && jf.isKnobHeld()) {
            return false;
        }
        return super.zoom(initialDistance, distance);
    }

    @Override
    protected void updateMovement(Unit unit) {
        Rect rect = Tmp.r3;

        UnitType type = unit.type;
        if (type == null) {
            return;
        }

        boolean omni = unit.type.omniMovement;
        boolean allowHealing = type.canHeal;
        boolean validHealTarget = allowHealing && target instanceof Building && ((Building) target).isValid()
                && target.team() == unit.team && ((Building) target).damaged() && target.within(unit, type.range);
        boolean boosted = (unit instanceof Mechc && unit.isFlying());

        if ((Units.invalidateTarget(target, unit, type.range) && !validHealTarget) || Vars.state.isEditor()) {
            target = null;
        }

        JoystickFeature jf = FeatureManager.getFeature(JoystickFeature.class);
        boolean joystickActive = jf != null && jf.isEnabled();
        boolean freeCam = FreeCameraFeature.isFreeCam();
        AutoplayFeature af = FeatureManager.getFeature(AutoplayFeature.class);
        boolean autoplaying = af != null && af.isEnabled();

        if (joystickActive) {
            Vec2 vec = jf.moveVector;
            if (!vec.isZero() && !Vars.player.dead()) {
                targetPos.set(Vars.player).add(Tmp.v2.set(vec).nor().scl(JOYSTICK_OFFSET));
            } else {
                targetPos.set(Vars.player);
            }
        } else if (freeCam) {
            targetPos.set(Vars.player);
        } else {
            // Faithful vanilla fallback: unit moves towards camera position on mobile
            targetPos.set(Core.camera.position);
        }

        float attractDst = 15f;
        float speed = unit.speed();
        float range = unit.hasWeapons() ? unit.range() : 0f;
        float mouseAngle = unit.angleTo(unit.aimX(), unit.aimY());
        boolean aimCursor = omni && Vars.player.shooting && type.hasWeapons() && !boosted && type.faceTarget;

        if (!autoplaying) {
            if (aimCursor) {
                unit.lookAt(mouseAngle);
            } else {
                unit.lookAt(unit.prefRotation());
            }
        }

        if (payloadTarget instanceof Healthc && !((Healthc) payloadTarget).isValid()) {
            payloadTarget = null;
        }

        if (payloadTarget != null && unit instanceof Payloadc) {
            Payloadc pay = (Payloadc) unit;
            targetPos.set(payloadTarget);
            attractDst = 0f;

            if (unit.within(payloadTarget, 3f * Time.delta)) {
                if (pay.hasPayload() && (payloadTarget instanceof Vec2
                        || (payloadTarget instanceof Building && ((Building) payloadTarget).team == Vars.player.team()
                                && ((Building) payloadTarget).acceptPayload((Building) payloadTarget, pay.payloads().peek())))) {
                    tryDropPayload();
                } else if (payloadTarget instanceof Building && ((Building) payloadTarget).team == unit.team) {
                    Call.requestBuildPayload(Vars.player, (Building) payloadTarget);
                } else if (payloadTarget instanceof Unit && pay.canPickup((Unit) payloadTarget)) {
                    Call.requestUnitPayload(Vars.player, (Unit) payloadTarget);
                }

                payloadTarget = null;
            }
        } else {
            payloadTarget = null;
        }

        movement.set(targetPos).sub(Vars.player).limit(speed);
        movement.setAngle(Mathf.slerp(movement.angle(), unit.vel.angle(), 0.05f));

        if (Vars.player.within(targetPos, attractDst)) {
            movement.setZero();
            unit.vel.approachDelta(Vec2.ZERO, unit.speed() * type.accel / 2f);
        }

        unit.hitbox(rect);
        rect.grow(4f);

        Vars.player.boosting = (Vars.collisions != null && Vars.collisions.overlapsTile(rect, EntityCollisions::solid))
                || !unit.within(targetPos, 85f);

        if (autoplaying) {
            if (joystickActive && jf.isKnobHeld() && !movement.isZero()) {
                unit.movePref(movement);
            }
            return;
        }

        unit.movePref(movement);

        if (!unit.activelyBuilding() && unit.mineTile == null && !Vars.state.isEditor()) {
            if (manualShooting) {
                Vars.player.shooting = !boosted;
                unit.aim(Vars.player.mouseX = Core.input.mouseWorldX(), Vars.player.mouseY = Core.input.mouseWorldY(), true);
            } else if (target == null) {
                Vars.player.shooting = false;
                if (Core.settings.getBool("autotarget") && !isControlledBlockUnit()) {
                    if (unit.type.canAttack) {
                        target = Units.closestTarget(unit.team, unit.x, unit.y, range, u -> u.checkTarget(type.targetAir, type.targetGround),
                                u -> type.targetGround && type.targetBuildingsMobile);
                    }

                    if (allowHealing && target == null) {
                        target = Geometry.findClosest(unit.x, unit.y, Vars.indexer.getDamaged(Vars.player.team()));
                        if (target != null && !unit.within(target, range)) {
                            target = null;
                        }
                    }
                }

                unit.aim(Core.input.mouseWorldX(), Core.input.mouseWorldY(), true);
            } else {
                Vec2 intercept = Vars.player.unit().type.weapons.contains(w -> w.predictTarget) ? Predict.intercept(unit, target, type.weapons.first().bullet)
                        : Tmp.v1.set(target);

                Vars.player.mouseX = intercept.x;
                Vars.player.mouseY = intercept.y;
                Vars.player.shooting = !boosted;

                unit.aim(Vars.player.mouseX, Vars.player.mouseY, true);
            }
        }

        unit.controlWeapons(Vars.player.shooting && !boosted);
    }

    private static boolean isControlledBlockUnit() {
        Unit unit = Vars.player.unit();
        if (unit instanceof BlockUnitUnit) {
            BlockUnitUnit blockUnit = (BlockUnitUnit) unit;
            return blockUnit.tile() instanceof ControlBlock && !((ControlBlock) blockUnit.tile()).shouldAutoTarget();
        }
        return false;
    }

    private class ModGestureListener implements GestureListener {

        @Override
        public boolean touchDown(float x, float y, int pointer, KeyCode button) {
            return false;
        }

        @Override
        public boolean tap(float x, float y, int count, KeyCode button) {
            return ModMobileInput.this.tap(x, y, count, button);
        }

        @Override
        public boolean longPress(float x, float y) {
            return ModMobileInput.this.longPress(x, y);
        }

        @Override
        public boolean fling(float velocityX, float velocityY, KeyCode button) {
            return false;
        }

        @Override
        public boolean pan(float x, float y, float deltaX, float deltaY) {
            return ModMobileInput.this.pan(x, y, deltaX, deltaY);
        }

        @Override
        public boolean panStop(float x, float y, int pointer, KeyCode button) {
            return ModMobileInput.this.panStop(x, y, pointer, button);
        }

        @Override
        public boolean zoom(float initialDistance, float distance) {
            return ModMobileInput.this.zoom(initialDistance, distance);
        }

        @Override
        public boolean pinch(Vec2 initialPointer1, Vec2 initialPointer2, Vec2 pointer1, Vec2 pointer2) {
            JoystickFeature jf = FeatureManager.getFeature(JoystickFeature.class);
            if (jf != null && jf.isEnabled() && jf.isKnobHeld()) {
                Vec2 panPointer = jf.activePointer == 0 ? pointer2 : pointer1;
                if (!pinchPanning) {
                    pinchPanning = true;
                    lastPinchPan.set(panPointer);
                } else {
                    float dx = panPointer.x - lastPinchPan.x;
                    float dy = panPointer.y - lastPinchPan.y;
                    lastPinchPan.set(panPointer);
                    ModMobileInput.this.pan(panPointer.x, panPointer.y, dx, dy);
                }
                isPanning = true;
                lastPanTime = Time.millis();
                return true;
            }
            return false;
        }

        @Override
        public void pinchStop() {
            pinchPanning = false;
            isPanning = false;
            lastPanTime = Time.millis();
        }
    }
}

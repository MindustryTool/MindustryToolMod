package mindustrytool.features.joystick;

import arc.Core;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.input.KeyCode;
import arc.util.Time;
import mindustry.Vars;
import static solim.UI.unit;

/**
 * Custom on-screen joystick element: a fixed outer base with a movable inner knob.
 * Touch input drives {@link JoystickFeature#moveVector}; double-tapping the knob
 * snaps the camera onto the player unit. Direct Arc element because Solim has no
 * joystick primitive.
 */
public class JoystickWidget extends Element {

    private static final float BASE_SIZE = 28f;
    private static final double TAP_INTERVAL_MILLIS = 300;

    private final JoystickFeature feature;
    private float knobX;
    private float knobY;
    private int activePointer = -1;
    private long lastKnobTapMillis;
    private float lastDiameter = -1f;

    public JoystickWidget(JoystickFeature feature) {
        this.feature = feature;
        touchable = Touchable.enabled;
        name = "joystickWidget";

        addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                float dist = Mathf.dst(x - getWidth() / 2f, y - getHeight() / 2f);
                if (activePointer != -1 || dist > radius()) {
                    return false;
                }

                //double-tap the knob to snap the camera onto the player unit
                long now = Time.millis();
                if (dist <= knobRadius() && now - lastKnobTapMillis < TAP_INTERVAL_MILLIS
                        && Vars.player != null && !Vars.player.dead()) {
                    Core.camera.position.set(Vars.player.x, Vars.player.y);
                }
                lastKnobTapMillis = now;

                activePointer = pointer;
                updateKnob(x, y);
                return true;
            }

            @Override
            public void touchDragged(InputEvent event, float x, float y, int pointer) {
                if (pointer == activePointer) {
                    updateKnob(x, y);
                }
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if (pointer == activePointer) {
                    activePointer = -1;
                    knobX = 0f;
                    knobY = 0f;
                    feature.moveVector.setZero();
                }
            }
        });
    }

    private float diameter() {
        Float size = feature.sizeConfig.get();
        return unit(BASE_SIZE) * (size != null ? size : 1f);
    }

    private float radius() {
        return diameter() / 2f;
    }

    private float knobRadius() {
        return radius() * 0.45f;
    }

    private void updateKnob(float x, float y) {
        float radius = radius();
        float dx = x - getWidth() / 2f;
        float dy = y - getHeight() / 2f;
        float len = Mathf.dst(dx, dy);
        if (len > radius) {
            dx = dx / len * radius;
            dy = dy / len * radius;
        }
        knobX = dx;
        knobY = dy;
        feature.moveVector.set(dx / radius, dy / radius);
    }

    @Override
    public float getPrefWidth() {
        return diameter();
    }

    @Override
    public float getPrefHeight() {
        return diameter();
    }

    @Override
    public void act(float delta) {
        super.act(delta);
        //apply size changes from settings to layout
        float current = diameter();
        if (lastDiameter >= 0f && Math.abs(current - lastDiameter) > 0.5f) {
            invalidateHierarchy();
        }
        lastDiameter = current;
    }

    @Override
    public void draw() {
        Float opacity = feature.opacityConfig.get();
        float alpha = opacity != null ? Mathf.clamp(opacity) : 1f;

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f;
        float radius = radius();

        //outer base
        Draw.color(0f, 0f, 0f, 0.35f * alpha);
        Fill.circle(cx, cy, radius);
        Draw.color(Color.white, 0.5f * alpha);
        Lines.stroke(unit(1f));
        Lines.circle(cx, cy, radius - Lines.getStroke() / 2f);

        //inner knob
        Draw.color(Color.white, 0.85f * alpha);
        Fill.circle(cx + knobX, cy + knobY, knobRadius());

        Draw.reset();
        super.draw();
    }
}

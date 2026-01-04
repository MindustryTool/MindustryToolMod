package mindustrytool.features.gameplay.controls;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.graphics.g2d.Draw;
import arc.graphics.g2d.Fill;
import arc.graphics.g2d.Lines;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.math.geom.Vec2;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.ImageButton;
import arc.scene.ui.Touchpad;
import arc.scene.ui.layout.Table;
import arc.util.Disposable;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import arc.scene.ui.Label;
import arc.util.Align;

public class TouchHandler implements Disposable {
    private boolean enabled;
    private Table uiTable;
    private Touchpad touchpad;

    // HUD Countdown
    private Table hudCountTable;
    private Label hudCountLabel;

    // For D-Pad style
    private boolean dUp, dDown, dLeft, dRight;
    private ImageButton dPadCenterBtn;
    private Vec2 movement = new Vec2();
    // Follow Mode State
    private boolean following = false;

    public TouchHandler() {
        Events.run(EventType.Trigger.update, this::update);
        // Reset state when loading a world or going to menu to prevent "Sticky Lock"
        Events.on(EventType.WorldLoadEvent.class, e -> resetState());
        Events.on(EventType.StateChangeEvent.class, e -> {
            if (e.to == mindustry.core.GameState.State.menu) {
                resetState();
            }
        });
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            rebuild();
        } else {
            if (uiTable != null) {
                uiTable.remove();
            }
        }
    }

    private void resetState() {
        unitLocked = false;
        following = false;
        holdSource = 0;
        lockTriggered = false;
        doubleTapHoldStart = 0;
        movement.setZero();
        if (hudCountTable != null)
            hudCountTable.visible = false;
    }

    public void rebuild() {
        if (!enabled)
            return;
        if (uiTable != null)
            uiTable.remove();

        uiTable = new Table();

        // Custom HUD Indicator for Countdown
        if (hudCountTable != null)
            hudCountTable.remove();
        hudCountTable = new Table();
        hudCountTable.background(Styles.black6);
        hudCountLabel = hudCountTable.add("").style(Styles.outlineLabel).get();
        // Position at top center, similar to Toast
        hudCountTable.setPosition(Core.graphics.getWidth() / 2f, Core.graphics.getHeight() - 150f, Align.center);
        hudCountTable.pack();
        hudCountTable.visible = false;
        Vars.ui.hudGroup.addChild(hudCountTable);

        if (Core.settings.getBool("touch-round", true)) {
            uiTable.bottom().left();
        } else {
            uiTable.bottom().left();
        }

        // Apply settings
        String style = Core.settings.getString("touch-style", "ROUND");
        float sizeScale = Core.settings.getFloat("touch-size-scale", 1.0f);
        float opacity = Core.settings.getFloat("touch-opacity", 0.9f);
        boolean locked = Core.settings.getBool("touch-locked", true);
        float posX = Core.settings.getFloat("touch-x", 20f);
        float posY = Core.settings.getFloat("touch-y", 20f);

        uiTable.color.a = opacity;

        if (style.equals("ROUND")) {
            buildMindustryJoystick(sizeScale);
        } else {
            buildMindustryDPad(sizeScale);
        }

        Vars.ui.hudGroup.addChild(uiTable);
        uiTable.setPosition(posX, posY);
        uiTable.pack();

        // Dragging Logic (Edit Mode)
        if (!locked) {
            uiTable.setBackground(Tex.buttonEdge4);
            uiTable.color.a = 1.0f;
            uiTable.getChildren().each(c -> c.touchable = Touchable.disabled);

            uiTable.addListener(new InputListener() {
                float lastX, lastY;

                @Override
                public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                    lastX = x;
                    lastY = y;
                    return true;
                }

                @Override
                public void touchDragged(InputEvent event, float x, float y, int pointer) {
                    uiTable.moveBy(x - lastX, y - lastY);
                    uiTable.keepInStage();
                    Core.settings.put("touch-x", uiTable.x);
                    Core.settings.put("touch-y", uiTable.y);
                }
            });
        }
    }

    // === MINDUSTRY NATIVE JOYSTICK (PURE CIRCLE MATCHING STYLE) ===
    private void buildMindustryJoystick(float scale) {
        // Remove Tex.buttonEdge4 container (Square Frame) as requested
        // Direct add to uiTable
        MindustryTouchpad pad = new MindustryTouchpad(10f, new Touchpad.TouchpadStyle(), scale);
        touchpad = pad;

        // Double-tap and Hold Listener
        pad.addListener(new InputListener() {
            long lastTapTime = 0;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                long time = Time.millis();

                // Any manual touch disengages follow mode immediately
                following = false;

                // 1. Strict Hit Test: Must tap center knob (Radius ~25f)
                // x, y are local to the Touchpad Actor
                float cx = pad.getWidth() / 2f;
                float cy = pad.getHeight() / 2f;

                // Tightened radius to 25f * scale (Visual knob is 12f)
                if (Mathf.len(x - cx, y - cy) > 25f * scale) {
                    return false; // Ignore double-tap logic if not on knob
                }

                // (Joystick Listener)
                if (time - lastTapTime < 300) {
                    // Double Tap Detected (Center Only)
                    if (Vars.player.unit() != null) {
                        // Check Setting
                        if (Core.settings.getBool("touch-enable-double-tap", true)) {
                            Core.camera.position.set(Vars.player.unit());
                            following = true;
                            pad.flash();

                            // Start Hold Timer (If Enabled)
                            if (Core.settings.getBool("touch-enable-unit-lock", true)) {
                                holdSource = 1; // Joystick Source
                                doubleTapHoldStart = time;
                                lockTriggered = false; // Reset trigger state
                            }
                        }
                    }
                } else {
                    holdSource = 0;
                    lockTriggered = false;
                }
                lastTapTime = time;
                return false; // Let Touchpad handle dragging
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                // Joystick touchUp might not trigger because touchDown returns false
                // But if it does (e.g. pointer logic), clear it.
                // Main cleanup is in update() checking isTouched()
                if (holdSource == 1)
                    holdSource = 0;
                lockTriggered = false;
            }
        });

        pad.getListeners().insert(0, pad.getListeners().pop());

        uiTable.add(pad).size(180f * scale);

        dUp = dDown = dLeft = dRight = false;
    }

    // Mindustry Native Style Touchpad
    private class MindustryTouchpad extends Touchpad {
        private float currentAlpha = 0.5f;
        float flashTime = 0f;

        public MindustryTouchpad(float deadzone, TouchpadStyle style, float scale) {
            super(deadzone, style);
        }

        @Override
        public void draw() {
            float cx = x + width / 2f;
            float cy = y + height / 2f;
            // Radius reduced so knob sticks out 50% at max extension
            // Knob Travel is (width / 2f - 30f), so we match that.
            float radius = width / 2f - 30f;

            // Smooth Fade
            float targetAlpha = isTouched() ? 1.0f : 0.5f;
            currentAlpha = Mathf.lerpDelta(currentAlpha, targetAlpha, 0.1f);

            float globalAlpha = parent.color.a * currentAlpha;

            // Base Circle - Matches Tex.buttonEdge4 background
            Draw.color(Pal.darkestGray, globalAlpha);
            Fill.circle(cx, cy, radius);

            // Border - Matches Tex.buttonEdge4 border (Thick)
            // Accent color if following, otherwise standard border color
            Draw.color(TouchHandler.this.following ? Pal.accent : Pal.gray, globalAlpha);
            Lines.stroke(4f); // Thick border as requested
            Lines.circle(cx, cy, radius);

            // Cardinal Ticks (subtle)
            Draw.color(Color.white, globalAlpha * 0.3f);
            Lines.stroke(1.5f);
            for (int i = 0; i < 4; i++) {
                float angle = i * 90f;
                float tx = cx + Mathf.cosDeg(angle) * radius;
                float ty = cy + Mathf.sinDeg(angle) * radius;
                float tx2 = cx + Mathf.cosDeg(angle) * (radius - 6f);
                float ty2 = cy + Mathf.sinDeg(angle) * (radius - 6f);
                Lines.line(tx, ty, tx2, ty2);
            }

            // Knob
            float knx = x + width / 2f + getKnobPercentX() * (width / 2f - 30f);
            float kny = y + height / 2f + getKnobPercentY() * (height / 2f - 30f);

            Draw.color(Color.white, globalAlpha);
            Fill.circle(knx, kny, 12f);
            Draw.color(Color.gray, globalAlpha);
            Lines.stroke(1.5f);
            Lines.circle(knx, kny, 12f);

            // Flash Effect (Double Tap confirmation)
            if (flashTime > 0) {
                flashTime -= Time.delta;
                Draw.color(Pal.accent, (flashTime / 20f) * globalAlpha);
                Fill.circle(cx, cy, radius);
            }

            Draw.reset();
        }

        public void flash() {
            flashTime = 20f;
        }
    }

    // === MINDUSTRY NATIVE D-PAD (SMALL DRILL STYLE) ===
    private void buildMindustryDPad(float scale) {
        touchpad = null;
        float btnSize = 60f * scale; // Slightly larger for better touch
        float btnPad = 2f; // Gap between buttons

        // No outer container, just direct buttons on uiTable
        uiTable.defaults().size(btnSize).pad(btnPad);

        // Row 1: Up
        uiTable.add();
        createMindustryBtn(uiTable, Icon.up, () -> dUp = true, () -> dUp = false);
        uiTable.add().row();

        // Row 2: Left, Center (X), Right
        createMindustryBtn(uiTable, Icon.left, () -> dLeft = true, () -> dLeft = false);

        // Center "X" Button
        dPadCenterBtn = new ImageButton(Icon.cancel, Styles.clearNonei) {
            float currentAlpha = 0.5f;

            @Override
            public void draw() {
                // (Draw logic same as before)
                float targetAlpha = (isPressed() || isOver()) ? 1.0f : 0.5f;
                currentAlpha = Mathf.lerpDelta(currentAlpha, targetAlpha, 0.1f);
                float globalAlpha = parent.color.a * color.a * currentAlpha;

                Draw.color(Pal.darkestGray, globalAlpha);
                Fill.rect(x + width / 2f, y + height / 2f, width, height);

                Draw.color(isPressed() ? Pal.accent : Pal.gray, globalAlpha);
                Lines.stroke(4f);
                Lines.rect(x, y, width, height);

                if (getImage() != null) {
                    Drawable icon = getImage().getDrawable();
                    float iconSize = 32f;
                    float ix = x + (width - iconSize) / 2f;
                    float iy = y + (height - iconSize) / 2f;
                    Color iconColor = isPressed() ? Pal.accent : Color.white;
                    Draw.color(iconColor, globalAlpha);
                    icon.draw(ix, iy, iconSize, iconSize);
                }
                Draw.reset();
            }
        };
        dPadCenterBtn.getImage().setColor(Color.white);

        dPadCenterBtn.addListener(new InputListener() {
            long lastTapTime = 0;

            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                long time = Time.millis();

                if (time - lastTapTime < 300) {
                    if (Vars.player.unit() != null) {
                        if (Core.settings.getBool("touch-enable-double-tap", true)) {
                            Core.camera.position.set(Vars.player.unit());
                            following = true;

                            // Start Hold Timer
                            if (Core.settings.getBool("touch-enable-unit-lock", true)) {
                                holdSource = 2; // D-Pad Source
                                doubleTapHoldStart = time;
                                lockTriggered = false; // Reset trigger
                            }
                        }
                    }
                } else {
                    holdSource = 0;
                    lockTriggered = false;
                }
                lastTapTime = time;
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                if (holdSource == 2)
                    holdSource = 0;
            }
        });
        uiTable.add(dPadCenterBtn);

        createMindustryBtn(uiTable, Icon.right, () -> dRight = true, () -> dRight = false);
        uiTable.row();

        // Row 3: Down
        uiTable.add();
        createMindustryBtn(uiTable, Icon.down, () -> dDown = true, () -> dDown = false);
        uiTable.add();
    }

    private void createMindustryBtn(Table parent, Drawable icon, Runnable onPress, Runnable onRelease) {
        // Use Styles.clearNonei as base (transparent), we will draw the background
        // manually
        ImageButton btn = new ImageButton(icon, Styles.clearNonei) {
            float currentAlpha = 0.5f;

            @Override
            public void draw() {
                // Fade effect matching Joystick
                float targetAlpha = (isPressed() || isOver()) ? 1.0f : 0.5f;
                currentAlpha = Mathf.lerpDelta(currentAlpha, targetAlpha, 0.1f);

                // Calculate global alpha
                float globalAlpha = parent.color.a * color.a * currentAlpha;

                // Draw Square (Small Drill Style) but with Joystick Colors/Border
                float cx = x;
                float cy = y;
                float w = width;
                float h = height;

                // 1. Base (Darkest Gray)
                Draw.color(Pal.darkestGray, globalAlpha);
                Fill.rect(cx + w / 2f, cy + h / 2f, w, h);

                // 2. Border (Gray, Thick 4px)
                Draw.color(isPressed() ? Pal.accent : Pal.gray, globalAlpha);
                Lines.stroke(4f);
                Lines.rect(cx, cy, w, h);

                // Joystick knobs are drawn manually.

                super.draw();

                // For simplicity, let's just let the icon remain bright or fade it?
                // Joystick knob logic: "Draw.color(Color.white, globalAlpha);"
                // So Joystick knob FADES.

                super.draw();

                // We should make the icon fade too.
                if (getImage() != null) {
                    Color imgColor = getImage().color;
                    imgColor.a = currentAlpha;
                }

                // Reset colors
                Draw.reset();
            }
        };

        // Icon visual logic
        btn.getImage().setColor(Color.white);

        btn.addListener(new InputListener() {
            @Override
            public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                onPress.run();
                btn.getImage().setColor(Pal.accent);
                return true;
            }

            @Override
            public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
                onRelease.run();
                btn.getImage().setColor(Color.white);
            }
        });

        parent.add(btn);
    }

    // Hold Logic Fields
    // 0 = None, 1 = Joystick, 2 = D-Pad
    private int holdSource = 0;
    private long doubleTapHoldStart = 0;
    private boolean unitLocked = false;
    private boolean lockTriggered = false;

    public void update() {
        if (!enabled)
            return;

        if (holdSource == 1 && touchpad != null) {
            float dist = Mathf.len(touchpad.getKnobPercentX(), touchpad.getKnobPercentY());

            // If released OR dragged beyond deadzone (0.1f)
            if (!touchpad.isTouched() || dist > 0.1f) {
                holdSource = 0;
                lockTriggered = false;
            }
        }

        // Safety Check for D-Pad Hold
        if (holdSource == 2 && dPadCenterBtn != null && !dPadCenterBtn.isPressed()) {
            holdSource = 0;
            lockTriggered = false;
        }

        // 5 Second Hold Check
        if (holdSource != 0 && !lockTriggered) {
            long elapsed = Time.timeSinceMillis(doubleTapHoldStart);
            int seconds = (int) (elapsed / 1000);

            if (seconds >= 2 && seconds < 5) {
                // Show HUD Table
                if (hudCountTable != null) {
                    hudCountTable.visible = true;
                    hudCountTable.toFront();

                    boolean targetState = !unitLocked;
                    String actionText = targetState ? "Locking Unit Movement" : "Unlocking Unit Movement";
                    hudCountLabel.setText(actionText + " in " + (5 - seconds) + "s");
                    hudCountTable.pack();

                    // Position at UNIT
                    if (Vars.player.unit() != null) {
                        Vec2 screen = Core.camera.project(Vars.player.unit().x, Vars.player.unit().y);
                        // Shift up slightly
                        hudCountTable.setPosition(screen.x,
                                screen.y + 40f * Core.settings.getFloat("touch-size-scale", 1f),
                                Align.center);
                    } else {
                        // Fallback if no unit
                        hudCountTable.setPosition(Core.graphics.getWidth() / 2f, Core.graphics.getHeight() / 2f,
                                Align.center);
                    }
                }
            } else if (seconds >= 5) {
                // Done
                if (hudCountTable != null)
                    hudCountTable.visible = false;

                unitLocked = !unitLocked;
                lockTriggered = true;
                Vars.ui.hudfrag.showToast(unitLocked ? "Unit Movement Locked" : "Unit Movement Unlocked");
            } else {
                // < 2 seconds
                if (hudCountTable != null)
                    hudCountTable.visible = false;
            }
        } else {
            // Not holding
            if (hudCountTable != null)
                hudCountTable.visible = false;
        }

        // Persistent Unit Lock Enforcement
        if (unitLocked && Vars.player.unit() != null) {
            Vars.player.unit().vel.setZero();
        }

        if (Vars.state.isMenu())
            return;

        if (!Core.settings.getBool("touch-locked", true))
            return;

        movement.setZero();

        if (touchpad != null && touchpad.parent != null) {
            float x = touchpad.getKnobPercentX();
            float y = touchpad.getKnobPercentY();
            movement.set(x, y);
        } else {
            if (dUp)
                movement.y += 1;
            if (dDown)
                movement.y -= 1;
            if (dRight)
                movement.x += 1;
            if (dLeft)
                movement.x -= 1;

            if (movement.len2() > 0)
                movement.nor();
        }

        if (movement.len2() > 0.01f) {
            float sens = Core.settings.getFloat("touch-sensitivity", 1.5f);
            float speed = 6.0f * sens;

            // Break Follow Mode on Pan (Fix for Sticky D-Pad)
            following = false;

            Core.camera.position.add(
                    movement.x * speed * Time.delta,
                    movement.y * speed * Time.delta);

            // If Unit is Locked, maybe we force the unit to stop trying to move?
            // (Assuming user implies unti was moving).
            // But we don't control unit here.

        } else if (following && Vars.player.unit() != null) {
            Core.camera.position.set(Vars.player.unit());
        }
    }

    @Override
    public void dispose() {
        if (uiTable != null) {
            uiTable.remove();
            uiTable = null;
        }
    }
}

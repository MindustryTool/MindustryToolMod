package mindustrytool.features.time;

import java.util.Optional;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.math.Mathf;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.event.Touchable;
import arc.scene.ui.Dialog;
import arc.scene.ui.Image;
import arc.scene.ui.layout.Table;
import arc.util.Log;
import arc.util.Time;
import mindustry.Vars;
import mindustry.game.EventType;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;

public class TimeControlFeature extends Table implements Feature {

    private float selected = 1f;
    private boolean doubleSpeed = false;

    private static final float[] SPEEDS = { 0.125f, 0.5f, 1f, 4f, 16f };

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.time-control")
                .description("@feature.time-control.description")
                .icon(Utils.icons("clock.png"))
                .order(1)
                .build();
    }

    @Override
    public void init() {
        touchable = Touchable.childrenOnly;

        setPosition(100, 100);

        Events.on(EventType.ResizeEvent.class, e -> rebuild());

        Core.app.post(this::rebuild);
    }

    void rebuild() {
        clear();

        Table container = new Table();
        container.background(Styles.black6);
        container.touchable = Touchable.enabled;

        float buttonSize = 48f;
        float margin = 6f;

        container.button(Icon.move, Styles.clearNonei, () -> {
        })
                .size(buttonSize)
                .margin(margin)
                .get()
                .addListener(new InputListener() {
                    float lastX, lastY;

                    @Override
                    public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
                        lastX = x;
                        lastY = y;
                        return true;
                    }

                    @Override
                    public void touchDragged(InputEvent event, float x, float y, int pointer) {
                        try {
                            moveBy(x - lastX, y - lastY);

                            float sw = Core.graphics.getWidth();
                            float sh = Core.graphics.getHeight();

                            x = Mathf.clamp(TimeControlFeature.this.x, 0, sw - 40f);
                            y = Mathf.clamp(TimeControlFeature.this.y, 0, sh - 40f);

                        } catch (Exception e) {
                            Log.err(e);
                        }
                    }
                });

        // separator
        Image sep = new Image(Tex.whiteui);
        sep.setColor(Pal.accent);
        container.add(sep).width(2f).fillY();

        Table content = new Table();

        for (float speed : SPEEDS) {
            Color color = speed == selected ? (doubleSpeed ? Pal.accent : Color.white) : Pal.gray;
            String speedString = "[#" + color.toString() + "]"
                    + String.valueOf((doubleSpeed && speed == selected) ? (speed >= 1 ? speed * 2f : speed / 2) : speed)
                    + "x";

            content.button(speedString, Styles.cleart, () -> {
                if (speed == selected) {
                    if (doubleSpeed) {
                        doubleSpeed = false;
                        applySpeed(speed);
                    } else {
                        doubleSpeed = true;
                        applySpeed(speed >= 1 ? speed * 2f : speed / 2);
                    }
                } else {
                    doubleSpeed = false;
                    applySpeed(speed);
                }
                selected = speed;
                rebuild();
            })
                    .wrapLabel(false)
                    .height(buttonSize)
                    .width(buttonSize * 1.5f)
                    .margin(margin);
        }

        container.add(content);

        add(container);
        pack();
    }

    private void applySpeed(float multipler) {
        Time.setDeltaProvider(() -> Core.graphics.getDeltaTime() * 60 * multipler);

        Log.info("Time speed set to: " + multipler);
    }

    @Override
    public void onEnable() {
        if (Vars.ui != null && Vars.ui.hudGroup != null) {
            remove();

            name = "time-control-hud";
            visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame());

            Core.app.post(() -> Vars.ui.hudGroup.addChild(this));
        }
    }

    @Override
    public void onDisable() {
        remove();
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.empty();
    }
}

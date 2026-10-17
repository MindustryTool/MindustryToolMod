package solim.core;

import arc.Core;
import arc.Events;
import arc.scene.ui.layout.Scl;
import mindustry.game.EventType.Trigger;
import solim.reactive.Signal;

public final class Units {

    public static final Signal<Float> dvw = Signal.of(calcDvw());
    public static final Signal<Float> dvh = Signal.of(calcDvh());

    static {
        Events.run(Trigger.update, () -> update());
    }

    private Units() {
    }

    private static final float BASE_UNIT = 4f;

    public static float unit(float value) {
        return value * BASE_UNIT;
    }

    public static int unit(int value) {
        return value * (int) BASE_UNIT;
    }

    public static void update() {
        dvw.set(calcDvw());
        dvh.set(calcDvh());
    }

    public static float calcDvw() {
        return screenWidth() / 100f;
    }

    public static float calcDvh() {
        return screenHeight() / 100f;
    }

    public static float screenWidth() {
        if (Core.graphics == null)
            return 0f;
        float scl = Scl.scl();
        return scl > 0f ? Core.graphics.getWidth() / scl : Core.graphics.getWidth();
    }

    public static float screenHeight() {
        if (Core.graphics == null)
            return 0f;
        float scl = Scl.scl();
        return scl > 0f ? Core.graphics.getHeight() / scl : Core.graphics.getHeight();
    }

    public static float dvw(float percentage) {
        return dvw.get() * percentage;
    }

    public static float dvh(float percentage) {
        return dvh.get() * percentage;
    }

    public static float width() {
        return screenWidth();
    }

    public static float height() {
        return screenHeight();
    }
}

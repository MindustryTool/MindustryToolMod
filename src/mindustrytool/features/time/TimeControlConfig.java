package mindustrytool.features.time;

import arc.Core;

public class TimeControlConfig {

    private static String getAxisKey(String axis) {
        return "mindustrytool.timecontrol." + axis + (Core.graphics.isPortrait() ? ".portrait" : ".landscape");
    }

    public static float x() {
        return Core.settings.getFloat(getAxisKey("x"), Core.graphics.getWidth() / 2f);
    }

    public static void x(float value) {
        Core.settings.put(getAxisKey("x"), value);
    }

    public static float y() {
        return Core.settings.getFloat(getAxisKey("y"), Core.graphics.getHeight() / 2f);
    }

    public static void y(float value) {
        Core.settings.put(getAxisKey("y"), value);
    }
}

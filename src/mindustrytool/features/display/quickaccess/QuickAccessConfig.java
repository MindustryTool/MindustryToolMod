package mindustrytool.features.display.quickaccess;

import arc.Core;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

public class QuickAccessConfig {
    private static String getAxisKey(String axis) {
        return "mindustrytool.quickaccess." + axis + (Core.graphics.isPortrait() ? ".portrait" : ".landscape");
    }

    public static float x() {
        return Core.settings.getFloat(getAxisKey("x"), 0);
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

    public static float opacity() {
        return Core.settings.getFloat("mindustrytool.quickaccess.opacity", 1f);
    }

    public static void opacity(float value) {
        Core.settings.put("mindustrytool.quickaccess.opacity", value);
    }

    public static float scale() {
        return Core.settings.getFloat("mindustrytool.quickaccess.scale", 1f);
    }

    public static void scale(float value) {
        Core.settings.put("mindustrytool.quickaccess.scale", value);
    }

    public static float width() {
        return Core.settings.getFloat("mindustrytool.quickaccess.width", 1f);
    }

    public static void width(float value) {
        Core.settings.put("mindustrytool.quickaccess.width", value);
    }

    public static int cols() {
        return Core.settings.getInt("mindustrytool.quickaccess.cols", 6);
    }

    public static void cols(int value) {
        Core.settings.put("mindustrytool.quickaccess.cols", value);
    }

    public static boolean isFeatureVisible(String name) {
        String hidden = Core.settings.getString("mindustrytool.quickaccess.hidden", "");
        if (hidden.isEmpty())
            return true;
        for (String s : hidden.split(",")) {
            if (s.equals(name))
                return false;
        }
        return true;
    }

    public static void setFeatureVisible(String name, boolean visible) {
        String hiddenStr = Core.settings.getString("mindustrytool.quickaccess.hidden", "");
        Set<String> hidden = new HashSet<>();
        if (!hiddenStr.isEmpty()) {
            hidden.addAll(Arrays.asList(hiddenStr.split(",")));
        }

        if (visible) {
            hidden.remove(name);
        } else {
            hidden.add(name);
        }

        Core.settings.put("mindustrytool.quickaccess.hidden", String.join(",", hidden));
    }
}

package mindustrytool.features.display.teamresource;

import arc.Core;

public class TeamResourceConfig {
    private static final String PREFIX = "mindustrytool.teamresources.";

    public static float opacity() {
        return Core.settings.getFloat(PREFIX + "opacity", 1f);
    }

    public static void opacity(float value) {
        Core.settings.put(PREFIX + "opacity", value);
    }

    public static float scale() {
        return Core.settings.getFloat(PREFIX + "scale", 1f);
    }

    public static void scale(float value) {
        Core.settings.put(PREFIX + "scale", value);
    }

    public static float overlayWidth() {
        return Core.settings.getFloat(PREFIX + "overlayWidth", 0.3f);
    }

    public static void overlayWidth(float value) {
        Core.settings.put(PREFIX + "overlayWidth", value);
    }

    public static boolean showItems() {
        return Core.settings.getBool(PREFIX + "showItems", true);
    }

    public static void showItems(boolean value) {
        Core.settings.put(PREFIX + "showItems", value);
    }

    public static boolean showUnits() {
        return Core.settings.getBool(PREFIX + "showUnits", false);
    }

    public static void showUnits(boolean value) {
        Core.settings.put(PREFIX + "showUnits", value);
    }

    public static boolean showPower() {
        return Core.settings.getBool(PREFIX + "showPower", true);
    }

    public static void showPower(boolean value) {
        Core.settings.put(PREFIX + "showPower", value);
    }

    public static boolean showStoredPower() {
        return Core.settings.getBool(PREFIX + "showStoredPower", false);
    }

    public static void showStoredPower(boolean value) {
        Core.settings.put(PREFIX + "showStoredPower", value);
    }

    public static boolean hideBackground() {
        return Core.settings.getBool(PREFIX + "hideBackground", false);
    }

    public static void hideBackground(boolean value) {
        Core.settings.put(PREFIX + "hideBackground", value);
    }
}

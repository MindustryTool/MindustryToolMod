package mindustrytool.features.display.teamresource;

import arc.Core;

public class TeamResourceConfig {
    private static final String PREFIX = "mindustrytool.team-resource.";

    public static float x() {
        return Core.settings.getFloat(PREFIX + "x",
                Core.graphics.getWidth() / 2 - Core.graphics.getWidth() * overlayWidth() / 2);
    }

    public static void x(float value) {
        Core.settings.put(PREFIX + "x", value);
    }

    public static float y() {
        return Core.settings.getFloat(PREFIX + "y", Core.graphics.getHeight() / 2f);
    }

    public static void y(float value) {
        Core.settings.put(PREFIX + "y", value);
    }

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
        return Core.settings.getFloat(PREFIX + "overlay-width", 0.3f);
    }

    public static void overlayWidth(float value) {
        Core.settings.put(PREFIX + "overlay-width", value);
    }

    public static boolean showItems() {
        return Core.settings.getBool(PREFIX + "show-items", true);
    }

    public static void showItems(boolean value) {
        Core.settings.put(PREFIX + "show-items", value);
    }

    public static boolean showUnits() {
        return Core.settings.getBool(PREFIX + "show-units", false);
    }

    public static void showUnits(boolean value) {
        Core.settings.put(PREFIX + "show-units", value);
    }

    public static boolean showPower() {
        return Core.settings.getBool(PREFIX + "show-power", true);
    }

    public static void showPower(boolean value) {
        Core.settings.put(PREFIX + "show-power", value);
    }

    public static boolean showStoredPower() {
        return Core.settings.getBool(PREFIX + "show-stored-power", false);
    }

    public static void showStoredPower(boolean value) {
        Core.settings.put(PREFIX + "show-stored-power", value);
    }

    public static boolean hideBackground() {
        return Core.settings.getBool(PREFIX + "hide-background", false);
    }

    public static void hideBackground(boolean value) {
        Core.settings.put(PREFIX + "hide-background", value);
    }

    public static boolean alwaysShowFlowRate() {
        return Core.settings.getBool(PREFIX + "always-show-flow-rate", false);
    }

    public static void alwaysShowFlowRate(boolean value) {
        Core.settings.put(PREFIX + "always-show-flow-rate", value);
    }
}

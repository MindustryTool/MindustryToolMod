package mindustrytool.features.display.healthbar;

import arc.Core;

public class HealthBarConfig {
    public static float zoomThreshold;
    public static float opacity;
    public static float scale;
    public static float width;

    public static void load() {
        zoomThreshold = Core.settings.getFloat("mindustrytool.health-bar.zoom-threshold", 0.5f);
        opacity = Core.settings.getFloat("mindustrytool.health-bar.opacity", 1f);
        scale = Core.settings.getFloat("mindustrytool.health-bar.scale", 1f);
        width = Core.settings.getFloat("mindustrytool.health-bar.width", 1f);
    }

    public static void save() {
        Core.settings.put("mindustrytool.health-bar.zoom-threshold", zoomThreshold);
        Core.settings.put("mindustrytool.health-bar.opacity", opacity);
        Core.settings.put("mindustrytool.health-bar.scale", scale);
        Core.settings.put("mindustrytool.health-bar.width", width);
    }

    public static void reset() {
        zoomThreshold = 0.5f;
        opacity = 1f;
        scale = 1f;
        width = 1f;
        save();
    }
}

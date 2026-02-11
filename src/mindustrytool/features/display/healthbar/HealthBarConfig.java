package mindustrytool.features.display.healthbar;

import arc.Core;

public class HealthBarConfig {
    public static float zoomThreshold;
    public static float opacity;
    public static float scale;
    public static float width;

    public static void load() {
        zoomThreshold = Core.settings.getFloat("mindustrytool.visualizer.healthbar.zoomThreshold", 0.5f);
        opacity = Core.settings.getFloat("mindustrytool.visualizer.healthbar.opacity", 1f);
        scale = Core.settings.getFloat("mindustrytool.visualizer.healthbar.scale", 1f);
        width = Core.settings.getFloat("mindustrytool.visualizer.healthbar.width", 1f);
    }

    public static void save() {
        Core.settings.put("mindustrytool.visualizer.healthbar.zoomThreshold", zoomThreshold);
        Core.settings.put("mindustrytool.visualizer.healthbar.opacity", opacity);
        Core.settings.put("mindustrytool.visualizer.healthbar.scale", scale);
        Core.settings.put("mindustrytool.visualizer.healthbar.width", width);
    }

    public static void reset() {
        zoomThreshold = 0.5f;
        opacity = 1f;
        scale = 1f;
        width = 1f;
        save();
    }
}

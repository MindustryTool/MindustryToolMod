package mindustrytool.features.display.healthbar;

import arc.Core;

public class HealthBarConfig {
    public static float zoomThreshold;
    public static float opacity;

    public static void load() {
        zoomThreshold = Core.settings.getFloat("mindustrytool.visualizer.healthbar.zoomThreshold", 0.5f);
        opacity = Core.settings.getFloat("mindustrytool.visualizer.healthbar.opacity", 1f);
    }

    public static void save() {
        Core.settings.put("mindustrytool.visualizer.healthbar.zoomThreshold", zoomThreshold);
        Core.settings.put("mindustrytool.visualizer.healthbar.opacity", opacity);
    }

    public static void reset() {
        zoomThreshold = 0.5f;
        opacity = 1f;
        save();
    }
}

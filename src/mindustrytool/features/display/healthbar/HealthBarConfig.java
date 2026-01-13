package mindustrytool.features.display.healthbar;

import arc.Core;

public class HealthBarConfig {
    public static float zoomThreshold;

    public static void load() {
        zoomThreshold = Core.settings.getFloat("mindustrytool.visualizer.healthbar.zoomThreshold", 0.5f);
    }

    public static void save() {
        Core.settings.put("mindustrytool.visualizer.healthbar.zoomThreshold", zoomThreshold);
    }

    public static void reset() {
        zoomThreshold = 0.5f;
        save();
    }
}

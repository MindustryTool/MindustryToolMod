package mindustrytool.features.display.progress;

import arc.Core;

public class ProgressConfig {
    public static float opacity;
    public static float scale;
    public static float width;

    public static void load() {
        opacity = Core.settings.getFloat("mindustrytool.visualizer.progress.opacity", 1f);
        scale = Core.settings.getFloat("mindustrytool.visualizer.progress.scale", 1f);
        width = Core.settings.getFloat("mindustrytool.visualizer.progress.width", 1f);
    }

    public static void save() {
        Core.settings.put("mindustrytool.visualizer.progress.opacity", opacity);
        Core.settings.put("mindustrytool.visualizer.progress.scale", scale);
        Core.settings.put("mindustrytool.visualizer.progress.width", width);
    }

    public static void reset() {
        opacity = 1f;
        scale = 1f;
        width = 1f;
        save();
    }
}

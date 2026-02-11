package mindustrytool.features.display.wavepreview;

import arc.Core;

public class WavePreviewConfig {
    public static boolean enabled() {
        return Core.settings.getBool("wavepreview-enabled", true);
    }

    public static void enabled(boolean val) {
        Core.settings.put("wavepreview-enabled", val);
    }

    public static float x() {
        return Core.settings.getFloat("wavepreview-x", 100f);
    }

    public static void x(float val) {
        Core.settings.put("wavepreview-x", val);
    }

    public static float y() {
        return Core.settings.getFloat("wavepreview-y", Core.graphics.getHeight() - 200f);
    }

    public static void y(float val) {
        Core.settings.put("wavepreview-y", val);
    }
    
    public static float scale() {
        return Core.settings.getFloat("wavepreview-scale", 1f);
    }

    public static void scale(float val) {
        Core.settings.put("wavepreview-scale", val);
    }

    public static float opacity() {
        return Core.settings.getFloat("wavepreview-opacity", 1f);
    }

    public static void opacity(float val) {
        Core.settings.put("wavepreview-opacity", val);
    }
}

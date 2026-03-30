package mindustrytool.features.display.wavepreview;

import arc.Core;

public class WavePreviewConfig {
    public static boolean enabled() {
        return Core.settings.getBool("mindustrytool.wave-preview.enabled", true);
    }

    public static void enabled(boolean val) {
        Core.settings.put("mindustrytool.wave-preview.enabled", val);
    }

    public static float x() {
        return Core.settings.getFloat("mindustrytool.wave-preview.x", 100f);
    }

    public static void x(float val) {
        Core.settings.put("mindustrytool.wave-preview.x", val);
    }

    public static float y() {
        return Core.settings.getFloat("mindustrytool.wave-preview.y", Core.graphics.getHeight() - 200f);
    }

    public static void y(float val) {
        Core.settings.put("mindustrytool.wave-preview.y", val);
    }
    
    public static float scale() {
        return Core.settings.getFloat("mindustrytool.wave-preview.scale", 1f);
    }

    public static void scale(float val) {
        Core.settings.put("mindustrytool.wave-preview.scale", val);
    }

    public static float opacity() {
        return Core.settings.getFloat("mindustrytool.wave-preview.opacity", 1f);
    }

    public static void opacity(float val) {
        Core.settings.put("mindustrytool.wave-preview.opacity", val);
    }
}

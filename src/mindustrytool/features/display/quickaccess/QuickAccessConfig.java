package mindustrytool.features.display.quickaccess;

import arc.Core;

public class QuickAccessConfig {
    public float x() {
        return Core.settings.getFloat("mindustrytool.quickaccess.x", 0);
    }

    public void x(float value) {
        Core.settings.put("mindustrytool.quickaccess.x", value);
    }

    public float y() {
        return Core.settings.getFloat("mindustrytool.quickaccess.y", Core.graphics.getHeight() / 2f);
    }

    public void y(float value) {
        Core.settings.put("mindustrytool.quickaccess.y", value);
    }
}

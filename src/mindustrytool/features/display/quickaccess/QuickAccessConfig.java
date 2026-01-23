package mindustrytool.features.display.quickaccess;

import arc.Core;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

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

    public boolean isFeatureVisible(String name) {
        String hidden = Core.settings.getString("mindustrytool.quickaccess.hidden", "");
        if (hidden.isEmpty())
            return true;
        for (String s : hidden.split(",")) {
            if (s.equals(name))
                return false;
        }
        return true;
    }

    public void setFeatureVisible(String name, boolean visible) {
        String hiddenStr = Core.settings.getString("mindustrytool.quickaccess.hidden", "");
        Set<String> hidden = new HashSet<>();
        if (!hiddenStr.isEmpty()) {
            hidden.addAll(Arrays.asList(hiddenStr.split(",")));
        }

        if (visible) {
            hidden.remove(name);
        } else {
            hidden.add(name);
        }

        Core.settings.put("mindustrytool.quickaccess.hidden", String.join(",", hidden));
    }
}

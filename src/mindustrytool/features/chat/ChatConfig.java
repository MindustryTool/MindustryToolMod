package mindustrytool.features.chat;

import arc.Core;

public class ChatConfig {
    public float x() {
        return Core.settings.getFloat("mindustrytool.chat.x", 0);
    }

    public void x(float value) {
        Core.settings.put("mindustrytool.chat.x", value);
    }

    public float y() {
        return Core.settings.getFloat("mindustrytool.chat.y", 0);
    }

    public void y(float value) {
        Core.settings.put("mindustrytool.chat.y", value);
    }

    public boolean collapsed() {
        return Core.settings.getBool("mindustrytool.chat.collapsed", false);
    }

    public void collapsed(boolean value) {
        Core.settings.put("mindustrytool.chat.collapsed", value);
    }
}

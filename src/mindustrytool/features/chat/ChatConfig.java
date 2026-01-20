package mindustrytool.features.chat;

import java.time.Instant;

import arc.Core;

public class ChatConfig {
    public float x(boolean collapsed) {
        String key = collapsed ? "mindustrytool.chat.collapsed.x" : "mindustrytool.chat.expanded.x";
        return Core.settings.getFloat(key, Core.settings.getFloat("mindustrytool.chat.x", 0));
    }

    public void x(boolean collapsed, float value) {
        String key = collapsed ? "mindustrytool.chat.collapsed.x" : "mindustrytool.chat.expanded.x";
        Core.settings.put(key, value);
    }

    public float y(boolean collapsed) {
        String key = collapsed ? "mindustrytool.chat.collapsed.y" : "mindustrytool.chat.expanded.y";
        return Core.settings.getFloat(key, Core.settings.getFloat("mindustrytool.chat.y", 0));
    }

    public void y(boolean collapsed, float value) {
        String key = collapsed ? "mindustrytool.chat.collapsed.y" : "mindustrytool.chat.expanded.y";
        Core.settings.put(key, value);
    }

    // Deprecated or Legacy support if needed, but we will update usages.
    public float x() {
        return x(collapsed());
    }

    public void x(float value) {
        x(collapsed(), value);
    }

    public float y() {
        return y(collapsed());
    }

    public void y(float value) {
        y(collapsed(), value);
    }

    public boolean collapsed() {
        return Core.settings.getBool("mindustrytool.chat.collapsed", false);
    }

    public void collapsed(boolean value) {
        Core.settings.put("mindustrytool.chat.collapsed", value);
    }

    public Instant lastRead() {
        return Instant.ofEpochMilli(Core.settings.getLong("mindustrytool.chat.lastRead", 0));
    }

    public void lastRead(Instant value) {
        Core.settings.put("mindustrytool.chat.lastRead", value.toEpochMilli());
    }
}

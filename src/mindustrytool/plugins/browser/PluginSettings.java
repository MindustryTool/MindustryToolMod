package mindustrytool.plugins.browser;

import arc.Core;

/**
 * Utility class for plugin settings with auto-persistence.
 * Settings are stored with prefix "plugin.{pluginName}.{key}".
 */
public class PluginSettings {
    private final String prefix;

    public PluginSettings(String pluginName) {
        this.prefix = "plugin." + pluginName + ".";
    }

    public int getInt(String key, int defaultValue) {
        return Core.settings.getInt(prefix + key, defaultValue);
    }

    public void setInt(String key, int value) {
        Core.settings.put(prefix + key, value);
    }

    public float getFloat(String key, float defaultValue) {
        return Core.settings.getFloat(prefix + key, defaultValue);
    }

    public void setFloat(String key, float value) {
        Core.settings.put(prefix + key, value);
    }

    public boolean getBool(String key, boolean defaultValue) {
        return Core.settings.getBool(prefix + key, defaultValue);
    }

    public void setBool(String key, boolean value) {
        Core.settings.put(prefix + key, value);
    }

    public String getString(String key, String defaultValue) {
        return Core.settings.getString(prefix + key, defaultValue);
    }

    public void setString(String key, String value) {
        Core.settings.put(prefix + key, value);
    }

    /** Reset a setting to default by removing it. */
    public void remove(String key) {
        Core.settings.remove(prefix + key);
    }
}

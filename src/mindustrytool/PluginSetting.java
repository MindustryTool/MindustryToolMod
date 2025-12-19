package mindustrytool;

import arc.scene.style.Drawable;

/**
 * Lightweight representation of a plugin-provided settings entry.
 */
public class PluginSetting {
    public final String id; // unique id
    public final String label;
    public final Drawable icon; // may be null
    public final Runnable action;

    public PluginSetting(String id, String label, Drawable icon, Runnable action) {
        this.id = id;
        this.label = label;
        this.icon = icon;
        this.action = action;
    }
}

package mindustrytool.plugins.browser;

import arc.input.KeyBind;
import arc.input.KeyCode;

/**
 * Custom keybinds for MindustryTool mod.
 * These will automatically appear in Settings → Controls under "MindustryTool"
 * category.
 */
public class ModKeybinds {
    public static KeyBind mapBrowser = KeyBind.add("mapBrowser", KeyCode.m, "MindustryTool"),
            schematicBrowser = KeyBind.add("schematicBrowser", KeyCode.b, "MindustryTool");

    /** Dummy method to trigger class loading and keybind registration */
    public static void init() {
        // Static initializer automatically registers keybinds
    }
}

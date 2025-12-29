package mindustrytool.plugins.browser;

import arc.input.KeyBind;
import arc.input.KeyCode;

/**
 * Custom keybinds for MindustryTool mod.
 * These will automatically appear in Settings → Controls under "MindustryTool"
 * category.
 */
public class ModKeybinds {
    public static KeyBind mapBrowser = KeyBind.add("mapBrowser", KeyCode.unset, "MindustryTool"),
            schematicBrowser = KeyBind.add("schematicBrowser", KeyCode.unset, "MindustryTool"),
            manageComponents = KeyBind.add("manageComponents", KeyCode.f10, "MindustryTool"),
            voiceChatSettings = KeyBind.add("voiceChatSettings", KeyCode.unset, "MindustryTool"),
            smartDrillToggle = KeyBind.add("smartDrillToggle", KeyCode.unset, "MindustryTool");

    /** Dummy method to trigger class loading and keybind registration */
    public static void init() {
        // Static initializer automatically registers keybinds
    }
}

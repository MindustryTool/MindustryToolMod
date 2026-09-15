package mindustrytool.features.browser.common;

import arc.Core;
import arc.input.KeyBind;
import arc.input.KeyCode;

/**
 * Shared keybind registrations for the schematic and map browsers. Binds are
 * unbound by default and rebindable from Mindustry's keybind settings. Features
 * poll these binds from their own update loops, only when enabled and no text
 * field is focused.
 */
public final class BrowserKeybinds {

    public static final KeyBind schematicBrowser = KeyBind.add("schematicBrowser", KeyCode.unset, "MindustryTool");
    public static final KeyBind mapBrowser = KeyBind.add("mapBrowser", KeyCode.unset, "MindustryTool");

    private BrowserKeybinds() {
    }

    /**
     * Returns true when it is safe to trigger a browser hotkey: no text field
     * holds keyboard focus and the chat input is not shown.
     */
    public static boolean noInputFocused() {
        return !Core.scene.hasField();
    }
}

package mindustrytool.plugins.browser;

import arc.Core;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;

/**
 * Handles global keyboard shortcuts for browser dialogs.
 * Supports modifier keys (Ctrl, Shift, Alt) combined with regular keys.
 */
public class KeybindHandler extends InputListener {
    private PluginSettings mapSettings;
    private PluginSettings schematicSettings;

    // Setting keys
    private static final String SHORTCUT_KEY = "shortcutKey";
    private static final String SHORTCUT_CTRL = "shortcutCtrl";
    private static final String SHORTCUT_SHIFT = "shortcutShift";
    private static final String SHORTCUT_ALT = "shortcutAlt";

    public KeybindHandler() {
        this.mapSettings = new PluginSettings("browser.map");
        this.schematicSettings = new PluginSettings("browser.schematic");
    }

    /**
     * Reload settings (called when shortcuts are changed)
     */
    public void reload() {
        this.mapSettings = new PluginSettings("browser.map");
        this.schematicSettings = new PluginSettings("browser.schematic");
    }

    @Override
    public boolean keyDown(InputEvent event, KeyCode keycode) {
        // Check Map Browser shortcut
        if (matchesShortcut(keycode, mapSettings)) {
            var dialog = BrowserPlugin.getMapDialog().getIfEnabled();
            if (dialog != null) {
                dialog.show();
                return true;
            }
        }

        // Check Schematic Browser shortcut
        if (matchesShortcut(keycode, schematicSettings)) {
            var dialog = BrowserPlugin.getSchematicDialog().getIfEnabled();
            if (dialog != null) {
                dialog.show();
                return true;
            }
        }

        return false;
    }

    /**
     * Check if current key press matches the configured shortcut.
     */
    private boolean matchesShortcut(KeyCode keycode, PluginSettings settings) {
        // Get configured shortcut (or use default)
        String configuredKey = settings.getString(SHORTCUT_KEY, null);
        if (configuredKey == null)
            return false;

        boolean requireCtrl = settings.getBool(SHORTCUT_CTRL, false);
        boolean requireShift = settings.getBool(SHORTCUT_SHIFT, false);
        boolean requireAlt = settings.getBool(SHORTCUT_ALT, false);

        // Check if key matches
        if (!keycode.value.equals(configuredKey))
            return false;

        // Check modifiers
        boolean ctrlPressed = Core.input.ctrl();
        boolean shiftPressed = Core.input.shift();
        boolean altPressed = Core.input.alt();

        return (requireCtrl == ctrlPressed) &&
                (requireShift == shiftPressed) &&
                (requireAlt == altPressed);
    }

    /**
     * Get the formatted shortcut string for display.
     */
    public static String getShortcutDisplay(ContentType type) {
        PluginSettings settings = new PluginSettings("browser." + type.name().toLowerCase());
        String key = settings.getString(SHORTCUT_KEY, null);

        if (key == null) {
            // Return default shortcuts
            return type == ContentType.MAP ? "Ctrl+M" : "Ctrl+B";
        }

        StringBuilder sb = new StringBuilder();
        if (settings.getBool(SHORTCUT_CTRL, false))
            sb.append("Ctrl+");
        if (settings.getBool(SHORTCUT_SHIFT, false))
            sb.append("Shift+");
        if (settings.getBool(SHORTCUT_ALT, false))
            sb.append("Alt+");
        sb.append(key);

        return sb.toString();
    }

    /**
     * Save a new shortcut configuration.
     */
    public static void saveShortcut(ContentType type, KeyCode keycode, boolean ctrl, boolean shift, boolean alt) {
        PluginSettings settings = new PluginSettings("browser." + type.name().toLowerCase());
        settings.setString(SHORTCUT_KEY, keycode.value);
        settings.setBool(SHORTCUT_CTRL, ctrl);
        settings.setBool(SHORTCUT_SHIFT, shift);
        settings.setBool(SHORTCUT_ALT, alt);
    }

    /**
     * Set default shortcuts for both dialogs.
     */
    public static void setDefaults() {
        // Map Browser: Ctrl+M
        saveShortcut(ContentType.MAP, KeyCode.m, true, false, false);

        // Schematic Browser: Ctrl+B
        saveShortcut(ContentType.SCHEMATIC, KeyCode.b, true, false, false);
    }

    /**
     * Check if shortcuts are configured, if not, set defaults.
     */
    public static void ensureDefaults() {
        PluginSettings mapSettings = new PluginSettings("browser.map");
        PluginSettings schematicSettings = new PluginSettings("browser.schematic");

        if (mapSettings.getString(SHORTCUT_KEY, null) == null) {
            saveShortcut(ContentType.MAP, KeyCode.m, true, false, false);
        }

        if (schematicSettings.getString(SHORTCUT_KEY, null) == null) {
            saveShortcut(ContentType.SCHEMATIC, KeyCode.b, true, false, false);
        }
    }
}

package mindustrytool.plugins.browser;

import arc.scene.ui.layout.Table;
import mindustry.ui.Styles;

/**
 * Helper class for adding keybind settings to game settings menu.
 * Creates a row with label and button without extending Setting class.
 */
public class SettingKeyBind {
    private final String name;
    private final ContentType contentType;
    private final Runnable onChange;
    private static KeyCaptureDialog keyCaptureDialog = new KeyCaptureDialog();

    public SettingKeyBind(String name, ContentType contentType, Runnable onChange) {
        this.name = name;
        this.contentType = contentType;
        this.onChange = onChange;
    }

    /**
     * Add this keybind setting to a settings table.
     */
    public void addTo(Table table) {
        table.table(row -> {
            row.left();

            // Label showing setting name
            row.add(name).left().growX().padLeft(6);

            // Button showing current shortcut
            row.button(KeybindHandler.getShortcutDisplay(contentType), Styles.defaultt, () -> {
                // Show key capture dialog
                keyCaptureDialog.show(data -> {
                    // Save the new shortcut
                    KeybindHandler.saveShortcut(contentType, data.keycode, data.ctrl, data.shift, data.alt);

                    // Trigger reload callback
                    if (onChange != null) {
                        onChange.run();
                    }
                });
            }).width(150).padRight(6).right();
        }).left().fillX().padTop(4).row();
    }
}

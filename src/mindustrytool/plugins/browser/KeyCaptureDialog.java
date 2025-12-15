package mindustrytool.plugins.browser;

import arc.Core;
import arc.func.Cons;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.event.InputEvent;
import arc.scene.event.InputListener;
import arc.scene.ui.Label;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Dialog that captures keyboard input to configure a shortcut.
 * Displays "Press any key combination..." and records the key press with
 * modifiers.
 */
public class KeyCaptureDialog extends BaseDialog {
    private Label instructionLabel;
    private Cons<ShortcutData> onCapture;
    private InputListener currentListener;

    public KeyCaptureDialog() {
        super(Core.bundle.get("settings.shortcut.capture", "Capture Shortcut"));

        instructionLabel = new Label(Core.bundle.get("settings.shortcut.press", "Press any key combination..."));
        instructionLabel.setColor(Color.yellow);

        cont.add(instructionLabel).pad(40).row();
        cont.add(Core.bundle.get("settings.shortcut.hint", "(Use Ctrl, Shift, Alt + any key)"))
                .color(Color.lightGray)
                .pad(10);

        buttons.button("@cancel", Icon.cancel, this::hide).size(210, 64);
    }

    /**
     * Show the dialog and call the callback when a key is captured.
     */
    public void show(Cons<ShortcutData> onCapture) {
        this.onCapture = onCapture;
        show();

        // Add input listener to capture keys
        currentListener = new InputListener() {
            @Override
            public boolean keyDown(InputEvent event, KeyCode keycode) {
                // Ignore modifier keys alone
                if (keycode == KeyCode.shiftLeft || keycode == KeyCode.shiftRight ||
                        keycode == KeyCode.controlLeft || keycode == KeyCode.controlRight ||
                        keycode == KeyCode.altLeft || keycode == KeyCode.altRight) {
                    return false;
                }

                // Ignore escape (let it close the dialog normally)
                if (keycode == KeyCode.escape) {
                    return false;
                }

                // Capture the key with modifiers
                boolean ctrl = Core.input.ctrl();
                boolean shift = Core.input.shift();
                boolean alt = Core.input.alt();

                ShortcutData data = new ShortcutData(keycode, ctrl, shift, alt);

                if (KeyCaptureDialog.this.onCapture != null) {
                    KeyCaptureDialog.this.onCapture.get(data);
                }

                hide();
                return true;
            }
        };

        Core.scene.addListener(currentListener);
    }

    @Override
    public void hide() {
        super.hide();
        // Remove the listener when hiding
        if (currentListener != null) {
            Core.scene.removeListener(currentListener);
            currentListener = null;
        }
    }

    /**
     * Data class to hold captured shortcut information.
     */
    public static class ShortcutData {
        public final KeyCode keycode;
        public final boolean ctrl;
        public final boolean shift;
        public final boolean alt;

        public ShortcutData(KeyCode keycode, boolean ctrl, boolean shift, boolean alt) {
            this.keycode = keycode;
            this.ctrl = ctrl;
            this.shift = shift;
            this.alt = alt;
        }

        public String getDisplayString() {
            StringBuilder sb = new StringBuilder();
            if (ctrl)
                sb.append("Ctrl+");
            if (shift)
                sb.append("Shift+");
            if (alt)
                sb.append("Alt+");
            sb.append(keycode.value);
            return sb.toString();
        }
    }
}

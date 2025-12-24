package mindustrytool.plugins.voicechat;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Table;
import mindustry.ui.dialogs.BaseDialog;

/**
 * Voice chat settings dialog.
 * Provides controls for mute, volume, denoise, etc.
 */
public class VoiceChatSettingsDialog extends BaseDialog {

    private final VoiceChatManager manager;

    public VoiceChatSettingsDialog(VoiceChatManager manager) {
        super("@voicechat.settings");
        this.manager = manager;

        addCloseButton();
        setup();
    }

    private void setup() {
        cont.clear();
        cont.defaults().width(400f).pad(5f);

        // Enable toggle
        cont.table(t -> {
            t.left();
            t.add("Enable Voice Chat").color(Color.lightGray).left();
            t.button(manager.isEnabled() ? "[green]ON" : "[red]OFF", () -> {
                manager.setEnabled(!manager.isEnabled());
                setup();
            }).width(80f).right();
        }).row();

        // Mute toggle
        cont.table(t -> {
            t.left();
            t.add("Mute Microphone").color(Color.lightGray).left();
            t.button(manager.isMuted() ? "[red]MUTED" : "[green]ACTIVE", () -> {
                manager.toggleMute();
                setup();
            }).width(80f).right();
        }).row();

        // Volume slider
        cont.add("Volume").left().row();
        cont.slider(0f, 100f, 1f, 80f, val -> {
            // Volume will be applied when we have speaker reference
        }).width(400f).row();

        cont.add("").height(20f).row();

        // Status info
        cont.table(t -> {
            t.left();
            t.add("[gray]Status: ").left();
            if (!manager.isEnabled()) {
                t.add("[yellow]Disabled").left();
            } else if (manager.isMuted()) {
                t.add("[red]Muted").left();
            } else {
                t.add("[green]Ready").left();
            }
        }).row();

        cont.add("[lightgray]Voice chat requires a server with LemmeSay mod.").wrap().width(380f).center().row();
    }
}

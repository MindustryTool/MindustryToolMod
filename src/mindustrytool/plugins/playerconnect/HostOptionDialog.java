package mindustrytool.plugins.playerconnect;

import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.ui.dialogs.BaseDialog;

public class HostOptionDialog extends BaseDialog {

    private final BaseDialog createRoomDialog;

    public HostOptionDialog(BaseDialog createRoomDialog) {
        super("@message.manage-room.host-title");
        this.createRoomDialog = createRoomDialog;

        addCloseButton();

        // Rebuild content each time dialog is shown
        shown(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.defaults().size(220f, 60f).pad(10f);

        // Debug logging
        arc.util.Log
                .info("[HostOptionDialog] isGame=" + Vars.state.isGame() + ", isCampaign=" + Vars.state.isCampaign());

        // Host button - always shown
        cont.button("@server.host", Icon.host, () -> {
            hide();
            Vars.ui.host.show();
        }).row();

        // Check if we're in a game and determine game mode
        if (Vars.state.isGame()) {
            if (Vars.state.isCampaign()) {
                // Campaign mode: show Planet Map button (existing behavior)
                cont.button("@message.manage-room.title", Icon.planet, () -> {
                    hide();
                    createRoomDialog.show();
                }).row();
            } else {
                // Custom game mode: show Change Map button
                cont.button("Change Map", Icon.map, () -> {
                    hide();
                    showMapSelector();
                }).row();

                // Also show Player Connect button
                cont.button("@message.manage-room.title", Icon.planet, () -> {
                    hide();
                    createRoomDialog.show();
                }).row();
            }
        } else {
            // Not in game: show default Player Connect button
            cont.button("@message.manage-room.title", Icon.planet, () -> {
                hide();
                createRoomDialog.show();
            }).row();
        }
    }

    private void showMapSelector() {
        try {
            // Use reflection to access custom game dialog
            Object customDialog = arc.util.Reflect.get(Vars.ui, "custom");
            if (customDialog instanceof mindustry.ui.dialogs.BaseDialog) {
                ((mindustry.ui.dialogs.BaseDialog) customDialog).show();
            } else {
                Vars.ui.showInfo("Could not find Custom Game dialog.");
            }
        } catch (Exception e) {
            Vars.ui.showException(e);
        }
    }

    // loadMap method is no longer needed but kept empty or removed to avoid unused
    // warnings if referenced elsewhere
    // In this file, it was only called by showMapSelector, so we can remove it.
}

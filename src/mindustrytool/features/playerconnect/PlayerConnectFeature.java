package mindustrytool.features.playerconnect;

import arc.Core;
import arc.Events;
import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;

public class PlayerConnectFeature implements Feature {
    private PlayerConnectRoomsDialog dialog;
    private CreateRoomDialog createRoomDialog;
    private JoinRoomDialog joinRoomDialog;

    @Override
    public FeatureMetadata getMetadata() {
        return new FeatureMetadata("Player Connect", "Join and create P2P rooms.", "host");
    }

    @Override
    public void init() {
        dialog = new PlayerConnectRoomsDialog();
        createRoomDialog = new CreateRoomDialog();
        joinRoomDialog = new JoinRoomDialog(dialog);
        
        Events.on(ClientLoadEvent.class, e -> {
            if (FeatureManager.getInstance().isEnabled(this)) {
                addButton();
            }
        });
    }
    
    private void addButton() {
        if (Vars.mobile) {
            Vars.ui.menufrag.addButton(Core.bundle.format("message.player-connect.title"), Icon.menu, () -> {
                if (FeatureManager.getInstance().isEnabled(this)) {
                    dialog.show();
                } else {
                    Vars.ui.showInfo("Feature is disabled.");
                }
            });
        }
    }

    @Override
    public void onEnable() {
        // Desktop is handled by Main's Tools menu
    }

    @Override
    public void onDisable() {
        if (dialog != null) dialog.hide();
        if (createRoomDialog != null) createRoomDialog.hide();
        if (joinRoomDialog != null) joinRoomDialog.hide();
    }
    
    public PlayerConnectRoomsDialog getDialog() {
        return dialog;
    }
    
    public CreateRoomDialog getCreateRoomDialog() {
        return createRoomDialog;
    }
    
    public JoinRoomDialog getJoinRoomDialog() {
        return joinRoomDialog;
    }
}

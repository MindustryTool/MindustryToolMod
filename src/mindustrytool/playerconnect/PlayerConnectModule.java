package mindustrytool.playerconnect;

import arc.Core;
import arc.Events;
import arc.util.Log;

import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;
import mindustry.gen.Icon;
import mindustry.ui.fragments.MenuFragment.MenuButton;

import mindustrytool.ModuleLoader.Module;
import mindustrytool.playerconnect.gui.CreateRoomDialog;
import mindustrytool.playerconnect.gui.JoinRoomDialog;
import mindustrytool.playerconnect.gui.PlayerConnectRoomsDialog;

/**
 * PlayerConnect Module - multiplayer via CLaJ.
 */
public class PlayerConnectModule implements Module {

    public static final String NAME = "PlayerConnect";

    public static PlayerConnectRoomsDialog playerConnectRoomsDialog;
    public static CreateRoomDialog createRoomDialog;
    public static JoinRoomDialog joinRoomDialog;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init() {
        Log.info("[PlayerConnectModule] Initializing...");

        // Create dialogs
        playerConnectRoomsDialog = new PlayerConnectRoomsDialog();
        createRoomDialog = new CreateRoomDialog();
        joinRoomDialog = new JoinRoomDialog(playerConnectRoomsDialog);

        // Register UI when client loads
        Events.on(ClientLoadEvent.class, e -> onClientLoad());

        Log.info("[PlayerConnectModule] Initialized");
    }

    private void onClientLoad() {
        Log.info("[PlayerConnectModule] Adding UI...");

        MenuButton playerConnectButton = new MenuButton(
                Core.bundle.format("message.player-connect.title"),
                Icon.menu,
                () -> playerConnectRoomsDialog.show()
        );

        if (Vars.mobile) {
            Vars.ui.menufrag.addButton(playerConnectButton.text, playerConnectButton.icon, playerConnectButton.runnable);
        } else {
            Vars.ui.menufrag.addButton(new MenuButton(
                    "Player Connect",
                    Icon.players,
                    () -> {},
                    playerConnectButton
            ));
        }

        Log.info("[PlayerConnectModule] UI added");
    }
}

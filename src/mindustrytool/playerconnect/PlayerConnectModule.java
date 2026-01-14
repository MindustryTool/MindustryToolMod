package mindustrytool.playerconnect;

import arc.Events;
import arc.util.Log;

import mindustry.Vars;
import mindustry.game.EventType.ClientLoadEvent;

import mindustrytool.ModuleLoader.Module;
// Import từ gui package
import mindustrytool.playerconnect.gui.CreateRoomDialog;
import mindustrytool.playerconnect.gui.PlayerConnectJoinInjector;

/**
 * PlayerConnect Module - multiplayer via CLaJ.
 */
public class PlayerConnectModule implements Module {

    public static final String NAME = "PlayerConnect";

    public static CreateRoomDialog createRoomDialog;
    private static PlayerConnectJoinInjector joinInjector;

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public void init() {
        Log.info("[PlayerConnectModule] Initializing...");

        // Create dialogs
        createRoomDialog = new CreateRoomDialog();
        joinInjector = new PlayerConnectJoinInjector();

        // Register UI when client loads
        Events.on(ClientLoadEvent.class, e -> onClientLoad());

        Log.info("[PlayerConnectModule] Initialized");
    }

    private void onClientLoad() {
        Log.info("[PlayerConnectModule] Adding UI...");

        Log.info("[PlayerConnectModule] UI added");

        // Inject into the game's join dialog
        if (Vars.ui.join != null) {
            joinInjector.inject(Vars.ui.join);
            
            // Re-inject when dialog is shown to handle UI refreshes
            Vars.ui.join.shown(() -> {
                joinInjector.inject(Vars.ui.join);
            });
        }
    }
}

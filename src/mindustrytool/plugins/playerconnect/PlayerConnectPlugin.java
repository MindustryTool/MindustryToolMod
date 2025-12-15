package mindustrytool.plugins.playerconnect;

import arc.util.Log;
import mindustrytool.Plugin;

/**
 * PlayerConnect Plugin - Self-contained multiplayer room system.
 * 
 * This plugin provides Player Connect functionality including:
 * - Creating and hosting rooms via proxy servers
 * - Joining rooms via player-connect:// links
 * - Browsing available rooms
 * - Server provider management
 * 
 * All functionality is self-contained within this package.
 */
public class PlayerConnectPlugin implements Plugin {
    
    private static PlayerConnectPlugin instance;
    private PlayerConnectRoomsDialog roomsDialog;
    private JoinRoomDialog joinRoomDialog;
    private boolean initialized = false;
    
    /** Gets the singleton instance of the plugin. */
    public static PlayerConnectPlugin getInstance() {
        if (instance == null) instance = new PlayerConnectPlugin();
        return instance;
    }
    
    @Override public String getName() { return "PlayerConnect"; }
    @Override public int getPriority() { return 60; }
    
    @Override public void init() {
        if (initialized) return;
        
        PlayerConnect.init();
        PlayerConnectProviders.loadCustom();
        
        roomsDialog = new PlayerConnectRoomsDialog();
        joinRoomDialog = new JoinRoomDialog(roomsDialog);
        JoinDialogInjector.inject();
        
        initialized = true;
    }
    
    @Override public void dispose() {
        Log.info("[PlayerConnect] Disposing...");
        PlayerConnect.disposeRoom();
        PlayerConnect.disposePinger();
    }
    
    public PlayerConnectRoomsDialog getRoomsDialog() { return roomsDialog; }
    public JoinRoomDialog getJoinRoomDialog() { return joinRoomDialog; }
    public void showRoomsBrowser() { if (roomsDialog != null) roomsDialog.show(); }
    public void showJoinDialog() { if (joinRoomDialog != null) joinRoomDialog.show(); }
    public void showCreateRoomDialog() { new CreateRoomDialog().show(); }
}

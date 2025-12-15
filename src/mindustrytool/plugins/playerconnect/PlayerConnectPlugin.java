package mindustrytool.plugins.playerconnect;

import arc.Core;
import arc.struct.Seq;
import arc.util.Log;
import mindustrytool.Plugin;
import mindustrytool.plugins.browser.LazyComponent;

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
    private boolean initialized = false;
    private static JoinRoomDialog joinRoomDialog;

    /** Registry of lazy-loaded components */
    public static final Seq<LazyComponent<?>> lazyComponents = new Seq<>();

    private static final LazyComponent<PlayerConnectRoomsDialog> roomsDialog = new LazyComponent<>(
            "PlayerConnect",
            Core.bundle.get("message.lazy.playerconnect.desc", "Multiplayer rooms - browse, join, and create"),
            PlayerConnectRoomsDialog::new);

    static {
        lazyComponents.add(roomsDialog);
    }

    /** Gets the singleton instance of the plugin. */
    public static PlayerConnectPlugin getInstance() {
        if (instance == null)
            instance = new PlayerConnectPlugin();
        return instance;
    }

    public static Seq<LazyComponent<?>> getLazyComponents() {
        return lazyComponents;
    }

    public static LazyComponent<PlayerConnectRoomsDialog> getRoomsDialog() {
        return roomsDialog;
    }

    public static JoinRoomDialog getJoinRoomDialog() {
        if (joinRoomDialog == null) {
            var rooms = roomsDialog.getIfEnabled();
            if (rooms != null) {
                joinRoomDialog = new JoinRoomDialog(rooms);
            }
        }
        return joinRoomDialog;
    }

    @Override
    public String getName() {
        return "PlayerConnect";
    }

    @Override
    public int getPriority() {
        return 60;
    }

    @Override
    public void init() {
        if (initialized)
            return;

        PlayerConnect.init();
        PlayerConnectProviders.loadCustom();
        JoinDialogInjector.inject();

        initialized = true;
    }

    @Override
    public void dispose() {
        Log.info("[PlayerConnect] Disposing...");
        PlayerConnect.disposeRoom();
        PlayerConnect.disposePinger();
        // Unload lazy components
        roomsDialog.unload();
        joinRoomDialog = null;
    }

    public void showRoomsBrowser() {
        var dialog = roomsDialog.getIfEnabled();
        if (dialog != null)
            dialog.show();
    }

    public void showJoinDialog() {
        var dialog = getJoinRoomDialog();
        if (dialog != null)
            dialog.show();
    }

    public void showCreateRoomDialog() {
        new CreateRoomDialog().show();
    }
}

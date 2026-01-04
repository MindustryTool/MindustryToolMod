package mindustrytool.features.social.multiplayer;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.game.EventType;
import mindustrytool.Feature;
import mindustrytool.features.content.browser.LazyComponent;

/**
 * PlayerConnect Plugin - Self-contained multiplayer room system.
 */
public class PlayerConnectFeature implements Feature {

    private static PlayerConnectFeature instance;
    private boolean initialized = false;
    private static JoinRoomDialog joinRoomDialog;

    /** Registry of lazy-loaded components */
    public static final Seq<LazyComponent<?>> lazyComponents = new Seq<>();

    private static final LazyComponent<PlayerConnectRoomsDialog> roomsDialog = new LazyComponent<>(
            "PlayerConnect",
            Core.bundle.get("mdt.message.lazy.playerconnect.desc", "Multiplayer rooms - browse, join, and create"),
            PlayerConnectRoomsDialog::new);

    static {
        roomsDialog.onSettings(() -> getInstance().showCreateRoomDialog());
    }

    // Voice Chat Component - Cross-platform (Desktop + Android)
    private static final LazyComponent<mindustrytool.features.social.voice.VoiceChatManager> voiceChatManager = new LazyComponent<>(
            "Voice Chat",
            "Enable voice communication with other players.",
            () -> {
                mindustrytool.features.social.voice.VoiceChatManager manager = new mindustrytool.features.social.voice.VoiceChatManager();
                manager.init();
                return manager;
            },
            false);

    static {
        // Register Settings for Voice Chat
        voiceChatManager.onSettings(() -> {
            mindustrytool.features.social.voice.VoiceChatManager vc = voiceChatManager.getIfEnabled();
            if (vc != null) {
                vc.showSettings();
            } else {
                arc.Core.app.post(() -> {
                    mindustry.ui.dialogs.BaseDialog d = new mindustry.ui.dialogs.BaseDialog("Info");
                    d.cont.add("Please enable 'Voice Chat' first.");
                    d.addCloseButton();
                    d.show();
                });
            }
        });

        lazyComponents.add(roomsDialog);
        lazyComponents.add(voiceChatManager);
    }

    // Static Access for UI

    /** Gets EntityVisibilityManager if enabled, for sharing filter data. */
    public static mindustrytool.features.gameplay.visuals.EntityVisibilityManager getEntityVisibilityManager() {
        return mindustrytool.features.gameplay.GameplayFeature.getEntityVisibilityManager();
    }

    // Hook into Tools Menu construction to add the Settings button.
    // Patch ToolsMenuDialog to check for 'Configurable' components or add hardcoded
    // buttons.
    /** Gets the singleton instance of the plugin. */
    public static PlayerConnectFeature getInstance() {
        if (instance == null)
            instance = new PlayerConnectFeature();
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
            PlayerConnectRoomsDialog rooms = roomsDialog.getIfEnabled();
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

    private static CreateRoomDialog createRoomDialog;

    @Override
    public void init() {
        if (initialized)
            return;

        // Register events to handle room merging
        Events.on(EventType.ClientLoadEvent.class, e -> {
            // Force-load Voice Chat Manager so it's ready for packets immediately
            // This fixes the "Host must open settings/mic first" bug
            voiceChatManager.get();
        });

        Events.on(EventType.HostEvent.class, e -> {
            // Ensure manager is loaded when hosting
            voiceChatManager.get();
        });

        // Inject listeners once (they check enabled state internally)
        JoinDialogInjector.inject();
        PausedMenuInjector.inject();

        // Register toggle handler for Core PlayerConnect logic
        roomsDialog.onToggle(this::onTogglePlayerConnect);

        // Initial state check
        if (roomsDialog.isEnabled()) {
            onTogglePlayerConnect(true);
        }

        // Auto Host Logic: Delegate to CreateRoomDialog.
        Runnable trigger = () -> {
            if (createRoomDialog != null && roomsDialog.isEnabled()) {
                arc.util.Timer.schedule(() -> createRoomDialog.triggerAutoHost(), 2f);
            }
        };

        Events.on(EventType.PlayEvent.class, e -> {
            trigger.run();
            // Sync map logic ...
            if (mindustry.Vars.net.server() && roomsDialog.isEnabled()) {
                arc.util.Timer.schedule(() -> {
                    try {
                        Iterable<? extends mindustry.net.NetConnection> connections = mindustry.Vars.net
                                .getConnections();
                        for (mindustry.net.NetConnection con : connections) {
                            if (con.player != null) {
                                mindustry.Vars.netServer.sendWorldData(con.player);
                            } else {
                                Log.warn("[PlayerConnect] Fast sync skipped: Player is null for @", con.address);
                            }
                        }
                    } catch (Exception ex) {
                        Log.err("[PlayerConnect] Error syncing map to clients", ex);
                    }
                }, 0.2f);
            }
        });

        Events.on(EventType.WorldLoadEvent.class, e -> {
            trigger.run();
            if (mindustry.Vars.net.server() && roomsDialog.isEnabled()) {
                try {
                    Iterable<? extends mindustry.net.NetConnection> connections = mindustry.Vars.net.getConnections();
                    for (mindustry.net.NetConnection con : connections) {
                        if (con.player != null) {
                            mindustry.Vars.netServer.sendWorldData(con.player);
                        }
                    }
                } catch (Exception ex) {
                }
            }
        });

        // Centralized Lazy Triggers: ... (keep existing)

        initialized = true;
    }

    private void onTogglePlayerConnect(boolean enabled) {
        if (enabled) {
            Log.info("[PlayerConnect] Enabling...");
            PlayerConnect.init();
            PlayerConnectProviders.loadCustom();
            if (createRoomDialog == null) {
                createRoomDialog = new CreateRoomDialog();
            }
        } else {
            Log.info("[PlayerConnect] Disabling...");
            PlayerConnect.disposeRoom();
            PlayerConnect.disposePinger();
            createRoomDialog = null; // Release reference
        }
    }

    @Override
    public void dispose() {
        Log.info("[PlayerConnect] Disposing...");
        onTogglePlayerConnect(false);

        // Unload lazy components
        roomsDialog.unload();
        joinRoomDialog = null;
    }

    public void showRoomsBrowser() {
        PlayerConnectRoomsDialog dialog = roomsDialog.getIfEnabled();
        if (dialog != null)
            dialog.show();
    }

    public void showJoinDialog() {
        JoinRoomDialog dialog = getJoinRoomDialog();
        if (dialog != null)
            dialog.show();
    }

    public void showCreateRoomDialog() {
        if (createRoomDialog != null) {
            createRoomDialog.show();
        }
    }

    /**
     * Get Voice Chat Manager if enabled.
     * Used by QuickAccessFeature to show Voice Settings.
     */
    public static mindustrytool.features.social.voice.VoiceChatManager getVoiceChatManager() {
        return voiceChatManager.getIfEnabled();
    }
}

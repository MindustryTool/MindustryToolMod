package mindustrytool.plugins.playerconnect;

import arc.Core;
import arc.Events;
import arc.struct.Seq;
import arc.util.Log;
import mindustry.game.EventType;
import mindustrytool.Plugin;
import mindustrytool.plugins.browser.LazyComponent;

/**
 * PlayerConnect Plugin - Self-contained multiplayer room system.
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
        roomsDialog.onSettings(() -> getInstance().showCreateRoomDialog());
    }

    // Enemy Pathfinding Visualization Component
    private static final LazyComponent<mindustrytool.visuals.PathfindingVisualizer> pathfindingVisualizer = new LazyComponent<>(
            "Enemy Pathfinding",
            "Visualizes predicted enemy paths with an organic flow effect.",
            mindustrytool.visuals.PathfindingVisualizer::new,
            false);

    // Health Bar Visualization Component
    private static final LazyComponent<mindustrytool.visuals.HealthBarVisualizer> healthBarVisualizer = new LazyComponent<>(
            "Health Bars",
            "Visualizes unit health bars.",
            mindustrytool.visuals.HealthBarVisualizer::new,
            false);

    // Entity Visibility Manager
    private static final LazyComponent<mindustrytool.visuals.EntityVisibilityManager> entityVisibilityManager = new LazyComponent<>(
            "Entity Hider",
            "Hides units/blocks to improve FPS.",
            mindustrytool.visuals.EntityVisibilityManager::new,
            false);

    // Strategic Overlays
    private static final LazyComponent<mindustrytool.visuals.VisualOverlayManager> visualOverlayManager = new LazyComponent<>(
            "Strategic Overlays",
            "Displays turret ranges, projector zones, and spawn points.",
            mindustrytool.visuals.VisualOverlayManager::new,
            false);

    // Team Resources Overlay
    private static final LazyComponent<mindustrytool.visuals.TeamResourcesOverlay> teamResourcesOverlay = new LazyComponent<>(
            "Team Resources",
            "Display team items and power stats with multi-team support.",
            mindustrytool.visuals.TeamResourcesOverlay::new,
            false);

    // Distribution Reveal Visualizer
    private static final LazyComponent<mindustrytool.visuals.DistributionRevealVisualizer> distributionRevealVisualizer = new LazyComponent<>(
            "Distribution Reveal",
            "Reveal items inside bridges, junctions, and unloaders.",
            mindustrytool.visuals.DistributionRevealVisualizer::new,
            false);

    // Voice Chat Component - Cross-platform (Desktop + Android)
    private static final LazyComponent<mindustrytool.plugins.voicechat.VoiceChatManager> voiceChatManager = new LazyComponent<>(
            "Voice Chat",
            "Enable voice communication with other players.",
            () -> {
                mindustrytool.plugins.voicechat.VoiceChatManager manager = new mindustrytool.plugins.voicechat.VoiceChatManager();
                manager.init();
                return manager;
            },
            false);

    // Smart Drill Component - Automated resource mining
    private static final LazyComponent<mindustrytool.plugins.autodrill.SmartDrillManager> smartDrillManager = new LazyComponent<>(
            "Smart Drill",
            "Automatically fill resource patches with drills.",
            mindustrytool.plugins.autodrill.SmartDrillManager::new,
            false);

    static {
        // Register Settings for Lazy Component (Settings Gear Icon in Manage
        // Components)
        entityVisibilityManager.onSettings(() -> {
            mindustrytool.visuals.EntityVisibilityManager manager = entityVisibilityManager.getIfEnabled();
            if (manager != null) {
                manager.showDialog();
            } else {
                arc.Core.app.post(() -> {
                    mindustry.ui.dialogs.BaseDialog d = new mindustry.ui.dialogs.BaseDialog("Info");
                    d.cont.add("Please enable 'Entity Hider' first.");
                    d.addCloseButton();
                    d.show();
                });
            }
        });

        // Register Settings for Enemy Pathfinding
        pathfindingVisualizer.onSettings(() -> {
            mindustrytool.visuals.PathfindingVisualizer viz = pathfindingVisualizer.getIfEnabled();
            if (viz != null) {
                viz.showSettings();
            } else {
                arc.Core.app.post(() -> {
                    mindustry.ui.dialogs.BaseDialog d = new mindustry.ui.dialogs.BaseDialog("Info");
                    d.cont.add("Please enable 'Enemy Pathfinding' first.");
                    d.addCloseButton();
                    d.show();
                });
            }
        });

        // Register Settings for Health Bars
        healthBarVisualizer.onSettings(() -> {
            mindustrytool.visuals.HealthBarVisualizer viz = healthBarVisualizer.getIfEnabled();
            if (viz != null) {
                viz.showSettings();
            } else {
                arc.Core.app.post(() -> {
                    mindustry.ui.dialogs.BaseDialog d = new mindustry.ui.dialogs.BaseDialog("Info");
                    d.cont.add("Please enable 'Health Bars' first.");
                    d.addCloseButton();
                    d.show();
                });
            }
        });

        lazyComponents.add(roomsDialog);
        lazyComponents.add(pathfindingVisualizer);
        lazyComponents.add(healthBarVisualizer);
        lazyComponents.add(entityVisibilityManager);
        lazyComponents.add(visualOverlayManager);
        // lazyComponents.add(distributionRevealVisualizer);
        lazyComponents.add(teamResourcesOverlay);
        lazyComponents.add(voiceChatManager);
        lazyComponents.add(smartDrillManager);

        // Register Settings for Smart Drill
        smartDrillManager.onSettings(() -> {
            mindustrytool.plugins.autodrill.SmartDrillManager sm = smartDrillManager.getIfEnabled();
            if (sm != null) {
                sm.showSettings();
            } else {
                arc.Core.app.post(() -> {
                    mindustry.ui.dialogs.BaseDialog d = new mindustry.ui.dialogs.BaseDialog("Info");
                    d.cont.add("Please enable 'Smart Drill' first.");
                    d.addCloseButton();
                    d.show();
                });
            }
        });

        // Register Settings for Voice Chat
        voiceChatManager.onSettings(() -> {
            mindustrytool.plugins.voicechat.VoiceChatManager vc = voiceChatManager.getIfEnabled();
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

        /*
         * // Register Settings for Distribution Reveal
         * distributionRevealVisualizer.onSettings(() -> {
         * mindustrytool.visuals.DistributionRevealVisualizer viz =
         * distributionRevealVisualizer.getIfEnabled();
         * if (viz != null) {
         * viz.showSettings();
         * } else {
         * arc.Core.app.post(() -> {
         * mindustry.ui.dialogs.BaseDialog d = new
         * mindustry.ui.dialogs.BaseDialog("Info");
         * d.cont.add("Please enable 'Distribution Reveal' first.");
         * d.addCloseButton();
         * d.show();
         * });
         * }
         * });
         */

        // Register Settings for Team Resources
        teamResourcesOverlay.onSettings(() -> {
            mindustrytool.visuals.TeamResourcesOverlay overlay = teamResourcesOverlay.getIfEnabled();
            if (overlay != null) {
                overlay.showDialog();
            }
        });

        // Register Settings for Strategic Overlays
        visualOverlayManager.onSettings(() -> {
            mindustrytool.visuals.VisualOverlayManager manager = visualOverlayManager.getIfEnabled();
            if (manager != null) {
                manager.showDialog();
            } else {
                arc.Core.app.post(() -> {
                    mindustry.ui.dialogs.BaseDialog d = new mindustry.ui.dialogs.BaseDialog("Info");
                    d.cont.add("Please enable 'Strategic Overlays' first.");
                    d.addCloseButton();
                    d.show();
                });
            }
        });
    }

    // Static Access for UI
    public static mindustrytool.visuals.EntityVisibilityManager getVisibilityManager() {
        return entityVisibilityManager.get();
    }

    /** Gets EntityVisibilityManager if enabled, for sharing filter data. */
    public static mindustrytool.visuals.EntityVisibilityManager getEntityVisibilityManager() {
        return entityVisibilityManager.getIfEnabled();
    }

    // Hook into Tools Menu construction to add the Settings button.
    // Patch ToolsMenuDialog to check for 'Configurable' components or add hardcoded
    // buttons.
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
        Events.run(EventType.Trigger.draw, () -> {
            if (!mindustry.Vars.state.isGame())
                return;

            mindustrytool.visuals.VisualOverlayManager overlays = visualOverlayManager.getIfEnabled();
            if (overlays != null)
                overlays.renderOverlays();

            mindustrytool.visuals.EntityVisibilityManager visibility = entityVisibilityManager.getIfEnabled();
            if (visibility != null)
                visibility.updateVisibility();

            mindustrytool.visuals.HealthBarVisualizer health = healthBarVisualizer.getIfEnabled();
            if (health != null)
                health.draw();

            mindustrytool.visuals.PathfindingVisualizer paths = pathfindingVisualizer.getIfEnabled();
            if (paths != null)
                paths.draw();

            mindustrytool.plugins.autodrill.SmartDrillManager sm = smartDrillManager.getIfEnabled();
            if (sm != null)
                sm.draw();
        });

        // Initialize immediately if enabled
        teamResourcesOverlay.getIfEnabled();

        // Smart Drill Events ...
        Events.on(EventType.TapEvent.class, e -> {
            mindustrytool.plugins.autodrill.SmartDrillManager sm = smartDrillManager.getIfEnabled();
            if (sm != null)
                sm.handleTap(e);
        });

        Events.run(EventType.Trigger.update, () -> {
            mindustrytool.plugins.autodrill.SmartDrillManager sm = smartDrillManager.getIfEnabled();
            if (sm != null)
                sm.update();
        });

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
        pathfindingVisualizer.unload();
        healthBarVisualizer.unload();
        // distributionRevealVisualizer.unload();
        entityVisibilityManager.unload();
        visualOverlayManager.unload();
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
     * Used by QuickAccessPlugin to show Voice Settings.
     */
    public static mindustrytool.plugins.voicechat.VoiceChatManager getVoiceChatManager() {
        return voiceChatManager.getIfEnabled();
    }
}

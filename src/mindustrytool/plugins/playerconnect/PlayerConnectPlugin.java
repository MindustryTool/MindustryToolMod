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

    // Enemy Pathfinding Visualization Component
    private static final LazyComponent<mindustrytool.visuals.PathfindingVisualizer> pathfindingVisualizer = new LazyComponent<>(
            "Enemy Pathfinding",
            "Visualizes predicted enemy paths with an organic flow effect.",
            mindustrytool.visuals.PathfindingVisualizer::new);

    // Health Bar Visualization Component
    private static final LazyComponent<mindustrytool.visuals.HealthBarVisualizer> healthBarVisualizer = new LazyComponent<>(
            "Health Bars",
            "Visualizes unit health bars.",
            mindustrytool.visuals.HealthBarVisualizer::new);

    // Entity Visibility Manager
    private static final LazyComponent<mindustrytool.visuals.EntityVisibilityManager> entityVisibilityManager = new LazyComponent<>(
            "Entity Hider",
            "Hides units/blocks to improve FPS.",
            mindustrytool.visuals.EntityVisibilityManager::new);

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

        lazyComponents.add(roomsDialog);
        lazyComponents.add(pathfindingVisualizer);
        lazyComponents.add(healthBarVisualizer);
        lazyComponents.add(entityVisibilityManager);
    }

    // Static Access for UI
    public static mindustrytool.visuals.EntityVisibilityManager getVisibilityManager() {
        return entityVisibilityManager.get();
    }

    // Hook into Tools Menu construction to add the Settings button
    // This assumes ToolsMenuDialog calls back or we modify ToolsMenuDialog.
    // Since ToolsMenuDialog is likely generic, I will patch it to check for
    // 'Configurable' components or just add a hardcoded button for now.
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

    private static CreateRoomDialog createRoomDialog;

    @Override
    public void init() {
        if (initialized)
            return;

        PlayerConnect.init();
        PlayerConnectProviders.loadCustom();
        JoinDialogInjector.inject();

        // Initialize and inject Host dialog
        createRoomDialog = new CreateRoomDialog();
        PausedMenuInjector.inject(createRoomDialog);

        // Auto Host Logic: Delegate to CreateRoomDialog
        // We use both PlayEvent and WorldLoadEvent to catch all cases (Saved games,
        // Campaign, New Games)
        // A small delay ensures the world is fully loaded and 'Vars.state' is correct.
        Runnable trigger = () -> {
            if (createRoomDialog != null) {
                arc.util.Timer.schedule(() -> createRoomDialog.triggerAutoHost(), 2f);
            }
        };

        Events.on(EventType.PlayEvent.class, e -> trigger.run());
        Events.on(EventType.WorldLoadEvent.class, e -> trigger.run());

        // Eagerly load enabled components (crucial for background visualizers)
        // This ensures PathfindingVisualizer starts if enabled in settings
        Core.app.post(() -> {
            for (LazyComponent<?> comp : lazyComponents) {
                if (comp.isEnabled()) {
                    comp.get(); // Force instantiation
                }
            }
        });

        initialized = true;
    }

    @Override
    public void dispose() {
        Log.info("[PlayerConnect] Disposing...");
        PlayerConnect.disposeRoom();
        PlayerConnect.disposePinger();
        // Unload lazy components
        roomsDialog.unload();
        pathfindingVisualizer.unload();
        joinRoomDialog = null;
        createRoomDialog = null;
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
        if (createRoomDialog != null) {
            createRoomDialog.show();
        }
    }
}

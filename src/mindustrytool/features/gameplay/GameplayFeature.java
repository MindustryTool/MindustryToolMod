package mindustrytool.features.gameplay;

import arc.Events;
import arc.struct.Seq;
import mindustry.game.EventType;
import mindustrytool.Feature;
import mindustrytool.features.content.browser.LazyComponent;
import mindustrytool.features.gameplay.autodrill.SmartDrillManager;
import mindustrytool.features.gameplay.visuals.*;

/**
 * Gameplay Feature - Manages gameplay helpers, visualizers, and overlays.
 * Extracted from PlayerConnectFeature to separate concerns.
 */
public class GameplayFeature implements Feature {

    private static GameplayFeature instance;
    private boolean initialized = false;

    /** Registry of lazy-loaded components */
    public static final Seq<LazyComponent<?>> lazyComponents = new Seq<>();

    // Enemy Pathfinding Visualization Component
    private static final LazyComponent<PathfindingVisualizer> pathfindingVisualizer = new LazyComponent<>(
            "Enemy Pathfinding",
            "Visualizes predicted enemy paths with an organic flow effect.",
            PathfindingVisualizer::new,
            false);

    // Health Bar Visualization Component
    private static final LazyComponent<HealthBarVisualizer> healthBarVisualizer = new LazyComponent<>(
            "Health Bars",
            "Visualizes unit health bars.",
            HealthBarVisualizer::new,
            false);

    // Entity Visibility Manager
    private static final LazyComponent<EntityVisibilityManager> entityVisibilityManager = new LazyComponent<>(
            "Entity Hider",
            "Hides units/blocks to improve FPS.",
            EntityVisibilityManager::new,
            false);

    // Strategic Overlays
    private static final LazyComponent<VisualOverlayManager> visualOverlayManager = new LazyComponent<>(
            "Strategic Overlays",
            "Displays turret ranges, projector zones, and spawn points.",
            VisualOverlayManager::new,
            false);

    // Team Resources Overlay
    private static final LazyComponent<TeamResourcesOverlay> teamResourcesOverlay = new LazyComponent<>(
            "Team Resources",
            "Display team items and power stats with multi-team support.",
            TeamResourcesOverlay::new,
            false);

    // Smart Drill Component - Automated resource mining
    private static final LazyComponent<SmartDrillManager> smartDrillManager = new LazyComponent<>(
            "Smart Drill",
            "Automatically fill resource patches with drills.",
            SmartDrillManager::new,
            false);

    // Distribution Reveal Visualizer
    private static final LazyComponent<DistributionRevealVisualizer> distributionRevealVisualizer = new LazyComponent<>(
            "Distribution Viz",
            "Visualizes item and liquid flow through bridges and conduits.",
            DistributionRevealVisualizer::new,
            false);

    static {
        // Register Settings for Lazy Components

        entityVisibilityManager.onSettings(() -> {
            EntityVisibilityManager manager = entityVisibilityManager.getIfEnabled();
            if (manager != null) {
                manager.showDialog();
            } else {
                showEnableFirstDialog("Entity Hider");
            }
        });

        pathfindingVisualizer.onSettings(() -> {
            PathfindingVisualizer viz = pathfindingVisualizer.getIfEnabled();
            if (viz != null) {
                viz.showSettings();
            } else {
                showEnableFirstDialog("Enemy Pathfinding");
            }
        });

        healthBarVisualizer.onSettings(() -> {
            HealthBarVisualizer viz = healthBarVisualizer.getIfEnabled();
            if (viz != null) {
                viz.showSettings();
            } else {
                showEnableFirstDialog("Health Bars");
            }
        });

        smartDrillManager.onSettings(() -> {
            SmartDrillManager sm = smartDrillManager.getIfEnabled();
            if (sm != null) {
                sm.showSettings();
            } else {
                showEnableFirstDialog("Smart Drill");
            }
        });

        distributionRevealVisualizer.onSettings(() -> {
            DistributionRevealVisualizer viz = distributionRevealVisualizer.getIfEnabled();
            if (viz != null) {
                viz.showSettings();
            } else {
                showEnableFirstDialog("Distribution Viz");
            }
        });

        teamResourcesOverlay.onSettings(() -> {
            TeamResourcesOverlay overlay = teamResourcesOverlay.getIfEnabled();
            if (overlay != null) {
                overlay.showDialog();
            }
        });

        visualOverlayManager.onSettings(() -> {
            VisualOverlayManager manager = visualOverlayManager.getIfEnabled();
            if (manager != null) {
                manager.showDialog();
            } else {
                showEnableFirstDialog("Strategic Overlays");
            }
        });

        lazyComponents.add(pathfindingVisualizer);
        lazyComponents.add(healthBarVisualizer);
        lazyComponents.add(entityVisibilityManager);
        lazyComponents.add(visualOverlayManager);
        lazyComponents.add(teamResourcesOverlay);
        lazyComponents.add(smartDrillManager);
        lazyComponents.add(distributionRevealVisualizer);

    }

    private static void showEnableFirstDialog(String featureName) {
        arc.Core.app.post(() -> {
            mindustry.ui.dialogs.BaseDialog d = new mindustry.ui.dialogs.BaseDialog("Info");
            d.cont.add("Please enable '" + featureName + "' first.");
            d.addCloseButton();
            d.show();
        });
    }

    public static GameplayFeature getInstance() {
        if (instance == null) {
            instance = new GameplayFeature();
        }
        return instance;
    }

    @Override
    public String getName() {
        return "Gameplay";
    }

    @Override
    public int getPriority() {
        return 40; // Load before PlayerConnect (60) just in case, or after? Doesn't matter much.
    }

    @Override
    public void init() {
        if (initialized)
            return;

        // Initialize immediately if enabled
        teamResourcesOverlay.getIfEnabled();

        // Register Draw Loop
        Events.run(EventType.Trigger.draw, () -> {
            if (!mindustry.Vars.state.isGame())
                return;

            VisualOverlayManager overlays = visualOverlayManager.getIfEnabled();
            if (overlays != null)
                overlays.renderOverlays();

            EntityVisibilityManager visibility = entityVisibilityManager.getIfEnabled();
            if (visibility != null)
                visibility.updateVisibility();

            HealthBarVisualizer health = healthBarVisualizer.getIfEnabled();
            if (health != null)
                health.draw();

            PathfindingVisualizer paths = pathfindingVisualizer.getIfEnabled();
            if (paths != null)
                paths.draw();

            SmartDrillManager sm = smartDrillManager.getIfEnabled();
            if (sm != null)
                sm.draw();

            DistributionRevealVisualizer distViz = distributionRevealVisualizer.getIfEnabled();
            if (distViz != null)
                distViz.draw();
        });

        // Register Smart Drill Events
        Events.on(EventType.TapEvent.class, e -> {
            SmartDrillManager sm = smartDrillManager.getIfEnabled();
            if (sm != null)
                sm.handleTap(e);
        });

        Events.run(EventType.Trigger.update, () -> {
            SmartDrillManager sm = smartDrillManager.getIfEnabled();
            if (sm != null)
                sm.update();
        });

        initialized = true;
    }

    @Override
    public void dispose() {
        pathfindingVisualizer.unload();
        healthBarVisualizer.unload();
        entityVisibilityManager.unload();
        visualOverlayManager.unload();
        smartDrillManager.unload();
        distributionRevealVisualizer.unload();
        // teamResourcesOverlay doesn't strictly need unload if it just listens to
        // events,
        // but consistent behavior is good.
    }

    // Static Accessors for Inter-Feature Communication (if needed)

    public static Seq<LazyComponent<?>> getLazyComponents() {
        return lazyComponents;
    }

    public static LazyComponent<SmartDrillManager> getSmartDrillComponent() {
        return smartDrillManager;
    }

    public static EntityVisibilityManager getEntityVisibilityManager() {
        return entityVisibilityManager.getIfEnabled();
    }
}

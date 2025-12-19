package mindustrytool.plugins.visuals;

import arc.struct.Seq;
import mindustrytool.Plugin;
import mindustrytool.plugins.browser.LazyComponent;

/**
 * Visuals plugin - provides in-game visualizers (pathfinding, health bars).
 */
public class VisualsPlugin implements Plugin {
    /** Registry of all lazy-loaded components in this plugin. */
    public static final Seq<LazyComponent<?>> lazyComponents = new Seq<>();

    private static final LazyComponent<PathfindingVisualizer> pathfindingVisualizer = new LazyComponent<>(
            "Enemy Pathfinding",
            "Visualizes predicted enemy paths with an organic flow effect.",
            PathfindingVisualizer::new);

    private static final LazyComponent<HealthBarVisualizer> healthBarVisualizer = new LazyComponent<>(
            "Health Bars",
            "Visualizes unit health bars.",
            HealthBarVisualizer::new);

    static {
        lazyComponents.add(pathfindingVisualizer);
        lazyComponents.add(healthBarVisualizer);
    }

    @Override
    public String getName() {
        return "Visuals";
    }

    @Override
    public int getPriority() {
        return 70;
    }

    @Override
    public void init() {
        // nothing to initialize eagerly
    }

    public static Seq<LazyComponent<?>> getLazyComponents() {
        return lazyComponents;
    }
}

package mindustrytool;

/**
 * Interface for MindustryTool plugins.
 * All plugins must implement this interface to be auto-loaded.
 */
public interface Plugin {
    /** Plugin name for logging. */
    String getName();

    /** Priority for loading order (higher = load first). Default is 0. */
    default int getPriority() {
        return 0;
    }

    /** Initialize the plugin. Called during mod init. */
    void init();

    /** List of plugin names that this plugin depends on. */
    default String[] getDependencies() {
        return new String[0];
    }

    /**
     * Reload plugin UI/state. Called when dev presses F12 to hot-reload.
     * Override this to rebuild UI without restarting game.
     * Default implementation calls init() again.
     */
    default void reload() {
        init();
    }

    /** Dispose plugin resources. Called during mod dispose. */
    default void dispose() {
    }
}

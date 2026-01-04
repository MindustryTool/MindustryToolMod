package mindustrytool;

/**
 * Interface for mindustrytool.features.
 * All features must implement this interface to be auto-loaded.
 */
public interface Feature {
    /** Feature name for logging. */
    String getName();

    /** Priority for loading order (higher = load first). Default is 0. */
    default int getPriority() {
        return 0;
    }

    /** Initialize the feature. Called during mod init. */
    void init();

    /** List of feature names that this feature depends on. */
    default String[] getDependencies() {
        return new String[0];
    }

    /**
     * Reload feature UI/state. Called when dev presses F12 to hot-reload.
     * Override this to rebuild UI without restarting game.
     * Default implementation calls init() again.
     */
    default void reload() {
        init();
    }

    /** Dispose feature resources. Called during mod dispose. */
    default void dispose() {
    }
}

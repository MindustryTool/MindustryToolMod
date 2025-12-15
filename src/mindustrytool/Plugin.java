package mindustrytool;

/**
 * Interface for MindustryTool plugins.
 * All plugins must implement this interface to be auto-loaded.
 */
public interface Plugin {
    /** Plugin name for logging. */
    String getName();
    
    /** Priority for loading order (higher = load first). Default is 0. */
    default int getPriority() { return 0; }
    
    /** Initialize the plugin. Called during mod init. */
    void init();
    
    /** Dispose plugin resources. Called during mod dispose. */
    default void dispose() {}
}

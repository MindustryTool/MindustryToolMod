package mindustrytool;

/**
 * Minimal module interface: only two required methods so modules stay lightweight.
 * Implementations should keep logic in `init()`; optional behaviors are provided
 * via separate interfaces in the same package (see `ClientLoad`, `ModuleMenu`, `ModuleDisposable`).
 */
public interface Module {
    /**
     * Human-friendly module name.
     */
    String getName();

    /**
     * Called once when the module is initialized.
     */
    void init();
}

package mindustrytool.plugins.browser;

import arc.func.Prov;

/**
 * Wrapper for lazy-loaded components.
 * Components are only instantiated when first accessed via get().
 */
public class LazyComponent<T> {
    private final String name;
    private final String description;
    private final Prov<T> factory;
    private T instance = null;
    private Runnable onSettings;

    public LazyComponent(String name, String description, Prov<T> factory) {
        this.name = name;
        this.description = description;
        this.factory = factory;
    }

    public LazyComponent<T> onSettings(Runnable onSettings) {
        this.onSettings = onSettings;
        return this;
    }

    /** Gets or creates the component instance. */
    public T get() {
        if (instance == null)
            instance = factory.get();
        return instance;
    }

    /** Returns true if the component has been loaded. */
    public boolean isLoaded() {
        return instance != null;
    }

    /** Unloads the component, freeing memory. */
    public void unload() {
        instance = null;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public boolean hasSettings() {
        return onSettings != null;
    }

    public void openSettings() {
        if (onSettings != null)
            onSettings.run();
    }
}

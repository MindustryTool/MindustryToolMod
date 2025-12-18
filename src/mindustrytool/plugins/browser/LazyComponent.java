package mindustrytool.plugins.browser;

import arc.Core;
import arc.func.Prov;

/**
 * Wrapper for lazy-loaded components.
 * Components are only instantiated when first accessed via get().
 * Supports proper disposal of resources when unloading.
 */
public class LazyComponent<T> {
    private final String name;
    private final String description;
    private final Prov<T> factory;
    private T instance = null;
    private Runnable onSettings;
    private Runnable onDispose;
    private boolean enabled = true;

    public LazyComponent(String name, String description, Prov<T> factory) {
        this.name = name;
        this.description = description;
        this.factory = factory;
        // Load persisted enabled state
        this.enabled = Core.settings.getBool("lazy." + name + ".enabled", true);
    }

    public LazyComponent<T> onSettings(Runnable onSettings) {
        this.onSettings = onSettings;
        return this;
    }

    /** Set dispose callback to clean up resources (threads, listeners, etc.) */
    public LazyComponent<T> onDispose(Runnable onDispose) {
        this.onDispose = onDispose;
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

    /** Returns true if the component is enabled. */
    public boolean isEnabled() {
        return enabled;
    }

    /**
     * Enable or disable the component. Disabling also unloads. State is persisted.
     */
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
        Core.settings.put("lazy." + name + ".enabled", enabled);
        if (!enabled)
            unload();
    }

    /** Gets instance only if enabled, otherwise returns null. */
    public T getIfEnabled() {
        if (!enabled)
            return null;
        return get();
    }

    /** Unloads the component, disposing resources and freeing memory. */
    public void unload() {
        if (instance != null) {
            // Auto-detect and call dispose() if available
            try {
                java.lang.reflect.Method disposeMethod = instance.getClass().getMethod("dispose");
                disposeMethod.invoke(instance);
            } catch (NoSuchMethodException e) {
                // No dispose method, that's fine
            } catch (Exception e) {
                arc.util.Log.err("Failed to dispose: " + name, e);
            }

            // Also call custom dispose callback if set
            if (onDispose != null) {
                try {
                    onDispose.run();
                } catch (Exception e) {
                    arc.util.Log.err("Failed custom dispose: " + name, e);
                }
            }
            instance = null;
        }
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

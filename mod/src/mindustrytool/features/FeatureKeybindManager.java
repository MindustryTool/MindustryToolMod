package mindustrytool.features;

import arc.Core;
import arc.Events;
import mindustry.game.EventType.Trigger;

/**
 * Centralized dispatcher for all feature keybinds.
 * Polls registered {@link FeatureKeybind}s on {@link Trigger#update},
 * suppresses execution while any scene field holds focus,
 * enforces {@code requireEnabled} preconditions,
 * and runs actions on the application main thread.
 */
public final class FeatureKeybindManager {
    private static boolean initialized = false;

    private FeatureKeybindManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        Events.run(Trigger.update, FeatureKeybindManager::update);
    }

    static void update() {
        if (Core.scene.hasField()) {
            return;
        }
        for (Feature feature : FeatureManager.getFeatures()) {
            for (FeatureKeybind keybind : feature.getKeybinds()) {
                if (shouldTrigger(feature, keybind, false, Core.input.keyRelease(keybind.getBind()))) {
                    Core.app.post(keybind.getAction());
                }
            }
        }
    }

    /**
     * Pure precondition check used by {@link #update()} and unit tests.
     *
     * @param feature     parent feature owning the keybind
     * @param keybind     keybind descriptor to evaluate
     * @param hasField    true when any scene field holds keyboard focus
     * @param keyReleased true when the bound key was pressed and released
     * @return true when the action should run
     */
    public static boolean shouldTrigger(Feature feature, FeatureKeybind keybind, boolean hasField,
            boolean keyReleased) {
        if (hasField || !keyReleased) {
            return false;
        }
        return !keybind.isRequireEnabled() || feature.isEnabled();
    }
}

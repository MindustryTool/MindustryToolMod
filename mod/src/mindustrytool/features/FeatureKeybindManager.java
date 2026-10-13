package mindustrytool.features;

import arc.Core;
import arc.Events;
import mindustry.game.EventType.Trigger;
import mindustrytool.components.SettingKeybindRow;

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
        if (Core.scene.hasField() || SettingKeybindRow.isAnyListening()) {
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

    public static boolean shouldTrigger(Feature feature, FeatureKeybind keybind, boolean hasField,
            boolean keyReleased) {
        if (hasField || !keyReleased) {
            return false;
        }
        return !keybind.isRequireEnabled() || feature.isEnabled();
    }
}

package mindustrytool.features;

import arc.Core;
import arc.Events;
import arc.input.KeyCode;
import arc.util.Nullable;
import mindustry.game.EventType.Trigger;
import mindustrytool.components.SettingKeybindRow;

public final class FeatureKeybindManager {
    private static boolean initialized = false;
    private static volatile @Nullable SettingKeybindRow captureTarget;

    private FeatureKeybindManager() {
    }

    public static void init() {
        if (initialized) {
            return;
        }
        initialized = true;
        Events.run(Trigger.update, FeatureKeybindManager::update);
    }

    public static void beginCapture(SettingKeybindRow row) {
        captureTarget = row;
    }

    public static void endCapture(SettingKeybindRow row) {
        if (captureTarget == row) {
            captureTarget = null;
        }
    }

    public static boolean isCapturing() {
        return captureTarget != null;
    }

    static void update() {
        SettingKeybindRow target = captureTarget;
        if (target != null) {
            captureKey(target);
            return;
        }
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

    private static void captureKey(SettingKeybindRow row) {
        for (KeyCode key : KeyCode.all) {
            if (Core.input.keyTap(key)) {
                row.handleInput(key);
                break;
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

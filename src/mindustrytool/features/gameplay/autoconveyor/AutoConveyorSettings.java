package mindustrytool.features.gameplay.autoconveyor;

import arc.Core;

public class AutoConveyorSettings {

    // Toggles
    public static final String SETTING_DESTRUCTIVE = "autoconveyor.destructive";
    public static final String SETTING_USE_BRIDGE = "autoconveyor.use_bridge";
    public static final String SETTING_USE_PHASE = "autoconveyor.use_phase";
    public static final String SETTING_USE_JUNCTION = "autoconveyor.use_junction";
    public static final String SETTING_USE_DUCT_BRIDGE = "autoconveyor.use_duct_bridge";

    // Selections
    public static final String SETTING_ALGORITHM = "autoconveyor.algorithm";

    public enum Algorithm {
        FAST("Fast"),
        BEAUTIFUL("Beautiful"),
        SCIENTIFIC("Scientific");

        public final String label;

        Algorithm(String label) {
            this.label = label;
        }
    }

    public static Algorithm getAlgorithm() {
        String name = Core.settings.getString(SETTING_ALGORITHM, Algorithm.SCIENTIFIC.name());
        try {
            return Algorithm.valueOf(name);
        } catch (IllegalArgumentException e) {
            return Algorithm.SCIENTIFIC;
        }
    }

    // Getters
    public static boolean isUseBridge() {
        return Core.settings.getBool(SETTING_USE_BRIDGE, true);
    }

    public static boolean isUsePhase() {
        return Core.settings.getBool(SETTING_USE_PHASE, true);
    }

    public static boolean isUseJunction() {
        return Core.settings.getBool(SETTING_USE_JUNCTION, true);
    }

    public static boolean isUseDuctBridge() {
        return Core.settings.getBool(SETTING_USE_DUCT_BRIDGE, true);
    }

    public static boolean isDestructive() {
        return Core.settings.getBool(SETTING_DESTRUCTIVE, false);
    }
}

package mindustrytool.features.settings;

import solim.config.ConfigGroup;
import solim.config.ConfigValue;

/**
 * Mod-wide settings backed by persistent {@link ConfigGroup}.
 * Holds global preferences that do not belong to any single feature.
 */
public final class ModSettings {

    public static final ConfigGroup GROUP = ConfigGroup.of("mindustrytool.settings");

    public static final ConfigValue<Boolean> betaParticipate = GROUP.boolValue("betaParticipate", false);

    private ModSettings() {}
}

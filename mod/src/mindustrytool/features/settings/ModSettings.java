package mindustrytool.features.settings;

import arc.struct.Seq;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.OrderedSeqPersister;

/**
 * Mod-wide settings backed by persistent {@link ConfigGroup}.
 * Holds global preferences that do not belong to any single feature.
 */
public final class ModSettings {

    public static final ConfigGroup GROUP = ConfigGroup.of("mindustrytool.settings");

    public static final ConfigValue<Seq<String>> featureOrder = GROUP.value("feature-order", Seq.with(), new OrderedSeqPersister());
    public static final ConfigValue<Boolean> betaParticipate = GROUP.boolValue("betaParticipate", false);
    public static final ConfigValue<Boolean> sharePresence = GROUP.boolValue("share-presence", true);
    public static final ConfigValue<Boolean> freeCamera = GROUP.boolValue("free-camera", false);

    private ModSettings() {}
}

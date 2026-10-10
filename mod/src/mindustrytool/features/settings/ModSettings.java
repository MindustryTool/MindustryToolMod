package mindustrytool.features.settings;

import arc.struct.Seq;
import arc.util.Nullable;
import java.util.Collections;
import java.util.Set;
import mindustry.Vars;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.OrderedSeqPersister;
import solim.reactive.Computed;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Mod-wide settings backed by persistent {@link ConfigGroup}.
 * Holds global preferences that do not belong to any single feature.
 */
public final class ModSettings {

    public static final ConfigGroup GROUP = ConfigGroup.of("mindustrytool.settings");

    public static final ConfigValue<Seq<String>> featureOrder = GROUP.value("feature-order", Seq.with(), new OrderedSeqPersister());
    public static final ConfigValue<Set<String>> favoriteFeatures = GROUP.setValue("favorites", String.class, Collections.emptySet());
    public static final ConfigValue<Boolean> betaParticipate = GROUP.boolValue("betaParticipate", false);
    public static final ConfigValue<Boolean> sharePresence = GROUP.boolValue("share-presence", true);
    public static final ConfigValue<Float> universalScale = GROUP.floatValue("universal-scale",
            Vars.mobile ? 0.8f : 1.0f);

    public static Computed<Float> effectiveScale(@Nullable Readable<Float> featureScale) {
        Readable<Float> local = featureScale != null ? featureScale : Readable.of(1f);
        return Signal.computed(() -> {
            Float universal = universalScale.signal().get();
            Float feature = local.get();
            return (universal != null ? universal : 1f) * (feature != null ? feature : 1f);
        });
    }

    public static float effectiveScale(float featureScale) {
        Float universal = universalScale.signal().peek();
        return (universal != null ? universal : 1f) * featureScale;
    }

    private ModSettings() {}
}

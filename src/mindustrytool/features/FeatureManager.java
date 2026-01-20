package mindustrytool.features;

import arc.Core;
import arc.struct.Seq;
import arc.util.Log;

public class FeatureManager {
    private static final FeatureManager instance = new FeatureManager();
    private final Seq<Feature> features = new Seq<>();

    private FeatureManager() {
    }

    public static FeatureManager getInstance() {
        return instance;
    }

    public void register(Feature... feature) {
        features.addAll(feature);
        features.sort((a, b) -> Integer.compare(a.getMetadata().order(), b.getMetadata().order()));
    }

    public void init() {
        for (Feature feature : features) {
            feature.init();
            Log.info("Initialized feature: @", feature.getMetadata().name());
            if (isEnabled(feature)) {
                feature.onEnable();
                Log.info("Enabled feature: @", feature.getMetadata().name());
            }
        }
    }

    public boolean isEnabled(Feature feature) {
        return Core.settings.getBool("mindustrytool.feature." + feature.getMetadata().name() + ".enabled",
                feature.getMetadata().enabledByDefault());
    }

    public void setEnabled(Feature feature, boolean enabled) {
        boolean current = isEnabled(feature);
        if (current == enabled)
            return;

        Core.settings.put("mindustrytool.feature." + feature.getMetadata().name() + ".enabled", enabled);
        if (enabled) {
            feature.onEnable();
        } else {
            feature.onDisable();
        }
    }

    public Seq<Feature> getFeatures() {
        return features;
    }

    public Seq<Feature> getEnableds() {
        return features.select(f -> isEnabled(f));
    }
}

package mindustrytool;

import arc.Core;
import arc.struct.Seq;

public class FeatureManager {
    private static final FeatureManager instance = new FeatureManager();
    private final Seq<Feature> features = new Seq<>();

    private FeatureManager() {}

    public static FeatureManager getInstance() {
        return instance;
    }

    public void register(Feature ...feature) {
        features.addAll(feature);
    }

    public void init() {
        for (Feature feature : features) {
            feature.init();
            if (isEnabled(feature)) {
                feature.onEnable();
            }
        }
    }

    public boolean isEnabled(Feature feature) {
        return Core.settings.getBool("mindustrytool.feature." + feature.getMetadata().name() + ".enabled", true);
    }

    public void setEnabled(Feature feature, boolean enabled) {
        boolean current = isEnabled(feature);
        if (current == enabled) return;

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
}

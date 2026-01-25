package mindustrytool.features;

import arc.Core;
import arc.struct.Seq;

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

    public <T extends Feature> T getFeature(Class<T> featureClass) {
        return featureClass.cast(features.find(f -> f.getClass() == featureClass));
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
        return Core.settings.getBool("mindustrytool.feature." + feature.getMetadata().name() + ".enabled",
                feature.getMetadata().enabledByDefault());
    }

    public void setEnabled(Feature feature, boolean enabled) {
        boolean current = isEnabled(feature);
        if (current == enabled) {
            return;
        }

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

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

    public void reEnable() {
        @SuppressWarnings("unchecked")
        Seq<String> enableds = Core.settings.getJson("mindustrytool.enabled-features", Seq.class, String.class,
                Seq::new);

        for (Feature feature : features) {
            if (enableds.contains(feature.getMetadata().name())) {
                feature.setEnabled(true);
            }
        }
    }

    public void disableAll() {
        Seq<String> enableds = getEnableds().map(f -> f.getMetadata().name());

        Core.settings.putJson("mindustrytool.enabled-features", String.class, enableds);

        for (Feature feature : features) {
            feature.setEnabled(false);
        }
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
            if (feature.isEnabled()) {
                feature.onEnable();
                feature.setting();
                feature.dialog();
            }
        }
    }

    public Seq<Feature> getFeatures() {
        return features;
    }

    public <T extends Feature> T get(Class<T> featureClass) {
        var feature = features.find(f -> f.getClass().equals(featureClass));
        if (feature == null) {
            throw new IllegalArgumentException("Feature not found: " + featureClass);
        }
        return featureClass.cast(feature);
    }

    public Seq<Feature> getEnableds() {
        return features.select(f -> f.isEnabled());
    }
}

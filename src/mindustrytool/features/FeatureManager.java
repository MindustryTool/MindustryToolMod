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
        Seq<String> enableds = Core.settings.getJson("mindustry-tool.enableds", Seq.class, String.class, Seq::new);

        for (Feature feature : features) {
            if (enableds.contains(feature.getMetadata().name())) {
                feature.setEnabled(true);
            }
        }
    }

    public void disableAll() {
        Seq<String> enableds = getEnableds().map(f -> f.getMetadata().name());

        Core.settings.putJson("mindustry-tool.enableds", String.class, enableds);

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
            if (feature.isEnabled()) {
                feature.init();
                feature.onEnable();
            }
        }
    }

    public Seq<Feature> getFeatures() {
        return features;
    }

    public Seq<Feature> getEnableds() {
        return features.select(f -> f.isEnabled());
    }
}

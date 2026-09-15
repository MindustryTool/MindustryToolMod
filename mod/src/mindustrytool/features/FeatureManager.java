package mindustrytool.features;

import arc.Core;
import arc.struct.Seq;
import solim.signal.Signal;

public class FeatureManager {
	private static final Signal<Seq<Feature>> features = Signal.of(new Seq<>());

	public static void reenable() {
		@SuppressWarnings("unchecked")
		Seq<String> enableds =
				Core.settings.getJson("mindustrytool.enabled-features", Seq.class, String.class, Seq::new);

		for (Feature feature : features.get()) {
			if (enableds.contains(feature.getMetadata().getId())) {
				feature.enable();
			}
		}
	}

	public static void disableAll() {
		Seq<String> enableds = getEnableds().map(f -> f.getMetadata().getId());

		Core.settings.putJson("mindustrytool.enabled-features", String.class, enableds);

		for (Feature feature : features.get()) {
			feature.disable();
		}
	}

	public static void register(Feature... feature) {
		features.update(seq -> {
			Seq<Feature> copy = new Seq<>(seq);
			copy.addAll(feature);
			copy.sort((a, b) ->
					Integer.compare(a.getMetadata().getOrder(), b.getMetadata().getOrder()));
			return copy;
		});
	}

	public static void unregister(Feature... feature) {
		features.update(seq -> {
			Seq<Feature> copy = new Seq<>(seq);
			for (Feature f : feature) {
				copy.remove(f);
			}
			return copy;
		});
	}

	public static <T extends Feature> T getFeature(Class<T> featureClass) {
		return featureClass.cast(features.get().find(f -> f.getClass() == featureClass));
	}

	public static void init() {
		for (Feature feature : features.get()) {
			if (!feature.getMetadata().isDevelopment() && feature.isEnabled()) {
				feature.onEnable();
			}
		}
	}

	public static Seq<Feature> getFeatures() {
		return features.get();
	}

	public static Signal<Seq<Feature>> features() {
		return features;
	}

	public static <T extends Feature> T get(Class<T> featureClass) {
		var feature = features.get().find(f -> f.getClass().equals(featureClass));
		if (feature == null) {
			throw new IllegalArgumentException("Feature not found: " + featureClass);
		}
		return featureClass.cast(feature);
	}

	public static Seq<Feature> getEnableds() {
		return features.get().select(f -> f.isEnabled());
	}
}

package mindustrytool.features;

import arc.Core;
import arc.struct.Seq;
import arc.util.Nullable;
import mindustrytool.features.settings.ModSettings;
import solim.signal.Readable;
import solim.signal.Signal;

public class FeatureManager {
	private static final Seq<Feature> registered = new Seq<>();
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
		for (Feature f : feature) {
			if (!registered.contains(f)) {
				registered.add(f);
			}
		}
		updateFeaturesFromRegistration();
	}

	public static void unregister(Feature... feature) {
		for (Feature f : feature) {
			registered.remove(f);
		}
		updateFeaturesFromRegistration();
	}

	public static void clear() {
		registered.clear();
		features.set(new Seq<>());
	}

	private static void updateFeaturesFromRegistration() {
		Seq<String> stored = ModSettings.featureOrder.get();
		if (stored != null && !stored.isEmpty()) {
			applyOrder(stored);
		} else {
			Seq<Feature> copy = new Seq<>(registered);
			copy.sort((a, b) -> {
				int cmp = Integer.compare(a.getMetadata().getOrder(), b.getMetadata().getOrder());
				return cmp != 0 ? cmp : a.getMetadata().getId().compareTo(b.getMetadata().getId());
			});
			features.set(copy);
		}
	}

	public static <T extends Feature> T getFeature(Class<T> featureClass) {
		return featureClass.cast(features.get().find(f -> f.getClass() == featureClass));
	}

	public static void init() {
		normalizeAndApplyOrder();

		for (Feature feature : features.get()) {
			if (!feature.getMetadata().isDevelopment() && feature.isEnabled()) {
				feature.onEnable();
			}
		}
	}

	@SuppressWarnings("unchecked")
    public static void normalizeAndApplyOrder() {
		String key = ModSettings.featureOrder.getKey();
		Seq<String> stored = null;
		boolean hasKey = Core.settings.has(key);
		if (hasKey) {
			try {
				stored = Core.settings.getJson(key, Seq.class, String.class, () -> null);
			} catch (Exception ignored) {
				stored = null;
			}
		}

		Seq<String> normalized = normalizeOrder(registered, stored);
		boolean corrupt = hasKey && stored == null;
		boolean dirty = corrupt || !hasKey || isOrderDirty(stored, normalized);

		if (dirty) {
			ModSettings.featureOrder.set(normalized);
		} else {
			ModSettings.featureOrder.signal().set(normalized);
		}

		applyOrder(normalized);
	}

	public static Seq<String> normalizeOrder(Seq<Feature> allFeatures, @Nullable Seq<String> storedOrder) {
		Seq<Feature> nonDevFeatures = allFeatures.select(f -> !f.getMetadata().isDevelopment());
		nonDevFeatures.sort((a, b) -> {
			int cmp = Integer.compare(a.getMetadata().getOrder(), b.getMetadata().getOrder());
			return cmp != 0 ? cmp : a.getMetadata().getId().compareTo(b.getMetadata().getId());
		});

		if (storedOrder == null) {
			return nonDevFeatures.map(f -> f.getMetadata().getId());
		}

		Seq<String> result = new Seq<>();
		for (String id : storedOrder) {
			if (id == null || result.contains(id)) {
				continue;
			}
			Feature found = allFeatures.find(f -> f.getMetadata().getId().equals(id));
			if (found != null && !found.getMetadata().isDevelopment()) {
				result.add(id);
			}
		}

		for (Feature f : nonDevFeatures) {
			String id = f.getMetadata().getId();
			if (!result.contains(id)) {
				result.add(id);
			}
		}

		return result;
	}

	public static boolean isOrderDirty(@Nullable Seq<String> stored, Seq<String> normalized) {
		if (stored == null || stored.size != normalized.size) {
			return true;
		}
		for (int i = 0; i < stored.size; i++) {
			if (!stored.get(i).equals(normalized.get(i))) {
				return true;
			}
		}
		return false;
	}

	public static void applyOrder(Seq<String> orderedNonDevIds) {
		Seq<Feature> ordered = new Seq<>(registered.size);

		for (String id : orderedNonDevIds) {
			Feature f = registered.find(feat -> feat.getMetadata().getId().equals(id));
			if (f != null && !f.getMetadata().isDevelopment() && !ordered.contains(f)) {
				ordered.add(f);
			}
		}

		for (Feature f : registered) {
			if (!f.getMetadata().isDevelopment() && !ordered.contains(f)) {
				ordered.add(f);
			}
		}

		Seq<Feature> devFeatures = registered.select(f -> f.getMetadata().isDevelopment());
		devFeatures.sort((a, b) -> a.getMetadata().getId().compareTo(b.getMetadata().getId()));
		ordered.addAll(devFeatures);

		features.set(ordered);
	}

	public static boolean canMoveLeft(Feature feature) {
		if (feature == null || feature.getMetadata().isDevelopment()) {
			return false;
		}
		Seq<String> order = ModSettings.featureOrder.get();
		if (order == null || order.isEmpty()) {
			return false;
		}
		int index = order.indexOf(feature.getMetadata().getId());
		return index > 0;
	}

	public static boolean canMoveRight(Feature feature) {
		if (feature == null || feature.getMetadata().isDevelopment()) {
			return false;
		}
		Seq<String> order = ModSettings.featureOrder.get();
		if (order == null || order.isEmpty()) {
			return false;
		}
		int index = order.indexOf(feature.getMetadata().getId());
		return index >= 0 && index < order.size - 1;
	}

	public static Readable<Boolean> canMoveLeftSignal(Feature feature) {
		if (feature == null || feature.getMetadata().isDevelopment()) {
			return Readable.of(false);
		}
		return ModSettings.featureOrder.signal().map(order -> {
			if (order == null || order.isEmpty()) {
				return false;
			}
			int index = order.indexOf(feature.getMetadata().getId());
			return index > 0;
		});
	}

	public static Readable<Boolean> canMoveRightSignal(Feature feature) {
		if (feature == null || feature.getMetadata().isDevelopment()) {
			return Readable.of(false);
		}
		return ModSettings.featureOrder.signal().map(order -> {
			if (order == null || order.isEmpty()) {
				return false;
			}
			int index = order.indexOf(feature.getMetadata().getId());
			return index >= 0 && index < order.size - 1;
		});
	}

	public static boolean moveLeft(Feature feature) {
		return feature != null && moveLeft(feature.getMetadata().getId());
	}

	public static boolean moveLeft(String featureId) {
		return swapAdjacent(featureId, -1);
	}

	public static boolean moveRight(Feature feature) {
		return feature != null && moveRight(feature.getMetadata().getId());
	}

	public static boolean moveRight(String featureId) {
		return swapAdjacent(featureId, 1);
	}

	private static boolean swapAdjacent(String featureId, int delta) {
		Feature feat = registered.find(f -> f.getMetadata().getId().equals(featureId));
		if (feat == null || feat.getMetadata().isDevelopment()) {
			return false;
		}

		Seq<String> current = ModSettings.featureOrder.get();
		if (current == null) {
			return false;
		}
		int index = current.indexOf(featureId);
		if (index < 0) {
			return false;
		}
		int targetIndex = index + delta;
		if (targetIndex < 0 || targetIndex >= current.size) {
			return false;
		}

		Seq<String> updated = new Seq<>(current);
		updated.swap(index, targetIndex);
		ModSettings.featureOrder.set(updated);
		applyOrder(updated);
		return true;
	}

	public static Seq<Feature> getFeatures() {
		return features.get();
	}

	public static Signal<Seq<Feature>> features() {
		return features;
	}

	public static <T extends Feature> T get(Class<T> featureClass) {
		Feature feature = features.get().find(f -> f.getClass().equals(featureClass));
		if (feature == null) {
			throw new IllegalArgumentException("Feature not found: " + featureClass);
		}
		return featureClass.cast(feature);
	}

	public static Seq<Feature> getEnableds() {
		return features.get().select(f -> f.isEnabled());
	}
}

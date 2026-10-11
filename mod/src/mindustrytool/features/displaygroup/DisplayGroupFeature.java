package mindustrytool.features.displaygroup;

import arc.func.Prov;
import arc.scene.Element;
import arc.struct.Seq;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mindustry.gen.Icon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.OrderedSeqPersister;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;

/**
 * Aggregates display overlay features into a single QuickAccess popup.
 * Tapping its QuickAccess button toggles {@link DisplayGroupPopup};
 * long-pressing opens the group settings dialog.
 */
public class DisplayGroupFeature extends Feature {

    public static final Seq<String> DEFAULT_ORDER = Seq.with(
            "health-bar",
            "range-display",
            "pathfinding",
            "progress-display",
            "team-resource",
            "visualizer",
            "wave-preview",
            "toggle-rendering");

    public final ConfigGroup config;
    public final ConfigValue<Integer> colsConfig;
    public final ConfigValue<Seq<String>> displayOrderConfig;
    public final ConfigValue<Set<String>> hiddenFeaturesConfig;
    public final ConfigValue<Set<String>> shownFeaturesConfig;

    private @Nullable DisplayGroupSettingsDialog settingsDialog;

    public DisplayGroupFeature() {
        super(FeatureMetadata.builder()
                .id("display-group")
                .icon(Icon.eye)
                .order(4)
                .enabledByDefault(true)
                .quickAccess(true)
                .build());

        config = configGroup();
        colsConfig = config.intValue("cols", 4);
        displayOrderConfig = config.value("display-order", Seq.with(), new OrderedSeqPersister());
        hiddenFeaturesConfig = config.setValue("hidden", String.class, Collections.emptySet());
        shownFeaturesConfig = config.setValue("shown", String.class, Collections.emptySet());
    }

    public Seq<Feature> displayCandidates() {
        Seq<Feature> all = FeatureManager.getFeatures();
        Seq<Feature> result = new Seq<>();
        if (all == null) {
            return result;
        }
        for (String id : DEFAULT_ORDER) {
            Feature found = all.find(feat -> feat.getMetadata().getId().equals(id));
            if (found != null) {
                result.add(found);
            }
        }
        return result;
    }

    public List<Feature> orderedFeatures(@Nullable Seq<String> order) {
        List<Feature> result = new ArrayList<>();
        if (order == null) {
            return result;
        }
        Seq<Feature> candidates = displayCandidates();
        for (String id : order) {
            Feature found = candidates.find(f -> f.getMetadata().getId().equals(id));
            if (found != null && !result.contains(found)) {
                result.add(found);
            }
        }
        return result;
    }

    public Seq<String> getDisplayOrder() {
        return healDisplayOrder();
    }

    public Seq<String> healDisplayOrder() {
        Seq<String> stored = displayOrderConfig.get();
        Seq<String> healed = normalizeDisplayOrder(displayCandidates(), stored);
        if (isDisplayOrderDirty(stored, healed)) {
            displayOrderConfig.set(healed);
        }
        return healed;
    }

    public Readable<List<Feature>> orderedFeaturesSignal() {
        return displayOrderConfig.signal().map(
                stored -> orderedFeatures(normalizeDisplayOrder(displayCandidates(), stored)));
    }

    public List<Feature> computeVisibleItems(
            @Nullable Seq<String> order,
            @Nullable Set<String> hidden,
            @Nullable Set<String> shown) {
        List<Feature> list = new ArrayList<>();
        for (Feature f : orderedFeatures(order)) {
            String id = f.getMetadata().getId();
            boolean isHidden = hidden != null && hidden.contains(id);
            boolean isShown = shown != null && shown.contains(id);
            boolean visible = isHidden ? false : (isShown ? true : DEFAULT_ORDER.contains(id));
            if (visible) {
                list.add(f);
            }
        }
        return list;
    }

    public boolean isFeatureVisible(String id) {
        Set<String> hidden = hiddenFeaturesConfig.get();
        if (hidden != null && hidden.contains(id)) {
            return false;
        }
        Set<String> shown = shownFeaturesConfig.get();
        if (shown != null && shown.contains(id)) {
            return true;
        }
        return DEFAULT_ORDER.contains(id);
    }

    public void setFeatureVisible(String id, boolean visible) {
        boolean defaultOn = DEFAULT_ORDER.contains(id);

        Set<String> currentHidden = hiddenFeaturesConfig.get();
        Set<String> hidden = currentHidden != null ? new HashSet<>(currentHidden) : new HashSet<>();

        Set<String> currentShown = shownFeaturesConfig.get();
        Set<String> shown = currentShown != null ? new HashSet<>(currentShown) : new HashSet<>();

        boolean hiddenChanged;
        boolean shownChanged;

        if (visible) {
            hiddenChanged = hidden.remove(id);
            shownChanged = !defaultOn ? shown.add(id) : shown.remove(id);
        } else {
            shownChanged = shown.remove(id);
            hiddenChanged = defaultOn ? hidden.add(id) : hidden.remove(id);
        }

        if (hiddenChanged) {
            hiddenFeaturesConfig.set(hidden);
        }
        if (shownChanged) {
            shownFeaturesConfig.set(shown);
        }
    }

    public static Seq<String> normalizeDisplayOrder(Seq<Feature> candidates, @Nullable Seq<String> stored) {
        Seq<String> result = new Seq<>();
        if (stored != null) {
            for (String id : stored) {
                if (id == null || result.contains(id)) {
                    continue;
                }
                Feature found = candidates.find(f -> f.getMetadata().getId().equals(id));
                if (found != null) {
                    result.add(id);
                }
            }
        }
        for (Feature f : candidates) {
            String id = f.getMetadata().getId();
            if (!result.contains(id)) {
                result.add(id);
            }
        }
        return result;
    }

    public static boolean isDisplayOrderDirty(@Nullable Seq<String> stored, Seq<String> normalized) {
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

    public boolean moveUp(String id) {
        return swapDisplayOrder(id, -1);
    }

    public boolean moveDown(String id) {
        return swapDisplayOrder(id, 1);
    }

    private boolean swapDisplayOrder(@Nullable String id, int delta) {
        if (id == null) {
            return false;
        }
        Seq<String> current = getDisplayOrder();
        int index = current.indexOf(id);
        int target = index + delta;
        if (index < 0 || target < 0 || target >= current.size) {
            return false;
        }
        Seq<String> updated = new Seq<>(current);
        updated.swap(index, target);
        displayOrderConfig.set(updated);
        return true;
    }

    public boolean canMoveUp(@Nullable String id) {
        Seq<String> order = getDisplayOrder();
        return id != null && order.indexOf(id) > 0;
    }

    public boolean canMoveDown(@Nullable String id) {
        Seq<String> order = getDisplayOrder();
        if (id == null) {
            return false;
        }
        int index = order.indexOf(id);
        return index >= 0 && index < order.size - 1;
    }

    public Readable<Boolean> canMoveUpSignal(String id) {
        return displayOrderConfig.signal().map(order -> {
            Seq<String> normalized = normalizeDisplayOrder(displayCandidates(), order);
            return id != null && normalized.indexOf(id) > 0;
        });
    }

    public Readable<Boolean> canMoveDownSignal(String id) {
        return displayOrderConfig.signal().map(order -> {
            Seq<String> normalized = normalizeDisplayOrder(displayCandidates(), order);
            if (id == null) {
                return false;
            }
            int index = normalized.indexOf(id);
            return index >= 0 && index < normalized.size - 1;
        });
    }

    public void resetToDefaults() {
        colsConfig.reset();
        displayOrderConfig.reset();
        hiddenFeaturesConfig.reset();
        shownFeaturesConfig.reset();
        healDisplayOrder();
    }

    public void togglePopup(@Nullable Element quickAccessBar) {
        DisplayGroupPopup.toggle(this, quickAccessBar);
    }

    @Override
    public void onQuickAccessClick(@Nullable Element anchor) {
        togglePopup(anchor);
    }

    @Override
    public void onQuickAccessLongClick(@Nullable Element anchor) {
        Prov<SolimDialog> dlg = getSettingDialog();
        if (dlg != null) {
            dlg.get().show();
        }
    }

    @Override
    public void onEnable() {
        healDisplayOrder();
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            healDisplayOrder();
            if (settingsDialog == null) {
                settingsDialog = new DisplayGroupSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}

package mindustrytool.features.quickaccess;

import arc.Core;
import arc.func.Prov;
import arc.scene.Element;
import arc.struct.Seq;
import arc.util.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import mindustry.Vars;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;
import solim.config.OrderedSeqPersister;

import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;
import solim.reactive.Signals;
import solim.core.Units;

public class QuickAccessFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<Integer> colsConfig;
    public final ConfigValue<Set<String>> hiddenFeaturesConfig;
    public final ConfigValue<Set<String>> shownFeaturesConfig;
    public final ConfigValue<Boolean> hideDragHandleConfig;
    public final ConfigValue<Seq<String>> displayOrderConfig;

    public final ContextualConfigValue<Float, Boolean> xConfig;
    public final ContextualConfigValue<Float, Boolean> yConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    private @Nullable QuickAccessHudView hudView;
    private @Nullable QuickAccessSettingsDialog settingsDialog;

    public QuickAccessFeature() {
        super(FeatureMetadata.builder()
                .id("quick-access")
                .icon(FileIcon.of("grid-2x2.png"))
                .order(10)
                .enabledByDefault(true)
                .quickAccess(false)
                .build());

        config = configGroup();

        opacityConfig = config.floatValue("opacity", 1f);
        scaleConfig = config.floatValue("scale", 1f);
        colsConfig = config.intValue("cols", 6);
        hiddenFeaturesConfig = config.setValue("hidden", String.class, Collections.emptySet());
        shownFeaturesConfig = config.setValue("shown", String.class, Collections.emptySet());
        hideDragHandleConfig = config.boolValue("hideDragHandle", false);
        displayOrderConfig = config.value("display-order", Seq.with(), new OrderedSeqPersister());

        xConfig = config.floatValueKeyed(
                "x",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sw = Units.screenWidth();
                    float defX = sw > 0 ? sw / 2f : 400f;
                    String oldKey = p ? "mindustrytool.quickaccess.x.portrait"
                            : "mindustrytool.quickaccess.x.landscape";
                    String groupKey = p ? "mindustrytool.quick-access.portrait.x"
                            : "mindustrytool.quick-access.landscape.x";
                    if (Core.settings.has(groupKey)) {
                        return Core.settings.getFloat(groupKey);
                    }
                    return Core.settings.getFloat(oldKey, defX);
                });

        yConfig = config.floatValueKeyed(
                "y",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sh = Units.screenHeight();
                    float defY = sh > 0 ? sh / 2f : 200f;
                    String oldKey = p ? "mindustrytool.quickaccess.y.portrait"
                            : "mindustrytool.quickaccess.y.landscape";
                    String groupKey = p ? "mindustrytool.quick-access.portrait.y"
                            : "mindustrytool.quick-access.landscape.y";
                    if (Core.settings.has(groupKey)) {
                        return Core.settings.getFloat(groupKey);
                    }
                    return Core.settings.getFloat(oldKey, defY);
                });

        xSignal = xConfig.signal();
        ySignal = yConfig.signal();

        Signals.isPortrait().subscribe(p -> {
            if (hudView != null) {
                Core.app.post(hudView::keepInScreen);
            }
        });
    }

    public float x() {
        Float val = xConfig.get();
        return val != null ? val : Units.screenWidth() / 2f;
    }

    public void x(float value) {
        xConfig.set(value);
    }

    public float y() {
        Float val = yConfig.get();
        return val != null ? val : Units.screenHeight() / 2f;
    }

    public void y(float value) {
        yConfig.set(value);
    }

    public void updateOrientationPosition() {
        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
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
        Feature f = FeatureManager.getFeatures().find(feat -> feat.getMetadata().getId().equals(id));
        return f != null && f.getMetadata().isQuickAccessByDefault();
    }

    public void setFeatureVisible(String id, boolean visible) {
        Feature f = FeatureManager.getFeatures().find(feat -> feat.getMetadata().getId().equals(id));
        boolean defaultOn = f != null && f.getMetadata().isQuickAccessByDefault();

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

    public Seq<Feature> quickAccessFeatures() {
        return FeatureManager.getFeatures().select(
                f -> f != this && !f.getMetadata().isDevelopment());
    }

    public List<Feature> orderedFeatures(@Nullable Seq<String> order) {
        List<Feature> result = new ArrayList<>();
        if (order == null) {
            return result;
        }
        Seq<Feature> candidates = quickAccessFeatures();
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
        Seq<String> healed = normalizeDisplayOrder(quickAccessFeatures(), stored);
        if (isDisplayOrderDirty(stored, healed)) {
            displayOrderConfig.set(healed);
        }
        return healed;
    }

    public Readable<List<Feature>> orderedFeaturesSignal() {
        return displayOrderConfig.signal().map(stored -> orderedFeatures(
                normalizeDisplayOrder(quickAccessFeatures(), stored)));
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
            Seq<String> normalized = normalizeDisplayOrder(quickAccessFeatures(), order);
            return id != null && normalized.indexOf(id) > 0;
        });
    }

    public Readable<Boolean> canMoveDownSignal(String id) {
        return displayOrderConfig.signal().map(order -> {
            Seq<String> normalized = normalizeDisplayOrder(quickAccessFeatures(), order);
            if (id == null) {
                return false;
            }
            int index = normalized.indexOf(id);
            return index >= 0 && index < normalized.size - 1;
        });
    }

    public void resetPosition() {
        float cx = Units.screenWidth() / 2f;
        float cy = Units.screenHeight() / 2f;

        Core.settings.put("mindustrytool.quick-access.x.portrait", cx);
        Core.settings.put("mindustrytool.quick-access.x.landscape", cx);
        Core.settings.put("mindustrytool.quick-access.y.portrait", cy);
        Core.settings.put("mindustrytool.quick-access.y.landscape", cy);

        xConfig.reset();
        yConfig.reset();

        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
    }

    @Override
    public void onEnable() {
        healDisplayOrder();
        if (Vars.ui.hudGroup != null) {
            if (hudView != null) {
                hudView.element().remove();
                hudView.dispose();
            }

            hudView = new QuickAccessHudView(this);
            Element el = hudView.element();
            el.name = "quick-access-hud";
            el.visible(() -> Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown && Vars.state != null
                    && Vars.state.isGame());

            Core.app.post(() -> {
                if (hudView != null && Vars.ui.hudGroup != null) {
                    Vars.ui.hudGroup.addChild(el);
                }
            });
        }
    }

    @Override
    public void onDisable() {
        if (hudView != null) {
            QuickAccessHudView view = hudView;
            hudView = null;
            Core.app.post(() -> {
                view.element().remove();
                view.dispose();
            });
        }
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            healDisplayOrder();
            if (settingsDialog == null) {
                settingsDialog = new QuickAccessSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}

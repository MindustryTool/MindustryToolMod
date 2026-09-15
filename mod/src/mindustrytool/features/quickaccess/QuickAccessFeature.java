package mindustrytool.features.quickaccess;

import arc.Core;
import arc.func.Prov;
import arc.scene.Element;
import arc.util.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import mindustry.Vars;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;

import solim.overlay.SolimDialog;
import solim.signal.Signal;
import solim.signal.Signals;
import solim.ui.Units;

public class QuickAccessFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<Integer> colsConfig;
    public final ConfigValue<Set<String>> hiddenFeaturesConfig;
    public final ConfigValue<Boolean> hideDragHandleConfig;

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
        hideDragHandleConfig = config.boolValue("hideDragHandle", false);

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
        return hidden == null || !hidden.contains(id);
    }

    public void setFeatureVisible(String id, boolean visible) {
        Set<String> current = hiddenFeaturesConfig.get();
        Set<String> hidden = current != null ? new HashSet<>(current) : new HashSet<>();
        if (visible) {
            hidden.remove(id);
        } else {
            hidden.add(id);
        }
        hiddenFeaturesConfig.set(hidden);
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
            if (settingsDialog == null) {
                settingsDialog = new QuickAccessSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}

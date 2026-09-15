package mindustrytool.features.quickaccess;

import arc.Core;
import arc.Events;
import arc.scene.Element;
import solim.overlay.SolimDialog;
import arc.util.Nullable;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import mindustry.Vars;
import mindustry.game.EventType.ResizeEvent;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.signal.Signal;

public class QuickAccessFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<Integer> colsConfig;
    public final ConfigValue<Set<String>> hiddenFeaturesConfig;

    public final ConfigGroup portraitGroup;
    public final ConfigGroup landscapeGroup;

    public final ConfigValue<Float> portraitXConfig;
    public final ConfigValue<Float> portraitYConfig;
    public final ConfigValue<Float> landscapeXConfig;
    public final ConfigValue<Float> landscapeYConfig;

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

        portraitGroup = config.group("portrait");
        landscapeGroup = config.group("landscape");

        float defX = Core.graphics.getWidth() / 2f;
        float defY = Core.graphics.getHeight() / 2f;

        portraitXConfig = portraitGroup.floatValue("x", Core.settings.getFloat("mindustrytool.quickaccess.x.portrait", defX));
        portraitYConfig = portraitGroup.floatValue("y", Core.settings.getFloat("mindustrytool.quickaccess.y.portrait", defY));
        landscapeXConfig = landscapeGroup.floatValue("x", Core.settings.getFloat("mindustrytool.quickaccess.x.landscape", defX));
        landscapeYConfig = landscapeGroup.floatValue("y", Core.settings.getFloat("mindustrytool.quickaccess.y.landscape", defY));

        xSignal = Signal.of(x());
        ySignal = Signal.of(y());

        xSignal.subscribe(val -> {
            if (val != null) {
                currentXConfig().set(val);
            }
        });
        ySignal.subscribe(val -> {
            if (val != null) {
                currentYConfig().set(val);
            }
        });

        Events.on(ResizeEvent.class, e -> updateOrientationPosition());
    }

    public ConfigValue<Float> currentXConfig() {
        return Core.graphics.isPortrait() ? portraitXConfig : landscapeXConfig;
    }

    public ConfigValue<Float> currentYConfig() {
        return Core.graphics.isPortrait() ? portraitYConfig : landscapeYConfig;
    }

    public float x() {
        Float val = currentXConfig().get();
        return val != null ? val : Core.graphics.getWidth() / 2f;
    }

    public void x(float value) {
        currentXConfig().set(value);
        xSignal.set(value);
    }

    public float y() {
        Float val = currentYConfig().get();
        return val != null ? val : Core.graphics.getHeight() / 2f;
    }

    public void y(float value) {
        currentYConfig().set(value);
        ySignal.set(value);
    }

    public void updateOrientationPosition() {
        xSignal.set(x());
        ySignal.set(y());
        if (hudView != null) {
            hudView.keepInScreen();
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
        float cx = Core.graphics.getWidth() / 2f;
        float cy = Core.graphics.getHeight() / 2f;
        portraitXConfig.set(cx);
        portraitYConfig.set(cy);
        landscapeXConfig.set(cx);
        landscapeYConfig.set(cy);
        xSignal.set(cx);
        ySignal.set(cy);
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
    public @Nullable SolimDialog getSettingDialog() {
        if (settingsDialog == null) {
            settingsDialog = new QuickAccessSettingsDialog(this);
        }
        return settingsDialog;
    }
}

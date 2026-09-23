package mindustrytool.features.teamresource;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;

import solim.overlay.SolimDialog;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;
import arc.scene.ui.layout.Scl;
import solim.reactive.Signal;
import solim.reactive.Signals;

/**
 * Feature responsible for registering and managing the Team Resource Tracker
 * overlay. Follows Solim architecture and ConfigGroup reactive configuration.
 */
public class TeamResourceFeature extends Feature {

    public final ConfigGroup config;
    public final ConfigValue<Float> opacityConfig;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<Float> overlayWidthConfig;
    public final ConfigValue<Float> overlayHeightConfig;

    public final ConfigValue<Boolean> showItemsConfig;
    public final ConfigValue<Boolean> showUnitsConfig;
    public final ConfigValue<Boolean> showPowerConfig;
    public final ConfigValue<Boolean> showStoredPowerConfig;
    public final ConfigValue<Boolean> hideBackgroundConfig;
    public final ConfigValue<Boolean> alwaysShowFlowRateConfig;
    public final ConfigValue<Boolean> hideZeroUnitsConfig;
    public final ConfigValue<Boolean> hideZeroItemsConfig;
    public final ConfigValue<Boolean> expandedConfig;

    public final ContextualConfigValue<Float, Boolean> xConfig;
    public final ContextualConfigValue<Float, Boolean> yConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    private final TeamResourceState state;
    private @Nullable TeamResourceHudView hudView;
    private @Nullable TeamResourceSettingsDialog settingsDialog;

    public TeamResourceFeature() {
        super(FeatureMetadata.builder()
                .id("team-resources")
                .icon(getFeatureIcon())
                .order(0)
                .enabledByDefault(true)
                .quickAccess(true)
                .build());

        config = configGroup();

        opacityConfig = config.floatValue("opacity", 1f);
        scaleConfig = config.floatValue("scale", 1f);
        overlayWidthConfig = config.floatValue("overlay-width", 0.28f);
        overlayHeightConfig = config.floatValue("overlay-height", 0.60f);

        showItemsConfig = config.boolValue("show-items", true);
        showUnitsConfig = config.boolValue("show-units", false);
        showPowerConfig = config.boolValue("show-power", true);
        showStoredPowerConfig = config.boolValue("show-stored-power", false);
        hideBackgroundConfig = config.boolValue("hide-background", false);
        alwaysShowFlowRateConfig = config.boolValue("always-show-flow-rate", true);
        hideZeroUnitsConfig = config.boolValue("hide-zero-units", true);
        hideZeroItemsConfig = config.boolValue("hide-zero-items", false);
        expandedConfig = config.boolValue("expanded", true);

        xConfig = config.floatValueKeyed(
                "x",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sw = getSceneWidth();
                    Float wFrac = overlayWidthConfig != null ? overlayWidthConfig.get() : 0.28f;
                    float width = sw * (wFrac != null ? wFrac : 0.28f);
                    float defX = sw > 0 ? Math.max(0f, (sw - width) / 2f) : 200f;
                    String oldKey = p ? "mindustrytool.team-resource.x.portrait"
                            : "mindustrytool.team-resource.x.landscape";
                    String groupKey = p ? "mindustrytool.features.team-resources.x.portrait"
                            : "mindustrytool.features.team-resources.x.landscape";
                    String fallbackKey1 = p ? "mindustrytool.team-resources.x.portrait"
                            : "mindustrytool.team-resources.x.landscape";
                    String fallbackKey2 = p ? "mindustrytool.team-resources.portrait.x"
                            : "mindustrytool.team-resources.landscape.x";
                    if (Core.settings.has(groupKey)) {
                        return Core.settings.getFloat(groupKey);
                    }
                    if (Core.settings.has(fallbackKey1)) {
                        return Core.settings.getFloat(fallbackKey1);
                    }
                    if (Core.settings.has(fallbackKey2)) {
                        return Core.settings.getFloat(fallbackKey2);
                    }
                    return Core.settings.getFloat(oldKey, defX);
                });

        yConfig = config.floatValueKeyed(
                "y",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sh = getSceneHeight();
                    float defY = sh > 0 ? sh / 2f : 200f;
                    String oldKey = p ? "mindustrytool.team-resource.y.portrait"
                            : "mindustrytool.team-resource.y.landscape";
                    String groupKey = p ? "mindustrytool.features.team-resources.y.portrait"
                            : "mindustrytool.features.team-resources.y.landscape";
                    String fallbackKey1 = p ? "mindustrytool.team-resources.y.portrait"
                            : "mindustrytool.team-resources.y.landscape";
                    String fallbackKey2 = p ? "mindustrytool.team-resources.portrait.y"
                            : "mindustrytool.team-resources.landscape.y";
                    if (Core.settings.has(groupKey)) {
                        return Core.settings.getFloat(groupKey);
                    }
                    if (Core.settings.has(fallbackKey1)) {
                        return Core.settings.getFloat(fallbackKey1);
                    }
                    if (Core.settings.has(fallbackKey2)) {
                        return Core.settings.getFloat(fallbackKey2);
                    }
                    return Core.settings.getFloat(oldKey, defY);
                });

        xSignal = xConfig.signal();
        ySignal = yConfig.signal();

        this.state = new TeamResourceState(this);

        Signals.isPortrait().subscribe(p -> {
            if (hudView != null) {
                Core.app.post(hudView::keepInScreen);
            }
        });
        Events.on(WorldLoadEvent.class, e -> state.onWorldLoad());
        Events.on(ResetEvent.class, e -> state.reset());
    }

    public float x() {
        Float val = xConfig.get();
        if (val != null) {
            return val;
        }
        float sw = getSceneWidth();
        Float wFrac = overlayWidthConfig != null ? overlayWidthConfig.get() : 0.28f;
        float width = sw * (wFrac != null ? wFrac : 0.28f);
        return sw > 0 ? Math.max(0f, (sw - width) / 2f) : 200f;
    }

    public void x(float value) {
        xConfig.set(value);
    }

    public float y() {
        Float val = yConfig.get();
        return val != null ? val : (getSceneHeight() > 0f ? getSceneHeight() / 2f : 200f);
    }

    public void y(float value) {
        yConfig.set(value);
    }

    public void updateOrientationPosition() {
        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
    }

    public void resetToDefaults() {
        opacityConfig.reset();
        scaleConfig.reset();
        overlayWidthConfig.reset();
        overlayHeightConfig.reset();
        showItemsConfig.reset();
        showUnitsConfig.reset();
        showPowerConfig.reset();
        showStoredPowerConfig.reset();
        hideBackgroundConfig.reset();
        alwaysShowFlowRateConfig.reset();
        hideZeroUnitsConfig.reset();
        hideZeroItemsConfig.reset();
        expandedConfig.reset();
    }

    public void resetPosition() {
        float sw = getSceneWidth();
        Float wFrac = overlayWidthConfig.get();
        float width = sw * (wFrac != null ? wFrac : 0.28f);
        float cx = sw > 0 ? Math.max(0f, (sw - width) / 2f) : 200f;
        float cy = getSceneHeight() > 0 ? getSceneHeight() / 2f : 200f;

        xConfig.set(cx);
        yConfig.set(cy);

        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
    }

    @Override
    public void onEnable() {
        if (Vars.ui != null && Vars.ui.hudGroup != null) {
            if (hudView != null) {
                hudView.element().remove();
                hudView.dispose();
            }

            state.reset();

            hudView = new TeamResourceHudView(this, state);
            Element el = hudView.element();
            el.name = "team-resources-hud";
            el.visible(() -> Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown && Vars.state != null
                    && Vars.state.isGame());

            Core.settings.put("coreitems", false);

            Core.app.post(() -> {
                if (hudView != null && Vars.ui != null && Vars.ui.hudGroup != null) {
                    Vars.ui.hudGroup.addChild(el);
                }
            });
        }
    }

    @Override
    public void onDisable() {
        if (hudView != null) {
            TeamResourceHudView view = hudView;
            hudView = null;
            Core.app.post(() -> {
                view.element().remove();
                view.dispose();
            });
        }
        Core.settings.put("coreitems", true);
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new TeamResourceSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    public TeamResourceState getState() {
        return state;
    }

    public @Nullable TeamResourceHudView getHudView() {
        return hudView;
    }

    private static Drawable getFeatureIcon() {
        try {
            Drawable icon = FileIcon.of("team-resources.png");
            if (icon != null) {
                return icon;
            }
        } catch (Throwable ignored) {
        }
        return Icon.layers != null ? Icon.layers : new TextureRegionDrawable();
    }

    public static float getSceneWidth() {
        if (Core.scene != null && Core.scene.getWidth() > 0f) {
            return Core.scene.getWidth();
        }
        float scl = Scl.scl();
        return (Core.graphics != null ? Core.graphics.getWidth() : 800f) / (scl > 0f ? scl : 1f);
    }

    public static float getSceneHeight() {
        if (Core.scene != null && Core.scene.getHeight() > 0f) {
            return Core.scene.getHeight();
        }
        float scl = Scl.scl();
        return (Core.graphics != null ? Core.graphics.getHeight() : 600f) / (scl > 0f ? scl : 1f);
    }
}

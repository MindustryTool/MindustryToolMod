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
import solim.reactive.Signal;
import solim.reactive.Signals;
import solim.core.Units;

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
        expandedConfig = config.boolValue("expanded", true);

        xConfig = config.floatValueKeyed(
                "x",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sw = Units.screenWidth();
                    float defX = sw > 0 ? sw / 2f : 200f;
                    String oldKey = p ? "mindustrytool.team-resource.x.portrait"
                            : "mindustrytool.team-resource.x.landscape";
                    String groupKey = p ? "mindustrytool.team-resources.portrait.x"
                            : "mindustrytool.team-resources.landscape.x";
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
                    String oldKey = p ? "mindustrytool.team-resource.y.portrait"
                            : "mindustrytool.team-resource.y.landscape";
                    String groupKey = p ? "mindustrytool.team-resources.portrait.y"
                            : "mindustrytool.team-resources.landscape.y";
                    if (Core.settings.has(groupKey)) {
                        return Core.settings.getFloat(groupKey);
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
        expandedConfig.reset();
    }

    public void resetPosition() {
        float cx = Units.screenWidth() / 2f;
        float cy = Units.screenHeight() / 2f;

        Core.settings.put("mindustrytool.team-resources.x.portrait", cx);
        Core.settings.put("mindustrytool.team-resources.x.landscape", cx);
        Core.settings.put("mindustrytool.team-resources.y.portrait", cy);
        Core.settings.put("mindustrytool.team-resources.y.landscape", cy);

        xConfig.reset();
        yConfig.reset();

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
}

package mindustrytool.features.teamresource;

import arc.Core;
import arc.Events;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import solim.overlay.SolimDialog;
import arc.util.Nullable;
import mindustry.Vars;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.ResizeEvent;
import mindustry.game.EventType.WorldLoadEvent;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.signal.Signal;

/**
 * Feature responsible for registering and managing the Team Resource Tracker overlay.
 * Follows Solim architecture and ConfigGroup reactive configuration.
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

    public final ConfigGroup portraitGroup;
    public final ConfigGroup landscapeGroup;

    public final ConfigValue<Float> portraitXConfig;
    public final ConfigValue<Float> portraitYConfig;
    public final ConfigValue<Float> landscapeXConfig;
    public final ConfigValue<Float> landscapeYConfig;

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

        portraitGroup = config.group("portrait");
        landscapeGroup = config.group("landscape");

        float defX = Core.graphics != null ? Core.graphics.getWidth() / 2f : 200f;
        float defY = Core.graphics != null ? Core.graphics.getHeight() / 2f : 200f;

        portraitXConfig = portraitGroup.floatValue("x", Core.settings.getFloat("mindustrytool.team-resource.x.portrait", defX));
        portraitYConfig = portraitGroup.floatValue("y", Core.settings.getFloat("mindustrytool.team-resource.y.portrait", defY));
        landscapeXConfig = landscapeGroup.floatValue("x", Core.settings.getFloat("mindustrytool.team-resource.x.landscape", defX));
        landscapeYConfig = landscapeGroup.floatValue("y", Core.settings.getFloat("mindustrytool.team-resource.y.landscape", defY));

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

        this.state = new TeamResourceState(this);

        Events.on(ResizeEvent.class, e -> updateOrientationPosition());
        Events.on(WorldLoadEvent.class, e -> state.onWorldLoad());
        Events.on(ResetEvent.class, e -> state.reset());
    }

    public ConfigValue<Float> currentXConfig() {
        return (Core.graphics != null && Core.graphics.isPortrait()) ? portraitXConfig : landscapeXConfig;
    }

    public ConfigValue<Float> currentYConfig() {
        return (Core.graphics != null && Core.graphics.isPortrait()) ? portraitYConfig : landscapeYConfig;
    }

    public float x() {
        Float val = currentXConfig().get();
        return val != null ? val : (Core.graphics != null ? Core.graphics.getWidth() / 2f : 200f);
    }

    public void x(float value) {
        currentXConfig().set(value);
        xSignal.set(value);
    }

    public float y() {
        Float val = currentYConfig().get();
        return val != null ? val : (Core.graphics != null ? Core.graphics.getHeight() / 2f : 200f);
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
        float cx = Core.graphics != null ? Core.graphics.getWidth() / 2f : 200f;
        float cy = Core.graphics != null ? Core.graphics.getHeight() / 2f : 200f;
        portraitXConfig.set(cx);
        portraitYConfig.set(cy);
        landscapeXConfig.set(cx);
        landscapeYConfig.set(cy);
        xSignal.set(cx);
        ySignal.set(cy);
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
            el.visible(() -> Vars.ui.hudfrag != null && Vars.ui.hudfrag.shown && Vars.state != null && Vars.state.isGame());

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
    public @Nullable SolimDialog getSettingDialog() {
        if (settingsDialog == null) {
            settingsDialog = new TeamResourceSettingsDialog(this);
        }
        return settingsDialog;
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

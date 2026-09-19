package mindustrytool.features.teamresource;

import arc.Core;
import arc.Events;
import arc.func.Prov;
import arc.math.Mathf;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
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
import solim.config.ContextualConfigValue;
import solim.overlay.SolimDialog;
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
    public final ConfigValue<Boolean> expandedConfig;

    public final ContextualConfigValue<Float, Boolean> xRatioConfig;
    public final ContextualConfigValue<Float, Boolean> yRatioConfig;

    public final ContextualConfigValue<Float, Boolean> xConfig;
    public final ContextualConfigValue<Float, Boolean> yConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    private boolean isDragging = false;
    private boolean isInternalPositionUpdate = false;

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

        xRatioConfig = config.floatValueKeyed(
                "x-ratio",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    String ratioKey = p ? "mindustrytool.features.team-resources.x-ratio.portrait"
                            : "mindustrytool.features.team-resources.x-ratio.landscape";
                    if (Core.settings.has(ratioKey)) {
                        return Core.settings.getFloat(ratioKey);
                    }

                    float sw = getSceneWidth();
                    Float wFrac = overlayWidthConfig != null ? overlayWidthConfig.get() : 0.28f;
                    float width = sw * (wFrac != null ? wFrac : 0.28f);
                    float availableW = Math.max(1f, sw - width);

                    String oldKey = p ? "mindustrytool.team-resource.x.portrait"
                            : "mindustrytool.team-resource.x.landscape";
                    String groupKey = p ? "mindustrytool.features.team-resources.x.portrait"
                            : "mindustrytool.features.team-resources.x.landscape";
                    String fallbackKey1 = p ? "mindustrytool.team-resources.x.portrait"
                            : "mindustrytool.team-resources.x.landscape";
                    String fallbackKey2 = p ? "mindustrytool.team-resources.portrait.x"
                            : "mindustrytool.team-resources.landscape.x";

                    if (Core.settings.has(groupKey)) {
                        float val = Core.settings.getFloat(groupKey);
                        return val <= 1.0f && val >= 0f ? val : Mathf.clamp(val / availableW, 0f, 1f);
                    }
                    if (Core.settings.has(fallbackKey1)) {
                        float val = Core.settings.getFloat(fallbackKey1);
                        return val <= 1.0f && val >= 0f ? val : Mathf.clamp(val / availableW, 0f, 1f);
                    }
                    if (Core.settings.has(fallbackKey2)) {
                        float val = Core.settings.getFloat(fallbackKey2);
                        return val <= 1.0f && val >= 0f ? val : Mathf.clamp(val / availableW, 0f, 1f);
                    }
                    if (Core.settings.has(oldKey)) {
                        float val = Core.settings.getFloat(oldKey);
                        return val <= 1.0f && val >= 0f ? val : Mathf.clamp(val / availableW, 0f, 1f);
                    }
                    return 0.5f;
                });

        yRatioConfig = config.floatValueKeyed(
                "y-ratio",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    String ratioKey = p ? "mindustrytool.features.team-resources.y-ratio.portrait"
                            : "mindustrytool.features.team-resources.y-ratio.landscape";
                    if (Core.settings.has(ratioKey)) {
                        return Core.settings.getFloat(ratioKey);
                    }

                    float sh = getSceneHeight();
                    float h = getHudHeight();
                    float availableH = Math.max(1f, sh - h);

                    String oldKey = p ? "mindustrytool.team-resource.y.portrait"
                            : "mindustrytool.team-resource.y.landscape";
                    String groupKey = p ? "mindustrytool.features.team-resources.y.portrait"
                            : "mindustrytool.features.team-resources.y.landscape";
                    String fallbackKey1 = p ? "mindustrytool.team-resources.y.portrait"
                            : "mindustrytool.team-resources.y.landscape";
                    String fallbackKey2 = p ? "mindustrytool.team-resources.portrait.y"
                            : "mindustrytool.team-resources.landscape.y";

                    if (Core.settings.has(groupKey)) {
                        float val = Core.settings.getFloat(groupKey);
                        return val <= 1.0f && val >= 0f ? val : Mathf.clamp(val / availableH, 0f, 1f);
                    }
                    if (Core.settings.has(fallbackKey1)) {
                        float val = Core.settings.getFloat(fallbackKey1);
                        return val <= 1.0f && val >= 0f ? val : Mathf.clamp(val / availableH, 0f, 1f);
                    }
                    if (Core.settings.has(fallbackKey2)) {
                        float val = Core.settings.getFloat(fallbackKey2);
                        return val <= 1.0f && val >= 0f ? val : Mathf.clamp(val / availableH, 0f, 1f);
                    }
                    if (Core.settings.has(oldKey)) {
                        float val = Core.settings.getFloat(oldKey);
                        return val <= 1.0f && val >= 0f ? val : Mathf.clamp(val / availableH, 0f, 1f);
                    }
                    return 0.5f;
                });

        xConfig = config.floatValueKeyed(
                "x",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sw = getSceneWidth();
                    Float wFrac = overlayWidthConfig != null ? overlayWidthConfig.get() : 0.28f;
                    float width = sw * (wFrac != null ? wFrac : 0.28f);
                    float availableW = Math.max(0f, sw - width);
                    Float rx = xRatioConfig.get();
                    return (rx != null ? rx : 0.5f) * availableW;
                });

        yConfig = config.floatValueKeyed(
                "y",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sh = getSceneHeight();
                    float h = getHudHeight();
                    Float ry = yRatioConfig.get();
                    return (ry != null ? ry : 0.5f) * Math.max(0f, sh - h);
                });

        xSignal = xConfig.signal();
        ySignal = yConfig.signal();

        xSignal.subscribe(px -> {
            if (!isInternalPositionUpdate && isDragging() && px != null) {
                saveRatioFromCurrentPosition();
            }
        });
        ySignal.subscribe(py -> {
            if (!isInternalPositionUpdate && isDragging() && py != null) {
                saveRatioFromCurrentPosition();
            }
        });

        this.state = new TeamResourceState(this);

        Signals.isPortrait().subscribe(p -> {
            Core.app.post(this::updatePositionFromRatio);
        });
        overlayWidthConfig.signal().subscribe(w -> {
            Core.app.post(this::updatePositionFromRatio);
        });
        scaleConfig.signal().subscribe(s -> {
            Core.app.post(this::updatePositionFromRatio);
        });
        expandedConfig.signal().subscribe(exp -> {
            Core.app.post(this::updatePositionFromRatio);
        });

        Events.on(ResizeEvent.class, e -> {
            isInternalPositionUpdate = true;
            try {
                updatePositionFromRatio();
            } finally {
                isInternalPositionUpdate = false;
            }
            Core.app.post(() -> {
                isInternalPositionUpdate = true;
                try {
                    updatePositionFromRatio();
                } finally {
                    isInternalPositionUpdate = false;
                }
            });
        });
        Events.on(WorldLoadEvent.class, e -> state.onWorldLoad());
        Events.on(ResetEvent.class, e -> state.reset());
    }

    public void setIsDragging(boolean dragging) {
        this.isDragging = dragging;
    }

    public boolean isDragging() {
        if (isDragging) {
            return true;
        }
        if (hudView != null && hudView.getHud() != null && hudView.getHud().isDragging()) {
            return true;
        }
        return false;
    }

    public float getXRatio() {
        Float r = xRatioConfig.get();
        return r != null ? Mathf.clamp(r, 0f, 1f) : 0.5f;
    }

    public float getYRatio() {
        Float r = yRatioConfig.get();
        return r != null ? Mathf.clamp(r, 0f, 1f) : 0.5f;
    }

    public float getHudWidth() {
        if (hudView != null && hudView.getHud() != null && hudView.getHud().root().getWidth() > 0) {
            return hudView.getHud().root().getWidth();
        }
        float sw = getSceneWidth();
        Float wFrac = overlayWidthConfig != null ? overlayWidthConfig.get() : 0.28f;
        return sw * (wFrac != null ? wFrac : 0.28f);
    }

    public float getHudHeight() {
        if (hudView != null && hudView.getHud() != null && hudView.getHud().root().getHeight() > 0) {
            return hudView.getHud().root().getHeight();
        }
        return 0f;
    }

    public void saveRatioFromCurrentPosition() {
        float sw = getSceneWidth();
        float sh = getSceneHeight();
        if (sw <= 100f || sh <= 100f) {
            return;
        }

        if (hudView != null && hudView.getHud() != null) {
            Table root = hudView.getHud().root();
            if (root.needsLayout() || root.getWidth() <= 0 || root.getHeight() <= 0) {
                root.pack();
            }
        }

        float w = getHudWidth();
        float h = getHudHeight();

        float curX = (hudView != null && hudView.getHud() != null) ? hudView.getHud().root().x : x();
        float curY = (hudView != null && hudView.getHud() != null) ? hudView.getHud().root().y : y();

        float availableW = Math.max(1f, sw - w);
        float availableH = Math.max(1f, sh - h);

        float rx = Mathf.clamp(curX / availableW, 0f, 1f);
        float ry = Mathf.clamp(curY / availableH, 0f, 1f);

        isInternalPositionUpdate = true;
        try {
            xRatioConfig.set(rx);
            yRatioConfig.set(ry);
            xConfig.set(curX);
            yConfig.set(curY);
        } finally {
            isInternalPositionUpdate = false;
        }
    }

    public void updatePositionFromRatio() {
        float sw = getSceneWidth();
        float sh = getSceneHeight();
        if (sw <= 100f || sh <= 100f) {
            return;
        }

        if (hudView != null && hudView.getHud() != null) {
            Table root = hudView.getHud().root();
            if (root.needsLayout() || root.getWidth() <= 0 || root.getHeight() <= 0) {
                root.pack();
            }
        }

        float w = getHudWidth();
        float h = getHudHeight();

        float rx = getXRatio();
        float ry = getYRatio();

        float targetX = rx * Math.max(0f, sw - w);
        float targetY = ry * Math.max(0f, sh - h);

        isInternalPositionUpdate = true;
        try {
            xSignal.set(targetX);
            ySignal.set(targetY);
            xConfig.set(targetX);
            yConfig.set(targetY);

            if (hudView != null && hudView.getHud() != null) {
                hudView.getHud().root().setPosition(targetX, targetY);
            }
        } finally {
            isInternalPositionUpdate = false;
        }
    }

    public float x() {
        if (xSignal != null && xSignal.get() != null) {
            return xSignal.get();
        }
        float sw = getSceneWidth();
        float w = getHudWidth();
        return getXRatio() * Math.max(0f, sw - w);
    }

    public void x(float value) {
        isInternalPositionUpdate = true;
        try {
            if (hudView != null && hudView.getHud() != null) {
                Table root = hudView.getHud().root();
                if (root.needsLayout() || root.getWidth() <= 0 || root.getHeight() <= 0) {
                    root.pack();
                }
            }
            float sw = getSceneWidth();
            float w = getHudWidth();
            float availableW = Math.max(1f, sw - w);
            float rx = Mathf.clamp(value / availableW, 0f, 1f);
            xRatioConfig.set(rx);
            xConfig.set(value);
            xSignal.set(value);
            if (hudView != null && hudView.getHud() != null) {
                hudView.getHud().root().x = value;
                Core.app.post(hudView::keepInScreen);
            }
        } finally {
            isInternalPositionUpdate = false;
        }
    }

    public float y() {
        if (ySignal != null && ySignal.get() != null) {
            return ySignal.get();
        }
        float sh = getSceneHeight();
        float h = getHudHeight();
        return getYRatio() * Math.max(0f, sh - h);
    }

    public void y(float value) {
        isInternalPositionUpdate = true;
        try {
            if (hudView != null && hudView.getHud() != null) {
                Table root = hudView.getHud().root();
                if (root.needsLayout() || root.getWidth() <= 0 || root.getHeight() <= 0) {
                    root.pack();
                }
            }
            float sh = getSceneHeight();
            float h = getHudHeight();
            float availableH = Math.max(1f, sh - h);
            float ry = Mathf.clamp(value / availableH, 0f, 1f);
            yRatioConfig.set(ry);
            yConfig.set(value);
            ySignal.set(value);
            if (hudView != null && hudView.getHud() != null) {
                hudView.getHud().root().y = value;
                Core.app.post(hudView::keepInScreen);
            }
        } finally {
            isInternalPositionUpdate = false;
        }
    }

    public void updateOrientationPosition() {
        updatePositionFromRatio();
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
        xRatioConfig.reset();
        yRatioConfig.reset();
        xConfig.reset();
        yConfig.reset();

        float sw = getSceneWidth();
        float sh = getSceneHeight();
        float w = getHudWidth();
        float h = getHudHeight();

        float cx = sw > 0 ? Math.max(0f, (sw - w) / 2f) : 200f;
        float cy = sh > 0 ? Math.max(0f, (sh - h) / 2f) : 200f;

        isInternalPositionUpdate = true;
        try {
            xSignal.set(cx);
            ySignal.set(cy);
            if (hudView != null && hudView.getHud() != null) {
                hudView.getHud().root().setPosition(cx, cy);
                Core.app.post(hudView::keepInScreen);
            }
        } finally {
            isInternalPositionUpdate = false;
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
                    updatePositionFromRatio();
                }
            });
        }
    }

    @Override
    public void onDisable() {
        if (hudView != null) {
            hudView.element().remove();
            hudView.dispose();
            hudView = null;
        }
        Core.settings.put("coreitems", true);
    }

    public TeamResourceState getState() {
        return state;
    }

    public @Nullable TeamResourceHudView getHudView() {
        return hudView;
    }

    public static float getSceneWidth() {
        if (Core.scene != null && Core.scene.getWidth() > 0f) {
            return Core.scene.getWidth();
        }
        float scl = Scl.scl();
        return Core.graphics != null ? Core.graphics.getWidth() / (scl > 0f ? scl : 1f) : 800f;
    }

    public static float getSceneHeight() {
        if (Core.scene != null && Core.scene.getHeight() > 0f) {
            return Core.scene.getHeight();
        }
        float scl = Scl.scl();
        return Core.graphics != null ? Core.graphics.getHeight() / (scl > 0f ? scl : 1f) : 600f;
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

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new TeamResourceSettingsDialog(this);
            }
            return settingsDialog;
        };
    }
}

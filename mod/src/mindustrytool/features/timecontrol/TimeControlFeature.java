package mindustrytool.features.timecontrol;

import arc.Core;
import arc.func.Prov;
import arc.math.Mathf;
import arc.scene.Element;
import arc.util.Nullable;
import arc.util.Time;
import mindustry.Vars;
import mindustrytool.components.FileIcon;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.FeatureMetadata;
import mindustrytool.features.PopupDisplayFeature;
import mindustrytool.features.quickaccess.QuickAccessFeature;
import solim.config.ConfigGroup;
import solim.config.ConfigValue;
import solim.config.ContextualConfigValue;

import solim.overlay.SolimDialog;
import solim.signal.Signal;
import solim.signal.Signals;
import solim.ui.Units;

/**
 * Controls game speed with a standalone draggable HUD. Applies only while
 * hosting or in single-player; speed is ephemeral and resets to 1x on disable,
 * world exit, client sessions, and interaction-mode switches.
 */
public class TimeControlFeature extends Feature implements PopupDisplayFeature {

    public static final float[] SPEEDS = { 0.125f, 0.5f, 1f, 2f, 8f };
    public static final float SLIDER_MIN_U = -1f;
    public static final float SLIDER_MAX_U = 1f;
    public static final float SLIDER_STEP_U = 0.1f;
    public static final float SLIDER_MIN_SPEED = 0.125f;
    public static final float SLIDER_MAX_SPEED = 8f;
    /**
     * Clamp applied after multiplication; 4x the top-preset nominal frame,
     * mirroring vanilla headroom.
     */
    public static final float MAX_STEP = 32f;
    public static final String MODE_PRESETS = "presets";
    public static final String MODE_SLIDER = "slider";
    public static final String DISPLAY_HUD = "hud";
    public static final String DISPLAY_POPUP = "popup";

    public final ConfigGroup config;
    public final ConfigValue<String> modeConfig;
    public final ConfigValue<String> displayModeConfig;
    public final ConfigValue<Float> scaleConfig;
    public final ConfigValue<Boolean> hideDragHandleConfig;

    public final ConfigGroup positionGroup;
    public final ContextualConfigValue<Float, Boolean> xConfig;
    public final ContextualConfigValue<Float, Boolean> yConfig;

    public final Signal<Float> xSignal;
    public final Signal<Float> ySignal;

    private final Signal<Float> speed = Signal.of(1f);
    private final Signal<Float> selectedPreset = Signal.of(1f);
    private final Signal<Boolean> boosted = Signal.of(false);
    private final Signal<Float> sliderPosition = Signal.of(0f);

    private @Nullable TimeControlHudView hudView;
    private @Nullable TimeControlSettingsDialog settingsDialog;

    public TimeControlFeature() {
        super(FeatureMetadata.builder()
                .id("time-control")
                .icon(FileIcon.of("clock.png"))
                .order(1)
                .enabledByDefault(false)
                .quickAccess(true)
                .build());

        config = configGroup();

        modeConfig = config.stringValue("mode", MODE_PRESETS);
        displayModeConfig = config.stringValue("displayMode", DISPLAY_POPUP);
        scaleConfig = config.floatValue("scale", 1f);
        hideDragHandleConfig = config.boolValue("hideDragHandle", false);

        positionGroup = config.group("position");

        xConfig = positionGroup.floatValueKeyed(
                "x",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sw = Units.screenWidth();
                    return sw > 0 ? sw / 2f : 400f;
                });
        yConfig = positionGroup.floatValueKeyed(
                "y",
                Signals.isPortrait(),
                p -> p ? "portrait" : "landscape",
                p -> {
                    float sh = Units.screenHeight();
                    return sh > 0 ? sh / 2f : 250f;
                });

        xSignal = xConfig.signal();
        ySignal = yConfig.signal();

        speed.subscribe(value -> {
            if (canApply()) {
                applyProvider(value != null ? value : 1f);
            }
        });

        sliderPosition.subscribe(position -> {
            if (canApply()) {
                speed.set(speedFromSlider(position != null ? position : 0f));
            }
        });

        Signals.netClient().subscribe(client -> {
            if (Boolean.TRUE.equals(client)) {
                resetSpeed();
            }
        });

        modeConfig.signal().subscribe(mode -> resetSpeed());
        displayModeConfig.signal().subscribe(mode -> updateHud());
    }

    /**
     * Effective multiplier for a preset with the legacy double-tap boost; 1x never
     * boosts.
     */
    public static float effectiveSpeed(float preset, boolean isBoosted) {
        return !isBoosted || Float.compare(preset, 1f) == 0 ? preset : preset >= 1f ? preset * 2f : preset / 2f;
    }

    /**
     * Two-sided-quadratic slider map around 1x; ratio-symmetric with u = 0 exactly
     * 1x.
     */
    public static float speedFromSlider(float position) {
        float squared = position * position;
        return position >= 0f ? 1f + squared * (SLIDER_MAX_SPEED - 1f)
                : 1f / (1f + squared * (1f / SLIDER_MIN_SPEED - 1f));
    }

    /**
     * Inverse of {@link #speedFromSlider(float)}; maps a speed back to slider
     * position.
     */
    public static float sliderFromSpeed(float value) {
        return value >= 1f ? Mathf.sqrt((value - 1f) / (SLIDER_MAX_SPEED - 1f))
                : -Mathf.sqrt((1f / value - 1f) / (1f / SLIDER_MIN_SPEED - 1f));
    }

    /** Short display form such as 8x or 0.25x. */
    public static String formatSpeed(float value) {
        float rounded = Math.round(value * 100f) / 100f;
        return rounded == (int) rounded ? String.valueOf((int) rounded) : String.valueOf(rounded);
    }

    public Signal<Float> speedSignal() {
        return speed;
    }

    public Signal<Float> selectedPresetSignal() {
        return selectedPreset;
    }

    public Signal<Boolean> boostedSignal() {
        return boosted;
    }

    /**
     * Returns the intermediate slider position signal (u in [-1, 1]); use this to
     * bind the slider widget.
     */
    public Signal<Float> sliderPositionSignal() {
        return sliderPosition;
    }

    public boolean isPresetMode() {
        return !MODE_SLIDER.equals(modeConfig.get());
    }

    public void selectPreset(float preset) {
        if (!canApply()) {
            return;
        }
        if (Float.compare(preset, 1f) == 0) {
            // 1x is boost-locked: always resets to 1x and clears boost; double-tap is a
            // no-op.
            selectedPreset.set(1f);
            boosted.set(false);
            speed.set(1f);
        } else if (Float.compare(preset, selectedPreset.peek()) == 0) {
            // Same non-1x preset tapped again: toggle boost.
            boosted.set(!Boolean.TRUE.equals(boosted.peek()));
            speed.set(effectiveSpeed(preset, Boolean.TRUE.equals(boosted.peek())));
        } else {
            selectedPreset.set(preset);
            boosted.set(false);
            speed.set(preset);
        }
    }

    /**
     * Resets slider position to 0 (exactly 1x), routing through position signal so
     * widget and speed never desync. Use this for all slider-mode resets.
     */
    public void resetSlider() {
        sliderPosition.set(0f);
        // speed will be updated reactively via the sliderPosition subscriber.
    }

    public void resetSpeed() {
        selectedPreset.set(1f);
        boosted.set(false);
        // Route through sliderPosition so slider widget stays in sync in slider mode.
        sliderPosition.set(0f);
        speed.set(1f);
        restoreDefaultProvider();
    }

    public void resetPosition() {
        float sw = Units.screenWidth();
        float sh = Units.screenHeight();
        float cx = sw > 0 ? sw / 2f : 400f;
        float cy = sh > 0 ? sh / 2f : 250f;

        Core.settings.put("mindustrytool.time-control.position.x.portrait", cx);
        Core.settings.put("mindustrytool.time-control.position.x.landscape", cx);
        Core.settings.put("mindustrytool.time-control.position.y.portrait", cy);
        Core.settings.put("mindustrytool.time-control.position.y.landscape", cy);

        xConfig.reset();
        yConfig.reset();

        if (hudView != null) {
            Core.app.post(hudView::keepInScreen);
        }
    }

    public boolean isPopupMode() {
        return DISPLAY_POPUP.equals(displayModeConfig.get());
    }

    public boolean isPopupActive() {
        if (!isPopupMode()) {
            return false;
        }
        try {
            QuickAccessFeature quickAccess = FeatureManager.getFeature(QuickAccessFeature.class);
            return quickAccess != null && quickAccess.isEnabled();
        } catch (Exception ignored) {
            return false;
        }
    }

    @Override
    public void togglePopup(@Nullable Element quickAccessBar) {
        TimeControlPopup.toggle(this, quickAccessBar);
    }

    @Override
    public void openPopup(@Nullable Element quickAccessBar) {
        TimeControlPopup.toggle(this, quickAccessBar);
    }

    @Override
    public void onEnable() {
        updateHud();
    }

    @Override
    public void onDisable() {
        resetSpeed();
        removeHud();
    }

    private boolean quickAccessHooked = false;

    private void updateHud() {
        ensureQuickAccessHook();
        if (!isEnabled() || isPopupActive()) {
            removeHud();
            return;
        }
        ensureHud();
    }

    private void ensureQuickAccessHook() {
        if (quickAccessHooked) {
            return;
        }
        try {
            QuickAccessFeature quickAccess = FeatureManager.getFeature(QuickAccessFeature.class);
            if (quickAccess == null) {
                return;
            }
            quickAccessHooked = true;
            quickAccess.enabled().subscribe(value -> updateHud());
        } catch (Exception ignored) {
        }
    }

    private void ensureHud() {
        if (hudView != null) {
            return;
        }
        hudView = new TimeControlHudView(this);
        Element el = hudView.element();
        el.name = "time-control-hud";
        el.visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame()
                && Boolean.FALSE.equals(Signals.netClient().peek()));

        Core.app.post(() -> {
            if (hudView != null && !isPopupActive()) {
                Vars.ui.hudGroup.addChild(el);
            }
        });
    }

    private void removeHud() {
        if (hudView == null) {
            return;
        }
        TimeControlHudView view = hudView;
        hudView = null;
        Core.app.post(() -> {
            view.element().remove();
            view.dispose();
        });
    }

    @Override
    public @Nullable Prov<SolimDialog> getSettingDialog() {
        return () -> {
            if (settingsDialog == null) {
                settingsDialog = new TimeControlSettingsDialog(this);
            }
            return settingsDialog;
        };
    }

    private boolean canApply() {
        return Boolean.FALSE.equals(Signals.netClient().peek()) && !Vars.net.client();
    }

    private void applyProvider(float multiplier) {
        float m = multiplier;
        Time.setDeltaProvider(() -> {
            float result = Core.graphics.getDeltaTime() * 60f * m;
            return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f : Mathf.clamp(result, 0.0001f, MAX_STEP);
        });
    }

    private void restoreDefaultProvider() {
        // Time exposes no getter or reset; restate the vanilla ClientLauncher form
        // verbatim.
        Time.setDeltaProvider(() -> {
            float result = Core.graphics.getDeltaTime() * 60f;
            return (Float.isNaN(result) || Float.isInfinite(result)) ? 1f
                    : Mathf.clamp(result, 0.0001f, Vars.maxDeltaClient);
        });
    }
}

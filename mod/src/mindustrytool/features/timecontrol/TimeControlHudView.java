package mindustrytool.features.timecontrol;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.game.EventType.ClientServerConnectEvent;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import mindustrytool.components.WebStyles;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.input.Button;
import solim.input.SolimSlider;
import solim.overlay.Hud;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;

/**
 * Standalone reactive TimeControl HUD overlay. Shows preset buttons or a slider
 * depending on the interaction mode; all state flows through the parent feature
 * signals with automatic bindings. Compact footprint at scale 1.0: ~320x40px
 * presets, ~280x40px slider; button, icon, and font sizes scale proportionally.
 */
public class TimeControlHudView extends BaseComponent {

    private final TimeControlFeature parentFeature;
    private @Nullable Hud hud;

    public TimeControlHudView(TimeControlFeature parentFeature) {
        this.parentFeature = parentFeature;
    }

    @Override
    protected Element build() {
        Readable<Float> scale = parentFeature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> dragIconSize = scale.map(s -> unit(7) * (s != null ? s : 1f));

        hud = hud(() -> {
            dynamic(parentFeature.hideDragHandleConfig.signal(), hide -> {
                if (!Boolean.TRUE.equals(hide)) {
                    return button()
                            .style(Styles.clearNonei)
                            .background(Styles.black6)
                            .size(buttonSize)
                            .children(() -> icon(Icon.move).size(dragIconSize))
                            .draggable(parentFeature.xSignal, parentFeature.ySignal);
                }
                return null;
            });

            buildControls(parentFeature, null);
        }).gap(unit(1));

        hud.position(parentFeature.xSignal, parentFeature.ySignal);
        hud.background(parentFeature.modeConfig.signal()
                .map(mode -> TimeControlFeature.MODE_SLIDER.equals(mode) ? Tex.clear : Styles.black6));

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });
        listen(ResetEvent.class, e -> parentFeature.resetSpeed());
        listen(ClientServerConnectEvent.class, e -> parentFeature.resetSpeed());

        effect(() -> {
            scale.get();
            Core.app.post(this::keepInScreen);
        });

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    public static Component buildControls(TimeControlFeature feature, @Nullable Readable<Boolean> canEdit) {
        Readable<Float> scale = feature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> presetWidth = scale.map(s -> unit(15) * (s != null ? s : 1f));
        Readable<Float> resetIconSize = scale.map(s -> unit(7) * (s != null ? s : 1f));
        Readable<Float> fontScale = scale.map(s -> s != null ? s : 1f);

        return dynamic(feature.modeConfig.signal(),
                mode -> buildModeContent(feature, mode, buttonSize, presetWidth, resetIconSize, fontScale, canEdit));
    }

    private static Component buildModeContent(TimeControlFeature feature, String mode, Readable<Float> buttonSize,
            Readable<Float> presetWidth, Readable<Float> resetIconSize, Readable<Float> fontScale,
            @Nullable Readable<Boolean> canEdit) {

        return TimeControlFeature.MODE_SLIDER.equals(mode)
                ? buildSliderContent(feature, buttonSize, resetIconSize, fontScale, canEdit)
                : buildPresetContent(feature, buttonSize, presetWidth, fontScale, canEdit);
    }

    private static Component buildPresetContent(TimeControlFeature feature, Readable<Float> buttonSize,
            Readable<Float> presetWidth, Readable<Float> fontScale, @Nullable Readable<Boolean> canEdit) {
        return row()
                .children(() -> {
                    for (float preset : TimeControlFeature.SPEEDS) {
                        presetButton(feature, preset, buttonSize, presetWidth, fontScale, canEdit);
                    }
                });
    }

    private static Component presetButton(TimeControlFeature feature, float preset, Readable<Float> buttonSize,
            Readable<Float> presetWidth, Readable<Float> fontScale, @Nullable Readable<Boolean> canEdit) {

        Computed<String> label = Signal.computed(() -> {
            float selected = feature.selectedPresetSignal().get();
            boolean isBoosted = Boolean.TRUE.equals(feature.boostedSignal().get());
            float shown = Float.compare(preset, selected) == 0 && isBoosted
                    ? TimeControlFeature.effectiveSpeed(preset, true)
                    : preset;
            
            return Core.bundle.format("feature.time-control.speed.format", TimeControlFeature.formatSpeed(shown));
        });

        Computed<Color> color = Signal.computed(() -> {
            boolean isSelected = Float.compare(preset, feature.selectedPresetSignal().get()) == 0;
            if (!isSelected) {
                return Pal.gray;
            }
            return Boolean.TRUE.equals(feature.boostedSignal().get()) ? Pal.accent : Color.white;
        });

        Button btn = button()
                .style(WebStyles.ghost())
                .height(buttonSize)
                .width(presetWidth)
                .onClick(() -> feature.selectPreset(preset))
                .children(() -> text(label).color(color).fontScale(fontScale));
        return canEdit != null ? btn.enabled(canEdit) : btn;
    }

    private static Component buildSliderContent(TimeControlFeature feature, Readable<Float> buttonSize,
            Readable<Float> resetIconSize, Readable<Float> fontScale, @Nullable Readable<Boolean> canEdit) {
        // Fixed-width reactive speed label so bar width never changes as value changes.
        Computed<String> label = feature.speedSignal()
                .map(value -> Core.bundle.format("feature.time-control.speed.format",
                        TimeControlFeature.formatSpeed(value != null ? value : 1f)));
        Readable<Float> sliderHeight = buttonSize.map(h -> (h != null ? h : unit(10)) * 0.9f);

        return row()
                .gap(unit(1))
                .marginLeft(unit(1))
                .children(() -> {
                    // Slider binds to the intermediate u-space position signal (single-directional:
                    // u -> speed).

                    row().background(Styles.black6).height(buttonSize).center().children(() -> {
                        SolimSlider slider = slider(feature.sliderPositionSignal(),
                                TimeControlFeature.SLIDER_MIN_U,
                                TimeControlFeature.SLIDER_MAX_U,
                                TimeControlFeature.SLIDER_STEP_U);

                        slider.slider().setWidth(unit(70));
                        effect(() -> {
                            if (canEdit != null) {
                                Boolean editable = canEdit.get();
                                slider.slider().setDisabled(!Boolean.TRUE.equals(editable));
                            }
                            Float sliderHeightValue = sliderHeight.get();
                            slider.slider().setHeight(
                                    sliderHeightValue != null ? sliderHeightValue : unit(9));
                        });
                    });

                    row().background(Styles.black6).center().height(buttonSize).width(unit(20)).children(() -> {
                        text(label).center().fontScale(fontScale);
                    });

                    // Reset button: zeroes slider position so widget and speed land exactly on 1x.
                    Button resetBtn = button()
                            .style(Styles.clearNonei)
                            .background(Styles.black6)
                            .size(buttonSize)
                            .tooltip(Core.bundle.get("feature.time-control.slider.reset.tooltip"))
                            .onClick(() -> feature.resetSlider())
                            .children(() -> icon(Icon.refresh).size(resetIconSize));
                    if (canEdit != null) {
                        resetBtn.enabled(canEdit);
                    }
                });
    }

    public @Nullable Hud getHud() {
        return hud;
    }

    public void keepInScreen() {
        if (hud != null) {
            hud.keepInScreen();
        }
    }
}

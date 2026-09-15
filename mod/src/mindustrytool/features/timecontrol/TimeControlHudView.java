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
import solim.core.BaseComponent;
import solim.core.Component;
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
        Readable<Float> buttonSize = scale.map(s -> unit(11) * s);
        Readable<Float> presetWidth = scale.map(s -> unit(15) * s);
        Readable<Float> dragIconSize = scale.map(s -> unit(7) * s);
        Readable<Float> resetIconSize = scale.map(s -> unit(7) * s);
        Readable<Float> fontScale = scale.map(s -> s != null ? s : 1f);

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

            dynamic(parentFeature.modeConfig.signal(),
                    mode -> buildModeContent(mode, buttonSize, presetWidth, resetIconSize, fontScale));
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

    private Component buildModeContent(String mode, Readable<Float> buttonSize, Readable<Float> presetWidth,
            Readable<Float> resetIconSize, Readable<Float> fontScale) {
        return TimeControlFeature.MODE_SLIDER.equals(mode)
                ? buildSliderContent(buttonSize, resetIconSize, fontScale)
                : buildPresetContent(buttonSize, presetWidth, fontScale);
    }

    private Component buildPresetContent(Readable<Float> buttonSize, Readable<Float> presetWidth,
            Readable<Float> fontScale) {
        return row()
                .children(() -> {
                    for (float preset : TimeControlFeature.SPEEDS) {
                        presetButton(preset, buttonSize, presetWidth, fontScale);
                    }
                });
    }

    private Component presetButton(float preset, Readable<Float> buttonSize, Readable<Float> presetWidth,
            Readable<Float> fontScale) {
        Computed<String> label = Signal.computed(() -> {
            float selected = parentFeature.selectedPresetSignal().get();
            boolean isBoosted = Boolean.TRUE.equals(parentFeature.boostedSignal().get());
            float shown = Float.compare(preset, selected) == 0 && isBoosted
                    ? TimeControlFeature.effectiveSpeed(preset, true)
                    : preset;
            return Core.bundle.format("feature.time-control.speed.format", TimeControlFeature.formatSpeed(shown));
        });

        Computed<Color> color = Signal.computed(() -> {
            boolean isSelected = Float.compare(preset, parentFeature.selectedPresetSignal().get()) == 0;
            if (!isSelected) {
                return Pal.gray;
            }
            return Boolean.TRUE.equals(parentFeature.boostedSignal().get()) ? Pal.accent : Color.white;
        });

        return button()
                .style(Styles.cleart)
                .height(buttonSize)
                .width(presetWidth)
                .onClick(() -> parentFeature.selectPreset(preset))
                .children(() -> text(label).color(color).fontScale(fontScale));
    }

    private Component buildSliderContent(Readable<Float> buttonSize, Readable<Float> resetIconSize,
            Readable<Float> fontScale) {
        // Fixed-width reactive speed label so bar width never changes as value changes.
        Computed<String> label = parentFeature.speedSignal()
                .map(value -> Core.bundle.format("feature.time-control.speed.format",
                        TimeControlFeature.formatSpeed(value != null ? value : 1f)));
        Readable<Float> sliderHeight = buttonSize.map(h -> (h != null ? h : unit(10)) * 0.9f);

        return row()
                .gap(unit(1))
                .cellPaddingLeft(unit(1))
                .children(() -> {
                    // Slider binds to the intermediate u-space position signal (single-directional:
                    // u -> speed).

                    row().background(Styles.black6).height(buttonSize).center().children(() -> {
                        SolimSlider slider = slider(parentFeature.sliderPositionSignal(),
                                TimeControlFeature.SLIDER_MIN_U,
                                TimeControlFeature.SLIDER_MAX_U,
                                TimeControlFeature.SLIDER_STEP_U);

                        slider.slider().setWidth(unit(70));
                        effect(() -> {
                            Float sliderHeightValue = sliderHeight.get();
                            slider.slider().setHeight(
                                    sliderHeightValue != null ? sliderHeightValue : unit(9));
                        });
                    });

                    row().background(Styles.black6).center().height(buttonSize).width(unit(20)).children(() -> {
                        text(label).center().fontScale(fontScale);
                    });

                    // Reset button: zeroes slider position so widget and speed land exactly on 1x.
                    button()
                            .style(Styles.clearNonei)
                            .background(Styles.black6)
                            .size(buttonSize)
                            .tooltip(Core.bundle.get("feature.time-control.slider.reset.tooltip"))
                            .onClick(() -> parentFeature.resetSlider())
                            .children(() -> icon(Icon.refresh).size(resetIconSize));
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

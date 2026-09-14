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
import solim.signal.Signal;

/**
 * Standalone reactive TimeControl HUD overlay. Shows preset buttons or a slider
 * depending on the interaction mode; all state flows through the parent feature
 * signals with automatic bindings. Compact footprint: ~320x40px presets,
 * ~280x40px slider.
 */
public class TimeControlHudView extends BaseComponent {

    private final TimeControlFeature parentFeature;
    private @Nullable Hud hud;

    public TimeControlHudView(TimeControlFeature parentFeature) {
        this.parentFeature = parentFeature;
    }

    @Override
    protected Element build() {
        hud = hud(() -> {
            button()
                    .style(Styles.clearNonei)
                    .background(Styles.black6)
                    .size(unit(10))
                    .children(() -> icon(Icon.move).size(unit(8)))
                    .draggable(parentFeature.xSignal, parentFeature.ySignal);

            dynamic(parentFeature.modeConfig.signal(), this::buildModeContent);
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

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    private Component buildModeContent(String mode) {
        return TimeControlFeature.MODE_SLIDER.equals(mode) ? buildSliderContent() : buildPresetContent();
    }

    private Component buildPresetContent() {
        return row()
                .children(() -> {
                    for (float preset : TimeControlFeature.SPEEDS) {
                        presetButton(preset);
                    }
                });
    }

    private Component presetButton(float preset) {
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
                .height(unit(10))
                .width(unit(15))
                .onClick(() -> parentFeature.selectPreset(preset))
                .children(() -> text(label).color(color));
    }

    private Component buildSliderContent() {
        // Fixed-width reactive speed label so bar width never changes as value changes.
        Computed<String> label = parentFeature.speedSignal()
                .map(value -> Core.bundle.format("feature.time-control.speed.format",
                        TimeControlFeature.formatSpeed(value != null ? value : 1f)));

        return row()
                .gap(unit(1))
                .cellPaddingLeft(unit(1))
                .children(() -> {
                    // Slider binds to the intermediate u-space position signal (single-directional:
                    // u -> speed).

                    row().background(Styles.black6).height(unit(10)).center().children(() -> {
                        SolimSlider slider = slider(parentFeature.sliderPositionSignal(),
                                TimeControlFeature.SLIDER_MIN_U,
                                TimeControlFeature.SLIDER_MAX_U,
                                TimeControlFeature.SLIDER_STEP_U);

                        slider.slider().setHeight(unit(9));
                        slider.slider().setWidth(unit(70));
                    });

                    row().background(Styles.black6).center().height(unit(10)).width(unit(20)).children(() -> {
                        text(label).center();
                    });

                    // Reset button: zeroes slider position so widget and speed land exactly on 1x.
                    button()
                            .style(Styles.clearNonei)
                            .background(Styles.black6)
                            .size(unit(10))
                            .tooltip(Core.bundle.get("feature.time-control.slider.reset.tooltip"))
                            .onClick(() -> parentFeature.resetSlider())
                            .children(() -> icon(Icon.refresh).size(unit(7)));
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

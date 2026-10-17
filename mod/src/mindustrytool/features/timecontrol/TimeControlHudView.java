package mindustrytool.features.timecontrol;

import static solim.UI.*;
import static solim.modifier.Values.border;
import static solim.modifier.Values.draggable;
import static solim.modifier.Values.gap;
import static solim.modifier.Values.height;
import static solim.modifier.Values.marginLeft;
import static solim.modifier.Values.position;
import static solim.modifier.Values.prefHeight;
import static solim.modifier.Values.prefWidth;
import static solim.modifier.Values.rounded;
import static solim.modifier.Values.size;
import static solim.modifier.Values.tooltip;
import static solim.modifier.Values.width;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.game.EventType.ClientServerConnectEvent;
import mindustry.game.EventType.ResetEvent;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustrytool.components.WebStyles;
import mindustrytool.features.settings.ModSettings;
import solim.core.Component;
import solim.input.Button;
import solim.input.SolimSlider;
import solim.overlay.Hud;
import solim.reactive.Computed;
import solim.reactive.Readable;

public class TimeControlHudView extends Component {

    private final TimeControlFeature feature;
    private @Nullable Hud hud;

    public TimeControlHudView(TimeControlFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> dragIconSize = scale.map(s -> unit(6) * (s != null ? s : 1f));

        hud = hud(() -> {
            when(feature.hideDragHandleConfig.signal())
                    .elseDo(() -> button()
                            .style(WebStyles.ghost())
                            .props(size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize))
                            .children(() -> icon(Icon.move).props(size(dragIconSize), prefWidth(dragIconSize), prefHeight(dragIconSize)))
                            .props(draggable(feature.xSignal, feature.ySignal)));

            buildControls(feature, null);
        }).props(gap(unit(1)));

        hud.props(position(feature.xSignal, feature.ySignal), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1.5f, WebStyles.Colors.BORDER));

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });
        listen(ResetEvent.class, e -> feature.resetSpeed());
        listen(ClientServerConnectEvent.class, e -> feature.resetSpeed());

        effect(() -> {
            scale.get();
            Core.app.post(this::keepInScreen);
        });

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    public static Component buildControls(TimeControlFeature feature, @Nullable Readable<Boolean> canEdit) {
        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> presetWidth = scale.map(s -> unit(15) * (s != null ? s : 1f));
        Readable<Float> resetIconSize = scale.map(s -> unit(6) * (s != null ? s : 1f));
        Readable<Float> fontScale = scale.map(s -> s != null ? s : 1f);

        return dynamic(feature.modeConfig.signal(), mode -> {
            buildModeContent(feature, mode, buttonSize, presetWidth, resetIconSize, fontScale, canEdit);
        });
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

        Readable<Boolean> isSelected = feature.selectedPresetSignal()
                .map(sel -> Float.compare(preset, sel != null ? sel : 1f) == 0);

        Readable<String> label = new Computed<>(() -> {
            boolean selected = Boolean.TRUE.equals(isSelected.get());
            boolean isBoosted = selected && Boolean.TRUE.equals(feature.boostedSignal().get());
            float shown = isBoosted ? TimeControlFeature.effectiveSpeed(preset, true) : preset;
            return Core.bundle.format("feature.time-control.speed.format", TimeControlFeature.formatSpeed(shown));
        });

        Readable<Color> color = new Computed<>(() -> {
            if (!Boolean.TRUE.equals(isSelected.get())) {
                return Pal.gray;
            }
            return Boolean.TRUE.equals(feature.boostedSignal().get()) ? Pal.accent : Color.white;
        });

        Button btn = button()
                .style(WebStyles.ghost())
                .props(height(buttonSize), prefHeight(buttonSize), width(presetWidth), prefWidth(presetWidth))
                .onClick(() -> feature.selectPreset(preset))
                .children(() -> text(label).color(color).fontScale(fontScale));
        return canEdit != null ? btn.enabled(canEdit) : btn;
    }

    private static Component buildSliderContent(TimeControlFeature feature, Readable<Float> buttonSize,
            Readable<Float> resetIconSize, Readable<Float> fontScale, @Nullable Readable<Boolean> canEdit) {

        Computed<String> label = feature.speedSignal()
                .map(value -> Core.bundle.format("feature.time-control.speed.format",
                        TimeControlFeature.formatSpeed(value != null ? value : 1f)));
        Readable<Float> sliderHeight = buttonSize.map(h -> (h != null ? h : unit(10)) * 0.9f);
        Readable<Float> sliderWidth = feature.scaleConfig.signal().map(s -> unit(70) * (s != null ? s : 1f));
        Readable<Float> labelWidth = feature.scaleConfig.signal().map(s -> unit(20) * (s != null ? s : 1f));

        return row()
                .props(gap(unit(1)), marginLeft(unit(1)))
                .children(() -> {

                    row().props(rounded(unit(2), WebStyles.Colors.SECONDARY_BG), border(1f, WebStyles.Colors.BORDER), height(buttonSize), prefHeight(buttonSize), center()).children(() -> {
                                SolimSlider slider = slider(feature.sliderPositionSignal(),
                                        TimeControlFeature.SLIDER_MIN_U,
                                        TimeControlFeature.SLIDER_MAX_U,
                                        TimeControlFeature.SLIDER_STEP_U);

                                effect(() -> {
                                    if (canEdit != null) {
                                        Boolean editable = canEdit.get();
                                        slider.slider().setDisabled(!Boolean.TRUE.equals(editable));
                                    }
                                    Float sliderWidthValue = sliderWidth.get();
                                    slider.slider().setWidth(
                                            sliderWidthValue != null ? sliderWidthValue : unit(70));
                                    Float sliderHeightValue = sliderHeight.get();
                                    slider.slider().setHeight(
                                            sliderHeightValue != null ? sliderHeightValue : unit(9));
                                });
                            });

                    row().props(rounded(unit(2), WebStyles.Colors.SECONDARY_BG), border(1f, WebStyles.Colors.BORDER), center(),
                            height(buttonSize), prefHeight(buttonSize), width(labelWidth), prefWidth(labelWidth)).children(() -> {
                                text(label).center().fontScale(fontScale);
                            });

                    Button resetBtn = button()
                            .style(WebStyles.ghost())
                            .props(size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.time-control.slider.reset.tooltip")))
                            .onClick(() -> feature.resetSlider())
                            .children(() -> icon(Icon.refresh).props(size(resetIconSize), prefWidth(resetIconSize), prefHeight(resetIconSize)));
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

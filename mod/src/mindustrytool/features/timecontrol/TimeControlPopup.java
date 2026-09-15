package mindustrytool.features.timecontrol;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.gen.Icon;
import mindustry.graphics.Pal;
import mindustry.ui.Styles;
import solim.core.Component;
import solim.input.SolimSlider;
import solim.overlay.Popup;
import solim.signal.Computed;
import solim.signal.Readable;
import solim.signal.Signal;
import solim.signal.Signals;

/**
 * Stacked vertical mini-panel popup for TimeControl, opened from QuickAccess
 * in popup display mode. Binds the same speed, preset, boost, and mode signals
 * as the standalone HUD with automatic ownership and no view-local state.
 */
public final class TimeControlPopup {

    private static @Nullable Popup<TimeControlFeature> menu;

    private TimeControlPopup() {
    }

    public static void show(TimeControlFeature feature, @Nullable Element quickAccessBar) {
        if (feature == null) {
            return;
        }
        if (menu == null) {
            menu = popup();
            menu.children(TimeControlPopup::buildContent).rounded(2);
        }
        float stageW = Core.scene != null ? Core.scene.getWidth() : 0f;
        float stageH = Core.scene != null ? Core.scene.getHeight() : 0f;
        float barX = 0f;
        float barY = 0f;
        float barW = 0f;
        float barH = 0f;
        if (quickAccessBar != null) {
            barX = quickAccessBar.x;
            barY = quickAccessBar.y;
            barW = quickAccessBar.getWidth();
            barH = quickAccessBar.getHeight();
        }
        float barCenterX = barX + barW / 2f;
        float barTop = barY + barH;
        float barCenterY = barY + barH / 2f;
        boolean upperHalf = stageH > 0f ? barCenterY >= stageH / 2f : false;
        float anchorX = quickAccessBar != null ? barCenterX : stageW / 2f;
        float anchorY = quickAccessBar != null ? (upperHalf ? barY : barTop) : stageH / 2f;

        menu.show(feature, anchorX, anchorY);

        if (Core.scene != null && quickAccessBar != null && menu.table() != null) {
            try {
                float menuW = menu.table().getWidth();
                float menuH = menu.table().getHeight();
                float desiredX = barCenterX - menuW / 2f;
                float desiredY = upperHalf ? barY - menuH : barTop;
                float clampedX = stageW > menuW ? Math.max(0f, Math.min(desiredX, stageW - menuW)) : 0f;
                float clampedY = stageH > menuH ? Math.max(0f, Math.min(desiredY, stageH - menuH)) : 0f;
                menu.table().setPosition(clampedX, clampedY);
            } catch (Exception ignored) {
            }
        }
    }

    public static void hide() {
        if (menu != null) {
            menu.hide();
        }
    }

    private static Component buildContent(@Nullable TimeControlFeature feature) {
        if (feature == null) {
            return column().children(() -> text(Core.bundle.get("feature.time-control.popup.title")));
        }
        Readable<Float> scale = feature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> presetWidth = scale.map(s -> unit(15) * (s != null ? s : 1f));
        Readable<Float> dragIconIgnored = scale.map(s -> unit(7) * (s != null ? s : 1f));
        Readable<Float> resetIconSize = scale.map(s -> unit(7) * (s != null ? s : 1f));
        Readable<Float> fontScale = scale.map(s -> s != null ? s : 1f);
        Readable<Boolean> canEdit = Signal.computed(() -> Boolean.TRUE.equals(feature.enabled().get())
                && Boolean.FALSE.equals(Signals.netClient().get()));

        return column()
                .growX()
                .padding(unit(2))
                .gap(unit(1))
                .children(() -> {
                    text(Core.bundle.get("feature.time-control.popup.title")).center();
                    dynamic(feature.modeConfig.signal(),
                            mode -> buildModeContent(feature, mode, buttonSize, presetWidth, resetIconSize,
                                    fontScale, canEdit, dragIconIgnored));
                });
    }

    private static Component buildModeContent(TimeControlFeature feature, String mode, Readable<Float> buttonSize,
            Readable<Float> presetWidth, Readable<Float> resetIconSize, Readable<Float> fontScale,
            Readable<Boolean> canEdit, Readable<Float> ignored) {
        return TimeControlFeature.MODE_SLIDER.equals(mode)
                ? buildSliderContent(feature, buttonSize, resetIconSize, fontScale, canEdit)
                : buildPresetContent(feature, buttonSize, presetWidth, fontScale, canEdit);
    }

    private static Component buildPresetContent(TimeControlFeature feature, Readable<Float> buttonSize,
            Readable<Float> presetWidth, Readable<Float> fontScale, Readable<Boolean> canEdit) {
        return column()
                .growX()
                .gap(unit(1))
                .children(() -> {
                    for (float preset : TimeControlFeature.SPEEDS) {
                        presetButton(feature, preset, buttonSize, presetWidth, fontScale, canEdit);
                    }
                });
    }

    private static Component presetButton(TimeControlFeature feature, float preset, Readable<Float> buttonSize,
            Readable<Float> presetWidth, Readable<Float> fontScale, Readable<Boolean> canEdit) {
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

        return button()
                .style(Styles.cleart)
                .height(buttonSize)
                .width(presetWidth)
                .growX()
                .enabled(canEdit)
                .onClick(() -> feature.selectPreset(preset))
                .children(() -> text(label).color(color).fontScale(fontScale));
    }

    private static Component buildSliderContent(TimeControlFeature feature, Readable<Float> buttonSize,
            Readable<Float> resetIconSize, Readable<Float> fontScale, Readable<Boolean> canEdit) {
        Computed<String> label = feature.speedSignal()
                .map(value -> Core.bundle.format("feature.time-control.speed.format",
                        TimeControlFeature.formatSpeed(value != null ? value : 1f)));
        Readable<Float> sliderHeight = buttonSize.map(h -> (h != null ? h : unit(10)) * 0.9f);

        return column()
                .growX()
                .gap(unit(1))
                .children(() -> {
                    row().background(Styles.black6).height(buttonSize).growX().center().children(() -> {
                        SolimSlider slider = slider(feature.sliderPositionSignal(),
                                TimeControlFeature.SLIDER_MIN_U,
                                TimeControlFeature.SLIDER_MAX_U,
                                TimeControlFeature.SLIDER_STEP_U);

                        slider.slider().setWidth(unit(70));
                        effect(() -> {
                            Boolean editable = canEdit.get();
                            slider.slider().setDisabled(!Boolean.TRUE.equals(editable));
                            Float sliderHeightValue = sliderHeight.get();
                            slider.slider().setHeight(
                                    sliderHeightValue != null ? sliderHeightValue : unit(9));
                        });
                    });

                    row().growX().gap(unit(1)).center().children(() -> {
                        row().background(Styles.black6).center().height(buttonSize).width(unit(20)).children(() -> {
                            text(label).center().fontScale(fontScale);
                        });

                        button()
                                .style(Styles.clearNonei)
                                .background(Styles.black6)
                                .size(buttonSize)
                                .enabled(canEdit)
                                .tooltip(Core.bundle.get("feature.time-control.slider.reset.tooltip"))
                                .onClick(() -> feature.resetSlider())
                                .children(() -> icon(Icon.refresh).size(resetIconSize));
                    });
                });
    }
}

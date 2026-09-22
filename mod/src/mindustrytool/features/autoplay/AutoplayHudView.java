package mindustrytool.features.autoplay;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.tasks.AutoplayTask;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.input.Button;
import solim.overlay.Hud;
import solim.reactive.Readable;
import solim.reactive.Signal;

/**
 * Standalone reactive Autoplay HUD overlay. Shows the shared horizontal task
 * strip (master switch plus task icon toggles); all state flows through the
 * parent feature signals with automatic bindings.
 */
public class AutoplayHudView extends BaseComponent {

    static final int STRIP_COLUMNS = 5;

    private final AutoplayFeature feature;
    private @Nullable Hud hud;

    public AutoplayHudView(AutoplayFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        hud = hud(() -> {
            row()
                    .padding(unit(1))
                    .gap(unit(1))
                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                    .border(1.5f, WebStyles.Colors.BORDER)
                    .center()
                    .children(() -> {
                        when(feature.hideDragHandleConfig.signal())
                                .elseDo(() -> button()
                                        .style(WebStyles.ghost())
                                        .size(buttonSize())
                                        .children(() -> icon(Icon.move).size(iconSize()))
                                        .draggable(feature.xSignal, feature.ySignal));

                        buildControls(feature, null);
                    });
        });

        hud.position(feature.xSignal, feature.ySignal);

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });

        effect(() -> {
            feature.scaleConfig.signal().get();
            Core.app.post(this::keepInScreen);
        });

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    private Readable<Float> buttonSize() {
        return feature.scaleConfig.signal().map(s -> unit(11) * (s != null ? s : 1f));
    }

    private Readable<Float> iconSize() {
        return feature.scaleConfig.signal().map(s -> unit(7) * (s != null ? s : 1f));
    }

    public static Component buildControls(AutoplayFeature feature, @Nullable Readable<Boolean> canEdit) {
        Readable<Float> scale = feature.scaleConfig.signal();
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> unit(7) * (s != null ? s : 1f));

        return row()
                .gap(unit(1))
                .center()
                .children(() -> {
                    masterButton(feature, buttonSize, iconSize, canEdit);

                    reactiveGrid(feature.tasks())
                            .key(AutoplayTask::getId)
                            .columns(STRIP_COLUMNS)
                            .gap(unit(1))
                            .children(task -> taskButton(feature, task, buttonSize, iconSize, canEdit));
                });
    }

    private static Component masterButton(AutoplayFeature feature, Readable<Float> buttonSize,
            Readable<Float> iconSize, @Nullable Readable<Boolean> canEdit) {
        Readable<Color> color = feature.enabled()
                .map(en -> Boolean.TRUE.equals(en) ? Color.white : Color.darkGray);

        Button btn = button()
                .style(WebStyles.ghost())
                .size(buttonSize)
                .tooltip(Core.bundle.get("feature.autoplay.hud.master.tooltip"))
                .checked(feature.enabled())
                .onClick(() -> feature.setEnabled(!feature.isEnabled()))
                .children(() -> icon(Icon.play).size(iconSize).color(color));
        return canEdit != null ? btn.enabled(canEdit) : btn;
    }

    private static Component taskButton(AutoplayFeature feature, @Nullable AutoplayTask task,
            Readable<Float> buttonSize, Readable<Float> iconSize, @Nullable Readable<Boolean> canEdit) {
        if (task == null) {
            return row();
        }

        Readable<Boolean> taskEnabled = feature.disabledTasks.signal()
                .map(disabled -> disabled == null || !disabled.contains(task.getId()));
        Readable<Boolean> isCurrent = feature.currentTaskId()
                .map(id -> task.getId().equals(id));

        Readable<Color> color = Signal.computed(() -> {
            Boolean current = isCurrent.get();
            Boolean enabled = taskEnabled.get();
            return Boolean.TRUE.equals(current) ? Color.gold
                    : Boolean.TRUE.equals(enabled) ? Color.white
                            : Color.darkGray;
        });

        Button btn = button()
                .style(WebStyles.ghost())
                .size(buttonSize)
                .tooltip(task.getName())
                .onClick(() -> feature.setTaskEnabled(task.getId(), !feature.isTaskEnabled(task.getId())))
                .children(() -> icon(task.getIcon()).size(iconSize).color(color));

        return canEdit != null ? btn.enabled(canEdit) : btn;
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

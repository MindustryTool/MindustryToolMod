package mindustrytool.features.autoplay;

import static solim.UI.*;

import arc.Core;
import arc.func.Prov;
import arc.graphics.Color;
import arc.scene.Element;
import arc.util.Nullable;
import mindustry.game.EventType.ResizeEvent;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.tasks.AutoplayTask;
import mindustrytool.features.settings.ModSettings;
import solim.core.Component;
import solim.input.Button;
import solim.layout.Direction;
import solim.overlay.Hud;
import solim.overlay.SolimDialog;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class AutoplayHudView extends Component {

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
                    .props(padding(unit(1)), rounded(unit(2), WebStyles.Colors.SECTION_BG), border(1.5f, WebStyles.Colors.BORDER), gap(unit(1)), center())
                    .children(() -> {
                        when(feature.hideDragHandleConfig.signal())
                                .elseDo(() -> button()
                                        .props(style(WebStyles.ghost()), size(buttonSize()), prefWidth(buttonSize()), prefHeight(buttonSize()), draggable(feature.xSignal, feature.ySignal))
                                        .children(() -> icon(Icon.move).props(size(iconSize()), prefWidth(iconSize()), prefHeight(iconSize()))));

                        buildControls(feature, null);
                    });
        });

        hud.props(position(feature.xSignal, feature.ySignal));

        listen(ResizeEvent.class, e -> {
            keepInScreen();
            Core.app.post(this::keepInScreen);
        });

        effect(() -> {
            feature.scaleConfig.signal().get();
            ModSettings.universalScale.signal().get();
            Core.app.post(this::keepInScreen);
        });

        Core.app.post(this::keepInScreen);

        return hud.element();
    }

    private Readable<Float> buttonSize() {
        return ModSettings.effectiveScale(feature.scaleConfig.signal()).map(s -> unit(11) * (s != null ? s : 1f));
    }

    private Readable<Float> iconSize() {
        return ModSettings.effectiveScale(feature.scaleConfig.signal()).map(s -> unit(6) * (s != null ? s : 1f));
    }

    public static Component buildControls(AutoplayFeature feature, @Nullable Readable<Boolean> canEdit) {
        Readable<Float> scale = ModSettings.effectiveScale(feature.scaleConfig.signal());
        Readable<Float> buttonSize = scale.map(s -> unit(11) * (s != null ? s : 1f));
        Readable<Float> iconSize = scale.map(s -> unit(6) * (s != null ? s : 1f));
        Readable<Integer> columns = feature.visibleTasks().map(list -> {
            int size = list != null ? list.size : 0;
            return size > 0 ? Math.min(size, STRIP_COLUMNS) : 1;
        });

        return row()
                .props(gap(unit(1)), center())
                .children(() -> {
                    masterButton(feature, buttonSize, iconSize, canEdit);

                    when(feature.visibleTasks().map(list -> list != null && list.size > 0))
                            .thenDo(() -> divider(Direction.Y));

                    reactiveGrid(feature.visibleTasks())
                            .key(AutoplayTask::getId)
                            .columns(columns)
                            .props(gap(unit(1)))
                            .items(task -> taskButton(feature, task, buttonSize, iconSize, canEdit));
                });
    }

    private static void openSettings(AutoplayFeature feature) {
        AutoplayPopup.hide();
        Prov<SolimDialog> dlg = feature.getSettingDialog();
        if (dlg != null) {
            dlg.get().show();
        }
    }

    private static Component masterButton(AutoplayFeature feature, Readable<Float> buttonSize,
            Readable<Float> iconSize, @Nullable Readable<Boolean> canEdit) {

        Readable<Color> color = feature.enabled()
                .map(en -> Boolean.TRUE.equals(en) ? Color.white : Color.darkGray);

        Button btn = button()
                .props(checked(feature.enabled()), style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(Core.bundle.get("feature.autoplay.hud.master.tooltip")))
                .onClick(() -> feature.setEnabled(!feature.isEnabled()))
                .onLongClick(300L, () -> openSettings(feature))
                .children(() -> icon(Icon.play).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), color(color)));
                
        return canEdit != null ? btn.props(enabled(canEdit)) : btn;
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
                .props(style(WebStyles.ghost()), size(buttonSize), prefWidth(buttonSize), prefHeight(buttonSize), tooltip(task.getName()))
                .onClick(() -> feature.setTaskEnabled(task.getId(), !feature.isTaskEnabled(task.getId())))
                .onLongClick(300L, () -> openSettings(feature))
                .children(() -> icon(task.getIcon()).props(size(iconSize), prefWidth(iconSize), prefHeight(iconSize), color(color)));

        return canEdit != null ? btn.props(enabled(canEdit)) : btn;
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

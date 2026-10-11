package mindustrytool.features.autoplay;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.struct.Seq;
import mindustry.gen.Icon;
import mindustrytool.components.FileIcon;
import mindustrytool.components.SettingCard;
import mindustrytool.components.SettingScaffold;
import mindustrytool.components.SettingSlider;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.tasks.AutoplayTask;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.layout.Direction;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class AutoplaySettingsView extends BaseComponent {

    private final AutoplayFeature feature;

    public AutoplaySettingsView(AutoplayFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return SettingScaffold.build(() -> {
            headerCard();
            behaviorAndDisplayCard();
            tasksCard();
        });
    }

    private Component headerCard() {
        return SettingCard.header(feature, "AUTOMATION");
    }

    private Component behaviorAndDisplayCard() {
        return SettingCard.section(Icon.settings, Core.bundle.get("team-resources.settings.section.layout", "Behavior & Display"), () -> {
            checkbox(Core.bundle.get("feature.autoplay.settings.follow-unit"), feature.followUnit.signal()).growX();

            divider().color(WebStyles.Colors.SECTION_BORDER);

            row().growX().gap(unit(2)).center().children(() -> {
                text(Core.bundle.get("feature.autoplay.settings.display-mode")).left()
                        .color(Color.white);

                spacer();
                row().gap(unit(1.5f)).children(() -> {
                    button(Core.bundle.get("feature.autoplay.settings.display-mode.hud"),
                            () -> feature.displayModeConfig.set(AutoplayFeature.DISPLAY_HUD))
                            .style(WebStyles.filterChipText())
                            .checked(feature.displayModeConfig.signal()
                                    .map(AutoplayFeature.DISPLAY_HUD::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));

                    button(Core.bundle.get("feature.autoplay.settings.display-mode.popup"),
                            () -> feature.displayModeConfig.set(AutoplayFeature.DISPLAY_POPUP))
                            .style(WebStyles.filterChipText())
                            .checked(feature.displayModeConfig.signal()
                                    .map(AutoplayFeature.DISPLAY_POPUP::equals))
                            .height(unit(8))
                            .paddingX(unit(2.5f));
                });
            });

            text(Core.bundle.get("feature.autoplay.hud.reorder-hint")).growX().left().wrap(true)
                    .color(WebStyles.Colors.GHOST_FG);

            divider().color(WebStyles.Colors.SECTION_BORDER);

            SettingSlider.of(
                    Core.bundle.get("feature.autoplay.settings.scale"),
                    feature.scaleConfig.signal(),
                    0.5f,
                    1.5f,
                    0.1f,
                    feature.scaleConfig.signal().map(v -> String.format("%.0f%%", (v != null ? v : 1f) * 100))
            );

            divider().color(WebStyles.Colors.SECTION_BORDER);

            checkbox(Core.bundle.get("feature.common.settings.hide-drag-handle"),
                    feature.hideDragHandleConfig.signal()).growX();

            divider().color(WebStyles.Colors.SECTION_BORDER);

            button(feature::resetPosition)
                    .style(WebStyles.outline())
                    .growX()
                    .height(unit(11))
                    .gap(unit(2))
                    .children(() -> {
                        icon(Icon.refresh).size(unit(6));
                        text(Core.bundle.get("feature.autoplay.settings.reset-position"));
                    });
        });
    }

    private Component tasksCard() {
        return SettingCard.section(Icon.list, Core.bundle.get("feature.autoplay.settings.tasks"), () -> {
            text(Core.bundle.get("feature.autoplay.settings.tasks.description"))
                    .growX().left().color(WebStyles.Colors.GHOST_FG).wrap();

            reactiveGrid(feature.tasks()).key(AutoplayTask::getId).gap(unit(2)).growX()
                    .children(task -> new TaskRow(feature, task));
        });
    }

    public static class TaskRow extends BaseComponent {
        private final AutoplayFeature feature;
        private final AutoplayTask task;
        private final Signal<Boolean> expanded = Signal.of(false);

        public TaskRow(AutoplayFeature feature, AutoplayTask task) {
            this.feature = feature;
            this.task = task;
        }

        @Override
        protected Element build() {
            Signal<Boolean> taskEnabled = Signal.of(feature.isTaskEnabled(task.getId()));

            effect(() -> {
                Seq<String> disabled = feature.disabledTasks.get();
                boolean isEn = disabled == null || !disabled.contains(task.getId());
                if (taskEnabled.peek() != isEn) {
                    taskEnabled.set(isEn);
                }
            });

            taskEnabled.subscribe(val -> {
                if (feature.isTaskEnabled(task.getId()) != Boolean.TRUE.equals(val)) {
                    feature.setTaskEnabled(task.getId(), Boolean.TRUE.equals(val));
                }
            });

            Readable<Boolean> enabled = taskEnabled;

            Readable<Boolean> isCurrent = feature.currentTaskId().map(id -> task.getId().equals(id));

            Readable<Color> borderColor = isCurrent
                    .map(current -> Boolean.TRUE.equals(current) ? WebStyles.Colors.CHIP_CHECKED_BORDER
                            : Boolean.TRUE.equals(enabled.get()) ? Color.lightGray : WebStyles.Colors.SECTION_BORDER);

            Readable<Drawable> expandIcon = expanded.map(exp -> Boolean.TRUE.equals(exp)
                    ? FileIcon.of("chevron-up.png", Icon.up)
                    : FileIcon.of("chevron-down.png", Icon.down));

            Readable<Color> color = enabled.map(c -> Boolean.TRUE.equals(c) ? Color.white : Color.darkGray);

            return card()
                    .growX()
                    .padding(unit(2))
                    .rounded(unit(2), WebStyles.Colors.SECTION_BG)
                    .border(1f, borderColor)
                    .children(() -> {
                        column().growX().gap(unit(2)).children(() -> {
                            row().growX().gap(unit(2)).children(() -> {
                                column().gap(unit(1)).center().children(() -> {
                                    button()
                                            .style(WebStyles.ghost())
                                            .size(unit(11))
                                            .tooltip(Core.bundle.get("feature.autoplay.tooltip.reorder-up"))
                                            .onClick(() -> feature.moveTaskUp(task.getId()))
                                            .children(() -> icon(FileIcon.of("chevron-up.png", Icon.up)).size(unit(7)));

                                    button()
                                            .style(WebStyles.ghost())
                                            .size(unit(11))
                                            .tooltip(Core.bundle.get("feature.autoplay.tooltip.reorder-down"))
                                            .onClick(() -> feature.moveTaskDown(task.getId()))
                                            .children(() -> icon(FileIcon.of("chevron-down.png", Icon.down))
                                                    .size(unit(7)));
                                });

                                divider(Direction.Y);

                                column().growX().paddingTop(unit(2)).gap(unit(2)).children(() -> {
                                    row().growX().gap(unit(2)).left().children(() -> {
                                        icon(task.getIcon()).size(unit(5))
                                                .color(color);
                                        text(task.getName())
                                                .color(color)
                                                .wrap(true);
                                    });

                                    text(task.status()).growX().left().wrap(true).color(color);
                                });

                                row().gap(unit(1)).center().children(() -> {
                                    Readable<Boolean> isVisible = feature.hiddenTasks.signal()
                                            .map(hidden -> hidden == null || !hidden.contains(task.getId()));

                                    Readable<Drawable> eyeIcon = isVisible.map(vis -> Boolean.TRUE.equals(vis)
                                            ? FileIcon.of("eye.png", Icon.eye)
                                            : FileIcon.of("eye-off.png", Icon.eyeOff));

                                    Readable<Color> eyeColor = isVisible.map(vis -> Boolean.TRUE.equals(vis)
                                            ? Color.white
                                            : Color.darkGray);

                                    Readable<String> eyeTooltip = isVisible.map(vis -> Boolean.TRUE.equals(vis)
                                            ? Core.bundle.get("feature.autoplay.tooltip.hide-task")
                                            : Core.bundle.get("feature.autoplay.tooltip.show-task"));
                          
                                    if (task.hasSettings()) {
                                        button()
                                                .style(WebStyles.ghost())
                                                .size(unit(11))
                                                .tooltip(Core.bundle.get("feature.autoplay.tooltip.configure"))
                                                .onClick(() -> expanded.set(!Boolean.TRUE.equals(expanded.peek())))
                                                .children(() -> icon(expandIcon).size(unit(7)));
                                    }

                                    button()
                                            .style(WebStyles.ghost())
                                            .size(unit(11))
                                            .tooltip(eyeTooltip)
                                            .onClick(() -> feature.setTaskVisible(task.getId(), !feature.isTaskVisible(task.getId())))
                                            .children(() -> icon(eyeIcon).size(unit(6)).color(eyeColor));


                                    checkbox("", taskEnabled)
                                            .size(unit(8))
                                            .tooltip(Core.bundle.get("feature.autoplay.tooltip.toggle-task"));
                                });
                            });

                            if (task.hasSettings()) {
                                collapser(expanded, () -> {
                                    divider().color(WebStyles.Colors.SECTION_BORDER);
                                    task.buildSettings(feature);
                                }).gap(unit(2)).growX();
                            }
                        });
                    }).element();
        }
    }
}

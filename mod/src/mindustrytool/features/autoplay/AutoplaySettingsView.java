package mindustrytool.features.autoplay;

import static solim.UI.*;

import arc.Core;
import arc.graphics.Color;
import arc.scene.Element;
import arc.scene.style.Drawable;
import arc.util.Strings;
import mindustry.gen.Icon;
import mindustrytool.components.WebStyles;
import mindustrytool.features.autoplay.tasks.AutoplayTask;
import solim.core.BaseComponent;
import solim.core.Component;
import solim.reactive.Readable;
import solim.reactive.Signal;

public class AutoplaySettingsView extends BaseComponent {

    private final AutoplayFeature feature;

    public AutoplaySettingsView(AutoplayFeature feature) {
        this.feature = feature;
    }

    @Override
    protected Element build() {
        return column().grow().center().children(() -> {
            scroll().grow().children(() -> {
                column().growX().gap(unit(3)).padding(unit(2)).children(() -> {
                    globalSection();
                    divider();
                    tasksSection();
                });
            });
        }).element();
    }

    private Component globalSection() {
        Readable<String> cooldownLabel = feature.overrideCooldown.signal().map(val ->
                Core.bundle.format("feature.autoplay.settings.override-cooldown", Strings.fixed(val != null ? val : 2.0f, 1)));

        return column().growX().gap(unit(2)).children(() -> {
            checkbox(Core.bundle.get("feature.autoplay.settings.follow-unit"), feature.followUnit.signal());

            column().growX().gap(unit(1)).children(() -> {
                text(cooldownLabel).growX().left().color(WebStyles.Colors.GHOST_FG);
                slider(feature.overrideCooldown.signal(), 0.5f, 5.0f, 0.1f).growX();
            });
        });
    }

    private Component tasksSection() {
        return column().growX().gap(unit(2)).children(() -> {
            column().growX().gap(unit(1)).children(() -> {
                text(Core.bundle.get("feature.autoplay.settings.tasks"))
                        .growX().left().color(WebStyles.Colors.GHOST_FG);
                text(Core.bundle.get("feature.autoplay.settings.tasks.description"))
                        .growX().left().color(WebStyles.Colors.GHOST_FG);
            });

            reactiveGrid(Signal.of(1), feature.tasks(),
                    AutoplayTask::getId,
                    task -> new TaskRow(feature, task))
                    .gap(unit(2))
                    .growX();
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
            Readable<Boolean> enabled = feature.disabledTasks.signal().map(disabled ->
                    disabled == null || !disabled.contains(task.getId()));

            Readable<Drawable> expandIcon = expanded.map(exp ->
                    Boolean.TRUE.equals(exp) ? Icon.up : Icon.down);

            return column().growX().gap(unit(1)).children(() -> {
                row().growX().gap(unit(1)).center().children(() -> {
                    button()
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle.get("feature.autoplay.tooltip.reorder-up"))
                            .onClick(() -> feature.moveTaskUp(task.getId()))
                            .children(() -> icon(Icon.up).size(unit(6)));

                    button()
                            .style(WebStyles.ghost())
                            .size(unit(11))
                            .tooltip(Core.bundle.get("feature.autoplay.tooltip.reorder-down"))
                            .onClick(() -> feature.moveTaskDown(task.getId()))
                            .children(() -> icon(Icon.down).size(unit(6)));

                    button(() -> feature.setTaskEnabled(task.getId(), !Boolean.TRUE.equals(enabled.peek())))
                            .style(WebStyles.filterChipText())
                            .checked(enabled)
                            .paddingX(unit(2))
                            .height(unit(8))
                            .children(() -> {
                                icon(task.getIcon()).size(unit(4));
                                text(task.getName()).color(enabled.map(c -> Boolean.TRUE.equals(c) ? Color.white : Color.gray));
                            });

                    spacer();

                    text(task.status()).color(WebStyles.Colors.GHOST_FG);

                    if (task.hasSettings()) {
                        button()
                                .style(WebStyles.ghost())
                                .size(unit(11))
                                .tooltip(Core.bundle.get("feature.autoplay.tooltip.configure"))
                                .onClick(() -> expanded.set(!Boolean.TRUE.equals(expanded.peek())))
                                .children(() -> icon(expandIcon).size(unit(6)));
                    }
                });

                if (task.hasSettings()) {
                    card().growX().padding(unit(2)).visible(expanded).children(() -> {
                        task.buildSettings(feature);
                    });
                }
            }).element();
        }
    }
}

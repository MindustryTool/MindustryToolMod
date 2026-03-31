package mindustrytool.features.settings;

import arc.Core;
import arc.graphics.Color;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.gen.Icon;
import mindustry.gen.Tex;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustrytool.Config;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureManager;
import mindustrytool.features.WebFeature;
import mindustrytool.ui.ChangelogDialog;
import arc.util.Http;
import arc.util.Log;
import mindustrytool.dto.TaskData;
import mindustrytool.dto.TaskResponse;
import java.util.List;

public class FeatureSettingDialog extends BaseDialog {

    private String filter = "";
    private Table paneTable;

    private List<TaskData> inProgressTasks = null;
    private List<TaskData> acceptedTasks = null;
    private boolean isLoadingTasks = false;

    private enum Tab {
        General, Development
    }

    private Tab currentTab = Tab.General;

    public FeatureSettingDialog() {
        super("Feature");

        addCloseButton();

        buttons.button("@feature.changelog", Icon.book, () -> {
            new ChangelogDialog().show();
        });

        buttons.button("@feature.report-bug", Icon.infoCircle, () -> {
            if (!Core.app.openURI(Config.DISCORD_INVITE_URL)) {
                Core.app.setClipboardText(Config.DISCORD_INVITE_URL);
                Vars.ui.showInfoFade("@copied");
            }
        });

        shown(this::rebuild);
        resized(this::rebuild);
    }

    private void rebuild() {
        cont.clear();
        cont.top();

        cont.table(t -> {
            t.button("General", Styles.togglet, () -> {
                currentTab = Tab.General;
                rebuild();
            }).checked(currentTab == Tab.General).growX().height(50).padRight(10).padLeft(10);

            t.button("Development", Styles.togglet, () -> {
                currentTab = Tab.Development;
                rebuild();
            }).checked(currentTab == Tab.Development).growX().height(50).padRight(10);
        }).growX().row();

        if (currentTab == Tab.General) {
            buildGeneral();
        } else {
            buildDevelopment();
        }

    }

    private void buildGeneral() {
        cont.table(s -> {
            s.left();
            s.image(Icon.zoom).padRight(8);
            s.field(filter, f -> {
                filter = f;
                rebuildPane();
            }).growX();
        }).growX().pad(10).row();

        cont.pane(table -> {
            this.paneTable = table;
            rebuildPane();
        }).scrollX(false).grow();
    }

    private void buildDevelopment() {
        if (inProgressTasks != null && acceptedTasks != null) {
            buildTaskTable(inProgressTasks, acceptedTasks);
            return;
        }

        if (isLoadingTasks) {
            cont.add("@loading").color(Color.gray).center();
            return;
        }

        isLoadingTasks = true;
        inProgressTasks = null;
        acceptedTasks = null;
        cont.add("@loading").color(Color.gray).center();

        // Load IN_PROGRESS
        Http.get(Config.PROJECT_URL + "/api/v1/projects/" + Config.PROJECT_ID + "/tasks?status=IN_PROGRESS")
                .error(this::handleError)
                .timeout(20000)
                .submit(response -> {
                    try {
                        TaskResponse taskResponse = Utils.fromJson(TaskResponse.class, response.getResultAsString());
                        inProgressTasks = taskResponse.data;
                        checkTasksLoaded();
                    } catch (Exception e) {
                        handleError(e);
                    }
                });

        // Load ACCEPTED
        Http.get(Config.PROJECT_URL + "/api/v1/projects/" + Config.PROJECT_ID + "/tasks?status=ACCEPTED")
                .error(this::handleError)
                .timeout(20000)
                .submit(response -> {
                    try {
                        TaskResponse taskResponse = Utils.fromJson(TaskResponse.class, response.getResultAsString());
                        acceptedTasks = taskResponse.data;
                        checkTasksLoaded();
                    } catch (Exception e) {
                        handleError(e);
                    }
                });
    }

    private void handleError(Throwable e) {
        isLoadingTasks = false;
        Core.app.post(() -> {
            if (currentTab != Tab.Development)
                return;
            cont.clear();
            cont.add("Error: " + e.getMessage()).color(Color.red).grow();
            Log.err(e);
        });
    }

    private void checkTasksLoaded() {
        if (inProgressTasks != null && acceptedTasks != null) {
            isLoadingTasks = false;
            Core.app.post(() -> {
                if (currentTab == Tab.Development) {
                    rebuild();
                }
            });
        }
    }

    private void buildTaskTable(List<TaskData> inProgress, List<TaskData> accepted) {
        cont.pane(table -> {
            table.top().left();

            table.table(header -> {
                header.left();
                header.button(Icon.refresh, () -> {
                    inProgressTasks = null;
                    acceptedTasks = null;
                    rebuild();
                }).pad(10).left().height(50);

                header.button("Create Suggestion", Icon.add, () -> {
                    Core.app.openURI(Config.PROJECT_URL + "/projects/" + Config.PROJECT_ID);
                }).pad(10).padLeft(0).left().growX();
            }).left().growX().height(50).row();

            // In Progress Section
            if (inProgress != null && !inProgress.isEmpty()) {
                table.add("In Progress").style(Styles.defaultLabel).color(Color.sky).pad(10).left().row();
                renderTaskList(table, inProgress);
            } else {
                table.add("No tasks in progress.").color(Color.gray).pad(10).left().row();
            }

            // Divider
            table.image().color(Color.gray).growX().height(3f).pad(10).row();

            // Accepted Section
            if (accepted != null && !accepted.isEmpty()) {
                table.add("Accepted").style(Styles.defaultLabel).color(Color.green).pad(10).left().row();
                renderTaskList(table, accepted);
            } else {
                table.add("No accepted tasks.").color(Color.gray).pad(10).left().row();
            }

        }).grow();
    }

    private void renderTaskList(Table table, List<TaskData> tasks) {
        for (TaskData task : tasks) {
            table.table(Tex.pane, t -> {
                t.left().top().margin(10);
                t.add(task.title)
                        .style(Styles.defaultLabel)
                        .color(Color.white)
                        .growX()
                        .left()
                        .row();

                if (task.description != null && !task.description.isEmpty()) {
                    String desc = task.description;
                    t.add(desc)
                            .style(Styles.outlineLabel)
                            .color(Color.lightGray)
                            .fontScale(0.8f)
                            .growX()
                            .left()
                            .wrap()
                            .padTop(5).row();
                }

                t.table(meta -> {
                    meta.left();
                    meta.table(info -> {
                        if (task.author != null) {
                            info.add("by " + task.author.getName())
                                    .style(Styles.outlineLabel)
                                    .color(Color.gray)
                                    .fontScale(0.8f)
                                    .growX();
                        }
                    }).growX();
                    meta.center().left();
                }).growX().center().left();

            }).growX().pad(5).row();
        }
    }

    private void rebuildPane() {
        if (paneTable == null) {
            return;
        }

        paneTable.clear();
        paneTable.top().left();

        int cols = Math.max(1, (int) (arc.Core.graphics.getWidth() / Scl.scl() * 0.85f / 340f));
        float cardWidth = ((float) arc.Core.graphics.getWidth() / Scl.scl() * 0.85f) / cols;

        paneTable.row();
        paneTable.button("@reeanable", () -> {
            FeatureManager.getInstance().reEnable();
            rebuildPane();
        }).width(250).top().left().pad(10).tooltip("Used after a crash");

        paneTable.row();

        int i = 0;
        // Toggleable Features
        for (Feature feature : FeatureManager.getInstance().getFeatures()) {
            if (!filter.isEmpty()
                    && !Utils.getString(feature.getMetadata().name()).toLowerCase().contains(filter.toLowerCase())) {
                continue;
            }

            FeatureCard.buildToggle(paneTable, feature, this::rebuildPane);

            if (++i % cols == 0) {
                paneTable.row();
            }
        }

        if (i % cols != 0) {
            paneTable.row();
        }

        paneTable.image().color(Color.gray).growX().height(4f).colspan(cols).pad(10).row();

        paneTable.add("@feature").padLeft(10).top().left().row();

        i = 0;

        // Features with Dialogs
        for (Feature feature : FeatureManager.getInstance().getEnableds()) {
            if (!feature.dialog().isPresent()) {
                continue;
            }

            if (!filter.isEmpty()
                    && !Utils.getString(feature.getMetadata().name()).toLowerCase().contains(filter.toLowerCase())) {
                continue;
            }

            FeatureCard.buildLink(paneTable, feature);

            if (++i % cols == 0) {
                paneTable.row();
            }
        }

        // Web Features
        for (WebFeature webFeature : WebFeature.defaults) {
            if (!filter.isEmpty() && !Utils.getString(webFeature.name()).toLowerCase().contains(filter.toLowerCase()))
                continue;

            FeatureCard.buildLink(paneTable, webFeature);
            if (++i % cols == 0)
                paneTable.row();
        }

        // Icon Dialog
        buildIconDialogButton(paneTable, cardWidth);
        if (++i % cols == 0)
            paneTable.row();

        paneTable.table().growX().row();
    }

    private void buildIconDialogButton(Table parent, float cardWidth) {
        parent.table(Tex.button, card -> {
            card.top().left();
            card.table(c -> {
                c.top().left().margin(12);
                c.table(header -> {
                    header.left();
                    header.add("Icon").style(Styles.defaultLabel).color(Color.white).growX().left();
                }).growX().row();

                c.add().growY().row();
                c.add().growX();

                c.button(Icon.linkSmall, () -> new IconBrowserDialog().show());
            }).grow();
        }).growX().height(180f).pad(10f);
    }
}

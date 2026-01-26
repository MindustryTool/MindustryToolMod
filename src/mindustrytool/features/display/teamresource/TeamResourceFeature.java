package mindustrytool.features.display.teamresource;

import arc.Core;
import arc.Events;
import arc.graphics.Color;
import arc.input.KeyCode;
import arc.scene.Element;
import arc.scene.Group;
import arc.scene.event.ClickListener;
import arc.scene.event.InputEvent;
import arc.scene.event.Touchable;
import arc.scene.style.Drawable;
import arc.scene.ui.Dialog;
import arc.scene.ui.Image;
import arc.scene.ui.Label;
import arc.scene.ui.Slider;
import arc.scene.ui.layout.Scl;
import arc.scene.ui.layout.Table;
import arc.struct.ObjectSet;
import arc.struct.Seq;
import arc.util.Interval;
import arc.util.Log;
import arc.util.Scaling;
import mindustry.Vars;
import mindustry.core.UI;
import mindustry.game.EventType.*;
import mindustry.game.Team;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.ui.Styles;
import mindustry.ui.dialogs.BaseDialog;
import mindustry.world.blocks.power.PowerGraph;
import mindustry.world.modules.ItemModule;
import mindustrytool.Utils;
import mindustrytool.features.Feature;
import mindustrytool.features.FeatureMetadata;
import java.util.Optional;

public class TeamResourceFeature extends Table implements Feature {
    private final ObjectSet<Item> usedItems = new ObjectSet<>();
    private final ObjectSet<UnitType> usedUnits = new ObjectSet<>();
    private final Interval timer = new Interval(2);

    private Team selectedTeam;
    private ItemModule coreItems;
    private ItemModule lastSnapshot = new ItemModule();
    private ItemModule rateDisplay = new ItemModule();
    private boolean viewingStats = false;
    private boolean holdingForStats = false;

    private Seq<PowerGraph> teamGraphs = new Seq<>();

    private boolean showTeamSelector = false;
    private boolean isHovered = false;

    private final ClickListener statsListener = new ClickListener() {
        @Override
        public void clicked(InputEvent event, float x, float y) {
            try {
                showTeamSelector = !showTeamSelector;
                rebuild();
            } catch (Exception e) {
                Log.err(e);
            }
        }

        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, KeyCode button) {
            try {
                holdingForStats = true;
                viewingStats = true;
                lastSnapshot.clear();
                return super.touchDown(event, x, y, pointer, button);
            } catch (Exception e) {
                viewingStats = false;
                holdingForStats = false;
                Log.err(e);
            }

            return false;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, KeyCode button) {
            try {
                holdingForStats = false;
                if (!Vars.mobile) {
                    if (!isHovered)
                        viewingStats = false;
                } else {
                    viewingStats = false;
                }
                lastSnapshot.clear();
                super.touchUp(event, x, y, pointer, button);
            } catch (Exception e) {
                Log.err(e);
            }
        }

        @Override
        public void enter(InputEvent event, float x, float y, int pointer, Element fromActor) {
            if (pointer == -1 && !holdingForStats) {
                viewingStats = true;
                lastSnapshot.clear();
            }
            super.enter(event, x, y, pointer, fromActor);
        }

        @Override
        public void exit(InputEvent event, float x, float y, int pointer, Element toActor) {
            if (pointer == -1 && !holdingForStats) {
                viewingStats = false;
                lastSnapshot.clear();
            }
            super.exit(event, x, y, pointer, toActor);
        }
    };

    @Override
    public FeatureMetadata getMetadata() {
        return FeatureMetadata.builder()
                .name("@feature.team-resources.name")
                .description("@feature.team-resources.description")
                .icon(Utils.icons("team-resources.png"))
                .order(0)
                .enabledByDefault(true)
                .quickAccess(true)
                .build();
    }

    @Override
    public void init() {
        Events.run(WorldLoadEvent.class, () -> {
            selectedTeam = Vars.player.team();
            lastSnapshot.clear();
            usedItems.clear();
            Core.app.post(() -> rebuild());
        });

        Events.run(ResetEvent.class, () -> {
            usedItems.clear();
            clear();
        });

        name = "team-resources-overlay";

        setFillParent(true);
        top();

        touchable = Touchable.childrenOnly;

        visible(() -> Vars.ui.hudfrag.shown && Vars.state.isGame());

        update(() -> {
            if (!visible) {
                return;
            }

            if (!selectedTeam.active()) {
                selectedTeam = Vars.player.team();
            }

            coreItems = (selectedTeam.data().hasCore() && selectedTeam.core() != null)
                    ? selectedTeam.core().items
                    : null;

            if (coreItems == null) {
                return;
            }

            if (getChildren().isEmpty()) {
                rebuild();
            }

            if (Vars.content.items().contains(item -> coreItems.get(item) > 0 && usedItems.add(item))) {
                rebuild();
            }

            if (timer.get(0, 30f)) {
                updateRates();
                if (TeamResourceConfig.showPower()) {
                    updatePowerStats();
                }
            }
        });
    }

    @Override
    public void onEnable() {
        if (Vars.ui != null && Vars.ui.hudGroup != null) {
            if (Vars.ui.hudGroup.find("team-resources-overlay") != null) {
                Vars.ui.hudGroup.find("team-resources-overlay").remove();
            }

            Element parent = Vars.ui.hudGroup.find("minimap/position");
            if (parent != null && parent instanceof Group) {
                ((Group) parent).addChild(this);
            } else {
                Core.app.post(() -> Vars.ui.hudGroup.addChild(this));
                name = "team-resources-overlay";
            }

            Core.settings.put("coreitems", false);
            Core.app.post(() -> rebuild());
        }
    }

    @Override
    public void onDisable() {
        remove();
        Core.settings.put("coreitems", true);
    }

    @Override
    public Optional<Dialog> setting() {
        return Optional.of(new TeamResourceSettingsDialog());
    }

    public void rebuild() {
        clear();
        top();

        this.color.a = TeamResourceConfig.opacity();
        Drawable bg = TeamResourceConfig.hideBackground() ? null : Styles.black3;

        float screenW = Core.graphics.getWidth();
        float calculatedWidth = screenW * TeamResourceConfig.overlayWidth();
        float minWidth = 100f;
        float widthToUse = Math.max(calculatedWidth, Vars.mobile ? minWidth : 120f);
        float scale = TeamResourceConfig.scale();

        final float finalWidth = widthToUse;

        table(bg, t -> {
            t.margin(8f * scale);

            t.collapser(c -> {
                c.pane(p -> {
                    p.left();

                    Seq<Team> validTeams = new Seq<>();
                    for (Team team : Team.all) {
                        if (team.active() && team.data().hasCore()) {
                            validTeams.add(team);
                        }
                    }

                    for (Team team : validTeams) {
                        p.button(b -> {
                            b.image().size(24f).color(team.color);
                        }, Styles.clearTogglei, () -> {
                            selectedTeam = team;
                            usedUnits.clear();
                            rebuild();
                        }).checked(sel -> selectedTeam == team).size(32f).pad(2f).margin(2f);
                    }

                    if (validTeams.size > 5) {
                        p.button("...", Styles.flatBordert, this::showAllTeamsDialog).size(32f).pad(2f);
                    }

                }).scrollX(true).scrollY(false).height(40f).growX();
            }, true, () -> showTeamSelector).growX().row();

            if (TeamResourceConfig.showItems()) {
                t.table(itemsTable -> {
                    itemsTable.left();
                    itemsTable.touchable = Touchable.enabled;
                    itemsTable.addListener(statsListener);

                    float iconSize = 24f * scale;
                    float labelWidth = 80f * scale;
                    float padding = 4f * scale;
                    float cellWidth = iconSize + labelWidth + padding;

                    int cols = Math.max(2, (int) (finalWidth / cellWidth));

                    int i = 0;
                    for (Item item : Vars.content.items()) {
                        if (!usedItems.contains(item))
                            continue;

                        itemsTable.image(item.uiIcon).size(iconSize).padRight(4f * scale);
                        itemsTable.label(() -> formatItem(item)).fontScale(0.8f * scale)
                                .padRight(4f * scale)
                                .minWidth(labelWidth).left();

                        if (++i % cols == 0)
                            itemsTable.row();
                    }
                }).growX().row();
            }

            if (TeamResourceConfig.showUnits() && selectedTeam != null) {
                t.table(unitsTable -> {
                    unitsTable.left();

                    for (UnitType type : Vars.content.units()) {
                        int count = selectedTeam.data().countType(type);
                        if (count > 0) {
                            usedUnits.add(type);
                        }
                    }

                    float unitSize = 32f * scale;
                    float unitPad = 4f * scale;
                    float cellWidth = unitSize + unitPad;

                    int cols = Math.max(2, (int) (finalWidth / cellWidth));

                    int i = 0;
                    for (UnitType type : Vars.content.units()) {
                        if (!usedUnits.contains(type)) {
                            continue;
                        }

                        unitsTable.stack(
                                new Image(type.uiIcon).setScaling(Scaling.fit),
                                new Table(lab -> lab.label(() -> {
                                    int c = selectedTeam.data().countType(type);
                                    return c > 0 ? UI.formatAmount(c) : "";
                                }).fontScale(0.72f * scale).style(Styles.outlineLabel)).right().bottom())
                                .size(unitSize)
                                .pad(2f * scale);

                        if (++i % cols == 0)
                            unitsTable.row();
                    }
                }).growX().padTop(6f * scale).row();
            }

            if (TeamResourceConfig.showPower()) {
                t.table(power -> {
                    float barHeight = 20f * scale;
                    power.defaults().growX();

                    if (teamGraphs.isEmpty()) {
                        power.add("@team-resources.no-power").fontScale(0.8f * scale).color(Color.gray);
                    } else {

                        power.label(() -> {
                            float totalBalance = 0;
                            for (PowerGraph g : teamGraphs)
                                totalBalance += g.getPowerBalance();
                            return Core.bundle.get("team-resources.power-prefix") + (totalBalance >= 0 ? "+" : "")
                                    + UI.formatAmount((long) (totalBalance * 60));
                        }).fontScale(0.8f * scale).left().padBottom(2f * scale).row();

                        power.add(new SplitBar(
                                teamGraphs,
                                SplitBar.Mode.SATISFACTION,
                                scale)).height(barHeight).row();

                        if (TeamResourceConfig.showStoredPower()) {

                            power.label(() -> {
                                float totalStored = 0;
                                float totalCap = 0;
                                for (PowerGraph g : teamGraphs) {
                                    totalStored += g.getLastPowerStored();
                                    totalCap += g.getLastCapacity();
                                }
                                return Core.bundle.get("team-resources.stored-prefix")
                                        + UI.formatAmount((long) totalStored) + " / "
                                        + UI.formatAmount((long) totalCap);
                            }).fontScale(0.8f * scale).left().padTop(4f * scale).padBottom(2f * scale).row();

                            power.add(new SplitBar(
                                    teamGraphs,
                                    SplitBar.Mode.STORED,
                                    scale)).height(barHeight).row();
                        }
                    }
                }).growX().padTop(Scl.scl(6f * scale));
            }
        }).width(finalWidth);
    }

    private void showAllTeamsDialog() {
        var dialog = new BaseDialog("@team-resources.all-teams") {
            {
                name = "teamResourceAllTeamsDialog";
                addCloseButton();
                cont.pane(p -> {
                    p.defaults().width(150f).height(50f).pad(5f);
                    int i = 0;
                    for (Team team : Team.all) {
                        p.button(b -> {
                            b.image().color(team.color).margin(4f).size(24f).padRight(10f);
                            b.add(team.localized()).color(team.color);
                        }, Styles.flatTogglet, () -> {
                            selectedTeam = team;
                            rebuild();
                            hide();
                        }).checked(team == selectedTeam);

                        if (++i % 3 == 0)
                            p.row();
                    }
                }).grow();
            }
        };

        Core.app.post(() -> dialog.show());
    }

    private void updateRates() {
        if (coreItems == null)
            return;
        if (lastSnapshot.any()) {
            coreItems.each((item, amount) -> rateDisplay.set(item, (amount - lastSnapshot.get(item)) * 2));
        }
        lastSnapshot.set(coreItems);
    }

    private void updatePowerStats() {
        ObjectSet<PowerGraph> found = new ObjectSet<>();
        Groups.build.each(b -> {
            if (b.team == selectedTeam && b.power != null && b.power.graph != null) {
                PowerGraph graph = b.power.graph;

                if (graph.getLastPowerProduced() > 0 || graph.getLastCapacity() > 0
                        || graph.getLastPowerProduced() > 0) {
                    found.add(graph);
                }
            }
        });

        Seq<PowerGraph> newGraphs = found.toSeq();
        newGraphs.sort((a, b) -> {
            int cap = Float.compare(b.getLastCapacity(), a.getLastCapacity());
            if (cap != 0)
                return cap;
            return Float.compare(b.getLastPowerProduced(), a.getLastPowerProduced());
        });

        if (!newGraphs.equals(teamGraphs)) {
            teamGraphs = newGraphs;

            Core.app.post(() -> rebuild());
        }
    }

    private CharSequence formatItem(Item item) {
        if (coreItems == null)
            return "0";

        int amount = coreItems.get(item);
        int rate = rateDisplay.get(item);

        if (viewingStats) {
            if (rate == 0)
                return "[gray]0/s";

            String prefix = rate > 0 ? "[lime]+" : "[scarlet]";

            return prefix + rate + "[gray]/s";
        }

        String color = "[white]";
        if (rate < 0) {
            color = "[scarlet]";
        } else if (rate > 0) {
            color = "[lime]";
        }

        return color + UI.formatAmount(amount);
    }

    public class TeamResourceSettingsDialog extends BaseDialog {
        public TeamResourceSettingsDialog() {
            super("@team-resources.settings.title");

            buttons.button("@back", Icon.left, this::hide).size(210f, 64f);

            buttons.button("@reset-to-defaults", Icon.refresh, () -> {
                TeamResourceConfig.opacity(1f);
                TeamResourceConfig.scale(1f);
                TeamResourceConfig.overlayWidth(0.3f);
                TeamResourceConfig.showItems(true);
                TeamResourceConfig.showUnits(false);
                TeamResourceConfig.showPower(true);
                TeamResourceConfig.showStoredPower(false);
                TeamResourceConfig.hideBackground(false);

                rebuild();
                hide();
                new TeamResourceSettingsDialog().show();
            }).size(250f, 64f);

            Table cont = this.cont;
            cont.clear();
            cont.defaults().pad(6).left();

            float width = Math.min(Core.graphics.getWidth() / 1.2f, 460f);

            Slider opacitySlider = new Slider(0.1f, 1f, 0.05f, false);
            opacitySlider.setValue(TeamResourceConfig.opacity());

            Label opacityValue = new Label(Math.round(TeamResourceConfig.opacity() * 100) + "%", Styles.outlineLabel);
            opacityValue.setColor(Color.lightGray);

            Table opacityContent = new Table();
            opacityContent.touchable = Touchable.disabled;
            opacityContent.margin(3f, 33f, 3f, 33f);
            opacityContent.add("@opacity", Styles.outlineLabel).left().growX();
            opacityContent.add(opacityValue).padLeft(10f).right();

            opacitySlider.changed(() -> {
                TeamResourceConfig.opacity(opacitySlider.getValue());
                opacityValue.setText(Math.round(TeamResourceConfig.opacity() * 100) + "%");
                rebuild();
            });

            cont.stack(opacitySlider, opacityContent).width(width).left().padTop(4f).row();

            Slider sizeSlider = new Slider(0.3f, 2f, 0.1f, false);
            sizeSlider.setValue(TeamResourceConfig.scale());

            Label sizeValue = new Label(Math.round(TeamResourceConfig.scale() * 100) + "%", Styles.outlineLabel);
            sizeValue.setColor(Color.lightGray);

            Table sizeContent = new Table();
            sizeContent.touchable = Touchable.disabled;
            sizeContent.margin(3f, 33f, 3f, 33f);
            sizeContent.add("@size", Styles.outlineLabel).left().growX();
            sizeContent.add(sizeValue).padLeft(10f).right();

            sizeSlider.changed(() -> {
                TeamResourceConfig.scale(sizeSlider.getValue());
                sizeValue.setText(Math.round(TeamResourceConfig.scale() * 100) + "%");
                rebuild();
            });

            cont.stack(sizeSlider, sizeContent).width(width).left().padTop(4f).row();

            Slider widthSlider = new Slider(0.15f, 1f, 0.05f, false);
            widthSlider.setValue(TeamResourceConfig.overlayWidth());

            Label widthValue = new Label(Math.round(TeamResourceConfig.overlayWidth() * 100) + "%",
                    Styles.outlineLabel);
            widthValue.setColor(Color.lightGray);

            Table widthContent = new Table();
            widthContent.touchable = Touchable.disabled;
            widthContent.margin(3f, 33f, 3f, 33f);
            widthContent.add("@width", Styles.outlineLabel).left().growX();
            widthContent.add(widthValue).padLeft(10f).right();

            widthSlider.changed(() -> {
                TeamResourceConfig.overlayWidth(widthSlider.getValue());
                widthValue.setText(Math.round(TeamResourceConfig.overlayWidth() * 100) + "%");
                rebuild();
            });

            cont.stack(widthSlider, widthContent).width(width).left().padTop(4f).row();

            cont.check("@team-resources.show-items", TeamResourceConfig.showItems(), b -> {
                TeamResourceConfig.showItems(b);
                rebuild();
            }).left().padTop(4).row();

            cont.check("@team-resources.show-units", TeamResourceConfig.showUnits(), b -> {
                TeamResourceConfig.showUnits(b);
                rebuild();
            }).left().padTop(4).row();

            cont.check("@team-resources.show-power", TeamResourceConfig.showPower(), b -> {
                TeamResourceConfig.showPower(b);
                rebuild();
            }).left().padTop(4).row();

            cont.check("@team-resources.show-stored-power", TeamResourceConfig.showStoredPower(), b -> {
                TeamResourceConfig.showStoredPower(b);
                rebuild();
            }).left().padTop(4).row();

            cont.check("@team-resources.hide-background", TeamResourceConfig.hideBackground(), b -> {
                TeamResourceConfig.hideBackground(b);
                rebuild();
            }).left().padTop(4).row();
        }
    }
}

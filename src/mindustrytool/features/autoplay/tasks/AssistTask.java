package mindustrytool.features.autoplay.tasks;

import java.util.Optional;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import arc.util.Align;
import mindustry.Vars;
import mindustry.ai.types.BuilderAI;
import mindustry.entities.units.AIController;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Player;
import mindustry.gen.Unit;
import mindustry.ui.Styles;

public class AssistTask implements AutoplayTask {
    private boolean enabled = true;
    private String status = "";
    private final BuilderAI ai = new BuilderAI();

    @Override
    public String getName() {
        return Iconc.players + " Auto Build";
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return Icon.players;
    }

    @Override
    public boolean isEnabled() {
        return enabled;
    }

    @Override
    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public boolean shouldRun(Unit unit) {
        if (!unit.canBuild()) {
            status = Core.bundle.get("autoplay.status.cannot-build");
            return false;
        }

        if (ai.assistFollowing != null) {
            Player p = Groups.player.find(pl -> pl.unit() == ai.assistFollowing);
            String name = (p != null) ? p.name : "unit";
            status = Core.bundle.format("autoplay.status.following", name);
            return true;
        }

        var buildingPlayer = Groups.player
                .find(p -> p != Vars.player && p.unit() != null && unit.team() == p.team()
                        && p.unit().buildPlan() != null);

        if (buildingPlayer != null) {
            unit.plans.add(buildingPlayer.unit().buildPlan());
            status = Core.bundle.format("autoplay.status.following", buildingPlayer.name);
            return true;
        }

        var plans = Vars.player.team().data().plans;

        if (plans.isEmpty()) {
            status = Core.bundle.get("autoplay.status.no-build-plans");
            return false;
        }

        status = Core.bundle.get("autoplay.status.building");
        return true;
    }

    @Override
    public AIController getAI() {
        return ai;
    }

    @Override
    public Optional<Table> settings() {
        Table table = new Table();
        build(table);

        return Optional.of(table);
    }

    private void build(Table table) {
        table.clear();
        table.add("@following").top().left().labelAlign(Align.left).padBottom(5).row();

        table.button("None", Styles.togglet, () -> {
            ai.assistFollowing = null;
            build(table);
        })
                .checked(ai.assistFollowing == null)
                .growX()
                .top()
                .left()
                .labelAlign(Align.left)
                .padBottom(5)
                .row();

        table.pane(t -> {
            t.top();

            for (Player p : Groups.player) {
                if (p.team() != Vars.player.team() || p == Vars.player) {
                    continue;
                }

                t.button(p.name, Styles.togglet, () -> {
                    ai.assistFollowing = p.unit();
                    build(table);
                })
                        .checked(ai.assistFollowing == p.unit())
                        .top()
                        .left()
                        .labelAlign(Align.left)
                        .padBottom(5)
                        .growX()
                        .row();
            }
        }).growX();
    }
}

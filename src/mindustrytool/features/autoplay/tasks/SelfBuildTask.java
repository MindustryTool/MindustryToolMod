package mindustrytool.features.autoplay.tasks;

import arc.Core;
import arc.scene.style.TextureRegionDrawable;
import arc.scene.ui.layout.Table;
import mindustry.Vars;
import mindustry.entities.units.AIController;
import mindustry.entities.units.BuildPlan;
import mindustry.game.Team;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;
import mindustry.ui.Styles;

import java.util.Optional;

public class SelfBuildTask implements AutoplayTask {
    private boolean enabled = true;
    private String status = "";
    private final SelfBuildAI ai = new SelfBuildAI();

    @Override
    public String getName() {
        return Iconc.hammer + " " + Core.bundle.get("autoplay.task.self-build.name");
    }

    @Override
    public TextureRegionDrawable getIcon() {
        return Icon.hammer;
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
    public boolean shouldRun(Unit unit) {
        if (!unit.canBuild()) {
            status = Core.bundle.get("autoplay.status.cannot-build");
            return false;
        }

        if (!unit.plans.isEmpty()) {
            status = Core.bundle.format("autoplay.status.building-self", unit.plans.size);
            return true;
        }

        status = Core.bundle.get("autoplay.status.no-build-plans");
        return false;
    }

    @Override
    public AIController getAI() {
        return ai;
    }

    @Override
    public String getStatus() {
        return status;
    }

    @Override
    public Optional<Table> settings() {
        Table table = new Table();
        table.button("@autoplay.settings.self-build.annihilate-derelict", Styles.togglet, () -> {
            if (Vars.player.unit() == null)
                return;

            var teamData = Vars.state.teams.get(Team.derelict);
            if (teamData != null) {
                teamData.buildings.each(b -> {
                    var plan = new BuildPlan(b.tile.x, b.tile.y);
                    plan.breaking = true;
                    Vars.player.unit().plans.add(plan);
                });
            }
        }).growX().height(50f);
        return Optional.of(table);
    }

    public static class SelfBuildAI extends BaseAutoplayAI {
        @Override
        public void updateMovement() {
            var req = unit.buildPlan();
            if (req != null) {
                float range = Math.min(unit.type.buildRange - 20f, 100f);
                moveTo(req.tile(), range - 10f, 20f);
            }
        }
    }
}

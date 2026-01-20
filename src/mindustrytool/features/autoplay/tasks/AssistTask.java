package mindustrytool.features.autoplay.tasks;

import arc.scene.style.TextureRegionDrawable;
import mindustry.Vars;
import mindustry.ai.types.BuilderAI;
import mindustry.entities.units.AIController;
import mindustry.gen.Groups;
import mindustry.gen.Icon;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;

public class AssistTask implements AutoplayTask {
    private boolean enabled = true;
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
    public boolean shouldRun(Unit unit) {
        if (!unit.canBuild()) {
            return false;
        }

        var buildingPlayer = Groups.player
                .find(p -> p.unit() != null && unit.team() == p.team() && p.unit().buildPlan() != null);

        if (buildingPlayer != null) {
            unit.plans.add(buildingPlayer.unit().buildPlan());

            return true;
        }

        return !Vars.player.team().data().plans.isEmpty();
    }

    @Override
    public AIController getAI() {
        return ai;
    }
}
